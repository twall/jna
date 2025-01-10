/*
 * @(#)dispatch.c       1.9 98/03/22
 *
 * Copyright (c) 1998 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2007-2013 Timothy Wall. All Rights Reserved.
 * Copyright (c) 2007 Wayne Meissner. All Rights Reserved.
 *
 * The contents of this file is dual-licensed under 2 
 * alternative Open Source/Free licenses: LGPL 2.1 or later and 
 * Apache License 2.0. (starting with JNA version 4.0.0).
 * 
 * You can freely decide which license you want to apply to 
 * the project.
 * 
 * You may obtain a copy of the LGPL License at:
 * 
 * http://www.gnu.org/licenses/licenses.html
 * 
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "LGPL2.1".
 * 
 * You may obtain a copy of the Apache License at:
 * 
 * http://www.apache.org/licenses/
 * 
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "AL2.0".
 */

#include "dispatch.h"

#include <string.h>
#include <stdlib.h>

#if defined(_WIN32)
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <psapi.h>
#define STRTYPE wchar_t*
#define NAME2CSTR(ENV,JSTR) newWideCString(ENV,JSTR)
#ifdef _WIN32_WCE
#include <tlhelp32.h>
#define DEFAULT_LOAD_OPTS 0 /* altered search path unsupported on CE */
#undef GetProcAddress
#define GetProcAddress GetProcAddressA
#else
/* See http://msdn.microsoft.com/en-us/library/ms682586(VS.85).aspx:
 * "Note that the standard search strategy and the alternate search strategy
 * specified by LoadLibraryEx with LOAD_WITH_ALTERED_SEARCH_PATH differ in
 * just one way: The standard search begins in the calling application's
 * directory, and the alternate search begins in the directory of the
 * executable module that LoadLibraryEx is loading."
 */
#define DEFAULT_LOAD_OPTS LOAD_WITH_ALTERED_SEARCH_PATH
#endif
#define LOAD_LIBRARY(NAME,OPTS) (NAME ? LoadLibraryExW(NAME, NULL, OPTS) : GetModuleHandleW(NULL))
#define LOAD_ERROR() w32_format_error(GetLastError())
#define STR_ERROR(CODE) w32_format_error(CODE)
#define FREE_LIBRARY(HANDLE) (((HANDLE)==GetModuleHandleW(NULL) || FreeLibrary(HANDLE))?0:-1)
#define FIND_ENTRY(HANDLE, NAME) w32_find_entry(env, HANDLE, NAME)
#else
#include <dlfcn.h>
#include <errno.h>
#include <assert.h>
#define STRTYPE char*
#ifdef USE_DEFAULT_LIBNAME_ENCODING
#define NAME2CSTR(ENV,JSTR) newCString(ENV,JSTR)
#else
#define NAME2CSTR(ENV,JSTR) newCStringUTF8(ENV,JSTR)
#endif
#define DEFAULT_LOAD_OPTS (RTLD_LAZY|RTLD_GLOBAL)
#define LOAD_LIBRARY(NAME,OPTS) dlopen(NAME, OPTS)
static inline char * LOAD_ERROR() {
    const char* message = dlerror();
    char* buf = (char*) malloc(strlen(message) + 1 /* null */);
    strcpy(buf, message);
    return buf;
}
static inline char * STR_ERROR(int code) {
    int i;
    char* buf = NULL;
    for(i = 256; i < (10 * 1024); i += 256) {
        buf = (char*) malloc(i);
        int res = strerror_r(code, buf, i);
        if(res == 0) {
            break;
        }
        free(buf);
        buf = NULL;
        if(res != ERANGE) {
            break;
        }
    }
    if(buf == NULL) {
        int requiredBuffer = 25 /* static part*/ + 10 /* space for int */ + 1 /* null */;
        buf = (char*) malloc(requiredBuffer);
        snprintf(buf, requiredBuffer, "Failed to convert error: %d", code);
    }
    return buf;
}
#define FREE_LIBRARY(HANDLE) dlclose(HANDLE)
#define FIND_ENTRY(HANDLE, NAME) dlsym(HANDLE, NAME)
#endif

#ifdef _AIX
#undef DEFAULT_LOAD_OPTS
#define DEFAULT_LOAD_OPTS (RTLD_MEMBER| RTLD_LAZY | RTLD_GLOBAL)
#undef LOAD_LIBRARY
#define LOAD_LIBRARY(NAME,OPTS) dlopen(NAME, OPTS)
#endif

#include <stdlib.h>
#include <wchar.h>
#include <jni.h>

#ifndef NO_JAWT
#include <jawt.h>
#include <jawt_md.h>
#endif

#ifdef HAVE_PROTECTION
// When we have SEH, default to protection on
#if defined(_WIN32) && !(defined(_WIN64) && defined(__GNUC__))
static int _protect = 1;
#else
static int _protect;
#endif
#undef PROTECT
#define PROTECT _protect
#endif

#define CHARSET_UTF8 "utf8"

#ifdef __cplusplus
extern "C" {
#else
#include <stdbool.h>
#endif

#define MEMCPY(ENV,D,S,L) do {     \
  PSTART(); memcpy(D,S,L); PEND(ENV); \
} while(0)
#define MEMSET(ENV,D,C,L) do {        \
  PSTART(); memset(D,C,L); PEND(ENV); \
} while(0)

#define MASK_CC           com_sun_jna_Function_MASK_CC
#define THROW_LAST_ERROR  com_sun_jna_Function_THROW_LAST_ERROR
#define USE_VARARGS       com_sun_jna_Function_USE_VARARGS
#define USE_VARARGS_SHIFT com_sun_jna_Function_USE_VARARGS_SHIFT

/* Cached class, field and method IDs */
static jclass classObject;
static jclass classClass;
static jclass classMethod;
static jclass classVoid, classPrimitiveVoid;
static jclass classBoolean, classPrimitiveBoolean;
static jclass classByte, classPrimitiveByte;
static jclass classCharacter, classPrimitiveCharacter;
static jclass classShort, classPrimitiveShort;
static jclass classInteger, classPrimitiveInteger;
static jclass classLong, classPrimitiveLong;
static jclass classFloat, classPrimitiveFloat;
static jclass classDouble, classPrimitiveDouble;
static jclass classString, classWString;
#ifndef NO_NIO_BUFFERS
static jclass classBuffer;
static jclass classByteBuffer;
static jclass classCharBuffer;
static jclass classShortBuffer;
static jclass classIntBuffer;
static jclass classLongBuffer;
static jclass classFloatBuffer;
static jclass classDoubleBuffer;
#endif /* NO_NIO_BUFFERS */

static jclass classPointer;
static jclass classNative;
static jclass classStructure;
static jclass classStructureByValue;
static jclass classCallback;
static jclass classCallbackReference;
static jclass classAttachOptions;
static jclass classNativeMapped;
static jclass classIntegerType;
static jclass classPointerType;
static jclass classJNIEnv;
static jclass class_ffi_callback;
static jclass classFromNativeConverter;

static jmethodID MID_Class_getComponentType;
static jmethodID MID_Object_toString;
static jmethodID MID_String_getBytes;
static jmethodID MID_String_getBytes2;
static jmethodID MID_String_toCharArray;
static jmethodID MID_String_init_bytes;
static jmethodID MID_String_init_bytes2;
static jmethodID MID_Method_getReturnType;
static jmethodID MID_Method_getParameterTypes;
static jmethodID MID_Long_init;
static jmethodID MID_Integer_init;
static jmethodID MID_Short_init;
static jmethodID MID_Character_init;
static jmethodID MID_Byte_init;
static jmethodID MID_Boolean_init;
static jmethodID MID_Float_init;
static jmethodID MID_Double_init;
#ifndef NO_NIO_BUFFERS
static jmethodID MID_Buffer_position;
static jmethodID MID_ByteBuffer_array;
static jmethodID MID_ByteBuffer_arrayOffset;
static jmethodID MID_CharBuffer_array;
static jmethodID MID_CharBuffer_arrayOffset;
static jmethodID MID_ShortBuffer_array;
static jmethodID MID_ShortBuffer_arrayOffset;
static jmethodID MID_IntBuffer_array;
static jmethodID MID_IntBuffer_arrayOffset;
static jmethodID MID_LongBuffer_array;
static jmethodID MID_LongBuffer_arrayOffset;
static jmethodID MID_FloatBuffer_array;
static jmethodID MID_FloatBuffer_arrayOffset;
static jmethodID MID_DoubleBuffer_array;
static jmethodID MID_DoubleBuffer_arrayOffset;
#endif /* NO_NIO_BUFFERS */

static jmethodID MID_Pointer_init;
static jmethodID MID_Native_dispose;
static jmethodID MID_Native_fromNativeCallbackParam;
static jmethodID MID_Native_fromNative;
static jmethodID MID_Native_nativeType;
static jmethodID MID_Native_toNativeTypeMapped;
static jmethodID MID_Native_fromNativeTypeMapped;
static jmethodID MID_Structure_getTypeInfo;
static jmethodID MID_Structure_newInstance;
static jmethodID MID_Structure_read;
static jmethodID MID_Structure_write;
static jmethodID MID_CallbackReference_getCallback;
static jmethodID MID_CallbackReference_getFunctionPointer;
static jmethodID MID_CallbackReference_getNativeString;
static jmethodID MID_CallbackReference_initializeThread;
static jmethodID MID_NativeMapped_toNative;
static jmethodID MID_WString_init;
static jmethodID MID_FromNativeConverter_nativeType;
static jmethodID MID_ffi_callback_invoke;

static jfieldID FID_Boolean_value;
static jfieldID FID_Byte_value;
static jfieldID FID_Short_value;
static jfieldID FID_Character_value;
static jfieldID FID_Integer_value;
static jfieldID FID_Long_value;
static jfieldID FID_Float_value;
static jfieldID FID_Double_value;

static jfieldID FID_Pointer_peer;
static jfieldID FID_Structure_memory;
static jfieldID FID_Structure_typeInfo;
static jfieldID FID_IntegerType_value;
static jfieldID FID_PointerType_pointer;

static int IS_BIG_ENDIAN;

jstring fileEncoding;

/* Forward declarations */
static char* newCString(JNIEnv *env, jstring jstr);
static char* newCStringEncoding(JNIEnv *env, jstring jstr, const char* encoding);
static wchar_t* newWideCString(JNIEnv *env, jstring jstr);

#ifndef NO_NIO_BUFFERS
static void* getBufferArray(JNIEnv*, jobject, jobject*, void **, void **);
static void* getDirectBufferAddress(JNIEnv*, jobject);
#endif
static char getArrayComponentType(JNIEnv *, jobject);
static ffi_type* getStructureType(JNIEnv *, jobject);

typedef void (JNICALL* release_t)(JNIEnv*,jarray,void*,jint);

#ifdef _WIN32
static char*
w32_format_error(int err) {
  wchar_t* wbuf = NULL;
  char* buf = NULL;
  int wlen =
    FormatMessageW(FORMAT_MESSAGE_FROM_SYSTEM
                   |FORMAT_MESSAGE_IGNORE_INSERTS
                   |FORMAT_MESSAGE_ALLOCATE_BUFFER,
                   NULL, err, 0, (LPWSTR)&wbuf, 0, NULL);
  if (wlen > 0) {
    int bufSize = WideCharToMultiByte(CP_UTF8, 0, wbuf, -1, NULL, 0, NULL, NULL);
    buf = (char*)malloc(bufSize);
    int result = WideCharToMultiByte(CP_UTF8, 0, wbuf, -1, buf, bufSize, NULL, NULL);
    if (result == 0) {
      fprintf(stderr, "JNA: error converting error message: %d\n", (int)GET_LAST_ERROR());
      free(buf);
      buf = NULL;
    }
  }
  if (wbuf) {
    LocalFree(wbuf);
  }

  return buf;
}
static wchar_t*
w32_short_name(JNIEnv* env, jstring str) {
  wchar_t* wstr = newWideCString(env, str);
  if (wstr && *wstr) {
    DWORD required;
    size_t size = wcslen(wstr) + 5;
    wchar_t* prefixed = (wchar_t*)alloca(sizeof(wchar_t) * size);

    swprintf(prefixed, size, L"\\\\?\\%ls", wstr);

    if ((required = GetShortPathNameW(prefixed, NULL, 0)) != 0) {
      wchar_t* wshort = (wchar_t*)malloc(sizeof(wchar_t) * required);
      if (GetShortPathNameW(prefixed, wshort, required)) {
        free((void *)wstr);
        wstr = wshort;
      }
      else {
        char* buf = LOAD_ERROR();
        throwByName(env, EError, buf);
        free((void *)buf);
        free((void *)wstr);
        free((void *)wshort);
        wstr = NULL;
      }
    }
    else if (GET_LAST_ERROR() != ERROR_FILE_NOT_FOUND) { 
      char* buf = LOAD_ERROR();
      throwByName(env, EError, buf);
      free((void *)buf);
      free((void *)wstr);
      wstr = NULL;
    }
  }
  return wstr;
}

static HANDLE
w32_find_entry(JNIEnv* env, HANDLE handle, const char* funname) {
  void* func = NULL;
  if (handle != GetModuleHandle(NULL)) {
    func = GetProcAddress(handle, funname);
  }
  else {
#if defined(_WIN32_WCE)
    /* CE has no EnumProcessModules, have to use an alternate API */
    HANDLE snapshot;
    if ((snapshot = CreateToolhelp32Snapshot(TH32CS_SNAPMODULE, 0)) != INVALID_HANDLE_VALUE) {
      MODULEENTRY32 moduleInfo;
      moduleInfo.dwSize = sizeof(moduleInfo);
      if (Module32First(snapshot, &moduleInfo)) {
        do {
          if ((func = (void *) GetProcAddress(moduleInfo.hModule, funname))) {
            break;
          }
        } while (Module32Next(snapshot, &moduleInfo));
      }
      CloseToolhelp32Snapshot(snapshot);
    }
#else
    HANDLE cur_proc = GetCurrentProcess ();
    HMODULE *modules;
    DWORD needed, i;
    if (!EnumProcessModules (cur_proc, NULL, 0, &needed)) {
    fail:
      throwByName(env, EError, "Unexpected error enumerating modules");
      return 0;
    }
    modules = (HMODULE*) alloca (needed);
    if (!EnumProcessModules (cur_proc, modules, needed, &needed)) {
      goto fail;
    }
    for (i = 0; i < needed / sizeof (HMODULE); i++) {
      if ((func = (void *) GetProcAddress (modules[i], funname))) {
        break;
      }
    }
#endif
  }
  return func;
}
#endif /* _WIN32 */

#if 0
/** Invokes System.err.println (for debugging only). */
void
println(JNIEnv* env, const char* msg) {
  jclass cls = (*env)->FindClass(env, "java/lang/System");
  if (!cls) {
    fprintf(stderr, "JNA: failed to find java.lang.System\n");
    return;
  }
  jfieldID fid = (*env)->GetStaticFieldID(env, cls, "err",
					  "Ljava/io/PrintStream;");
  jobject err = (*env)->GetStaticObjectField(env, cls, fid);
  if (!err) {
    fprintf(stderr, "JNA: failed to find System.err\n");
    return;
  }
  jclass pscls = (*env)->FindClass(env, "java/io/PrintStream");
  if (!pscls) {
    fprintf(stderr, "JNA: failed to find java.io.PrintStream\n");
    return;
  }
  jmethodID mid = (*env)->GetMethodID(env, pscls, "println",
                                      "(Ljava/lang/String;)V");
  jstring str = newJavaString(env, msg, CHARSET_UTF8);
  (*env)->CallObjectMethod(env, err, mid, str);
}
#endif

/** Throw an exception by name */
void
throwByName(JNIEnv *env, const char *name, const char *msg)
{
  jclass cls;

  jthrowable thrown = (*env)->ExceptionOccurred(env);
  (*env)->ExceptionClear(env);

  cls = (*env)->FindClass(env, name);

  if (cls != NULL) { /* Otherwise an exception has already been thrown */
    if (thrown != NULL) {
        /* Attach thrown exception if possible. */
        /* do_throw_clean is an exit path that will delete 'thrown' if we can't do that. */
        jmethodID init = (*env)->GetMethodID(env, cls, "<init>", "(Ljava/lang/String;)V");
        if (init == NULL) {
            goto do_throw_clean;
        }
        jmethodID initCause = (*env)->GetMethodID(env, cls, "initCause", "(Ljava/lang/Throwable;)Ljava/lang/Throwable;");
        if (initCause == NULL) {
            goto do_throw_clean;
        }
        jobject str = (*env)->NewStringUTF(env, msg);
        if (str == NULL) {
            goto do_throw_clean;
        }
        jthrowable t = (jthrowable) (*env)->NewObject(env, cls, init, str);
        (*env)->DeleteLocalRef(env, str);
        if (t == NULL) {
            goto do_throw_clean;
        }
        jobject ret = (*env)->CallObjectMethod(env, t, initCause, thrown);
        if (ret == NULL) {
            goto do_throw_clean;
        }
        (*env)->DeleteLocalRef(env, ret);
        (*env)->Throw(env, t);
        goto clean_cls;
	} else {
        goto do_throw;
	}
	/* Deletes thrown exception reference and clears the exception.  */
do_throw_clean:
    (*env)->DeleteLocalRef(env, thrown);
    (*env)->ExceptionClear(env);
do_throw:
    (*env)->ThrowNew(env, cls, msg);

clean_cls:
    /* It's a good practice to clean up the local references. */
    (*env)->DeleteLocalRef(env, cls);
  } else if (thrown != NULL) {
    (*env)->DeleteLocalRef(env, thrown);
  }
}

