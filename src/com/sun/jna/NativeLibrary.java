/* Copyright (c) 2007 Wayne Meissner, All Rights Reserved
 * Copyright (c) 2007-2013 Timothy Wall, All Rights Reserved
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

import com.sun.jna.internal.Cleaner;
import static com.sun.jna.Native.DEBUG_LOAD;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides management of native library resources.  One instance of this
 * class corresponds to a single loaded native library.  May also be used
 * to map to the current process (see {@link NativeLibrary#getProcess()}).
 * <p>
 * <a name=library_search_paths></a>
 * <b>Library Search Paths</b>
 * A search for a given library will scan the following locations:
 * <ol>
 * <li><code>jna.library.path</code> User-customizable path
 * <li><code>jna.platform.library.path</code> Platform-specific paths
 * <li>On OSX, <code>~/Library/Frameworks</code>,
 * <code>/Library/Frameworks</code>, and
 * <code>/System/Library/Frameworks</code> will be searched for a framework
 * with a name corresponding to that requested.  Absolute paths to frameworks
 * are also accepted, either ending at the framework name (sans ".framework")
 * or the full path to the framework shared library
 * (e.g. CoreServices.framework/CoreServices).
 * <li>Context class loader classpath.  Deployed native libraries may be
 * installed on the classpath under
 * <code>${os-prefix}/LIBRARY_FILENAME</code>, where <code>${os-prefix}</code>
 * is the OS/Arch prefix returned by {@link
 * Platform#getNativeLibraryResourcePrefix()}.  If bundled in a jar file, the
 * resource will be extracted to <code>jna.tmpdir</code> for loading, and
 * later removed (but only if <code>jna.nounpack</code> is false or not set).
 * </ol>
 * You may set the system property <code>jna.debug_load=true</code> to make
 * JNA print the steps of its library search to the console.
 * @author Wayne Meissner, split library loading from Function.java
 * @author twall
 */
public class NativeLibrary implements Closeable {

    private static final Logger LOG = Logger.getLogger(NativeLibrary.class.getName());
    private static final Level DEBUG_LOAD_LEVEL = DEBUG_LOAD ? Level.INFO : Level.FINE;
    private static final SymbolProvider NATIVE_SYMBOL_PROVIDER = new SymbolProvider() {
        @Override
        public long getSymbolAddress(long handle, String name, SymbolProvider parent) {
            return Native.findSymbol(handle, name);
        }
    };

    private final Cleaner.Cleanable cleanable;
    private volatile long handle;
    private final String libraryName;
    private final String libraryPath;
    private final Map<String, Function> functions = new HashMap<>();
    private final SymbolProvider symbolProvider;
    private final int callFlags;
    private final String encoding;
    private final Map<String, ?> options;

    private static final Map<String, Reference<NativeLibrary>> libraries = new HashMap<>();

    private static final Map<String, List<String>> searchPaths = new ConcurrentHashMap<>();
    private static final LinkedHashSet<String> librarySearchPath = new LinkedHashSet<>();

    static {
        // Force initialization of native library
        if (Native.POINTER_SIZE == 0)
            throw new Error("Native library not initialized");
    }

    private static String functionKey(String name, int flags, String encoding) {
        return name + "|" + flags + "|" + encoding;
    }

    @SuppressWarnings("LeakingThisInConstructor")
    private NativeLibrary(String libraryName, String libraryPath, long handle, Map<String, ?> options) {
        this.libraryName = getLibraryName(libraryName);
        this.libraryPath = libraryPath;
        this.handle = handle;
        this.cleanable = Cleaner.getCleaner().register(this, new NativeLibraryDisposer(handle));
        Object option = options.get(Library.OPTION_CALLING_CONVENTION);
        int callingConvention = option instanceof Number ? ((Number)option).intValue() : Function.C_CONVENTION;
        this.callFlags = callingConvention;
        this.options = options;
        SymbolProvider optionSymbolProvider = (SymbolProvider)options.get(Library.OPTION_SYMBOL_PROVIDER);
        if (optionSymbolProvider == null) {
            this.symbolProvider = NATIVE_SYMBOL_PROVIDER;
        } else {
            this.symbolProvider = optionSymbolProvider;
        }

        String encodingValue = (String) options.get(Library.OPTION_STRING_ENCODING);
        if (encodingValue == null) {
            encodingValue = Native.getDefaultStringEncoding();
        }
        this.encoding = encodingValue;

        // Special workaround for w32 kernel32.GetLastError
        // Short-circuit the function to use built-in GetLastError access
        if (Platform.isWindows() && "kernel32".equals(this.libraryName.toLowerCase())) {
            synchronized(functions) {
                Function f = new Function(this, "GetLastError", Function.ALT_CONVENTION, encoding) {
                        @Override
                        Object invoke(Object[] args, Class<?> returnType, boolean b, int fixedArgs) {
                            return Integer.valueOf(Native.getLastError());
                        }

                        @Override
                        Object invoke(Method invokingMethod, Class<?>[] paramTypes, Class<?> returnType, Object[] inArgs, Map<String, ?> options) {
                            return Integer.valueOf(Native.getLastError());
                        }
                    };
                functions.put(functionKey("GetLastError", callFlags, encoding), f);
            }
        }
    }

