/*
 * Copyright (c) 2019 Daniel Widdis
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
package com.sun.jna.platform.mac;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.platform.mac.CoreFoundation.CFAllocatorRef;
import com.sun.jna.platform.mac.CoreFoundation.CFArrayRef;
import com.sun.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

/**
 * The I/O Kit framework implements non-kernel access to I/O Kit objects
 * (drivers and nubs) through the device-interface mechanism.
 */
public interface IOKit extends Library {

    IOKit INSTANCE = Native.load("IOKit", IOKit.class);

    int kIORegistryIterateRecursively = 0x00000001;
    int kIORegistryIterateParents = 0x00000002;

    double kIOPSTimeRemainingUnlimited = -2.0;
    double kIOPSTimeRemainingUnknown = -1.0;

    MachPort MACH_PORT_NULL = new MachPort();

    /**
     * Communication between tasks is an important element of the Mach philosophy.
     * Mach supports a client/server system structure in which tasks (clients)
     * access services by making requests of other tasks (servers) via messages sent
     * over a communication channel.
     * <p>
     * The endpoints of these communication channels in Mach are called ports, while
     * port rights denote permission to use the channel.
     */
    class MachPort extends PointerType {
        public MachPort() {
            super();
        }

        public MachPort(Pointer p) {
            super(p);
        }
    }

    /**
     * IOKitLib implements non-kernel task access to common IOKit object types -
     * IORegistryEntry, IOService, IOIterator etc. These functions are generic -
     * families may provide API that is more specific.
     * <p>
     * IOKitLib represents IOKit objects outside the kernel with the types
     * io_object_t, io_registry_entry_t, io_service_t, and io_connect_t. Function
     * names usually begin with the type of object they are compatible with - e.g.,
     * IOObjectRelease can be used with any io_object_t. Inside the kernel, the c++
     * class hierarchy allows the subclasses of each object type to receive the same
     * requests from user level clients, for example in the kernel, IOService is a
     * subclass of IORegistryEntry, which means any of the IORegistryEntryXXX
     * functions in IOKitLib may be used with io_service_t's as well as
     * io_registry_t's. There are functions available to introspect the class of the
     * kernel object which any io_object_t et al. represents. IOKit objects returned
     * by all functions should be released with IOObjectRelease.
     */
    class IOObject extends MachPort {
        public IOObject() {
            super();
        }

        public IOObject(Pointer p) {
            super(p);
        }

        /**
         * Convenience method for {@link IOKit#IOObjectRelease} on this object.
         *
         * @return 0 if successful, otherwise a {@code kern_return_t} error code.
         */
        public int release() {
            return INSTANCE.IOObjectRelease(this);
        }
    }

    /**
     * An IOKit iterator handle.
     */
    class IOIterator extends IOObject {
        public IOIterator() {
            super();
        }

        public IOIterator(Pointer p) {
            super(p);
        }

        /**
         * Convenience method for {@link IOKit#IOIteratorNext} on this object.
         *
         * @return If the iterator handle is valid, the next element in the iteration is
         *         returned, otherwise zero is returned. The element should be released
         *         by the caller when it is finished.
         */
        public IOService next() {
            return INSTANCE.IOIteratorNext(this);
        }
    }

    /**
     * The base class for all objects in the registry.
     */
    class IORegistryEntry extends IOObject {
        public IORegistryEntry() {
            super();
        }

        public IORegistryEntry(Pointer p) {
            super(p);
        }
    }

    /**
     * The base class for most I/O Kit families, devices, and drivers.
     */
    class IOService extends IORegistryEntry {
        public IOService() {
            super();
        }

        public IOService(Pointer p) {
            super(p);
        }
    }

    /**
     * For an application to communicate with a device, the first thing it must do
     * is create a connection between itself and the in-kernel object representing
     * the device. To do this, it creates a user client object.
     */
    class IOConnect extends IOService {
        public IOConnect() {
            super();
        }

        public IOConnect(Pointer p) {
            super(p);
        }
    }

    /**
     * Returns the mach port used to initiate communication with IOKit.
     *
     * @param bootstrapPort
     *            Pass {@link #MACH_PORT_NULL} for the default.
     * @param masterPort
     *            A pointer to the master port is returned.
     * @return 0 if successful, otherwise a {@code kern_return_t} error code.
     */
    int IOMasterPort(MachPort bootstrapPort, PointerByReference masterPort);