/** Translate FFI errors into exceptions. */
jboolean
ffi_error(JNIEnv* env, const char* op, ffi_status status) {
  char msg[MSG_SIZE];
  switch(status) {
  case FFI_BAD_ABI:
    snprintf(msg, sizeof(msg), "%s: Invalid calling convention (FFI_BAD_ABI)", op);
    throwByName(env, EIllegalArgument, msg);
    return JNI_TRUE;
  case FFI_BAD_TYPEDEF:
    snprintf(msg, sizeof(msg),
             "%s: Invalid structure definition (native typedef error, FFI_BAD_TYPEDEF)", op);
    throwByName(env, EIllegalArgument, msg);
    return JNI_TRUE;
  case FFI_BAD_ARGTYPE:
    snprintf(msg, sizeof(msg),
             "%s: Invalid argument type (FFI_BAD_ARGTYPE)", op);
    throwByName(env, EIllegalArgument, msg);
    return JNI_TRUE;
  default:
    snprintf(msg, sizeof(msg), "%s failed (%d)", op, status);
    throwByName(env, EError, msg);
    return JNI_TRUE;
  case FFI_OK:
    return JNI_FALSE;
  }
}

/* invoke the real native function */
static void
dispatch(JNIEnv *env, void* func, jint flags, jobjectArray args,
         ffi_type *return_type, void *presult)
{
  int i, nargs;
  jvalue* c_args;
  char array_pt;
  struct _array_elements {
    jobject array;
    void *elems;
    release_t release;
  } *array_elements;
  volatile int array_count = 0;
  ffi_cif cif;
  ffi_type** arg_types;
  void** arg_values;
  ffi_abi abi;
  ffi_status status;
  char msg[MSG_SIZE];
  callconv_t callconv = flags & MASK_CC;
  const char* volatile throw_type = NULL;
  const char* volatile throw_msg = NULL;
  int fixed_args = (flags >> USE_VARARGS_SHIFT) & USE_VARARGS;

  nargs = (*env)->GetArrayLength(env, args);

  if (nargs > MAX_NARGS) {
    snprintf(msg, sizeof(msg), "Too many arguments (max %ld)", MAX_NARGS);
    throwByName(env, EUnsupportedOperation, msg);
    return;
  }

  c_args = (jvalue*)alloca(nargs * sizeof(jvalue));
  array_elements = (struct _array_elements*)
    alloca(nargs * sizeof(struct _array_elements));
  arg_types = (ffi_type**)alloca(nargs * sizeof(ffi_type*));
  arg_values = (void**)alloca(nargs * sizeof(void*));

  for (i = 0; i < nargs; i++) {
    jobject arg = (*env)->GetObjectArrayElement(env, args, i);

    if (arg == NULL) {
      c_args[i].l = NULL;
      arg_types[i] = &ffi_type_pointer;
      arg_values[i] = &c_args[i].l;
    }
    else if ((*env)->IsInstanceOf(env, arg, classBoolean)) {
      c_args[i].i = (*env)->GetBooleanField(env, arg, FID_Boolean_value);
      arg_types[i] = &ffi_type_uint32;
      arg_values[i] = &c_args[i].i;
    }
    else if ((*env)->IsInstanceOf(env, arg, classByte)) {
      c_args[i].b = (*env)->GetByteField(env, arg, FID_Byte_value);
      // Promote char to int if we prepare a varargs call
      if(fixed_args && i >= fixed_args && sizeof(char) < sizeof(int)) {
        arg_types[i] = &ffi_type_uint32;
        arg_values[i] = alloca(sizeof(int));
        *(int*)arg_values[i] = (int) c_args[i].b;
      } else {
        arg_types[i] = &ffi_type_sint8;
        arg_values[i] = &c_args[i].b;
      }
    }
    else if ((*env)->IsInstanceOf(env, arg, classShort)) {
      c_args[i].s = (*env)->GetShortField(env, arg, FID_Short_value);
      // Promote short to int if we prepare a varargs call
      if(fixed_args && i >= fixed_args && sizeof(short) < sizeof(int)) {
        arg_types[i] = &ffi_type_uint32;
        arg_values[i] = alloca(sizeof(int));
        *(int*)arg_values[i] = (int) c_args[i].s;
      } else {
        arg_types[i] = &ffi_type_sint16;
        arg_values[i] = &c_args[i].s;
      }
    }
    else if ((*env)->IsInstanceOf(env, arg, classCharacter)) {
      if (sizeof(wchar_t) == 2) {
        c_args[i].c = (*env)->GetCharField(env, arg, FID_Character_value);
        if(fixed_args && i >= fixed_args && sizeof(short) < sizeof(int)) {
          arg_types[i] = &ffi_type_uint32;
          arg_values[i] = alloca(sizeof(int));
          *(int*)arg_values[i] = (int) c_args[i].c;    
        } else {
          arg_types[i] = &ffi_type_uint16;
          arg_values[i] = &c_args[i].c;
        }
      }
      else if (sizeof(wchar_t) == 4) {
        c_args[i].i = (*env)->GetCharField(env, arg, FID_Character_value);
        arg_types[i] = &ffi_type_uint32;
        arg_values[i] = &c_args[i].i;
      }
      else {
        snprintf(msg, sizeof(msg), "Unsupported wchar_t size (%d)", (int)sizeof(wchar_t));
        throw_type = EUnsupportedOperation;
        throw_msg = msg;
        goto cleanup;
      }
    }
    else if ((*env)->IsInstanceOf(env, arg, classInteger)) {
      c_args[i].i = (*env)->GetIntField(env, arg, FID_Integer_value);
      arg_types[i] = &ffi_type_sint32;
      arg_values[i] = &c_args[i].i;
    }
    else if ((*env)->IsInstanceOf(env, arg, classLong)) {
      c_args[i].j = (*env)->GetLongField(env, arg, FID_Long_value);
      arg_types[i] = &ffi_type_sint64;
      arg_values[i] = &c_args[i].j;
    }
    else if ((*env)->IsInstanceOf(env, arg, classFloat)) {
      c_args[i].f = (*env)->GetFloatField(env, arg, FID_Float_value);
      arg_types[i] = &ffi_type_float;
      arg_values[i] = &c_args[i].f;
    }
    else if ((*env)->IsInstanceOf(env, arg, classDouble)) {
      c_args[i].d = (*env)->GetDoubleField(env, arg, FID_Double_value);
      arg_types[i] = &ffi_type_double;
      arg_values[i] = &c_args[i].d;
    }
    else if ((*env)->IsInstanceOf(env, arg, classPointer)) {
      c_args[i].l = getNativeAddress(env, arg);
      arg_types[i] = &ffi_type_pointer;
      arg_values[i] = &c_args[i].l;
    }
    else if ((*env)->IsInstanceOf(env, arg, classJNIEnv)) {
      c_args[i].l = (void*)env;
      arg_types[i] = &ffi_type_pointer;
      arg_values[i] = &c_args[i].l;
    }
    else if ((*env)->IsInstanceOf(env, arg, classStructure)) {
      c_args[i].l = getStructureAddress(env, arg);
      arg_types[i] = getStructureType(env, arg);
      arg_values[i] = c_args[i].l;
      if (!arg_types[i]) {
        snprintf(msg, sizeof(msg),
                 "Structure type info not initialized at argument %d", i);
        throw_type = EIllegalState;
        throw_msg = msg;
        goto cleanup;
      }
    }
#ifndef NO_NIO_BUFFERS
    else if ((*env)->IsInstanceOf(env, arg, classBuffer)) {
      c_args[i].l = getDirectBufferAddress(env, arg);
      arg_types[i] = &ffi_type_pointer;
      arg_values[i] = &c_args[i].l;
      if (c_args[i].l == NULL) {
        c_args[i].l =
          getBufferArray(env, arg, &array_elements[array_count].array,
                         &array_elements[array_count].elems,
                         (void**)&array_elements[array_count].release);
        if (c_args[i].l == NULL) {
          throw_type = EIllegalArgument;
          throw_msg = "Buffer arguments must be direct or have a primitive backing array";
          goto cleanup;
        }
        ++array_count;
      }
    }
#endif /* NO_NIO_BUFFERS */
    else if ((array_pt = getArrayComponentType(env, arg)) != 0
             && array_pt != 'L') {
      void *ptr = NULL;
      release_t release = NULL;

#define GET_ELEMS(TYPE) do {ptr=(*env)->Get##TYPE##ArrayElements(env,arg,NULL); release=(void*)(*env)->Release##TYPE##ArrayElements; }while(0)
      switch(array_pt) {
      case 'Z': GET_ELEMS(Boolean); break;
      case 'B': GET_ELEMS(Byte); break;
      case 'C': GET_ELEMS(Char); break;
      case 'S': GET_ELEMS(Short); break;
      case 'I': GET_ELEMS(Int); break;
      case 'J': GET_ELEMS(Long); break;
      case 'F': GET_ELEMS(Float); break;
      case 'D': GET_ELEMS(Double); break;
      }
      if (!ptr) {
        throw_type = EOutOfMemory;
        throw_msg = "Could not obtain memory for primitive buffer";
        goto cleanup;
      }
      c_args[i].l = ptr;
      arg_types[i] = &ffi_type_pointer;
      arg_values[i] = &c_args[i].l;
      array_elements[array_count].array = arg;
      array_elements[array_count].elems = ptr;
      array_elements[array_count++].release = release;
    }
    else {
      // Anything else, pass directly as a pointer
      c_args[i].l = (void*)arg;
      arg_types[i] = &ffi_type_pointer;
      arg_values[i] = &c_args[i].l;
    }
  }

  switch (callconv) {
  case CALLCONV_C:
    abi = FFI_DEFAULT_ABI;
    break;
#ifdef _WIN32
  case CALLCONV_STDCALL:
#if defined(_WIN64) || defined(_WIN32_WCE)
    // Ignore requests for stdcall on win64/wince
    abi = FFI_DEFAULT_ABI;
#else
    abi = FFI_STDCALL;
#endif
    break;
#endif // _WIN32
  default:
    abi = (int)callconv;
    if (!(abi > FFI_FIRST_ABI && abi < FFI_LAST_ABI)) {
      snprintf(msg, sizeof(msg),
               "Unrecognized calling convention: %d", abi);
      throw_type = EIllegalArgument;
      throw_msg = msg;
      goto cleanup;
    }
    break;
  }

  status = fixed_args
    ? ffi_prep_cif_var(&cif, abi, fixed_args, nargs, return_type, arg_types)
    : ffi_prep_cif(&cif, abi, nargs, return_type, arg_types);
  if (!ffi_error(env, "Native call setup", status)) {
    PSTART();
    if ((flags & THROW_LAST_ERROR) != 0) {
      SET_LAST_ERROR(0);
    }
    ffi_call(&cif, FFI_FN(func), presult, arg_values);
    {
      int err = GET_LAST_ERROR();
      JNA_set_last_error(env, err);
      if ((flags & THROW_LAST_ERROR) && err) {
        char* emsg = STR_ERROR(err);
        snprintf(msg, MSG_SIZE, "[%d] %s", err, emsg);
        free(emsg);
        throw_type = ELastError;
        throw_msg = msg;
      }
    }

    PROTECTED_END(do { throw_type=EError;throw_msg="Invalid memory access";} while(0));
  }

 cleanup:

  // Release array elements
  for (i=0;i < array_count;i++) {
    array_elements[i].release(env, array_elements[i].array,
                              array_elements[i].elems, 0);
  }

  // Must raise any exception *after* all other JNI operations
  if (throw_type) {
    throwByName(env, throw_type, throw_msg);
  }
}

/** Copy characters from the Java character array into native memory. */
static void
getChars(JNIEnv* env, wchar_t* volatile dst, jcharArray chars, volatile jint off, volatile jint len) {
  PSTART();

  if (sizeof(jchar) == sizeof(wchar_t)) {
    (*env)->GetCharArrayRegion(env, chars, off, len, (jchar*)dst);
  }
  else {
    jchar* buf;
    int count = len > 1000 ? 1000 : len;
    buf = (jchar *)alloca(count * sizeof(jchar));
    if (!buf) {
      throwByName(env, EOutOfMemory, "Can't read characters");
    }
    else {
      while (len > 0) {
        int i;
        (*env)->GetCharArrayRegion(env, chars, off, count, buf);
        for (i=0;i < count;i++) {
          // TODO: ensure proper encoding conversion from jchar to native
          // wchar_t 
          dst[i] = (wchar_t)buf[i];
        }
        dst += count;
        off += count;
        len -= count;
        if (count > len) count = len;
      }
    }
  }
  PEND(env);
}

static void
setChars(JNIEnv* env, wchar_t* src, jcharArray chars, volatile jint off, volatile jint len) {
  jchar* buf = (jchar*)src;
  PSTART();

  if (sizeof(jchar) == sizeof(wchar_t)) {
    (*env)->SetCharArrayRegion(env, chars, off, len, buf);
  }
  else {
    int count = len > 1000 ? 1000 : len;
    buf = (jchar *)alloca(count * sizeof(jchar));
    if (!buf) {
      throwByName(env, EOutOfMemory, "Can't write characters");
    }
    else {
      while (len > 0) {
        int i;
        for (i=0;i < count;i++) {
          buf[i] = (jchar)src[off+i];
        }
        (*env)->SetCharArrayRegion(env, chars, off, count, buf);
        off += count;
        len -= count;
        if (count > len) count = len;
      }
    }
  }
  PEND(env);
}

/* Translates a Java string to a C string using the
 * String.getBytes(byte[],String), using the requested encoding.
 */
static char *
newCString(JNIEnv *env, jstring jstr)
{
    jbyteArray bytes = 0;
    char *result = NULL;

    bytes = (*env)->CallObjectMethod(env, jstr, MID_String_getBytes);
    if (!(*env)->ExceptionCheck(env)) {
        jint len = (*env)->GetArrayLength(env, bytes);
        result = (char *)malloc(len + 1);
        if (result == NULL) {
            (*env)->DeleteLocalRef(env, bytes);
            throwByName(env, EOutOfMemory, "Can't allocate C string");
            return NULL;
        }
        (*env)->GetByteArrayRegion(env, bytes, 0, len, (jbyte *)result);
        result[len] = 0; /* NUL-terminate */
    }
    (*env)->DeleteLocalRef(env, bytes);
    return result;
}

/* Translates a Java string to a C string using the String.getBytes("UTF8")
 * method, which uses UTF8 encoding.
 */
const char *
newCStringUTF8(JNIEnv *env, jstring jstr)
{
  return newCStringEncoding(env, jstr, CHARSET_UTF8);
}

static char*
newCStringEncoding(JNIEnv *env, jstring jstr, const char* encoding)
{
    jbyteArray bytes = 0;
    char *result = NULL;

    if (!encoding) return newCString(env, jstr);

    bytes = (*env)->CallObjectMethod(env, jstr, MID_String_getBytes2,
                                     newJavaString(env, encoding, CHARSET_UTF8));
    if (!(*env)->ExceptionCheck(env)) {
        jint len = (*env)->GetArrayLength(env, bytes);
        result = (char *)malloc(len + 1);
        if (result == NULL) {
            (*env)->DeleteLocalRef(env, bytes);
            throwByName(env, EOutOfMemory, "Can't allocate C string");
            return NULL;
        }
        (*env)->GetByteArrayRegion(env, bytes, 0, len, (jbyte *)result);
        result[len] = 0; /* NUL-terminate */
    }
    (*env)->DeleteLocalRef(env, bytes);
    return result;
}

/* Translates a Java string to a wide C string using the String.toCharArray
 * method.
 */
static wchar_t *
newWideCString(JNIEnv *env, jstring str)
{
    jcharArray chars = 0;
    wchar_t *result = NULL;

    if ((*env)->IsSameObject(env, str, NULL)) {
      return result;
    }

    chars = (*env)->CallObjectMethod(env, str, MID_String_toCharArray);
    if (!(*env)->ExceptionCheck(env)) {
        jint len = (*env)->GetArrayLength(env, chars);
        result = (wchar_t *)malloc(sizeof(wchar_t) * (len + 1));
        if (result == NULL) {
            (*env)->DeleteLocalRef(env, chars);
            throwByName(env, EOutOfMemory, "Can't allocate wide C string");
            return NULL;
        }
        getChars(env, result, chars, 0, len);
        if ((*env)->ExceptionCheck(env)) {
          free((void *)result);
          result = NULL;
        }
        else {
          result[len] = 0; /* NUL-terminate */
        }
    }
    (*env)->DeleteLocalRef(env, chars);
    return result;
}

jobject
newJavaWString(JNIEnv *env, const wchar_t* ptr) {
  if (ptr) {
    jstring s = newJavaString(env, (const char*)ptr, NULL);
    return (*env)->NewObject(env, classWString, MID_WString_init, s);
  }
  return NULL;
}

jstring
encodingString(JNIEnv *env, const char* ptr) {
  jstring result = NULL;
  jbyteArray bytes = 0;
  int len = (int)strlen((const char*)ptr);
  
  bytes = (*env)->NewByteArray(env, len);
  if (bytes != NULL) {
    (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte *)ptr);
    result = (*env)->NewObject(env, classString,
                               MID_String_init_bytes, bytes);
    (*env)->DeleteLocalRef(env, bytes);
  }
  return result;
}