    private static final int DEFAULT_OPEN_OPTIONS = -1;
    private static final int DEFAULT_WINDOWS_RELATIVE_PATH_OPEN_OPTIONS = 0;
    private static int openFlags(Map<String, ?> options, boolean isAbsolutePath) {
        Object opt = options.get(Library.OPTION_OPEN_FLAGS);
        if (opt instanceof Number) {
            return ((Number)opt).intValue();
        }
        if (Platform.isWindows() && !isAbsolutePath) {
            return DEFAULT_WINDOWS_RELATIVE_PATH_OPEN_OPTIONS;
        }
        return DEFAULT_OPEN_OPTIONS;
    }

    private static NativeLibrary loadLibrary(final String libraryName, final Map<String, ?> options) {
        LOG.log(DEBUG_LOAD_LEVEL, "Looking for library '" + libraryName + "'");

        List<Throwable> exceptions = new ArrayList<>();
        boolean isAbsolutePath = new File(libraryName).isAbsolute();
        LinkedHashSet<String> searchPath = new LinkedHashSet<>();
        int openFlags = openFlags(options, isAbsolutePath);

        //
        // Prepend any custom search paths specifically for this library
        //
        List<String> customPaths = searchPaths.get(libraryName);
        if (customPaths != null) {
            synchronized (customPaths) {
                searchPath.addAll(customPaths);
            }
        }

        // Append web start path, if available.  Note that this does not
        // attempt any library name variations
        String webstartPath = Native.getWebStartLibraryPath(libraryName);
        if (webstartPath != null) {
            LOG.log(DEBUG_LOAD_LEVEL, "Adding web start path " + webstartPath);
            searchPath.add(webstartPath);
        }

        LOG.log(DEBUG_LOAD_LEVEL, "Adding paths from jna.library.path: " + System.getProperty("jna.library.path"));

        searchPath.addAll(initPaths("jna.library.path"));
        String libraryPath = findLibraryPath(libraryName, searchPath);
        long handle = 0;
        //
        // Only search user specified paths first.  This will also fall back
        // to dlopen/LoadLibrary() since findLibraryPath returns the mapped
        // name if it cannot find the library.
        //
        try {
            LOG.log(DEBUG_LOAD_LEVEL, "Trying " + libraryPath);
            handle = Native.open(libraryPath, openFlags);
        } catch(UnsatisfiedLinkError e) {
            // Add the system paths back for all fallback searching
            LOG.log(DEBUG_LOAD_LEVEL, "Loading failed with message: " + e.getMessage());
            LOG.log(DEBUG_LOAD_LEVEL, "Adding system paths: " + librarySearchPath);
            exceptions.add(e);
            searchPath.addAll(librarySearchPath);
        }

        try {
            if (handle == 0) {
                libraryPath = findLibraryPath(libraryName, searchPath);
                LOG.log(DEBUG_LOAD_LEVEL, "Trying " + libraryPath);
                handle = Native.open(libraryPath, openFlags);
                if (handle == 0) {
                    throw new UnsatisfiedLinkError("Failed to load library '" + libraryName + "'");
                }
            }
        } catch(UnsatisfiedLinkError ule) {
            LOG.log(DEBUG_LOAD_LEVEL, "Loading failed with message: " + ule.getMessage());
            exceptions.add(ule);
            // For android, try to "preload" the library using
            // System.loadLibrary(), which looks into the private /data/data
            // path, not found in any properties
            if (Platform.isAndroid()) {
                try {
                    LOG.log(DEBUG_LOAD_LEVEL, "Preload (via System.loadLibrary) " + libraryName);
                    System.loadLibrary(libraryName);
                    handle = Native.open(libraryPath, openFlags);
                }
                catch(UnsatisfiedLinkError e2) {
                    LOG.log(DEBUG_LOAD_LEVEL, "Loading failed with message: " + e2.getMessage());
                    exceptions.add(e2);
                }
            }
            else if (Platform.isLinux() || Platform.isFreeBSD()) {
                //
                // Failed to load the library normally - try to match libfoo.so.*
                //
                LOG.log(DEBUG_LOAD_LEVEL, "Looking for version variants");
                libraryPath = matchLibrary(libraryName, searchPath);
                if (libraryPath != null) {
                    LOG.log(DEBUG_LOAD_LEVEL, "Trying " + libraryPath);
                    try {
                        handle = Native.open(libraryPath, openFlags);
                    }
                    catch(UnsatisfiedLinkError e2) {
                        LOG.log(DEBUG_LOAD_LEVEL, "Loading failed with message: " + e2.getMessage());
                        exceptions.add(e2);
                    }
                }
            }
            // Search framework libraries on OS X
            else if (Platform.isMac() && !libraryName.endsWith(".dylib")) {
                for(String frameworkName : matchFramework(libraryName)) {
                    try {
                        LOG.log(DEBUG_LOAD_LEVEL, "Trying " + frameworkName);
                        handle = Native.open(frameworkName, openFlags);
                        break;
                    }
                    catch(UnsatisfiedLinkError e2) {
                        LOG.log(DEBUG_LOAD_LEVEL, "Loading failed with message: " + e2.getMessage());
                        exceptions.add(e2);
                    }
                }
            }
            // Try the same library with a "lib" prefix
            else if (Platform.isWindows() && !isAbsolutePath) {
                LOG.log(DEBUG_LOAD_LEVEL, "Looking for lib- prefix");
                libraryPath = findLibraryPath("lib" + libraryName, searchPath);
                if (libraryPath != null) {
                    LOG.log(DEBUG_LOAD_LEVEL, "Trying " + libraryPath);
                    try {
                        handle = Native.open(libraryPath, openFlags);
                    } catch(UnsatisfiedLinkError e2) {
                        LOG.log(DEBUG_LOAD_LEVEL, "Loading failed with message: " + e2.getMessage());
                        exceptions.add(e2);
                    }
                }
            }
            // As a last resort, try to extract the library from the class
            // path, using the current context class loader.
            if (handle == 0) {
                try {
                    File embedded = Native.extractFromResourcePath(libraryName, (ClassLoader)options.get(Library.OPTION_CLASSLOADER));
                    if (embedded != null) {
                        try {
                            handle = Native.open(embedded.getAbsolutePath(), openFlags);
                            libraryPath = embedded.getAbsolutePath();
                        } finally {
                            // Don't leave temporary files around
                            if (Native.isUnpacked(embedded)) {
                                Native.deleteLibrary(embedded);
                            }
                        }
                    }
                }
                catch(IOException e2) {
                    LOG.log(DEBUG_LOAD_LEVEL, "Loading failed with message: " + e2.getMessage());
                    exceptions.add(e2);
                }
            }

            if (handle == 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("Unable to load library '");
                sb.append(libraryName);
                sb.append("':");
                for(Throwable t: exceptions) {
                    sb.append("\n");
                    sb.append(t.getMessage());
                }
                UnsatisfiedLinkError res = new UnsatisfiedLinkError(sb.toString());
                for(Throwable t: exceptions) {
                    addSuppressedReflected(res, t);
                }
                throw res;
            }
        }

        LOG.log(DEBUG_LOAD_LEVEL, "Found library '" + libraryName + "' at " + libraryPath);
        return new NativeLibrary(libraryName, libraryPath, handle, options);
    }

