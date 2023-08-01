package com.sun.jna.different_package;

import com.sun.jna.Structure;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class CleanerTest extends TestCase {
    private static final int NUM_THREADS = 16;
    private static final long ITERATIONS = 100000;

    @Structure.FieldOrder({"bytes"})
    public static class Dummy extends Structure {
        public byte[] bytes;

        public Dummy() {}

        public Dummy(byte[] what) { bytes = what; }
    }

    private static class Allocator implements Runnable {
        @Override
        public void run() {
            for (long i = 0; i < ITERATIONS; ++i) {
                Dummy d = new Dummy(new byte[1024]);
                d.write();
            }
        }
    }

    public void testOOM() {
        List<Thread> threads = new ArrayList<>(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; ++i) {
            Thread t = new Thread(new Allocator());
            t.start();
            threads.add(t);
        }
        for (Thread t : threads) {
            while (t.isAlive()) {
                try {
                    t.join();
                } catch (InterruptedException ignore) {}
            }
        }
    }
}