/* Constructs a Java string from a char array (using the String(byte[],String)
 * constructor) or a short array (using the
 * String(char[]) ctor, which uses the character values unmodified).
 */
jstring
newJavaString(JNIEnv *env, const char *ptr, const char* charset)
{
    volatile jstring result = 0;
    PSTART();

    if (ptr) {
      if (charset == NULL) {
        // TODO: proper conversion from native wchar_t to jchar, if any
        jsize len = (int)wcslen((const wchar_t*)ptr);
        if (sizeof(jchar) != sizeof(wchar_t)) {
          // NOTE: while alloca may succeed here, writing to the stack
          // memory may fail with really large buffers
          jchar* buf = (jchar*)malloc(len * sizeof(jchar));
          if (!buf) {
            throwByName(env, EOutOfMemory, "Can't allocate space for conversion to Java String");
          }
          else {
            int i;
            for (i=0;i < len;i++) {
              buf[i] = *((const wchar_t*)ptr + i);
            }
            result = (*env)->NewString(env, buf, len);
            free((void*)buf);
          }
        }
        else {
          result = (*env)->NewString(env, (const jchar*)ptr, len);
        }
      }
      else {
        jbyteArray bytes = 0;
        int len = (int)strlen((const char*)ptr);

        bytes = (*env)->NewByteArray(env, len);
        if (bytes != NULL) {
          (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte *)ptr);
          result = (*env)->NewObject(env, classString,
                                     MID_String_init_bytes2, bytes, 
                                     encodingString(env, charset));
          (*env)->DeleteLocalRef(env, bytes);
        }
      }
    }
    PEND(env);

    return result;
}

jobject
newJavaPointer(JNIEnv *env, void *p)
{
    jobject obj = NULL;
    if (p != NULL) {
      obj = (*env)->NewObject(env, classPointer, MID_Pointer_init, A2L(p));
    }
    return obj;
}

jobject
newJavaStructure(JNIEnv *env, void *data, jclass type)
{
  if (data != NULL) {
    volatile jobject obj = (*env)->CallStaticObjectMethod(env, classStructure, MID_Structure_newInstance, type, A2L(data));
    if (obj == NULL) {
      fprintf(stderr, "JNA: failed to create structure\n");
    }
    return obj;
  }
  return NULL;
}

jobject
newJavaCallback(JNIEnv* env, void* fptr, jclass type)
{
  if (fptr != NULL) {
    jobject ptr = newJavaPointer(env, fptr);
    return (*env)->CallStaticObjectMethod(env, classCallbackReference,
                                          MID_CallbackReference_getCallback,
                                          type, ptr, JNI_TRUE);
  }
  return NULL;
}

void*
getNativeString(JNIEnv* env, jstring s, jboolean wide) {
  if (s != NULL) {
    jobject ptr = (*env)->CallStaticObjectMethod(env, classCallbackReference,
                                                 MID_CallbackReference_getNativeString,
                                                 s, wide);
    if (!(*env)->ExceptionCheck(env)) {
      return getNativeAddress(env, ptr);
    }
  }
  return NULL;
}

int
get_conversion_flag(JNIEnv* env, jclass cls) {
  int type = get_java_type(env, cls);
  if (type == 's') {
    return CVT_STRUCTURE_BYVAL;
  }
  if (type == '*') {
    if ((*env)->IsAssignableFrom(env, cls, classPointer)) {
      return CVT_POINTER;
    }
    if ((*env)->IsAssignableFrom(env, cls, classStructure)) {
      return CVT_STRUCTURE;
    }
    if ((*env)->IsAssignableFrom(env, cls, classString)) {
      return CVT_STRING;
    }
    if ((*env)->IsAssignableFrom(env, cls, classWString)) {
      return CVT_WSTRING;
    }
    if ((*env)->IsAssignableFrom(env, cls, classCallback)) {
      return CVT_CALLBACK;
    }
    if ((*env)->IsAssignableFrom(env, cls, classIntegerType)) {
      return CVT_INTEGER_TYPE;
    }
    if ((*env)->IsAssignableFrom(env, cls, classPointerType)) {
      return CVT_POINTER_TYPE;
    }
    if ((*env)->IsAssignableFrom(env, cls, classNativeMapped)) {
      return CVT_NATIVE_MAPPED;
    }
  }
  return CVT_DEFAULT;
}

int
get_java_type_from_ffi_type(ffi_type* type) {
  switch(type->type) {
    // NOTE 'Z' aliases 'C' on *nix and platforms where sizeof(wchar_t) is 4;
    // this will cause problems if anyone ever installs a type mapper for 
    // char/Character (not a common arg type)
  case FFI_TYPE_UINT32: return 'Z';
  case FFI_TYPE_SINT8: return 'B';
  case FFI_TYPE_SINT16: return 'S';
  case FFI_TYPE_UINT16: return 'C';
  case FFI_TYPE_SINT32: return 'I';
  case FFI_TYPE_SINT64: return 'J';
  case FFI_TYPE_FLOAT: return 'F';
  case FFI_TYPE_DOUBLE: return 'D';
  default: return '*';
  }
}

int
get_java_type(JNIEnv* env, jclass cls) {

  if ((*env)->IsSameObject(env, classVoid, cls)
      || (*env)->IsSameObject(env, classPrimitiveVoid, cls))
    return 'V';
  if ((*env)->IsSameObject(env, classBoolean, cls)
      || (*env)->IsSameObject(env, classPrimitiveBoolean, cls))
    return 'Z';
  if ((*env)->IsSameObject(env, classByte, cls)
      || (*env)->IsSameObject(env, classPrimitiveByte, cls))
    return 'B';
  if ((*env)->IsSameObject(env, classCharacter, cls)
      || (*env)->IsSameObject(env, classPrimitiveCharacter, cls))
    return 'C';
  if ((*env)->IsSameObject(env,classShort, cls)
      || (*env)->IsSameObject(env, classPrimitiveShort, cls))
    return 'S';
  if ((*env)->IsSameObject(env, classInteger, cls)
      || (*env)->IsSameObject(env, classPrimitiveInteger, cls))
    return 'I';
  if ((*env)->IsSameObject(env, classLong, cls)
      || (*env)->IsSameObject(env, classPrimitiveLong, cls))
    return 'J';
  if ((*env)->IsSameObject(env, classFloat, cls)
      || (*env)->IsSameObject(env, classPrimitiveFloat, cls))
    return 'F';
  if ((*env)->IsSameObject(env, classDouble, cls)
      || (*env)->IsSameObject(env, classPrimitiveDouble, cls))
    return 'D';
  if ((*env)->IsAssignableFrom(env, cls, classStructure)) {
    if ((*env)->IsAssignableFrom(env, cls, classStructureByValue))
      return 's';
    return '*';
  }
  if ((*env)->IsAssignableFrom(env, cls, classPointer)
      || (*env)->IsAssignableFrom(env, cls, classCallback)
      || (*env)->IsAssignableFrom(env, cls, classNativeMapped)
      || (*env)->IsAssignableFrom(env, cls, classWString)
      || (*env)->IsAssignableFrom(env, cls, classString))
    return '*';
  return -1;
}

jlong
getIntegerTypeValue(JNIEnv* env, jobject obj) {
  if(obj == NULL) {
    return 0;
  } else {
    return (*env)->GetLongField(env, obj, FID_IntegerType_value);
  }
}

void*
getPointerTypeAddress(JNIEnv* env, jobject obj) {
  return getNativeAddress(env, (*env)->GetObjectField(env, obj, FID_PointerType_pointer));
}

void *
getStructureAddress(JNIEnv *env, jobject obj) {
  if (obj != NULL) {
    jobject ptr = (*env)->GetObjectField(env, obj, FID_Structure_memory);
    if (!(*env)->ExceptionCheck(env)) {
      return getNativeAddress(env, ptr);
    }
  }
  return NULL;
}

void
writeStructure(JNIEnv *env, jobject s) {
  if (s != NULL) {
    (*env)->CallVoidMethod(env, s, MID_Structure_write);
  }
}

void *
getCallbackAddress(JNIEnv *env, jobject obj) {
  if (obj != NULL) {
    jobject ptr = (*env)->CallStaticObjectMethod(env, classCallbackReference, MID_CallbackReference_getFunctionPointer, obj, JNI_TRUE);
    if (!(*env)->ExceptionCheck(env)) {
      return getNativeAddress(env, ptr);
    }
  }
  return NULL;
}

jobject
initializeThread(callback* cb, AttachOptions* args) {
  JavaVM* jvm = cb->vm;
  JNIEnv* env;
  jobject group = NULL;
  int attached = (*jvm)->GetEnv(jvm, (void *)&env, JNI_VERSION_1_4) == JNI_OK;

  if (!attached) {
    if ((*jvm)->AttachCurrentThread(jvm, (void *)&env, NULL) != JNI_OK) {
      fprintf(stderr, "JNA: Can't attach native thread to VM for callback thread initialization\n");
      return NULL;
    }
  }
  (*env)->PushLocalFrame(env, 16);
  {
    jobject cbobj = (*env)->NewLocalRef(env, cb->object);
    if (!(*env)->IsSameObject(env, cbobj, NULL)) {
      jobject argsobj = newJavaStructure(env, args, classAttachOptions);
      group = (*env)->CallStaticObjectMethod(env, classCallbackReference,
                                             MID_CallbackReference_initializeThread,
                                             cbobj, argsobj);
      if (group != NULL) {
        group = (*env)->NewWeakGlobalRef(env, group);
      }
      if (args->name != NULL) {
        // Make a copy, since the Java Structure which owns this native memory
        // will go out of scope and be available for GC
        args->name = STRDUP(args->name);
      }
    }
  }
  (*env)->PopLocalFrame(env, NULL);
  if (!attached) {
    if ((*jvm)->DetachCurrentThread(jvm) != 0) {
      fprintf(stderr, "JNA: could not detach thread after callback init\n");
    }
  }

  return group;
}

jclass
getNativeType(JNIEnv* env, jclass cls) {
  return (*env)->CallStaticObjectMethod(env, classNative,
                                        MID_Native_nativeType, cls);
}

jclass
getNativeTypeMapped(JNIEnv* env, jobject converter) {
  return (*env)->CallObjectMethod(env, converter,
                                          MID_FromNativeConverter_nativeType);
}

void
toNative(JNIEnv* env, jobject obj, void* valuep, size_t size, jboolean promote, const char* encoding) {
  if (obj != NULL) {
    jobject arg = (*env)->CallObjectMethod(env, obj, MID_NativeMapped_toNative);
    if (!(*env)->ExceptionCheck(env)) {
      extract_value(env, arg, valuep, size, promote, encoding);
    }
  }
  else {
    MEMSET(env, valuep, 0, size);
  }
}

static void
toNativeTypeMapped(JNIEnv* env, jobject obj, void* valuep, size_t size, jobject to_native, const char* encoding) {
  if (obj != NULL) {
    jobject arg = (*env)->CallStaticObjectMethod(env, classNative, MID_Native_toNativeTypeMapped, to_native, obj);
    if (!(*env)->ExceptionCheck(env)) {
      extract_value(env, arg, valuep, size, JNI_FALSE, encoding);
    }
  }
  else {
    MEMSET(env, valuep, 0, size);
  }
}

static void
fromNativeTypeMapped(JNIEnv* env, jobject from_native,
                     void* native_return_value,
                     int jtype, size_t size,
                     jobject java_method,
                     void* result_storage,
                     const char* encoding) {
  jobject value = new_object(env, (char)jtype, native_return_value, JNI_TRUE, encoding);
  if (!(*env)->ExceptionCheck(env)) {
    jobject obj = (*env)->CallStaticObjectMethod(env, classNative,
                                                 MID_Native_fromNativeTypeMapped,
                                                 from_native, value, java_method);
    if (!(*env)->ExceptionCheck(env)) {
      // Convert objects into primitive types if the return class demands it
      jclass java_return_class = (*env)->CallObjectMethod(env, java_method, MID_Method_getReturnType);
      if ((*env)->IsSameObject(env, java_return_class, classPrimitiveBoolean)
          || (*env)->IsSameObject(env, java_return_class, classPrimitiveByte)
          || (*env)->IsSameObject(env, java_return_class, classPrimitiveCharacter)
          || (*env)->IsSameObject(env, java_return_class, classPrimitiveShort)
          || (*env)->IsSameObject(env, java_return_class, classPrimitiveInteger)
          || (*env)->IsSameObject(env, java_return_class, classPrimitiveLong)
          || (*env)->IsSameObject(env, java_return_class, classPrimitiveFloat)
          || (*env)->IsSameObject(env, java_return_class, classPrimitiveDouble)) {
        extract_value(env, obj, result_storage, size, JNI_TRUE, encoding);
      }
      else {
        *(jobject*)result_storage = obj;
      }
    }
  }
}

jobject
fromNativeCallbackParam(JNIEnv* env, jclass javaClass, ffi_type* type, void* resp, jboolean promote, const char* encoding) {
  int jtype = get_java_type_from_ffi_type(type);
  jobject value = new_object(env, (char)jtype, resp, promote, encoding);
  if (!(*env)->ExceptionCheck(env)) {
    return (*env)->CallStaticObjectMethod(env, classNative,
                                          MID_Native_fromNativeCallbackParam,
                                          javaClass, value);
  }
  return NULL;
}

jobject
fromNative(JNIEnv* env, jobject javaMethod, ffi_type* type, void* resp, jboolean promote, const char* encoding) {
  int jtype = get_java_type_from_ffi_type(type);
  jobject value = new_object(env, (char)jtype, resp, promote, encoding);
  if (!(*env)->ExceptionCheck(env)) {
    return (*env)->CallStaticObjectMethod(env, classNative,
                                          MID_Native_fromNative,
                                          javaMethod, value);
  }
  return NULL;
}


static ffi_type*
getStructureType(JNIEnv *env, jobject obj) {
  jlong typeInfo = (*env)->GetLongField(env, obj, FID_Structure_typeInfo);
  if (!typeInfo) {
    (*env)->CallObjectMethod(env, obj, MID_Structure_getTypeInfo);
    if (!(*env)->ExceptionCheck(env)) {
      typeInfo = (*env)->GetLongField(env, obj, FID_Structure_typeInfo);
    }
  }
  return (ffi_type*)L2A(typeInfo);
}

void *
getNativeAddress(JNIEnv *env, jobject obj) {
  if (obj != NULL)
    return L2A((*env)->GetLongField(env, obj, FID_Pointer_peer));
  return NULL;
}

static char
getArrayComponentType(JNIEnv *env, jobject obj) {
  jclass cls = (*env)->GetObjectClass(env, obj);
  jclass type = (*env)->CallObjectMethod(env, cls, MID_Class_getComponentType);
  if (type != NULL) {
    return (char)get_java_type(env, type);
  }
  return 0;
}

#ifndef NO_NIO_BUFFERS
/** Get the direct buffer address, accounting for buffer position. */
static void*
getDirectBufferAddress(JNIEnv* env, jobject buf) {
  void *ptr = (*env)->GetDirectBufferAddress(env, buf);
  if (ptr != NULL) {
    int offset = (*env)->CallIntMethod(env, buf, MID_Buffer_position);
    int size = 0;
    if ((*env)->IsInstanceOf(env, buf, classByteBuffer)) {
      size = 1;
    }
    else if ((*env)->IsInstanceOf(env, buf, classCharBuffer)) {
      // WARNING: likely mismatch with sizeof(wchar_t)
      size = 2;
    }
    else if ((*env)->IsInstanceOf(env, buf, classShortBuffer)) {
      size = 2;
    }
    else if ((*env)->IsInstanceOf(env, buf, classIntBuffer)) {
      size = 4;
    }
    else if ((*env)->IsInstanceOf(env, buf, classLongBuffer)) {
      size = 8;
    }
    else if ((*env)->IsInstanceOf(env, buf, classFloatBuffer)) {
      size = 4;
    }
    else if ((*env)->IsInstanceOf(env, buf, classDoubleBuffer)) {
      size = 8;
    }
    else {
      ptr = NULL;
      throwByName(env, EError, "Unrecognized NIO buffer type");
    }
    ptr = (char*)ptr + offset*size;
  }
  return ptr;
}