    private static Method addSuppressedMethod = null;
    static {
        try {
            addSuppressedMethod = Throwable.class.getMethod("addSuppressed", Throwable.class);
        } catch (NoSuchMethodException ex) {
            // This is the case for JDK < 7
        } catch (SecurityException ex) {
            Logger.getLogger(NativeLibrary.class.getName()).log(Level.SEVERE, "Failed to initialize 'addSuppressed' method", ex);
        }
    }

    private static void addSuppressedReflected(Throwable target, Throwable suppressed) {
        if(addSuppressedMethod == null) {
            // Make this a NOOP on an unsupported JDK
            return;
        }
        try {
            addSuppressedMethod.invoke(target, suppressed);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException("Failed to call addSuppressedMethod", ex);
        }
    }

    /** Look for a matching framework (OSX) */
    static String[] matchFramework(String libraryName) {
        Set<String> paths = new LinkedHashSet<>();
        File framework = new File(libraryName);
        if (framework.isAbsolute()) {
            if (libraryName.contains(".framework")) {
                if (framework.exists()) {
                    return new String[]{framework.getAbsolutePath()};
                }
                paths.add(framework.getAbsolutePath());
            }
            else {
                framework = new File(new File(framework.getParentFile(), framework.getName() + ".framework"), framework.getName());
                if (framework.exists()) {
                    return new String[]{framework.getAbsolutePath()};
                }
                paths.add(framework.getAbsolutePath());
            }
        }
        else {
            final String[] PREFIXES = { System.getProperty("user.home"), "", "/System" };
            String suffix = !libraryName.contains(".framework")
                    ? libraryName + ".framework/" + libraryName : libraryName;
            for (String prefix : PREFIXES) {
                framework = new File(prefix + "/Library/Frameworks/" + suffix);
                if (framework.exists()) {
                    return new String[]{framework.getAbsolutePath()};
                }
                paths.add(framework.getAbsolutePath());
            }
        }
        return paths.toArray(new String[0]);
    }

