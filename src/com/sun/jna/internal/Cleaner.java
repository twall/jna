/* Copyright (c) 2021, Matthias Bläsing, All Rights Reserved
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

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implement ReferenceQueue based cleanup of resources associated with GCed
 * objects. It replaces the {@code Object#finalize} based resource deallocation
 * that is deprecated for removal from the JDK.
 *
 * <p><strong>This class is intented to be used only be JNA itself.</strong></p>
 */
public class Cleaner {
    private static final Cleaner INSTANCE = new Cleaner();

    public static Cleaner getCleaner() {
        return INSTANCE;
    }

    // Guard for trackedObjects and cleanerThread. The readlock is utilized when
    // the trackedObjects are manipulated, the writelock protectes starting and
    // stopping the CleanerThread
    private final ReadWriteLock cleanerThreadLock = new ReentrantReadWriteLock();
    private final ReferenceQueue<Object> referenceQueue;
    // Count of objects tracked by the cleaner. When >0 it means objects are
    // being tracked by the cleaner and the cleanerThread must be running
    private final AtomicLong trackedObjects = new AtomicLong();
    // Map only serves as holder, so that the CleanerRefs stay hard referenced
    // and quickly accessible for removal
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<CleanerRef,CleanerRef> map = new ConcurrentHashMap<>();
    // Thread to handle the actual cleaning
    private volatile Thread cleanerThread;

    private Cleaner() {
        referenceQueue = new ReferenceQueue<>();
    }

    @SuppressWarnings("EmptySynchronizedStatement")
    public Cleanable register(Object obj, Runnable cleanupTask) {
        // The important side effect is the PhantomReference, that is yielded
        // after the referent is GCed
        try {
            return add(new CleanerRef(this, obj, referenceQueue, cleanupTask));
        } finally {
            synchronized (obj) {
                // Used as a reachability fence for obj
                // Ensure, that add completes before obj can be collected and
                // the cleaner is run
            }
        }
    }

    private CleanerRef add(CleanerRef ref) {
        map.put(ref, ref);
        cleanerThreadLock.readLock().lock();
        try {
            long count = trackedObjects.incrementAndGet();
            if (cleanerThread == null && count > 0) {
                cleanerThreadLock.readLock().unlock();
                cleanerThreadLock.writeLock().lock();
                try {
                    if (cleanerThread == null && trackedObjects.get() > 0) {
                        Logger.getLogger(Cleaner.class.getName()).log(Level.FINE, "Starting CleanerThread");
                        cleanerThread = new CleanerThread();
                        cleanerThread.start();
                    }
                } finally {
                    cleanerThreadLock.readLock().lock();
                    cleanerThreadLock.writeLock().unlock();
                }
            }
        } finally {
            cleanerThreadLock.readLock().unlock();
        }
        return ref;
    }

    private void remove(CleanerRef ref) {
        map.remove(ref);
        cleanerThreadLock.readLock().lock();
        try {
            trackedObjects.decrementAndGet();
        } finally {
            cleanerThreadLock.readLock().unlock();
        }
    }

    private static class CleanerRef extends PhantomReference<Object> implements Cleanable {
        private final Cleaner cleaner;
        private final Runnable cleanupTask;
        private final AtomicBoolean cleaned = new AtomicBoolean(false);

        public CleanerRef(Cleaner cleaner, Object referent, ReferenceQueue<? super Object> q, Runnable cleanupTask) {
            super(referent, q);
            this.cleaner = cleaner;
            this.cleanupTask = cleanupTask;
        }

        @Override
        public void clean() {
            if (!cleaned.getAndSet(true)) {
                cleaner.remove(this);
                cleanupTask.run();
            }
        }
    }

    public static interface Cleanable {
        public void clean();
    }

    private class CleanerThread extends Thread {

        private static final long CLEANER_LINGER_TIME = 30000;

        public CleanerThread() {
            super("JNA Cleaner");
            setDaemon(true);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Reference<? extends Object> ref = referenceQueue.remove(CLEANER_LINGER_TIME);
                    if (ref instanceof CleanerRef) {
                        ((CleanerRef) ref).clean();
                    } else if (ref == null) {
                        cleanerThreadLock.readLock().lock();
                        try {
                            if (trackedObjects.get() == 0) {
                                cleanerThreadLock.readLock().unlock();
                                cleanerThreadLock.writeLock().lock();
                                try {
                                    if (trackedObjects.get() == 0) {
                                        Logger.getLogger(Cleaner.class.getName()).log(Level.FINE, "Shutting down CleanerThread");
                                        cleanerThread = null;
                                    }
                                    break;
                                } finally {
                                    cleanerThreadLock.readLock().lock();
                                    cleanerThreadLock.writeLock().unlock();
                                }
                            }
                        } finally {
                            cleanerThreadLock.readLock().unlock();
                        }
                        Logger logger = Logger.getLogger(Cleaner.class.getName());
                        if (logger.isLoggable(Level.FINER)) {
                            logger.log(Level.FINER, "Registered Cleaners: {0}", trackedObjects.get());
                        }
                    }
                } catch (InterruptedException ex) {
                    // Can be raised on shutdown. If anyone else messes with
                    // our reference queue, well, there is no way to separate
                    // the two cases.
                    // https://groups.google.com/g/jna-users/c/j0fw96PlOpM/m/vbwNIb2pBQAJ
                    break;
                } catch (Exception ex) {
                    Logger.getLogger(Cleaner.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