static void*
getBufferArray(JNIEnv* env, jobject buf,
               jobject* arrayp, void **basep,
               void **releasep) {
  void *ptr = NULL;
  int offset = 0;
  jobject array = NULL;

#define GET_ARRAY(TYPE, ELEM_SIZE) \
  do {                                                                  \
    array = (*env)->CallObjectMethod(env, buf, MID_##TYPE##Buffer_array); \
    if (array != NULL) {                                                \
      offset =                                                          \
        ((*env)->CallIntMethod(env, buf, MID_##TYPE##Buffer_arrayOffset) \
         + (*env)->CallIntMethod(env, buf, MID_Buffer_position))        \
        * ELEM_SIZE;                                                    \
      ptr = (*env)->Get##TYPE##ArrayElements(env, array, NULL);         \
      if (releasep) *releasep = (void*)(*env)->Release##TYPE##ArrayElements; \
    }                                                                   \
    else if (releasep) *releasep = NULL;                                \
  } while(0)

  if ((*env)->IsInstanceOf(env, buf, classByteBuffer)) {
    GET_ARRAY(Byte, 1);
  }
  else if((*env)->IsInstanceOf(env, buf, classCharBuffer)) {
    // WARNING: likely mismatch with sizeof(wchar_t)
    GET_ARRAY(Char, 2);
  }
  else if((*env)->IsInstanceOf(env, buf, classShortBuffer)) {
    GET_ARRAY(Short, 2);
  }
  else if((*env)->IsInstanceOf(env, buf, classIntBuffer)) {
    GET_ARRAY(Int, 4);
  }
  else if((*env)->IsInstanceOf(env, buf, classLongBuffer)) {
    GET_ARRAY(Long, 8);
  }
  else if((*env)->IsInstanceOf(env, buf, classFloatBuffer)) {
    GET_ARRAY(Float, 4);
  }
  else if((*env)->IsInstanceOf(env, buf, classDoubleBuffer)) {
    GET_ARRAY(Double, 8);
  }
  if (ptr != NULL) {
    if (basep) *basep = ptr;
    if (arrayp) *arrayp = array;
    ptr = (char *)ptr + offset;
  }

  return ptr;
}
#endif /* NO_NIO_BUFFERS */

static jstring
get_system_property(JNIEnv* env, const char* name) {
  jclass classSystem = (*env)->FindClass(env, "java/lang/System");
  if (classSystem != NULL) {
    jmethodID mid = (*env)->GetStaticMethodID(env, classSystem, "getProperty",
                                              "(Ljava/lang/String;)Ljava/lang/String;");
    if (mid != NULL) {
      jstring propname = newJavaString(env, name, CHARSET_UTF8);
      return (*env)->CallStaticObjectMethod(env, classSystem, mid, propname);
    }
  }
  return NULL;
}

static const char*
JNA_init(JNIEnv* env) {
  if (!LOAD_CREF(env, Object, "java/lang/Object")) return "java.lang.Object";
  if (!LOAD_CREF(env, Class, "java/lang/Class")) return "java.lang.Class";
  if (!LOAD_CREF(env, Method, "java/lang/reflect/Method")) return "java.lang.reflect.Method";
  if (!LOAD_CREF(env, String, "java/lang/String")) return "java.lang.String";
#ifndef NO_NIO_BUFFERS
  if (!LOAD_CREF(env, Buffer, "java/nio/Buffer")) return "java.nio.Buffer";
  if (!LOAD_CREF(env, ByteBuffer, "java/nio/ByteBuffer")) return "java.nio.ByteBuffer";
  if (!LOAD_CREF(env, CharBuffer, "java/nio/CharBuffer")) return "java.nio.CharBuffer";
  if (!LOAD_CREF(env, ShortBuffer, "java/nio/ShortBuffer")) return "java.nio.ShortBuffer";
  if (!LOAD_CREF(env, IntBuffer, "java/nio/IntBuffer")) return "java.nio.IntBuffer";
  if (!LOAD_CREF(env, LongBuffer, "java/nio/LongBuffer")) return "java.nio.LongBuffer";
  if (!LOAD_CREF(env, FloatBuffer, "java/nio/FloatBuffer")) return "java.nio.FloatBuffer";
  if (!LOAD_CREF(env, DoubleBuffer, "java/nio/DoubleBuffer")) return "java.nio.DoubleBuffer";
#endif

  if (!LOAD_PCREF(env, Void, "java/lang/Void")) return "java.lang.Void";
  if (!LOAD_PCREF(env, Boolean, "java/lang/Boolean")) return "java.lang.Boolean";
  if (!LOAD_PCREF(env, Byte, "java/lang/Byte")) return "java.lang.Byte";
  if (!LOAD_PCREF(env, Character, "java/lang/Character")) return "java.lang.Character";
  if (!LOAD_PCREF(env, Short, "java/lang/Short")) return "java.lang.Short";
  if (!LOAD_PCREF(env, Integer, "java/lang/Integer")) return "java.lang.Integer";
  if (!LOAD_PCREF(env, Long, "java/lang/Long")) return "java.lang.Long";
  if (!LOAD_PCREF(env, Float, "java/lang/Float")) return "java.lang.Float";
  if (!LOAD_PCREF(env, Double, "java/lang/Double")) return "java.lang.Double";

  if (!LOAD_MID(env, MID_Long_init, classLong,
                "<init>", "(J)V"))
    return "java.lang.Long<init>(J)V";
  if (!LOAD_MID(env, MID_Integer_init, classInteger,
                "<init>", "(I)V"))
    return "java.lang.Integer<init>(I)V";
  if (!LOAD_MID(env, MID_Short_init, classShort,
                "<init>", "(S)V"))
    return "java.lang.Short<init>(S)V";
  if (!LOAD_MID(env, MID_Character_init, classCharacter,
                "<init>", "(C)V"))
    return "java.lang.Character<init>(C)V";
  if (!LOAD_MID(env, MID_Byte_init, classByte,
                "<init>", "(B)V"))
    return "java.lang.Byte<init>(B)V";
  if (!LOAD_MID(env, MID_Boolean_init, classBoolean,
                "<init>", "(Z)V"))
    return "java.lang.Boolean<init>(Z)V";
  if (!LOAD_MID(env, MID_Float_init, classFloat,
                "<init>", "(F)V"))
    return "java.lang.Float<init>(F)V";
  if (!LOAD_MID(env, MID_Double_init, classDouble,
                "<init>", "(D)V"))
    return "java.lang.Double<init>(D)V";
  if (!LOAD_MID(env, MID_Class_getComponentType, classClass,
                "getComponentType", "()Ljava/lang/Class;"))
    return "Class.getComponentType()";
  if (!LOAD_MID(env, MID_Object_toString, classObject,
                "toString", "()Ljava/lang/String;"))
    return "Object.toString()";
  if (!LOAD_MID(env, MID_String_getBytes, classString,
                "getBytes", "()[B"))
    return "String.getBytes()";
  if (!LOAD_MID(env, MID_String_getBytes2, classString,
                "getBytes", "(Ljava/lang/String;)[B"))
    return "String.getBytes(String)";
  if (!LOAD_MID(env, MID_String_toCharArray, classString,
                "toCharArray", "()[C"))
    return "String.toCharArray()";
  if (!LOAD_MID(env, MID_String_init_bytes, classString,
                "<init>", "([B)V"))
    return "String<init>([B)V";
  if (!LOAD_MID(env, MID_String_init_bytes2, classString,
                "<init>", "([BLjava/lang/String;)V"))
    return "String<init>([B)V";
  if (!LOAD_MID(env, MID_Method_getParameterTypes, classMethod,
                "getParameterTypes", "()[Ljava/lang/Class;"))
    return "Method.getParameterTypes()";
  if (!LOAD_MID(env, MID_Method_getReturnType, classMethod,
                "getReturnType", "()Ljava/lang/Class;"))
    return "Method.getReturnType()";

#ifndef NO_NIO_BUFFERS
  if (!LOAD_MID(env, MID_Buffer_position, classBuffer, "position", "()I"))
    return "Buffer.position";
  if (!LOAD_MID(env, MID_ByteBuffer_array, classByteBuffer, "array", "()[B"))
    return "ByteBuffer.array";
  if (!LOAD_MID(env, MID_ByteBuffer_arrayOffset, classByteBuffer, "arrayOffset", "()I"))
    return "ByteBuffer.arrayOffset";
  if (!LOAD_MID(env, MID_CharBuffer_array, classCharBuffer, "array", "()[C"))
    return "CharBuffer.array";
  if (!LOAD_MID(env, MID_CharBuffer_arrayOffset, classCharBuffer, "arrayOffset", "()I"))
    return "CharBuffer.arrayOffset";
  if (!LOAD_MID(env, MID_ShortBuffer_array, classShortBuffer, "array", "()[S"))
    return "ShortBuffer.array";
  if (!LOAD_MID(env, MID_ShortBuffer_arrayOffset, classShortBuffer, "arrayOffset", "()I"))
    return "ShortBuffer.arrayOffset";
  if (!LOAD_MID(env, MID_IntBuffer_array, classIntBuffer, "array", "()[I"))
    return "IntBuffer.array";
  if (!LOAD_MID(env, MID_IntBuffer_arrayOffset, classIntBuffer, "arrayOffset", "()I"))
    return "IntBuffer.arrayOffset";
  if (!LOAD_MID(env, MID_LongBuffer_array, classLongBuffer, "array", "()[J"))
    return "LongBuffer.array";
  if (!LOAD_MID(env, MID_LongBuffer_arrayOffset, classLongBuffer, "arrayOffset", "()I"))
    return "LongBuffer.arrayOffset";
  if (!LOAD_MID(env, MID_FloatBuffer_array, classFloatBuffer, "array", "()[F"))
    return "FloatBuffer.array";
  if (!LOAD_MID(env, MID_FloatBuffer_arrayOffset, classFloatBuffer, "arrayOffset", "()I"))
    return "FloatBuffer.arrayOffset";
  if (!LOAD_MID(env, MID_DoubleBuffer_array, classDoubleBuffer, "array", "()[D"))
    return "DoubleBuffer.array";
  if (!LOAD_MID(env, MID_DoubleBuffer_arrayOffset, classDoubleBuffer, "arrayOffset", "()I"))
    return "DoubleBuffer.arrayOffset";
#endif

  if (!LOAD_FID(env, FID_Boolean_value, classBoolean, "value", "Z"))
    return "Boolean.value";
  if (!LOAD_FID(env, FID_Byte_value, classByte, "value", "B"))
    return "Byte.value";
  if (!LOAD_FID(env, FID_Short_value, classShort, "value", "S"))
    return "Short.value";
  if (!LOAD_FID(env, FID_Character_value, classCharacter, "value", "C"))
    return "Character.value";
  if (!LOAD_FID(env, FID_Integer_value, classInteger, "value", "I"))
    return "Integer.value";
  if (!LOAD_FID(env, FID_Long_value, classLong, "value", "J"))
    return "Long.value";
  if (!LOAD_FID(env, FID_Float_value, classFloat, "value", "F"))
    return "Float.value";
  if (!LOAD_FID(env, FID_Double_value, classDouble, "value", "D"))
    return "Double.value";

  fileEncoding = get_system_property(env, "file.encoding");
  if (fileEncoding) {
    fileEncoding = (*env)->NewGlobalRef(env, fileEncoding);
  }

  return NULL;
}

/** Copy value from the given Java object into the given storage buffer.
 * If the value is being extracted from a String or WString, you are
 * responsible for freeing the allocated memory.
 */
void
extract_value(JNIEnv* env, jobject value, void* buffer, size_t size, jboolean promote, const char* encoding) {
  if (value == NULL) {
    *(void **)buffer = NULL;
  }
  else if ((*env)->IsInstanceOf(env, value, classVoid)) {
    // nothing to do
  }
  else if ((*env)->IsInstanceOf(env, value, classBoolean)) {
    jboolean b = (*env)->GetBooleanField(env, value, FID_Boolean_value);
    if (promote) {
      *(ffi_arg*)buffer = b;
    }
    else {
      *(jint*)buffer = b;
    }
  }
  else if ((*env)->IsInstanceOf(env, value, classByte)) {
    jbyte b = (*env)->GetByteField(env, value, FID_Byte_value);
    if (promote) {
      *(ffi_arg*)buffer = b;
    }
    else {
      *(jbyte*)buffer = b;
    }
  }
  else if ((*env)->IsInstanceOf(env, value, classShort)) {
    jshort s = (*env)->GetShortField(env, value, FID_Short_value);
    if (promote) {
      *(ffi_arg*)buffer = s;
    }
    else {
      *(jshort*)buffer = s;
    }
  }
  else if ((*env)->IsInstanceOf(env, value, classCharacter)) {
    jchar c = (*env)->GetCharField(env, value, FID_Character_value);
    if (promote) {
      *(ffi_arg*)buffer = c;
    }
    else {
      *(wchar_t*)buffer = c;
    }
  }
  else if ((*env)->IsInstanceOf(env, value, classInteger)) {
    jint i = (*env)->GetIntField(env, value, FID_Integer_value);
    if (promote) {
      *(ffi_arg*)buffer = i;
    }
    else {
      *(jint*)buffer = i;
    }
  }
  else if ((*env)->IsInstanceOf(env, value, classLong)) {
    *(jlong *)buffer = (*env)->GetLongField(env, value, FID_Long_value);
  }
  else if ((*env)->IsInstanceOf(env, value, classFloat)) {
    *(float *)buffer = (*env)->GetFloatField(env, value, FID_Float_value);
  }
  else if ((*env)->IsInstanceOf(env, value, classDouble)) {
    *(double *)buffer = (*env)->GetDoubleField(env, value, FID_Double_value);
  }
  else if ((*env)->IsInstanceOf(env, value, classStructure)) {
    void* ptr = getStructureAddress(env, value);
    memcpy(buffer, ptr, size);
  }
  else if ((*env)->IsInstanceOf(env, value, classPointer)) {
    *(void **)buffer = getNativeAddress(env, value);
  }
  else if ((*env)->IsInstanceOf(env, value, classString)) {
    *(void **)buffer = newCStringEncoding(env, (jstring)value, encoding);
  }
  else if ((*env)->IsInstanceOf(env, value, classWString)) {
    jstring s = (*env)->CallObjectMethod(env, value, MID_Object_toString);
    *(void **)buffer = newWideCString(env, s);
  }
  else {
    char msg[MSG_SIZE];
    snprintf(msg, sizeof(msg), "Can't convert type to native, native size %d\n", (int)size);
    fprintf(stderr, "JNA: extract_value: %s", msg);
    memset(buffer, 0, size);
    throwByName(env, EError, msg);
  }
}

/** Construct a new Java object from a native value.  */
jobject
new_object(JNIEnv* env, char jtype, void* valuep, jboolean promote, const char* encoding) {
    switch(jtype) {
    case 's':
      return newJavaPointer(env, valuep);
    case 'c':
      return newJavaString(env, *(void**)valuep, encoding);
    case 'w':
      return newJavaString(env, *(void **)valuep, NULL);
    case '*':
      return newJavaPointer(env, *(void**)valuep);
    case 'J':
      return (*env)->NewObject(env, classLong, MID_Long_init,
                               *(jlong *)valuep);
    case 'F':
      return (*env)->NewObject(env, classFloat, MID_Float_init,
                               *(float *)valuep);
    case 'D':
      return (*env)->NewObject(env, classDouble, MID_Double_init,
                               *(double *)valuep);
    case 'Z':
      // Default mapping for boolean is int32_t
      return (*env)->NewObject(env, classBoolean, MID_Boolean_init,
                               (promote
				? (jint)*(ffi_arg*)valuep
				: (*(jint *)valuep)) ? JNI_TRUE : JNI_FALSE);
    case 'B':
      return (*env)->NewObject(env, classByte, MID_Byte_init,
                               promote
			       ? (jbyte)*(ffi_arg*)valuep
			       : (*(jbyte *)valuep));
    case 'C':
      return (*env)->NewObject(env, classCharacter, MID_Character_init,
                               promote
			       ? (jchar)*(ffi_arg*)valuep
			       : (jchar)(*(wchar_t *)valuep));
    case 'S':
      return (*env)->NewObject(env, classShort, MID_Short_init,
                               promote
			       ? (jshort)*(ffi_arg*)valuep
			       : (*(jshort *)valuep));
    case 'I':
      return (*env)->NewObject(env, classInteger, MID_Integer_init,
                               promote
			       ? (jint)*(ffi_arg*)valuep
			       : *(jint *)valuep);
    default:
      return NULL;
    }
}

/** Get the FFI type for the native type which will be converted to the given
    Java class. */
ffi_type*
get_ffi_type(JNIEnv* env, jclass cls, char jtype) {
  switch (jtype) {
  case 'Z':
    return &ffi_type_uint32;
  case 'B':
    return &ffi_type_sint8;
  case 'C':
    return sizeof(wchar_t) == 2 ? &ffi_type_uint16 : &ffi_type_uint32;
  case 'S':
    return &ffi_type_sint16;
  case 'I':
    return &ffi_type_sint32;
  case 'J':
    return &ffi_type_sint64;
  case 'F':
    return &ffi_type_float;
  case 'D':
    return &ffi_type_double;
  case 'V':
    return &ffi_type_void;
  case 's': {
#define PLACEHOLDER_MEMORY ((jlong)0)
    jobject s = (*env)->CallStaticObjectMethod(env, classStructure,
                                               MID_Structure_newInstance, cls, PLACEHOLDER_MEMORY);
    if (s) {
      return getStructureType(env, s);
    }
    return NULL;
  }
  case '*':
  case 'c':
  case 'w':
  default:
    return &ffi_type_pointer;
  }
}

/** Return the FFI type corresponding to the native equivalent of a
    callback function's return value. */
ffi_type*
get_ffi_return_type(JNIEnv* env, jclass cls, char jtype) {
  switch (jtype) {
  case 'Z':
  case 'B':
  case 'C':
  case 'S':
  case 'I':
    /*
     * Always use a return type the size of a cpu register.  This fixes up
     * callbacks on big-endian 64bit machines, and does not break things on
     * i386 or amd64.
     */
    return &ffi_type_slong;
  default:
    return get_ffi_type(env, cls, jtype);
  }
}

typedef struct _method_data {
  ffi_cif cif;
  ffi_cif closure_cif;
  void*   fptr;
  ffi_type** arg_types;
  ffi_type** closure_arg_types;
  int*    flags;
  int     rflag;
  jobject closure_method;
  jobject* to_native;
  jobject  from_native;
  jboolean throw_last_error;
  const char* encoding;
} method_data;

/** Direct invocation glue.  VM vectors to this callback, which in turn calls
    native code
*/
static void
dispatch_direct(ffi_cif* cif, void* volatile resp, void** argp, void *cdata) {
  JNIEnv* env = (JNIEnv*)*(void **)argp[0];
  method_data *data = (method_data*)cdata;

  // ignore first two arguments, which are pointers
  void** volatile args = argp + 2;
  void** volatile objects = NULL;
  release_t* volatile release = NULL;
  void** volatile elems = NULL;
  unsigned i;
  void* oldresp = resp;
  const char* volatile throw_type = NULL;
  const char* volatile throw_msg = NULL;
  char msg[MSG_SIZE];

  if (data->flags) {
    objects = alloca(data->cif.nargs * sizeof(void*));
    memset(objects, 0, data->cif.nargs * sizeof(void*));
    release = alloca(data->cif.nargs * sizeof(release_t));
    memset(release, 0, data->cif.nargs * sizeof(release_t));
    elems = alloca(data->cif.nargs * sizeof(void*));
    for (i=0;i < data->cif.nargs;i++) {
      if (data->flags[i] == CVT_DEFAULT) {
        continue;
      }
      if (data->arg_types[i]->type == FFI_TYPE_POINTER
          && *(void **)args[i] == NULL) {
        continue;
      }
      switch(data->flags[i]) {
      case CVT_INTEGER_TYPE:
        {
          jlong value = getIntegerTypeValue(env, *(void **)args[i]);
          if (cif->arg_types[i+2]->size < data->cif.arg_types[i]->size) {
            args[i] = alloca(data->cif.arg_types[i]->size);
          }
          if (data->cif.arg_types[i]->size > sizeof(ffi_arg)) {
            *(jlong *)args[i] = value;
          }
          else {
            if(IS_BIG_ENDIAN) {
                value = value << ((sizeof(ffi_arg) - data->cif.arg_types[i]->size) * 8);
            }
            *(ffi_arg *)args[i] = (ffi_arg)value;
          }
        }
        break;
      case CVT_POINTER_TYPE:
        *(void **)args[i] = getPointerTypeAddress(env, *(void **)args[i]);
        break;
      case CVT_TYPE_MAPPER:
      case CVT_TYPE_MAPPER_STRING:
      case CVT_TYPE_MAPPER_WSTRING:
        {
          void* valuep = args[i];
          int jtype = get_java_type_from_ffi_type(data->closure_cif.arg_types[i+2]);
          jobject obj = jtype == '*'
            ? *(void **)valuep
            : new_object(env, (char)jtype, valuep, JNI_FALSE, data->encoding);
          if (cif->arg_types[i+2]->size < data->cif.arg_types[i]->size) {
            args[i] = alloca(data->cif.arg_types[i]->size);
          }
          toNativeTypeMapped(env, obj, args[i],
                             data->cif.arg_types[i]->size,
                             data->to_native[i],
                             data->encoding);
        }
        break;
      case CVT_NATIVE_MAPPED:
      case CVT_NATIVE_MAPPED_STRING:
      case CVT_NATIVE_MAPPED_WSTRING:
        toNative(env, *(void **)args[i], args[i], data->cif.arg_types[i]->size, JNI_FALSE, data->encoding);
        break;
      case CVT_POINTER:
        *(void **)args[i] = getNativeAddress(env, *(void **)args[i]);
        break;
      case CVT_JNIENV:
        *(void **)args[i] = (void*)env;
        break;
      case CVT_STRUCTURE:
        objects[i] = *(void **)args[i];
        writeStructure(env, *(void **)args[i]);
        *(void **)args[i] = getStructureAddress(env, *(void **)args[i]);
        break;
      case CVT_STRUCTURE_BYVAL:
        objects[i] = *(void **)args[i];
        writeStructure(env, objects[i]);
        args[i] = getStructureAddress(env, objects[i]);
        break;
      case CVT_STRING:
        *(void **)args[i] = newCStringEncoding(env, (jstring)*(void **)args[i], data->encoding);
        break;
      case CVT_WSTRING:
        {
          jstring s = (*env)->CallObjectMethod(env, *(void **)args[i], MID_Object_toString);
          *(void **)args[i] = newWideCString(env, s);
        }
        break;
      case CVT_CALLBACK:
        *(void **)args[i] = getCallbackAddress(env, *(void **)args[i]);
        break;
#ifndef NO_NIO_BUFFERS
      case CVT_BUFFER:
        {
          void *ptr = getDirectBufferAddress(env, *(void **)args[i]);
          if (ptr != NULL) {
            objects[i] = NULL;
            release[i] = NULL;
          }
          else {
            ptr = getBufferArray(env, *(jobject *)args[i], (jobject *)&objects[i], &elems[i], (void**)&release[i]);
            if (ptr == NULL) {
              throw_type = EIllegalArgument;
              throw_msg = "Buffer arguments must be direct or have a primitive backing array";
              goto cleanup;
            }
          }
          *(void **)args[i] = ptr;
        }
        break;
#endif /* NO_NIO_BUFFERS */
#define ARRAY(Type)                             \
 do { \
   objects[i] = *(void **)args[i];                                      \
   release[i] = (void *)(*env)->Release##Type##ArrayElements;           \
   elems[i] = *(void **)args[i] = (*env)->Get##Type##ArrayElements(env, objects[i], NULL); } while(0)
      case CVT_ARRAY_BOOLEAN: ARRAY(Boolean); break;
      case CVT_ARRAY_BYTE: ARRAY(Byte); break;
      case CVT_ARRAY_SHORT: ARRAY(Short); break;
      case CVT_ARRAY_CHAR: ARRAY(Char); break;
      case CVT_ARRAY_INT: ARRAY(Int); break;
      case CVT_ARRAY_LONG: ARRAY(Long); break;
      case CVT_ARRAY_FLOAT: ARRAY(Float); break;
      case CVT_ARRAY_DOUBLE: ARRAY(Double); break;
      default:
        break;
      }
    }
    if ((*env)->ExceptionCheck(env)) {
      goto cleanup;
    }
  }

  if (data->rflag == CVT_NATIVE_MAPPED) {
    resp = alloca(sizeof(jobject));
  }
  else if (data->rflag == CVT_TYPE_MAPPER) {
    // Ensure enough space for the inner call result, which may differ
    // from the closure result
    resp = alloca(data->cif.rtype->size);
  }
  else if (data->rflag == CVT_STRUCTURE_BYVAL) {
    // In the case of returned structure by value, the inner and
    // outer calls have different return types; we pass the structure memory
    // to the inner call but return a Java object to the outer call.
    resp = alloca(data->cif.rtype->size);
  }

  {
    PSTART();
    if (data->throw_last_error) {
      SET_LAST_ERROR(0);
    }
    ffi_call(&data->cif, FFI_FN(data->fptr), resp, args);
    {
      int err = GET_LAST_ERROR();
      JNA_set_last_error(env, err);
      if (data->throw_last_error && err) {
        char* emsg = STR_ERROR(err);
        snprintf(msg, sizeof(msg), "[%d] %s", err, emsg);
        free(emsg);
        throw_type = ELastError;
        throw_msg = msg;
      }
    }
    PROTECTED_END(do { throw_type=EError;throw_msg="Invalid memory access"; } while(0));
  }

  switch(data->rflag) {
  case CVT_TYPE_MAPPER:
  case CVT_TYPE_MAPPER_STRING:
  case CVT_TYPE_MAPPER_WSTRING:
    {
       int jtype;
       if(data->rflag == CVT_TYPE_MAPPER_STRING) {
           jtype = 'c';
       } else if (data->rflag == CVT_TYPE_MAPPER_WSTRING) {
           jtype = 'w';
       } else {
           jclass returnClass = getNativeTypeMapped(env, data->from_native);
           jtype = get_java_type(env, returnClass);
           if(jtype == -1) {
               jtype = get_java_type_from_ffi_type(data->cif.rtype);
           }
       }
       
       fromNativeTypeMapped(env, data->from_native, resp, jtype, data->cif.rtype->size,
                           data->closure_method, oldresp, data->encoding);
    }
    break;
  case CVT_INTEGER_TYPE:
  case CVT_POINTER_TYPE:
  case CVT_NATIVE_MAPPED:
  case CVT_NATIVE_MAPPED_STRING:
  case CVT_NATIVE_MAPPED_WSTRING:
    *(void **)oldresp = fromNative(env, data->closure_method, data->cif.rtype, resp, JNI_TRUE, data->encoding);
    break;
  case CVT_POINTER:
    *(void **)resp = newJavaPointer(env, *(void **)resp);
    break;
  case CVT_STRING:
    *(void **)resp = newJavaString(env, *(void **)resp, data->encoding);
    break;
  case CVT_WSTRING:
    *(void **)resp = newJavaWString(env, *(void **)resp);
    break;
  case CVT_STRUCTURE:
    {
      jclass return_class = (*env)->CallObjectMethod(env, data->closure_method, MID_Method_getReturnType);
      *(void **)resp = newJavaStructure(env, *(void **)resp, return_class);
    }
    break;
  case CVT_STRUCTURE_BYVAL:
    {
      jclass return_class = (*env)->CallObjectMethod(env, data->closure_method, MID_Method_getReturnType);
      *(void **)oldresp = newJavaStructure(env, resp, return_class);
    }
    break;
  case CVT_CALLBACK:
    {
      jclass return_class = (*env)->CallObjectMethod(env, data->closure_method, MID_Method_getReturnType);
      *(void **)resp = newJavaCallback(env, *(void **)resp, return_class);
    }
    break;
  default:
    break;
  }

  cleanup:
  if (data->flags) {
    for (i=0;i < data->cif.nargs;i++) {
      switch(data->flags[i]) {
      case CVT_STRUCTURE:
        if (objects[i] && !(*env)->ExceptionCheck(env)) {
          (*env)->CallVoidMethod(env, objects[i], MID_Structure_read);
        }
        break;
      case CVT_STRING:
      case CVT_WSTRING:
      case CVT_TYPE_MAPPER_STRING:
      case CVT_TYPE_MAPPER_WSTRING:
      case CVT_NATIVE_MAPPED_STRING:
      case CVT_NATIVE_MAPPED_WSTRING:
        // Free allocated native strings
        free(*(void **)args[i]);
        break;
      case CVT_BUFFER:
      case CVT_ARRAY_BOOLEAN:
      case CVT_ARRAY_BYTE:
      case CVT_ARRAY_SHORT:
      case CVT_ARRAY_CHAR:
      case CVT_ARRAY_INT:
      case CVT_ARRAY_LONG:
      case CVT_ARRAY_FLOAT:
      case CVT_ARRAY_DOUBLE:
        if (*(void **)args[i] && release[i] != NULL) {
          release[i](env, objects[i], elems[i], 0);
        }
        break;
      }
    }
  }

  if (throw_type) {
    throwByName(env, throw_type, throw_msg);
  }
}

static void
closure_handler(ffi_cif* cif, void* resp, void** argp, void *cdata)
{
  callback* cb = (callback *)cdata;
  JavaVM* jvm = cb->vm;
  JNIEnv* env;
  jobject obj;
  int attached = (*jvm)->GetEnv(jvm, (void *)&env, JNI_VERSION_1_4) == JNI_OK;

  if (!attached) {
    if ((*jvm)->AttachCurrentThread(jvm, (void *)&env, NULL) != JNI_OK) {
      fprintf(stderr, "JNA: Can't attach native thread to VM for closure handler\n");
      return;
    }
  }

  // Give the callback its own local frame to ensure all local references
  // are properly disposed
  if ((*env)->PushLocalFrame(env, 16) < 0) {
    fprintf(stderr, "JNA: Out of memory: Can't allocate local frame");
  }
  else {
    obj = (*env)->NewLocalRef(env, cb->object);
    if ((*env)->IsSameObject(env, obj, NULL)) {
      fprintf(stderr, "JNA: callback object has been garbage collected\n");
      if (cif->rtype->type != FFI_TYPE_VOID)
        memset(resp, 0, cif->rtype->size);
    }
    else {
      (*env)->CallVoidMethod(env, obj, MID_ffi_callback_invoke,
                             A2L(cif), A2L(resp), A2L(argp));
    }

    (*env)->PopLocalFrame(env, NULL);
  }

  if (!attached) {
    if ((*jvm)->DetachCurrentThread(jvm) != 0) {
      fprintf(stderr, "JNA: could not detach thread after callback handling\n");
    }
  }
}

////////////////////
// API Methods
////////////////////

/*
 * Class:     com_sun_jna_Native
 * Method:    invokePointer
 * Signature: (Lcom/sun/jna/Function;JI[Ljava/lang/Object;)J
 */
JNIEXPORT jlong JNICALL 
Java_com_sun_jna_Native_invokePointer (JNIEnv *env, jclass UNUSED(cls),
                                       jobject UNUSED(function), jlong fp,
                                       jint callconv, jobjectArray arr)
{
    jvalue result;
    dispatch(env, L2A(fp), callconv, arr, &ffi_type_pointer, &result);
    return A2L(result.l);
}


/*
 * Class:     com_sun_jna_Native
 * Method:    invokeObject
 * Signature: (Lcom/sun/jna/Function;JI[Ljava/lang/Object;)Ljava/lang/Object;
 */
JNIEXPORT jobject 
JNICALL Java_com_sun_jna_Native_invokeObject(JNIEnv *env, jclass UNUSED(cls),
                                             jobject UNUSED(function), jlong fp,
                                             jint callconv, jobjectArray arr)
{
    jvalue result;
    dispatch(env, L2A(fp), callconv, arr, &ffi_type_pointer, &result);
    return result.l;
}


/*
 * Class:     com_sun_jna_Native
 * Method:    invokeStructure
 * Signature: (Lcom/sun/jna/Function;JI[Ljava/lang/Object;JJ)V
 */
JNIEXPORT void JNICALL 
Java_com_sun_jna_Native_invokeStructure(JNIEnv *env, jclass UNUSED(cls), 
                                        jobject UNUSED(function), jlong fp,
                                        jint callconv, jobjectArray arr,
                                        jlong memory, jlong type_info)
{
  ffi_type* rtype = (ffi_type*)L2A(type_info);
  if (!rtype) {
    throwByName(env, EIllegalState, "Return structure type info not initialized");
  }
  else {
    dispatch(env, L2A(fp), callconv, arr, rtype, L2A(memory));
  }
}

/*
 * Class:     com_sun_jna_Native
 * Method:    invokeDouble
 * Signature: (Lcom/sun/jna/Function;JI[Ljava/lang/Object;)D
 */
JNIEXPORT jdouble JNICALL
Java_com_sun_jna_Native_invokeDouble(JNIEnv *env, jclass UNUSED(cls), 
                                     jobject UNUSED(function), jlong fp, 
                                     jint callconv, jobjectArray arr)
{
    jvalue result;
    dispatch(env, L2A(fp), callconv, arr, &ffi_type_double, &result);
    return result.d;
}

/*
 * Class:     com_sun_jna_Native
 * Method:    invokeFloat
 * Signature: (Lcom/sun/jna/Function;JI[Ljava/lang/Object;)F
 */
JNIEXPORT jfloat JNICALL
Java_com_sun_jna_Native_invokeFloat(JNIEnv *env, jclass UNUSED(cls), 
                                    jobject UNUSED(function), jlong fp,
                                    jint callconv, jobjectArray arr)
{
    jvalue result;
    dispatch(env, L2A(fp), callconv, arr, &ffi_type_float, &result);
    return result.f;
}

/*
 * Class:     com_sun_jna_Native
 * Method:    invokeInt
 * Signature: (Lcom/sun/jna/Function;JI[Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_jna_Native_invokeInt(JNIEnv *env, jclass UNUSED(cls), 
                                  jobject UNUSED(function), jlong fp, jint callconv,
                                  jobjectArray arr)
{
    ffi_arg result;
    dispatch(env, L2A(fp), callconv, arr, &ffi_type_sint32, &result);
    return (jint)result;
}

/*
 * Class:     com_sun_jna_Native
 * Method:    invokeLong
 * Signature: (Lcom/sun/jna/Function;JI[Ljava/lang/Object;)J
 */
JNIEXPORT jlong JNICALL
Java_com_sun_jna_Native_invokeLong(JNIEnv *env, jclass UNUSED(cls),
                                   jobject UNUSED(function), jlong fp, jint callconv,
                                   jobjectArray arr)
{
    jvalue result;
    dispatch(env, L2A(fp), callconv, arr, &ffi_type_sint64, &result);
    return result.j;
}

/*
 * Class:     com_sun_jna_Native
 * Method:    invokeVoid
 * Signature: (Lcom/sun/jna/Function;JI[Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL
Java_com_sun_jna_Native_invokeVoid(JNIEnv *env, jclass UNUSED(cls),
                                   jobject UNUSED(function), jlong fp, jint callconv,
                                   jobjectArray arr)
{
    jvalue result;
    dispatch(env, L2A(fp), callconv, arr, &ffi_type_void, &result);
}

JNIEXPORT jlong JNICALL
Java_com_sun_jna_Native_createNativeCallback(JNIEnv *env,
                                             jclass UNUSED(cls),
                                             jobject obj,
                                             jobject method,
                                             jobjectArray arg_types,
                                             jclass return_type,
                                             jint call_conv,
                                             jint options,
                                             jstring encoding) {
  callback* cb =
    create_callback(env, obj, method, arg_types, return_type,
                    call_conv, options, encoding);

  return A2L(cb);
}

JNIEXPORT void JNICALL
Java_com_sun_jna_Native_freeNativeCallback(JNIEnv *env,
                                           jclass UNUSED(cls),
                                           jlong ptr) {
  free_callback(env, (callback*)L2A(ptr));
}

/*
 * Class:     Native
 * Method:    open
 * Signature: (Ljava/lang/String;I)J
 */
JNIEXPORT jlong JNICALL
Java_com_sun_jna_Native_open(JNIEnv *env, jclass UNUSED(cls), jstring lib, jint flags){
    /* dlopen on Unix allows NULL to mean "current process" */
    const STRTYPE libname = NULL;
    void *handle = NULL;

    if (flags == -1) {
      flags = DEFAULT_LOAD_OPTS;
    }

    if (lib != NULL) {
      if ((libname = NAME2CSTR(env, lib)) == NULL) {
        return A2L(NULL);
      }
    }

    handle = (void *)LOAD_LIBRARY(libname, flags);
#if defined(_WIN32)
    /** Reattempt lookup using the short name version */
    if (!handle) {
      const STRTYPE short_libname = NULL;
      if ((short_libname = w32_short_name(env, lib)) != NULL) {
        free((void *)libname);
        libname = short_libname;
        handle = (void *)LOAD_LIBRARY(libname, flags);
      }
    }
#endif
    if (!handle) {
      char* buf = LOAD_ERROR();
      throwByName(env, EUnsatisfiedLink, buf);
      free(buf);
    }
    if (libname != NULL) {
      free((void *)libname);
    }
    return A2L(handle);
}

/*
 * Class:     Native
 * Method:    close
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_com_sun_jna_Native_close(JNIEnv *env, jclass UNUSED(cls), jlong handle)
{
  if (FREE_LIBRARY(L2A(handle))) {
    char* buf = LOAD_ERROR();
    throwByName(env, EError, buf);
    free(buf);
  }
}

/*
 * Class:     Native
 * Method:    findSymbol
 * Signature: (JLjava/lang/String;)J
 */
JNIEXPORT jlong JNICALL
Java_com_sun_jna_Native_findSymbol(JNIEnv *env, jclass UNUSED(cls),
                                   jlong libHandle, jstring fun) {

    void *handle = L2A(libHandle);
    void *func = NULL;
    const char* funname = newCString(env, fun);

    if (funname != NULL) {
      func = (void *)FIND_ENTRY(handle, funname);
      if (!func) {
        char* buf = LOAD_ERROR();
        throwByName(env, EUnsatisfiedLink, buf);
        free(buf);
      }
      free((void *)funname);
    }
    return A2L(func);
}

/*
 * Class:     com_sun_jna_Native
 * Method:    write
 * Signature: (Lcom/sun/jna/Pointer;JJ[BII)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_write__Lcom_sun_jna_Pointer_2JJ_3BII
(JNIEnv *env, jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jbyteArray arr, jint off, jint n)
{
  PSTART();
  (*env)->GetByteArrayRegion(env, arr, off, n, L2A(addr + offset));
  PEND(env);
}

/*
 * Class:     com_sun_jna_Native
 * Method:    write
 * Signature: (Lcom/sun/jna/Pointer;JJ[CII)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_write__Lcom_sun_jna_Pointer_2JJ_3CII
(JNIEnv *env, jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jcharArray arr, jint off, jint n)
{
  getChars(env, (wchar_t*)L2A(addr + offset), arr, off, n);
}

/*
 * Class:     com_sun_jna_Native
 * Method:    write
 * Signature: (Lcom/sun/jna/Pointer;JJ[DII)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_write__Lcom_sun_jna_Pointer_2JJ_3DII
(JNIEnv *env, jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jdoubleArray arr, jint off, jint n)
{
  PSTART();
  (*env)->GetDoubleArrayRegion(env, arr, off, n, (jdouble*)L2A(addr + offset));
  PEND(env);
}

/*
 * Class:     com_sun_jna_Native
 * Method:    write
 * Signature: (Lcom/sun/jna/Pointer;JJ[FII)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_write__Lcom_sun_jna_Pointer_2JJ_3FII
(JNIEnv *env, jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jfloatArray arr, jint off, jint n)
{
  PSTART();
  (*env)->GetFloatArrayRegion(env, arr, off, n, (jfloat*)L2A(addr + offset));
  PEND(env);
}

/*
 * Class:     com_sun_jna_Native
 * Method:    write
 * Signature: (Lcom/sun/jna/Pointer;JJ[III)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_write__Lcom_sun_jna_Pointer_2JJ_3III
(JNIEnv *env, jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jintArray arr, jint off, jint n)
{
  PSTART();
  (*env)->GetIntArrayRegion(env, arr, off, n, (jint*)L2A(addr + offset));
  PEND(env);
}

/*
 * Class:     com_sun_jna_Native
 * Method:    write
 * Signature: (Lcom/sun/jna/Pointer;JJ[JII)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_write__Lcom_sun_jna_Pointer_2JJ_3JII
(JNIEnv *env, jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jlongArray arr, jint off, jint n)
{
  PSTART();
  (*env)->GetLongArrayRegion(env, arr, off, n, (jlong*)L2A(addr + offset));
  PEND(env);
}

/*
 * Class:     com_sun_jna_Native
 * Method:    write
 * Signature: (Lcom/sun/jna/Pointer;JJ[SII)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_write__Lcom_sun_jna_Pointer_2JJ_3SII
(JNIEnv *env, jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jshortArray arr, jint off, jint n)
{
  PSTART();
  (*env)->GetShortArrayRegion(env, arr, off, n, (jshort*)L2A(addr + offset));
  PEND(env);
}

/*
 * Class:     com_sun_jna_Native
 * Method:    indexOf
 * Signature: (Lcom/sun/jna/Pointer;JJB)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_jna_Native_indexOf
(JNIEnv * UNUSED_ENV(env), jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jbyte value)
{
  jbyte *peer = (jbyte *)L2A(addr + offset);
  volatile jlong i = 0;
  volatile jlong result = -1L;
  PSTART();
  while (i >= 0 && result == -1L) {
    if (peer[i] == value)
      result = i;
    ++i;
  }
  PEND(env);

  return result;
}

/*
 * Class:     com_sun_jna_Native
 * Method:    read
 * Signature: (Lcom/sun/jna/Pointer;JJ[BII)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_read__Lcom_sun_jna_Pointer_2JJ_3BII
(JNIEnv *env, jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jbyteArray arr, jint off, jint n)
{
  PSTART();
  (*env)->SetByteArrayRegion(env, arr, off, n, L2A(addr + offset));
  PEND(env);
}

/*
 * Class:     com_sun_jna_Native
 * Method:    read
 * Signature: (Lcom/sun/jna/Pointer;JJ[CII)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_read__Lcom_sun_jna_Pointer_2JJ_3CII
    (JNIEnv *env, jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jcharArray arr, jint off, jint n)
{
  setChars(env, (wchar_t*)L2A(addr + offset), arr, off, n);
}

/*
 * Class:     com_sun_jna_Native
 * Method:    read
 * Signature: (Lcom/sun/jna/Pointer;JJ[DII)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_read__Lcom_sun_jna_Pointer_2JJ_3DII
    (JNIEnv *env, jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jdoubleArray arr, jint off, jint n)
{
  PSTART();
  (*env)->SetDoubleArrayRegion(env, arr, off, n, (jdouble*)L2A(addr + offset));
  PEND(env);
}

/*
 * Class:     com_sun_jna_Native
 * Method:    read
 * Signature: (Lcom/sun/jna/Pointer;JJ[FII)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_read__Lcom_sun_jna_Pointer_2JJ_3FII
    (JNIEnv *env, jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jfloatArray arr, jint off, jint n)
{
  PSTART();
  (*env)->SetFloatArrayRegion(env, arr, off, n, (jfloat*)L2A(addr + offset));
  PEND(env);
}

/*
 * Class:     com_sun_jna_Native
 * Method:    read
 * Signature: (Lcom/sun/jna/Pointer;JJ[III)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_read__Lcom_sun_jna_Pointer_2JJ_3III
    (JNIEnv *env, jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jintArray arr, jint off, jint n)
{
  PSTART();
  (*env)->SetIntArrayRegion(env, arr, off, n, (jint*)L2A(addr + offset));
  PEND(env);
}

/*
 * Class:     com_sun_jna_Native
 * Method:    read
 * Signature: (Lcom/sun/jna/Pointer;JJ[JII)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_read__Lcom_sun_jna_Pointer_2JJ_3JII
    (JNIEnv *env, jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jlongArray arr, jint off, jint n)
{
  PSTART();
  (*env)->SetLongArrayRegion(env, arr, off, n, (jlong*)L2A(addr + offset));
  PEND(env);
}

/*
 * Class:     com_sun_jna_Native
 * Method:    read
 * Signature: (Lcom/sun/jna/Pointer;JJ[SII)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_read__Lcom_sun_jna_Pointer_2JJ_3SII
    (JNIEnv *env, jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jshortArray arr, jint off, jint n)
{
  PSTART();
  (*env)->SetShortArrayRegion(env, arr, off, n, (jshort*)L2A(addr + offset));
  PEND(env);
}

/*
 * Class:     com_sun_jna_Native
 * Method:    getByte
 * Signature: (Lcom/sun/jna/Pointer;JJ)B
 */
JNIEXPORT jbyte JNICALL Java_com_sun_jna_Native_getByte
(JNIEnv * UNUSED_ENV(env), jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset)
{
    jbyte res = 0;
    MEMCPY(env, &res, L2A(addr + offset), sizeof(res));
    return res;
}

/*
 * Class:     com_sun_jna_Native
 * Method:    getChar
 * Signature: (Lcom/sun/jna/Pointer;JJ)C
 */
JNIEXPORT jchar JNICALL Java_com_sun_jna_Native_getChar
(JNIEnv * UNUSED_ENV(env), jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset)
{
    wchar_t res = 0;
    MEMCPY(env, &res, L2A(addr + offset), sizeof(res));
    return (jchar)res;
}

/*
 * Class:     com_sun_jna_Native
 * Method:    _getPointer
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_jna_Native__1getPointer
(JNIEnv *UNUSED_ENV(env), jclass UNUSED(cls), jlong addr)
{
    void *ptr = NULL;
    MEMCPY(env, &ptr, L2A(addr), sizeof(ptr));
    return A2L(ptr);
}

/*
 * Class:     com_sun_jna_Native
 * Method:    getDirectByteBuffer
 * Signature: (Lcom/sun/jna/Pointer;JJJ)Ljava/nio/ByteBuffer;
 */
JNIEXPORT jobject JNICALL Java_com_sun_jna_Native_getDirectByteBuffer__Lcom_sun_jna_Pointer_2JJJ
    (JNIEnv *env, jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jlong length)
{
#ifdef NO_NIO_BUFFERS
    return NULL;
#else
    return (*env)->NewDirectByteBuffer(env, L2A(addr + offset), length);
#endif
}

/*
 * Class:     com_sun_jna_Native
 * Method:    getDouble
 * Signature: (Lcom/sun/jna/Pointer;JJ)D
 */
JNIEXPORT jdouble JNICALL Java_com_sun_jna_Native_getDouble
(JNIEnv * UNUSED_ENV(env), jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset)
{
    jdouble res = 0;
    MEMCPY(env, &res, L2A(addr + offset), sizeof(res));
    return res;
}

/*
 * Class:     com_sun_jna_Native
 * Method:    getFloat
 * Signature: (Lcom/sun/jna/Pointer;JJ)F
 */
JNIEXPORT jfloat JNICALL Java_com_sun_jna_Native_getFloat
(JNIEnv * UNUSED_ENV(env), jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset)
{
    jfloat res = 0;
    MEMCPY(env, &res, L2A(addr + offset), sizeof(res));
    return res;
}

/*
 * Class:     com_sun_jna_Native
 * Method:    getInt
 * Signature: (Lcom/sun/jna/Pointer;JJ)I
 */
JNIEXPORT jint JNICALL Java_com_sun_jna_Native_getInt
(JNIEnv * UNUSED_ENV(env), jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset)
{
    jint res = 0;
    MEMCPY(env, &res, L2A(addr + offset), sizeof(res));
    return res;
}

/*
 * Class:     com_sun_jna_Native
 * Method:    getLong
 * Signature: (Lcom/sun/jna/Pointer;JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_jna_Native_getLong
(JNIEnv * UNUSED_ENV(env), jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset)
{
    jlong res = 0;
    MEMCPY(env, &res, L2A(addr + offset), sizeof(res));
    return res;
}

/*
 * Class:     com_sun_jna_Native
 * Method:    getShort
 * Signature: (Lcom/sun/jna/Pointer;JJ)S
 */
JNIEXPORT jshort JNICALL Java_com_sun_jna_Native_getShort
(JNIEnv * UNUSED_ENV(env), jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset)
{
    jshort res = 0;
    MEMCPY(env, &res, L2A(addr + offset), sizeof(res));
    return res;
}

/*
 * Class:     com_sun_jna_Native
 * Method:    getWideString
 * Signature: (Lcom/sun/jna/Pointer;JJ)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_jna_Native_getWideString
(JNIEnv *env, jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset)
{
  return newJavaString(env, L2A(addr + offset), NULL);
}

/*
 * Class:     com_sun_jna_Native
 * Method:    getStringBytes
 * Signature: (Lcom/sun/jna/Pointer;JJ)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_sun_jna_Native_getStringBytes
(JNIEnv *env, jclass UNUSED(cls), jobject UNUSED(pointer), jlong baseaddr, jlong offset)
{
  volatile jbyteArray bytes = 0;
  PSTART();
  {
    void* addr = L2A(baseaddr + offset);
    int len = (int)strlen(addr);
    bytes = (*env)->NewByteArray(env, len);
    if (bytes != 0) {
      (*env)->SetByteArrayRegion(env, bytes, 0, len, (jbyte *)addr);
    }
    else {
      throwByName(env, EOutOfMemory, "Can't allocate byte array");
    }
  }
  PEND(env);
  return bytes;
}

/*
 * Class:     com_sun_jna_Native
 * Method:    setMemory
 * Signature: (Lcom/sun/jna/Pointer;JJJB)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_setMemory
(JNIEnv *UNUSED_ENV(env), jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jlong count, jbyte value)
{
  MEMSET(env, L2A(addr + offset), (int)value, (size_t)count);
}

/*
 * Class:     com_sun_jna_Native
 * Method:    setByte
 * Signature: (Lcom/sun/jna/Pointer;JJB)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_setByte
(JNIEnv * UNUSED_ENV(env), jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jbyte value)
{
  MEMCPY(env, L2A(addr  + offset), &value, sizeof(value));
}

/*
 * Class:     com_sun_jna_Native
 * Method:    setChar
 * Signature: (Lcom/sun/jna/Pointer;JJC)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_setChar
(JNIEnv * UNUSED_ENV(env), jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jchar value)
{
  wchar_t ch = value;
  MEMCPY(env, L2A(addr + offset), &ch, sizeof(ch));
}

/*
 * Class:     com_sun_jna_Native
 * Method:    setPointer
 * Signature: (Lcom/sun/jna/Pointer;JJJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_setPointer
(JNIEnv * UNUSED_ENV(env), jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jlong value)
{
  void *ptr = L2A(value);
  MEMCPY(env, L2A(addr + offset), &ptr, sizeof(void *));
}

/*
 * Class:     com_sun_jna_Native
 * Method:    setDouble
 * Signature: (Lcom/sun/jna/Pointer;JJD)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_setDouble
(JNIEnv * UNUSED_ENV(env), jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jdouble value)
{
  MEMCPY(env, L2A(addr + offset), &value, sizeof(value));
}

/*
 * Class:     com_sun_jna_Native
 * Method:    setFloat
 * Signature: (Lcom/sun/jna/Pointer;JJF)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_setFloat
(JNIEnv * UNUSED_ENV(env), jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jfloat value)
{
  MEMCPY(env, L2A(addr + offset), &value, sizeof(value));
}

/*
 * Class:     com_sun_jna_Native
 * Method:    setInt
 * Signature: (Lcom/sun/jna/Pointer;JJI)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_setInt
(JNIEnv * UNUSED_ENV(env), jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jint value)
{
  MEMCPY(env, L2A(addr + offset), &value, sizeof(value));
}

/*
 * Class:     com_sun_jna_Native
 * Method:    setLong
 * Signature: (Lcom/sun/jna/Pointer;JJJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_setLong
(JNIEnv * UNUSED_ENV(env), jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jlong value)
{
  MEMCPY(env, L2A(addr + offset), &value, sizeof(value));
}

/*
 * Class:     com_sun_jna_Native
 * Method:    setShort
 * Signature: (Lcom/sun/jna/Pointer;JJS)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_setShort
(JNIEnv * UNUSED_ENV(env), jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jshort value)
{
  MEMCPY(env, L2A(addr + offset), &value, sizeof(value));
}

/*
 * Class:     com_sun_jna_Native
 * Method:    setWideString
 * Signature: (Lcom/sun/jna/Pointer;JJLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_setWideString
(JNIEnv *env, jclass UNUSED(cls), jobject UNUSED(pointer), jlong addr, jlong offset, jstring value)
{
    int len = (*env)->GetStringLength(env, value);
    const void* volatile str;
    volatile int size = (len + 1) * sizeof(wchar_t);

    str = newWideCString(env, value);
    if (str != NULL) {
      MEMCPY(env, L2A(addr + offset), str, size);
      free((void*)str);
    }
}

/*
 * Class:     Native
 * Method:    malloc
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_jna_Native_malloc
(JNIEnv *UNUSED(env), jclass UNUSED(cls), jlong size)
{
    return A2L(malloc((size_t)size));
}

/*
 * Class:     Native
 * Method:    free
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_jna_Native_free
(JNIEnv *UNUSED(env), jclass UNUSED(cls), jlong ptr)
{
    free(L2A(ptr));
}


/*
 * Class:     Native
 * Method:    sizeof
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL
Java_com_sun_jna_Native_sizeof(JNIEnv *env, jclass UNUSED(cls), jint type)
{
  switch(type) {
  case com_sun_jna_Native_TYPE_VOIDP: return sizeof(void*);
  case com_sun_jna_Native_TYPE_LONG: return sizeof(long);
  case com_sun_jna_Native_TYPE_WCHAR_T: return sizeof(wchar_t);
  case com_sun_jna_Native_TYPE_SIZE_T: return sizeof(size_t);
  case com_sun_jna_Native_TYPE_BOOL: return sizeof(bool);
  case com_sun_jna_Native_TYPE_LONG_DOUBLE: return sizeof(long double);
  default:
    {
      char msg[MSG_SIZE];
      snprintf(msg, sizeof(msg), "Invalid sizeof type %d", (int)type);
      throwByName(env, EIllegalArgument, msg);
      return -1;
    }
  }
}

/** Initialize com.sun.jna classes separately from the library load to
 * avoid initialization inconsistencies.
 */
JNIEXPORT void JNICALL
Java_com_sun_jna_Native_initIDs(JNIEnv *env, jclass cls) {
  if (!LOAD_CREF(env, Pointer, "com/sun/jna/Pointer")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain class com.sun.jna.Pointer");
  }
  else if (!LOAD_MID(env, MID_Pointer_init, classPointer,
                     "<init>", "(J)V")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain constructor for class com.sun.jna.Pointer");
  }
  else if (!LOAD_FID(env, FID_Pointer_peer, classPointer, "peer", "J")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain peer field ID for class com.sun.jna.Pointer");
  }
  else if (!(classNative = (*env)->NewWeakGlobalRef(env, cls))) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain global reference for class com.sun.jna.Native");
  }
  else if (!(MID_Native_dispose
             = (*env)->GetStaticMethodID(env, classNative,
                                         "dispose", "()V"))) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain static method dispose from class com.sun.jna.Native");
  }
  else if (!(MID_Native_fromNativeCallbackParam
             = (*env)->GetStaticMethodID(env, classNative,
                                         "fromNative", "(Ljava/lang/Class;Ljava/lang/Object;)Lcom/sun/jna/NativeMapped;"))) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain static method fromNative(Class, Object) from class com.sun.jna.Native");
  }
  else if (!(MID_Native_fromNative
             = (*env)->GetStaticMethodID(env, classNative,
                                         "fromNative", "(Ljava/lang/reflect/Method;Ljava/lang/Object;)Lcom/sun/jna/NativeMapped;"))) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain static method fromNative(Method, Object) from class com.sun.jna.Native");
  }
  else if (!(MID_Native_nativeType
             = (*env)->GetStaticMethodID(env, classNative,
                                         "nativeType", "(Ljava/lang/Class;)Ljava/lang/Class;"))) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain static method nativeType from class com.sun.jna.Native");
  }
  else if (!(MID_Native_toNativeTypeMapped
             = (*env)->GetStaticMethodID(env, classNative,
                                         "toNative", "(Lcom/sun/jna/ToNativeConverter;Ljava/lang/Object;)Ljava/lang/Object;"))) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain static method toNative from class com.sun.jna.Native");
  }
  else if (!(MID_Native_fromNativeTypeMapped
             = (*env)->GetStaticMethodID(env, classNative,
                                         "fromNative", "(Lcom/sun/jna/FromNativeConverter;Ljava/lang/Object;Ljava/lang/reflect/Method;)Ljava/lang/Object;"))) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain static method fromNative(FromNativeConverter, Object, Method) from class com.sun.jna.Native");
  }
  else if (!LOAD_CREF(env, Structure, "com/sun/jna/Structure")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain class com.sun.jna.Structure");
  }
  else if (!LOAD_MID(env, MID_Structure_getTypeInfo, classStructure,
                     "getTypeInfo", "()Lcom/sun/jna/Pointer;")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain getTypeInfo method for class com.sun.jna.Structure");
  }
  else if (!(MID_Structure_newInstance
             = (*env)->GetStaticMethodID(env, classStructure,
                                         "newInstance", "(Ljava/lang/Class;J)Lcom/sun/jna/Structure;"))) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain static newInstance method for class com.sun.jna.Structure");
  }
  else if (!LOAD_MID(env, MID_Structure_read, classStructure,
                     "autoRead", "()V")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain read method for class com.sun.jna.Structure");
  }
  else if (!LOAD_MID(env, MID_Structure_write, classStructure,
                     "autoWrite", "()V")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain write method for class com.sun.jna.Structure");
  }
  else if (!LOAD_FID(env, FID_Structure_memory, classStructure, "memory", "Lcom/sun/jna/Pointer;")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain memory field ID for class com.sun.jna.Structure");
  }
  else if (!LOAD_FID(env, FID_Structure_typeInfo, classStructure, "typeInfo", "J")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain typeInfo field ID for class com.sun.jna.Structure");
  }
  else if (!LOAD_CREF(env, StructureByValue, "com/sun/jna/Structure$ByValue")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain class com.sun.jna.Structure.ByValue");
  }
  else if (!LOAD_CREF(env, Callback, "com/sun/jna/Callback")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain class com.sun.jna.Callback");
  }
  else if (!LOAD_CREF(env, AttachOptions, "com/sun/jna/CallbackReference$AttachOptions")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain class com.sun.jna.CallbackReference.AttachOptions");
  }
  else if (!LOAD_CREF(env, CallbackReference, "com/sun/jna/CallbackReference")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain class com.sun.jna.CallbackReference");
  }
  else if (!(MID_CallbackReference_getCallback
             = (*env)->GetStaticMethodID(env, classCallbackReference,
                                         "getCallback", "(Ljava/lang/Class;Lcom/sun/jna/Pointer;Z)Lcom/sun/jna/Callback;"))) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain static method getCallback from class com.sun.jna.CallbackReference");
  }
  else if (!(MID_CallbackReference_getFunctionPointer
             = (*env)->GetStaticMethodID(env, classCallbackReference,
                                         "getFunctionPointer", "(Lcom/sun/jna/Callback;Z)Lcom/sun/jna/Pointer;"))) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain static method getFunctionPointer from class com.sun.jna.CallbackReference");
  }
  else if (!(MID_CallbackReference_getNativeString
             = (*env)->GetStaticMethodID(env, classCallbackReference,
                                         "getNativeString", "(Ljava/lang/Object;Z)Lcom/sun/jna/Pointer;"))) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain static method getNativeString from class com.sun.jna.CallbackReference");
  }
  else if (!(MID_CallbackReference_initializeThread
             = (*env)->GetStaticMethodID(env, classCallbackReference,
                                         "initializeThread", "(Lcom/sun/jna/Callback;Lcom/sun/jna/CallbackReference$AttachOptions;)Ljava/lang/ThreadGroup;"))) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain static method initializeThread from class com.sun.jna.CallbackReference");
  }
  else if (!LOAD_CREF(env, WString, "com/sun/jna/WString")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain class com.sun.jna.WString");
  }
  else if (!LOAD_CREF(env, NativeMapped, "com/sun/jna/NativeMapped")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain class com.sun.jna.NativeMapped");
  }
  else if (!LOAD_MID(env, MID_NativeMapped_toNative, classNativeMapped,
                     "toNative", "()Ljava/lang/Object;")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain toNative method for class com.sun.jna.NativeMapped");
  }
  else if (!LOAD_CREF(env, IntegerType, "com/sun/jna/IntegerType")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain class com.sun.jna.IntegerType");
  }
  else if (!LOAD_FID(env, FID_IntegerType_value, classIntegerType, "value", "J")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain value field ID for class com.sun.jna.IntegerType");
  }
  else if (!LOAD_CREF(env, PointerType, "com/sun/jna/PointerType")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain class com.sun.jna.PointerType");
  }
  else if (!LOAD_FID(env, FID_PointerType_pointer, classPointerType, "pointer", "Lcom/sun/jna/Pointer;")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain typeInfo field ID for class com.sun.jna.Structure");
  }
  else if (!LOAD_MID(env, MID_WString_init, classWString,
                     "<init>", "(Ljava/lang/String;)V")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain constructor for class com.sun.jna.WString");
  }
  else if (!LOAD_CREF(env, JNIEnv, "com/sun/jna/JNIEnv")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain class com.sun.jna.JNIEnv");
  }
  else if (!LOAD_CREF(env, _ffi_callback, "com/sun/jna/Native$ffi_callback")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain class com.sun.jna.Native$ffi_callback");
  }
  else if (!LOAD_MID(env, MID_ffi_callback_invoke, class_ffi_callback,
                     "invoke", "(JJJ)V")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain invoke method from class com.sun.jna.Native$ffi_callback");
  }
  else if (!LOAD_CREF(env, FromNativeConverter, "com/sun/jna/FromNativeConverter")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain class com.sun.jna.FromNativeConverter");
  }
  else if (!LOAD_MID(env, MID_FromNativeConverter_nativeType, classFromNativeConverter, 
                "nativeType", "()Ljava/lang/Class;")) {
    throwByName(env, EUnsatisfiedLink,
                "Can't obtain method nativeType for class com.sun.jna.FromNativeConverter");
  }
  // Initialize type fields within Structure.FFIType
  else {
#define CFFITYPE "com/sun/jna/Structure$FFIType$FFITypes"
    jclass cls = (*env)->FindClass(env, CFFITYPE);
    jfieldID fid;
    unsigned i;
    const char* fields[] = {
      "void",
      "float", "double", "longdouble",
      "uint8", "sint8", "uint16", "sint16",
      "uint32", "sint32", "uint64", "sint64",
      "pointer",
    };
    ffi_type* types[] = {
      &ffi_type_void,
      &ffi_type_float, &ffi_type_double, &ffi_type_longdouble,
      &ffi_type_uint8, &ffi_type_sint8, &ffi_type_uint16, &ffi_type_sint16,
      &ffi_type_uint32, &ffi_type_sint32, &ffi_type_uint64, &ffi_type_sint64,
      &ffi_type_pointer,
    };
    char field[32];
    if (!cls) {
      throwByName(env, EUnsatisfiedLink, "Structure$FFIType missing");
      return;
    }
    for (i=0;i < sizeof(fields)/sizeof(fields[0]);i++) {
      snprintf(field, sizeof(field), "ffi_type_%s", fields[i]);
      fid = (*env)->GetStaticFieldID(env, cls, field, "Lcom/sun/jna/Pointer;");
      if (!fid) {
        throwByName(env, EUnsatisfiedLink, field);
        return;
      }
      (*env)->SetStaticObjectField(env, cls, fid, newJavaPointer(env, types[i]));
    }
    short checkValue = 0x0001;
    IS_BIG_ENDIAN = ((char *) &checkValue)[0] == 1 ? 0 : 1;
  }
}