    private String getLibraryName(String libraryName) {
        String simplified = libraryName;
        final String BASE = "---";
        String template = mapSharedLibraryName(BASE);
        int prefixEnd = template.indexOf(BASE);
        if (prefixEnd > 0 && simplified.startsWith(template.substring(0, prefixEnd))) {
            simplified = simplified.substring(prefixEnd);
        }
        String suffix = template.substring(prefixEnd + BASE.length());
        int suffixStart = simplified.indexOf(suffix);
        if (suffixStart != -1) {
            simplified = simplified.substring(0, suffixStart);
        }
        return simplified;
    }

    /**
     * Returns an instance of NativeLibrary for the specified name.
     * The library is loaded if not already loaded.  If already loaded, the
     * existing instance is returned.<p>
     * More than one name may map to the same NativeLibrary instance; only
     * a single instance will be provided for any given unique file path.
     *
     * @param libraryName The library name to load.
     *      This can be short form (e.g. "c"),
     *      an explicit version (e.g. "libc.so.6"), or
     *      the full path to the library (e.g. "/lib/libc.so.6").
     */
    public static final NativeLibrary getInstance(String libraryName) {
        return getInstance(libraryName, Collections.<String, Object>emptyMap());
    }

    /**
     * Returns an instance of NativeLibrary for the specified name.
     * The library is loaded if not already loaded.  If already loaded, the
     * existing instance is returned.<p>
     * More than one name may map to the same NativeLibrary instance; only
     * a single instance will be provided for any given unique file path.
     *
     * @param libraryName The library name to load.
     *      This can be short form (e.g. "c"),
     *      an explicit version (e.g. "libc.so.6"), or
     *      the full path to the library (e.g. "/lib/libc.so.6").
     * @param classLoader The class loader to use to load the native library.
     *      This only affects library loading when the native library is
     *      included somewhere in the classpath, either bundled in a jar file
     *      or as a plain file within the classpath.
     */
    public static final NativeLibrary getInstance(String libraryName, ClassLoader classLoader) {
        return getInstance(libraryName, Collections.singletonMap(Library.OPTION_CLASSLOADER, classLoader));
    }

    /**
     * Returns an instance of NativeLibrary for the specified name.
     * The library is loaded if not already loaded.  If already loaded, the
     * existing instance is returned.<p>
     * More than one name may map to the same NativeLibrary instance; only
     * a single instance will be provided for any given unique file path.
     *
     * @param libraryName The library name to load.
     *      This can be short form (e.g. "c"),
     *      an explicit version (e.g. "libc.so.6" or
     *      "QuickTime.framework/Versions/Current/QuickTime"), or
     *      the full (absolute) path to the library (e.g. "/lib/libc.so.6").
     * @param libraryOptions Native library options for the given library (see {@link Library}).
     */
    public static final NativeLibrary getInstance(String libraryName, Map<String, ?> libraryOptions) {
        Map<String, Object> options = new HashMap<>(libraryOptions);
        if (options.get(Library.OPTION_CALLING_CONVENTION) == null) {
            options.put(Library.OPTION_CALLING_CONVENTION, Integer.valueOf(Function.C_CONVENTION));
        }

        // Use current process to load libraries we know are already
        // loaded by the VM to ensure we get the correct version
        if ((Platform.isLinux() || Platform.isFreeBSD() || Platform.isAIX())
            && Platform.C_LIBRARY_NAME.equals(libraryName)) {
            libraryName = null;
        }
        synchronized (libraries) {
            Reference<NativeLibrary> ref = libraries.get(libraryName + options);
            NativeLibrary library = (ref != null) ? ref.get() : null;

            if (library == null) {
                if (libraryName == null) {
                    library = new NativeLibrary("<process>", null, Native.open(null, openFlags(options, true)), options);
                }
                else {
                    library = loadLibrary(libraryName, options);
                }
                ref = new WeakReference<>(library);
                libraries.put(library.getName() + options, ref);
                File file = library.getFile();
                if (file != null) {
                    libraries.put(file.getAbsolutePath() + options, ref);
                    libraries.put(file.getName() + options, ref);
                }
            }
            return library;
        }
    }

