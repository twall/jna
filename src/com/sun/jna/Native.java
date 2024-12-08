/* Copyright (c) 2007-2015 Timothy Wall, All Rights Reserved
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
package com.sun.jna;

import com.sun.jna.Callback.UncaughtExceptionHandler;
import com.sun.jna.Structure.FFIType;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Window;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/** Provides generation of invocation plumbing for a defined native
 * library interface.  Also provides various utilities for native operations.
 * <p>
 * {@link #getTypeMapper} and {@link #getStructureAlignment} are provided
 * to avoid having to explicitly pass these parameters to {@link Structure}s,
 * which would require every {@link Structure} which requires custom mapping
 * or alignment to define a constructor and pass parameters to the superclass.
 * To avoid lots of boilerplate, the base {@link Structure} constructor
 * figures out these properties based on its enclosing interface.<p>
 * <a name=library_loading></a>
 * <h2>Library Loading</h2>
 * <p>When JNA classes are loaded, the native shared library (jnidispatch) is
 * loaded as well.  An attempt is made to load it from the any paths defined
 * in <code>jna.boot.library.path</code> (if defined), then the system library
 * path using {@link System#loadLibrary}, unless <code>jna.nosys=true</code>.
 * If not found, the appropriate library will be extracted from the class path
 * (into a temporary directory if found within a jar file) and loaded from
 * there, unless <code>jna.noclasspath=true</code>.  If your system has
 * additional security constraints regarding execution or load of files
 * (SELinux, for example), you should  probably install the native library in
 * an accessible location and configure  your system accordingly, rather than
 * relying on JNA to extract the library  from its own jar file.</p>
 * <p>To avoid the automatic unpacking (in situations where you want to force a
 * failure if the JNA native library is not properly installed on the system),
 * set the system property <code>jna.nounpack=true</code>.
 * </p>
 * <p>While this class and its corresponding native library are loaded, the
 * system property <code>jna.loaded</code> will be set.  The property will be
 * cleared when native support has been unloaded (i.e. the Native class and
 * its underlying native support has been GC'd).</p>
 * <p>NOTE: all native functions are provided within this class to ensure that
 * all other JNA-provided classes and objects are GC'd and/or
 * finalized/disposed before this class is disposed and/or removed from
 * memory (most notably Memory and any other class which by default frees its
 * resources in a finalizer).</p>
 * <a name=native_library_loading></a>
 * <h2>Native Library Loading</h2>
 * Native libraries loaded via {@link #load(Class)} may be found in
 * <a href="NativeLibrary.html#library_search_paths">several locations</a>.
 * @see Library
 * @author Todd Fast, todd.fast@sun.com
 * @author twall@users.sf.net
 */
public final class Native implements Version {

    private static final Logger LOG = Logger.getLogger(Native.class.getName());

    public static final Charset DEFAULT_CHARSET;
    public static final String DEFAULT_ENCODING;
    static {
        // JNA used the defaultCharset to determine which encoding to use when
        // converting strings to native char*. The defaultCharset is set from
        // the system property file.encoding. Up to JDK 17 its value defaulted
        // to the system default encoding. From JDK 18 onwards its default value
        // changed to UTF-8.
        // JDK 18+ exposes the native encoding as the new system property
        // native.encoding, prior versions don't have that property and will
        // report NULL for it.
        // The algorithm is simple: If native.encoding is set, it will be used
        // else the original implementation of Charset#defaultCharset is used
        String nativeEncoding = System.getProperty("native.encoding");
        Charset nativeCharset = null;
        if (nativeEncoding != null) {
            try {
                nativeCharset = Charset.forName(nativeEncoding);
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Failed to get charset for native.encoding value : '" + nativeEncoding + "'", ex);
            }
        }
        if (nativeCharset == null) {
            nativeCharset = Charset.defaultCharset();
        }
        DEFAULT_CHARSET = nativeCharset;
        DEFAULT_ENCODING = nativeCharset.name();
    }
    public static final boolean DEBUG_LOAD = Boolean.getBoolean("jna.debug_load");
    public static final boolean DEBUG_JNA_LOAD = Boolean.getBoolean("jna.debug_load.jna");
    private final static Level DEBUG_JNA_LOAD_LEVEL = DEBUG_JNA_LOAD ? Level.INFO : Level.FINE;