#ifndef NO_JAWT
    #if !defined(__APPLE__)
        #define JAWT_HEADLESS_HACK
        #ifdef _WIN32
            #if defined(_WIN64)
                #define METHOD_NAME "JAWT_GetAWT"
            #else
                #define METHOD_NAME "_JAWT_GetAWT@8"
            #endif
        #else
            #define METHOD_NAME "JAWT_GetAWT"
        #endif

        static void* jawt_handle = NULL;
        static jboolean (JNICALL *pJAWT_GetAWT)(JNIEnv*,JAWT*);

        #define JAWT_GetAWT (*pJAWT_GetAWT)
    #endif
#endif /* NO_JAWT */

JNIEXPORT jlong JNICALL
Java_com_sun_jna_Native_getWindowHandle0(JNIEnv* UNUSED_JAWT(env), jclass UNUSED(classp), jobject UNUSED_JAWT(w)) {
  jlong handle = 0;
#ifndef NO_JAWT
  JAWT_DrawingSurface* ds;
  JAWT_DrawingSurfaceInfo* dsi;
  jint lock;
  JAWT awt;

  // NOTE: AWT/JAWT must be loaded prior to this code's execution
  // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6539705
  awt.version = JAWT_VERSION_1_4;
#ifdef JAWT_HEADLESS_HACK
  // Java versions 1.5/1.6 throw UnsatisfiedLinkError when run headless
  // Avoid the issue by dynamic linking
  if (!pJAWT_GetAWT) {
#ifdef _WIN32
    // Windows needs the full path to JAWT; calling System.loadLibrary("jawt")
    // from Java adds it to the path so that a simple LoadLibrary("jawt.dll")
    // works, but may cause other attempts to load that library from Java to
    // to get an UnsatisfiedLinkError, reporting that the library is already
    // loaded in a different class loader, since there is no way to force the
    // JAWT library by the system class loader.
    // Use Unicode strings in case the path to the library includes non-ASCII
    // characters.
    wchar_t* path = L"jawt.dll";
    jstring jprop = get_system_property(env, "java.home");
    if (jprop != NULL) {
      const wchar_t* prop = newWideCString(env, jprop);
      const wchar_t* suffix = L"/bin/jawt.dll";
      size_t len = wcslen(prop) + wcslen(suffix) + 1;
      path = (wchar_t*)alloca(len * sizeof(wchar_t));

      swprintf(path, len, L"%s%s", prop, suffix);

      free((void *)prop);
    }
    if ((jawt_handle = LOAD_LIBRARY(path, DEFAULT_LOAD_OPTS)) == NULL) {
      char* msg = LOAD_ERROR();
      throwByName(env, EUnsatisfiedLink, msg);
      free(msg);
      return -1;
    }
#else
    const char* jawtLibraryName = "libjawt.so";

    // Try to load the libjawt.so library first from the the directories listed
    // in the sun.boot.library.path system property. At least Ubuntu builds the
    // JDK with RUNPATH set instead of RPATH. The two differ in their effect on
    // loading transitive dependencies.
    //
    // RPATHs effect also covers libraries loaded as transitive dependencies,
    // while RUNPATH does not. In the case of JNA libjawt is loaded by
    // libdispatch (the native JNA part), which makes it a transtive load.
    //
    // The solution is to load the library with the full path based on the
    // sun.boot.library.path system property, which points to the native library
    // dirs.
    jstring jprop = get_system_property(env, "sun.boot.library.path");
    if (jprop != NULL) {

      char* prop = newCString(env, jprop);
      char* saveptr = NULL;
      char* propToBeTokeninzed = NULL;

      for(propToBeTokeninzed = prop; ; propToBeTokeninzed = NULL) {
          char* pathElement = strtok_r(propToBeTokeninzed, ":", &saveptr);

          if(pathElement == NULL) {
            break;
          }

          size_t len = strlen(pathElement) + strlen(jawtLibraryName) + 2;
          char* path = (char*) alloca(len);

          sprintf(path, "%s/%s", pathElement, jawtLibraryName);

          jawt_handle = LOAD_LIBRARY(path, DEFAULT_LOAD_OPTS);
          if(jawt_handle != NULL) {
            break;
          }
      }

      free((void *)prop);
    }

    if (jawt_handle == NULL) {
      if ((jawt_handle = LOAD_LIBRARY(jawtLibraryName, DEFAULT_LOAD_OPTS)) == NULL) {
        char* msg = LOAD_ERROR();
        throwByName(env, EUnsatisfiedLink, msg);
        free(msg);
        return -1;
      }
    }
#endif
    if ((pJAWT_GetAWT = (void*)FIND_ENTRY(jawt_handle, METHOD_NAME)) == NULL) {
      char* buf = LOAD_ERROR();
      size_t requiredBuffer = strlen(METHOD_NAME) + strlen(buf) + 31 /*static part*/ + 1 /*null*/;
      char* msg = (char*) malloc(requiredBuffer);
      snprintf(msg, requiredBuffer, "Error looking up JAWT method %s: %s", METHOD_NAME, buf);
      throwByName(env, EUnsatisfiedLink, msg);
      free(buf);
      free(msg);
      return -1;
    }
  }
#endif

  if (!JAWT_GetAWT(env, &awt)) {
    throwByName(env, EUnsatisfiedLink, "Can't load JAWT");
    return 0;
  }

  ds = awt.GetDrawingSurface(env, w);
  if (ds == NULL) {
    throwByName(env, EError, "Can't get drawing surface");
  }
  else {
    lock = ds->Lock(ds);
    if ((lock & JAWT_LOCK_ERROR) != 0) {
      awt.FreeDrawingSurface(ds);
      throwByName(env, EError, "Can't get drawing surface lock");
      return 0;
    }
    dsi = ds->GetDrawingSurfaceInfo(ds);
    if (dsi == NULL) {
      throwByName(env, EError, "Can't get drawing surface info");
    }
    else {
#ifdef _WIN32
      JAWT_Win32DrawingSurfaceInfo* wdsi =
        (JAWT_Win32DrawingSurfaceInfo*)dsi->platformInfo;
      if (wdsi != NULL) {
        // FIXME this kills the VM if the window is not realized;
        // if not, wdsi might be a bogus, non-null value
        // TODO: fix JVM (right) or ensure window is realized (done in Java)
        handle = A2L(wdsi->hwnd);
        if (!handle) {
          throwByName(env, EIllegalState, "Can't get HWND");
        }
      }
      else {
        throwByName(env, EError, "Can't get w32 platform info");
      }
#elif __APPLE__
      // WARNING: the view ref is not guaranteed to be stable except during
      // component paint (see jni_md.h)
      JAWT_MacOSXDrawingSurfaceInfo* mdsi =
        (JAWT_MacOSXDrawingSurfaceInfo*)dsi->platformInfo;
      if (mdsi != NULL) {
        handle = (unsigned long)mdsi->cocoaViewRef;
        if (!handle) {
          throwByName(env, EIllegalState, "Can't get Cocoa View");
        }
      }
      else {
        throwByName(env, EError, "Can't get OS X platform info");
      }
#else
      JAWT_X11DrawingSurfaceInfo* xdsi =
        (JAWT_X11DrawingSurfaceInfo*)dsi->platformInfo;
      if (xdsi != NULL) {
        handle = xdsi->drawable;
        if (!handle) {
          throwByName(env, EIllegalState, "Can't get Drawable");
        }
      }
      else {
        throwByName(env, EError, "Can't get X11 platform info");
      }
#endif
      ds->FreeDrawingSurfaceInfo(dsi);
    }
    ds->Unlock(ds);
    awt.FreeDrawingSurface(ds);
  }
#endif /* NO_JAWT */
  return handle;
}