    /**
     * Returns an instance of NativeLibrary which refers to the current
     * process.  This is useful for accessing functions which were already
     * mapped by some other mechanism, without having to reference or even
     * know the exact name of the native library.
     */
    public static synchronized final NativeLibrary getProcess() {
        return getInstance(null);
    }

    /**
     * Returns an instance of NativeLibrary which refers to the current
     * process.  This is useful for accessing functions which were already
     * mapped by some other mechanism, without having to reference or even
     * know the exact name of the native library.
     */
    public static synchronized final NativeLibrary getProcess(Map<String, ?> options) {
        return getInstance(null, options);
    }

    /**
     * Add a path to search for the specified library, ahead of any system
     * paths.  This is similar to setting <code>jna.library.path</code>, but
     * only extends the search path for a single library.
     *
     * @param libraryName The name of the library to use the path for
     * @param path The path to use when trying to load the library
     */
    public static final void addSearchPath(String libraryName, String path) {
        List<String> customPaths = searchPaths.get(libraryName);
        if (customPaths == null) {
            customPaths = Collections.synchronizedList(new ArrayList<String>());
            searchPaths.put(libraryName, customPaths);
        }

        customPaths.add(path);
    }

    /**
     * Create a new {@link Function} that is linked with a native
     * function that follows the NativeLibrary's calling convention.
     *
     * <p>The allocated instance represents a pointer to the named native
     * function from the library.
     *
     * @param    functionName
     *            Name of the native function to be linked with
     * @throws   UnsatisfiedLinkError if the function is not found
     */
    public Function getFunction(String functionName) {
        return getFunction(functionName, callFlags);
    }

    /**
     * Create a new {@link Function} that is linked with a native
     * function that follows the NativeLibrary's calling convention.
     *
     * <p>The allocated instance represents a pointer to the named native
     * function from the library.
     *
     * @param    name
     *            Name of the native function to be linked with.  Uses a
     *            function mapper option if one was provided to
     *            transform the name.
     * @param    method
     *            Method to which the native function is to be mapped
     * @throws   UnsatisfiedLinkError if the function is not found
     */
    Function getFunction(String name, Method method) {
        FunctionMapper mapper = (FunctionMapper) options.get(Library.OPTION_FUNCTION_MAPPER);
        if (mapper != null) {
            name = mapper.getFunctionName(this, method);
        }
        // If there's native method profiler prefix, strip it
        String prefix = System.getProperty("jna.profiler.prefix", "$$YJP$$");
        if (name.startsWith(prefix)) {
            name = name.substring(prefix.length());
        }
        int flags = this.callFlags;
        Class<?>[] etypes = method.getExceptionTypes();
        for (int i=0;i < etypes.length;i++) {
            if (LastErrorException.class.isAssignableFrom(etypes[i])) {
                flags |= Function.THROW_LAST_ERROR;
            }
        }
        return getFunction(name, flags);
    }

    /**
     * Create a new  {@link Function} that is linked with a native
     * function that follows a given calling flags.
     *
     * @param    functionName
     *            Name of the native function to be linked with
     * @param    callFlags
     *            Flags affecting the function invocation
     * @throws   UnsatisfiedLinkError if the function is not found
     */
    public Function getFunction(String functionName, int callFlags) {
        return getFunction(functionName, callFlags, encoding);
    }

