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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation.CFAllocatorRef;
import com.sun.jna.platform.mac.CoreFoundation.CFArrayRef;
import com.sun.jna.platform.mac.CoreFoundation.CFDataRef;
import com.sun.jna.platform.mac.CoreFoundation.CFIndex;
import com.sun.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFNumberRef;
import com.sun.jna.platform.mac.CoreFoundation.CFNumberType;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

public class CoreFoundationTest {

    private static final CoreFoundation CF = CoreFoundation.INSTANCE;

    @Test
    public void testCFStringRef() {
        String awesome = "ǝɯosǝʍɐ sı ∀Nſ"; // Unicode
        CFStringRef cfAwesome = CFStringRef.createCFString(awesome);
        assertEquals(awesome.length(), CF.CFStringGetLength(cfAwesome).intValue());
        assertEquals(awesome, cfAwesome.stringValue());

        Memory mem = new Memory(awesome.getBytes().length + 1);
        mem.clear();
        assertNotEquals(0,
                CF.CFStringGetCString(cfAwesome, mem, new CFIndex(mem.size()), CoreFoundation.kCFStringEncodingUTF8));
        byte[] awesomeBytes = mem.getByteArray(0, (int) mem.size() - 1);
        byte[] awesomeArr = awesome.getBytes();
        for (int i = 0; i < awesomeArr.length; i++) {
            assertEquals(awesomeArr[i], awesomeBytes[i]);
        }
        // Essentially a toString, can't rely on format but should contain the string
        CFStringRef desc = CF.CFCopyDescription(cfAwesome);
        assertTrue(desc.stringValue().contains(awesome));

        desc.release();
        cfAwesome.release();
    }

    @Test
    public void testCFNumberRef() {
        LongByReference max = new LongByReference(Long.MAX_VALUE);
        CFNumberRef cfMax = CF.CFNumberCreate(null, CFNumberType.kCFNumberLongLongType.typeIndex(), max);
        assertEquals(Long.MAX_VALUE, cfMax.longValue());
        cfMax.release();

        IntByReference zero = new IntByReference(0);
        IntByReference one = new IntByReference(1);
        CFNumberRef cfZero = CF.CFNumberCreate(null, CFNumberType.kCFNumberIntType.typeIndex(), zero);
        CFNumberRef cfOne = CF.CFNumberCreate(null, CFNumberType.kCFNumberIntType.typeIndex(), one);

        assertEquals(0, cfZero.intValue());
        assertEquals(1, cfOne.intValue());
        cfZero.release();
        cfOne.release();
    }

    @Test
    public void testCFRetainCount() {
        DoubleByReference pi = new DoubleByReference(Math.PI);
        DoubleByReference e = new DoubleByReference(Math.E);
        CFNumberRef cfE = CF.CFNumberCreate(null, CFNumberType.kCFNumberDoubleType.typeIndex(), e);
        CFNumberRef cfPi = CF.CFNumberCreate(null, CFNumberType.kCFNumberDoubleType.typeIndex(), pi);
        assertEquals(1, CF.CFGetRetainCount(cfE).intValue());
        assertEquals(1, CF.CFGetRetainCount(cfPi).intValue());
        cfE.retain();
        cfPi.retain();
        cfPi.retain();
        assertEquals(2, CF.CFGetRetainCount(cfE).intValue());
        assertEquals(3, CF.CFGetRetainCount(cfPi).intValue());

        List<CFTypeRef> irrationalReferences = new ArrayList<>();
        irrationalReferences.add(cfE);
        irrationalReferences.add(cfPi);
        for (CFTypeRef value : irrationalReferences) {
            value.release();
        }

        assertEquals(1, CF.CFGetRetainCount(cfE).intValue());
        assertEquals(2, CF.CFGetRetainCount(cfPi).intValue());
        cfPi.release();
        assertEquals(1, CF.CFGetRetainCount(cfPi).intValue());
        cfE.release();
        cfPi.release();
    }

