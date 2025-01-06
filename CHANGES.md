NOTE: as of JNA 4.0, JNA is now dual-licensed under LGPL and AL 2.0 (see LICENSE).

NOTE: JNI native support is typically incompatible between minor versions, and almost always incompatible between major versions.

Next Release (5.17.0)
=====================

Features
--------
* [#1537](https://github.com/java-native-access/jna/pull/1537): Add MSI database access functions - [@russellbanks](https://github.com/russellbanks)

Bug Fixes
---------


Release 5.16.0
==============

Features
--------
* [#1626](https://github.com/java-native-access/jna/pull/1626): Add caching of field list and field validation in `Structure` along with more efficient reentrant read-write locking instead of synchronized() blocks - [@BrettWooldridge](https://github.com/brettwooldridge)

Bug Fixes
---------
* [#1618](https://github.com/java-native-access/jna/issues/1618): Fix calls to jnidispatch on Android with 16KB page size - [@Thomyrock](https://github.com/Thomyrock)

Release 5.15.0
==============

Features
--------
* [#1578](https://github.com/java-native-access/jna/pull/1578): Add support for FreeBSD aarch64 - [@alexdupre](https://github.com/alexdupre).
* [#1593](https://github.com/java-native-access/jna/pull/1593): Add support for DragonFly BSD x86-64 - [@liweitianux](https://github.com/liweitianux).
* [#1595](https://github.com/java-native-access/jna/pull/1595): Add `IsProcessorFeaturePresent` to `c.s.j.p.win32.Kernel32` - [@dbwiddis](https://github.com/dbwiddis).
* [#1602](https://github.com/java-native-access/jna/pull/1602): Add `XMoveWindow`, `XResizeWindow`, `XMoveResizeWindow`, `XRaiseWindow`, `XLowerWindow` X11 calls to `c.s.j.p.unix.X11` - [@vinceh121](https://github.com/vinceh121).
* [#1613](https://github.com/java-native-access/jna/issues/1613): Added static helper method `Native#getNativeLibrary' for getting the underlying NativeLibrary instance from a Library interface instance or from a "registered" class - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1624](https://github.com/java-native-access/jna/pull/1624): Enable linker build-id for android builds - [@mstyura](https://github.com/mstyura).

Bug Fixes
---------
* [#1579](https://github.com/java-native-access/jna/issues/1579): Fix analysis of ELF binary on arm systems running with a java ELF binary without section table headers (java8 on armv7 NAS) - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1586](https://github.com/java-native-access/jna/issues/1586): Fix free_callback JNI weak reference leak - [@xiezhaokun](https://github.com/xiezhaokun).
* [6486c90d913a413f247eef84742ce3c474738933](https://github.com/java-native-access/jna/commit/6486c90d913a413f247eef84742ce3c474738933): Check CallbackReference#cbstruct for null when checking existing Reference - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1622](https://github.com/java-native-access/jna/issues/1622): Add "linux-riscv64" entry to OSGI Bundle-NativeCode header in MANIFEST.MF  - [@matthiasblaesing](https://github.com/matthiasblaesing).

Release 5.14.0
==============

Features
--------
* [#1556](https://github.com/java-native-access/jna/pull/1556): Add `SetJob`, `SetPrinter` to `c.s.j.p.w.Winspool` - [@tresf](https://github.com/tresf).
* [#1534](https://github.com/java-native-access/jna/pull/1534): Add `GetMethod`, `Put`, `SpawnInstance` to `c.s.j.p.win32.COM.WbemCli#IWbemClassObject` and `ExecMethod` to `c.s.j.p.win32.COM.WbemCli#IWbemServices` - [@faddom](https://github.com/faddom).
* [#1544](https://github.com/java-native-access/jna/pull/1544): Add `GetPriorityClass`, `SetPriorityClass`, `GetThreadPriority`, `SetThreadPriority` and associated constants to `c.s.j.p.win32.Kernel32` - [@dEajL3kA](https://github.com/dEajL3kA).
* [#1548](https://github.com/java-native-access/jna/pull/1548): Make interface `c.s.j.p.mac.XAttr public` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1551](https://github.com/java-native-access/jna/pull/1551): Add `c.s.j.p.bsd.ExtAttr` and `c.s.j.p.bsd.ExtAttrUtil` to wrap BSD [<sys/extattr.h>](https://man.freebsd.org/cgi/man.cgi?query=extattr&sektion=2) system calls. [@rednoah](https://github.com/rednoah).
* [#1517](https://github.com/java-native-access/jna/pull/1517): Add missing `O_*` (e.g. `O_APPEND`, `O_SYNC`, `O_DIRECT`, ...) to `c.s.j.p.linux.Fcntl` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1521](https://github.com/java-native-access/jna/issues/1521): Shutdown CleanerThread once the last cleanable is removed - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1557](https://github.com/java-native-access/jna/issues/1557): Build linux-riscv64 on Ubuntu focal to improve compatibility with older glibc versions - [@matthiasblaesing](https://github.com/matthiasblaesing).

Bug Fixes
---------
* [#1501](https://github.com/java-native-access/jna/pull/1501): `Library.OPTION_STRING_ENCODING` is ignore for string arguments function calls - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1504](https://github.com/java-native-access/jna/pull/1504): Increase maximum supported fixed args on varargs calls from 3 to 255 - [@andrew-nowak](https://github.com/andrew-nowak).
* [#1545](https://github.com/java-native-access/jna/pull/1545): Fix Java 6 incompatibility in `c.s.j.p.win32.Kerne32Util` and `c.s.j.p.win32.DBT` - [@matthiasblaesing](https://github.com/matthiasblaesing).

Important Changes
-----------------
* The interfaces between Java and native code have changed, so `libjnidispatch`
  must be rebuilt to be compatible with this release.
* Release drops support for JDKs 6 + 7, so you'll need at least JDK 8 to
  update to use this version.

Release 5.13.0
==============

Features
--------
* [#1454](https://github.com/java-native-access/jna/pull/1454): Add `c.s.j.p.win32.Psapi.QueryWorkingSetEx` and associated Types - [@crain-32](https://github.com/Crain-32).
* [#1459](https://github.com/java-native-access/jna/pull/1459): Add `VirtualLock` and `VirtualUnlock` in `c.s.j.p.win32.Kernel32` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1471](https://github.com/java-native-access/jna/pull/1471): Add `c.s.j.p.win32.Advapi32Util#isCurrentProcessElevated` and associated Types - [@dbwiddis](https://github.com/dbwiddis).
* [#1474](https://github.com/java-native-access/jna/pull/1474): Add `c.s.j.p.win32.WbemCli#IWbemClassObject.IWbemQualifierSet`, `IWbemServices.GetObject`, `IWbemContext.SetValue` and associated methods - [@rchateauneu](https://github.com/rchateauneu).
* [#1482](https://github.com/java-native-access/jna/pull/1482): Add multilingual support of `Kernel32Util.formatMessage` - [@overpathz](https://github.com/overpathz).
* [#1490](https://github.com/java-native-access/jna/pull/1490): Adds support for a custom `SymbolProvider` in `NativeLibrary` & `Library` - [@soywiz](https://github.com/soywiz).
* [#1491](https://github.com/java-native-access/jna/pull/1491): Update libffi to v3.4.4  - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1487](https://github.com/java-native-access/jna/issues/1487): Add 'uses' information to OSGI metadata in MANIFEST.MF to improve stability of package resolution - [@sratz](https://github.com/sratz).

Bug Fixes
---------
* [#1452](https://github.com/java-native-access/jna/issues/1452): Fix memory allocation/handling for error message generation in native library code (`dispatch.c`) - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1460](https://github.com/java-native-access/jna/issues/1460): Fix win32 variant date conversion in DST offest window and with millisecond values - [@eranl](https://github.com/eranl).
* [#1472](https://github.com/java-native-access/jna/issues/1472): Fix incorrect bitmask in `c.s.j.Pointer#createConstant(int)` - [@dbwiddis](https://github.com/dbwiddis).
* [#1481](https://github.com/java-native-access/jna/issues/1481): Fix NPE in NativeLibrary when unpacking from classpath is disabled - [@trespasserw](https://github.com/trespasserw).
* [#1489](https://github.com/java-native-access/jna/pull/1489): Fixes typo in `OpenGL32Util#wglGetProcAddress`, instead of parameter `procName` the hardcoded value `wglEnumGpusNV` was used - [@soywiz](https://github.com/soywiz).

Release 5.12.1
==============

Bug Fixes
---------
* [#1447](https://github.com/java-native-access/jna/issues/1447): Null-check cleanable in `c.s.j.Memory#close` - [@dbwiddis](https://github.com/dbwiddis).

Release 5.12.0
==============

Features
--------
* [#1433](https://github.com/java-native-access/jna/pull/1433): Add `CFEqual`, `CFDictionaryRef.ByReference`, `CFStringRef.ByReference` to `c.s.j.p.mac.CoreFoundation` - [@shalupov](https://github.com/shalupov)
* [#978](https://github.com/java-native-access/jna/issues/978): Remove use of finalizers in JNA and improve concurrency for `Memory`, `CallbackReference` and `NativeLibrary` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1440](https://github.com/java-native-access/jna/pull/1440): Support for LoongArch64 - [@Panxuefeng-loongson](https://github.com/Panxuefeng-loongson).
* [#1444](https://github.com/java-native-access/jna/pull/1444): Update embedded libffi to 1f14b3fa92d4442a60233e9596ddec428a985e3c and rebuild native libraries - [@matthiasblaesing](https://github.com/matthiasblaesing).

Bug Fixes
---------
* [#1438](https://github.com/java-native-access/jna/pull/1438): Handle arrays in structures with differing size - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1442](https://github.com/java-native-access/jna/issues/1442): Handle race condition in `c.s.j.p.win32.PdhUtil#PdhEnumObjectItems` - [@dbwiddis](https://github.com/dbwiddis).

Important Changes
-----------------
* `Memory#dispose`, `CallbackReference#dispose` and `NativeLibrary#dispose`
   were called by the `Object#finalize` override. These calls were replaced by
   the use of a cleaner. It is not guaranteed anymore, that `dispose` is called
   on subclasses on finalization.

Release 5.11.0
==============

Features
--------
* [#1398](https://github.com/java-native-access/jna/pull/1398): Increase `c.s.j.p.win32.Sspi#MAX_TOKEN_SIZE` on Windows 8/Server 2012 and later - [@dbwiddis](https://github.com/dbwiddis).
* [#1403](https://github.com/java-native-access/jna/pull/1403): Rebuild AIX binaries with libffi 3.4.2 (other architectures were part of 5.10) - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1404](https://github.com/java-native-access/jna/issues/1404): Added Solaris Kstat2 library - [@dbwiddis](https://github.com/dbwiddis).
* [#1416](https://github.com/java-native-access/jna/pull/1416): Add `CFDictionaryGetCount` to `c.s.j.p.mac.CoreFoundation` - [@shalupov](https://github.com/shalupov)
* [#1418](https://github.com/java-native-access/jna/pull/1418): Add `CertOpenStore` to `c.s.j.p.win32.Crypt32` - [@shalupov](https://github.com/shalupov)

Bug Fixes
---------
* [#1411](https://github.com/java-native-access/jna/pull/1411): Do not throw `Win32Exception` on success for empty section in `Kernel32Util#getPrivateProfileSection` - [@mkarg](https://github.com/mkarg).
* [#1414](https://github.com/java-native-access/jna/pull/1414): Fix definition of `c.s.j.p.unix.X11.XK_Shift_R` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1323](https://github.com/java-native-access/jna/issues/1323). Fix crashes in direct callbacks on mac OS aarch64 - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1422](https://github.com/java-native-access/jna/pull/1422): Load jawt library relative to `sun.boot.library.path` system on unix OSes - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1427](https://github.com/java-native-access/jna/pull/1427): Rebuild all binaries with fix from #1422 and #1323 - [@matthiasblaesing](https://github.com/matthiasblaesing).

Release 5.10.0
==============

Features
--------
* [#1377](https://github.com/java-native-access/jna/pull/1377): Add `RegLoadAppKey` to `c.s.j.p.win32.Advapi32` and `registryLoadAppKey` to `c.s.j.p.win32.Advapi32Util` - [@mfilippov](https://github.com/mfilippov).
* [#1093](https://github.com/java-native-access/jna/issues/1093): Add `OpenFileMapping` to `c.s.j.p.win32.Kernel32` - [@lmitusinski](https://github.com/lmitusinski).
* [#1388](https://github.com/java-native-access/jna/issues/1388): Map the arch `zarch_64` as reported by SAPJVM8 to `s390x` - [@MBaesken](https://github.com/MBaesken).
* [#1381](https://github.com/java-native-access/jna/issues/1381): Update embedded libffi to 3.4.2 - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1393](https://github.com/java-native-access/jna/issues/1393): Update native encoding detection for JEP400 / JDK 18 (`file.encoding` now defaults to `UTF-8`) - [@matthiasblaesing](https://github.com/matthiasblaesing).

Bug Fixes
---------
* [#1378](https://github.com/java-native-access/jna/pull/1378): Handle failure in `ffi_closure_alloc` - [@davecturner](https://github.com/davecturner).


Release 5.9.0
=============

Features
--------
* [#1336](https://github.com/java-native-access/jna/pull/1336): Add `HKEY_CURRENT_USER_LOCAL_SETTINGS` to `c.s.j.p.win32.WinReg` - [@Dani-Hub](https://github.com/Dani-Hub).
* [#1337](https://github.com/java-native-access/jna/pull/1337): Add `REG_NOTIFY_THREAD_AGNOSTIC` to `c.s.j.p.win32.WinNet` and update `REG_LEGAL_CHANGE_FILTER` - [@Dani-Hub](https://github.com/Dani-Hub).
* [#1338](https://github.com/java-native-access/jna/pull/1338): Add `RegNotifyChangeKeyValue` to `c.s.j.p.win32.Advapi32` - [@Dani-Hub](https://github.com/Dani-Hub).
* [#1340](https://github.com/java-native-access/jna/issues/1340): Add `CM_Get_DevNode_Registry_Property` to `c.s.j.p.win32.Cfgmgr32` and corresponding util in `c.s.j.p.win32.Cfgmgr32Util` - [@dbwiddis](https://github.com/dbwiddis).
* [#1352](https://github.com/java-native-access/jna/pull/1352): Add `BringWindowToTop` to `c.s.j.p.win32.User32` - [@kahgoh](https://github.com/kahgoh).
* [#1354](https://github.com/java-native-access/jna/pull/1352): Add `GetParent` to `c.s.j.p.win32.User32` - [@kahgoh](https://github.com/kahgoh).
* [#1360](https://github.com/java-native-access/jna/issues/1360): Add `CommandLineToArgvW` to `c.s.j.p.win32.Shell32` and corresponding util in `c.s.j.p.win32.Shell32Util` - [@dbwiddis](https://github.com/dbwiddis).
* [#1363](https://github.com/java-native-access/jna/issues/1363): Update `NUMA_NODE_RELATIONSHIP` in `c.s.j.p.win32.WinNT` to new version of the structure and improve support for future values of `c.s.j.p.win32.WinNT.LOGICAL_PROCESSOR_RELATIONSHIP` - [@dbwiddis](https://github.com/dbwiddis).

Bug Fixes
---------
* [#1343](https://github.com/java-native-access/jna/issues/1343): `c.s.j.p.mac.CoreFoundation.CFStringRef#stringValue` buffer needs space for a null byte - [@dbwiddis](https://github.com/dbwiddis).
* [#1351](https://github.com/java-native-access/jna/issues/1351): Define `c.s.j.p.unix.size_t.ByReference` and fix macOS sysctl `size_t *` parameters - [@dbwiddis](https://github.com/dbwiddis).
* [#1362](https://github.com/java-native-access/jna/issues/1362): Clear security sensitive data after usage in `c.s.j.p.win32.Crypt32Util#cryptProtectData` and `#cryptUnprotectData` - [@dmytro-sheyko](https://github.com/dmytro-sheyko).
* [#1361](https://github.com/java-native-access/jna/issues/1361): Make `c.s.j.p.win32.Crypt32Util#cryptProtectData` and `#cryptUnprotectData` properly handle 0-length array as input - [@dmytro-sheyko](https://github.com/dmytro-sheyko).

Release 5.8.0
=============

Features
--------
* [#1313](https://github.com/java-native-access/jna/issues/1313): Normalize `RESOURCE_PREFIX` for darwin to `darwin-$arch` and split jnidispatch library per architecture - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1318](https://github.com/java-native-access/jna/pull/1318): Add support for linux-riscv64 - [@thentschel](https://github.com/thentschel).
* [#1327](https://github.com/java-native-access/jna/pull/1327): Add partial support for future values of `c.s.j.p.win32.WinNT.LOGICAL_PROCESSOR_RELATIONSHIP` enum present in Windows Insider builds - [@dbwiddis](https://github.com/dbwiddis).

Bug Fixes
---------
* [#1317](https://github.com/java-native-access/jna/pull/1317): Change the maven coordinates of the JPMS artifacts from classifier `jpms` to custom artifact ids `jna-jpms` and `jna-platform-jpms` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1322](https://github.com/java-native-access/jna/pull/1322): Handle 0-length domain names in `c.s.j.p.win32.Advapi32Util#getAccountBySid` - [@dbwiddis](https://github.com/dbwiddis).
* [#1326](https://github.com/java-native-access/jna/pull/1326): Ensure pointers indirected from Memory and pointing into Memory retain originating object - [@matthiasblaesing](https://github.com/matthiasblaesing).

Important Changes
-----------------
* The maven coordinates of the experimental JPMS (java module system) artifacts
  were moved from using the classifier `jpms` to custom artifact ids `jna-jpms`
  and `jna-platform-jpms`, without an classifier. The reason for this is, that
  the platform artifacts depend on the jna artifacts and need to pull in the
  right variant. This is not possible if the classifier is used.

* `RESOURCE_PREFIX` for darwin (mac OS) was changed from `darwin` to
  `darwin-$arch` as the fat binaries on mac OS causes various problems:
  It was reported, that binaries were rejected from the appstore because x86
  binaries were found in the application (jnidispatch for mac OS x86) and that
  builds needed to be special cased so that the native library can be
  assembled. The latter is also true for JNA.<br />
  While the prefix is changed, the old prefix is still searched as a fallback
  location, so if only a fat binary is present, it can still be loaded.

Release 5.7.0
=============

Features
--------
* [#1301](https://github.com/java-native-access/jna/pull/1301/): Improve bindings of the printer notification functions (`FindFirstPrinterChangeNotification`, `FindNextPrinterChangeNotification`) in `c.s.j.p.w.Winspool` - [@ianjoneill](https://github.com/ianjoneill).
* [#1238](https://github.com/java-native-access/jna/pull/1238): Add macOS `aarch64` architecture to universal `darwin` target. Cherry pick [libffi/libffi#577](https://github.com/libffi/libffi/pull/577). - [@fkistner](https://github.com/fkistner), [@Vzor-](https://github.com/Vzor-), [@tresf](https://github.com/tresf).
* [#1264](https://github.com/java-native-access/jna/pull/1264): Update libffi to v3.3; Add Windows `aarch64` target. - [@tresf](https://github.com/tresf).
* [#1293](https://github.com/java-native-access/jna/issues/1293): Bind part of Windows Application Recovery and Restart API: `RegisterApplicationRestart`, `UnregisterApplicationRestart` and `GetApplicationRestartSettings` in `c.s.j.p.w.Kernel32` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1217](https://github.com/java-native-access/jna/pull/1217): Add mappings for AIX `Perfstat` library to `c.s.j.p.unix.aix` - [@dbwiddis](https://github.com/dbwiddis).
* [#1231](https://github.com/java-native-access/jna/pull/1231): The test suite can now be executed on Windows using either ANSI or UNICODE win32 API by passing `-Dw32.ascii=true/false` to ant. Previously, UNICODE was always used. - [@T-Svensson](https://github.com/T-Svensson/)
* [#1237](https://github.com/java-native-access/jna/pull/1237): *Experimental:* Add artifacts that make jna and jna-platform named modules (provide `module-info.class`). The new artifacts are named `jna-jpms.jar` and `jna-platform-jpms.jar` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1242](https://github.com/java-native-access/jna/pull/1242): Add CallWindowProc to User32 - [@heldplayer](https://github.com/heldplayer).
* [#1239](https://github.com/java-native-access/jna/pull/1239): Improve performance of allocation of `c.s.j.Memory` objects - [@joerg1985](https://github.com/joerg1985).
* [#1246](https://github.com/java-native-access/jna/pull/1246): Improve performance of `c.s.j.Structure#read` and `c.s.j.Structure#write` - [@joerg1985](https://github.com/joerg1985).
* [#1260](https://github.com/java-native-access/jna/pull/1260): Add mapping for X11 generic events - [@lafoletc](https://github.com/lafoletc).
* [#1263](https://github.com/java-native-access/jna/pull/1263): Add LowLevelMouseProc - [@nordiakt](https://github.com/nordiakt).
* [#1265](https://github.com/java-native-access/jna/pull/1265): Add mapping for XQueryExtension - [@lafoletc](https://github.com/lafoletc).
* [#1299](https://github.com/java-native-access/jna/pull/1299): Add `c.s.j.p.win32.IPHlpApi#GetExtendedTcpTable`, `c.s.j.p.win32.IPHlpApi#GetExtendedUdpTable`, and supporting structures.  - [@dbwiddis](https://github.com/dbwiddis).

Bug Fixes
---------
* [#1286](https://github.com/java-native-access/jna/pull/1286): Fix bindings of `c.s.j.p.win32.DBT` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#326](https://github.com/java-native-access/jna/issues/326): Fix loading library that re-uses pointers for different callbacks - [@fpapai](https://github.com/fpapai).
* [#1244](https://github.com/java-native-access/jna/issues/1244): Fix building on GCC 10 - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1252](https://github.com/java-native-access/jna/issues/1252): - Fix bindings of `CTL_ENTRY#getRgAttribute`, `CTL_INFO#getRgCTLEntry`, `CTL_INFO#getRgExtension`, `CERT_EXTENSIONS#getRgExtension`, `CERT_INFO#getRgExtension`, `CRL_INFO#getRgCRLEntry`, `CRL_INFO#getRgExtension`, `CRL_ENTRY#getRgExtension`. Add bindings for `CertEnumCertificatesInStore`, `CertEnumCTLsInStore`, `CertEnumCRLsInStore` and `CryptQueryObject` in `c.s.j.p.win32.Crypt32`.<br> *WARNING:* The signatures for `CTL_INFO#getRgCTLEntry` and `CTL_INFO#getRgExtension` were changed - as the original signatures were obviously wrong and read the wrong attributes, it is not considered an API break - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1275](https://github.com/java-native-access/jna/issues/1275): Fix `CFStringRef#stringValue` for empty Strings - [@dyorgio](https://github.com/dyorgio).
* [#1279](https://github.com/java-native-access/jna/issues/1279): Remove `DLLCallback` import from `CallbackReference` - [@dyorgio](https://github.com/dyorgio).
* [#1278](https://github.com/java-native-access/jna/pull/1278): Improve compatibility of `c.s.j.p.WindowUtils#getProcessFilePath` and fix unittests for windows 32bit intel  - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1284](https://github.com/java-native-access/jna/pull/1284): Fix illegal access exceptions, when retrieving options for private library interfaces with an instance field - [@fkistner](https://github.com/fkistner).
* [#1300](https://github.com/java-native-access/jna/pull/1300): Deprecate `c.s.j.p.win32.WTypes.BSTR` String constructor and `setValue` method, as `BSTR` allocation should be managed by COM, Automation, and Interop functions - [@dbwiddis](https://github.com/dbwiddis).


Breaking Changes
----------------
* Prebuild native library for darwin x86 (32bit java on mac OS) was removed

Release 5.6.0
=============

Features
--------
* [#1160](https://github.com/java-native-access/jna/issues/1160): Make FileUtils#moveToTrash a varargs method - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1167](https://github.com/java-native-access/jna/pull/1167): Add `c.s.j.p.win32.Kernel32#GetProcessAffinityMask` - [@dbwiddis](https://github.com/dbwiddis).
* [#1168](https://github.com/java-native-access/jna/pull/1168): Add `c.s.j.p.win32.Kernel32#SetProcessAffinityMask` - [@dbwiddis](https://github.com/dbwiddis).
* [#1169](https://github.com/java-native-access/jna/issues/1169): Wait for process in getLinuxLdPaths - [@rdesgroppes](https://github.com/rdesgroppes).
* [#1178](https://github.com/java-native-access/jna/pull/1178): Add `c.s.j.p.win32.IPHlpAPI#GetTcpStatistics`, `c.s.j.p.win32.IPHlpAPI#GetUdpStatistics`, `c.s.j.p.win32.IPHlpAPI#GetTcpStatisticsEx` and `c.s.j.p.win32.IPHlpAPI#GetUdpStatisticsEx` - [@dbwiddis](https://github.com/dbwiddis).
* [#1182](https://github.com/java-native-access/jna/pull/1182): Add `toString` to classes extending `c.s.j.ptr.ByReference` - [@dbwiddis](https://github.com/dbwiddis).
* [#1191](https://github.com/java-native-access/jna/pull/1191): Add `c.s.j.p.win32.Advapi32Util#getTokenPrimaryGroup` - [@dbwiddis](https://github.com/dbwiddis).
* [#1194](https://github.com/java-native-access/jna/pull/1194): Add `GetConsoleScreenBufferInfo`, `ReadConsoleInput` and `WriteConsole` with associated structures to `c.s.j.p.win32.Wincon` - [@rednoah](https://github.com/rednoah).
* [#1198](https://github.com/java-native-access/jna/pull/1198): Add `NetSessionEnum` to `c.s.j.p.win32.Netapi32` and `WTSEnumerateSessions`, `WTSQuerySessionInformation`, and `WTSFreeMemory` to `c.s.j.p.win32.Wtsapi32` - [@dbwiddis](https://github.com/dbwiddis).
* [#1200](https://github.com/java-native-access/jna/pull/1200): Add mappings for `libudev` to `c.s.j.p.linux.Udev` - [@dbwiddis](https://github.com/dbwiddis).
* [#1202](https://github.com/java-native-access/jna/pull/1202): Add mappings supporting shared memory including `c.s.j.p.unix.LibCAPI` types `size_t` and `ssize_t`, `c.s.j.p.linux.LibC` methods `munmap()`, `msync()`, and `close()`, `c.s.j.p.unix.LibCUtil` mapping `mmap()` and `ftruncate()`, and `c.s.j.p.linux.LibRT` methods `shm_open()` and `shm_unlink()` - [@dbwiddis](https://github.com/dbwiddis).
* [#1209](https://github.com/java-native-access/jna/pull/1209): Add mappings for `Thread32First` and `Thread32Next` to `c.s.j.p.win32.Kernel32` - [@dbwiddis](https://github.com/dbwiddis).
* [#1214](https://github.com/java-native-access/jna/pull/1214): Add mapping for EnumProcesses to `c.s.j.p.win32.Psapi` and `c.s.j.p.win32.PsapiUtil` - [@T-Svensson](https://github.com/T-Svensson/).

Bug Fixes
---------
* [#1183](https://github.com/java-native-access/jna/pull/1183): `c.s.j.p.win32.WinDef.CHARByReference#getValue` should only read one byte - [@dbwiddis](https://github.com/dbwiddis).
* [#1184](https://github.com/java-native-access/jna/pull/1184): `c.s.j.p.win32.WinDef.ULONGLONG` should always be 8 bytes - [@dbwiddis](https://github.com/dbwiddis).
* [#1196](https://github.com/java-native-access/jna/pull/1196): `c.s.j.p.win32.WinNT.LARGE_INTEGER` needs to populate both union fields - [@dbwiddis](https://github.com/dbwiddis).
* [#1216](https://github.com/java-native-access/jna/pull/1216): Failure loading frameworks on macOS 11 - [@dkocher](https://github.com/dkocher).

Release 5.5.0
=============

Features
--------
* [#1131](https://github.com/java-native-access/jna/pull/1131): Add CoreFoundation, IOKit, and DiskArbitration mappings in `c.s.j.p.mac`. [@dbwiddis](https://github.com/dbwiddis).
* [#1143](https://github.com/java-native-access/jna/pull/1143): `c.s.j.p.mac.SystemB` now extends `c.s.j.p.unix.LibCAPI`. [@dbwiddis](https://github.com/dbwiddis).
* [#1147](https://github.com/java-native-access/jna/pull/1147): Add additional OSGi headers for the JNA bundle to support 32bit ARM (hardfloat) [@mattixtech](https://github.com/mattixtech).
* [#1148](https://github.com/java-native-access/jna/pull/1148)/[#1096](https://github.com/java-native-access/jna/issues/1096): Include Win32 COM utils (`c.s.j.p.win32.com.util` and `c.s.j.p.win32.com.annotation`) in OSGI bundle [@dbwiddis](https://github.com/dbwiddis).

Bug Fixes
---------
* [#1115](https://github.com/java-native-access/jna/issues/1115): Fix signature for `c.s.j.p.win32.Kernel32#CreateRemoteThread` and bind `VirtualAllocEx`, `VirtualFreeEx`, `GetExitCodeThread` in `c.s.j.p.win32.Kernel32` - [@apangin](https://github.com/apangin), [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1127](https://github.com/java-native-access/jna/issues/1127): Windows needs a wide string in `c.s.j.p.win32.COM.IShellFolder#ParseDisplayName` - [@dbwiddis](https://github.com/dbwiddis).
* [#1128](https://github.com/java-native-access/jna/issues/1128): KEY_ALL_ACCESS value is incorrect in `c.s.j.p.win32.WinNT.java` - [@trevormaggs](https://github.com/trevormaggs).
* [#1133](https://github.com/java-native-access/jna/issues/1133): Ensure JARs created from the build system don't contain invalid `Info-ZIP Unicode Path` extra info - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1134](https://github.com/java-native-access/jna/issues/1134): Read correct member of `WinBase.SYSTEM_INFO.processorArchitecture` union - [@dbwiddis](https://github.com/dbwiddis).
* [#1118](https://github.com/java-native-access/jna/issues/1118): Fix passing unions containing integer and floating point members as parameters by value - [@matthiasblaesing](https://github.com/matthiasblaesing).

Release 5.4.0
=============

Features
--------
* [#1105](https://github.com/java-native-access/jna/issues/1105): Deprecate `c.s.j.p.win32.Advapi32Util.EventLogRecord#getEventId` in favor of `#getInstanceId` - [@dbwiddis](https://github.com/dbwiddis).
* [#1097](https://github.com/java-native-access/jna/issues/1097): Allow `.ocx` as extension of native libraries on windows - [@dmigowski](https://github.com/dmigowski).
* [#1108](https://github.com/java-native-access/jna/pull/1108): Improve performance of `c.s.j.Structure#newInstance` by iteration available constructors instead of exception handling [@bjorndarri](https://github.com/bjorndarri).

Bug Fixes
---------
* [#1095](https://github.com/java-native-access/jna/pull/1095) Align behaviour of com.sun.jna.platform.macXAttrUtil#setXattr and #getXAttr with CLI tool - [@jrobhoward](https://github.com/jrobhoward), [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1091](https://github.com/java-native-access/jna/issues/1091): Check target number to be greater than zero, before calling `Structure#toArray` in `c.s.j.p.win32.Netapi32Util` - [@trevormagg](https://github.com/trevormaggs), [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1103](https://github.com/java-native-access/jna/issues/1103): On big endian architecture IntegerType based values are incorrectly decoded when using direct binding - [@matthiasblaesing](https://github.com/matthiasblaesing).

Release 5.3.1
=============

Bug Fixes
---------
* [#1089](https://github.com/java-native-access/jna/issues/1089): `c.s.j.internal.ReflectionUtils` accesses `java.lang.invoke.MethodType` without reflection, causing `java.lang.NoClassDefFoundError` on android API level < 26 - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1087](https://github.com/java-native-access/jna/pull/1087): Fix wrong calls to Structure#toArray with zero sized arrays - [@matthiasblaesing](https://github.com/matthiasblaesing).

Release 5.3.0
=============

Features
--------
* [#1058](https://github.com/java-native-access/jna/pull/1058): Add selectable timeout to stopService() and improve timeout handling - [@keithharp](https://github.com/keithharp).
* [#1050](https://github.com/java-native-access/jna/pull/1050): Add `c.s.j.p.win32.VersionHelpers` and supporting functions - [@dbwiddis](https://github.com/dbwiddis).
* [#1061](https://github.com/java-native-access/jna/pull/1061): replace toArray(new T[size]) with toArray(new T[0]) for better performance - [@hc-codersatlas](https://github.com/hc-codersatlas).
* [#1064](https://github.com/java-native-access/jna/pull/1064): Add `c.s.j.p.win32.Kernel32.GetLogicalProcessorInformationEx` function, convenience Util method and supporting structures - [@dbwiddis](https://github.com/dbwiddis).
* [#1065](https://github.com/java-native-access/jna/pull/1065): Add `c.s.j.p.win32.PowrProf#CallNTPowerInformation` and supporting structures - [@dbwiddis](https://github.com/dbwiddis).
* [#1063](https://github.com/java-native-access/jna/pull/1063): Enhance `c.s.j.p.win32.User32` and associated classes to support keyboard related functionality. - [@kevemueller](https://github.com/kevemueller).
* [#1068](https://github.com/java-native-access/jna/pull/1068): `c.s.j.p.win32.Advapi32Util.getAccountBySid(String systemName, PSID sid)` ignored parameter instead of passing it to the native function - [@nirud](https://github.com/nirud).
* [#813](https://github.com/java-native-access/jna/issues/813): Support for default methods on interfaces (experimental) - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1073](https://github.com/java-native-access/jna/issues/1073): Support COM setters with multiple parameters using `c.s.j.p.win32.COM.util.ProxyObject` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1083](https://github.com/java-native-access/jna/issues/1083): Prevent access to unsupported values in `c.s.j.p.win32.COM.WbemcliUtil#enumerateProperties` and bind `c.s.j.p.win32.COM.Wbemcli.IWbemClassObject.GetNames` - [@matthiasblaesing](https://github.com/matthiasblaesing).

Bug Fixes
---------
* [#1052](https://github.com/java-native-access/jna/pull/1052), [#1053](https://github.com/java-native-access/jna/issues/1053): WinXP compatibility for `c.s.j.p.win32.PdhUtil` - [@dbwiddis](https://github.com/dbwiddis).
* [#1055](https://github.com/java-native-access/jna/pull/1055): Include `c.s.j.p.linux` in OSGi bundle. - [@dbwiddis](https://github.com/dbwiddis).
* [#1066](https://github.com/java-native-access/jna/issues/1066): On AIX OpenJDK differs from IBM J9 in the mapping of library names. While J9 maps jnidispatch to `libjnidispatch.a`, OpenJDK maps to `libjnidispatch.so`, which causes the native library extractor to fail. AIX is now hard-coded to `libjnidispatch.a` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1079](https://github.com/java-native-access/jna/issues/1079): Fix maximum structure alignment for Android i386 - [@BugsBeGone](https://github.com/BugsBeGone).

Release 5.2.0
=============

Features
--------
* [#1038](https://github.com/java-native-access/jna/pull/1038): Improve exception when native library loading fails by preserving the original exceptions and messages - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1039](https://github.com/java-native-access/jna/pull/1039): Remove use of `System.out` and `System.err` in favor of regular logging - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1048](https://github.com/java-native-access/jna/pull/1048): Bind `c.s.j.p.win32.Kernel32#OpenEvent` - [@matthiasblaesing](https://github.com/matthiasblaesing).

Bug Fixes
---------
* [#1036](https://github.com/java-native-access/jna/issues/1036): `Advapi32Util.registryValueExists` called on non existing key raises exception instead of returning `false` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#384](https://github.com/java-native-access/jna/issues/384): Android only supports loading libraries through the JVM `System#loadLibrary` mechanism, defaulting `jna.nosys` to `true` disabled that code path - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1041](https://github.com/java-native-access/jna/pull/1041): Avoid IllegalArgumentException when reading xattrs with zero length - [@jrobhoward](https://github.com/jrobhoward).
* [#1042](https://github.com/java-native-access/jna/issues/1042): `c.s.j.p.WindowUtils.W32WindowUtils.getProcessFilePath(HWND)` does not close process handle - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1044](https://github.com/java-native-access/jna/issues/1044): Fix field order for `c.s.j.p.unix.X11.XResizeRequestEvent` - [@matthiasblaesing](https://github.com/matthiasblaesing).

Release 5.1.0
=============

Features
--------
* [#1029](https://github.com/java-native-access/jna/issues/1029): Add `statvfs` to `c.s.j.platform.linux.LibC` - [@dbwiddis](https://github.com/dbwiddis).
* [#1032](https://github.com/java-native-access/jna/pull/1032): Deprecate `c.s.j.platform.win32.COM.util.annotation.ComEventCallback` in favour of `c.s.j.platform.win32.COM.util.annotation.ComMethod` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1021](https://github.com/java-native-access/jna/pull/1021): Added `com.sun.jna.platform.linux.XAttr` and `com.sun.jna.platform.linux.XAttrUtil` JNA wrapper for `<sys/xattr.h>` for Linux - [@wilx](https://github.com/wilx).
* [#381](https://github.com/java-native-access/jna/issues/381): Bind `c.s.j.p.win32.Secur32#CompleteAuthToken` - [@matthiasblaesing](https://github.com/matthiasblaesing).

Bug Fixes
---------
* [#1025](https://github.com/java-native-access/jna/issues/1025): Restore java 6 compatibility and introduce animal-sniffer to prevent regressions - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1027](https://github.com/java-native-access/jna/issues/1027): Fix Linux LibC.Sysinfo FieldOrder - [@dbwiddis](https://github.com/dbwiddis).
* [#1033](https://github.com/java-native-access/jna/pull/1033): Replace deprecated Class#newInstance calls - [@matthiasblaesing](https://github.com/matthiasblaesing).

Release 5.0.0
=============

Features
--------
* [#975](https://github.com/java-native-access/jna/pull/982): Add package versions to OSGi metadata. - [@io7m](https://github.com/io7m).
* [#915](https://github.com/java-native-access/jna/pull/915): Adding interfaces to call to Cryptui and Crypt32 windows libraries and adding related structures to Wincrypt. - [@rosh89](https://github.com/rosh89).
* [#903](https://github.com/java-native-access/jna/pull/903): Carry `HRESULT` in `c.s.j.p.win32.COM.COMException`, introduce `c.s.j.p.win32.COM.COMInvokeException` as subclass of `COMException` for exception as the result of a `IDispatch#Invoke`. The `EXECPINFO` is unwrapped into fields in the `COMInvokeException` and correctly freed. - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#822](https://github.com/java-native-access/jna/issues/822): `Native#loadLibrary` requires that the interface class passed in is an instance of Library. The runtime check can be enhanced by using a constraint generic. This breaks binary compatibility (see notes below) - [@d-noll](https://github.com/d-noll).<br /><br />In a followup, the original `loadLibrary` methods were deprecated and `Native#load` methods were introduced, that hold the new generic definitions. So this change is now binary compatible.
* [#889](https://github.com/java-native-access/jna/issues/889): The `Structure#newInstance` receive the target type as a parameter. This adds a limited generic type, so that the return type ist the target type and not a generic structure, removing the necessity to do an explizit cast - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#913](https://github.com/java-native-access/jna/issues/913): Add `@ComInterface` annotation to `com.sun.jna.platform.win32.COM.util.IConnectionPoint` to make it possible to retrieve it via `IUnknown#queryInterface` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#797](https://github.com/java-native-access/jna/issues/797): Binding `Advapi32#EnumDependendServices`, `Advapi32#EnumServicesStatusEx` and `Advapi32#QueryServiceStatus`. `W32Service#stopService` was modified to be more resilent when stopping service - [@matthiasblaesing](https://github.com/matthiasblaesing).
* Bind `com.sun.jna.platform.win32.Kernel32.ExpandEnvironmentStrings` and add helper method for it as `Kernel32Util#expandEnvironmentStrings` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#935](https://github.com/java-native-access/jna/pull/935): Add RegConnectRegistry to Advapi32 mappings. - [@cxcorp](https://github.com/cxcorp).
* [#947](https://github.com/java-native-access/jna/pull/947):  Allow retrieval of `ACEs` from `com.sun.jna.platform.win32.WinNT.ACL` even if the contained `ACE` is not currently supported - [@jrobhoward](https://github.com/jrobhoward).
* [#954](https://github.com/java-native-access/jna/pull/954): Add `c.s.j.Structure.FieldOrder` annotation to define the field order of a structures without implementing `Structure#getFieldOrder()` - [@idosu](https://github.com/idosu).
* [#959](https://github.com/java-native-access/jna/pull/959): Added `GetProcessTimes` and `GetProcessIoCounters` to `com.sun.jna.platform.win32.Kernel32` - [@dbwiddis](https://github.com/dbwiddis).
* [#952](https://github.com/java-native-access/jna/issues/952): Added `CreateMutex`, `OpenMutex` and `ReleaseMutex` to `com.sun.jna.platform.win32.Kernel32` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#973](https://github.com/java-native-access/jna/issues/973): Added `PdhLookupPerfNameByIndex`, `PdhLookupPerfIndexByName`, and `PdhEnumObjectItems` to `c.s.j.platform.win32.Pdh` and a `c.s.j.platform.win32.PdhUtil` class to access them - [@dbwiddis](https://github.com/dbwiddis).
* [#980](https://github.com/java-native-access/jna/issues/980): Added `PERF_OBJECT_TYPE`, `PERF_COUNTER_BLOCK`, and `PERF_COUNTER_DEFINITION` to `c.s.j.platform.win32.WinPerf` and added `Pointer` constructors to ``PERF_INSTANCE_DEFINITION` and `PERF_DATA_BLOCK` - [@dbwiddis](https://github.com/dbwiddis).
* [#981](https://github.com/java-native-access/jna/issues/981): Added `WTS_PROCESS_INFO_EX`, `WTSEnumerateProcessesEx`, and `WTSFreeMemoryEx` to `c.s.j.platform.win32.Wtsapi32` - [@dbwiddis](https://github.com/dbwiddis).
* [#983](https://github.com/java-native-access/jna/issues/983): Added `GetIfEntry`, `GetIfEntry2`, and `GetNetworkParams` and supporting structures `MIB_IFROW`, `MIB_IF_ROW2`, and `FIXED_INFO` to `c.s.j.platform.win32.IPHlpAPI.java` - [@dbwiddis](https://github.com/dbwiddis).
* [#984](https://github.com/java-native-access/jna/issues/984): Added `CM_Locate_DevNode`, `CM_Get_Parent`, `CM_Get_Child`, `CM_Get_Sibling`, `CM_Get_Device_ID`, and `CM_Get_Device_ID_Size` to `c.s.j.platform.win32.Cfgmgr32.java` and a `c.s.j.platform.win32.Cfgmgr32Util` class for `CM_Get_Device_ID` - [@dbwiddis](https://github.com/dbwiddis).
* [#988](https://github.com/java-native-access/jna/issues/988): Added `PdhLookupPerfIndexByEnglishName` to `c.s.j.platform.win32.PdhUtil` - [@dbwiddis](https://github.com/dbwiddis).
* [#992](https://github.com/java-native-access/jna/pull/992): Improve stability of windows tests and add appveyor configuration for windows CI builds - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#995](https://github.com/java-native-access/jna/pull/995): Added structures and methods to `c.s.j.platform.mac.SystemB` for Process, Network interface, Swapfile, Time, and Filesystem info - [@dbwiddis](https://github.com/dbwiddis).
* [#997](https://github.com/java-native-access/jna/issues/997): Added `Sysinfo` structure and function to `c.s.j.platform.linux.LibC` - [@dbwiddis](https://github.com/dbwiddis).
* [#1001](https://github.com/java-native-access/jna/pull/1001): Added overloads to `c.s.j.platform.win32.Advapi32Util` methods which allow the caller to specify `samDesiredExtra` to request additional registry key security and access rights - [@camw](https://github.com/camw).
* [#1007](https://github.com/java-native-access/jna/pull/1007): Added OSGi export of Solaris package - [@swimmesberger](https://github.com/swimmesberger).
* [#1003](https://github.com/java-native-access/jna/pull/1003): Allow `NativeMapped` to be used with enums - [@koraktor](https://github.com/koraktor).
* [#994](https://github.com/java-native-access/jna/issues/994): Added `CoInitializeSecurity` and `CoSetProxyBlanket` to `c.s.j.platform.win32.Ole32`, added new `c.s.j.platform.win32.Wbemcli` classes needed to query WMI, and added a `WbemcliUtil` class implementing WMI queries. - [@dbwiddis](https://github.com/dbwiddis).
* [#1013](https://github.com/java-native-access/jna/pull/1013): Add automatic module name entries to manifest of jna and jna-platform - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#985](https://github.com/java-native-access/jna/issues/985): Improve handling of dynamicaly extracted native library. On Mac OS X systems `~/Library/Application Support/JNA/temp` and on other Unix like systems `$XDG_CACHE_DIR/JNA/temp` (Default value is: `~/.cache/JNA/temp`) is used - [@matthiasblaesing](https://github.com/matthiasblaesing).
* Bind `com.sun.jna.platform.win32.Kernel32#ExitProcess` - [@Groostav](https://github.com/Groostav).

Bug Fixes
---------
* [#652](https://github.com/java-native-access/jna/issues/652): Dead Lock in class initialization - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#843](https://github.com/java-native-access/jna/pull/843): Correctly bind `com.sun.jna.platform.win32.SecBufferDesc` and add convenience binding as `com.sun.jna.platform.win32.SspiUtil.ManagedSecBufferDesc`. Bind SSPI functions `InitializeSecurityContext`, `AcceptSecurityContext`, `QueryCredentialsAttributes`, `QuerySecurityPackageInfo`, `EncryptMessage`, `DecryptMessage`, `MakeSignature`, `VerifySignature` in `com.sun.jna.platform.win32.Secur32` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#863](https://github.com/java-native-access/jna/pull/863): Fix ARM softfloat/hardfloat detection by modifying armSoftFloat condition in ELFAnalyser. Before this fix a softfloat binary could be misdetected as hardfloat. - [@kunkun26](https://github.com/kunkun26).
* [#867](https://github.com/java-native-access/jna/issues/867): Fix memory leak in `COMLateBindingObject#getStringProperty` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#871](https://github.com/java-native-access/jna/issues/871): Fix mapping of libc function `gethostname`, `sethostname`, `getdomainname` and `setdomainname` and bind `com.sun.jna.platform.win32.Winsock2.gethostname(byte[], int)` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#876](https://github.com/java-native-access/jna/pull/876): Restore java 6 compatibility - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#882](https://github.com/java-native-access/jna/pull/882): Correctly close file in `ELFAnalyser#runDetection`, fix suggested by [@Sylvyrfysh](https://github.com/Sylvyrfysh) in [#880](https://github.com/java-native-access/jna/pull/880) - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#887](https://github.com/java-native-access/jna/issues/887): MacFileUtils.moveToTrash() doesn't work in a sandboxed app fix suggested by [@sobakasu](https://github.com/sobakasu) - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#894](https://github.com/java-native-access/jna/issues/894): NullPointerException can be caused by calling `com.sun.jna.platform.win32.COM.util.ProxyObject#dispose` multiple times - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#925](https://github.com/java-native-access/jna/issues/925): Optimize `Structure#validate` and prevent `ArrayIndexOutOfBoundsException` in `SAFEARRAY#read` for zero dimensions - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#340](https://github.com/java-native-access/jna/issues/340): Guard registry handling against out-of-bounds reads by ensuring all read strings are NULL terminated - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#902](https://github.com/java-native-access/jna/issues/902): Allow building JNA on JDK 10. `javah` 
  was removed from the JDK and `javac` is now used to create the necessary headers. 
  JNA now has JDK 8 as the minimum build version, at runtime Java 6 is the minimum version.<br />
  Native code for platforms with a JDK version lower than 8 can still be build by (demonstrated for Solaris x86): 
  <ol>
      <li>Run `ant -Dbuild.os.name=SunOS -Dbuild.os.arch=x86 native-build-package`</li>
      <li>Transfer the `build/build-package-sunos-x86-5.2.1.zip` file to the target system. The file holds the native sources, the necessary headers and and a shell script for the build.</li>
      <li>Expand the zip on the target system.</li>
      <li>Setup `JAVA_HOME` to point to the JDK sources.</li>
      <li>Change into the expanded directory and run `bash build.sh`.</li>
      <li>The resulting `sunos-x86.jar` is copied back to the original build system to `lib/native/sunos-x86.jar`</li>
  </ol> - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#958](https://github.com/java-native-access/jna/issues/958): Update for PR 863: Old toolchains produce binaries without hard-/softfloat markers. Rasbian is missing the markers and the oracle JDK is also affected. For hardfloat detection now also the Arm EABI section is also considered - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#974](https://github.com/java-native-access/jna/issues/974): If the callback code failed to attach to the JVM, this lead to a segfault. The success of attaching to the JVM was checked to late and an invalid `JNIEnv` pointer was used to access the JVM - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#1010](https://github.com/java-native-access/jna/issues/1010): Fix binding of `lpAccessName` parameter of `com.sun.jna.platform.win32.Mpr#WNetUseConnection` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#384](https://github.com/java-native-access/jna/issues/384): Switch default value for `jna.nosys` to `true`. By default then the embedded native library is used - [@matthiasblaesing](https://github.com/matthiasblaesing).

Breaking Changes
----------------

* `com.sun.jna.Pointer#SIZE` is removed. Its use is replaced by `com.sun.jna.Native#POINTER_SIZE` 
  to prevent a class loading deadlock, when JNA is initialized from multiple threads
* `com.sun.jna.Pointer#getString(long offset, boolean wide)` is removed. It was replaced by
  `com.sun.jna.Pointer#getString(long offset)` or
  `com.sun.jna.Pointer#getWideString(long offset)`
* `com.sun.jna.Pointer#getStringArray(long offset, boolean wide)` is removed. It was replaced by
  `com.sun.jna.Pointer#getStringArray(long offset)` or
  `com.sun.jna.Pointer#getWideStringArray(long offset)`
* `com.sun.jna.Pointer#setString(long offset, String value, boolean wide)` is removed. It was replaced by
  `com.sun.jna.Pointer#setString(long offset, String value)` or
  `com.sun.jna.Pointer#setWideString(long offset, String value)`
* `com.sun.jna.Structure#setFieldOrder` is removed. It was replaced by 
  `com.sun.jna.Structure#getFieldOrder` and threw an `java.lang.Error` on call.
* `com.sun.jna.Native#parseVersion` was removed without replacement
* `com.sun.jna.Native#setPreserveLastError` and `com.sun.jna.Native#getPreserveLastError`
  were removed without replacement. They were turned into NOOPs in the past.
* `com.sun.jna.Native#getDirectByteBuffer` was replaced by `com.sun.jna.Pointer#getByteBuffer`
* the parameters of the methods `gethostname`, `sethostname`, `getdomainname` 
   and `setdomainname` in the interface `com.sun.jna.platform.unix.LibCAPI`
  were changed from `(char[] name, int len)` to `(byte[] name, int len)`
* `com.sun.jna.Platform#isAix` was replaced by `com.sun.jna.Platform#isAIX`
* `com.sun.jna.platform.win32.Sspi.SecBufferDesc` was incompatibly changed to 
  match the correct native semantics. SecBufferDesc describing more than one
  buffer were broken. For most usecases 
  `com.sun.jna.platform.win32.SspiUtil.ManagedSecBufferDesc` is the best 
  alternative.
* `com.sun.jna.platform.win32.WinBase.FILETIME#toLong()` was replaced by
  `com.sun.jna.platform.win32.WinBase.FILETIME#toTime()`
* `com.sun.jna.platform.win32.Variant#COM_DAYS_ADJUSTMENT` was removed
* `com.sun.jna.platform.win32.Variant#MICRO_SECONDS_PER_DAY` was removed
* `com.sun.jna.platform.win32.Variant.VARIANT#toJavaDate` was removed
* `com.sun.jna.platform.win32.Variant.VARIANT#fromJavaDate` was removed
* `com.sun.jna.platform.win32.User32#MonitorFromPoint(Point pt, int dwFlags)`
  was replaced by
  `com.sun.jna.platform.win32.User32#MonitorFromPoint(Point.ByValue pt, int dwFlags)`
* `com.sun.jna.platform.win32.OleAuto.LoadTypeLib(WString, PointerByReference)`
  was replaced by
  `com.sun.jna.platform.win32.OleAuto.LoadTypeLib(String, PointerByReference)`
* `com.sun.jna.platform.win32.Kernel32Util.formatMessageFromHR(HRESULT)`
  was replaced by
  `com.sun.jna.platform.win32.Kernel32Util.formatMessage(HRESULT)`
* `com.sun.jna.platform.win32.COM.COMException` was structurally modified. The
  `pExcepInfo` and `puArgErr` members were removed and `hresult` member was added.
  The now missing information in `COMException` was moved to `COMInvokeException`.
* The third parameter of `com.sun.jna.platform.win32.COM.IShellFolder#GetDisplayNameOf`
  was changed from `PointerByReference` to `STRRET` and matching this,
  the first parameter of `com.sun.jna.platform.win32.Shlwapi.StrRetToStr` was
  changed identically.
* `ACE_HEADER` replaces `ACEStructure` as the base class for `ACEs`.
  `com.sun.jna.platform.win32.WinNT.ACL` was modified to support ACLS, that contain
  `ACEs` other than `ACCESS_ALLOWED_ACE_TYPE` and `ACCESS_DENIED_ACE_TYPE` by
   widening the return type of `getACEStructures` to `ACE_HEADER[]` and renaming
   the method to `getACEs`. In
   consequence `com.sun.jna.platform.win32.Advapi32Util#getFileSecurity` was
   changed similarly. The SID accessors `getSidString` and `getSID` were moved
   from `ACEStructure` to `ACCESS_ACEStructure`.
* `com.sun.jna.platform.win32.Ole32#CoCreateGuid(GUID.ByReference pguid)` was replaced by
  `com.sun.jna.platform.win32.Ole32#CoCreateGuid(GUID pguid)` and 
  `com.sun.jna.platform.win32.Ole32#CLSIDFromString(WString lpsz, CLSID.ByReference pclsid)` was replaced by
  `com.sun.jna.platform.win32.Ole32#CLSIDFromString(String lpsz, CLSID.ByReference pclsid)`

Release 4.5.0
=============

Features
--------
* [#774](https://github.com/java-native-access/jna/pull/774): Addition win32 api : SendMessage, GetActiveWindow, COPYDATASTRUCT and a few constants + a demo application - [@cnico](https://github.com/cnico).
* [#783](https://github.com/java-native-access/jna/pull/783): Add Ole32 functions: `OleBuildVersion`, `OleInitialize`, `OleUninitialize`, `OleFlushClipboard`, `OleRun`, add VARIANT conversion functions to OleAuto, add default locale, LCID and LANG to WinNT - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#784](https://github.com/java-native-access/jna/issues/784): Added Solaris Kstat library - [@dbwiddis](https://github.com/dbwiddis).
* [#805](https://github.com/java-native-access/jna/issues/805): Provide a way to pass JNIEnv pointer to native method and support OPTION_ALLOW_OBJECTs for direct mapping - [@ncruces](https://github.com/ncruces).
* [#816](https://github.com/java-native-access/jna/pull/816): Support `boolean[]` in direct mapping - [@ncruces](https://github.com/ncruces).
* [#827](https://github.com/java-native-access/jna/pull/827): Add support for linux-mips64el - [@all7](https://github.com/all7).
* [#845](https://github.com/java-native-access/jna/issues/845): Add support for linux-s390x - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#771](https://github.com/java-native-access/jna/issues/771): Rebuild native libraries for linux-x86 and linux-x86-64 with an against an older GLIBC (minimum version of glibc for these architectures is now 2.7) - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#821](https://github.com/java-native-access/jna/issues/821): Move windows service related functions and structures in ntservice sample project to `com.sun.jna.platform.win32.Advapi32`: `StartServiceCtrlDispatcher`, `RegisterServiceCtrlHandler`, `RegisterServiceCtrlHandlerEx`, `SetServiceStatus`, `CreateService`, `DeleteService` and `com.sun.jna.platform.win32.Winsvc`: `Handler`, `HandlerEx`, `SERVICE_MAIN_FUNCTION`, `SERVICE_TABLE_ENTRY`, `SERVICE_DESCRIPTION`, `SERVICE_STATUS_HANDLE` - [@matthiasblaesing](https://github.com/matthiasblaesing).

Bug Fixes
---------
* [#776](https://github.com/java-native-access/jna/issues/776): Do not include ClassPath attribute in MANIFEST.MF of jna-platform. - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#785](https://github.com/java-native-access/jna/issues/785): OaIdlUtil#toPrimitiveArray fails if dimension bounds are not 0-based - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#795](https://github.com/java-native-access/jna/issues/795): com.sun.jna.platform.win32.WinDef.WORDByReference holds a WORD which is defined to 16 bit on windows, so it needs to be accessed as short (getShort()). Fix suggested by  - [@kdeines](https://github.com/kdeines).
* [#804](https://github.com/java-native-access/jna/pull/804) Main-Class in jna-platform.jar collides with java 9 module system - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#838](https://github.com/java-native-access/jna/pull/838): Fix binding of `com.sun.jna.platform.win32.User32#MonitorFromPoint` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#834](https://github.com/java-native-access/jna/issues/834): Declare structure members of LPPRINTER_DEFAULTS as public - [@matthiasblaesing](https://github.com/matthiasblaesing).

Release 4.4.0
=============

Features
--------
* [#757](https://github.com/java-native-access/jna/issues/757): Build android archive (AAR) - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#767](https://github.com/java-native-access/jna/pull/767): Add Win32 API mapping for Shlwapi PathIsUNC - [@ivanwick](https://github.com/ivanwick).
* [#753](https://github.com/java-native-access/jna/issues/753): Support arm hardfloat and softfloat by introducing armel as platform (ARM EABI Little-endian) - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#772](https://github.com/java-native-access/jna/pull/772): Improved speed of GDIUtil.getScreenshot() by ~30% - [@sommd](https://github.com/sommd).

Bug Fixes
---------
* [#754](https://github.com/java-native-access/jna/issues/754): Move MSVC build to standard stdbool.h and require Visual C++ 2015 (sizeof(bool) = 1 is now also true on MSVC build) - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#399](https://github.com/java-native-access/jna/issues/399): Check native version before attempting to call into native code - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#763](https://github.com/java-native-access/jna/issues/763): Fix vararg calls without fixed parts - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#770](https://github.com/java-native-access/jna/pull/770): Fix for free data handle in DdemlUtil. - [@stolarczykt](https://github.com/stolarczykt).

Release 4.3.0
=============

Features
--------
* [#526](https://github.com/java-native-access/jna/pull/526): Added initialization and conversion between Windows SYSTEMTIME and Java Calendar - [@lgoldstein](https://github.com/lgoldstein).
* [#532](https://github.com/java-native-access/jna/pull/529): Added `com.sun.jna.platform.win32.Mpr`, `com.sun.jna.platform.win32.LmShare`, and `com.sun.jna.platform.win32.Winnetwk` - [@amarcionek](https://github.com/amarcionek).
* [#532](https://github.com/java-native-access/jna/pull/529): Added `ACCESS_*` definitions to `com.sun.jna.platform.win32.LmAccess` - [@amarcionek](https://github.com/amarcionek).
* [#532](https://github.com/java-native-access/jna/pull/529): Added `NetShareAdd` and `NetShareDel` to `com.sun.jna.platform.win32.Netapi32` - [@amarcionek](https://github.com/amarcionek).
* [#535](https://github.com/java-native-access/jna/pull/535): Added `CreateProcessWithLogonW` to `com.sun.jna.platform.win32.Advapi32` - [@mlfreeman2](https://github.com/mlfreeman2).
* [#535](https://github.com/java-native-access/jna/pull/535): Added `CertAddEncodedCertificateToSystemStore` to `com.sun.jna.platform.win32.Crypt32` - [@mlfreeman2](https://github.com/mlfreeman2).
* [#535](https://github.com/java-native-access/jna/pull/535): Added `BitBlt` to `com.sun.jna.platform.win32.GDI32`, Added `com.sun.jna.platform.win32.GDI32Util` and added `getScreenshot()` to it - [@mlfreeman2](https://github.com/mlfreeman2).
* [#535](https://github.com/java-native-access/jna/pull/535): Added `SHEmptyRecycleBin`, `ShellExecuteEx` to `com.sun.jna.platform.win32.Shell32` - [@mlfreeman2](https://github.com/mlfreeman2).
* [#535](https://github.com/java-native-access/jna/pull/535): Added `GetDesktopWindow` to `com.sun.jna.platform.win32.User32` - [@mlfreeman2](https://github.com/mlfreeman2).
* [#540](https://github.com/java-native-access/jna/pull/539): Added Missing Windows kernel32 method: QueryFullProcessImageName - [@yossieilaty](https://github.com/yossieilaty).
* [#543](https://github.com/java-native-access/jna/pull/543): Added `ProcessIdToSessionId`, `LoadLibraryEx`, `FreeLibrary` and `Find/Load/Lock/SizeofResource` to `com.sun.jna.platform.win32.Kernel32` - [@mlfreeman2](https://github.com/mlfreeman2).
* [#545](https://github.com/java-native-access/jna/pull/545): Added `EnumResourceTypes` and `EnumResourceNames` to `com.sun.jna.platform.win32.Kernel32` - [@mlfreeman2](https://github.com/mlfreeman2).
* [#547](https://github.com/java-native-access/jna/pull/547): Added `GetSystemTimes` to `com.sun.jna.platform.win32.Kernel32` - [@dbwiddis](https://github.com/dbwiddis).
* [#548](https://github.com/java-native-access/jna/pull/548): Return 64-bit unsigned integer from `com.sun.jna.platform.win32.WinBase.FILETIME` - [@dbwiddis](https://github.com/dbwiddis).
* [#524](https://github.com/java-native-access/jna/pull/524): Added IShellFolder interface plus necessary utility functions to Windows platform, and a sample for enumerating objects in My Computer - [@lwahonen](https://github.com/lwahonen).
* [#471](https://github.com/java-native-access/jna/issues/471): Determine size of native `bool` - [@twall](https://github.com/twall).
* [#484](https://github.com/java-native-access/jna/pull/484): Added `XFetchName` to `X11` interface - [@pinaf](https://github.com/pinaf).
* [#554](https://github.com/java-native-access/jna/pull/554): Initial code for a few Unix 'libc' API(s) [@lgoldstein](https://github.com/lgoldstein)
* [#552](https://github.com/java-native-access/jna/pull/552): Added `Module32FirstW` and `Module32NextW` to `com.sun.jna.platform.win32.Kernel32` (and helper to `com.sun.jna.platform.win32.Kernel32Util`) and `MODULEENTRY32W` structure to `com.sun.jna.platform.win32.Tlhelp32` - [@mlfreeman2](https://github.com/mlfreeman2).
* [#564](https://github.com/java-native-access/jna/pull/564): Use generic definition of Native#loadLibrary [@lgoldstein](https://github.com/lgoldstein)
* [#562](https://github.com/java-native-access/jna/pull/562): Added `com.sun.jna.platform.win32.VersionUtil` with `getFileVersionInfo` utility method to get file major, minor, revision, and build version parts - [@mlfreeman2](https://github.com/mlfreeman2).
* [#563](https://github.com/java-native-access/jna/pull/563): Added `com.sun.jna.platform.win32.Wininet` with the following 4 methods: `FindFirstUrlCacheEntry`, `DeleteUrlCacheEntry`, `FindCloseUrlCache`, `FindNextUrlCacheEntry`, and the `INTERNET_CACHE_ENTRY_INFO` structure, and a helper in `com.sun.jna.platform.win32.WininetUtil` for parsing WinInet's cache - [@mlfreeman2](https://github.com/mlfreeman2).
* [#567](https://github.com/java-native-access/jna/pull/567): Added `PrintWindow`, `IsWindowEnabled`, `IsWindow`, `FindWindowEx`, `GetAncestor`, `GetCursorPos`, `SetCursorPos`, `SetWinEventHook`, `UnhookWinEvent`, `CopyIcon`, and `GetClassLong` to `com.sun.jna.platform.win32.User32` and supporting constants to `com.sun.jna.platform.win32.WinUser` - [@mlfreeman2](https://github.com/mlfreeman2).
* [#573](https://github.com/java-native-access/jna/pull/573): Added `EnumProcessModules`, `GetModuleInformation`, and `GetProcessImageFileName` to `com.sun.jna.platform.win32.Psapi` and added `ExtractIconEx` to `com.sun.jna.platform.win32.Shell32` - [@mlfreeman2](https://github.com/mlfreeman2).
* [#574](https://github.com/java-native-access/jna/pull/574): Using static final un-modifiable List of field names for structure(s) - [@lgoldstein](https://github.com/lgoldstein).
* [#577](https://github.com/java-native-access/jna/pull/577): Apply generic definitions wherever applicable - [@lgoldstein](https://github.com/lgoldstein).
* [#569](https://github.com/java-native-access/jna/pull/569): Added `com.sun.jna.platform.win32.Winspool.PRINTER_INFO_2` support. Added GetPrinter and ClosePrinter functions in `com.sun.jna.platform.win32.Winspool` - [@IvanRF](https://github.com/IvanRF).
* [#583](https://github.com/java-native-access/jna/pull/583): Added printer attributes and status - [@IvanRF](https://github.com/IvanRF).
* [#589](https://github.com/java-native-access/jna/pull/589): Use `com.sun.jna.MethodResultContext` in direct mapping (as done in interface mapping) - [@marco2357](https://github.com/marco2357).
* [#595](https://github.com/java-native-access/jna/pull/595): Allow calling COM methods/getters requiring hybrid calling (METHOD+PROPERTYGET) - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#582](https://github.com/java-native-access/jna/pull/582): Mavenize the build process - Phase 1: building the native code via Maven - [@lgoldstein](https://github.com/lgoldstein).
* [#606](https://github.com/java-native-access/jna/pull/606): Added Kernel32Util method to facilitate checking that calls to LocalFree/GlobalFree are successful - [@lgoldstein](https://github.com/lgoldstein).
* [#612](https://github.com/java-native-access/jna/pull/612): `Kernel32Util.freeLocalMemory()`/`Kernel32Util.freeGlobalMemory()` always throw `com.sun.jna.platform.win32.Win32Exception` if failed - [@lgoldstein](https://github.com/lgoldstein).
* [#608](https://github.com/java-native-access/jna/pull/608): Mavenize the build process - change parent and native pom artifactId/name to differentiate in IDE and build tools. - [@bhamail](https://github.com/bhamail)
* [#613](https://github.com/java-native-access/jna/pull/613): Make `com.sun.jna.platform.win32.Win32Exception` extend `com.sun.jna.LastErrorException` - [@lgoldstein](https://github.com/lgoldstein).
* [#614](https://github.com/java-native-access/jna/pull/614): Added standard `com.sun.jna.platform.win32.Kernel32Util.closeHandle()` method that throws a `com.sun.jna.platform.win32.Win32Exception` if failed to close the handle - [@lgoldstein](https://github.com/lgoldstein).
* [#618](https://github.com/java-native-access/jna/pull/618): Implement SAFEARRAY access and bugfix VARIANT - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#616](https://github.com/java-native-access/jna/pull/616): Allow access to base interfaces (most important IDispatch) via ProxyObject and improve binding by allowing to use dispId for the call - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#621](https://github.com/java-native-access/jna/pull/621): Added TYPEFLAGS-constants for `wTypeFlags` in `com.sun.jna.platform.win32.OaIdl.TYPEATTR` - [@SevenOf9Sleeper](https://github.com/SevenOf9Sleeper).
* [#625](https://github.com/java-native-access/jna/pull/625): Make conversion to/from java to/from VARIANT in `com.sun.jna.platform.win32.COM.util.Convert` more flexible and dependable - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#639](https://github.com/java-native-access/jna/pull/639): Add getloadavg() to OS X and Unix - [@dbwiddis](https://github.com/dbwiddis).
* [#640](https://github.com/java-native-access/jna/pull/640): Add `com.sun.jna.platform.win32.Psapi.GetPerformanceInfo()`, `com.sun.jna.platform.win32.Kernel32.GetTickCount64()`, and `com.sun.jna.platform.win32.Kernel32.SetErrorMode()` - [@dbwiddis](https://github.com/dbwiddis).
* [#642](https://github.com/java-native-access/jna/pull/642): COM calls with variable number of arguments (varargs) are now supported - [@SevenOf9Sleeper](https://github.com/SevenOf9Sleeper).
* [#644](https://github.com/java-native-access/jna/pull/644): New ant target 'install' for installing JNA artifacts in local m2-repository - [@SevenOf9Sleeper](https://github.com/SevenOf9Sleeper).
* [#649](https://github.com/java-native-access/jna/pull/649): Bugfix msoffice sample and add two samples taken from MSDN and translated from VisualBasic to Java  - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#654](https://github.com/java-native-access/jna/pull/654): Support named arguments for `com.sun.jna.platform.win32.COM.util.CallbackProxy` based callbacks - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#659](https://github.com/java-native-access/jna/issues/659): Enable LCID (locale) override for `com.sun.jna.platform.win32.COM.util.ProxyObject`-based COM calls - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#665](https://github.com/java-native-access/jna/pull/665): Added `XSetWMProtocols` and `XGetWMProtocols` to `com.sun.jna.platform.unix.X11` - [@zainab-ali](https://github.com/zainab-ali).
* [#667](https://github.com/java-native-access/jna/pull/667): Added SetFileSecurity, GetSecurityInfo and SetSecurityInfo to `com.sun.jna.platform.win32.Advapi32` - [@amarcionek](https://github.com/amarcionek).
* [#667](https://github.com/java-native-access/jna/pull/667): Added NtSetSecurityObject and NtQuerySecurityObject to `com.sun.jna.platform.win32.NtDll` - [@amarcionek](https://github.com/amarcionek).
* [#680](https://github.com/java-native-access/jna/pull/680): Added `SetCurrentProcessExplicitAppUserModelID` and `GetCurrentProcessExplicitAppUserModelID` to `com.sun.jna.platform.win32.Shell32` for setting the [System.AppUserModel.ID](https://msdn.microsoft.com/en-us/library/windows/desktop/dd391569.aspx) of the host process - [@rednoah](https://github.com/rednoah).
* [#693](https://github.com/java-native-access/jna/pull/693): Bind DDEML (Dynamic Data Exchange Management Library), add a thread implementation that runs a windows message loop - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#702](https://github.com/java-native-access/jna/pull/702): Added `GetClientRect` to `com/sun/jna/platform/win32/User32` - [@Jonatino](https://github.com/Jonatino).
* [#689](https://github.com/java-native-access/jna/pull/689): Add `GetProcAddress(HMODULE, int)` to `com.sun.jna.platform.win32.Kernel32` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#723](https://github.com/java-native-access/jna/pull/723): Added `com.sun.jna.platform.win32.Wevtapi` and `com.sun.jna.platform.win32.Winevt` - [@sakamotodesu](https://github.com/sakamotodesu).
* [#720](https://github.com/java-native-access/jna/issues/720): Added `SetThreadExecutionState` to `com.sun.jna.platform.win32.Kernel32` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#738](https://github.com/java-native-access/jna/pull/738): Added `GetSecurityDescriptorOwner`, `SetSecurityDescriptorOwner`, `GetSecurityDescriptorGroup`, `SetSecurityDescriptorGroup`, `GetSecurityDescriptorControl`, `SetSecurityDescriptorControl`, `GetSecurityDescriptorDacl`, `SetSecurityDescriptorDacl`, `MakeSelfRelativeSD`, `MakeAbsoluteSD`, `EqualSid`, `InitializeSecurityDescriptor`, `InitializeAcl`, `AddAce`, `AddAccessAllowedAce`, `AddAccessAllowedAceEx`, and `GetAce` to `com.sun.jna.platform.win32.Advapi32 - [@amarcionek](https://github.com/amarcionek).
* [#738](https://github.com/java-native-access/jna/pull/738): Added `RtlNtStatusToDosError` to `com.sun.jna.platform.win32.NtDll - [@amarcionek](https://github.com/amarcionek).
* [#732](https://github.com/java-native-access/jna/pull/732): Added `com.sun.jna.platform.win32.WinioctlUtil` for help in determining FSCTL_* codes - [@amarcionek](https://github.com/amarcionek).
* [#732](https://github.com/java-native-access/jna/pull/732): Added `com.sun.jna.platform.win32.Ntifs` with Reparse Point structures and defines - [@amarcionek](https://github.com/amarcionek).
* [#732](https://github.com/java-native-access/jna/pull/732): Added initialization of FILETIME from LARGE_INTEGER - [@amarcionek](https://github.com/amarcionek).
* [#732](https://github.com/java-native-access/jna/pull/732): Added `GetFileInformationByHandleEx` and `SetFileInformationByHandle` to `com.sun.jna.platform.win32.Kernel32` - [@amarcionek](https://github.com/amarcionek).
* [#740](https://github.com/java-native-access/jna/pull/740): Modified `com.sun.jna.platform.win32.WinioctlUtil` for simplicity dealing with FSCTL_* codes - [@amarcionek](https://github.com/amarcionek).
* [#745](https://github.com/java-native-access/jna/pull/745): Added Secur32#QueryContextAttributes - [@barney2k7](https://github.com/barney2k7).

Bug Fixes
---------
* [#549](https://github.com/java-native-access/jna/pull/549): Fixed bug in types derived from XID - [@twall](https://github.com/twall).
* [#536](https://github.com/java-native-access/jna/pull/536): Fixed bug in determining the Library and options associated with types defined outside of a Library - [@twall](https://github.com/twall).
* [#531](https://github.com/java-native-access/jna/pull/531): Ensure direct-mapped callbacks use the right calling convention - [@twall](https://github.com/twall).
* [#566](https://github.com/java-native-access/jna/pull/566): Fix return type of Native#loadLibrary to match unconstrained generic [@lgoldstein](https://github.com/lgoldstein).
* [#584](https://github.com/java-native-access/jna/pull/584): Promote float varargs to double - [@marco2357](https://github.com/marco2357).
* [#588](https://github.com/java-native-access/jna/pull/588): Fix varargs calls on arm - [@twall](https://github.com/twall).
* [#593](https://github.com/java-native-access/jna/pull/593): Improve binding of TypeLib bindings - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#578](https://github.com/java-native-access/jna/pull/578): Fix COM CallbackHandlers, allow usage of VARIANTs directly in c.s.j.p.w.COM.util.ProxyObject and fix native memory leak in c.s.j.p.w.COM.util.ProxyObject - [@matthiasblaesing](https://github.com/matthiasblaesing)
* [#601](https://github.com/java-native-access/jna/pull/601): Remove COMThread and COM initialization from objects and require callers to initialize COM themselves. Asserts are added to guard correct usage. - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#602](https://github.com/java-native-access/jna/pull/602): Make sure SID related memory is properly released once no longer required [@lgoldstein](https://github.com/lgoldstein).
* [#610](https://github.com/java-native-access/jna/pull/610): Fixed issue #604: Kernel32#GetLastError() always returns ERROR_SUCCESS [@lgoldstein](https://github.com/lgoldstein).
* [#633](https://github.com/java-native-access/jna/pull/633): Restore default usage of platform native encoding for Java strings passed to native functions (was hard-coded to UTF-8 in 4.0 and later) [@amake](https://github.com/amake)
* [#634](https://github.com/java-native-access/jna/pull/634): Improve BSTR handling and add `SysStringByteLen` and `SysStringLen` to `com.sun.jna.platform.win32.OleAuto` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#646](https://github.com/java-native-access/jna/issues/646): `platform.win32.COM.COMBindingBaseObject` swallows reason if instantiation fails - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#674](https://github.com/java-native-access/jna/pull/674): Update references to Apache License as requested by issue #673 [@bhamail](https://github.com/bhamail)
* [#636](https://github.com/java-native-access/jna/issues/636): Staticly link visual c++ runtime when building with MSVC - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#624](https://github.com/java-native-access/jna/issues/624): WinDef.DWORD getLow() & getHigh() using incorrect bit mask - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#668](https://github.com/java-native-access/jna/issues/668): Correct typemapper used for structures defined in `com.sun.jna.platform.win32.DsGetDC`, `com.sun.jna.platform.win32.LMAccess`, `com.sun.jna.platform.win32.LMShare`, `com.sun.jna.platform.win32.Sspi`, `com.sun.jna.platform.win32.WinBase`, `com.sun.jna.platform.win32.WinCrypt`, `com.sun.jna.platform.win32.WinUser` and `com.sun.jna.platform.win32.Winnetwk` - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#696](https://github.com/java-native-access/jna/issues/696): COMLateBindingObject.getAutomationProperty method that takes iDispatch parameter doesn't use it - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#664](https://github.com/java-native-access/jna/issues/664): Prevent premature GC of Pointer and Function objects by passing whole object into JNI call in addition to the raw pointer value - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#669](https://github.com/java-native-access/jna/pull/669): Ensure XSI-compliant strerror_r is used, to prevent corrupted error messages on linux - [@DavidKeller](https://github.com/DavidKeller).
* [#697](https://github.com/java-native-access/jna/issues/697): Ensure disposed memory is removed from Memory#allocatedMemory map - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#731](https://github.com/java-native-access/jna/issues/731): Require mingw-w64 instead of mingw as the alternative to the MSVC build - [@matthiasblaesing](https://github.com/matthiasblaesing).
* [#747](https://github.com/java-native-access/jna/issues/747): - Call `Native.toString()` in `#getFileName()` and `#getAlternateFileName()` in `c.s.j.p.win32.WinBase` instead of custom NUL terminator logic - [@jhult](https://github.com/jhult).

Release 4.2.1
=============

Features
--------
* [#504](https://github.com/java-native-access/jna/pull/504): Add support for linux-sparcv9 - [@alexvsimon](https://github.com/alexvsimon).
* [#510](https://github.com/java-native-access/jna/pull/510): Added `GetCommState`, `GetCommTimeouts` `SetCommState` and `SetCommTimeouts` to `com.sun.jna.platform.win32.Kernel32`. Added `DCB` structure to `com.sun.jna.platform.win32.WinBase` - [@MBollig](https://github.com/MBollig).
* [#512](https://github.com/java-native-access/jna/pull/512): Make loading debug flags mutable [@lwahonen](https://github.com/lwahonen).
* [#514](https://github.com/java-native-access/jna/pull/514): Added `host_processor_info` to `com.sun.jna.platform.mac.SystemB`  - [@dbwiddis](https://github.com/dbwiddis).
* [#519](https://github.com/java-native-access/jna/pull/519): Added JNA functional overview - [@twall](https://github.com/twall).
* [#528](https://github.com/java-native-access/jna/pull/528): Added idea-jar ant task that creates a convenience jar that contains all native dispatch libraries - [@lwahonen](https://github.com/lwahonen).

Bug Fixes
---------
* [#515](https://github.com/java-native-access/jna/issues/515): Update linux-arm natives omitted in 4.2 [@twall](https://github.com/twall).

Release 4.2
===========

Features
--------
* [#452](https://github.com/java-native-access/jna/pull/452): Added Mac OS X System.B library at `com.sun.jna.platform.mac.SystemB`  including support for `sysctl`, `sysctlbyname`, `sysctlnametomib`, `mach_host_self`, `host_page_size`, `host_statistics` and `host_statistics64` - [@dbwiddis](https://github.com/dbwiddis).
* [#446](https://github.com/java-native-access/jna/pull/446): Added `com.sun.jna.platform.win32.Advapi32.GetNamedSecurityInfo`, `SetNamedSecurityInfo`, `GetSecurityDescriptorLength`, `IsValidSecurityDescriptor`, `IsValidAcl` - [@amarcionek](https://github.com/amarcionek).
* [#387](https://github.com/java-native-access/jna/pull/397): Use of interfaces and annotations to provide easier implementation of COM interfaces (with `InvocationHandler`) - [@dhakehurst](https://github.com/dhakehurst).
* [#387](https://github.com/java-native-access/jna/pull/397): Support for COM event callbacks - [@dhakehurst](https://github.com/dhakehurst).
* [#387](https://github.com/java-native-access/jna/pull/397): Support for COM interface discovery by iteration over `RunningObjectTable` - [@dhakehurst](https://github.com/dhakehurst).
* Updated AIX natives and build - [@twall](https://github.com/twall).
* [#290](https://github.com/java-native-access/jna/pull/290): Improved the stacktrace for the exceptions thrown by `com.sun.jna.Structure` - [@ebourg](https://github.com/ebourg).
* [#332](https://github.com/java-native-access/jna/pull/332): Added Win32 Monitor Configuration API in `com.sun.jna.platform.win32.Dxva2` - [@msteiger](https://github.com/msteiger).
* Added Winspool monitor sample and updated Kernel32, WinBase, Winspool - [@wolftobias](https://github.com/wolftobias).
* [#333](https://github.com/java-native-access/jna/pull/333): Added `CoTaskMemAlloc`, `CoTaskMemRealloc` and `CoTaskMemFree` to `com.sun.jna.platform.win32.Ole32` - [@msteiger](https://github.com/msteiger).
* [#334](https://github.com/java-native-access/jna/pull/334): Added `com.sun.jna.platform.win32.Shell32.SHGetKnownFolderPath` and `KnownFolders` GUID constants - [@msteiger](https://github.com/msteiger).
* [#338](https://github.com/java-native-access/jna/pull/338): Added `com.sun.jna.platform.mac.XAttr` and `com.sun.jna.platform.mac.XAttrUtil` JNA wrapper for `<sys/xattr.h>` for Mac OS X - [@rednoah](https://github.com/rednoah).
* [#339](https://github.com/java-native-access/jna/pull/339): Added `GetWindowPlacement`, `SetWindowPlacement`, `AdjustWindowRect`, `AdjustWindowRectEx`, `ExitWindowsEx`, and `LockWorkstation` to `com.sun.jna.platform.win32.User32` - [@Timeroot](https://github.com/Timeroot).
* [#286](https://github.com/java-native-access/jna/pull/286): Added `CreateRemoteThread`, `WritePocessMemory` and `ReadProcessMemory` to `com.sun.jna.platform.win32.Kernel32` - [@sstokic-tgm](https://github.com/sstokic-tgm).
* [#350](https://github.com/java-native-access/jna/pull/350): Added `jnacontrib.x11.api.X.Window.getSubwindows` - [@rm5248](https://github.com/rm5248).
* Improved `contrib/msoffice` sample - [@wolftobias](https://github.com/wolftobias).
* [#352](https://github.com/java-native-access/jna/pull/352): Performance improvements due to reduced locking in `com.sun.jna.Library$Handler` and fewer vararg checks in `com.sun.jna.Function` - [@Boereck](https://github.com/Boereck).
* [#353](https://github.com/java-native-access/jna/pull/353): Performance improvements by improved collaboration between `com.sun.jna.Library$Handler` and `com.sun.jna.Function` - [@Boereck](https://github.com/Boereck).
* [#357](https://github.com/java-native-access/jna/pull/357): Added `com.sun.jna.platform.win32.Kernel32.SetSystemTime` - [@lgoldstein](https://github.com/lgoldstein), [@thomasjoulin](https://github.com/thomasjoulin).
* [#365](https://github.com/java-native-access/jna/pull/365): Added `com.sun.jna.platform.win32.Kernel32.GetComputerNameEx` support - [@lgoldstein](https://github.com/lgoldstein).
* [#368](https://github.com/java-native-access/jna/pull/368): Added `com.sun.jna.platform.win32.Kernel32.VirtualQueryEx`, `com.sun.jna.platform.win32.WinNT.MEMORY_BASIC_INFORMATION` and `MEM_COMMIT`, `MEM_FREE`, `MEM_RESERVE`, `MEM_IMAGE`, `MEM_MAPPED`, `MEM_PRIVATE` constants - [@apsk](https://github.com/apsk).
* Allow interoperation with JNI revision changes - [@twall](https://github.com/twall).
* [#376](https://github.com/java-native-access/jna/pull/373): Added `com.sun.jna.VarArgsChecker` for faster vararg checks. Used in `com.sun.jna.Function` - [@Boereck](https://github.com/Boereck).
* [#391](https://github.com/java-native-access/jna/pull/391): Added `com.sun.jna.platform.win32.Advapi3.EncryptFile`, `DecryptFile`, `FileEncryptionStatus`, `EncryptionDisable`, `OpenEncryptedFileRaw`, `ReadEncryptedFileRaw`, `WriteEncryptedFileRaw`, and `CloseEncryptedFileRaw` with related `Advapi32Util` helpers - [@khalidq](https://github.com/khalidq).
* [#400](https://github.com/java-native-access/jna/pull/400): Added `com.sun.jna.platform.WindowUtils.getAllWindows`, `getWindowIcon`, `getIconSize`, `getWindowTitle`, `getPRocessFilePath` and `getWindowLocationAndSize` - [@PAX523](https://github.com/PAX523).
* [#400](https://github.com/java-native-access/jna/pull/400): Added `com.sun.jna.platform.win32.Kernel32Util.getLastErrorMessage` - [@PAX523](https://github.com/PAX523).
* [#400](https://github.com/java-native-access/jna/pull/400): Added `com.sun.jna.platform.win32.GDI32.GetObject` - [@PAX523](https://github.com/PAX523).
* [#400](https://github.com/java-native-access/jna/pull/400): Added `com.sun.jna.platform.win32.Psapi.GetModuleFileNameEx`- [@PAX523](https://github.com/PAX523).
* [#400](https://github.com/java-native-access/jna/pull/400): Added `com.sun.jna.platform.win32.User32.GetIconInfo`, `SendMessageTimeout` and `GetClassLongPtr` - [@PAX523](https://github.com/PAX523).
* [#400](https://github.com/java-native-access/jna/pull/400): Added `com.sun.jna.platform.win32.WinGDI.ICONINFO` and `BITMAP` - [@PAX523](https://github.com/PAX523).
* [#400](https://github.com/java-native-access/jna/pull/400): Added process-specific access rights constants in `com.sun.jna.platform.win32.WinNT` - [@PAX523](https://github.com/PAX523).
* [#400](https://github.com/java-native-access/jna/pull/400): Added specific constants for request of icon settings in `com.sun.jna.platform.win32.WinUser` - [@PAX523](https://github.com/PAX523).
* [#400](https://github.com/java-native-access/jna/pull/400): Added constants for `GetClassLong`, `SendMessageTimeout` and `GetIconInfo` in `com.sun.jna.platform.win32.WinUser` - [@PAX523](https://github.com/PAX523).
* [#419](https://github.com/java-native-access/jna/pull/419): Added `SetupDiOpenDevRegKey` , `SetupDiEnumDeviceInfo` and related constants to `com.sun.jna.platform.win32.SetupApi` - [@ChristianSchwarz](https://github.com/ChristianSchwarz).
* [#421](https://github.com/java-native-access/jna/pull/421): Added `XGrabKeyboard` and `XUngrabKeyboard` to `X11` interface - [@pinaf](https://github.com/pinaf).
* [#422](https://github.com/java-native-access/jna/pull/422): Update libffi to v3.2.1 - [@tdaitx](https://github.com/tdaitx).
* [#425](https://github.com/java-native-access/jna/pull/425): Add support for PPCLE - [@tdaitx](https://github.com/tdaix).
* [#428](https://github.com/java-native-access/jna/pull/428): Added Wincon.h related functions and definitions to `com.sun.jna.platform.win32.Kernel32` - [@lgoldstein](https://github.com/lgoldstein).
* [#430](https://github.com/java-native-access/jna/issues/430): Add android `aarch64`, `x86-64`, `mips`, and `mips64` targets - [@twall](https://github.com/twall).
* [#431](https://github.com/java-native-access/jna/pull/431): Added named pipe API support to `com.sun.jna.platform.win32.Kernel32` - [@lgoldstein](https://github.com/lgoldstein).
* [#432](https://github.com/java-native-access/jna/pull/432): Added `com.sun.jna.platform.win32.Kernel32.SetLocalTime` - [@lgoldstein](https://github.com/lgoldstein).
* [#434](https://github.com/java-native-access/jna/pull/434): Added `com.sun.jna.platform.win32.Kernel32.GetEnvironmentStrings`  - [@lgoldstein](https://github.com/lgoldstein).
* Loosen OSGI OS name matching to accommodate Windows 8 family - Niels Bertram.
* [#436](https://github.com/java-native-access/jna/pull/469): Added basic Pdh API implementation to 'com.sun.jna.platform.win32' - [@lgoldstein](https://github.com/lgoldstein).
* [#451](https://github.com/java-native-access/jna/pull/451): Add VARIANT support for CHAR and BYTE  - [@mitkola](https://github.com/mitkola).
* [#478](https://github.com/java-native-access/jna/issues/451): Ask ldconfig for more places to search for libraries  - [@gohal](https://github.com/gohal).
* [#481](https://github.com/java-native-access/jna/pull/481): Added volume management functions to `com.sun.jna.platform.win32` - [@lgoldstein](https://github.com/lgoldstein).
* [#483](https://github.com/java-native-access/jna/pull/483): Found and fixed duplicate method definitions for the same API in `com.sun.jna.platform.win32` - [@lgoldstein](https://github.com/lgoldstein).
* [#485](https://github.com/java-native-access/jna/pull/485): Implemented `Comparable` interface for many of the base types in `com.sun.jna.platform.win32.WinDef` - [@lgoldstein](https://github.com/lgoldstein).
* [#488](https://github.com/java-native-access/jna/pull/488): Added `GetRawInputDeviceList` to `com.sun.jna.platform.win32.User32` and `User32Util` - [@lgoldstein](https://github.com/lgoldstein).
* [#490](https://github.com/java-native-access/jna/issues/490): Allow arbitrary calling convention specification, including FFI_MS_CDECL which alters handling of struct return values, and multiple Linux/PowerPC conventions - [@twall](https://github.com/twall).

Bug Fixes
---------
* [#450](https://github.com/java-native-access/jna/pull/450): Libraries extracted to temp directory are now cleaned up in case of library loading errors, as well - [@aschnab](https://github.com/aschnab).
* [#319](https://github.com/java-native-access/jna/pull/319): Fix direct-mapping type-mapped pointer result types - [@marco2357](https://github.com/marco2357).
* [#350](https://github.com/java-native-access/jna/pull/350): Fix `jnacontrib.x11.api.X.Window.getXXXProperty`, returns `null` if the window property is not found - [@rm5248](https://github.com/rm5248).
* Fixed `com.sun.jna.platform.win32.Variant` and `TlbImp` - [@wolftobias](https://github.com/wolftobias).
* Fixed `com.sun.jna.Pointer.getWideStringArray` not respecting the length parameter - [@csoren](https://github.com/csoren).
* Fix missing code in X11 demos - [@twall](https://github.com/twall).
* Fix compiler warnings OSX - [@twall](https://github.com/twall).
* Remove unsupported JAWT from OSX build - [@twall](https://github.com/twall).
* Disable WebStart tests - [@twall](https://github.com/twall).
* Dispose all native resources when JNA's native library is unloaded - Paul Grütter,  [@twall](https://github.com/twall).
* Weakly hold registered direct-mapped classes - [@twall](https://github.com/twall).
* [#382](https://github.com/java-native-access/jna/pull/382): Fixed memory allocation in `com.sun.jna.platform.win32.WTypes.LPWSTR` and `LPSTR` constructors - [@junak-michal](https://github.com/junak-michal).
* Fix publish doc links - [@bhamail](https://github.com/bhamail).
* [#388](https://github.com/java-native-access/jna/issues/388): Ensure native library always opened with provided flags - [@zolyfarkas](https://github.com/zolyfarkas).
* [#403](https://github.com/java-native-access/jna/pull/403): Fix `com.sun.jna.platform.win32.COM.COMUtils.SUCCEEDED` and `FAILED` - [@lwahonen](https://github.com/lwahonen).
* [#404](https://github.com/java-native-access/jna/pull/404): Fix `VARIANT` constructors for `int`, `short`, and `long` - [@lwahonen](https://github.com/lwahonen).
* [#420](https://github.com/java-native-access/jna/pull/420): Fix structure leaving always one element in ThreadLocal set - [@sjappig](https://github.com/sjappig).
* [#467](https://github.com/java-native-access/jna/issues/467): Fix TypeMapper usage with direct-mapped libraries converting primitives to Java objects (specifically enums) - [@twall](https://github.com/twall).
* [#475](https://github.com/java-native-access/jna/issues/475): Avoid modifying native memory in `Structure.equals()/hashCode()`- [@twall](https://github.com/twall).
* [#496](https://github.com/java-native-access/jna/issues/496): Properly handle direct mapping with type mappers which return String/WString - [@twall](https://github.com/twall).

Release 4.1
===========

Features
--------
* Added `com.sun.jna.platform.win32.Advapi32Util.registryCloseKey` - [@falldog](https://github.com/falldog).
* Enabled platform tests to be run w/o building native bits - [@twall](https://github.com/twall).
* Added COM/Typelib java code generator `com.sun.jna.platform.win32.COM.tlb.TlbImp` - [@wolftobias](https://github.com/wolftobias).
* [#226](https://github.com/java-native-access/jna/issues/226): Added OSGI information to jna-platform.jar - [@brettwooldridge](https://github.com/brettwooldridge).
* [#267](https://github.com/java-native-access/jna/pull/267): Added support for Windows RAS32 API, `com.sun.jna.platform.win32.Rasapi32` and `Rasapi32Util` - [@kc7bfi](https://github.com/kc7bfi).
* [#101](https://github.com/java-native-access/jna/issues/101): Modify `com.sun.jna.platform.win32.Advapi32Util.registryGet*` API to support `KEY_WOW64` option - [@falldog](https://github.com/falldog).
* [#271](https://github.com/java-native-access/jna/pull/271): Added `com.sun.jna.platform.win32.Gdi32.ChoosePixelFormat` and `SetPixelFormat` - [@kc7bfi](https://github.com/kc7bfi).
* [#271](https://github.com/java-native-access/jna/pull/271): Added `com.sun.jna.platform.win32.OpenGL32`, `OpenGL32Util` and `WinOpenGL` - [@kc7bfi](https://github.com/kc7bfi).
* [#250](https://github.com/java-native-access/jna/pull/250): Added `com.sun.jna.platform.win32.Kernel32.GetPrivateProfileSection`, `GetPrivateProfileSectionNames` and `WritePrivateProfileSection` and corresponding `Kernel32Util` helpers - [@quipsy-karg](https://github.com/quipsy-karg).
* [#287](https://github.com/java-native-access/jna/pull/287): Added `DBTF_MEDIA` and `DBTF_NET` to `com.sun.jna.platform.win32.DBT` - [@daifei4321](https://github.com/daifei4321).
* [#295](https://github.com/java-native-access/jna/pull/295): Added `com.sun.jna.platform.win32.Kernel32.ResetEvent` - [@manithree](https://github.com/manithree).
* [#301](https://github.com/java-native-access/jna/pull/301): Added `accessCheck` to `com.sun.jna.platform.win32.Advapi32Util`, `MapGenericMask` and `AccessCheck` to `com.sun.jna.platform.win32.Advapi32`, `PRIVILEGE_SET` and `GENERIC_MAPPING` to `com.sun.jna.platform.win32.WinNT` - [@BusyByte](https://github.com/BusyByte).

Bug Fixes
---------
* Fixed inconsistent behavior on `Structure.ByValue` fields within a `Structure` - [@twall](https://github.com/twall).
* [#279](https://github.com/java-native-access/jna/issues/279): Accommodate FreeBSD libc loading - [@sevan](https://github.com/sevan).
* [#287](https://github.com/java-native-access/jna/pull/287): Fixed contrib `win32.Win32WindowDemo`, now showing the added/removed drive letter, and whether the event is about media in drive or physical drive - [@daifei4321](https://github.com/daifei4321).
* [#300](https://github.com/java-native-access/jna/issues/300): Fix stdcall argument alignment - [@twall](https://github.com/twall).

Release 4.0
===========

Features
--------
* Added ASL licensing to facilitate distribution - [@twall](https://github.com/twall).
* [#109](https://github.com/java-native-access/jna/issues/109): Set default Java compatibility level to 1.6 - [@twall](https://github.com/twall).
* [#209](https://github.com/java-native-access/jna/issues/209): Improved default performance saving last error results - [@twall](https://github.com/twall).
* Use predictable names for CPU architecture prefix (namely x86, x86-64); names correspond to OSGI processor values - [@twall](https://github.com/twall).
* Avoid superfluous Structure memory allocation from native - [@twall](https://github.com/twall).
* Added `Library.OPTION_CLASSLOADER`, which enables loading native libraries from any class loader (including JNA's native library). This enables parallel dependencies on JNA (e.g. within a tomcat deployment without having to include JNA in the app server environment) - [@twall](https://github.com/twall).
* Use per-library String encoding settings (see `Native.getDefaultStringEncoding()` and `Structure.getStringEncoding()`) - [@twall](https://github.com/twall).
* Added memory dump for debugging (see `com.sun.jna.Memory`) - [@twall](https://github.com/twall).
* Improved caching of Structure alignment, type mapping, and encoding information - [@twall](https://github.com/twall).
* [#225](https://github.com/java-native-access/jna/pull/225): Added `platform.win32.Kernel32.GetLogicalProcessorInformation` and `platform.win32.Kernel32Util.getLogicalProcessorInformation` - [@trejkaz](https://github.com/trejkaz).
* [#236](https://github.com/java-native-access/jna/issues/236): Auto-strip profiler native method prefix specified by `jna.profiler.prefix`, which defaults to $$YJP$$ - [@twall](https://github.com/twall).
* Added `jna.debug_load` property to diagnose library loading issues - [@twall](https://github.com/twall).
* Throw explicit `IllegalArgumentException` when `Structure.ByReference` is used where it shouldn't be (can result in multiply freed memory or other unexpected behavior) - [@twall](https://github.com/twall).
* [#243](https://github.com/java-native-access/jna/issues/243): Automatically accommodate long library paths on Windows which would otherwise fail - [@twall](https://github.com/twall).
* [#241](https://github.com/java-native-access/jna/issues/241) - Added  `com.sun.jna.platform.win32.Shell32.SHAppBarMessage` - [@bsorrentino](https://github.com/bsorrentino).
* Make `Structure.read/writeField()` protected to facilitate per-field overrides - [@twall](https://github.com/twall).
* Speed up callback lookup where large numbers of native function pointers are in use - [@twall](https://github.com/twall).

Bug Fixes
---------
* [#213](https://github.com/java-native-access/jna/pull/213): Fixed `Structure.toString()` not to dump memory when `jna.dump_memory` is false - [@tomohiron](https://github.com/tomohiron).
* Use dedicated TLS to indicate callback detach state, to avoid any potential conflicts with last error storage - [@twall](https://github.com/twall).
* [#173](https://github.com/java-native-access/jna/issues/173): Fixed OSX 10.8/Xcode 4+ builds, web start path with Oracle 1.7 JDK - [@mkjellman](https://github.com/mkjellman).
* [#215](https://github.com/java-native-access/jna/issues/215): Forced use of XSI `strerror_r` on linux - [LionelCons](https://github.com/LionelCons).
* [#214](https://github.com/java-native-access/jna/issues/214): Don't map library names when an absolute path is provided - [@twall](https://github.com/twall).
* [#218](https://github.com/java-native-access/jna/issues/218): Explicitly handled broken Android `SecurityManager` implementation - [@twall](https://github.com/twall).
* [#223](https://github.com/java-native-access/jna/issues/223): Fixed layout/size derivation for unions - [@twall](https://github.com/twall).
* [#229](https://github.com/java-native-access/jna/issues/229): Added `CreateProcessW` (Unicode version) - [@twall](https://github.com/twall).
* Avoid solaris/x86 JVM bug w/library open flags - [@twall](https://github.com/twall).
* Fixed NPE returning wide string from a direct-mapped function - [@twall](https://github.com/twall).
* [#237](https://github.com/java-native-access/jna/issues/237): Fix LastErrorException/getLastError on AIX - [@skissane](https://github.com/skissane).
* [#228](https://github.com/java-native-access/jna/issues/228): Fix win32/win64 crashes due to LastErrorException buffer overruns (`snprintf` on windows is broken) - [@davidhoyt](https://github.com/davidhoyt).

Release 3.5.2
=============

Features
--------
* Basic [COM support](https://github.com/java-native-access/jna/blob/master/www/PlatformLibrary.md) for w32 - [@wolftobias](https://github.com/wolftobias).
* Avoid superfluous Structure memory allocation by using Structure(Pointer) ctors if available - [@twall](https://github.com/twall).
* [PR#120](https://github.com/java-native-access/jna/pull/120): Provide methods for extracting native libraries from the class path for use by JNA - [@Zlika](https://github.com/Zlika).
* [PR#163](https://github.com/java-native-access/jna/pull/163): The Java `GUID` structure can be used directly as alternative to `Ole32Util.getGUIDFromString()` - [@wolftobias](https://github.com/wolftobias).
* [PR#163](https://github.com/java-native-access/jna/pull/163): Ported Win32 `dbt.h` - [@wolftobias](https://github.com/wolftobias).
* [PR#163](https://github.com/java-native-access/jna/pull/163): Added Win32 `WTSRegisterSessionNotification()` and `WTSUnRegisterSessionNotification()` from `Wtsapi32.dll` - [@wolftobias](https://github.com/wolftobias).
* [PR#163](https://github.com/java-native-access/jna/pull/163): Added Win32 `native_window_msg` that creates windows, registers for USB device and logon/logoff notifications - [@wolftobias](https://github.com/wolftobias).
* [PR#178](https://github.com/java-native-access/jna/pull/178): Added Win32 `USER_INFO_10` structure from `LMAccess.h` - [@davidmc24](https://github.com/davidmc24).
* [PR#192](https://github.com/java-native-access/jna/pull/192): Added Win32 `SHGetSpecialFolderPath()` and initialization file (.ini) API functions from `kernel32.dll` - [@headcrashing](https://github.com/headcrashing).
* [PR#194](https://github.com/java-native-access/jna/pull/194): Added Unit Test for `CLSIDFromProgID()` - [@headcrashing](https://github.com/headcrashing).
* [PR#196](https://github.com/java-native-access/jna/pull/196): Added Win32 `RegisterWindowMessage()` and new wrapper `User32Util` for convenient use of `RegisterWindowMessage`, `CreateWindow` and `CreateWindowEx` - [@headcrashing](https://github.com/headcrashing).
* [PR#187](https://github.com/java-native-access/jna/pull/187): Allow `StructureFieldOrderTest` unit test in platform project to run on Linux. - [@bhamail](https://github.com/bhamail).

Bug Fixes
---------
* [PR#180](https://github.com/java-native-access/jna/pull/180): Fix: added missing fields in `XEvents.getFieldOrder()` - [@xwizard](https://github.com/xwizard).
* [PR#183](https://github.com/java-native-access/jna/pull/183): Fix `LMAccess.GROUP_INFO_3.getFieldOrder()` to return correct fields names - [@bhamail](https://github.com/bhamail).
* [PR#187](https://github.com/java-native-access/jna/pull/187): Fix `getFieldOrder()` to return correct field names for some X11 structures - [@bhamail](https://github.com/bhamail).
* Remove deprecated methods on Memory (getSize,isValid) and Structure (getSize) - [@twall](https://github.com/twall).
* Remove problematic AWT check via `Class.forName("java.awt.Component")` (see [here](https://bugs.eclipse.org/bugs/show_bug.cgi?id=388170)) - [@twall](https://github.com/twall).
* [PR#210](https://github.com/java-native-access/jna/pull/210) Add OSGI processor specs for Mac OS X - [@bertfrees](https://github.com/bertfrees).
* [PR#174](https://github.com/java-native-access/jna/pull/174): Recompile linux-amd64 natives to remove glibc-2.11 dependencies, now requires only 2.2.5 or better - [@twall](https://github.com/twall).
* [PR#183](https://github.com/java-native-access/jna/pull/183): Added `StructureFieldOrderInspector` unit test utility to scan for `Structure` field issues; see: `com.sun.jna.platform.StructureFieldOrderTest.testMethodGetFieldOrder` - [@bhamail](https://github.com/bhamail).
* [PR#187](https://github.com/java-native-access/jna/pull/187): Allow `StructureFieldOrderTest` unit test in platform project to run on Linux - [@bhamail](https://github.com/bhamail).
* [#206](https://github.com/java-native-access/jna/issues/206): Fix `moveToTrash()` on OSX to work with symlinks - [@twall](https://github.com/twall).
* Fix NPE if `Thread.getContextClassLoader()` returns `null`  - [@twall](https://github.com/twall).

Release 3.5.1
=============

Bug Fixes
---------
* Fix bug where string fields sometimes failed to be writtern - [@twall](https://github.com/twall) (roman kisluhin).
* [PR#145](https://github.com/java-native-access/jna/pull/145): Fix `Netapi32Util.getDomainTrusts()` returns "empty" domain object - [@aikidojohn](https://github.com/aikidojohn).
* [PR#145](https://github.com/java-native-access/jna/pull/145): Fix `Netapi32.getDC()` - added missing fields in `DOMAIN_CONTROLLER_INFO` - [@aikidojohn](https://github.com/aikidojohn).
* [PR#151](https://github.com/java-native-access/jna/pull/151): 'platform.jar' in the dist directory was not updated for release 3.5.0. (The 'platform.jar' published to maven central was correct.)

Release 3.5.0
=============

Features
--------
* [#62](https://github.com/java-native-access/jna/issues/62) If a callback is required to reside in a DLL, use [`DLLCallback`](https://java-native-access.github.io/jna/3.5.1/javadoc/com/sun/jna/win32/DLLCallback.html) to tag your Callback object - [@twall](https://github.com/twall).
* `Structure.getFieldOrder()` supersedes `Structure.setFieldOrder()` and is now required - [@twall](https://github.com/twall).
* Search `~/Library/Frameworks` and `/Library/Frameworks` on OSX - [@shaneholloway](https://github.com/shaneholloway).
* Automatic cleanup of native threads (based on suggestions from neil smith) - [@twall](https://github.com/twall).
* Add `android-arm` target - [@ochafik](https://github.com/ochafik), [@twall](https://github.com/twall).
* Add `jna.tmpdir` to override temporary JNA storage location - [@twall](https://github.com/twall).
* Add `EXTRA_MAKE_OPTS` ant property to override make variables - [@twall](https://github.com/twall).
* Add `Library.OPTION_OPEN_FLAGS` to customize dlopen behavior - [@twall](https://github.com/twall).
* [#113](https://github.com/java-native-access/jna/issues/113), [#114](https://github.com/java-native-access/jna/issues/114): Add support for GNU/kFreeBSD and debian multi-arch distros - [@twall](https://github.com/twall).

Bug Fixes
---------
* Fix `Advapi32Util.registryGetValues()` tried to allocate memory for a zero-length `REG_BINARY` value - [@phailwhale22](https://github.com/phailwhale22).
* Fix crash in direct mode callbacks with certain type conversions - [@twall](https://github.com/twall).
* More thoroughly propagate unexpected exceptions generated in jnidispatch - [@twall](https://github.com/twall).
* Cleanup maven poms and publishing to central repo - [@bhamail](https://github.com/bhamail).
* [#129](https://github.com/java-native-access/jna/issues/129): Allow `Memory` field in structure - [@twall](https://github.com/twall).
* Preserve `PointerType` fields on `Structure.read()` if unchanged - [@twall](https://github.com/twall).
* [#128](https://github.com/java-native-access/jna/issues/128): Fix masking extracting DWORD upper and lower WORD values - [@twall](https://github.com/twall).
* [#135](https://github.com/java-native-access/jna/issues/135): Fix for `Advapi32Util.registryGetValues()` when reading zero length values - [@danwi](https://github.com/danwi).

Release 3.4.2
=============

Features
--------
* Add `platform.win32.Kernel32.GetEnvironmentVariable` and `platform.win32.Kernel32Util.getEnvironmentVariable` - [@dblock](https://github.com/dblock).
* Moved `Kernel32.dll` function definitions from `WinNT.java` into `Kernel32.java` - [@dblock](https://github.com/dblock).
* Provide `toPointer()` methods on all `_PTR` types (platform win32) - [@twall](https://github.com/twall).
* Provide `ant -Dskip-native` to skip platform native build - [@twall](https://github.com/twall).
* Provide `ant -Dheadless=true` to run unit tests headless - [@twall](https://github.com/twall).
* Added Windows dev environment instructions - [@twall](https://github.com/twall).

Bug Fixes
---------
* Ensure platform win32 classes use unsigned where appropriate (`ULONG_PTR`, `UINT_PTR`, `ULONGLONG`, `WORD`, `DWORDLONG`) - [@twall](https://github.com/twall).
* [#71](https://github.com/java-native-access/jna/issues/71), [#73](https://github.com/java-native-access/jna/issues/73): Fix OSGI entries in manifest - [@twall](https://github.com/twall).
* [#78](https://github.com/java-native-access/jna/issues/78): Fix NPE in `platform.win32.Netapi32Util.getDomainTrusts` - [@dblock](https://github.com/dblock).
* Fix: auto-sync memory for `struct**` arguments (array of struct pointers) - [@twall](https://github.com/twall).
* Fix: `platform.win32.Secur32.AcquireCredentialsHandle`, `InitializeSecurityContext` and `AcceptSecurityContext` on Win32 64-bit - [@dblock](https://github.com/dblock).
* Fix: avoid overwriting native `char *` or `wchar_t *` fields within structures when unmodified (similar to current operation with pointers) - [@twall](https://github.com/twall).
* Fix: `platform.win32.DsGetDC.DS_DOMAIN_TRUSTS` and `DsEnumerateDomainTrusts` on Win32 64-bit - [@trejkaz](https://github.com/trejkaz).
* Fix: Crash freeing the wrong pointer in `Netapi32Util.getDomainTrusts` - [@trejkaz](https://github.com/trejkaz).
* [#100](https://github.com/java-native-access/jna/issues/100): Fix `platform.win32.W32FileMonitor` - [@dblock](https://github.com/dblock).
* Return INT_PTR from `platform.win32.Shell32.ShellExecute`, since returning
`HINSTANCE` is useless.
* Fix runtime error in some instances where Structure.setFieldOrder is used (never return self when sharing AutoAllocated memory).
* [#107](https://github.com/java-native-access/jna/issues/107): `Structure.clear()` always calls `ensureAllocated()` to avoid NPE.
* Ensure internal memory pointer is *always* allocated when calling `Structure.useMemory()`, even if layout is not yet determined.

Release 3.4.1
=============

Features
--------
* Add 'unsigned' modifier to IntegerType.
* Add to `platform.win32.User32`: `GetLastInputInfo`.
* Add `platform.win32.WinNT.GetFileType` and `platform.win32.Kernel32Util.getFileType`.
* Add to `platform.win32.Kernel32Util`: `getFileType`.

Bug Fixes
---------
* Re-build linux-amd and linux-i386 against older versions of glibc (2.2.5 and
2.1.3 respectively).
* Properly initialize first printer info struct in winspool library.
* Properly support getting and setting zero-array-length `REG_MULTI_SZ` values on Win32.
* Fixed SID in Win32 `USER_INFO_23` and `GROUP_INFO_3`.
* Fixed passing domain name into Win32 `Netapi32Util.getUserInfo`.

Release 3.4.0
=============

Features
--------
* Provide `jna.nosys=true` to avoid loading any system-provided JNA (useful for local build/development).
* Allow override of default jnidispatch library name with `jna.boot.library.name` system property.
* Throw an Error if a system install of JNA is incompatible or if JNA's JNI library does not match.
* Disable automatic jnidispatch unpacking with `jna.nounpack=true`.
* Automatically look up system error messages for LastErrorException.
* Improved callback thread-mapping support; re-use, rename, and group callback
threads.
* Cache structure layout results, improving performance of structure creation.
* linux/arm 32-bit support (hardware provided by Alex Lam).
* linux/ppc 32-bit support (hardware provided by Fritiof Hedman).
* Preliminary linux/ia64, linux/ppc64 support (thanks to Laurent Guerby and the GCC compile farm).
* Windows CE/Mobile support (w32ce-arm) (resources provided by andrea antonello and Hydrologis SRL).
* linux multi-arch support (kohsuke).
* Added REG_QWORD registry type support
* Add to `platform.unix.x11`: `XGrabKey`, `XUngrabKey`, `XSetErrorHandler`.
* Add to `platform.mac.Carbon`: `GetEventDispatcherTarget`, `InstallEventHandler`, `RegisterEventHotKey`, `GetEventParameter`, `RemoveEventHandler`, `UnregisterEventHotKey`.
* Add to `platform.win32.Kernel32`: `CopyFile`, `MoveFile`, `MoveFileEx`, `CreateProcess`, `SetEnvironmentVariables`, `GetFileTime`, `SetFileTime`, `SetFileAttributes`, `DeviceIoControl`, `GetDiskFreeSpaceEx`, `CreateToolhelp32Snapshot`, `Process32First`, `Process32Next`.
* Add to `platform.win32.Msi`: `MsiGetComponentPath`, `MsiLocateComponent`, `MsiGetProductCode`, `MsiEnumComponents`.
* Add to `platform.win32.User32`: `RegisterHotKey`, `UnregisterHotKey`
* Add to `platform.win32.SetupApi`: `SetupDiGetClassDevs`, `SetupDiDestroyDeviceInfoList`, `SetupDiEnumDeviceInterfaces`, `SetupDiGetDeviceInterfaceDetail`, `SetupDiGetDeviceRegistryProperty`.
* Add `platform.win32.Shell32.ShellExecute`.
* Add to `platform.win32.User32`: `SetParent`, `IsWindowVisible`, `MoveWindow`, `SetWindowPos`, `AttachInputThread`, `SetForegroundWindow`, `GetForegroundWindow`, `SetFocus`, `SendInput`, `WaitForInputIdle`, `InvalidateRect`, `RedrawWindow`, `GetWindow`, `UpdateWindow`, `ShowWindow`, `CloseWindow`.
* Add to `platform.win32.Version`: `GetFileVersionInfoSize`, `GetFileVersionInfo`, `VerQueryValue`.
* Add to `platform.win32.Advapi32`: `GetFileSecurity`, `RegQueryValueEx(...Long...)`.
* Add to `platform.win32.Netapi32`: `NetUserGetInfo`.

Bug Fixes
--------
* Revise cleanup of in-use temporary files on win32 (issue 6).
* Fix structure alignment issues on linux/ppc.
* Fix structure alignment issues on linux/arm.
* Account for NIO Buffer position (JIRA issue 185).
* Avoid crash with very long Strings (> 150k in length).
* Fix bug tracking Memory with an associated direct ByteBuffer.
* Fix bug handling structs by value when type mappers are in effect (JIRA issue 188).

Release 3.3.0
=============

Features
--------

* Facilitate `Memory` subclasses (jbellis).
* Allow multiple fields of the same type in Unions (Francis Barber).
* Add `platform.win32.Advapi32.AdjustTokenPrivileges`, `platform.win32.Advapi32.LookupPrivilegeName`, `platform.win32.Advapi32.LookupPrivilegeValue`, `platform.win32.Advapi32.ImpersonateSelf`.
* Add `platform.win32.Advapi32.DuplicateTokenEx`, `platform.win32.Advapi32.CreateProcessAsUser`, `platform.win32.Kernel32.GetExitCodeProcess`, `platform.win32.Kernel32.TerminateProcess`, `platform.win32.Kernel32.ReadFile`, `platform.win32.Kernel32.CreatePipe`, `platform.win32.Kernel32.SetHandleInformation` and related constants / structures in `platform.win32.WinBase` and `platform.win32.WinNT`. Please note that the `SECURITY_ATTRIBUTES` structure has been moved from `platform.win32.WinNT` to `platform.win32.WinBase`.
* Add `platform.win32.Kernel32.DeleteFile` and `platform.win32.Kernel32Util.deleteFile`.
* Add `platform.win32.Kernel32.GetFileAttributes` and `platform.win32.Kernel32Util.getFileAttributes`.
* Add `platform.win32.Kernel32.GetTickCount`.
* Add Win32 Service functions to `platform.win32.Advapi32`.
* Add `platform.win32.W32ServiceManager` and `W32Service`.
* Add Win32 Event Logging functions to `platform.win32.Advapi32` and `platform.win32.Advapi32Util.EventLogIterator`.
* `platform.win32.Advapi32Util.registryCreateKey` returns `true` if key was created, `false` if it already exists.
* Add `REG_BINARY`, `REG_EXPAND_SZ` and `REG_MULTI_SZ` support to `platform.win32.Advapi32Util` registry functions.
* Reduce JNI crossings in a number of native methods, moving object creation out into pure Java code.

Bug Fixes
---------

* Move all native functions into `com.sun.jna.Native`, to ensure that all dependent classes must be disposed before the `Native` class is unloaded.  Note that this change is incompatible with all previous JNA native libraries.
* Fix `platform.win32.Kernel32.GetNativeSystemInfo` and `GetSystemInfo` AV on Win64.
* Fix several potential minor bugs as reported by TvT.
* Fix bug in Structure.StructureSet.toString (Blair Zajac), exposed by Tomcat ThreadLocal cleanup.
* Fix several bugs when using Structure(Pointer) ctor and array fields (Samuel Audet).

Release 3.2.7
=============

Features
--------

* Add native peer value accessors for Pointer
* The `jna.library.path` property is now re-evaluated whenever a native library is loaded.  Previously this value was cached when the JNA classes loaded.
* `Native.loadLibrary` can now load `.drv` files.
* Refactor `com.sun.jna.platform.win32.WINBASE` into `WinDef`, `WinNT` and `BaseTSD`, matching Windows SDK headers.
* Refactor constants from `com.sun.jna.platform.win32.GDI32` into `WinGDI`, matching Windows SDK headers.
* Refactor constants from `com.sun.jna.platform.win32.User32` into `WinUser`, matching Windows SDK headers.
* Refactor `platform.win32.WinNT.LARGE_INTEGER` into a union.
* Add `platform.win32.ObjBase`, `com.sun.jna.platform.win32.Ole32.CoInitializeEx`, `CoUninitialize`, and `CoCreateInstance`.
* Add `platform.win32.Oleaut32.SysAllocString` and `SysFreeString`.
* Add `platform.win32.Secur32.ImpersonateSecurityContext` and `RevertSecurityContext`.
* Add `platform.win32.WinNT.WELL_KNOWN_SID_TYPE`, `SECURITY_MAX_SID_SIZE` and other related SID-related constants.
* Add `platform.win32.Advapi32.CreateWellKnownSid` and `IsWellKnownSid` and `com.sun.jna.platform.win32.Advapi32Util.isWellKnownSid`.
* Add `platform.win32.Kernel32.GetVersion`, `GetVersionEx`, `GetSystemInfo`, `GetNativeSystemInfo`, `GlobalMemoryStatusEx`, `GetLogicalDriveStrings` and `IsWow64Process`.
* Add `platform.win32.Kernel32Util.getLogicalDriveStrings`.
* Add `platform.win32.User32.GetSystemMetrics`.
* Add `platform.win32.BaseTSD.DWORD_PTR`.
* Add `platform.win32.WinBase.SYSTEM_INFO` and `MEMORYSTATUSEX`.
* Add `platform.win32.WinNT.OSVERSIONINFOEX`, `VER` constants.
* Add `platform.win32.WinDef.ULONGLONG` and `DWORDLONG`.
* Add `platform.win32.Shell32.SHGetDesktopFolder` (prep work for Com4JNA).
* Add `platform.win32.Winspool.GetPrinterInfo`.
* Add `platform.win32.WinspoolUtil.getPrinterInfo1`.
* Add `platform.win32.GDI32.GetDeviceCaps`.
* Add `platform.win32.GDI32.GetDIBits`.

Bug Fixes
---------

* Fix `ClassCastException` in `Structure.equals` (issue 152).
* Fix bug initializing a structure object from existing memory when the structure has initialized fields (issue 133).
* Fix NPE reading an array of string from a pointer when an element of the array is `NULL` (issue 151).
* Avoid calling `UnregisterNatives` in native code (issue 154).
* Compare unpacked library path against canonical (long) filename (issue 156).
* Fix `read()` of uninitialized memory in `platform.win32.Advapi32Util.getTokenGroups` and `getTokenAccount`.
* Fix `com.sun.jna.platform.win32.Secur32.QuerySecurityContextToken` to take a `CtxtHandle` instead of `PSecHandle`.
* Fix definition of BITMAPINFO (platform/win32).

Release 3.2.5
=============

Features
--------

* Split code in examples.jar into a contrib platform.jar package and individual packages for demos.
* Fix Eclipse build and added Eclipse projects for all contrib samples, import projects from jnalib and contrib.
* Ensure Structure fields correctly ordered when inherited.
* Use explicit Structure field whenever provided, regardless of whether the VM requires it.
* Add Win32 mappings for two dozen functions from Kernel32.dll, Advapi32.dll, Netapi32.dll, Secur32.dll, NtDll.dll, Ole32.dll, Shell32.dll and Crypt32.dll to com.sun.jna.platform.win32.
* Port parts of WinError.h, WinNT.h, LMAccess.h, LMCons.h, LMErr.h, LMJoin.h, NTStatus.h, ShlObj.h, WinDef.h, ShellApi.h, Wdm.h, WinReg.h, WinCrypt.h, Sspi.h, Guid.h, NtSecApi.h and DsGetDc.h.
* Add Win32 simplified utility interfaces Kernel32Util, Advapi32Util, Netapi32Util, Crypt32Util, NtDllUtil, Shell32Util, Ole32Util and Secur32Util to com.sun.jna.platform.win32.
* Support unicode paths in W32FileUtils.
* Fix exception during dispose in W32FileMonitor.

Bug Fixes
---------

* Provide String.replace for 1.4 compatibility.
* Avoid allocating memory when Structure is provided a pointer in the ctor.
* Ensure proper value returned in Pointer.getValue() for non-null, unchanged NIO Buffer values.
* Use 1.4-compatible URI generation (issue 149).

Release 3.2.4
=============

Features
--------

* Make Pointer ctor public.
* Provide access to Function objects for arbitrary Pointer values.
* Add linux/ia64 binaries (bpiwowar).  See issue 134 patch.

Bug Fixes
---------

* Use a more robust method to decode a file-based URL (issue 135).

Release 3.2.3
=============

Features
--------

* Include version information in code in case package information lost.

Bug Fixes
---------

* Fix WindowUtils exception on mouse over TrayIcon.
* Fix bug toggling windows transparent/opaque (win32/OSX).
* Avoid overwriting unchanged Pointer values in arrays (function calls with Pointer[] and Structure.read).
* Ensure Structure fields marked `final` are never written.
* Fix bug preventing proper population Structure.ByReference fields on Structure read.
* Ensure double buffering is disabled in components added to a transparent window.
* Fix UnsatisfiedLinkError attempting to load system libraries under Web Start.
* Fix loading Web Start-provided libraries on OSX (libraries must have a .jnilib suffix under Web Start).
* Properly include sources in Maven zip file (Issue 129).

Release 3.2.2
=============

Features
--------

* Provide length-specified Pointer.getStringArray()

Bug Fixes
---------

* Fix crash with direct mapping if NULL struct* used (Issue 125).
* Fix case where null-valued Structure fields would get non-null values on write.
* Synch callback Structure/Structure[] arguments on callback return.
* Fix NPE when mapping an interface to the current process.
* Automatically load proper C library version from current process on Linux (avoids crashing bug on Ubuntu with libc-i686 packages active).
* Avoid scanning structure contents in Structure.toString if contents aren't actually used.

Release 3.2.1
==========

Features
--------

* Add HRESULT, LONG mapping to W32API (marc strapetz).


Bug Fixes
---------

* Fix definition of HWND_BROADCAST in W32API.
* Fix memory alignment checking (Issue 121).
* Fix Structure equals/hashCode implementation, based on current Java fields rather than strictly native memory contents.  Avoid using equals/hashCode when avoiding recursive reads/writes.

Release 3.2.0
=============

Features
--------

* Handle String, Structure, Callback, Buffer, and primitive arrays in direct mappings.  Handle NativeMapped and TypeMapper, with optimized paths for IntegerType and PointerType.
* Optionally throw errno/GetLastError as an exception.  This is preferred to (and more efficient than) calling Native.getLastError().
* Unload/delete native library unpacked from jna.jar if Native class is garbage collected.  Only install shutdown hook if using the system class loader.
* Auto-write contiguous Structure arrays when first element is written.
* Support NativeMapped[] as function arguments for interface-mapped libraries (Issue 90).
* Enable function lookup within current process on Windows.

Bug Fixes
---------

* Restrict recursive structure reads/writes by thread instead of globally. This avoids potentially missed reads/writes with concurrent access (Issue 120).
* Ensure Memory is not GC'd and freed if direct NIO buffers mapped to it are extant.
* Allow types derived from java.nio.Buffer as Structure fields.

Release 3.1.0
=============

Features
--------

* Add raw JNI mapping of static Java methods.  Performance is about 10X that of traditional JNA interface mapping, although with less type conversion functionality.
* Add library option to allow passing/return of Java Objects.
* Allow handling of uncaught callback exceptions (Issue 63).
* Object oriented interface to X server (see contrib/x11)
* Make Memory class more accessible.
* Provide Structure ctor with Pointer argument (issue 102).
* Allow implicit library access to current process on linux (issue 98).
* Open all shared libraries with RTLD_GLOBAL, if applicable.  This was the default behavior on OSX and changes the default behavior on linux.
* Allow NIO Buffer as Structure field (with limitations) (Issue 57)
* Add `size_t` size.

Bug Fixes
---------

* Run tests with libjsig.so, if available, which fixes some crashes when running tests on 64-bit platforms.
* Fix Issue 104.
* Fix Issue 94 (Java 1.6 update 10 regression).
* Fix Issue 51 (Java 1.6 update 10 regression).
* Fix Issue 95.
* Fix Issue 101.
* Fix Issue 111, memory leak with String-returning Callback.
* Fix missing storage of union type information (affects usage of struct/union by value as argument and return type).
* Remove non-functional Structure ctors requiring explicit size.

Release 3.0.9
=============

Bug Fixes
---------

*  Fix issue 93 by only manually searching jna.library.path, then falling back to passing the mapped library name to dlopen/LoadLibrary.  This fixes an issue in JRUBY where the incorrect libc.so.6 was being loaded.

Release 3.0.8
==========

Features
--------

* Auto-map Pointer[]/String[]/WString[] return values.
* Provide utility functions to convert String to primitive array.
* Add jna.library.boot.path property to define the directory that the native stub library is loaded from

Release 3.0.7
==========

Features
--------

* Improve Win32 loading of libraries with dependencies.

Bug Fixes
---------

* Fix bug reading structures with PointerType fields, introduced with Pointer field preservation fix.

Release 3.0.6
=============

Features
--------

* Allow arbitrary callback method names if only one method is defined in the class which implements Callback (colinwalters).
* Allow specification of callback type mappers by using a TYPE_MAPPER field (colinwalters).
* Allow uninitialized (null-valued) boxed primitives in Structures (colinwalters).
* Add convenience methods to set active Union field and value simultaneously (xylo).
* Augment Union read/writeField to set the active field.
* Allow Structure auto-synch across native calls to be disabled.
* Win64 support.

Bug Fixes
---------

* Avoid overwriting unchanged Structure fields of type Pointer.
* Avoid more content dragging on OSX or warn if it's too late.
* Fix UnsatisfiedLinkError using transparent window on Win2K.
* Fix memory leak with callbacks called from native threads with no Java context (johnwallace).
* Defer structure size calculation if type mapper not yet set, allowing type mapper to be set in derived constructors (colinwalters).
* Ensure structure memory is allocated in Structure.read/writeField.

Release 3.0.5
=============

Features
--------

* Allow explicit declaration of field order for VMs which have an unpredictable field order.
* Check for w32 libraries with a "lib" prefix in addition to normal lookup.
* Allow String[]/WString[] as callback argument/return value (assume NULL-terminated array).
* Add Solaris8 compatibility to sunos-sparc build (Corey Puffalt).
* Look up libraries using web start library path, if appropriate (Corey Puffalt).
* Use constants to return integer boolean values.

Bug Fixes
---------

* Properly track cursor on alpha-masked windows.
* Avoid searching /lib or /usr/lib on 64-bit Linux.
* Avoid using incorrect version of a library when both 32- and 64-bit versions are found.
* Avoid transparent window events always dragging window bug on OSX.
* Fix division by zero error calculating structure size on OSX/ppc.
* Avoid overwriting initialized NativeMapped Structure fields when calculating structure size.
* Fix NPE reading back into StringArray.

Release 3.0.4
=============

Features
--------

* Automatically write contents of Structure.ByReference fields on Structure.write().
* Use the actual parameter type in Function invocations if no parameter type information is available (whether method is missing or untyped varargs).
* Augmented X11 library mappings (xylo).
* Support read/write of NativeMapped arrays within Structure (notably NativeLong).

Bug Fixes
---------

* Fix library load error when /usr/lib32 and /usr/lib both exist (linux) (Marek Slama).
* Avoid incorrect matches against libraries named with the same prefix (e.g. libc-client.so vs libc.so) (xylo).
* Properly handle arrays of NativeMapped (e.g. NativeLong) as a Structure field (stefan endrullis).
* Ensure structure size calculated prior to setting union active type.
* XID is 64-bits on 64-bit X clients (xylo).
* Ensure proper arch name is used on Debian (amd64 instead of x86_64).

Release 3.0.3
=============

Features
--------

* Enable build/run using IBM's J9 VM (leonardo).
* Make StdCallFunctionMapper attempt a leading underscore if the simpler mapping doesn't work.
* Allow Structure.read to overwrite final fields (may not work on some 1.4 VMs).

Bug Fixes
---------

* Fix NPE when passing an array of Structure.ByReference.
* Compare entire linux library version when finding a match.
* Don't pass struct by value unless the method signature declares it.
* Restrict custom first element structure alignment to OSX/ppc.
* Improve performance and reduce memory footprint for window masks. Optimize polygon-based masks on w32.  Use XFillRectangles on X11.
* Fix linkage settings on sunos-amd64 to avoid relocation errors.
* Fix callback allocation code on w32, solaris, freebsd, darwin (libffi was misconfigured).
* Fix bug when NativeMapped fields are used in a Structure.ByValue instance.
* Fix NPE calling Structure.read() before memory is initialized.
* Fix NPE calling Structure.read/write with uninitialized NativeMapped fields.

Release 3.0.2
=============

Features
--------

* Attempt to force unload of jnidispatch library prior to deleting it (w32).
* Added amd64 targets for OSX, FreeBSD, and Solaris.

Bug Fixes
---------

* Reduce space allocated for invocation arguments.
* Fix NPE when NativeMapped type is used in a Structure.
* Fix some X11 type mappings for 64-bit.
* Fix OSX Leopard/JRE1.5+ window transparency.
* Fix window alpha compositing on X11.
* Fix loading of libraries with unicode names on OSX.

Release 3.0.1
=============

Features
--------

* Improve transparent window drawing performance on w32
* Use closure allocation from libffi

Bug Fixes
---------

* Ensure nested structure arrays initialized with Structure.toArray use the appropriate native memory.
* Ensure structure size is calculated prior to converting to array
* Avoid creating new windows when setting a window mask
* Fix bug in Pointer.setChar.

Release 3.0
===========

Features
--------

* More supported platforms, via GCC's libffi (wmeissner)
* Support struct by value as parameter and return value (duncan)
* Support struct by reference within structures
* Provide access to native peer for java.awt.Component
* Provide access to native peer on OS X.
* Support MINGW32 builds (fullung)
* Allow per-field Structure read/write by field name
* Avoid writing Structure fields marked 'volatile'
* Read and wrap function pointers in Structure fields when read with a Java proxy to allow easy Java-side invocation (Ken Larson)
* Support array-backed Buffers as arguments (wmeissner)
* Auto-conversion of custom types (wmeissner)
* Allow pointer type-safety
* Optional VM crash protection, via Native.setProtected(boolean)
* Auto-convert WString[]
* Provide library synchronization wrapper similar to Collections.synchronizedX
* Support lookup of OSX framework libraries by name
* Explicit access to shared library global data
* Invocation interception to facilitate translation of C preprocessor macros and inline functions
* Provide utility to determine Web Start native library cache location; auto-include this path if jnidispatch is included as a &lt;nativelib&gt; (robertengels)
* Provide access to aligned memory
* Versioning information embedded in jna.jar and native library

Bug Fixes
---------

* Avoid attempts to free native library if it failed to load (wmeissner)
* Explicitly check method signatures for varargs instead of heuristically guessing (wmeissner)
* Disallow declaring Pointer-derived fields in Structures (Function, Memory)
* Ensure Object.toString/hashCode/equals methods are intercepted on proxyied interfaces
* Update X11 library for 64-bit use (wmeissner)
* Properly map arrays of char*/wchar_t* under w32
* Allow Pointer[] as a Structure field and Function argument
* Fix some misleading Structure error messages
* Properly preserve/return GetLastError/errno after native calls
* Allocate executable memory on w32 to avoid errors with hardware-enforced data execution protection (DEP)
* Fix VM crash on w32 stdcall callbacks
* Use long offsets and sizes rather than ints (64-bit safe)
* Properly clean up references and release closure memory on JNI_Unload
* Use simpler AWT/JAWT library loading workaround
* Avoid changing array references within a Structure on read

Release 2.5
===========

Features
--------

* Unions
* Optimized shaped windows (chris deckers & olivier chafik); instantiation time improved by about 2-3 orders of magnitude for large, mostly contiguous shapes
* Provide type mapping in callback arguments/results
* Provide access to ByteBuffer direct address as a Pointer
* Provide customization of native string encoding with jna.encoding system property

Bug Fixes
---------

* Properly handle VMs with reversed Structure member storage
* Avoid making window undecorated when clearing window mask on X11
* Fix structure alignment bug on OSX/PPC when first element is > 4 bytes in size
* Clearing OSX window mask by setting to MASK_NONE now works properly
* Avoid index exceptions if native buffers are not NUL-terminated on string conversions
* Write initialized Structure[] argument memory prior to function calls
* Fix IllegalArgumentException reading WString into a Structure
* Clear memory when allocating a structure block (fixes VM crash)
* Remove versioned JAWT dependency on OSX, allowing use on 10.3/JRE1.4.

Release 2.4
===========

Features
--------

* Explicitly support unaligned structures
* Auto-reallocate structure arrays
* Automatic handling of w32 UNICODE/ASCII variants
* Automatic mapping of decorated w32 stdcall function names
* Customizable, automatic type conversion of arguments and results (wmeissner)
* Support char*[] arguments as Java String[]
* Structure supports Callback members (wmeissner)
* getByteBuffer from Pointer/Memory (wmeissner)
* Allow GC of native libraries
* Facilitate use from non-Java contexts (JRuby et al.) (wmeissner)
* Improve library path searching (wmeissner)
* Handle Structure[] arguments
* Handle native long arguments and return values
* Handle direct and array-based ByteBuffer arguments (wmeissner)
* Change default w32 build to use GCC (it's free, yo)

Bug Fixes
---------

* Structure.toArray failed to initialize members
* Disallow explicit free of Structure/Memory
* Ensure native libraries are only loaded once until released
* Properly handle NULL when the return value is a Structure
* Proper conversion to wchar_t on linux
* Copy full length of Java strings to C strings instead of stopping when a NUL character is encountered