    /**
     * Create a new  {@link Function} that is linked with a native
     * function that follows a given calling flags.
     *
     * @param    functionName
     *            Name of the native function to be linked with
     * @param    callFlags
     *            Flags affecting the function invocation
     * @param   encoding
     *                  Encoding to use to convert between Java and native
     *                  strings.
     * @throws   UnsatisfiedLinkError if the function is not found
     */
    public Function getFunction(String functionName, int callFlags, String encoding) {
        if (functionName == null) {
            throw new NullPointerException("Function name may not be null");
        }
        synchronized (functions) {
            String key = functionKey(functionName, callFlags, encoding);
            Function function = functions.get(key);
            if (function == null) {
                function = new Function(this, functionName, callFlags, encoding);
                functions.put(key, function);
            }
            return function;
        }
    }

    /** @return this native library instance's options. */
    public Map<String, ?> getOptions() {
        return options;
    }

    /** Look up the given global variable within this library.
     * @param symbolName
     * @return Pointer representing the global variable address
     * @throws UnsatisfiedLinkError if the symbol is not found
     */
    public Pointer getGlobalVariableAddress(String symbolName) {
        try {
            return new Pointer(getSymbolAddress(symbolName));
        } catch(UnsatisfiedLinkError e) {
            throw new UnsatisfiedLinkError("Error looking up '" + symbolName + "': " + e.getMessage());
        }
    }

    /**
     * Used by the Function class to locate a symbol
     * @throws UnsatisfiedLinkError if the symbol can't be found
     */
    long getSymbolAddress(String name) {
        if (handle == 0) {
            throw new UnsatisfiedLinkError("Library has been unloaded");
        }
        return this.symbolProvider.getSymbolAddress(handle, name, NATIVE_SYMBOL_PROVIDER);
    }

    @Override
    public String toString() {
        return "Native Library <" + libraryPath + "@" + handle + ">";
    }
    /** Returns the simple name of this library. */
    public String getName() {
        return libraryName;
    }
    /**
     * Returns the file on disk corresponding to this NativeLibrary instance.
     * If this NativeLibrary represents the current process, this function will return null.
     */
    public File getFile() {
        if (libraryPath == null)
            return null;
        return new File(libraryPath);
    }

    /** Close all open native libraries. */
    static void disposeAll() {
        Set<Reference<NativeLibrary>> values;
        synchronized(libraries) {
            values = new LinkedHashSet<>(libraries.values());
        }
        for (Reference<NativeLibrary> ref : values) {
            NativeLibrary lib = ref.get();
            if (lib != null) {
                lib.close();
            }
        }
    }

    /** Close the native library we're mapped to. */
    public void close() {
        Set<String> keys = new HashSet<>();
        synchronized(libraries) {
            for (Map.Entry<String, Reference<NativeLibrary>> e : libraries.entrySet()) {
                Reference<NativeLibrary> ref = e.getValue();
                if (ref.get() == this) {
                    keys.add(e.getKey());
                }
            }

            for (String k : keys) {
                libraries.remove(k);
            }
        }

        synchronized(this) {
            if (handle != 0) {
                handle = 0;
                cleanable.clean();
            }
        }
    }

    @Deprecated
    public void dispose() {
        close();
    }

    private static List<String> initPaths(String key) {
        String value = System.getProperty(key, "");
        if ("".equals(value)) {
            return Collections.emptyList();
        }
        StringTokenizer st = new StringTokenizer(value, File.pathSeparator);
        List<String> list = new ArrayList<>();
        while (st.hasMoreTokens()) {
            String path = st.nextToken();
            if (!"".equals(path)) {
                list.add(path);
            }
        }
        return list;
    }

    /** Use standard library search paths to find the library. */
    private static String findLibraryPath(String libName, Collection<String> searchPath) {

        //
        // If a full path to the library was specified, don't search for it
        //
        if (new File(libName).isAbsolute()) {
            return libName;
        }

        //
        // Get the system name for the library (e.g. libfoo.so)
        //
        String name = mapSharedLibraryName(libName);

        // Search in the JNA paths for it
        for (String path : searchPath) {
            File file = new File(path, name);
            if (file.exists()) {
                return file.getAbsolutePath();
            }
            if (Platform.isMac()) {
                // Native libraries delivered via JNLP class loader
                // may require a .jnilib extension to be found
                if (name.endsWith(".dylib")) {
                    file = new File(path, name.substring(0, name.lastIndexOf(".dylib")) + ".jnilib");
                    if (file.exists()) {
                        return file.getAbsolutePath();
                    }
                }
            }
        }

        //
        // Default to returning the mapped library name and letting the system
        // search for it
        //
        return name;
    }

