package com.sun.jna;

import com.sun.jna.internal.MasterAccessor;
import junit.framework.TestCase;

import java.util.function.Supplier;

public class MasterCleanerTest extends TestCase {
    private CallbacksTest.TestLibrary lib;

    public static boolean waitFor(Supplier<Boolean> cond, long maxWaitMs) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() <= start + maxWaitMs && !cond.get()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignore) {}
        }
        return cond.get();
    }

    @Override
    protected void setUp() {
        lib = Native.load("testlib", CallbacksTest.TestLibrary.class);
    }

    @Override
    protected void tearDown() {
        lib = null;
    }

    public void testGCCallbackOnFinalize() throws Exception {
        final boolean[] latch = { false };
        Thread thread = new Thread(() -> {
            CallbacksTest.TestLibrary.VoidCallback cb = new CallbacksTest.TestLibrary.VoidCallback() {
                @Override
                public void callback() {
                    latch[0] = true;
                }
            };
            synchronized (latch) {
                lib.callVoidCallback(cb);
                latch.notifyAll();
                try {
                    latch.wait();
                } catch (InterruptedException ignore) {}
            }
        });
        thread.start();

        CallbackReference ref;
        synchronized (latch) {
            if (!latch[0]) {
                latch.wait();
            }
            assertTrue(latch[0]);
            // Assert thread is running and has registered a callback and master is running too
            assertTrue(thread.isAlive());
            assertTrue(MasterAccessor.masterIsRunning());
            assertFalse(MasterAccessor.getCleanerImpls().isEmpty());
            assertEquals(1, CallbackReference.callbackMap.size());
            ref = CallbackReference.callbackMap.values().iterator().next();
            CallbacksTest.waitForGc(new CallbacksTest.Condition<CallbackReference>(ref) {
                public boolean evaluate(CallbackReference _ref) {
                    return _ref.get() != null || CallbackReference.callbackMap.containsValue(_ref);
                }
            });
            assertNotNull("Callback GC'd prematurely", ref.get());
            assertTrue("Callback no longer in map", CallbackReference.callbackMap.containsValue(ref));
            latch.notifyAll();
        }
        thread.join();

        // thread is no longer running, dummy is collectable, master is still running
        final Pointer cbstruct = ref.cbstruct;
        assertTrue(MasterAccessor.masterIsRunning());
        CallbacksTest.waitForGc(new CallbacksTest.Condition<CallbackReference>(ref) {
            public boolean evaluate(CallbackReference _ref) {
                return _ref.get() != null || CallbackReference.callbackMap.containsValue(_ref);
            }
        });
        assertNull("Callback not GC'd", ref.get());
        assertFalse("Callback still in map", CallbackReference.callbackMap.containsValue(ref));

        assertTrue("CleanerImpl still exists",
                   waitFor(() -> MasterAccessor.getCleanerImpls().isEmpty(), 60_000)); // 1 minute

        thread = null;
        // thread is collectable -> wait until master terminates
        assertTrue("Master still running",
                   waitFor(() -> !MasterAccessor.masterIsRunning(), 60_000)); // 1 minute
    }
}
