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
package com.sun.jna.platform.win32;

import java.util.Arrays;

import com.sun.jna.platform.win32.WinBase.FILETIME;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import junit.framework.TestCase;

public class MsiTest extends TestCase {
    public static void main(String[] args) {
        junit.textui.TestRunner.run(MsiTest.class);
    }

    public void testMsiEnumComponents() {
        char[] componentBuffer = new char[40];
        assertEquals("MsiEnumComponents", W32Errors.ERROR_SUCCESS, Msi.INSTANCE.MsiEnumComponents(new WinDef.DWORD(0), componentBuffer));
        String component = new String(componentBuffer).trim();
        assertFalse("Component is empty", component.isEmpty());
    }

    public void testMsiGetProductCodeW() {
        char[] componentBuffer = new char[40];
        assertEquals("MsiEnumComponents", W32Errors.ERROR_SUCCESS, Msi.INSTANCE.MsiEnumComponents(new WinDef.DWORD(0), componentBuffer));
        String component = new String(componentBuffer).trim();
        assertFalse("Component is empty", component.isEmpty());

        char[] productBuffer = new char[40];
        assertEquals("MsiGetProductCode", W32Errors.ERROR_SUCCESS, Msi.INSTANCE.MsiGetProductCode(component, productBuffer));

        String product = new String(productBuffer).trim();
        assertFalse("Product is empty", product.isEmpty());
    }

    public void testMsiLocateComponentW() {
        char[] componentBuffer = new char[40];
        assertEquals("MsiEnumComponents", W32Errors.ERROR_SUCCESS, Msi.INSTANCE.MsiEnumComponents(new WinDef.DWORD(0), componentBuffer));
        String component = new String(componentBuffer).trim();
        assertFalse("Component is empty", component.isEmpty());

        char[] pathBuffer = new char[WinDef.MAX_PATH];
        IntByReference pathBufferSize = new IntByReference(pathBuffer.length);
        int installState = Msi.INSTANCE.MsiLocateComponent(component, pathBuffer, pathBufferSize);
        assertTrue(String.format("MsiLocateComponent returned %d for component %s", installState, component),
                Arrays.asList(Msi.INSTALLSTATE_LOCAL, Msi.INSTALLSTATE_SOURCE).contains(installState));

        String path = new String(pathBuffer, 0, pathBufferSize.getValue()).trim();
        assertFalse("Path is empty", path.isEmpty());
    }

    public void testMsiGetComponentPathW() {
        char[] componentBuffer = new char[40];
        assertEquals("MsiEnumComponents", W32Errors.ERROR_SUCCESS, Msi.INSTANCE.MsiEnumComponents(new WinDef.DWORD(0), componentBuffer));
        String component = new String(componentBuffer).trim();
        assertFalse("Component is empty", component.isEmpty());

        char[] productBuffer = new char[40];
        assertEquals("MsiGetProductCode", W32Errors.ERROR_SUCCESS,  Msi.INSTANCE.MsiGetProductCode(component, productBuffer));

        String product = new String(productBuffer).trim();
        assertFalse("Product is empty", product.isEmpty());

        char[] pathBuffer = new char[WinDef.MAX_PATH];
        IntByReference pathBufferSize = new IntByReference(pathBuffer.length);
        int installState = Msi.INSTANCE.MsiGetComponentPath(product, component, pathBuffer, pathBufferSize);
        assertTrue(String.format("MsiGetComponentPath returned %d for component %s in product %s", installState, component, product),
                Arrays.asList(Msi.INSTALLSTATE_LOCAL, Msi.INSTALLSTATE_SOURCE).contains(installState));

        String path = new String(pathBuffer, 0, pathBufferSize.getValue()).trim();
        assertFalse("Path is empty", path.isEmpty());
    }

    public void testMsiOpenDatabaseW() {
        PointerByReference phDatabase = new PointerByReference();
        assertEquals(WinError.ERROR_INVALID_PARAMETER, Msi.INSTANCE.MsiOpenDatabase("", Msi.MSIDBOPEN_READONLY, phDatabase));
    }

    public void testMsiCloseHandle() {
        PointerByReference handle = new PointerByReference();
        assertEquals(WinError.ERROR_INVALID_HANDLE, Msi.INSTANCE.MsiCloseHandle(handle.getPointer()));
    }

    public void testMsiDatabaseOpenViewW() {
        PointerByReference hDatabase = new PointerByReference();
        PointerByReference phView = new PointerByReference();
        assertEquals(WinError.ERROR_INVALID_HANDLE, Msi.INSTANCE.MsiDatabaseOpenView(hDatabase.getPointer(), "", phView));
    }

    public void testMsiRecordGetStringW() {
        PointerByReference hRecord = new PointerByReference();
        IntByReference pcchValueBuf = new IntByReference();
        char[] szValueBuf = new char[40];
        pcchValueBuf.setValue(40);
        assertEquals(WinError.ERROR_INVALID_HANDLE, Msi.INSTANCE.MsiRecordGetString(hRecord.getPointer(), 0, szValueBuf, pcchValueBuf));
    }

    public void testMsiViewFetch() {
        PointerByReference hView = new PointerByReference();
        PointerByReference phRecord = new PointerByReference();
        assertEquals(WinError.ERROR_INVALID_HANDLE, Msi.INSTANCE.MsiViewFetch(hView.getPointer(), phRecord));
    }


    public void testMsiViewExecute() {
        PointerByReference hView = new PointerByReference();
        PointerByReference hRecord = new PointerByReference();
        assertEquals(WinError.ERROR_INVALID_HANDLE, Msi.INSTANCE.MsiViewExecute(hView.getPointer(), hRecord.getPointer()));
    }

    public void testMsiGetSummaryInformationW() {
        PointerByReference hDatabase = new PointerByReference();
        PointerByReference phSummaryInfo = new PointerByReference();
        assertEquals(WinError.ERROR_INVALID_HANDLE, Msi.INSTANCE.MsiGetSummaryInformation(hDatabase.getPointer(), "", 0, phSummaryInfo));
    }

    public void testMsiSummaryInfoGetProperty() {
        PointerByReference hSummaryInfo = new PointerByReference();
        IntByReference puiDataType = new IntByReference();
        IntByReference piValue = new IntByReference();
        FILETIME pftValue = new FILETIME();
        char[] szValueBuf = new char[40];
        IntByReference pcchValueBuf = new IntByReference();
        pcchValueBuf.setValue(40);
        assertEquals(WinError.ERROR_INVALID_HANDLE, Msi.INSTANCE.MsiSummaryInfoGetProperty(hSummaryInfo.getPointer(), 7, puiDataType, piValue, pftValue, szValueBuf, pcchValueBuf));
    }
}