    /** Similar to {@link System#mapLibraryName}, except that it maps to
        standard shared library formats rather than specifically JNI formats.
        @param libName base (undecorated) name of library
    */
    static String mapSharedLibraryName(String libName) {
        if (Platform.isMac()) {
            if (libName.startsWith("lib")
                && (libName.endsWith(".dylib")
                    || libName.endsWith(".jnilib"))) {
                return libName;
            }
            String name = System.mapLibraryName(libName);
            // On MacOSX, System.mapLibraryName() returns the .jnilib extension
            // (the suffix for JNI libraries); ordinarily shared libraries have
            // a .dylib suffix
            if (name.endsWith(".jnilib")) {
                return name.substring(0, name.lastIndexOf(".jnilib")) + ".dylib";
            }
            return name;
        }
        else if (Platform.isLinux() || Platform.isFreeBSD()) {
            if (isVersionedName(libName) || libName.endsWith(".so")) {
                // A specific version was requested - use as is for search
                return libName;
            }
        }
        else if (Platform.isAIX()) {    // can be libx.a, libx.a(shr.o), libx.so
            if (isVersionedName(libName) || libName.endsWith(".so") || libName.startsWith("lib") || libName.endsWith(".a")) {
                // A specific version was requested - use as is for search
                return libName;
            }
        }
        else if (Platform.isWindows()) {
            if (libName.endsWith(".drv") || libName.endsWith(".dll") || libName.endsWith(".ocx")) {
                return libName;
            }
        }

        String mappedName = System.mapLibraryName(libName);
        if(Platform.isAIX() && mappedName.endsWith(".so")) {
            return mappedName.replaceAll(".so$", ".a");
        } else {
            return mappedName;
        }
    }

