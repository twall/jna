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

import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation.CFBooleanRef;
import com.sun.jna.platform.mac.CoreFoundation.CFDataRef;
import com.sun.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFNumberRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;
import com.sun.jna.platform.mac.IOKit.IOIterator;
import com.sun.jna.platform.mac.IOKit.IORegistryEntry;
import com.sun.jna.platform.mac.IOKit.IOService;
import com.sun.jna.platform.mac.SystemB.MachPort;
import com.sun.jna.ptr.PointerByReference;

/**
 * Provides utilities for IOKit.
 */
public class IOKitUtil {
    private static final IOKit IO = IOKit.INSTANCE;
    private static final CoreFoundation CF = CoreFoundation.INSTANCE;

    private IOKitUtil() {
    }

    /**
     * Gets a pointer to the Mach Master Port.
     *
     * @return The master port.
     *         <p>
     *         Multiple calls to {@link #getMasterPort} will not result in leaking
     *         ports (each call to {@link IOKit#IOMasterPort} adds another send
     *         right to the port) but it is considered good programming practice to
     *         deallocate the port when you are finished with it, using
     *         {@link SystemB#mach_port_deallocate}.
     */
    public static MachPort getMasterPort() {
        PointerByReference port = new PointerByReference();
        IO.IOMasterPort(SystemB.MACH_PORT_NULL, port);
        return new MachPort(port.getValue());
    }

    /**
     * Gets the IO Registry root.
     *
     * @return a handle to the IORoot. Callers should release when finished, using
     *         {@link IOKit#IOObjectRelease}.
     */
    public static IORegistryEntry getRoot() {
        MachPort masterPort = getMasterPort();
        IORegistryEntry root = IO.IORegistryGetRootEntry(masterPort);
        masterPort.deallocate();
        return root;
    }

    /**
     * Opens a the first IOService matching a service name.
     *
     * @param serviceName
     *            The service name to match
     * @return a handle to an IOService if successful, {@code null} if failed.
     *         Callers should release when finished, using
     *         {@link IOKit#IOObjectRelease}.
     */
    public static IOService getMatchingService(String serviceName) {
        CFMutableDictionaryRef dict = IO.IOServiceMatching(serviceName);
        if (dict != null) {
            return getMatchingService(dict);
        }
        return null;
    }

    /**
     * Opens a the first IOService matching a dictionary.
     *
     * @param matchingDictionary
     *            The dictionary to match. This method will consume a reference to
     *            the dictionary.
     * @return a handle to an IOService if successful, {@code null} if failed.
     *         Callers should release when finished, using
     *         {@link IOKit#IOObjectRelease}.
     */
    public static IOService getMatchingService(CFDictionaryRef matchingDictionary) {
        MachPort masterPort = getMasterPort();
        IOService service = IO.IOServiceGetMatchingService(masterPort, matchingDictionary);
        masterPort.deallocate();
        return service;
    }

    /**
     * Convenience method to get IOService objects matching a service name.
     *
     * @param serviceName
     *            The service name to match
     * @return a handle to an IOIterator if successful, {@code null} if failed.
     *         Callers should release when finished, using
     *         {@link IOKit#IOObjectRelease}.
     */
    public static IOIterator getMatchingServices(String serviceName) {
        CFMutableDictionaryRef dict = IO.IOServiceMatching(serviceName);
        if (dict != null) {
            return getMatchingServices(dict);
        }
        return null;
    }

    /**
     * Convenience method to get IOService objects matching a dictionary.
     *
     * @param matchingDictionary
     *            The dictionary to match. This method will consume a reference to
     *            the dictionary.
     * @return a handle to an IOIterator if successful, {@code null} if failed.
     *         Callers should release when finished, using
     *         {@link IOKit#IOObjectRelease}.
     */
    public static IOIterator getMatchingServices(CFDictionaryRef matchingDictionary) {
        MachPort masterPort = getMasterPort();
        PointerByReference serviceIterator = new PointerByReference();
        int result = IO.IOServiceGetMatchingServices(masterPort, matchingDictionary, serviceIterator);
        masterPort.deallocate();
        if (result == 0 && serviceIterator.getValue() != null) {
            return new IOIterator(serviceIterator.getValue());
        }
        return null;
    }