    // Used by tests, do not remove
    static String jnidispatchPath = null;
    private static final Map<Class<?>, Map<String, Object>> typeOptions = Collections.synchronizedMap(new WeakHashMap<Class<?>, Map<String, Object>>());
    private static final Map<Class<?>, Reference<?>> libraries = Collections.synchronizedMap(new WeakHashMap<Class<?>, Reference<?>>());
    private static final String _OPTION_ENCLOSING_LIBRARY = "enclosing-library";
    private static final UncaughtExceptionHandler DEFAULT_HANDLER =
        new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Callback c, Throwable e) {
                LOG.log(Level.WARNING, "JNA: Callback " + c + " threw the following exception", e);
            }
        };
    private static UncaughtExceptionHandler callbackExceptionHandler = DEFAULT_HANDLER;

    /** The size of a native pointer (<code>void*</code>) on the current
     * platform, in bytes.
     */
    public static final int POINTER_SIZE;
    /** Size of a native <code>long</code> type, in bytes. */
    public static final int LONG_SIZE;
    /** Size of a native <code>wchar_t</code> type, in bytes. */
    public static final int WCHAR_SIZE;
    /** Size of a native <code>size_t</code> type, in bytes. */
    public static final int SIZE_T_SIZE;
    /** Size of a native <code>bool</code> type (C99 and later), in bytes. */
    public static final int BOOL_SIZE;
    /** Size of a native <code>long double</code> type (C99 and later), in bytes. */
    public static final int LONG_DOUBLE_SIZE;

    private static final int TYPE_VOIDP = 0;
    private static final int TYPE_LONG = 1;
    private static final int TYPE_WCHAR_T = 2;
    private static final int TYPE_SIZE_T = 3;
    private static final int TYPE_BOOL = 4;
    private static final int TYPE_LONG_DOUBLE = 5;

    private static final MethodHandle stackWalkerGetInstance;
    private static final Enum stackWalkerRetainClassReference;
    private static final MethodHandle stackWalkerWalk;
    private static final Object stackWalkerFilter;

    private static final MethodHandle securityManagerExposerConstructor;
    private static final MethodHandle securityManagerGetClassContext;

    private static final MethodHandle accessControllerDoPrivileged;

    static final int MAX_ALIGNMENT;
    static final int MAX_PADDING;

    /**
     * Version string must have the structure <major>.<minor>.<revision>
     * a bugfix change in the native code increments revision, the minor is
     * incremented for backwards compatible changes and the major version
     * is changed for backwards incompatbile changes.
     *
     * @param expectedVersion
     * @param nativeVersion
     * @return true if nativeVersion describes a version compatible to expectedVersion
     */
    static boolean isCompatibleVersion(String expectedVersion, String nativeVersion) {
        String[] expectedVersionParts = expectedVersion.split("\\.");
        String[] nativeVersionParts = nativeVersion.split("\\.");
        if(expectedVersionParts.length < 3 || nativeVersionParts.length < 3) {
            return false;
        }

        int expectedMajor = Integer.parseInt(expectedVersionParts[0]);
        int nativeMajor = Integer.parseInt(nativeVersionParts[0]);
        int expectedMinor = Integer.parseInt(expectedVersionParts[1]);
        int nativeMinor = Integer.parseInt(nativeVersionParts[1]);

        if(expectedMajor != nativeMajor) {
            return false;
        }

        if(expectedMinor > nativeMinor) {
            return false;
        }

        return true;
    }

    static {
        loadNativeDispatchLibrary();

        if (! isCompatibleVersion(VERSION_NATIVE, getNativeVersion())) {
            String LS = System.lineSeparator();
            throw new Error(LS + LS
                            + "There is an incompatible JNA native library installed on this system" + LS
                            + "Expected: " + VERSION_NATIVE + LS
                            + "Found:    " + getNativeVersion() + LS
                            + (jnidispatchPath != null
                               ? "(at " + jnidispatchPath + ")" : System.getProperty("java.library.path"))
                            + "." + LS
                            + "To resolve this issue you may do one of the following:" + LS
                            + " - remove or uninstall the offending library" + LS
                            + " - set the system property jna.nosys=true" + LS
                            + " - set jna.boot.library.path to include the path to the version of the " + LS
                            + "   jnidispatch library included with the JNA jar file you are using" + LS);
        }

        POINTER_SIZE = sizeof(TYPE_VOIDP);
        LONG_SIZE = sizeof(TYPE_LONG);
        WCHAR_SIZE = sizeof(TYPE_WCHAR_T);
        SIZE_T_SIZE = sizeof(TYPE_SIZE_T);
        BOOL_SIZE = sizeof(TYPE_BOOL);
        LONG_DOUBLE_SIZE = sizeof(TYPE_LONG_DOUBLE);

        // Perform initialization of other JNA classes until *after*
        // initializing the above final fields
        initIDs();
        if (Boolean.getBoolean("jna.protected")) {
            setProtected(true);
        }
        MAX_ALIGNMENT = Platform.isSPARC() || Platform.isWindows()
            || (Platform.isLinux() && (Platform.isARM() || Platform.isPPC() || Platform.isMIPS() || Platform.isLoongArch()))
            || Platform.isAIX()
            || (Platform.isAndroid() && !Platform.isIntel())
            ? 8 : LONG_SIZE;
        MAX_PADDING = (Platform.isMac() && Platform.isPPC()) ? 8 : MAX_ALIGNMENT;

        Enum stackWalkerRetainClassReferenceBuilder;
        MethodHandle stackWalkerGetInstanceBuilder;
        MethodHandle stackWalkerWalkBuilder;
        Object stackWalkerFilterBuilder;
        try {
            Lookup lookup = MethodHandles.lookup();
            Class<?> stackWalkerClass = Class.forName("java.lang.StackWalker");
            Class<? extends Enum> stackWalkerOptionClass = (Class<? extends Enum>) Class.forName("java.lang.StackWalker$Option");
            stackWalkerRetainClassReferenceBuilder = Enum.valueOf(stackWalkerOptionClass, "RETAIN_CLASS_REFERENCE");
            stackWalkerGetInstanceBuilder = lookup.findStatic(stackWalkerClass, "getInstance", MethodType.methodType(stackWalkerClass, stackWalkerOptionClass));
            stackWalkerWalkBuilder = lookup.findVirtual(stackWalkerClass, "walk", MethodType.methodType(Object.class, java.util.function.Function.class));
            Class<?> stackframe = Class.forName("java.lang.StackWalker$StackFrame");
            MethodHandle stackFrameGetDeclaringClass = lookup.findVirtual(stackframe, "getDeclaringClass", MethodType.methodType(Class.class));
            stackWalkerFilterBuilder = new java.util.function.Function<Stream<Object>, Class<?>>() {
                @Override
                public Class<?> apply(Stream<Object> t) {
                    Object stackFrame = t.skip(2).findFirst().get();
                    try {
                        return (Class<?>) stackFrameGetDeclaringClass.invoke(stackFrame);
                    } catch (Throwable ex) {
                        return null;
                    }
                }
            };

        } catch (Throwable ex) {
            LOG.log(Level.FINE, "Failed to initialize stack accessor method StackWalker", ex);
            stackWalkerRetainClassReferenceBuilder = null;
            stackWalkerGetInstanceBuilder = null;
            stackWalkerWalkBuilder = null;
            stackWalkerFilterBuilder = null;
        }
        stackWalkerRetainClassReference = stackWalkerRetainClassReferenceBuilder;
        stackWalkerGetInstance = stackWalkerGetInstanceBuilder;
        stackWalkerWalk = stackWalkerWalkBuilder;
        stackWalkerFilter = stackWalkerFilterBuilder;

        MethodHandle securityManagerExposerConstructorBuilder;
        MethodHandle securityManagerGetClassContextBuilder;
        try {
            Lookup lookup = MethodHandles.lookup();
            Class<?> securityManagerExposerClass = Class.forName("com.sun.jna.SecurityManagerExposer");
            securityManagerExposerConstructorBuilder = lookup.findConstructor(securityManagerExposerClass, MethodType.methodType(void.class));
            securityManagerGetClassContextBuilder = lookup.findVirtual(securityManagerExposerClass, "getClassContext", MethodType.methodType(Class[].class));
        } catch (Throwable ex) {
            LOG.log(Level.FINE, "Failed to initialize stack accessor method SecurityManager", ex);
            securityManagerExposerConstructorBuilder = null;
            securityManagerGetClassContextBuilder = null;
        }
        securityManagerExposerConstructor = securityManagerExposerConstructorBuilder;
        securityManagerGetClassContext = securityManagerGetClassContextBuilder;

        MethodHandle accessControllerDoPrivilegedBuilder = null;
        try {
            Lookup lookup = MethodHandles.lookup();
            Class<?> accessControllerClass = Class.forName("java.security.AccessController");
            accessControllerDoPrivilegedBuilder = lookup.findStatic(accessControllerClass, "doPrivileged", MethodType.methodType(Object.class, PrivilegedAction.class));
        } catch (Throwable ex) {
            LOG.log(Level.FINE, "Failed to initialize AccessController#doPrivileged", ex);
            accessControllerDoPrivilegedBuilder = null;
        }
        accessControllerDoPrivileged = accessControllerDoPrivilegedBuilder;

        System.setProperty("jna.loaded", "true");
    }

    /** Force a dispose when the Native class is GC'd. */
    private static final Object finalizer = new Object() {
        @Override
        protected void finalize() throws Throwable {
            dispose();
            super.finalize();
        }
    };

    /** Properly dispose of JNA functionality.
        Called when this class is finalized and also from JNI when
        JNA's native shared library is unloaded.
     */
    private static void dispose() {
        CallbackReference.disposeAll();
        Memory.disposeAll();
        NativeLibrary.disposeAll();
        unregisterAll();
        jnidispatchPath = null;
        System.setProperty("jna.loaded", "false");
    }

    /** Remove any automatically unpacked native library.

        This will fail on windows, which disallows removal of any file that is
        still in use, so an alternative is required in that case.  Mark
        the file that could not be deleted, and attempt to delete any
        temporaries on next startup.

        Do NOT force the class loader to unload the native library, since
        that introduces issues with cleaning up any extant JNA bits
        (e.g. Memory) which may still need use of the library before shutdown.
     */
    static boolean deleteLibrary(File lib) {
        if (lib.delete()) {
            return true;
        }

        // Couldn't delete it, mark for later deletion
        markTemporaryFile(lib);

        return false;
    }

    private Native() { }

    private static native void initIDs();

    /** Set whether native memory accesses are protected from invalid
     * accesses.  This should only be set true when testing or debugging,
     * and should not be considered reliable or robust for applications
     * where JNA native calls are occurring on multiple threads.
     * Protected mode will be automatically set if the
     * system property <code>jna.protected</code> has a value of "true"
     * when the JNA library is first loaded.<p>
     * If not supported by the underlying platform, this setting will
     * have no effect.<p>
     * NOTE: On platforms which support signals (non-Windows), JNA uses
     * signals to trap errors.  This may interfere with the JVM's own use of
     * signals.  When protected mode is enabled, you should make use of the
     * jsig library, if available (see <a href="http://download.oracle.com/javase/6/docs/technotes/guides/vm/signal-chaining.html">Signal Chaining</a>).
     * In short, set the environment variable <code>LD_PRELOAD</code> to the
     * path to <code>libjsig.so</code> in your JRE lib directory
     * (usually ${java.home}/lib/${os.arch}/libjsig.so) before launching your
     * Java application.
     */
    public static synchronized native void setProtected(boolean enable);

    /** Returns whether protection is enabled.  Check the result of this method
     * after calling {@link #setProtected setProtected(true)} to determine
     * if this platform supports protecting memory accesses.
     */
    public static synchronized native boolean isProtected();

    /** Utility method to get the native window ID for a Java {@link Window}
     * as a <code>long</code> value.
     * This method is primarily for X11-based systems, which use an opaque
     * <code>XID</code> (usually <code>long int</code>) to identify windows.
     * @throws HeadlessException if the current VM is running headless
     */
    public static long getWindowID(Window w) throws HeadlessException {
        return AWT.getWindowID(w);
    }

    /** Utility method to get the native window ID for a heavyweight Java
     * {@link Component} as a <code>long</code> value.
     * This method is primarily for X11-based systems, which use an opaque
     * <code>XID</code> (usually <code>long int</code>) to identify windows.
     * @throws HeadlessException if the current VM is running headless
     */
    public static long getComponentID(Component c) throws HeadlessException {
        return AWT.getComponentID(c);
    }

    /** Utility method to get the native window pointer for a Java
     * {@link Window} as a {@link Pointer} value.  This method is primarily for
     * w32, which uses the <code>HANDLE</code> type (actually
     * <code>void *</code>) to identify windows.
     * @throws HeadlessException if the current VM is running headless
     */
    public static Pointer getWindowPointer(Window w) throws HeadlessException {
        return new Pointer(AWT.getWindowID(w));
    }

    /** Utility method to get the native window pointer for a heavyweight Java
     * {@link Component} as a {@link Pointer} value.  This method is primarily
     * for w32, which uses the <code>HWND</code> type (actually
     * <code>void *</code>) to identify windows.
     * @throws HeadlessException if the current VM is running headless
     */
    public static Pointer getComponentPointer(Component c) throws HeadlessException {
        return new Pointer(AWT.getComponentID(c));
    }

    static native long getWindowHandle0(Component c);

    /** Convert a direct {@link Buffer} into a {@link Pointer}.
     * @throws IllegalArgumentException if the buffer is not direct.
     */
    public static Pointer getDirectBufferPointer(Buffer b) {
        long peer = _getDirectBufferPointer(b);
        return peer == 0 ? null : new Pointer(peer);
    }

    private static native long _getDirectBufferPointer(Buffer b);

    /**
     * Gets the charset belonging to the given {@code encoding}.
     * @param encoding The encoding - if {@code null} then the default platform
     * encoding is used.
     * @return The charset belonging to the given {@code encoding} or the platform default.
     * Never {@code null}.
     */
    private static Charset getCharset(String encoding) {
        Charset charset = null;
        if (encoding != null) {
            try {
                charset = Charset.forName(encoding);
            }
            catch(IllegalCharsetNameException | UnsupportedCharsetException  e) {
                LOG.log(Level.WARNING, "JNA Warning: Encoding ''{0}'' is unsupported ({1})",
                        new Object[]{encoding, e.getMessage()});
            }
        }
        if (charset == null) {
            LOG.log(Level.WARNING, "JNA Warning: Using fallback encoding {0}", Native.DEFAULT_CHARSET);
            charset = Native.DEFAULT_CHARSET;
        }
        return charset;
    }

    /**
     * Obtain a Java String from the given native byte array.  If there is
     * no NUL terminator, the String will comprise the entire array.  The
     * encoding is obtained from {@link #getDefaultStringEncoding()}.
     *
     * @param buf The buffer containing the encoded bytes
     * @see #toString(byte[], String)
     */
    public static String toString(byte[] buf) {
        return toString(buf, getDefaultStringEncoding());
    }

    /**
     * Obtain a Java String from the given native byte array, using the given
     * encoding.  If there is no NUL terminator, the String will comprise the
     * entire array.
     *
     * <p><strong>Usage note</strong>: This function assumes, that {@code buf}
     * holds a {@code char} array. This means only single-byte encodings are
     * supported.</p>
     *
     * @param buf The buffer containing the encoded bytes.  Must not be {@code null}.
     * @param encoding The encoding name - if {@code null} then the platform
     * default encoding will be used
     */
    public static String toString(byte[] buf, String encoding) {
        return Native.toString(buf, Native.getCharset(encoding));
    }

    /**
     * Obtain a Java String from the given native byte array, using the given
     * encoding.  If there is no NUL terminator, the String will comprise the
     * entire array.
     *
     * <p><strong>Usage note</strong>: This function assumes, that {@code buf}
     * holds a {@code char} array. This means only single-byte encodings are
     * supported.</p>
     *
     * @param buf The buffer containing the encoded bytes. Must not be {@code null}.
     * @param charset The charset to decode {@code buf}. Must not be {@code null}.
     */
    public static String toString(byte[] buf, Charset charset) {
        int len = buf.length;
        // find out the effective length
        for (int index = 0; index < len; index++) {
            if (buf[index] == 0) {
                len = index;
                break;
            }
        }

        if (len == 0) {
            return "";
        }

        return new String(buf, 0, len, charset);
    }

    /**
     * Obtain a Java String from the given native wchar_t array.  If there is
     * no NUL terminator, the String will comprise the entire array.
     *
     * @param buf The buffer containing the characters
     */
    public static String toString(char[] buf) {
        int len = buf.length;
        for (int index = 0; index < len; index++) {
            if (buf[index] == '\0') {
                len = index;
                break;
            }
        }

        if (len == 0) {
            return "";
        } else {
            return new String(buf, 0, len);
        }
    }

    /**
     * Converts a &quot;list&quot; of strings each null terminated
     * into a {@link List} of {@link String} values. The end of the
     * list is signaled by an extra NULL value at the end or by the
     * end of the buffer.
     * @param buf The buffer containing the strings
     * @return A {@link List} of all the strings in the buffer
     * @see #toStringList(char[], int, int)
     */
    public static List<String> toStringList(char[] buf) {
        return toStringList(buf, 0, buf.length);
    }

    /**
     * Converts a &quot;list&quot; of strings each null terminated
     * into a {@link List} of {@link String} values. The end of the
     * list is signaled by an extra NULL value at the end or by the
     * end of the data.
     * @param buf The buffer containing the strings
     * @param offset Offset to start parsing
     * @param len The total characters to parse
     * @return A {@link List} of all the strings in the buffer
     */
    public static List<String> toStringList(char[] buf, int offset, int len) {
        List<String> list = new ArrayList<>();
        int lastPos = offset;
        int maxPos = offset + len;
        for (int curPos = offset; curPos < maxPos; curPos++) {
            if (buf[curPos] != '\0') {
                continue;
            }

            // check if found the extra null terminator
            if (lastPos == curPos) {
                return list;
            }

            String value = new String(buf, lastPos, curPos - lastPos);
            list.add(value);
            lastPos = curPos + 1;   // skip the '\0'
        }

        // This point is reached if there is no double null terminator
        if (lastPos < maxPos) {
            String value = new String(buf, lastPos, maxPos - lastPos);
            list.add(value);
        }

        return list;
    }

    /** Map a library interface to the current process, providing
     * the explicit interface class.
     * Native libraries loaded via this method may be found in
     * <a href="NativeLibrary.html#library_search_paths">several locations</a>.
     * @param <T> Type of expected wrapper
     * @param interfaceClass The implementation wrapper interface
     * @return an instance of the requested interface, mapped to the current
     * process.
     * @throws UnsatisfiedLinkError if the library cannot be found or
     * dependent libraries are missing.
     */
    public static <T extends Library> T load(Class<T> interfaceClass) {
        return load(null, interfaceClass);
    }

    /** Map a library interface to the current process, providing
     * the explicit interface class.  Any options provided for the library are
     * cached and associated with the library and any of its defined
     * structures and/or functions.
     * Native libraries loaded via this method may be found in
     * <a href="NativeLibrary.html#library_search_paths">several locations</a>.
     * @param <T> Type of expected wrapper
     * @param interfaceClass The implementation wrapper interface
     * @param options Map of library options
     * @return an instance of the requested interface, mapped to the current
     * process.
     * @throws UnsatisfiedLinkError if the library cannot be found or
     * dependent libraries are missing.
     * @see #load(String, Class, Map)
     */
    public static <T extends Library> T load(Class<T> interfaceClass, Map<String, ?> options) {
        return load(null, interfaceClass, options);
    }

    /** Map a library interface to the given shared library, providing
     * the explicit interface class.
     * If <code>name</code> is null, attempts to map onto the current process.
     * Native libraries loaded via this method may be found in
     * <a href="NativeLibrary.html#library_search_paths">several locations</a>.
     * @param <T> Type of expected wrapper
     * @param name Library base name
     * @param interfaceClass The implementation wrapper interface
     * @return an instance of the requested interface, mapped to the indicated
     * native library.
     * @throws UnsatisfiedLinkError if the library cannot be found or
     * dependent libraries are missing.
     * @see #load(String, Class, Map)
     */
    public static <T extends Library> T load(String name, Class<T> interfaceClass) {
        return load(name, interfaceClass, Collections.<String, Object>emptyMap());
    }

    /** Load a library interface from the given shared library, providing
     * the explicit interface class and a map of options for the library.
     * If no library options are detected the map is interpreted as a map
     * of Java method names to native function names.<p>
     * If <code>name</code> is null, attempts to map onto the current process.
     * Native libraries loaded via this method may be found in
     * <a href="NativeLibrary.html#library_search_paths">several locations</a>.
     * @param <T> Type of expected wrapper
     * @param name Library base name
     * @param interfaceClass The implementation wrapper interface
     * @param options Map of library options
     * @return an instance of the requested interface, mapped to the indicated
     * native library.
     * @throws UnsatisfiedLinkError if the library cannot be found or
     * dependent libraries are missing.
     */
    public static <T extends Library> T load(String name, Class<T> interfaceClass, Map<String, ?> options) {
        if (!Library.class.isAssignableFrom(interfaceClass)) {
            // Maybe still possible if the caller is not using generics?
            throw new IllegalArgumentException("Interface (" + interfaceClass.getSimpleName() + ")"
                    + " of library=" + name + " does not extend " + Library.class.getSimpleName());
        }

        Library.Handler handler = new Library.Handler(name, interfaceClass, options);
        ClassLoader loader = interfaceClass.getClassLoader();
        Object proxy = Proxy.newProxyInstance(loader, new Class[] {interfaceClass}, handler);
        cacheOptions(interfaceClass, options, proxy);
        return interfaceClass.cast(proxy);
    }

    /**
     * Provided for improved compatibility between JNA 4.X and 5.X
     *
     * @see Native#load(java.lang.Class)
     */
    @Deprecated
    public static <T> T loadLibrary(Class<T> interfaceClass) {
        return loadLibrary(null, interfaceClass);
    }

    /**
     * Provided for improved compatibility between JNA 4.X and 5.X
     *
     * @see Native#load(java.lang.Class, java.util.Map)
     */
    @Deprecated
    public static <T> T loadLibrary(Class<T> interfaceClass, Map<String, ?> options) {
        return loadLibrary(null, interfaceClass, options);
    }

    /**
     * Provided for improved compatibility between JNA 4.X and 5.X
     *
     * @see Native#load(java.lang.String, java.lang.Class)
     */
    @Deprecated
    public static <T> T loadLibrary(String name, Class<T> interfaceClass) {
        return loadLibrary(name, interfaceClass, Collections.<String, Object>emptyMap());
    }

    /**
     * Provided for improved compatibility between JNA 4.X and 5.X
     *
     * @see Native#load(java.lang.String, java.lang.Class, java.util.Map)
     */
    @Deprecated
    public static <T> T loadLibrary(String name, Class<T> interfaceClass, Map<String, ?> options) {
        if (!Library.class.isAssignableFrom(interfaceClass)) {
            // Maybe still possible if the caller is not using generics?
            throw new IllegalArgumentException("Interface (" + interfaceClass.getSimpleName() + ")"
                    + " of library=" + name + " does not extend " + Library.class.getSimpleName());
        }

        Library.Handler handler = new Library.Handler(name, interfaceClass, options);
        ClassLoader loader = interfaceClass.getClassLoader();
        Object proxy = Proxy.newProxyInstance(loader, new Class[] {interfaceClass}, handler);
        cacheOptions(interfaceClass, options, proxy);
        return interfaceClass.cast(proxy);
    }

    /** Attempts to force initialization of an instance of the library interface
     * by loading a public static field of the requisite type.
     * Returns whether an instance variable was instantiated.
     * Expects that lock on libraries is already held
     */
    private static void loadLibraryInstance(Class<?> cls) {
        if (cls != null && !libraries.containsKey(cls)) {
            try {
                Field[] fields = cls.getFields();
                for (int i=0;i < fields.length;i++) {
                    Field field = fields[i];
                    if (field.getType() == cls
                        && Modifier.isStatic(field.getModifiers())) {
                        // Ensure the field gets initialized by reading it
                        field.setAccessible(true); // interface might be private
                        libraries.put(cls, new WeakReference<>(field.get(null)));
                        break;
                    }
                }
            }
            catch (Exception e) {
                throw new IllegalArgumentException("Could not access instance of "
                                                   + cls + " (" + e + ")");
            }
        }
    }

    /**
     * Find the library interface corresponding to the given class.  Checks
     * all ancestor classes and interfaces for a declaring class which
     * implements {@link Library}.
     * @param cls The given class
     * @return The enclosing class
     */
    static Class<?> findEnclosingLibraryClass(Class<?> cls) {
        if (cls == null) {
            return null;
        }
        // Check for direct-mapped libraries, which won't necessarily
        // implement com.sun.jna.Library.
        Map<String, ?> libOptions = typeOptions.get(cls);
        if (libOptions != null) {
            Class<?> enclosingClass = (Class<?>)libOptions.get(_OPTION_ENCLOSING_LIBRARY);
            if (enclosingClass != null) {
                return enclosingClass;
            }
            return cls;
        }
        if (Library.class.isAssignableFrom(cls)) {
            return cls;
        }
        if (Callback.class.isAssignableFrom(cls)) {
            cls = CallbackReference.findCallbackClass(cls);
        }
        Class<?> declaring = cls.getDeclaringClass();
        Class<?> fromDeclaring = findEnclosingLibraryClass(declaring);
        if (fromDeclaring != null) {
            return fromDeclaring;
        }
        return findEnclosingLibraryClass(cls.getSuperclass());
    }


    /** Return the preferred native library configuration options for the given
     * class.  First attempts to load any field of the interface type within
     * the interface mapping, then checks the cache for any specified library
     * options.  If none found, a set of library options will be generated
     * from the fields (by order of precedence) <code>OPTIONS</code> (a {@link
     * Map}), <code>TYPE_MAPPER</code> (a {@link TypeMapper}),
     * <code>STRUCTURE_ALIGNMENT</code> (an {@link Integer}), and
     * <code>STRING_ENCODING</code> (a {@link String}).
     *
     * @param type The type class
     * @return The options map
     */
    public static Map<String, Object> getLibraryOptions(Class<?> type) {
        Map<String, Object> libraryOptions;
        // cached already ?
        libraryOptions = typeOptions.get(type);
        if (libraryOptions != null) {
            return libraryOptions;
        }

        Class<?> mappingClass = findEnclosingLibraryClass(type);
        if (mappingClass != null) {
            loadLibraryInstance(mappingClass);
        } else {
            mappingClass = type;
        }

        libraryOptions = typeOptions.get(mappingClass);
        if (libraryOptions != null) {
            typeOptions.put(type, libraryOptions);  // cache for next time
            return libraryOptions;
        }

        try {
            Field field = mappingClass.getField("OPTIONS");
            field.setAccessible(true);
            libraryOptions = (Map<String, Object>) field.get(null);
            if (libraryOptions == null) {
                throw new IllegalStateException("Null options field");
            }
        } catch (NoSuchFieldException e) {
            libraryOptions = Collections.<String, Object>emptyMap();
        } catch (Exception e) {
            throw new IllegalArgumentException("OPTIONS must be a public field of type java.util.Map (" + e + "): " + mappingClass);
        }
        // Make a clone of the original options
        libraryOptions = new HashMap<>(libraryOptions);
        if (!libraryOptions.containsKey(Library.OPTION_TYPE_MAPPER)) {
            libraryOptions.put(Library.OPTION_TYPE_MAPPER, lookupField(mappingClass, "TYPE_MAPPER", TypeMapper.class));
        }
        if (!libraryOptions.containsKey(Library.OPTION_STRUCTURE_ALIGNMENT)) {
            libraryOptions.put(Library.OPTION_STRUCTURE_ALIGNMENT, lookupField(mappingClass, "STRUCTURE_ALIGNMENT", Integer.class));
        }
        if (!libraryOptions.containsKey(Library.OPTION_STRING_ENCODING)) {
            libraryOptions.put(Library.OPTION_STRING_ENCODING, lookupField(mappingClass, "STRING_ENCODING", String.class));
        }
        libraryOptions = cacheOptions(mappingClass, libraryOptions, null);
        // Store the original lookup class, if different from the mapping class
        if (type != mappingClass) {
            typeOptions.put(type, libraryOptions);
        }
        return libraryOptions;
    }

    private static Object lookupField(Class<?> mappingClass, String fieldName, Class<?> resultClass) {
        try {
            Field field = mappingClass.getField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        }
        catch (NoSuchFieldException e) {
            return null;
        }
        catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " must be a public field of type "
                                               + resultClass.getName() + " ("
                                               + e + "): " + mappingClass);
        }
    }

    /** Return the preferred {@link TypeMapper} for the given native interface.
     * See {@link com.sun.jna.Library#OPTION_TYPE_MAPPER}.
     */
    public static TypeMapper getTypeMapper(Class<?> cls) {
        Map<String, ?> options = getLibraryOptions(cls);
        return (TypeMapper) options.get(Library.OPTION_TYPE_MAPPER);
    }

    /**
     * @param cls The native interface type
     * @return The preferred string encoding for the given native interface.
     * If there is no setting, defaults to the {@link #getDefaultStringEncoding()}.
     * @see com.sun.jna.Library#OPTION_STRING_ENCODING
     */
    public static String getStringEncoding(Class<?> cls) {
        Map<String, ?> options = getLibraryOptions(cls);
        String encoding = (String) options.get(Library.OPTION_STRING_ENCODING);
        return encoding != null ? encoding : getDefaultStringEncoding();
    }

    /**
     * @return The default string encoding.  Returns the value of the system
     * property <code>jna.encoding</code> or {@link Native#DEFAULT_ENCODING}.
     */
    public static String getDefaultStringEncoding() {
        return System.getProperty("jna.encoding", DEFAULT_ENCODING);
    }

    /**
     * @param cls The native interface type
     * @return The preferred structure alignment for the given native interface.
     * @see com.sun.jna.Library#OPTION_STRUCTURE_ALIGNMENT
     */
    public static int getStructureAlignment(Class<?> cls) {
        Integer alignment = (Integer)getLibraryOptions(cls).get(Library.OPTION_STRUCTURE_ALIGNMENT);
        return alignment == null ? Structure.ALIGN_DEFAULT : alignment;
    }

    /**
     * @param s The input string
     * @return A byte array corresponding to the given String.  The encoding
     * used is obtained from {@link #getDefaultStringEncoding()}.
     */
    static byte[] getBytes(String s) {
        return getBytes(s, getDefaultStringEncoding());
    }

    /**
     * @param s The string. Must not be {@code null}.
     * @param encoding The encoding - if {@code null} then the default platform
     * encoding is used
     * @return A byte array corresponding to the given String, using the given
     * encoding.  If the encoding is not found default to the platform native
     * encoding.
    */
    static byte[] getBytes(String s, String encoding) {
        return Native.getBytes(s, Native.getCharset(encoding));
    }

    /**
     * @param s The string. Must not be {@code null}.
     * @param charset The charset used to encode {@code s}. Must not be {@code null}.
     * @return A byte array corresponding to the given String, using the given
     * charset.
    */
    static byte[] getBytes(String s, Charset charset) {
        return s.getBytes(charset);
    }

    /**
     * @param s The string
     * @return A NUL-terminated byte buffer equivalent to the given String,
     * using the encoding returned by {@link #getDefaultStringEncoding()}.
     * @see #toByteArray(String, String)
     */
    public static byte[] toByteArray(String s) {
        return toByteArray(s, getDefaultStringEncoding());
    }

    /**
     * @param s The string. Must not be {@code null}.
     * @param encoding The encoding - if {@code null} then the default platform
     * encoding is used
     * @return A NUL-terminated byte buffer equivalent to the given String,
     * using the given encoding.
     * @see #getBytes(String, String)
     */
    public static byte[] toByteArray(String s, String encoding) {
        return Native.toByteArray(s, Native.getCharset(encoding));
    }

    /**
     * @param s The string. Must not be {@code null}.
     * @param charset The charset used to encode {@code s}. Must not be {@code null}.
     * @return A NUL-terminated byte buffer equivalent to the given String,
     * using the given charset.
     * @see #getBytes(String, String)
     */
    public static byte[] toByteArray(String s, Charset charset) {
        byte[] bytes = Native.getBytes(s, charset);
        byte[] buf = new byte[bytes.length+1];
        System.arraycopy(bytes, 0, buf, 0, bytes.length);
        return buf;
    }

    /**
     * @param s The string
     * @return A NUL-terminated wide character buffer equivalent to the given string.
    */
    public static char[] toCharArray(String s) {
        char[] chars = s.toCharArray();
        char[] buf = new char[chars.length+1];
        System.arraycopy(chars, 0, buf, 0, chars.length);
        return buf;
    }

    /**
     * Loads the JNA stub library.
     * First tries jna.boot.library.path, then the system path, then from the
     * jar file.
     */
    private static void loadNativeDispatchLibrary() {
        if (!Boolean.getBoolean("jna.nounpack")) {
            try {
                removeTemporaryFiles();
            }
            catch(IOException e) {
                LOG.log(Level.WARNING, "JNA Warning: IOException removing temporary files", e);
            }
        }

        String libName = System.getProperty("jna.boot.library.name", "jnidispatch");
        String bootPath = System.getProperty("jna.boot.library.path");
        if (bootPath != null) {
            // String.split not available in 1.4
            StringTokenizer dirs = new StringTokenizer(bootPath, File.pathSeparator);
            while (dirs.hasMoreTokens()) {
                String dir = dirs.nextToken();
                File file = new File(new File(dir), System.mapLibraryName(libName).replace(".dylib", ".jnilib"));
                String path = file.getAbsolutePath();
                LOG.log(DEBUG_JNA_LOAD_LEVEL, "Looking in {0}", path);
                if (file.exists()) {
                    try {
                        LOG.log(DEBUG_JNA_LOAD_LEVEL, "Trying {0}", path);
                        System.setProperty("jnidispatch.path", path);
                        System.load(path);
                        jnidispatchPath = path;
                        LOG.log(DEBUG_JNA_LOAD_LEVEL, "Found jnidispatch at {0}", path);
                        return;
                    } catch (UnsatisfiedLinkError ex) {
                        // Not a problem if already loaded in anoteher class loader
                        // Unfortunately we can't distinguish the difference...
                        //System.out.println("File found at " + file + " but not loadable: " + ex.getMessage());
                    }
                }
                if (Platform.isMac()) {
                    String orig, ext;
                    if (path.endsWith("dylib")) {
                        orig = "dylib";
                        ext = "jnilib";
                    } else {
                        orig = "jnilib";
                        ext = "dylib";
                    }
                    path = path.substring(0, path.lastIndexOf(orig)) + ext;
                    LOG.log(DEBUG_JNA_LOAD_LEVEL, "Looking in {0}", path);
                    if (new File(path).exists()) {
                        try {
                            LOG.log(DEBUG_JNA_LOAD_LEVEL, "Trying {0}", path);
                            System.setProperty("jnidispatch.path", path);
                            System.load(path);
                            jnidispatchPath = path;
                            LOG.log(DEBUG_JNA_LOAD_LEVEL, "Found jnidispatch at {0}", path);
                            return;
                        } catch (UnsatisfiedLinkError ex) {
                            LOG.log(Level.WARNING, "File found at " + path + " but not loadable: " + ex.getMessage(), ex);
                        }
                    }
                }
            }
        }
        String jnaNosys = System.getProperty("jna.nosys", "true");
        if ((!Boolean.parseBoolean(jnaNosys)) || Platform.isAndroid()) {
            try {
                LOG.log(DEBUG_JNA_LOAD_LEVEL, "Trying (via loadLibrary) {0}", libName);
                System.loadLibrary(libName);
                LOG.log(DEBUG_JNA_LOAD_LEVEL, "Found jnidispatch on system path");
                return;
            }
            catch(UnsatisfiedLinkError e) {
            }
        }
        if (!Boolean.getBoolean("jna.noclasspath")) {
            loadNativeDispatchLibraryFromClasspath();
        }
        else {
            throw new UnsatisfiedLinkError("Unable to locate JNA native support library");
        }
    }

    static final String JNA_TMPLIB_PREFIX = "jna";
    /**
     * Attempts to load the native library resource from the filesystem,
     * extracting the JNA stub library from jna.jar if not already available.
     */
    private static void loadNativeDispatchLibraryFromClasspath() {
        try {
            String mappedName = System.mapLibraryName("jnidispatch").replace(".dylib", ".jnilib");
            if(Platform.isAIX()) {
                // OpenJDK is reported to map to .so -- this works around the
                // difference between J9 and OpenJDK
                mappedName = "libjnidispatch.a";
            }
            String libName = "/com/sun/jna/" + Platform.RESOURCE_PREFIX + "/" + mappedName;
            File lib = extractFromResourcePath(libName, Native.class.getClassLoader());
            if (lib == null) {
                if (lib == null) {
                    throw new UnsatisfiedLinkError("Could not find JNA native support");
                }
            }

            LOG.log(DEBUG_JNA_LOAD_LEVEL, "Trying {0}", lib.getAbsolutePath());
            System.setProperty("jnidispatch.path", lib.getAbsolutePath());
            System.load(lib.getAbsolutePath());
            jnidispatchPath = lib.getAbsolutePath();
            LOG.log(DEBUG_JNA_LOAD_LEVEL, "Found jnidispatch at {0}", jnidispatchPath);

            // Attempt to delete immediately once jnidispatch is successfully
            // loaded.  This avoids the complexity of trying to do so on "exit",
            // which point can vary under different circumstances (native
            // compilation, dynamically loaded modules, normal application, etc).
            if (isUnpacked(lib)
                && !Boolean.getBoolean("jnidispatch.preserve")) {
                deleteLibrary(lib);
            }
        }
        catch(IOException e) {
            throw new UnsatisfiedLinkError(e.getMessage());
        }
    }

    /** Identify temporary files unpacked from classpath jar files. */
    static boolean isUnpacked(File file) {
        return file.getName().startsWith(JNA_TMPLIB_PREFIX);
    }

    /** Attempt to extract a native library from the current resource path,
     * using the current thread context class loader.
     * @param name Base name of native library to extract.  May also be an
     * absolute resource path (i.e. starts with "/"), in which case the
     * no transformations of the library name are performed.  If only the base
     * name is given, the resource path is attempted both with and without
     * {@link Platform#RESOURCE_PREFIX}, after mapping the library name via
     * {@link NativeLibrary#mapSharedLibraryName(String)}.
     * @return File indicating extracted resource on disk
     * @throws IOException if resource not found
     */
    public static File extractFromResourcePath(String name) throws IOException {
        return extractFromResourcePath(name, null);
    }

    /** Attempt to extract a native library from the resource path using the
     * given class loader.
     * @param name Base name of native library to extract.  May also be an
     * absolute resource path (i.e. starts with "/"), in which case the
     * no transformations of the library name are performed.  If only the base
     * name is given, the resource path is attempted both with and without
     * {@link Platform#RESOURCE_PREFIX}, after mapping the library name via
     * {@link NativeLibrary#mapSharedLibraryName(String)}.
     * @param loader Class loader to use to load resources
     * @return File indicating extracted resource on disk
     * @throws IOException if resource not found
     */
    public static File extractFromResourcePath(String name, ClassLoader loader) throws IOException {

        final Level DEBUG = (DEBUG_LOAD
            || (DEBUG_JNA_LOAD && name.contains("jnidispatch"))) ? Level.INFO : Level.FINE;
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
            // Context class loader is not guaranteed to be set
            if (loader == null) {
                loader = Native.class.getClassLoader();
            }
        }
        LOG.log(DEBUG, "Looking in classpath from {0} for {1}", new Object[]{loader, name});
        String libname = name.startsWith("/") ? name : NativeLibrary.mapSharedLibraryName(name);
        String resourcePath = name.startsWith("/") ? name : Platform.RESOURCE_PREFIX + "/" + libname;
        if (resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }
        URL url = loader.getResource(resourcePath);
        if (url == null) {
            if (resourcePath.startsWith(Platform.RESOURCE_PREFIX)) {
                // Fallback for legacy darwin behaviour: darwin was in the past
                // special cased in that all architectures were mapped to the same
                // prefix and it was expected, that a fat binary was present at that
                // point, that contained all architectures.
                if(Platform.RESOURCE_PREFIX.startsWith("darwin")) {
                    url = loader.getResource("darwin/" + resourcePath.substring(Platform.RESOURCE_PREFIX.length() + 1));
                }
                if (url == null) {
                    // If not found with the standard resource prefix, try without it
                    url = loader.getResource(libname);
                }
            } else if (resourcePath.startsWith("com/sun/jna/" + Platform.RESOURCE_PREFIX + "/")) {
                // Fallback for legacy darwin behaviour: darwin was in the past
                // special cased in that all architectures were mapped to the same
                // prefix and it was expected, that a fat binary was present at that
                // point, that contained all architectures.
                if(Platform.RESOURCE_PREFIX.startsWith("com/sun/jna/darwin")) {
                    url = loader.getResource("com/sun/jna/darwin" + resourcePath.substring(("com/sun/jna/" + Platform.RESOURCE_PREFIX).length() + 1));
                }
                if (url == null) {
                    // If not found with the standard resource prefix, try without it
                    url = loader.getResource(libname);
                }
            }
        }
        if (url == null) {
            String path = System.getProperty("java.class.path");
            if (loader instanceof URLClassLoader) {
                path = Arrays.asList(((URLClassLoader)loader).getURLs()).toString();
            }
            throw new IOException("Native library (" + resourcePath + ") not found in resource path (" + path + ")");
        }
        LOG.log(DEBUG, "Found library resource at {0}", url);

        File lib = null;
        if (url.getProtocol().toLowerCase().equals("file")) {
            try {
                lib = new File(new URI(url.toString()));
            }
            catch(URISyntaxException e) {
                lib = new File(url.getPath());
            }
            LOG.log(DEBUG, "Looking in {0}", lib.getAbsolutePath());
            if (!lib.exists()) {
                throw new IOException("File URL " + url + " could not be properly decoded");
            }
        }
        else if (!Boolean.getBoolean("jna.nounpack")) {
            InputStream is = url.openStream();
            if (is == null) {
                throw new IOException("Can't obtain InputStream for " + resourcePath);
            }

            FileOutputStream fos = null;
            try {
                // Suffix is required on windows, or library fails to load
                // Let Java pick the suffix, except on windows, to avoid
                // problems with Web Start.
                File dir = getTempDir();
                lib = File.createTempFile(JNA_TMPLIB_PREFIX, Platform.isWindows()?".dll":null, dir);
                if (!Boolean.getBoolean("jnidispatch.preserve")) {
                    lib.deleteOnExit();
                }
                LOG.log(DEBUG, "Extracting library to {0}", lib.getAbsolutePath());
                fos = new FileOutputStream(lib);
                int count;
                byte[] buf = new byte[1024];
                while ((count = is.read(buf, 0, buf.length)) > 0) {
                    fos.write(buf, 0, count);
                }
            }
            catch(IOException e) {
                throw new IOException("Failed to create temporary file for " + name + " library: " + e.getMessage());
            }
            finally {
                try { is.close(); } catch(IOException e) { }
                if (fos != null) {
                    try { fos.close(); } catch(IOException e) { }
                }
            }
        }
        return lib;
    }

    /**
     * Initialize field and method IDs for native methods of this class.
     * Returns the size of a native pointer.
     **/
    private static native int sizeof(int type);

    private static native String getNativeVersion();
    private static native String getAPIChecksum();

    /** Retrieve last error set by the OS.  This corresponds to
     * <code>GetLastError()</code> on Windows, and <code>errno</code> on
     * most other platforms.  The value is preserved per-thread, but whether
     * the original value is per-thread depends on the underlying OS.
     * <p>
     * An alternative method of obtaining the last error result is
     * to declare your mapped method to throw {@link LastErrorException}
     * instead.  If a method's signature includes a throw of {@link
     * LastErrorException}, the last error will be set to zero before the
     * native call and a {@link LastErrorException} will be raised if the last
     * error value is non-zero after the call, regardless of the actual
     * returned value from the native function.</p>
     */
    public static native int getLastError();

    /** Set the OS last error code.  The value will be saved on a per-thread
     * basis.
     */
    public static native void setLastError(int code);

    /**
     * Returns a synchronized (thread-safe) library backed by the specified
     * library.  This wrapping will prevent simultaneous invocations of any
     * functions mapped to a given {@link NativeLibrary}.  Note that the
     * native library may still be sensitive to being called from different
     * threads.
     * <p>
     * @param  library the library to be "wrapped" in a synchronized library.
     * @return a synchronized view of the specified library.
     */
    public static Library synchronizedLibrary(final Library library) {
        Class<?> cls = library.getClass();
        if (!Proxy.isProxyClass(cls)) {
            throw new IllegalArgumentException("Library must be a proxy class");
        }
        InvocationHandler ih = Proxy.getInvocationHandler(library);
        if (!(ih instanceof Library.Handler)) {
            throw new IllegalArgumentException("Unrecognized proxy handler: " + ih);
        }
        final Library.Handler handler = (Library.Handler)ih;
        InvocationHandler newHandler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                synchronized(handler.getNativeLibrary()) {
                    return handler.invoke(library, method, args);
                }
            }
        };
        return (Library)Proxy.newProxyInstance(cls.getClassLoader(),
                                               cls.getInterfaces(),
                                               newHandler);
    }

    /** If running web start, determine the location of a given native
     * library.  This value may be used to properly set
     * <code>jna.library.path</code> so that JNA can load libraries identified
     * by the &lt;nativelib&gt; tag in the JNLP configuration file.  Returns
     * <code>null</code> if the Web Start native library cache location can not
     * be determined.  Note that the path returned may be different for any
     * given library name.
     * <p>
     * Use <code>System.getProperty("javawebstart.version")</code> to detect
     * whether your code is running under Web Start.
     * @throws UnsatisfiedLinkError if the library can't be found by the
     * Web Start class loader, which usually means it wasn't included as
     * a <code>&lt;nativelib&gt;</code> resource in the JNLP file.
     * @return null if unable to query the web start loader.
     */
    public static String getWebStartLibraryPath(final String libName) {
        if (System.getProperty("javawebstart.version") == null)
            return null;
        try {

            final ClassLoader cl = Native.class.getClassLoader();
            Method m = (Method) accessControllerDoPrivileged.invoke(new PrivilegedAction<Method>() {
                @Override
                public Method run() {
                    try {
                        Method m = ClassLoader.class.getDeclaredMethod("findLibrary", new Class[] { String.class });
                        m.setAccessible(true);
                        return m;
                    }
                    catch(Exception e) {
                        return null;
                    }
                }
            });
            String libpath = (String)m.invoke(cl, new Object[] { libName });
            if (libpath != null) {
                return new File(libpath).getParent();
            }
            return null;
        }
        catch (Throwable e) {
            return null;
        }
    }

    /** Perform cleanup of automatically unpacked native shared library.
     */
    static void markTemporaryFile(File file) {
        // If we can't force an unload/delete, flag the file for later
        // deletion
        try {
            File marker = new File(file.getParentFile(), file.getName() + ".x");
            marker.createNewFile();
        }
        catch(IOException e) { e.printStackTrace(); }
    }

    /** Obtain a directory suitable for writing JNA-specific temporary files.
        Override with <code>jna.tmpdir</code>
    */
    static File getTempDir() throws IOException {
        File jnatmp;
        String prop = System.getProperty("jna.tmpdir");
        if (prop != null) {
            jnatmp = new File(prop);
            jnatmp.mkdirs();
        }
        else {
            File tmp = new File(System.getProperty("java.io.tmpdir"));
            if(Platform.isMac()) {
                // https://developer.apple.com/library/archive/documentation/FileManagement/Conceptual/FileSystemProgrammingGuide/MacOSXDirectories/MacOSXDirectories.html
                jnatmp = new File(System.getProperty("user.home"), "Library/Caches/JNA/temp");
            } else if (Platform.isLinux() || Platform.isSolaris() || Platform.isAIX() || Platform.isDragonFlyBSD() || Platform.isFreeBSD() || Platform.isNetBSD() || Platform.isOpenBSD() || Platform.iskFreeBSD()) {
                // https://standards.freedesktop.org/basedir-spec/basedir-spec-latest.html
                // The XDG_CACHE_DIR is expected to be per user
                String xdgCacheEnvironment = System.getenv("XDG_CACHE_HOME");
                File xdgCacheFile;
                if(xdgCacheEnvironment == null || xdgCacheEnvironment.trim().isEmpty()) {
                    xdgCacheFile = new File(System.getProperty("user.home"), ".cache");
                } else {
                    xdgCacheFile = new File(xdgCacheEnvironment);
                }
                jnatmp = new File(xdgCacheFile, "JNA/temp");
            } else {
                // Loading DLLs via System.load() under a directory with a unicode
                // name will fail on windows, so use a hash code of the user's
                // name in case the user's name contains non-ASCII characters
                jnatmp = new File(tmp, "jna-" + System.getProperty("user.name").hashCode());
            }

            jnatmp.mkdirs();
            if (!jnatmp.exists() || !jnatmp.canWrite()) {
                jnatmp = tmp;
            }
        }
        if (!jnatmp.exists()) {
            throw new IOException("JNA temporary directory '" + jnatmp + "' does not exist");
        }
        if (!jnatmp.canWrite()) {
            throw new IOException("JNA temporary directory '" + jnatmp + "' is not writable");
        }
        return jnatmp;
    }

    /** Remove all marked temporary files in the given directory. */
    static void removeTemporaryFiles() throws IOException {
        File dir = getTempDir();
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".x") && name.startsWith(JNA_TMPLIB_PREFIX);
            }
        };
        File[] files = dir.listFiles(filter);
        for (int i=0;files != null && i < files.length;i++) {
            File marker = files[i];
            String name = marker.getName();
            name = name.substring(0, name.length()-2);
            File target = new File(marker.getParentFile(), name);
            if (!target.exists() || target.delete()) {
                marker.delete();
            }
        }
    }

    /**
     * @param type The Java class for which the native size is to be determined
     * @param value an instance of said class (if available)
     * @return the native size of the given class, in bytes.
     * For use with arrays.
     */
    public static int getNativeSize(Class<?> type, Object value) {
        if (type.isArray()) {
            int len = Array.getLength(value);
            if (len > 0) {
                Object o = Array.get(value, 0);
                return len * getNativeSize(type.getComponentType(), o);
            }
            // Don't process zero-length arrays
            throw new IllegalArgumentException("Arrays of length zero not allowed: " + type);
        }
        if (Structure.class.isAssignableFrom(type)
            && !Structure.ByReference.class.isAssignableFrom(type)) {
            return Structure.size((Class<Structure>) type, (Structure)value);
        }
        try {
            return getNativeSize(type);
        }
        catch(IllegalArgumentException e) {
            throw new IllegalArgumentException("The type \"" + type.getName()
                                               + "\" is not supported: "
                                               + e.getMessage());
        }
    }

    /**
     * Returns the native size for a given Java class.  Structures are
     * assumed to be <code>struct</code> pointers unless they implement
     * {@link Structure.ByValue}.
     *
     * @param cls The Java class
     * @return The native size for the class
     */
    public static int getNativeSize(Class<?> cls) {
        if (NativeMapped.class.isAssignableFrom(cls)) {
            cls = NativeMappedConverter.getInstance(cls).nativeType();
        }
        // boolean defaults to 32 bit integer if not otherwise mapped
        if (cls == boolean.class || cls == Boolean.class) return 4;
        if (cls == byte.class || cls == Byte.class) return 1;
        if (cls == short.class || cls == Short.class) return 2;
        if (cls == char.class || cls == Character.class) return WCHAR_SIZE;
        if (cls == int.class || cls == Integer.class) return 4;
        if (cls == long.class || cls == Long.class) return 8;
        if (cls == float.class || cls == Float.class) return 4;
        if (cls == double.class || cls == Double.class) return 8;
        if (Structure.class.isAssignableFrom(cls)) {
            if (Structure.ByValue.class.isAssignableFrom(cls)) {
                return Structure.size((Class<? extends Structure>) cls);
            }
            return POINTER_SIZE;
        }
        if (Pointer.class.isAssignableFrom(cls)
            || (Platform.HAS_BUFFERS && Buffers.isBuffer(cls))
            || Callback.class.isAssignableFrom(cls)
            || String.class == cls
            || WString.class == cls) {
            return POINTER_SIZE;
        }
        throw new IllegalArgumentException("Native size for type \"" + cls.getName()
                                           + "\" is unknown");
    }

    /**
     * @param cls The Java class
     * @return {@code true} whether the given class is supported as a native argument type.
     */
    public static boolean isSupportedNativeType(Class<?> cls) {
        if (Structure.class.isAssignableFrom(cls)) {
            return true;
        }
        try {
            return getNativeSize(cls) != 0;
        }
        catch(IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Set the default handler invoked when a callback throws an uncaught
     * exception.  If the given handler is <code>null</code>, the default
     * handler will be reinstated.
     *
     * @param eh The default handler
     */
    public static void setCallbackExceptionHandler(UncaughtExceptionHandler eh) {
        callbackExceptionHandler = eh == null ? DEFAULT_HANDLER : eh;
    }

    /** @return the current handler for callback uncaught exceptions. */
    public static UncaughtExceptionHandler getCallbackExceptionHandler() {
        return callbackExceptionHandler;
    }

    /**
     * When called from a class static initializer, maps all native methods
     * found within that class to native libraries via the JNA raw calling
     * interface.
     * @param libName library name to which functions should be bound
     */
    public static void register(String libName) {
        register(findDirectMappedClass(getCallingClass()), libName);
    }

    /**
     * When called from a class static initializer, maps all native methods
     * found within that class to native libraries via the JNA raw calling
     * interface.
     * @param lib native library to which functions should be bound
     */
    public static void register(NativeLibrary lib) {
        register(findDirectMappedClass(getCallingClass()), lib);
    }

    /** Find the nearest enclosing class with native methods. */
    static Class<?> findDirectMappedClass(Class<?> cls) {
        Method[] methods = cls.getDeclaredMethods();
        for (Method m : methods) {
            if ((m.getModifiers() & Modifier.NATIVE) != 0) {
                return cls;
            }
        }
        int idx = cls.getName().lastIndexOf("$");
        if (idx != -1) {
            String name = cls.getName().substring(0, idx);
            try {
                return findDirectMappedClass(Class.forName(name, true, cls.getClassLoader()));
            } catch(ClassNotFoundException e) {
                // ignored
            }
        }
        throw new IllegalArgumentException("Can't determine class with native methods from the current context (" + cls + ")");
    }

    /** Try to determine the class context in which a {@link #register(String)} call
        was made.
    */
    static Class<?> getCallingClass() {
        if (stackWalkerGetInstance != null) {
            try {
                Object walker = stackWalkerGetInstance.invoke(stackWalkerRetainClassReference);
                Class<?> caller = (Class<?>) stackWalkerWalk.invoke(walker, stackWalkerFilter);
                return caller;
            } catch (Throwable ex) {
                LOG.log(Level.WARNING, "Failed to invoke StackWalker#getInstance or StackWalker#walk", ex);
            }
        }

        if (securityManagerExposerConstructor != null) {
            Class<?>[] context = null;
            try {
                Object securityManagerExposer = securityManagerExposerConstructor.invoke();
                context = (Class<?>[]) securityManagerGetClassContext.invoke(securityManagerExposer);
            } catch (Throwable ex) {
                LOG.log(Level.WARNING, "Failed to invoke SecurityManagerExposer#<init> or SecurityManagerExposer#getClassContext", ex);
            }

            if (context == null) {
                throw new IllegalStateException("The SecurityManager implementation on this platform is broken; you must explicitly provide the class to register");
            }
            if (context.length < 4) {
                throw new IllegalStateException("This method must be called from the static initializer of a class");
            }
            return context[3];
        }

        throw new IllegalStateException("Neither the StackWalker, nor the SecurityManager based getCallingClass implementation are useable; you must explicitly provide the class to register");
    }

    /**
     * Set a thread initializer for the given callback.
     * @param cb The callback to invoke
     * @param initializer The thread initializer indicates desired thread configuration when the
     * given Callback is invoked on a native thread not yet attached to the VM.
     */
    public static void setCallbackThreadInitializer(Callback cb, CallbackThreadInitializer initializer) {
        CallbackReference.setCallbackThreadInitializer(cb, initializer);
    }

    private static final Map<Class<?>, long[]> registeredClasses = new WeakHashMap<>();
    private static final Map<Class<?>, NativeLibrary> registeredLibraries = new WeakHashMap<>();

    private static void unregisterAll() {
        synchronized(registeredClasses) {
            for (Map.Entry<Class<?>, long[]> e : registeredClasses.entrySet()) {
                unregister(e.getKey(), e.getValue());
            }

            registeredClasses.clear();
        }
    }

    /** Remove all native mappings for the calling class.
        Should only be called if the class is no longer referenced and about
        to be garbage collected.
     */
    public static void unregister() {
        unregister(findDirectMappedClass(getCallingClass()));
    }

    /** Remove all native mappings for the given class.
        Should only be called if the class is no longer referenced and about
        to be garbage collected.
     */
    public static void unregister(Class<?> cls) {
        synchronized(registeredClasses) {
            long[] handles = registeredClasses.get(cls);
            if (handles != null) {
                unregister(cls, handles);
                registeredClasses.remove(cls);
                registeredLibraries.remove(cls);
            }
        }
    }

    /**
     * @param cls The type {@link Class}
     * @return whether the given class's native components are registered.
     */
    public static boolean registered(Class<?> cls) {
        synchronized(registeredClasses) {
            return registeredClasses.containsKey(cls);
        }
    }

    /* Unregister the native methods for the given class. */
    private static native void unregister(Class<?> cls, long[] handles);

    static String getSignature(Class<?> cls) {
        if (cls.isArray()) {
            return "[" + getSignature(cls.getComponentType());
        }
        if (cls.isPrimitive()) {
            if (cls == void.class) return "V";
            if (cls == boolean.class) return "Z";
            if (cls == byte.class) return "B";
            if (cls == short.class) return "S";
            if (cls == char.class) return "C";
            if (cls == int.class) return "I";
            if (cls == long.class) return "J";
            if (cls == float.class) return "F";
            if (cls == double.class) return "D";
        }
        return "L" + replace(".", "/", cls.getName()) + ";";
    }

    // No String.replace available in 1.4
    static String replace(String s1, String s2, String str) {
        StringBuilder buf = new StringBuilder();
        while (true) {
            int idx = str.indexOf(s1);
            if (idx == -1) {
                buf.append(str);
                break;
            }
            else {
                buf.append(str.substring(0, idx));
                buf.append(s2);
                str = str.substring(idx + s1.length());
            }
        }
        return buf.toString();
    }

    /** Indicates whether the callback has an initializer. */
    static final int CB_HAS_INITIALIZER = 1;

    private static final int CVT_UNSUPPORTED = -1;
    private static final int CVT_DEFAULT = 0;
    private static final int CVT_POINTER = 1;
    private static final int CVT_STRING = 2;
    private static final int CVT_STRUCTURE = 3;
    private static final int CVT_STRUCTURE_BYVAL = 4;
    private static final int CVT_BUFFER = 5;
    private static final int CVT_ARRAY_BYTE = 6;
    private static final int CVT_ARRAY_SHORT = 7;
    private static final int CVT_ARRAY_CHAR = 8;
    private static final int CVT_ARRAY_INT = 9;
    private static final int CVT_ARRAY_LONG = 10;
    private static final int CVT_ARRAY_FLOAT = 11;
    private static final int CVT_ARRAY_DOUBLE = 12;
    private static final int CVT_ARRAY_BOOLEAN = 13;
    private static final int CVT_BOOLEAN = 14;
    private static final int CVT_CALLBACK = 15;
    private static final int CVT_FLOAT = 16;
    private static final int CVT_NATIVE_MAPPED = 17;
    private static final int CVT_NATIVE_MAPPED_STRING = 18;
    private static final int CVT_NATIVE_MAPPED_WSTRING = 19;
    private static final int CVT_WSTRING = 20;
    private static final int CVT_INTEGER_TYPE = 21;
    private static final int CVT_POINTER_TYPE = 22;
    private static final int CVT_TYPE_MAPPER = 23;
    private static final int CVT_TYPE_MAPPER_STRING = 24;
    private static final int CVT_TYPE_MAPPER_WSTRING = 25;
    private static final int CVT_OBJECT = 26;
    private static final int CVT_JNIENV = 27;
    private static final int CVT_SHORT = 28;
    private static final int CVT_BYTE = 29;

    private static int getConversion(Class<?> type, TypeMapper mapper, boolean allowObjects) {
        if (type == Void.class) type = void.class;

        if (mapper != null) {
            FromNativeConverter fromNative = mapper.getFromNativeConverter(type);
            ToNativeConverter toNative = mapper.getToNativeConverter(type);
            if (fromNative != null) {
                Class<?> nativeType = fromNative.nativeType();
                if (nativeType == String.class) {
                    return CVT_TYPE_MAPPER_STRING;
                }
                if (nativeType == WString.class) {
                    return CVT_TYPE_MAPPER_WSTRING;
                }
                return CVT_TYPE_MAPPER;
            }
            if (toNative != null) {
                Class<?> nativeType = toNative.nativeType();
                if (nativeType == String.class) {
                    return CVT_TYPE_MAPPER_STRING;
                }
                if (nativeType == WString.class) {
                    return CVT_TYPE_MAPPER_WSTRING;
                }
                return CVT_TYPE_MAPPER;
            }
        }

        if (Pointer.class.isAssignableFrom(type)) {
            return CVT_POINTER;
        }
        if (String.class == type) {
            return CVT_STRING;
        }
        if (WString.class.isAssignableFrom(type)) {
            return CVT_WSTRING;
        }
        if (Platform.HAS_BUFFERS && Buffers.isBuffer(type)) {
            return CVT_BUFFER;
        }
        if (Structure.class.isAssignableFrom(type)) {
            if (Structure.ByValue.class.isAssignableFrom(type)) {
                return CVT_STRUCTURE_BYVAL;
            }
            return CVT_STRUCTURE;
        }
        if (type.isArray()) {
            switch(type.getName().charAt(1)) {
                case 'Z': return CVT_ARRAY_BOOLEAN;
                case 'B': return CVT_ARRAY_BYTE;
                case 'S': return CVT_ARRAY_SHORT;
                case 'C': return CVT_ARRAY_CHAR;
                case 'I': return CVT_ARRAY_INT;
                case 'J': return CVT_ARRAY_LONG;
                case 'F': return CVT_ARRAY_FLOAT;
                case 'D': return CVT_ARRAY_DOUBLE;
                default: break;
            }
        }
        if (type.isPrimitive()) {
            return type == boolean.class ? CVT_BOOLEAN : CVT_DEFAULT;
        }
        if (Callback.class.isAssignableFrom(type)) {
            return CVT_CALLBACK;
        }
        if (IntegerType.class.isAssignableFrom(type)) {
            return CVT_INTEGER_TYPE;
        }
        if (PointerType.class.isAssignableFrom(type)) {
            return CVT_POINTER_TYPE;
        }
        if (NativeMapped.class.isAssignableFrom(type)) {
            Class<?> nativeType = NativeMappedConverter.getInstance(type).nativeType();
            if (nativeType == String.class) {
                return CVT_NATIVE_MAPPED_STRING;
            }
            if (nativeType == WString.class) {
                return CVT_NATIVE_MAPPED_WSTRING;
            }
            return CVT_NATIVE_MAPPED;
        }
        if (JNIEnv.class == type) {
            return CVT_JNIENV;
        }
        return allowObjects ? CVT_OBJECT : CVT_UNSUPPORTED;
    }

    /**
     * When called from a class static initializer, maps all native methods
     * found within that class to native libraries via the JNA raw calling
     * interface.  Uses the class loader of the given class to search for the
     * native library in the resource path if it is not found in the system
     * library load path or <code>jna.library.path</code>.
     * @param cls Class with native methods to register
     * @param libName name of or path to native library to which functions
     * should be bound
     */
    public static void register(Class<?> cls, String libName) {
        NativeLibrary library =
                NativeLibrary.getInstance(libName, Collections.singletonMap(Library.OPTION_CLASSLOADER, cls.getClassLoader()));
        register(cls, library);
    }

    /** When called from a class static initializer, maps all native methods
     * found within that class to native libraries via the JNA raw calling
     * interface.
     * @param cls Class with native methods to register
     * @param lib library to which functions should be bound
     */
    // TODO: derive options from annotations (per-class or per-method)
    // options: read parameter type mapping (long/native long),
    // method name, library name, call conv
    public static void register(Class<?> cls, NativeLibrary lib) {
        Method[] methods = cls.getDeclaredMethods();
        List<Method> mlist = new ArrayList<>();
        Map<String, ?> options = lib.getOptions();
        TypeMapper mapper = (TypeMapper) options.get(Library.OPTION_TYPE_MAPPER);
        boolean allowObjects = Boolean.TRUE.equals(options.get(Library.OPTION_ALLOW_OBJECTS));
        options = cacheOptions(cls, options, null);

        for (Method m : methods) {
            if ((m.getModifiers() & Modifier.NATIVE) != 0) {
                mlist.add(m);
            }
        }

        long[] handles = new long[mlist.size()];
        for (int i=0;i < handles.length;i++) {
            Method method = mlist.get(i);
            String sig = "(";
            Class<?> rclass = method.getReturnType();
            long rtype, closure_rtype;
            Class<?>[] ptypes = method.getParameterTypes();
            long[] atypes = new long[ptypes.length];
            long[] closure_atypes = new long[ptypes.length];
            int[] cvt = new int[ptypes.length];
            ToNativeConverter[] toNative = new ToNativeConverter[ptypes.length];
            FromNativeConverter fromNative = null;
            int rcvt = getConversion(rclass, mapper, allowObjects);
            boolean throwLastError = false;
            switch (rcvt) {
                case CVT_UNSUPPORTED:
                    throw new IllegalArgumentException(rclass + " is not a supported return type (in method " + method.getName() + " in " + cls + ")");
                case CVT_TYPE_MAPPER:
                case CVT_TYPE_MAPPER_STRING:
                case CVT_TYPE_MAPPER_WSTRING:
                    fromNative = mapper.getFromNativeConverter(rclass);
                    // FFIType.get() always looks up the native type for any given
                    // class, so if we actually have conversion into a Java
                    // object, make sure we use the proper type information
                    closure_rtype = FFIType.get(rclass.isPrimitive() ? rclass : Pointer.class).getPointer().peer;
                    rtype = FFIType.get(fromNative.nativeType()).getPointer().peer;
                    break;
                case CVT_NATIVE_MAPPED:
                case CVT_NATIVE_MAPPED_STRING:
                case CVT_NATIVE_MAPPED_WSTRING:
                case CVT_INTEGER_TYPE:
                case CVT_POINTER_TYPE:
                    closure_rtype = FFIType.get(Pointer.class).getPointer().peer;
                    rtype = FFIType.get(NativeMappedConverter.getInstance(rclass).nativeType()).getPointer().peer;
                    break;
                case CVT_STRUCTURE:
                case CVT_OBJECT:
                    closure_rtype = rtype = FFIType.get(Pointer.class).getPointer().peer;
                    break;
                case CVT_STRUCTURE_BYVAL:
                    closure_rtype = FFIType.get(Pointer.class).getPointer().peer;
                    rtype = FFIType.get(rclass).getPointer().peer;
                    break;
                default:
                    closure_rtype = rtype = FFIType.get(rclass).getPointer().peer;
            }

            for (int t=0;t < ptypes.length;t++) {
                Class<?> type = ptypes[t];
                sig += getSignature(type);
                int conversionType = getConversion(type, mapper, allowObjects);
                cvt[t] = conversionType;
                if (conversionType == CVT_UNSUPPORTED) {
                    throw new IllegalArgumentException(type + " is not a supported argument type (in method " + method.getName() + " in " + cls + ")");
                }
                if ((conversionType == CVT_NATIVE_MAPPED)
                    || (conversionType == CVT_NATIVE_MAPPED_STRING)
                    || (conversionType == CVT_NATIVE_MAPPED_WSTRING)
                    || (conversionType == CVT_INTEGER_TYPE)) {
                    type = NativeMappedConverter.getInstance(type).nativeType();
                } else if ((conversionType == CVT_TYPE_MAPPER)
                        || (conversionType == CVT_TYPE_MAPPER_STRING)
                        || (conversionType == CVT_TYPE_MAPPER_WSTRING)) {
                    toNative[t] = mapper.getToNativeConverter(type);
                }

                // Determine the type that will be passed to the native
                // function, as well as the type to be passed
                // from Java initially
                switch(conversionType) {
                    case CVT_STRUCTURE_BYVAL:
                    case CVT_INTEGER_TYPE:
                    case CVT_POINTER_TYPE:
                    case CVT_NATIVE_MAPPED:
                    case CVT_NATIVE_MAPPED_STRING:
                    case CVT_NATIVE_MAPPED_WSTRING:
                        atypes[t] = FFIType.get(type).getPointer().peer;
                        closure_atypes[t] = FFIType.get(Pointer.class).getPointer().peer;
                        break;
                    case CVT_TYPE_MAPPER:
                    case CVT_TYPE_MAPPER_STRING:
                    case CVT_TYPE_MAPPER_WSTRING:
                        closure_atypes[t] = FFIType.get(type.isPrimitive() ? type : Pointer.class).getPointer().peer;
                        atypes[t] = FFIType.get(toNative[t].nativeType()).getPointer().peer;
                        break;
                    case CVT_DEFAULT:
                        closure_atypes[t] = atypes[t] = FFIType.get(type).getPointer().peer;
                        break;
                    default:
                        closure_atypes[t] = atypes[t] = FFIType.get(Pointer.class).getPointer().peer;
                }
            }
            sig += ")";
            sig += getSignature(rclass);

            Class<?>[] etypes = method.getExceptionTypes();
            for (int e=0;e < etypes.length;e++) {
                if (LastErrorException.class.isAssignableFrom(etypes[e])) {
                    throwLastError = true;
                    break;
                }
            }

            Function f = lib.getFunction(method.getName(), method);
            try {
                handles[i] = registerMethod(cls, method.getName(),
                                            sig, cvt,
                                            closure_atypes, atypes, rcvt,
                                            closure_rtype, rtype,
                                            method,
                                            f.peer, f.getCallingConvention(),
                                            throwLastError,
                                            toNative, fromNative,
                                            f.encoding);
            } catch(NoSuchMethodError e) {
                throw new UnsatisfiedLinkError("No method " + method.getName() + " with signature " + sig + " in " + cls);
            }
        }
        synchronized(registeredClasses) {
            registeredClasses.put(cls, handles);
            registeredLibraries.put(cls, lib);
        }
    }

    /**
     * Get the {@link NativeLibrary} instance that is wrapped by the given
     * {@link Library} interface instance.
     *
     * @param library the {@link Library} interface instance, which was created
     * by the {@link Native#load Native.load()} method
     * @return the wrapped {@link NativeLibrary} instance
     */
    public static NativeLibrary getNativeLibrary(final Library library) {
        if(library == null) {
            throw new IllegalArgumentException("null passed to getNativeLibrary");
        }
        if(! Proxy.isProxyClass(library.getClass())) {
            throw new IllegalArgumentException("library object passed to getNativeLibrary in not a proxy");
        }
        final InvocationHandler handler = Proxy.getInvocationHandler(library);
        if (!(handler instanceof Library.Handler)) {
            throw new IllegalArgumentException("Object is not a properly initialized Library interface instance");
        }
        return ((Library.Handler) handler).getNativeLibrary();
    }

    /**
     * Get the {@link NativeLibrary} instance to which the given "registered"
     * class is bound.
     *
     * @param cls the "registered" class, which was previously registered via
     * the {@link Native#register register()} method
     * @return the {@link NativeLibrary} instance to which the "registered"
     * class is bound
     */
    public static NativeLibrary getNativeLibrary(final Class<?> cls) {
        if(cls == null) {
            throw new IllegalArgumentException("null passed to getNativeLibrary");
        }
        final Class<?> mappedClass = findDirectMappedClass(cls);
        synchronized(registeredClasses) {
            final NativeLibrary nativeLibrary = registeredLibraries.get(mappedClass);
            if (nativeLibrary == null) {
                throw new IllegalArgumentException("Class " + cls.getName() + " is not currently registered");
            } else {
                return nativeLibrary;
            }
        }
    }

    /* Take note of options used for a given library mapping, to facilitate
     * looking them up later.
     */
    private static Map<String, Object> cacheOptions(Class<?> cls, Map<String, ?> options, Object proxy) {
        Map<String, Object> libOptions = new HashMap<>(options);
        libOptions.put(_OPTION_ENCLOSING_LIBRARY, cls);
        typeOptions.put(cls, libOptions);
        if (proxy != null) {
            libraries.put(cls, new WeakReference<>(proxy));
        }

        // If it's a direct mapping, AND implements a Library interface,
        // cache the library interface as well, so that any nested
        // classes get the appropriate associated options
        if (!cls.isInterface()
            && Library.class.isAssignableFrom(cls)) {
            Class<?> ifaces[] = cls.getInterfaces();
            for (Class<?> ifc : ifaces) {
                if (Library.class.isAssignableFrom(ifc)) {
                    cacheOptions(ifc, libOptions, proxy);
                    break;
                }
            }
        }
        return libOptions;
    }

    private static native long registerMethod(Class<?> cls,
                                              String name,
                                              String signature,
                                              int[] conversions,
                                              long[] closure_arg_types,
                                              long[] arg_types,
                                              int rconversion,
                                              long closure_rtype,
                                              long rtype,
                                              Method method,
                                              long fptr,
                                              int callingConvention,
                                              boolean throwLastError,
                                              ToNativeConverter[] toNative,
                                              FromNativeConverter fromNative,
                                              String encoding);


    // Called from native code
    private static NativeMapped fromNative(Class<?> cls, Object value) {
        // NOTE: technically should be CallbackParameterContext
        return (NativeMapped)NativeMappedConverter.getInstance(cls).fromNative(value, new FromNativeContext(cls));
    }
    // Called from native code
    private static NativeMapped fromNative(Method m, Object value) {
        Class<?> cls = m.getReturnType();
        return (NativeMapped)NativeMappedConverter.getInstance(cls).fromNative(value, new MethodResultContext(cls, null, null, m));
    }
    // Called from native code
    private static Class<?> nativeType(Class<?> cls) {
        return NativeMappedConverter.getInstance(cls).nativeType();
    }
    // Called from native code
    private static Object toNative(ToNativeConverter cvt, Object o) {
        // NOTE: technically should be either CallbackResultContext or
        // FunctionParameterContext
        return cvt.toNative(o, new ToNativeContext());
    }
    // Called from native code
    private static Object fromNative(FromNativeConverter cvt, Object o, Method m) {
        return cvt.fromNative(o, new MethodResultContext(m.getReturnType(), null, null, m));
    }

    /** Create a new cif structure. */
    public static native long ffi_prep_cif(int abi, int nargs, long ffi_return_type, long ffi_types);
    /** Make an FFI function call. */
    public static native void ffi_call(long cif, long fptr, long resp, long args);
    public static native long ffi_prep_closure(long cif, ffi_callback cb);
    public static native void ffi_free_closure(long closure);

    /** Returns the size (calculated by libffi) of the given type. */
    static native int initialize_ffi_type(long type_info);

    public interface ffi_callback {
        void invoke(long cif, long resp, long argp);
    }

    /** Prints JNA library details to the console. */
    public static void main(String[] args) {
        final String DEFAULT_TITLE = "Java Native Access (JNA)";
        final String DEFAULT_VERSION = VERSION;
        final String DEFAULT_BUILD = VERSION + " (package information missing)";
        Package pkg = Native.class.getPackage();
        String title = pkg != null
            ? pkg.getSpecificationTitle() : DEFAULT_TITLE;
        if (title == null) title = DEFAULT_TITLE;
        String version = pkg != null
            ? pkg.getSpecificationVersion() : DEFAULT_VERSION;
        if (version == null) version = DEFAULT_VERSION;
        title += " API Version " + version;
        System.out.println(title);
        version = pkg != null
            ? pkg.getImplementationVersion() : DEFAULT_BUILD;
        if (version == null) version = DEFAULT_BUILD;
        System.out.println("Version: " + version);
        System.out.println(" Native: " + getNativeVersion() + " ("
                           + getAPIChecksum() + ")");
        System.out.println(" Prefix: " + Platform.RESOURCE_PREFIX);
    }

    /** Free the given callback trampoline. */
    static synchronized native void freeNativeCallback(long ptr);

    /** Use direct mapping for callback. */
    static final int CB_OPTION_DIRECT = 1;
    /** Return a DLL-resident fucntion pointer. */
    static final int CB_OPTION_IN_DLL = 2;

    /** Create a native trampoline to delegate execution to the Java callback.
     */
    static synchronized native long createNativeCallback(Callback callback,
                                                         Method method,
                                                         Class<?>[] parameterTypes,
                                                         Class<?> returnType,
                                                         int callingConvention,
                                                         int flags,
                                                         String encoding);

    /**
     * Call the native function.
     *
     * @param function  Present to prevent the GC to collect the Function object
     *                  prematurely
     * @param fp        function pointer
     * @param callFlags calling convention to be used
     * @param args      Arguments to pass to the native function
     *
     * @return The value returned by the target native function
     */
    static  native int invokeInt(Function function, long fp, int callFlags, Object[] args);

    /**
     * Call the native function.
     *
     * @param function  Present to prevent the GC to collect the Function object
     *                  prematurely
     * @param fp        function pointer
     * @param callFlags calling convention to be used
     * @param args      Arguments to pass to the native function
     *
     * @return The value returned by the target native function
     */
    static native long invokeLong(Function function, long fp, int callFlags, Object[] args);

    /**
     * Call the native function.
     *
     * @param function  Present to prevent the GC to collect the Function object
     *                  prematurely
     * @param fp        function pointer
     * @param callFlags calling convention to be used
     * @param args      Arguments to pass to the native function
     */
    static native void invokeVoid(Function function, long fp, int callFlags, Object[] args);

    /**
     * Call the native function.
     *
     * @param function  Present to prevent the GC to collect the Function object
     *                  prematurely
     * @param fp        function pointer
     * @param callFlags calling convention to be used
     * @param args      Arguments to pass to the native function
     *
     * @return The value returned by the target native function
     */
    static native float invokeFloat(Function function, long fp, int callFlags, Object[] args);

    /**
     * Call the native function.
     *
     * @param function  Present to prevent the GC to collect the Function object
     *                  prematurely
     * @param fp        function pointer
     * @param callFlags calling convention to be used
     * @param args      Arguments to pass to the native function
     *
     * @return The value returned by the target native function
     */
    static native double invokeDouble(Function function, long fp, int callFlags, Object[] args);

    /**
     * Call the native function.
     *
     * @param function  Present to prevent the GC to collect the Function object
     *                  prematurely
     * @param fp        function pointer
     * @param callFlags calling convention to be used
     * @param args      Arguments to pass to the native function
     *
     * @return The value returned by the target native function
     */
    static native long invokePointer(Function function, long fp, int callFlags, Object[] args);

    /**
     * Call the native function, returning a struct by value.
     *
     * @param function  Present to prevent the GC to collect the Function object
     *                  prematurely
     * @param fp        function pointer
     * @param callFlags calling convention to be used
     * @param args      Arguments to pass to the native function
     * @param memory    Memory for pre-allocated structure to hold the result
     * @param typeInfo  Native type information for the Structure
     */
    private static native void invokeStructure(Function function, long fp, int callFlags,
                                               Object[] args, long memory,
                                               long type_info);

    /**
     * Call the native function, returning a struct by value.
     *
     * @param function  Present to prevent the GC to collect the Function object
     *                  prematurely
     * @param fp        function pointer
     * @param callFlags calling convention to be used
     * @param args      Arguments to pass to the native function
     *
     * @return the passed-in Structure
     */
    static Structure invokeStructure(Function function, long fp, int callFlags, Object[] args,
                                     Structure s) {
        invokeStructure(function, fp, callFlags, args, s.getPointer().peer,
                        s.getTypeInfo().peer);
        return s;
    }

    /**
     * Call the native function, returning a Java <code>Object</code>.
     *
     * @param function  Present to prevent the GC to collect the Function object
     *                  prematurely
     * @param fp        function pointer
     * @param callFlags calling convention to be used
     * @param args      Arguments to pass to the native function
     *
     * @return  The returned Java <code>Object</code>
     */
    static native Object invokeObject(Function function, long fp, int callFlags, Object[] args);

    /** Open the requested native library with default options. */
    static long open(String name) {
        return open(name, -1);
    }

    /** Open the requested native library with the specified platform-specific
     * otions.
     */
    static native long open(String name, int flags);

    /** Close the given native library. */
    static native void close(long handle);

    static native long findSymbol(long handle, String name);

    /*
    ============================================================================

    The first argument of the following read, write, get<Type> and set<Type>
    function is present to protect it from the GC.

    Although on the native side only the baseaddr and offset are used to access
    the memory, the Pointer argument must not be removed. This is the usecase:

    --------------------------------------
    Memory pointer = <init>;
    <do something and work on Memory>
    String result = pointer.getWideString(0)
    <do nothing more with Memory>
    --------------------------------------

    In getWideString the pointer address is resolved and is passed to native. If
    the Memory object itself is not passed to native, the GC can collect the
    object at that point as it is not used anymore and the finalizers could run.

    The would introduce a race between the native call and the GC running the
    finalizers. The finalizers free the allocated memory, which results in
    a SEGFAULT.

    Passing only the Pointer object and loading the peer value via JNI was not
    implemented, as in microbenchmarks it showed large impact. Passing the
    Pointer object instead of the peer and offset value to getInt resulted in
    a performance of 70% of the unmodified source.

    ============================================================================
     */
    static native long indexOf(Pointer pointer, long baseaddr, long offset, byte value);

    static native void read(Pointer pointer, long baseaddr, long offset, byte[] buf, int index, int length);

    static native void read(Pointer pointer, long baseaddr, long offset, short[] buf, int index, int length);

    static native void read(Pointer pointer, long baseaddr, long offset, char[] buf, int index, int length);

    static native void read(Pointer pointer, long baseaddr, long offset, int[] buf, int index, int length);

    static native void read(Pointer pointer, long baseaddr, long offset, long[] buf, int index, int length);

    static native void read(Pointer pointer, long baseaddr, long offset, float[] buf, int index, int length);

    static native void read(Pointer pointer, long baseaddr, long offset, double[] buf, int index, int length);

    static native void write(Pointer pointer, long baseaddr, long offset, byte[] buf, int index, int length);

    static native void write(Pointer pointer, long baseaddr, long offset, short[] buf, int index, int length);

    static native void write(Pointer pointer, long baseaddr, long offset, char[] buf, int index, int length);

    static native void write(Pointer pointer, long baseaddr, long offset, int[] buf, int index, int length);

    static native void write(Pointer pointer, long baseaddr, long offset, long[] buf, int index, int length);

    static native void write(Pointer pointer, long baseaddr, long offset, float[] buf, int index, int length);

    static native void write(Pointer pointer, long baseaddr, long offset, double[] buf, int index, int length);

    static native byte getByte(Pointer pointer, long baseaddr, long offset);

    static native char getChar(Pointer pointer, long baseaddr, long offset);

    static native short getShort(Pointer pointer, long baseaddr, long offset);

    static native int getInt(Pointer pointer, long baseaddr, long offset);

    static native long getLong(Pointer pointer, long baseaddr, long offset);

    static native float getFloat(Pointer pointer, long baseaddr, long offset);

    static native double getDouble(Pointer pointer, long baseaddr, long offset);

    static Pointer getPointer(long addr) {
        long peer = _getPointer(addr);
        return peer == 0 ? null : new Pointer(peer);
    }

    private static native long _getPointer(long addr);

    static native String getWideString(Pointer pointer, long baseaddr, long offset);

    static String getString(Pointer pointer, long offset) {
        return getString(pointer, offset, getDefaultStringEncoding());
    }

    static String getString(Pointer pointer, long offset, String encoding) {
        byte[] data = getStringBytes(pointer, pointer.peer, offset);
        if (encoding != null) {
            try {
                return new String(data, encoding);
            }
            catch(UnsupportedEncodingException e) {
            }
        }
        return new String(data);
    }

    static native byte[] getStringBytes(Pointer pointer, long baseaddr, long offset);

    static native void setMemory(Pointer pointer, long baseaddr, long offset, long length, byte value);

    static native void setByte(Pointer pointer, long baseaddr, long offset, byte value);

    static native void setShort(Pointer pointer, long baseaddr, long offset, short value);

    static native void setChar(Pointer pointer, long baseaddr, long offset, char value);

    static native void setInt(Pointer pointer, long baseaddr, long offset, int value);

    static native void setLong(Pointer pointer, long baseaddr, long offset, long value);

    static native void setFloat(Pointer pointer, long baseaddr, long offset, float value);

    static native void setDouble(Pointer pointer, long baseaddr, long offset, double value);

    static native void setPointer(Pointer pointer, long baseaddr, long offset, long value);

    static native void setWideString(Pointer pointer, long baseaddr, long offset, String value);

    static native ByteBuffer getDirectByteBuffer(Pointer pointer, long addr, long offset, long length);

    /**
     * Call the real native malloc
     * @param size size of the memory to be allocated
     * @return native address of the allocated memory block; zero if the
     * allocation failed.
     */
    public static native long malloc(long size);

    /**
     * Call the real native free
     * @param ptr native address to be freed; a value of zero has no effect,
     * passing an already-freed pointer will cause pain.
     */
    public static native void free(long ptr);

    private static final ThreadLocal<Memory> nativeThreadTerminationFlag =
        new ThreadLocal<Memory>() {
            @Override
            protected Memory initialValue() {
                Memory m = new Memory(4);
                m.clear();
                return m;
            }
        };
    private static final Map<Thread, Pointer> nativeThreads = Collections.synchronizedMap(new WeakHashMap<Thread, Pointer>());

    /** <p>Indicate whether the JVM should detach the current native thread when
        the current Java code finishes execution.  Generally this is used to
        avoid detaching native threads when it is known that a given thread
        will be relatively long-lived and call back to Java code frequently.
        </p>
        This call is lightweight; it only results in an additional JNI
        crossing if the desired state changes from its last setting.

        @throws IllegalStateException if {@link #detach detach(true)} is
        called on a thread created by the JVM.
     */
    public static void detach(boolean detach) {
        Thread thread = Thread.currentThread();
        if (detach) {
            // If a CallbackThreadInitializer was used to avoid detach,
            // we won't have put that thread into the nativeThreads map.
            // Performance is not as critical in that case, and since
            // detach is the default behavior, force an update of the detach
            // state every time.  Clear the termination flag, since it's not
            // needed when the native thread is detached normally.
            nativeThreads.remove(thread);
            Pointer p = nativeThreadTerminationFlag.get();
            setDetachState(true, 0);
        }
        else {
            if (!nativeThreads.containsKey(thread)) {
                Pointer p = nativeThreadTerminationFlag.get();
                nativeThreads.put(thread, p);
                setDetachState(false, p.peer);
            }
        }
    }

    static Pointer getTerminationFlag(Thread t) {
        return nativeThreads.get(t);
    }

    private static native void setDetachState(boolean detach, long terminationFlag);

    private static class Buffers {
        static boolean isBuffer(Class<?> cls) {
            return Buffer.class.isAssignableFrom(cls);
        }
    }

    /** Provides separation of JAWT functionality for the sake of J2ME
     * ports which do not include AWT support.
     */
    private static class AWT {
        static long getWindowID(Window w) throws HeadlessException {
            return getComponentID(w);
        }
        // Declaring the argument as Object rather than Component avoids class not
        // found errors on phoneME foundation profile.
        static long getComponentID(Object o) throws HeadlessException {
            if (GraphicsEnvironment.isHeadless()) {
                throw new HeadlessException("No native windows when headless");
            }
            Component c = (Component)o;
            if (c.isLightweight()) {
                throw new IllegalArgumentException("Component must be heavyweight");
            }
            if (!c.isDisplayable())
                throw new IllegalStateException("Component must be displayable");
            // On X11 VMs prior to 1.5, the window must be visible
            if (Platform.isX11()
                && System.getProperty("java.version").startsWith("1.4")) {
                if (!c.isVisible()) {
                    throw new IllegalStateException("Component must be visible");
                }
            }
            // By this point, we're certain that Toolkit.loadLibraries() has
            // been called, thus avoiding AWT/JAWT link errors
            // (see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6539705).
            return Native.getWindowHandle0(c);
        }
    }
}