    /**
     * Create a matching dictionary that specifies an {@code IOService} class match.
     *
     * @param name
     *            The class name. Class matching is successful on {@code IOService}s
     *            of this class or any subclass.
     * @return The matching dictionary created, is returned on success, or
     *         {@code null} on failure.
     *         <p>
     *         The dictionary is commonly passed to
     *         {@link #IOServiceGetMatchingServices} which will consume a reference,
     *         otherwise it should be released with {@link CoreFoundation#CFRelease}
     *         by the caller.
     */
    CFMutableDictionaryRef IOServiceMatching(String name);

    /**
     * Create a matching dictionary that specifies an {@code IOService} name match.
     *
     * @param name
     *            The {@code IOService} name.
     * @return The matching dictionary created, is returned on success, or
     *         {@code null} on failure.
     *         <p>
     *         The dictionary is commonly passed to
     *         {@link #IOServiceGetMatchingServices} which will consume a reference,
     *         otherwise it should be released with {@link CoreFoundation#CFRelease}
     *         by the caller.
     */
    CFMutableDictionaryRef IOServiceNameMatching(String name);

    /**
     * Create a matching dictionary that specifies an {@code IOService} match based
     * on BSD device name.
     *
     * @param masterPort
     *            The master port obtained from {@link #IOMasterPort}.
     * @param options
     *            No options are currently defined.
     * @param bsdName
     *            The BSD name.
     * @return The matching dictionary created, is returned on success, or
     *         {@code null} on failure.
     *         <p>
     *         The dictionary is commonly passed to
     *         {@link #IOServiceGetMatchingServices} which will consume a reference,
     *         otherwise it should be released with {@link CoreFoundation#CFRelease}
     *         by the caller.
     */
    CFMutableDictionaryRef IOBSDNameMatching(MachPort masterPort, int options, String bsdName);

    /**
     * Look up a registered IOService object that matches a matching dictionary.
     *
     * @param masterPort
     *            The master port obtained from {@link #IOMasterPort}.
     * @param matchingDictionary
     *            A CF dictionary containing matching information, of which one
     *            reference is always consumed by this function. IOKitLib can
     *            construct matching dictionaries for common criteria with helper
     *            functions such as {@link #IOServiceMatching},
     *            {@link #IOServiceNameMatching}, and {@link #IOBSDNameMatching}.
     * @return The first service matched is returned on success.
     *         <p>
     *         The service must be released by the caller.
     */
    IOService IOServiceGetMatchingService(MachPort masterPort, CFMutableDictionaryRef matchingDictionary);

    /**
     * Look up registered IOService objects that match a matching dictionary.
     *
     * @param masterPort
     *            The master port obtained from {@link #IOMasterPort}.
     * @param matchingDictionary
     *            A CF dictionary containing matching information, of which one
     *            reference is always consumed by this function. IOKitLib can
     *            construct matching dictionaries for common criteria with helper
     *            functions such as {@link #IOServiceMatching},
     *            {@link #IOServiceNameMatching}, and {@link #IOBSDNameMatching}.
     * @param iterator
     *            An iterator handle is returned on success, and should be released
     *            by the caller when the iteration is finished.
     * @return 0 if successful, otherwise a {@code kern_return_t} error code.
     */
    int IOServiceGetMatchingServices(MachPort masterPort, CFMutableDictionaryRef matchingDictionary,
            PointerByReference iterator);

    /**
     * Returns the next object in an iteration.
     *
     * @param iterator
     *            An IOKit iterator handle.
     * @return If the iterator handle is valid, the next element in the iteration is
     *         returned, otherwise zero is returned. The element should be released
     *         by the caller when it is finished.
     */
    IOService IOIteratorNext(IOIterator iterator);

    /**
     * Create a CF representation of a registry entry's property.
     *
     * @param entry
     *            The registry entry handle whose property to copy.
     * @param key
     *            A {@code CFString} specifying the property name.
     * @param allocator
     *            The CF allocator to use when creating the CF container.
     * @param options
     *            No options are currently defined.
     * @return A CF container is created and returned the caller on success.
     *         <p>
     *         The caller should release with {@link CoreFoundation#CFRelease}.
     */
    CFTypeRef IORegistryEntryCreateCFProperty(IORegistryEntry entry, CFStringRef key, CFAllocatorRef allocator,
            int options);