    @Test
    public void testCFArray() {
        CFNumberRef[] refArray = new CFNumberRef[3];
        int size = Native.getNativeSize(CFNumberRef.class);
        Memory contiguousArray = new Memory(size * refArray.length);
        for (int i = 0; i < refArray.length; i++) {
            refArray[i] = CF.CFNumberCreate(null, CoreFoundation.CFNumberType.kCFNumberIntType.typeIndex(),
                    new IntByReference(i));
            contiguousArray.setPointer(i * size, refArray[i].getPointer());
        }
        CFArrayRef cfPtrArray = CF.CFArrayCreate(null, contiguousArray, new CFIndex(refArray.length), null);

        assertEquals(refArray.length, CF.CFArrayGetCount(cfPtrArray).intValue());
        for (int i = 0; i < refArray.length; i++) {
            Pointer result = CF.CFArrayGetValueAtIndex(cfPtrArray, new CFIndex(i));
            CFNumberRef numRef = new CFNumberRef(result);
            assertEquals(i, numRef.intValue());
        }

        for (int i = 0; i < refArray.length; i++) {
            refArray[i].release();
        }
        cfPtrArray.release();
    }

    @Test
    public void testCFData() {
        int size = 128;
        // Create some random bytes
        byte[] randomBytes = new byte[size];
        new Random().nextBytes(randomBytes);
        // Fill native memory with them
        Memory nativeBytes = new Memory(size);
        for (int i = 0; i < size; i++) {
            nativeBytes.setByte(i, randomBytes[i]);
        }
        // Create a CF reference to the data
        CFDataRef cfData = CF.CFDataCreate(null, nativeBytes, new CFIndex(size));
        long dataSize = CF.CFDataGetLength(cfData).intValue();
        assertEquals(size, dataSize);
        // Read it back out and convert to an array
        Pointer bytes = CF.CFDataGetBytePtr(cfData);
        byte[] dataBytes = bytes.getByteArray(0, (int) dataSize);
        assertArrayEquals(randomBytes, dataBytes);
        cfData.release();
    }

    @Test
    public void testCFDictionary() {
        CFAllocatorRef alloc = CF.CFAllocatorGetDefault();
        CFMutableDictionaryRef dict = CF.CFDictionaryCreateMutable(alloc, new CFIndex(2), null, null);
        CFStringRef oneStr = CFStringRef.createCFString("one");

        // Key does not exist, returns null
        assertEquals(0, CF.CFDictionaryGetValueIfPresent(dict, oneStr, null));
        Pointer cfNull = CF.CFDictionaryGetValue(dict, oneStr);
        assertNull(cfNull);

        // Store and retrieve null value
        CF.CFDictionarySetValue(dict, oneStr, null);
        assertNotEquals(0, CF.CFDictionaryGetValueIfPresent(dict, oneStr, null));
        Pointer cfNullValue = CF.CFDictionaryGetValue(dict, oneStr);
        assertNull(cfNullValue);

        // Store (replace the null) and retrieve integer value
        IntByReference one = new IntByReference(1);
        CFNumberRef cfOne = CF.CFNumberCreate(null, CFNumberType.kCFNumberIntType.typeIndex(), one);
        CF.CFDictionarySetValue(dict, oneStr, cfOne);

        assertNotEquals(0, CF.CFDictionaryGetValueIfPresent(dict, oneStr, null));
        Pointer result = CF.CFDictionaryGetValue(dict, oneStr);
        CFNumberRef numRef = new CFNumberRef(result);
        assertEquals(1, numRef.intValue());

        PointerByReference resultPtr = new PointerByReference();
        assertNotEquals(0, CF.CFDictionaryGetValueIfPresent(dict, oneStr, resultPtr));
        numRef = new CFNumberRef(resultPtr.getValue());
        assertEquals(1, numRef.intValue());

        // Test non-CF type as key
        IntByReference onePtr = new IntByReference(1);
        CF.CFDictionarySetValue(dict, onePtr, oneStr);
        result = CF.CFDictionaryGetValue(dict, onePtr);
        CFStringRef strRef = new CFStringRef(result);
        assertEquals("one", strRef.stringValue());

        oneStr.release();
        cfOne.release();
        dict.release();
    }
}
