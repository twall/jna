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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implement ReferenceQueue based cleanup of resources associated with GCed
 * objects. It replaces the {@code Object#finalize} based resource deallocation
 * that is deprecated for removal from the JDK.
 *
 * <p><strong>This class is intended to be used only be JNA itself.</strong></p>
 */
public class Cleaner {
    private static final Logger LOG = Logger.getLogger(Cleaner.class.getName());

    /* General idea:
     *
     * There's one Cleaner per thread, kept in a ThreadLocal static variable.
     * This instance handles all to-be-cleaned objects registered by this
     * thread. Whenever the thread registers another object, it first checks
     * if there are references in the queue and cleans them up, then continues
     * with the registration.
     *
     * This leaves two cases open, for which we employ a "Master Cleaner" and
     * a separate cleaning thread.
     * 1. If a long-lived thread registers some objects in the beginning, but
     *    then stops registering more objects, the previously registered
     *    objects will never be cleared.
     * 2. When a thread exits before all its registered objects have been
     *    cleared, the ThreadLocal instance is lost, and so are the pending
     *    objects.
     *
     * The Master Cleaner handles the first issue by regularly checking the
     * activity of the Cleaners registered with it, and taking over the queues
     * of any cleaners appearing to be idle.
     * Similarly, the second issue is handled by taking over the queues of threads
     * that have terminated.
     */

    public static final long MASTER_CLEANUP_INTERVAL_MS = 5000;
    public static final long MASTER_MAX_LINGER_MS = 30000;

    private static class CleanerImpl {
        protected final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<Object>();
        protected final Map<Long,CleanerRef> cleanables = new ConcurrentHashMap<Long,CleanerRef>();
        private final AtomicBoolean lock = new AtomicBoolean(false);

        private void cleanQueue() {
            if (lock.compareAndSet(false, true)) {
                try {
                    Reference<?> ref;
                    while ((ref = referenceQueue.poll()) != null) {
                        try {
                            if (ref instanceof Cleanable) {
                                ((Cleanable) ref).clean();
                            }
                        } catch (RuntimeException ex) {
                            Logger.getLogger(Cleaner.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } finally {
                    lock.set(false);
                }
            }
        }

        public Cleanable register(Object obj, Runnable cleanupTask) {
            cleanQueue();
            // The important side effect is the PhantomReference, that is yielded
            // after the referent is GCed
            return new CleanerRef(this, obj, referenceQueue, cleanupTask);
        }

        protected void put(long n, CleanerRef ref) {
            cleanables.put(n, ref);
        }

        protected boolean remove(long n) {
            return cleanables.remove(n) != null;
        }
    }

    static class MasterCleaner extends Cleaner {
        static MasterCleaner INSTANCE;

        public static synchronized void add(Cleaner cleaner) {
            if (INSTANCE == null) {
                INSTANCE = new MasterCleaner();
            }
            INSTANCE.cleaners.add(cleaner);
        }

        /** @return true if the caller thread can terminate */
        private static synchronized boolean deleteIfEmpty(MasterCleaner caller) {
            if (INSTANCE == caller && INSTANCE.cleaners.isEmpty()) {
                INSTANCE = null;
            }
            return caller.cleaners.isEmpty();
        }

        /* The lifecycle of a Cleaner instance consists of four phases:
         * 1. New instances are contained in Cleaner.INSTANCES and added to a MasterCleaner.cleaners set.
         * 2. At some point, the master cleaner takes control of the instance by removing it
         *    from Cleaner.INSTANCES and MasterCleaner.cleaners, and then adding it to its
         *    referencedCleaners and watchedCleaners sets. Note that while it is no longer
         *    in Cleaner.INSTANCES, a thread may still be holding a reference to it.
         * 3. Possibly some time later, the last reference to the cleaner instance is dropped and
         *    it is GC'd. It is then also removed from referencedCleaners but remains in watchedCleaners.
         * 4. The master cleaner continues to monitor the watchedCleaners until they are empty and no
         *    longer referenced. At that point they are also removed from watchedCleaners.
         */
        final Set<Cleaner> cleaners = Collections.synchronizedSet(new HashSet<>());
        final Set<CleanerImpl> referencedCleaners = new HashSet<>();
        final Set<CleanerImpl> watchedCleaners = new HashSet<>();

        private MasterCleaner() {
            Thread cleanerThread = new Thread(() -> {
                long lastNonEmpty = System.currentTimeMillis();
                long now;
                long lastMasterRun = 0;
                while ((now = System.currentTimeMillis()) < lastNonEmpty + MASTER_MAX_LINGER_MS || !deleteIfEmpty(MasterCleaner.this)) {
                    if (!cleaners.isEmpty()) { lastNonEmpty = now; }
                    try {
                        Reference<?> ref = impl.referenceQueue.remove(MASTER_CLEANUP_INTERVAL_MS);
                        if(ref instanceof CleanerRef) {
                            ((CleanerRef) ref).clean();
                        }
                        // "now" is not really *now* at this point, but off by no more than MASTER_CLEANUP_INTERVAL_MS
                        if (lastMasterRun + MASTER_CLEANUP_INTERVAL_MS <= now) {
                            masterCleanup();
                            lastMasterRun = now;
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
                LOG.log(Level.FINE, "MasterCleaner thread {0} exiting", Thread.currentThread());
            }, "JNA Cleaner");
            LOG.log(Level.FINE, "Starting new MasterCleaner thread {0}", cleanerThread);
            cleanerThread.setDaemon(true);
            cleanerThread.start();
        }

        private void masterCleanup() {
            for (Iterator<Map.Entry<Thread,Cleaner>> it = Cleaner.INSTANCES.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Thread,Cleaner> entry = it.next();
                if (!cleaners.contains(entry.getValue())) { continue; }
                Cleaner cleaner = entry.getValue();
                long currentCount = cleaner.counter.get();
                if (currentCount == cleaner.lastCount // no new cleanables registered since last master cleanup interval -> assume it is no longer in use
                        || !entry.getKey().isAlive()) { // owning thread died -> assume it is no longer in use
                    it.remove();
                    CleanerImpl impl = cleaner.impl;
                    LOG.log(Level.FINE, () -> "MasterCleaner stealing cleaner " + impl + " from thread " + entry.getKey());
                    referencedCleaners.add(impl);
                    watchedCleaners.add(impl);
                    register(cleaner, () -> {
                        referencedCleaners.remove(impl);
                        LOG.log(Level.FINE, "Cleaner {0} no longer referenced", impl);
                    });
                    cleaners.remove(cleaner);
                } else {
                    cleaner.lastCount = currentCount;
                }
            }

            for (Iterator<CleanerImpl> it = watchedCleaners.iterator(); it.hasNext(); ) {
                CleanerImpl impl = it.next();
                impl.cleanQueue();
                if (!referencedCleaners.contains(impl)) {
                    if (impl.cleanables.isEmpty()) {
                        it.remove();
                        LOG.log(Level.FINE, "Discarding empty Cleaner {0}", impl);
                    }
                }
            }
        }
    }

    private static final Map<Thread,Cleaner> INSTANCES = new ConcurrentHashMap<>();

    public static Cleaner getCleaner() {
        return INSTANCES.computeIfAbsent(Thread.currentThread(), Cleaner::new);
    }

    protected final CleanerImpl impl;
    protected final Thread owner;
    protected final AtomicLong counter = new AtomicLong(Long.MIN_VALUE);
    protected long lastCount; // used by MasterCleaner only

    private Cleaner() {
        this(null);
    }

    private Cleaner(Thread owner) {
        impl = new CleanerImpl();
        this.owner = owner;
        if (owner != null) {
            MasterCleaner.add(this);
        }
        LOG.log(Level.FINE, () -> owner == null ? "Created new MasterCleaner"
                                                : "Created new Cleaner " + impl + " for thread " + owner);
    }

    public Cleanable register(Object obj, Runnable cleanupTask) {
        counter.incrementAndGet();
        return impl.register(obj, cleanupTask);
    }

    private static class CleanerRef extends PhantomReference<Object> implements Cleanable {
        private static final AtomicLong COUNTER = new AtomicLong(Long.MIN_VALUE);

        private final CleanerImpl cleaner;
        private final long number = COUNTER.incrementAndGet();
        private Runnable cleanupTask;

        public CleanerRef(CleanerImpl impl, Object referent, ReferenceQueue<Object> q, Runnable cleanupTask) {
            super(referent, q);
            LOG.log(Level.FINER, () -> "Registering " + referent + " with " + impl + " as " + this);
            this.cleaner = impl;
            this.cleanupTask = cleanupTask;
            cleaner.put(number, this);
        }

        @Override
        public void clean() {
            if(cleaner.remove(this.number) && cleanupTask != null) {
                LOG.log(Level.FINER, "Cleaning up {0}", this);
                cleanupTask.run();
                cleanupTask = null;
            }
        }
    }

    public interface Cleanable {
        void clean();
    }
}