    /**
     * Create a CF dictionary representation of a registry entry's property table.
     *
     * @param entry
     *            The registry entry handle whose property table to copy.
     * @param properties
     *            A CFDictionary is created and returned the caller on success. The
     *            caller should release with CFRelease.
     * @param allocator
     *            The CF allocator to use when creating the CF containers.
     * @param options
     *            No options are currently defined.
     * @return 0 if successful, otherwise a {@code kern_return_t} error code.
     */
    int IORegistryEntryCreateCFProperties(IORegistryEntry entry, PointerByReference properties,
            CFAllocatorRef allocator,
            int options);

    /**
     * Create a CF representation of a registry entry's property.
     *
     * @param entry
     *            The registry entry at which to start the search.
     * @param plane
     *            The name of an existing registry plane. Plane names are defined in
     *            {@code IOKitKeys.h}, for example, {@code kIOServicePlane}.
     * @param key
     *            A {@code CFString} specifying the property name.
     * @param allocator
     *            The CF allocator to use when creating the CF container.
     * @param options
     *            {@link #kIORegistryIterateRecursively} may be set to recurse
     *            automatically into the registry hierarchy. Without this option,
     *            this method degenerates into the standard
     *            {@link #IORegistryEntryCreateCFProperty} call.
     *            {@link #kIORegistryIterateParents} may be set to iterate the
     *            parents of the entry, in place of the children.
     * @return A CF container is created and returned the caller on success. The
     *         caller should release with CFRelease.
     */
    CFTypeRef IORegistryEntrySearchCFProperty(IORegistryEntry entry, String plane, CFStringRef key,
            CFAllocatorRef allocator,
            int options);

    /**
     * Returns an ID for the registry entry that is global to all tasks.
     *
     * @param entry
     *            The registry entry handle whose ID to look up.
     * @param id
     *            The resulting ID.
     * @return 0 if successful, otherwise a {@code kern_return_t} error code.
     */
    int IORegistryEntryGetRegistryEntryID(IORegistryEntry entry, LongByReference id);

    /**
     * Returns a name assigned to a registry entry.
     *
     * @param entry
     *            The registry entry handle whose name to look up.
     * @param name
     *            The caller's buffer to receive the name. This must be a 128-byte
     *            buffer.
     * @return 0 if successful, otherwise a {@code kern_return_t} error code.
     */
    int IORegistryEntryGetName(IORegistryEntry entry, Pointer name);

    /**
     * Returns an iterator over a registry entry’s child entries in a plane.
     *
     * @param entry
     *            The registry entry whose children to iterate over.
     * @param plane
     *            The name of an existing registry plane. Plane names are defined in
     *            {@code IOKitKeys.h}, for example, {@code kIOServicePlane}.
     * @param iter
     *            The created iterator over the children of the entry, on success.
     *            The iterator must be released when the iteration is finished.
     * @return 0 if successful, otherwise a {@code kern_return_t} error code.
     */
    int IORegistryEntryGetChildIterator(IORegistryEntry entry, String plane, PointerByReference iter);

    /**
     * Returns the first child of a registry entry in a plane.
     *
     * @param entry
     *            The registry entry whose child to look up.
     * @param plane
     *            The name of an existing registry plane. Plane names are defined in
     *            {@code IOKitKeys.h}, for example, {@code kIOServicePlane}.
     * @param child
     *            The first child of the registry entry, on success. The child must
     *            be released by the caller.
     * @return 0 if successful, otherwise a {@code kern_return_t} error code.
     */
    int IORegistryEntryGetChildEntry(IORegistryEntry entry, String plane, PointerByReference child);

    /**
     * Returns the first parent of a registry entry in a plane.
     *
     * @param entry
     *            The registry entry whose parent to look up.
     * @param plane
     *            The name of an existing registry plane. Plane names are defined in
     *            {@code IOKitKeys.h}, for example, {@code kIOServicePlane}.
     * @param parent
     *            The first parent of the registry entry, on success. The parent
     *            must be released by the caller.
     * @return 0 if successful, otherwise a {@code kern_return_t} error code.
     */
    int IORegistryEntryGetParentEntry(IORegistryEntry entry, String plane, PointerByReference parent);

    /**
     * Return a handle to the registry root.
     *
     * @param masterPort
     *            The master port obtained from {@link #IOMasterPort}.
     * @return A handle to the IORegistryEntry root instance, to be released with
     *         {@link #IOObjectRelease} by the caller, or 0 on failure.
     */
    IORegistryEntry IORegistryGetRootEntry(MachPort masterPort);