    /**
     * Convenience method to get the IO dictionary matching a bsd name.
     *
     * @param bsdName
     *            The bsd name of the registry entry
     * @return The dictionary ref if successful, {@code null} if failed. Callers
     *         should release when finished, using {@link IOKit#IOObjectRelease}.
     */
    public static CFMutableDictionaryRef getBSDNameMatchingDict(String bsdName) {
        MachPort masterPort = getMasterPort();
        CFMutableDictionaryRef result = IO.IOBSDNameMatching(masterPort, 0, bsdName);
        masterPort.deallocate();
        return result;
    }

    /**
     * Convenience method to get a String value from an IO Registry
     *
     * @param entry
     *            A handle to the registry entry
     * @param key
     *            The string name of the key to retrieve
     * @return The value of the registry entry if it exists;{@code null} otherwise
     */
    public static String getIORegistryStringProperty(IORegistryEntry entry, String key) {
        String value = null;
        CFStringRef keyAsCFString = CFStringRef.createCFString(key);
        CFTypeRef valueAsCFType = IO.IORegistryEntryCreateCFProperty(entry, keyAsCFString, CF.CFAllocatorGetDefault(),
                0);
        if (valueAsCFType != null && valueAsCFType.getPointer() != null) {
            CFStringRef valueAsCFString = new CFStringRef(valueAsCFType.getPointer());
            value = valueAsCFString.stringValue();
        }
        keyAsCFString.release();
        if (valueAsCFType != null) {
            valueAsCFType.release();
        }
        return value;
    }

    /**
     * Convenience method to get a {@code long} value from an IO Registry.
     *
     * @param entry
     *            A handle to the registry entry
     * @param key
     *            The string name of the key to retrieve
     * @param defaultValue
     *            The value to return if unsuccessful
     * @return The value of the registry entry if it exists.
     *         <p>
     *         This method assumes a 64-bit integer is stored and does not do type
     *         checking. If this object's type differs from the return type, and the
     *         conversion is lossy or the return value is out of range, then this
     *         method returns an approximate value. If the value does not exist, the
     *         {@code defaultValue} is returned.
     */
    public static long getIORegistryLongProperty(IORegistryEntry entry, String key, long defaultValue) {
        long value = defaultValue;
        CFStringRef keyAsCFString = CFStringRef.createCFString(key);
        CFTypeRef valueAsCFType = IO.IORegistryEntryCreateCFProperty(entry, keyAsCFString, CF.CFAllocatorGetDefault(),
                0);
        if (valueAsCFType != null && valueAsCFType.getPointer() != null) {
            CFNumberRef valueAsCFNumber = new CFNumberRef(valueAsCFType.getPointer());
            value = valueAsCFNumber.longValue();
        }
        keyAsCFString.release();
        if (valueAsCFType != null) {
            valueAsCFType.release();
        }
        return value;
    }

    /**
     * Convenience method to get an {@code int} value from an IO Registry.
     *
     * @param entry
     *            A handle to the registry entry
     * @param key
     *            The string name of the key to retrieve
     * @param defaultValue
     *            The value to return if unsuccessful
     * @return The value of the registry entry if it exists.
     *         <p>
     *         This method assumes a 32-bit integer is stored and does not do type
     *         checking. If this object's type differs from the return type, and the
     *         conversion is lossy or the return value is out of range, then this
     *         method returns an approximate value. If the value does not exist, the
     *         {@code defaultValue} is returned.
     */
    public static int getIORegistryIntProperty(IORegistryEntry entry, String key, int defaultValue) {
        int value = defaultValue;
        CFStringRef keyAsCFString = CFStringRef.createCFString(key);
        CFTypeRef valueAsCFType = IO.IORegistryEntryCreateCFProperty(entry, keyAsCFString, CF.CFAllocatorGetDefault(),
                0);
        if (valueAsCFType != null) {
            CFNumberRef valueAsCFNumber = new CFNumberRef(valueAsCFType.getPointer());
            value = valueAsCFNumber.intValue();
        }
        keyAsCFString.release();
        if (valueAsCFType != null) {
            valueAsCFType.release();
        }
        return value;
    }