JNIEXPORT jlong JNICALL
Java_com_sun_jna_Native__1getDirectBufferPointer(JNIEnv *env, jclass UNUSED(classp), jobject buffer) {
  void* addr = NULL;
#ifndef NO_NIO_BUFFERS
  addr = (*env)->GetDirectBufferAddress(env, buffer);
#endif
  if (addr == NULL) {
    throwByName(env, EIllegalArgument, "Non-direct Buffer is not supported");
    return 0;
  }
  return A2L(addr);
}

#ifdef HAVE_PROTECTION
JNIEXPORT void JNICALL
Java_com_sun_jna_Native_setProtected(JNIEnv *UNUSED(env), jclass UNUSED(classp), jboolean protect_access) {
  _protect = protect_access;
}
#else
JNIEXPORT void JNICALL
Java_com_sun_jna_Native_setProtected(JNIEnv *UNUSED(env), jclass UNUSED(classp), jboolean UNUSED(protect_access)) {
  /* Unsupported */
}
#endif

jboolean
is_protected() {
#ifdef HAVE_PROTECTION
  if (_protect) return JNI_TRUE;
#endif
  return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_sun_jna_Native_isProtected(JNIEnv *UNUSED(env), jclass UNUSED(classp)) {
  return is_protected();
}

JNIEXPORT void JNICALL
Java_com_sun_jna_Native_setLastError(JNIEnv *env, jclass UNUSED(classp), jint code) {
  JNA_set_last_error(env, code);
  SET_LAST_ERROR(code);
}

JNIEXPORT jint JNICALL
Java_com_sun_jna_Native_getLastError(JNIEnv *env, jclass UNUSED(classp)) {
  return JNA_get_last_error(env);
}

JNIEXPORT jstring JNICALL
Java_com_sun_jna_Native_getNativeVersion(JNIEnv *env, jclass UNUSED(classp)) {
#ifndef JNA_JNI_VERSION
#define JNA_JNI_VERSION "undefined"
#endif
  return newJavaString(env, JNA_JNI_VERSION, CHARSET_UTF8);
}

JNIEXPORT jstring JNICALL
Java_com_sun_jna_Native_getAPIChecksum(JNIEnv *env, jclass UNUSED(classp)) {
#ifndef CHECKSUM
#define CHECKSUM "undefined"
#endif
  return newJavaString(env, CHECKSUM, CHARSET_UTF8);
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *jvm, void *UNUSED(reserved)) {
  JNIEnv* env;
  int result = JNI_VERSION_1_4;
  int attached = (*jvm)->GetEnv(jvm, (void *)&env, JNI_VERSION_1_4) == JNI_OK;
  const char* err;

  if (!attached) {
    if ((*jvm)->AttachCurrentThread(jvm, (void *)&env, NULL) != JNI_OK) {
      fprintf(stderr, "JNA: Can't attach native thread to VM on load\n");
      return 0;
    }
  }

  if ((err = JNA_init(env)) != NULL) {
    fprintf(stderr, "JNA: Problems loading core IDs: %s\n", err);
    result = 0;
  }
  else if ((err = JNA_callback_init(env)) != NULL) {
    fprintf(stderr, "JNA: Problems loading callback IDs: %s\n", err);
    result = 0;
  }
  if (!attached) {
    if ((*jvm)->DetachCurrentThread(jvm) != 0) {
      fprintf(stderr, "JNA: could not detach thread on initial load\n");
    }
  }

  return result;
}

JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *UNUSED(reserved)) {
  jobject* refs[] = {
    &classObject, &classClass, &classMethod,
    &classString,
#ifndef NO_NIO_BUFFERS
    &classBuffer, &classByteBuffer, &classCharBuffer,
    &classShortBuffer, &classIntBuffer, &classLongBuffer,
    &classFloatBuffer, &classDoubleBuffer,
#endif /* NO_NIO_BUFFERS */
    &classVoid, &classPrimitiveVoid,
    &classBoolean, &classPrimitiveBoolean,
    &classByte, &classPrimitiveByte,
    &classCharacter, &classPrimitiveCharacter,
    &classShort, &classPrimitiveShort,
    &classInteger, &classPrimitiveInteger,
    &classLong, &classPrimitiveLong,
    &classFloat, &classPrimitiveFloat,
    &classDouble, &classPrimitiveDouble,
    &classPointer, &classNative, &classWString,
    &classStructure, &classStructureByValue,
    &classCallbackReference, &classAttachOptions, &classNativeMapped,
    &classIntegerType, &classPointerType,
  };
  unsigned i;
  JNIEnv* env;
  int attached = (*vm)->GetEnv(vm, (void*)&env, JNI_VERSION_1_4) == JNI_OK;
  if (!attached) {
    if ((*vm)->AttachCurrentThread(vm, (void*)&env, NULL) != JNI_OK) {
      fprintf(stderr, "JNA: Can't attach native thread to VM on unload\n");
      return;
    }
  }

  // Calls back to the Native class are unsafe at this point
  //(*env)->CallStaticObjectMethod(env, classNative, MID_Native_dispose);

  if (fileEncoding) {
    (*env)->DeleteGlobalRef(env, fileEncoding);
    fileEncoding = NULL;
  }

  for (i=0;i < sizeof(refs)/sizeof(refs[0]);i++) {
    if (*refs[i]) {
      (*env)->DeleteWeakGlobalRef(env, *refs[i]);
      *refs[i] = NULL;
    }
  }

  JNA_callback_dispose(env);

#ifdef JAWT_HEADLESS_HACK
  if (jawt_handle != NULL) {
    FREE_LIBRARY(jawt_handle);
    jawt_handle = NULL;
    pJAWT_GetAWT = NULL;
  }
#endif

  if (!attached) {
    if ((*vm)->DetachCurrentThread(vm) != 0) {
      fprintf(stderr, "JNA: could not detach thread on unload\n");
    }
  }
}

