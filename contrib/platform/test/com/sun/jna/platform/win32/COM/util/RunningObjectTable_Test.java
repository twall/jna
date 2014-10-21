/* Copyright (c) 2014 Dr David H. Akehurst, All Rights Reserved
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
package com.sun.jna.platform.win32.COM.util;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.jna.platform.win32.COM.COMException;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.COM.proxy.Factory;
import com.sun.jna.platform.win32.COM.proxy.annotation.ComInterface;
import com.sun.jna.platform.win32.COM.proxy.annotation.ComObject;
import com.sun.jna.platform.win32.COM.proxy.annotation.Method;
import com.sun.jna.platform.win32.COM.proxy.annotation.Property;
import com.sun.jna.platform.win32.Guid.IID;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.Ole32Util;
import com.sun.jna.platform.win32.OleAuto;
import com.sun.jna.platform.win32.Variant;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;

public class RunningObjectTable_Test {

	@ComInterface(iid="{00020970-0000-0000-C000-000000000046}")
	interface Application {
		@Property
		boolean getVisible();
		
		@Property
		void setVisible(boolean value);
		
		@Method
		void Quit();
	}	
	
	@ComObject(progId="Word.Application")
	interface MsWordApp extends Application {
	}
	
	MsWordApp msWord;
	
	@Before
	public void before() {
		Factory.initializeThreadForComAccess();
		
		this.msWord = Factory.createObject(MsWordApp.class);
		msWord.setVisible(true);
	}
	
	@After
	public void after() {
		this.msWord.Quit();
		
		Factory.releaseThreadFromComAccess();
	}
	
	@Test
	public void getRunningObjectTable() {
		IRunningObjectTable rot = RunningObjectTable.getRunningObjectTable();

		assertNotNull(rot);
	}

	@Test
	public void enumRunning() {
		IRunningObjectTable rot = RunningObjectTable.getRunningObjectTable();

		for(IUnknown obj: rot.enumRunning()) {
			try {
				Application msw = obj.queryInterface(Application.class);
			} catch(COMException ex) {
				int i= 0;
			}
		}
	}
	
	@Test
	public void getActiveObjectsByInterface() {
		IRunningObjectTable rot = RunningObjectTable.getRunningObjectTable();
		
		List<Application> objs = rot.getActiveObjectsByInterface(Application.class);
		assertTrue(objs.size() > 0);
		
		for(Application dobj: objs) {
			msWord.setVisible(true);
			boolean v2 = dobj.getVisible();
			assertEquals(true, v2);
		}
		
	}
}