    /**
     * Convenience method to get a {@code double} value from an IO Registry.
     *
     * @param entry
     *            A handle to the registry entry
     * @param key
     *            The string name of the key to retrieve
     * @param defaultValue
     *            The value to return if unsuccessful
     * @return The value of the registry entry if it exists.
     *         <p>
     *         This method assumes a floating point value is stored and does not do
     *         type checking. If this object's type differs from the return type,
     *         and the conversion is lossy or the return value is out of range, then
     *         this method returns an approximate value. If the value does not
     *         exist, the {@code defaultValue} is returned.
     */
    public static double getIORegistryIntProperty(IORegistryEntry entry, String key, double defaultValue) {
        double value = defaultValue;
        CFStringRef keyAsCFString = CFStringRef.createCFString(key);
        CFTypeRef valueAsCFType = IO.IORegistryEntryCreateCFProperty(entry, keyAsCFString, CF.CFAllocatorGetDefault(),
                0);
        if (valueAsCFType != null) {
            CFNumberRef valueAsCFNumber = new CFNumberRef(valueAsCFType.getPointer());
            value = valueAsCFNumber.doubleValue();
        }
        keyAsCFString.release();
        if (valueAsCFType != null) {
            valueAsCFType.release();
        }
        return value;
    }

    /**
     * Convenience method to get a Boolean value from an IO Registry.
     *
     * @param entry
     *            A handle to the registry entry
     * @param key
     *            The string name of the key to retrieve
     * @param defaultValue
     *            The value to return if unsuccessful
     * @return The value of the registry entry if it exists; {@code defaultValue}
     *         otherwise
     */
    public static boolean getIORegistryBooleanProperty(IORegistryEntry entry, String key, boolean defaultValue) {
        boolean value = defaultValue;
        CFStringRef keyAsCFString = CFStringRef.createCFString(key);
        CFTypeRef valueAsCFType = IO.IORegistryEntryCreateCFProperty(entry, keyAsCFString, CF.CFAllocatorGetDefault(),
                0);
        if (valueAsCFType != null) {
            CFBooleanRef valueAsCFBoolean = new CFBooleanRef(valueAsCFType.getPointer());
            value = valueAsCFBoolean.booleanValue();
        }
        keyAsCFString.release();
        if (valueAsCFType != null) {
            valueAsCFType.release();
        }
        return value;
    }

    /**
     * Convenience method to get a byte array value from an IO Registry.
     *
     * @param entry
     *            A handle to the registry entry
     * @param key
     *            The string name of the key to retrieve
     * @return The value of the registry entry if it exists; {@code null} otherwise
     */
    public static byte[] getIORegistryByteArrayProperty(IORegistryEntry entry, String key) {
        byte[] value = null;
        CFStringRef keyAsCFString = CFStringRef.createCFString(key);
        CFTypeRef valueAsCFType = IO.IORegistryEntryCreateCFProperty(entry, keyAsCFString, CF.CFAllocatorGetDefault(),
                0);
        if (valueAsCFType != null) {
            CFDataRef valueAsCFData = new CFDataRef(valueAsCFType.getPointer());
            int length = CF.CFDataGetLength(valueAsCFData).intValue();
            Pointer p = CF.CFDataGetBytePtr(valueAsCFData);
            value = p.getByteArray(0, length);
        }
        keyAsCFString.release();
        if (valueAsCFType != null) {
            valueAsCFType.release();
        }
        return value;
    }
}