JNIEXPORT void JNICALL
Java_com_sun_jna_Native_unregister(JNIEnv *env, jclass UNUSED(ncls), jclass cls, jlongArray handles) {
  jlong* data = (*env)->GetLongArrayElements(env, handles, NULL);
  int count = (*env)->GetArrayLength(env, handles);

  while (count-- > 0) {
    method_data* md = (method_data*)L2A(data[count]);
    if (md->to_native) {
      unsigned i;
      for (i=0;i < md->cif.nargs;i++) {
        if (md->to_native[i])
          (*env)->DeleteWeakGlobalRef(env, md->to_native[i]);
      }
    }
    if (md->from_native) (*env)->DeleteWeakGlobalRef(env, md->from_native);
    if (md->closure_method) (*env)->DeleteGlobalRef(env, md->closure_method);
    free(md->arg_types);
    free(md->closure_arg_types);
    free(md->flags);
    free((void *)md->encoding);
    free(md);
  }
  (*env)->ReleaseLongArrayElements(env, handles, data, 0);

  // Not required, or recommended for normal code (see description in JNI docs)
  // see http://java.sun.com/j2se/1.4.2/docs/guide/jni/spec/functions.html
  // However, we're not "normal" code
  // This *may* cause issues in some linux code, not completely verified:
  // see http://java.net/jira/browse/JNA-154
  (*env)->UnregisterNatives(env, cls);
}