    /**
     * Performs an OSDynamicCast operation on an IOKit object.
     *
     * @param object
     *            An IOKit object.
     * @param className
     *            The name of the class.
     * @return If the object handle is valid, and represents an object in the kernel
     *         that dynamic casts to the class true is returned, otherwise false.
     */
    boolean IOObjectConformsTo(IOObject object, String className);

    /**
     * Releases an object handle previously returned by {@code IOKitLib}.
     *
     * @param object
     *            The IOKit object to release.
     * @return 0 if successful, otherwise a {@code kern_return_t} error code.
     */
    int IOObjectRelease(IOObject object);

    /**
     * A request to create a connection to an IOService.
     *
     * @param service
     *            The IOService object to open a connection to, usually obtained via
     *            the {@link #IOServiceGetMatchingServices} API.
     * @param owningTask
     *            The mach task requesting the connection.
     * @param type
     *            A constant specifying the type of connection to be created,
     *            interpreted only by the IOService's family.
     * @param connect
     *            An {@code io_connect_t} handle is returned on success, to be used
     *            with the IOConnectXXX APIs. It should be destroyed with
     *            {@link IOServiceClose}.
     * @return A return code generated by {@code IOService::newUserClient}.
     */
    int IOServiceOpen(IOService service, MachPort owningTask, int type, PointerByReference connect);

    /**
     * Returns the busyState of an IOService.
     *
     * @param service
     *            The IOService whose busyState to return.
     * @param busyState
     *            The busyState count is returned.
     * @return 0 if successful, otherwise a {@code kern_return_t} error code.
     */
    int IOServiceGetBusyState(IOService service, IntByReference busyState);

    /**
     * Close a connection to an IOService and destroy the connect handle.
     *
     * @param connect
     *            The connect handle created by IOServiceOpen. It will be destroyed
     *            by this function, and should not be released with IOObjectRelease.
     * @return 0 if successful, otherwise a {@code kern_return_t} error code.
     */
    int IOServiceClose(IOConnect connect);

    /**
     * Returns a blob of Power Source information in an opaque CFTypeRef.
     *
     * @return {@code null} if errors were encountered, a {@link CFTypeRef}
     *         otherwise.
     *         <p>
     *         Caller must {@link CoreFoundation#CFRelease} the return value when
     *         done accessing it.
     */
    CFTypeRef IOPSCopyPowerSourcesInfo();

    /**
     * Returns a CFArray of Power Source handles, each of type CFTypeRef.
     *
     * @param blob
     *            Takes the {@link CFTypeRef} returned by
     *            {@link #IOPSCopyPowerSourcesInfo}
     * @return {@code null} if errors were encountered, otherwise a CFArray of
     *         {@link CFTypeRef}s.
     *         <p>
     *         Caller must {@link CoreFoundation#CFRelease} the returned
     *         {@link CFArrayRef}.
     */
    CFArrayRef IOPSCopyPowerSourcesList(CFTypeRef blob);

    /**
     * Returns a CFDictionary with readable information about the specific power
     * source.
     *
     * @param blob
     *            the {@link CFTypeRef} returned by
     *            {@link #IOPSCopyPowerSourcesInfo}
     * @param ps
     *            One of the {@link CFTypeRef}s in the CFArray returned by
     *            {@link #IOPSCopyPowerSourcesList}.
     * @return {@code null} if an error was encountered, otherwise a CFDictionary.
     *         <p>
     *         Caller should NOT release the returned CFDictionary - it will be
     *         released as part of the {@link CFTypeRef} returned by
     *         {@link IOPSCopyPowerSourcesInfo}.
     */
    CFDictionaryRef IOPSGetPowerSourceDescription(CFTypeRef blob, CFTypeRef ps);

    /**
     * Returns the estimated seconds remaining until all power sources (battery
     * and/or UPS's) are empty.
     *
     * @return Returns {@link #kIOPSTimeRemainingUnknown} if the OS cannot determine
     *         the time remaining.
     *         <p>
     *         Returns {@link #kIOPSTimeRemainingUnlimited} if the system has an
     *         unlimited power source.
     *         <p>
     *         Otherwise returns a positive number indicating the time remaining in
     *         seconds until all power sources are depleted.
     */
    double IOPSGetTimeRemainingEstimate();
}
