/* Copyright (c) 2010, 2013 Daniel Doubrovkine, Markus Karg, All Rights Reserved
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.  
 */
package com.sun.jna.platform.win32;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.ShellAPI.APPBARDATA;
import com.sun.jna.platform.win32.ShellAPI.SHELLEXECUTEINFO;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.UINT_PTR;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.PointerByReference;

import junit.framework.TestCase;


/**
 * @author dblock[at]dblock[dot]org
 * @author markus[at]headcrashing[dot]eu
 */
public class Shell32Test extends TestCase {

    private static final int RESIZE_HEIGHT = 500;
    private static final int WM_USER = 0x0400;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Shell32Test.class);
    }

    public void testSHGetFolderPath() {
    	char[] pszPath = new char[WinDef.MAX_PATH];
    	assertEquals(W32Errors.S_OK, Shell32.INSTANCE.SHGetFolderPath(null, 
    			ShlObj.CSIDL_PROGRAM_FILES, null, ShlObj.SHGFP_TYPE_CURRENT, 
    			pszPath));
    	assertTrue(Native.toString(pszPath).length() > 0);
    }

    public void testSHGetDesktopFolder() {
        PointerByReference ppshf = new PointerByReference();
        WinNT.HRESULT hr = Shell32.INSTANCE.SHGetDesktopFolder(ppshf);
        assertTrue(W32Errors.SUCCEEDED(hr.intValue()));
        assertTrue(ppshf.getValue() != null);
        // should release the interface, but we need Com4JNA to do that.
    }

    public final void testSHGetSpecialFolderPath() {
        final char[] pszPath = new char[WinDef.MAX_PATH];
        assertTrue(Shell32.INSTANCE.SHGetSpecialFolderPath(null, pszPath, ShlObj.CSIDL_APPDATA, false));
        assertFalse(Native.toString(pszPath).isEmpty());
    }

    
    private void newAppBar() {
        APPBARDATA data = new APPBARDATA.ByReference();
        data.cbSize.setValue(data.size());
        data.uCallbackMessage.setValue(WM_USER + 1);

        UINT_PTR result = Shell32.INSTANCE.SHAppBarMessage(new DWORD(ShellAPI.ABM_NEW), data);
        assertNotNull(result);
    }

    private void removeAppBar() {
        APPBARDATA data = new APPBARDATA.ByReference();
        data.cbSize.setValue(data.size());
        UINT_PTR result = Shell32.INSTANCE.SHAppBarMessage(new DWORD(ShellAPI.ABM_REMOVE), data);
        assertNotNull(result);

    }

    private void queryPos(APPBARDATA data) {
        UINT_PTR h = Shell32.INSTANCE.SHAppBarMessage(new DWORD(ShellAPI.ABM_QUERYPOS), data);

        assertNotNull(h);
        assertTrue(h.intValue() > 0);

    }

    public void testResizeDesktopFromBottom() throws InterruptedException {

        newAppBar();

        APPBARDATA data = new APPBARDATA.ByReference();

        data.uEdge.setValue(ShellAPI.ABE_BOTTOM);
        data.rc.top = User32.INSTANCE.GetSystemMetrics(User32.SM_CYFULLSCREEN) - RESIZE_HEIGHT;
        data.rc.left = 0;
        data.rc.bottom = User32.INSTANCE.GetSystemMetrics(User32.SM_CYFULLSCREEN);
        data.rc.right = User32.INSTANCE.GetSystemMetrics(User32.SM_CXFULLSCREEN);

        queryPos(data);

        UINT_PTR h = Shell32.INSTANCE.SHAppBarMessage(new DWORD(ShellAPI.ABM_SETPOS), data);

        assertNotNull(h);
        assertTrue(h.intValue() >= 0);

        removeAppBar();
    }

    public void testResizeDesktopFromTop() throws InterruptedException {
        
        newAppBar();

        APPBARDATA data = new APPBARDATA.ByReference();
        data.uEdge.setValue(ShellAPI.ABE_TOP);
        data.rc.top = 0;
        data.rc.left = 0;
        data.rc.bottom = RESIZE_HEIGHT;
        data.rc.right = User32.INSTANCE.GetSystemMetrics(User32.SM_CXFULLSCREEN);

        queryPos(data);

        UINT_PTR h = Shell32.INSTANCE.SHAppBarMessage(new DWORD(ShellAPI.ABM_SETPOS), data);

        assertNotNull(h);
        assertTrue(h.intValue() >= 0);

        removeAppBar();

    }

	public void testSHGetKnownFolderPath() {
		int flags = ShlObj.KNOWN_FOLDER_FLAG.NONE.getFlag();
		PointerByReference outPath = new PointerByReference();
		HANDLE token = null;
		GUID guid = KnownFolders.FOLDERID_Fonts;
		HRESULT hr = Shell32.INSTANCE.SHGetKnownFolderPath(guid, flags, token, outPath);

		Ole32.INSTANCE.CoTaskMemFree(outPath.getValue());

		assertTrue(W32Errors.SUCCEEDED(hr.intValue()));
	}

	public void testSHEmptyRecycleBin() {
		File file = new File(System.getProperty("java.io.tmpdir") + System.nanoTime() + ".txt");
		try {
			// Create a file and immediately send it to the recycle bin.
			try {
				fillTempFile(file);
				W32FileUtils.getInstance().moveToTrash(new File[] { file });
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			int result = Shell32.INSTANCE.SHEmptyRecycleBin(null, null,
					Shell32.SHERB_NOCONFIRMATION | Shell32.SHERB_NOPROGRESSUI | Shell32.SHERB_NOSOUND);
			// for reasons I can not find documented, the function returns the
			// following:
			// -2147418113 when the recycle bin has no items in it
			// 0 when the recycle bin has items in it
			assertTrue("Result should have been 0 when emptying Recycle Bin - there should have been a file in it.",
					result == 0);
		} finally {
			// if the file wasn't sent to the recycle bin, delete it.
			if (file.exists()) {
				file.delete();
			}
		}
	}

	public void testShellExecuteEx() {
		File file = new File(System.getProperty("java.io.tmpdir") + System.nanoTime() + ".txt");
		try {
			try {
				fillTempFile(file);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			SHELLEXECUTEINFO lpExecInfo = new SHELLEXECUTEINFO();
			// to avoid opening something and having hProcess come up null
			// (meaning we opened something but can't close it)
			// we will do a negative test with a bogus action.
			lpExecInfo.lpVerb = new WString("0p3n");
			lpExecInfo.nShow = User32.SW_SHOWDEFAULT;
			lpExecInfo.fMask = Shell32.SEE_MASK_NOCLOSEPROCESS | Shell32.SEE_MASK_FLAG_NO_UI;
			lpExecInfo.lpFile = new WString(file.getAbsolutePath());

			assertFalse("ShellExecuteEx should have returned false - action verb was bogus.",
					Shell32.INSTANCE.ShellExecuteEx(lpExecInfo));
			assertTrue("GetLastError() should have been set to ERROR_NO_ASSOCIATION because of bogus action",
					Native.getLastError() == W32Errors.ERROR_NO_ASSOCIATION);
		} finally {
			if (file.exists()) {
				file.delete();
			}
		}

	}

	/**
	 * Creates (if needed) and fills the specified file with some content
	 * 
	 * @param file
	 *            The file to fill with content
	 * @throws IOException
	 *             If writing the content fails
	 */
	private void fillTempFile(File file) throws IOException {
		file.createNewFile();
		FileWriter fileWriter = new FileWriter(file);
		for (int i = 0; i < 10; i++) {
			fileWriter.write("Sample text " + i + System.getProperty("line.separator"));
		}
		fileWriter.close();
	}
}