    private static boolean isVersionedName(String name) {
        if (name.startsWith("lib")) {
            int so = name.lastIndexOf(".so.");
            if (so != -1 && so + 4 < name.length()) {
                for (int i=so+4;i < name.length();i++) {
                    char ch = name.charAt(i);
                    if (!Character.isDigit(ch) && ch != '.') {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * matchLibrary() is very Linux specific.  It is here to deal with the case
     * where /usr/lib/libc.so does not exist, or it is not a valid symlink to
     * a versioned file (e.g. /lib/libc.so.6).
     */
    static String matchLibrary(final String libName, Collection<String> searchPath) {
        File lib = new File(libName);
        if (lib.isAbsolute()) {
            searchPath = Arrays.asList(lib.getParent());
        }
        FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return (filename.startsWith("lib" + libName + ".so")
                            || (filename.startsWith(libName + ".so")
                                && libName.startsWith("lib")))
                        && isVersionedName(filename);
                }
            };

        Collection<File> matches = new LinkedList<>();
        for (String path : searchPath) {
            File[] files = new File(path).listFiles(filter);
            if (files != null && files.length > 0) {
                matches.addAll(Arrays.asList(files));
            }
        }

        //
        // Search through the results and return the highest numbered version
        // i.e. libc.so.6 is preferred over libc.so.5
        double bestVersion = -1;
        String bestMatch = null;
        for (File f : matches) {
            String path = f.getAbsolutePath();
            String ver = path.substring(path.lastIndexOf(".so.") + 4);
            double version = parseVersion(ver);
            if (version > bestVersion) {
                bestVersion = version;
                bestMatch = path;
            }
        }
        return bestMatch;
    }

    static double parseVersion(String ver) {
        double v = 0;
        double divisor = 1;
        int dot = ver.indexOf(".");
        while (ver != null) {
            String num;
            if (dot != -1) {
                num = ver.substring(0, dot);
                ver = ver.substring(dot + 1);
                dot = ver.indexOf(".");
            }
            else {
                num = ver;
                ver = null;
            }
            try {
                v += Integer.parseInt(num) / divisor;
            }
            catch(NumberFormatException e) {
                return 0;
            }
            divisor *= 100;
        }

        return v;
    }

    static {
        String webstartPath = Native.getWebStartLibraryPath("jnidispatch");
        if (webstartPath != null) {
            librarySearchPath.add(webstartPath);
        }
        if (System.getProperty("jna.platform.library.path") == null
            && !Platform.isWindows()) {
            // Add default path lookups for unix-like systems
            String platformPath = "";
            String sep = "";
            String archPath = "";

            //
            // Search first for an arch specific path if one exists, but always
            // include the generic paths if they exist.
            // NOTES (wmeissner):
            // Some older linux amd64 distros did not have /usr/lib64, and
            // 32bit distros only have /usr/lib.  FreeBSD also only has
            // /usr/lib by default, with /usr/lib32 for 32bit compat.
            // Solaris seems to have both, but defaults to 32bit userland even
            // on 64bit machines, so we have to explicitly search the 64bit
            // one when running a 64bit JVM.
            //
            if (Platform.isLinux() || Platform.isSolaris()
                || Platform.isFreeBSD() || Platform.iskFreeBSD()) {
                // Linux & FreeBSD use /usr/lib32, solaris uses /usr/lib/32
                archPath = (Platform.isSolaris() ? "/" : "") + Native.POINTER_SIZE * 8;
            }
            String[] paths = {
                "/usr/lib" + archPath,
                "/lib" + archPath,
                "/usr/lib",
                "/lib",
            };
            // Multi-arch support on Ubuntu (and other
            // multi-arch distributions)
            // paths is scanned against real directory
            // so for platforms which are not multi-arch
            // this should continue to work.
            if (Platform.isLinux() || Platform.iskFreeBSD() || Platform.isGNU()) {
                String multiArchPath = getMultiArchPath();

                // Assemble path with all possible options
                paths = new String[] {
                    "/usr/lib/" + multiArchPath,
                    "/lib/" + multiArchPath,
                    "/usr/lib" + archPath,
                    "/lib" + archPath,
                    "/usr/lib",
                    "/lib",
                };
            }

            // We might be wrong with the multiArchPath above. Raspbian,
            // the Raspberry Pi flavor of Debian, for example, uses
            // uses arm-linux-gnuabihf since it's using the hard-float
            // ABI for armv6. Other distributions might use a different
            // tuple for the same thing. Query ldconfig to get the additional
            // library paths it knows about.
            if (Platform.isLinux()) {
                ArrayList<String> ldPaths = getLinuxLdPaths();
                // prepend the paths we already have
                for (int i=paths.length-1; 0 <= i; i--) {
                    int found = ldPaths.indexOf(paths[i]);
                    if (found != -1) {
                        ldPaths.remove(found);
                    }
                    ldPaths.add(0, paths[i]);
                }
                paths = ldPaths.toArray(new String[0]);
            }

            for (int i=0;i < paths.length;i++) {
                File dir = new File(paths[i]);
                if (dir.exists() && dir.isDirectory()) {
                    platformPath += sep + paths[i];
                    sep = File.pathSeparator;
                }
            }
            if (!"".equals(platformPath)) {
                System.setProperty("jna.platform.library.path", platformPath);
            }
        }
        librarySearchPath.addAll(initPaths("jna.platform.library.path"));
    }

    private static String getMultiArchPath() {
        String cpu = Platform.ARCH;
        String kernel = Platform.iskFreeBSD()
            ? "-kfreebsd"
            : (Platform.isGNU() ? "" : "-linux");
        String libc = "-gnu";

        if (Platform.isIntel()) {
            cpu = (Platform.is64Bit() ? "x86_64" : "i386");
        }
        else if (Platform.isPPC()) {
            cpu = (Platform.is64Bit() ? "powerpc64" : "powerpc");
        }
        else if (Platform.isARM()) {
            cpu = "arm";
            libc = "-gnueabi";
        }
        else if (Platform.ARCH.equals("mips64el")) {
            libc = "-gnuabi64";
        }

        return cpu + kernel + libc;
    }

    /**
     * Get the library paths from ldconfig cache. Tested against ldconfig 2.13.
     */
    private static ArrayList<String> getLinuxLdPaths() {
        ArrayList<String> ldPaths = new ArrayList<>();
        Process process = null;
        BufferedReader reader = null;
        try {
            process = Runtime.getRuntime().exec("/sbin/ldconfig -p");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String buffer;
            while ((buffer = reader.readLine()) != null) {
                int startPath = buffer.indexOf(" => ");
                int endPath = buffer.lastIndexOf('/');
                if (startPath != -1 && endPath != -1 && startPath < endPath) {
                    String path = buffer.substring(startPath + 4, endPath);
                    if (!ldPaths.contains(path)) {
                        ldPaths.add(path);
                    }
                }
            }
        } catch (Exception e) {
        } finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
            if(process != null) {
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                }
            }
        }
        return ldPaths;
    }

    private static final class NativeLibraryDisposer implements Runnable {

        private long handle;

        public NativeLibraryDisposer(long handle) {
            this.handle = handle;
        }

        public synchronized void run() {
            if (handle != 0) {
                try {
                    Native.close(handle);
                } finally {
                    handle = 0;
                }
            }
        }

    }
}
