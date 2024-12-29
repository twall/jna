/* Copyright (c) 2024 Peter Conrad, All Rights Reserved
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
package com.sun.jna.internal;

import java.util.Set;

public class MasterAccessor {
    public static synchronized boolean masterIsRunning() { // synchronized is for memory synch not mutex
        return Cleaner.MasterCleaner.INSTANCE != null;
    }

    public static synchronized Set<?> getCleanerImpls() { // synchronized is for memory synch not mutex
        Cleaner.MasterCleaner mc = Cleaner.MasterCleaner.INSTANCE;
        return mc == null ? null : mc.cleaners;
    }
}
