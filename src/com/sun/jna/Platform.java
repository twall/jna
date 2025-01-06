/*
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

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Provide simplified platform information. */
public final class Platform {
    public static final int UNSPECIFIED = -1;
    public static final int MAC = 0;
    public static final int LINUX = 1;
    public static final int WINDOWS = 2;
    public static final int SOLARIS = 3;
    public static final int FREEBSD = 4;
    public static final int OPENBSD = 5;
    public static final int WINDOWSCE = 6;
    public static final int AIX = 7;
    public static final int ANDROID = 8;
    public static final int GNU = 9;
    public static final int KFREEBSD = 10;
    public static final int NETBSD = 11;
    public static final int DRAGONFLYBSD = 12;

    /** Whether read-only (final) fields within Structures are supported. */
    public static final boolean RO_FIELDS;
    /** Whether this platform provides NIO Buffers. */
    public static final boolean HAS_BUFFERS;
    /** Whether this platform provides the AWT Component class; also false if
     * running headless.
     */
    public static final boolean HAS_AWT;
    /** Whether this platform supports the JAWT library. */
    public static final boolean HAS_JAWT;
    /** Canonical name of this platform's math library. */
    public static final String MATH_LIBRARY_NAME;
    /** Canonical name of this platform's C runtime library. */
    public static final String C_LIBRARY_NAME;
    /** Whether in-DLL callbacks are supported. */
    public static final boolean HAS_DLL_CALLBACKS;
    /** Canonical resource prefix for the current platform.  This value is
     * used to load bundled native libraries from the class path.
     */
    public static final String RESOURCE_PREFIX;

    private static final int osType;
    /** Current platform architecture. */
    public static final String ARCH;