JNIEXPORT jlong JNICALL
Java_com_sun_jna_Native_registerMethod(JNIEnv *env, jclass UNUSED(ncls),
                                       jclass cls, jstring name,
                                       jstring signature,
                                       jintArray conversions,
                                       jlongArray closure_atypes,
                                       jlongArray atypes,
                                       jint rconversion,
                                       jlong closure_return_type,
                                       jlong return_type,
                                       jobject closure_method,
                                       jlong function, jint cc,
                                       jboolean throw_last_error,
                                       jobjectArray to_native,
                                       jobject from_native,
                                       jstring encoding)
{
  int argc = atypes ? (*env)->GetArrayLength(env, atypes) : 0;
  const char* cname = newCStringUTF8(env, name);
  const char* sig = newCStringUTF8(env, signature);
  void *code;
  void *closure;
  method_data* data = calloc(1, sizeof(method_data));
  ffi_cif* closure_cif = &data->closure_cif;
  int status;
  int i;
  int abi = cc == CALLCONV_C ? FFI_DEFAULT_ABI : cc;
  ffi_type* rtype = (ffi_type*)L2A(return_type);
  ffi_type* closure_rtype = (ffi_type*)L2A(closure_return_type);
  jlong* types = atypes ? (*env)->GetLongArrayElements(env, atypes, NULL) : NULL;
  jlong* closure_types = closure_atypes ? (*env)->GetLongArrayElements(env, closure_atypes, NULL) : NULL;
  jint* cvts = conversions ? (*env)->GetIntArrayElements(env, conversions, NULL) : NULL;
#if defined(_WIN32) && !defined(_WIN64) && !defined(_WIN32_WCE)
  if (cc == CALLCONV_STDCALL) abi = FFI_STDCALL;
#endif
  if (!(abi > FFI_FIRST_ABI && abi < FFI_LAST_ABI)) {
    char msg[MSG_SIZE];
    snprintf(msg, sizeof(msg), "Invalid calling convention %d", abi);
    throwByName(env, EIllegalArgument, msg);
    status = FFI_BAD_ABI;
    goto cleanup;
  }

  data->throw_last_error = throw_last_error;
  data->arg_types = calloc(argc, sizeof(ffi_type*));
  data->closure_arg_types = calloc(argc + 2, sizeof(ffi_type*));
  data->closure_arg_types[0] = &ffi_type_pointer;
  data->closure_arg_types[1] = &ffi_type_pointer;
  data->closure_method = NULL;
  data->flags = cvts ? calloc(argc, sizeof(jint)) : NULL;
  data->rflag = rconversion;
  data->to_native = NULL;
  data->from_native = from_native ? (*env)->NewWeakGlobalRef(env, from_native) : NULL;
  data->encoding = newCStringUTF8(env, encoding);

  for (i=0;i < argc;i++) {
    data->closure_arg_types[i+2] = (ffi_type*)L2A(closure_types[i]);
    data->arg_types[i] = (ffi_type*)L2A(types[i]);
    if (cvts) {
      data->flags[i] = cvts[i];
      // Type mappers only apply to non-primitive arguments
      if (cvts[i] == CVT_TYPE_MAPPER
          || cvts[i] == CVT_TYPE_MAPPER_STRING
          || cvts[i] == CVT_TYPE_MAPPER_WSTRING) {
        if (!data->to_native) {
          data->to_native = calloc(argc, sizeof(jweak));
        }
        data->to_native[i] = (*env)->NewWeakGlobalRef(env, (*env)->GetObjectArrayElement(env, to_native, i));
      }
    }
  }
  if (types) (*env)->ReleaseLongArrayElements(env, atypes, types, 0);
  if (closure_types) (*env)->ReleaseLongArrayElements(env, closure_atypes, closure_types, 0);
  if (cvts) (*env)->ReleaseIntArrayElements(env, conversions, cvts, 0);
  data->fptr = L2A(function);
  data->closure_method = (*env)->NewGlobalRef(env, closure_method);

  status = ffi_prep_cif(closure_cif, abi, argc+2, closure_rtype, data->closure_arg_types);
  if (ffi_error(env, "Native method mapping", status)) {
    goto cleanup;
  }

  status = ffi_prep_cif(&data->cif, abi, argc, rtype, data->arg_types);
  if (ffi_error(env, "Native method setup", status)) {
    goto cleanup;
  }

  closure = ffi_closure_alloc(sizeof(ffi_closure), &code);
  if (closure == NULL) {
    throwByName(env, EUnsupportedOperation, "Failed to allocate closure");
    status = FFI_BAD_ABI;
    goto cleanup;
  }
  status = ffi_prep_closure_loc(closure, closure_cif, dispatch_direct, data, code);
  if (status != FFI_OK) {
    throwByName(env, EError, "Native method linkage failed");
    goto cleanup;
  }

  {
    JNINativeMethod m = { (char*)cname, (char*)sig, code };
    (*env)->RegisterNatives(env, cls, &m, 1);
  }

 cleanup:
  if (status != FFI_OK) {
    free(data->arg_types);
    free(data->flags);
    free(data);
    data = NULL;
  }
  free((void *)cname);
  free((void *)sig);

  return A2L(data);
}

JNIEXPORT void JNICALL
Java_com_sun_jna_Native_ffi_1call(JNIEnv *UNUSED(env), jclass UNUSED(cls), jlong cif, jlong fptr, jlong resp, jlong args)
{
  ffi_call(L2A(cif), FFI_FN(L2A(fptr)), L2A(resp), L2A(args));
}

JNIEXPORT jlong JNICALL
Java_com_sun_jna_Native_ffi_1prep_1cif(JNIEnv *env, jclass UNUSED(cls), jint abi, jint nargs, jlong return_type, jlong arg_types)
{
  ffi_cif* cif = malloc(sizeof(ffi_cif));
  ffi_status s = ffi_prep_cif(L2A(cif), abi ? abi : FFI_DEFAULT_ABI, nargs, L2A(return_type), L2A(arg_types));
  if (ffi_error(env, "ffi_prep_cif", s)) {
    return 0;
  }
  return A2L(cif);
}

JNIEXPORT jlong JNICALL
Java_com_sun_jna_Native_ffi_1prep_1closure(JNIEnv *env, jclass UNUSED(cls), jlong cif, jobject obj)
{
  callback* cb = (callback *)calloc(1, sizeof(callback));
  ffi_status s;

  if ((*env)->GetJavaVM(env, &cb->vm) != JNI_OK) {
    free(cb);
    throwByName(env, EUnsatisfiedLink, "Can't get Java VM");
    return 0;
  }

  cb->object = (*env)->NewWeakGlobalRef(env, obj);
  if (cb->object == NULL) {
    // either obj was null or an OutOfMemoryError has been thrown
    free(cb);
    return 0;
  }
  cb->closure = ffi_closure_alloc(sizeof(ffi_closure), L2A(&cb->x_closure));
  if (cb->closure == NULL) {
    (*env)->DeleteWeakGlobalRef(env, cb->object);
    free(cb);
    throwByName(env, EUnsupportedOperation, "Failed to allocate closure");
    return 0;
  }

  s = ffi_prep_closure_loc(cb->closure, L2A(cif), &closure_handler,
                           cb, cb->x_closure);
  if (ffi_error(env, "ffi_prep_cif", s)) {
    ffi_closure_free(cb->closure);
    (*env)->DeleteWeakGlobalRef(env, cb->object);
    free(cb);
    return 0;
  }
  return A2L(cb);
}

JNIEXPORT void JNICALL
Java_com_sun_jna_Native_ffi_1free_1closure(JNIEnv *env, jclass UNUSED(cls), jlong closure) {
  callback* cb = (callback *)L2A(closure);

  (*env)->DeleteWeakGlobalRef(env, cb->object);
  ffi_closure_free(cb->closure);
  free(cb);
}

JNIEXPORT jint JNICALL
Java_com_sun_jna_Native_initialize_1ffi_1type(JNIEnv *env, jclass UNUSED(cls), jlong type_info) {
  ffi_type* type = L2A(type_info);
  ffi_cif cif;
  ffi_status status = ffi_prep_cif(&cif, FFI_DEFAULT_ABI, 0, type, NULL);
  if (ffi_error(env, "ffi_prep_cif", status)) {
    return 0;
  }
  return (jint)type->size;
}

JNIEXPORT void JNICALL
Java_com_sun_jna_Native_setDetachState(JNIEnv* env, jclass UNUSED(cls), jboolean d, jlong flag) {
  JNA_detach(env, d, L2A(flag));
}

#ifdef __cplusplus
}
#endif