    static {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Linux")) {
            if ("dalvik".equals(System.getProperty("java.vm.name").toLowerCase())) {
                osType = ANDROID;
                // Native libraries on android must be bundled with the APK
                System.setProperty("jna.nounpack", "true");
            }
            else {
                osType = LINUX;
            }
        }
        else if (osName.startsWith("AIX")) {
            osType = AIX;
        }
        else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
            osType = MAC;
        }
        else if (osName.startsWith("Windows CE")) {
            osType = WINDOWSCE;
        }
        else if (osName.startsWith("Windows")) {
            osType = WINDOWS;
        }
        else if (osName.startsWith("Solaris") || osName.startsWith("SunOS")) {
            osType = SOLARIS;
        }
        else if (osName.startsWith("FreeBSD")) {
            osType = FREEBSD;
        }
        else if (osName.startsWith("OpenBSD")) {
            osType = OPENBSD;
        }
        else if (osName.equalsIgnoreCase("gnu")) {
            osType = GNU;
        }
        else if (osName.equalsIgnoreCase("gnu/kfreebsd")) {
            osType = KFREEBSD;
        }
        else if (osName.equalsIgnoreCase("netbsd")) {
            osType = NETBSD;
        }
        else if (osName.equalsIgnoreCase("dragonflybsd")) {
            osType = DRAGONFLYBSD;
        }
        else {
            osType = UNSPECIFIED;
        }
        boolean hasBuffers = false;
        try {
            Class.forName("java.nio.Buffer");
            hasBuffers = true;
        }
        catch(ClassNotFoundException e) {
        }
        // NOTE: we used to do Class.forName("java.awt.Component"), but that
        // has the unintended side effect of actually loading AWT native libs,
        // which can be problematic
        HAS_AWT = osType != WINDOWSCE && osType != ANDROID && osType != AIX;
        HAS_JAWT = HAS_AWT && osType != MAC;
        HAS_BUFFERS = hasBuffers;
        RO_FIELDS = osType != WINDOWSCE;
        C_LIBRARY_NAME = osType == WINDOWS ? "msvcrt" : osType == WINDOWSCE ? "coredll" : "c";
        MATH_LIBRARY_NAME = osType == WINDOWS ? "msvcrt" : osType == WINDOWSCE ? "coredll" : "m";
        ARCH = getCanonicalArchitecture(System.getProperty("os.arch"), osType);
        // Windows aarch64 callbacks disabled via ASMFN_OFF (no mingw support)
        HAS_DLL_CALLBACKS = osType == WINDOWS && !ARCH.startsWith("aarch");
        RESOURCE_PREFIX = getNativeLibraryResourcePrefix();
    }
    private Platform() { }
    public static final int getOSType() {
        return osType;
    }
    public static final boolean isMac() {
        return osType == MAC;
    }
    public static final boolean isAndroid() {
        return osType == ANDROID;
    }
    public static final boolean isLinux() {
        return osType == LINUX;
    }
    public static final boolean isAIX() {
        return osType == AIX;
    }
    public static final boolean isWindowsCE() {
        return osType == WINDOWSCE;
    }
    /** Returns true for any windows variant. */
    public static final boolean isWindows() {
        return osType == WINDOWS || osType == WINDOWSCE;
    }
    public static final boolean isSolaris() {
        return osType == SOLARIS;
    }
    public static final boolean isDragonFlyBSD() {
        return osType == DRAGONFLYBSD;
    }
    public static final boolean isFreeBSD() {
        return osType == FREEBSD;
    }
    public static final boolean isOpenBSD() {
        return osType == OPENBSD;
    }
    public static final boolean isNetBSD() {
        return osType == NETBSD;
    }
    public static final boolean isGNU() {
        return osType == GNU;
    }
    public static final boolean iskFreeBSD() {
        return osType == KFREEBSD;
    }
    public static final boolean isX11() {
        // TODO: check filesystem for /usr/X11 or some other X11-specific test
        return !Platform.isWindows() && !Platform.isMac();
    }
    public static final boolean hasRuntimeExec() {
        if (isWindowsCE() && "J9".equals(System.getProperty("java.vm.name")))
            return false;
        return true;
    }
    public static final boolean is64Bit() {
        String model = System.getProperty("sun.arch.data.model",
                                          System.getProperty("com.ibm.vm.bitmode"));
        if (model != null) {
            return "64".equals(model);
        }
        if ("x86-64".equals(ARCH)
            || "ia64".equals(ARCH)
            || "ppc64".equals(ARCH) || "ppc64le".equals(ARCH)
            || "sparcv9".equals(ARCH)
            || "mips64".equals(ARCH) || "mips64el".equals(ARCH)
            || "loongarch64".equals(ARCH)
            || "amd64".equals(ARCH)
            || "aarch64".equals(ARCH)) {
            return true;
        }
        return Native.POINTER_SIZE == 8;
    }

    public static final boolean isIntel() {
        if (ARCH.startsWith("x86")) {
            return true;
        }
        return false;
    }

    public static final boolean isPPC() {
        if (ARCH.startsWith("ppc")) {
            return true;
        }
        return false;
    }

    public static final boolean isARM() {
        return ARCH.startsWith("arm") || ARCH.startsWith("aarch");
    }

    public static final boolean isSPARC() {
        return ARCH.startsWith("sparc");
    }

    public static final boolean isMIPS() {
        if (ARCH.equals("mips")
            || ARCH.equals("mips64")
            || ARCH.equals("mipsel")
            || ARCH.equals("mips64el")) {
            return true;
        }
        return false;
    }

    public static final boolean isLoongArch() {
        return ARCH.startsWith("loongarch");
    }

    static String getCanonicalArchitecture(String arch, int platform) {
        arch = arch.toLowerCase().trim();
        if ("powerpc".equals(arch)) {
            arch = "ppc";
        }
        else if ("powerpc64".equals(arch)) {
            arch = "ppc64";
        }
        else if ("i386".equals(arch) || "i686".equals(arch)) {
            arch = "x86";
        }
        else if ("x86_64".equals(arch) || "amd64".equals(arch)) {
            arch = "x86-64";
        }
        else if ("zarch_64".equals(arch)) {
            arch = "s390x";
        }
        // Work around OpenJDK mis-reporting os.arch
        // https://bugs.openjdk.java.net/browse/JDK-8073139
        if ("ppc64".equals(arch) && "little".equals(System.getProperty("sun.cpu.endian"))) {
            arch = "ppc64le";
        }
        // Map arm to armel if the binary is running as softfloat build
        if("arm".equals(arch) && platform == Platform.LINUX && isSoftFloat()) {
            arch = "armel";
        }

        return arch;
    }

    static boolean isSoftFloat() {
        try {
            File self = new File("/proc/self/exe");
            if (self.exists()) {
                ELFAnalyser ahfd = ELFAnalyser.analyse(self.getCanonicalPath());
                return ! ahfd.isArmHardFloat();
            }
        } catch (IOException ex) {
            // asume hardfloat
            Logger.getLogger(Platform.class.getName()).log(Level.INFO, "Failed to read '/proc/self/exe' or the target binary.", ex);
        } catch (SecurityException ex) {
            // asume hardfloat
            Logger.getLogger(Platform.class.getName()).log(Level.INFO, "SecurityException while analysing '/proc/self/exe' or the target binary.", ex);
        }
        return false;
    }

    /** Generate a canonical String prefix based on the current OS
        type/arch/name.
    */
    static String getNativeLibraryResourcePrefix() {
        String prefix = System.getProperty("jna.prefix");
        if(prefix != null) {
            return prefix;
        } else {
            return getNativeLibraryResourcePrefix(getOSType(), System.getProperty("os.arch"), System.getProperty("os.name"));
        }
    }

    /** Generate a canonical String prefix based on the given OS
        type/arch/name.
        @param osType from {@link #getOSType()}
        @param arch from <code>os.arch</code> System property
        @param name from <code>os.name</code> System property
    */
    static String getNativeLibraryResourcePrefix(int osType, String arch, String name) {
        String osPrefix;
        arch = getCanonicalArchitecture(arch, osType);
        switch(osType) {
            case Platform.ANDROID:
                if (arch.startsWith("arm")) {
                    arch = "arm";
                }
                osPrefix = "android-" + arch;
                break;
            case Platform.WINDOWS:
                osPrefix = "win32-" + arch;
                break;
            case Platform.WINDOWSCE:
                osPrefix = "w32ce-" + arch;
                break;
            case Platform.MAC:
                osPrefix = "darwin-" + arch;
                break;
            case Platform.LINUX:
                osPrefix = "linux-" + arch;
                break;
            case Platform.SOLARIS:
                osPrefix = "sunos-" + arch;
                break;
            case Platform.DRAGONFLYBSD:
                osPrefix = "dragonflybsd-" + arch;
                break;
            case Platform.FREEBSD:
                osPrefix = "freebsd-" + arch;
                break;
            case Platform.OPENBSD:
                osPrefix = "openbsd-" + arch;
                break;
            case Platform.NETBSD:
                osPrefix = "netbsd-" + arch;
                break;
            case Platform.KFREEBSD:
                osPrefix = "kfreebsd-" + arch;
                break;
            default:
                osPrefix = name.toLowerCase();
                int space = osPrefix.indexOf(" ");
                if (space != -1) {
                    osPrefix = osPrefix.substring(0, space);
                }
                osPrefix += "-" + arch;
                break;
        }
        return osPrefix;
    }

    public static String getMultiArchPath() {
        String cpu = ARCH;
        String kernel = iskFreeBSD()
            ? "-kfreebsd"
            : (isGNU() ? "" : "-linux");
        String libc = "-gnu";

        if (isIntel()) {
            cpu = (is64Bit() ? "x86_64" : "i386");
        }
        else if (isPPC()) {
            cpu = cpu.replace("ppc", "powerpc");
        }
        else if (isARM()) {
            cpu = (is64Bit() ? "aarch64" : "arm");
            libc = is64Bit()
                ? "-gnu"
                : ("armel".equals(ARCH) ? "-gnueabi" : "-gnueabihf");
        }
        else if (ARCH.equals("mips64el")) {
            libc = "-gnuabi64";
        }

        return cpu + kernel + libc;
    }
}
