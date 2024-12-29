/* Copyright (c) 2007-2013 Timothy Wall, All Rights Reserved
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
package com.sun.jna;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.jna.Callback.UncaughtExceptionHandler;
import com.sun.jna.CallbacksTest.TestLibrary.CbCallback;
import com.sun.jna.internal.Cleaner;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.W32APIOptions;

import junit.framework.TestCase;

/** Exercise callback-related functionality.
 *
 * @author twall@users.sf.net
 */
//@SuppressWarnings("unused")
public class CallbacksTest extends TestCase implements Paths {

    // On OSX, on Oracle JVM 1.8+, pthread cleanup thinks the native thread is
    // not attached, and the JVM never unmaps the defunct native thread.  In
    // order to avoid this situation causing tests to time out, we need to
    // explicitly detach the native thread after our Java code is done with it.
    // Also reproducible on Ubuntu 6 (x86-64), Java 6
    private static final boolean THREAD_DETACH_BUG = Platform.isMac() || (Platform.isLinux() && Platform.is64Bit());

    private static final String UNICODE = "[\u0444]";

    private static final double DOUBLE_MAGIC = -118.625d;
    private static final float FLOAT_MAGIC = -118.625f;

    private static final int THREAD_TIMEOUT = 5000;

    protected void waitFor(Thread thread) {
        long start = System.currentTimeMillis();
        while (thread.isAlive()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
            if (System.currentTimeMillis() - start > THREAD_TIMEOUT) {
                fail("Timed out waiting for native thread " + thread
                        + " to detach and terminate");
            }
        }
    }

    public static abstract class Condition<T> {
        protected final T obj;

        public Condition(T t) { obj = t; }

        public boolean evaluate() { return evaluate(obj); }

        abstract boolean evaluate(T t);
    }
    public static void waitForGc(Condition<?> condition) throws Exception {
        for (int i = 0; i < 2 * Cleaner.MASTER_CLEANUP_INTERVAL_MS / 10 + 5 && condition.evaluate(); ++i) {
            synchronized (CallbacksTest.class) { // cleanups happen in a different thread, make sure we see it here
                System.gc();
            }
            Thread.sleep(10); // Give the GC a chance to run
        }
    }

    public static class SmallTestStructure extends Structure {
        public static final List<String> FIELDS = createFieldsOrder("value");
        public double value;
        public static int allocations = 0;
        @Override
        protected void allocateMemory(int size) {
            super.allocateMemory(size);
            ++allocations;
        }
        public SmallTestStructure() { }
        public SmallTestStructure(Pointer p) { super(p); read(); }
        @Override
        protected List<String> getFieldOrder() {
            return FIELDS;
        }
    }
    public static class TestStructure extends Structure {
        public static class ByValue extends TestStructure implements Structure.ByValue { }
        public static interface TestCallback extends Callback {
            TestStructure.ByValue callback(TestStructure.ByValue s);
        }

        public static final List<String> FIELDS = createFieldsOrder("c", "s", "i", "j", "inner");
        public byte c;
        public short s;
        public int i;
        public long j;
        public SmallTestStructure inner;
        @Override
        protected List<String> getFieldOrder() {
            return FIELDS;
        }
    }
    public static interface TestLibrary extends Library {
        interface NoMethodCallback extends Callback {
        }
        interface CustomMethodCallback extends Callback {
            void invoke();
        }
        interface TooManyMethodsCallback extends Callback {
            void invoke();
            void invoke2();
        }
        interface MultipleMethodsCallback extends Callback {
            void invoke();
            void callback();
        }
        interface VoidCallback extends Callback {
            void callback();
        }
        void callVoidCallback(VoidCallback c);
        void callVoidCallbackThreaded(VoidCallback c, int count, int ms, String name, int stacksize);
        interface VoidCallbackCustom extends Callback {
            void customMethodName();
        }
        abstract class VoidCallbackCustomAbstract implements VoidCallbackCustom {
            @Override
            public void customMethodName() { }
        }
        class VoidCallbackCustomDerived extends VoidCallbackCustomAbstract { }
        void callVoidCallback(VoidCallbackCustom c);
        interface BooleanCallback extends Callback {
            boolean callback(boolean arg, boolean arg2);
        }
        boolean callBooleanCallback(BooleanCallback c, boolean arg, boolean arg2);
        interface ByteCallback extends Callback {
            byte callback(byte arg, byte arg2);
        }
        byte callInt8Callback(ByteCallback c, byte arg, byte arg2);
        interface ShortCallback extends Callback {
            short callback(short arg, short arg2);
        }
        short callInt16Callback(ShortCallback c, short arg, short arg2);
        interface Int32Callback extends Callback {
            int callback(int arg, int arg2);
        }
        int callInt32Callback(Int32Callback c, int arg, int arg2);
        interface NativeLongCallback extends Callback {
            NativeLong callback(NativeLong arg, NativeLong arg2);
        }
        NativeLong callNativeLongCallback(NativeLongCallback c, NativeLong arg, NativeLong arg2);
        interface Int64Callback extends Callback {
            long callback(long arg, long arg2);
        }
        long callInt64Callback(Int64Callback c, long arg, long arg2);
        interface FloatCallback extends Callback {
            float callback(float arg, float arg2);
        }
        float callFloatCallback(FloatCallback c, float arg, float arg2);
        interface DoubleCallback extends Callback {
            double callback(double arg, double arg2);
        }
        double callDoubleCallback(DoubleCallback c, double arg, double arg2);
        interface StructureCallback extends Callback {
            SmallTestStructure callback(SmallTestStructure arg);
        }
        SmallTestStructure callStructureCallback(StructureCallback c, SmallTestStructure arg);
        interface StringCallback extends Callback {
            String callback(String arg, String arg2);
        }
        String callStringCallback(StringCallback c, String arg, String arg2);
        interface WideStringCallback extends Callback {
            WString callback(WString arg, WString arg2);
        }
        WString callWideStringCallback(WideStringCallback c, WString arg, WString arg2);
        interface CopyArgToByReference extends Callback {
            int callback(int arg, IntByReference result);
        }
        interface StringArrayCallback extends Callback {
            String[] callback(String[] arg);
        }
        Pointer callStringArrayCallback(StringArrayCallback c, String[] arg);
        int callCallbackWithByReferenceArgument(CopyArgToByReference cb, int arg, IntByReference result);
        TestStructure.ByValue callCallbackWithStructByValue(TestStructure.TestCallback callback, TestStructure.ByValue cbstruct);
        interface CbCallback extends Callback {
            CbCallback callback(CbCallback arg);
        }
        CbCallback callCallbackWithCallback(CbCallback cb);

        interface Int32CallbackX extends Callback {
            public int callback(int arg);
        }
        Int32CallbackX returnCallback();
        Int32CallbackX returnCallbackArgument(Int32CallbackX cb);

        interface CustomCallback extends Callback {
            Custom callback(Custom arg1, Custom arg2);
        }
        int callInt32Callback(CustomCallback cb, int arg1, int arg2);

        class CbStruct extends Structure {
            public Callback cb;
            @Override
            protected List<String> getFieldOrder() {
                return Arrays.asList(new String[] { "cb" });
            }
        }
        void callCallbackInStruct(CbStruct cbstruct);

        // Union (by value)
        class TestUnion extends Union implements Structure.ByValue {
            public String f1;
            public int f2;
        }
        interface UnionCallback extends Callback {
            TestUnion invoke(TestUnion arg);
        }
        TestUnion testUnionByValueCallbackArgument(UnionCallback cb, TestUnion arg);
    }

    TestLibrary lib;

    @Override
    protected void setUp() {
        lib = Native.load("testlib", TestLibrary.class);
    }

    @Override
    protected void tearDown() {
        lib = null;
    }

    public static class Custom implements NativeMapped {
        private int value;
        public Custom() { }
        public Custom(int value) {
            this.value = value;
        }
        @Override
        public Object fromNative(Object nativeValue, FromNativeContext context) {
            return new Custom(((Integer)nativeValue).intValue());
        }
        @Override
        public Class<?> nativeType() {
            return Integer.class;
        }
        @Override
        public Object toNative() {
            return Integer.valueOf(value);
        }
        @Override
        public boolean equals(Object o) {
            return o instanceof Custom && ((Custom)o).value == value;
        }
    }

    public void testLookupNullCallback() {
        assertNull("NULL pointer should result in null callback",
                   CallbackReference.getCallback(null, null));
        try {
            CallbackReference.getCallback(TestLibrary.VoidCallback.class, new Pointer(0));
            fail("Null pointer lookup should fail");
        } catch(NullPointerException e) {
            // expected
        }
    }

    public void testLookupNonCallbackClass() {
        try {
            CallbackReference.getCallback(String.class, new Pointer(0));
            fail("Request for non-Callback class should fail");
        }
        catch(IllegalArgumentException e) {
        }
    }

    public void testAcceptMultiplyMappedCallbacks() {
        Pointer p = new Pointer(getName().hashCode());

        Callback cbV1 = CallbackReference.getCallback(TestLibrary.VoidCallback.class, p);
        Callback cbB1 = CallbackReference.getCallback(TestLibrary.ByteCallback.class, p);
        Callback cbV2 = CallbackReference.getCallback(TestLibrary.VoidCallback.class, p);
        Callback cbB2 = CallbackReference.getCallback(TestLibrary.ByteCallback.class, p);

        assertSame(cbV1, cbV2);
        assertSame(cbB1, cbB2);
    }

    public void testNoMethodCallback() {
        try {
            CallbackReference.getCallback(TestLibrary.NoMethodCallback.class, new Pointer(getName().hashCode()));
            fail("Callback with no callback method should fail");
        }
        catch(IllegalArgumentException e) {
        }
    }

    public void testCustomMethodCallback() {
        CallbackReference.getCallback(TestLibrary.CustomMethodCallback.class, new Pointer(getName().hashCode()));
    }

    public void testTooManyMethodsCallback() {
        try {
            CallbackReference.getCallback(TestLibrary.TooManyMethodsCallback.class, new Pointer(getName().hashCode()));
            fail("Callback lookup with too many methods should fail");
        }
        catch(IllegalArgumentException e) {
        }
    }

    public void testMultipleMethodsCallback() {
        CallbackReference.getCallback(TestLibrary.MultipleMethodsCallback.class, new Pointer(getName().hashCode()));
    }

    public void testNativeFunctionPointerStringValue() {
        Callback cb = CallbackReference.getCallback(TestLibrary.VoidCallback.class, new Pointer(getName().hashCode()));
        Class<?> cls = CallbackReference.findCallbackClass(cb.getClass());
        assertTrue("toString should include Java Callback type: " + cb + " ("
                   + cls + ")", cb.toString().indexOf(cls.getName()) != -1);
    }

    public void testLookupSameCallback() {
        Callback cb = CallbackReference.getCallback(TestLibrary.VoidCallback.class, new Pointer(getName().hashCode()));
        Callback cb2 = CallbackReference.getCallback(TestLibrary.VoidCallback.class, new Pointer(getName().hashCode()));

        assertEquals("Callback lookups for same pointer should return same Callback object", cb, cb2);
    }

    // Allow direct tests to override
    protected Map<Callback, CallbackReference> callbackCache() {
        return CallbackReference.callbackMap;
    }

    // Fails on OpenJDK(linux/ppc), probably finalize not run
    public void testGCCallbackOnFinalize() throws Exception {
        final boolean[] called = { false };
        TestLibrary.VoidCallback cb = new TestLibrary.VoidCallback() {
            @Override
            public void callback() {
                called[0] = true;
            }
        };
        lib.callVoidCallback(cb);
        assertTrue("Callback not called", called[0]);

        assertTrue("Callback not cached", callbackCache().containsKey(cb));
        final Map<Callback, CallbackReference> refs = callbackCache();
        CallbackReference ref = refs.get(cb);
        final Pointer cbstruct = ref.cbstruct;

        cb = null;
        waitForGc(new Condition<CallbackReference>(ref) {
            public boolean evaluate(CallbackReference _ref) {
                return _ref.get() != null || refs.containsValue(_ref);
            }
        });
        assertNull("Callback not GC'd", ref.get());
        assertFalse("Callback still in map", refs.containsValue(ref));

        ref = null;
        waitForGc(new Condition<Object>(null) {
            public boolean evaluate(Object o) {
                return refs.size() > 0 || cbstruct.peer != 0;
            }
        });
        assertEquals("Callback trampoline not freed", 0, cbstruct.peer);
    }

    public void testFindCallbackInterface() {
        TestLibrary.Int32Callback cb = new TestLibrary.Int32Callback() {
            @Override
            public int callback(int arg, int arg2) {
                return arg + arg2;
            }
        };
        assertEquals("Wrong callback interface",
                     TestLibrary.Int32Callback.class,
                     CallbackReference.findCallbackClass(cb.getClass()));
    }

    public void testCallVoidCallback() {
        final boolean[] called = { false };
        TestLibrary.VoidCallback cb = new TestLibrary.VoidCallback() {
            @Override
            public void callback() {
                called[0] = true;
            }
        };
        lib.callVoidCallback(cb);
        assertTrue("Callback not called", called[0]);
    }

    public void testCallInt32Callback() {
        final int MAGIC = 0x11111111;
        final boolean[] called = { false };
        TestLibrary.Int32Callback cb = new TestLibrary.Int32Callback() {
            @Override
            public int callback(int arg, int arg2) {
                called[0] = true;
                return arg + arg2;
            }
        };
        final int EXPECTED = MAGIC*3;
        int value = lib.callInt32Callback(cb, MAGIC, MAGIC*2);
        assertTrue("Callback not called", called[0]);
        assertEquals("Wrong callback value", Integer.toHexString(EXPECTED),
                     Integer.toHexString(value));

        value = lib.callInt32Callback(cb, -1, -2);
        assertEquals("Wrong callback return", -3, value);
    }

    public void testCallInt64Callback() {
        final long MAGIC = 0x1111111111111111L;
        final boolean[] called = { false };
        TestLibrary.Int64Callback cb = new TestLibrary.Int64Callback() {
            @Override
            public long callback(long arg, long arg2) {
                called[0] = true;
                return arg + arg2;
            }
        };
        final long EXPECTED = MAGIC*3;
        long value = lib.callInt64Callback(cb, MAGIC, MAGIC*2);
        assertTrue("Callback not called", called[0]);
        assertEquals("Wrong callback value", Long.toHexString(EXPECTED),
                     Long.toHexString(value));

        value = lib.callInt64Callback(cb, -1, -2);
        assertEquals("Wrong callback return", -3, value);
    }

    public void testCallFloatCallback() {
        final boolean[] called = { false };
        final float[] args = { 0, 0 };
        TestLibrary.FloatCallback cb = new TestLibrary.FloatCallback() {
            @Override
            public float callback(float arg, float arg2) {
                called[0] = true;
                args[0] = arg;
                args[1] = arg2;
                return arg + arg2;
            }
        };
        final float EXPECTED = FLOAT_MAGIC*3;
        float value = lib.callFloatCallback(cb, FLOAT_MAGIC, FLOAT_MAGIC*2);
        assertTrue("Callback not called", called[0]);
        assertEquals("Wrong first argument", FLOAT_MAGIC, args[0], 0);
        assertEquals("Wrong second argument", FLOAT_MAGIC*2, args[1], 0);
        assertEquals("Wrong callback value", EXPECTED, value, 0);

        value = lib.callFloatCallback(cb, -1f, -2f);
        assertEquals("Wrong callback return", -3f, value, 0);
    }

    public void testCallDoubleCallback() {
        final boolean[] called = { false };
        final double[] args = { 0, 0 };
        TestLibrary.DoubleCallback cb = new TestLibrary.DoubleCallback() {
            @Override
            public double callback(double arg, double arg2) {
                called[0] = true;
                args[0] = arg;
                args[1] = arg2;
                return arg + arg2;
            }
        };
        final double EXPECTED = DOUBLE_MAGIC*3;
        double value = lib.callDoubleCallback(cb, DOUBLE_MAGIC, DOUBLE_MAGIC*2);
        assertTrue("Callback not called", called[0]);
        assertEquals("Wrong first argument", DOUBLE_MAGIC, args[0], 0);
        assertEquals("Wrong second argument", DOUBLE_MAGIC*2, args[1], 0);
        assertEquals("Wrong callback value", EXPECTED, value, 0);

        value = lib.callDoubleCallback(cb, -1d, -2d);
        assertEquals("Wrong callback return", -3d, value, 0);
    }

    public void testCallStructureCallback() {
        final boolean[] called = {false};
        final Pointer[] cbarg = { null };
        final SmallTestStructure s = new SmallTestStructure();
        final double MAGIC = 118.625;
        TestLibrary.StructureCallback cb = new TestLibrary.StructureCallback() {
            @Override
            public SmallTestStructure callback(SmallTestStructure arg) {
                called[0] = true;
                cbarg[0] = arg.getPointer();
                arg.value = MAGIC;
                return arg;
            }
        };
        SmallTestStructure.allocations = 0;
        SmallTestStructure value = lib.callStructureCallback(cb, s);
        assertTrue("Callback not called", called[0]);
        assertEquals("Wrong argument passed to callback", s.getPointer(), cbarg[0]);
        assertEquals("Structure argument not synched on callback return",
                     MAGIC, s.value, 0d);
        assertEquals("Wrong structure return", s.getPointer(), value.getPointer());
        assertEquals("Structure return not synched",
                     MAGIC, value.value, 0d);
        // All structures involved should be created from pointers, with no
        // memory allocation at all.
        assertEquals("No structure memory should be allocated", 0, SmallTestStructure.allocations);
    }

    public void testCallStructureArrayCallback() {
        final SmallTestStructure s = new SmallTestStructure();
        final SmallTestStructure[] array = (SmallTestStructure[])s.toArray(2);
        final double MAGIC = 118.625;
        TestLibrary.StructureCallback cb = new TestLibrary.StructureCallback() {
            @Override
            public SmallTestStructure callback(SmallTestStructure arg) {
                SmallTestStructure[] array =
                    (SmallTestStructure[])arg.toArray(2);
                array[0].value = MAGIC;
                array[1].value = MAGIC*2;
                return arg;
            }
        };
        SmallTestStructure value = lib.callStructureCallback(cb, s);
        assertEquals("Structure array element 0 not synched on callback return",
                     MAGIC, array[0].value, 0d);
        assertEquals("Structure array element 1 not synched on callback return",
                     MAGIC*2, array[1].value, 0d);
    }

    public void testCallBooleanCallback() {
        final boolean[] called = {false};
        final boolean[] cbargs = { false, false };
        TestLibrary.BooleanCallback cb = new TestLibrary.BooleanCallback() {
            @Override
            public boolean callback(boolean arg, boolean arg2) {
                called[0] = true;
                cbargs[0] = arg;
                cbargs[1] = arg2;
                return arg && arg2;
            }
        };
        boolean value = lib.callBooleanCallback(cb, true, false);
        assertTrue("Callback not called", called[0]);
        assertEquals("Wrong first callback argument", true, cbargs[0]);
        assertEquals("Wrong second callback argument", false, cbargs[1]);
        assertFalse("Wrong boolean return", value);
    }

    public void testCallInt8Callback() {
        final boolean[] called = {false};
        final byte[] cbargs = { 0, 0 };
        TestLibrary.ByteCallback cb = new TestLibrary.ByteCallback() {
            @Override
            public byte callback(byte arg, byte arg2) {
                called[0] = true;
                cbargs[0] = arg;
                cbargs[1] = arg2;
                return (byte)(arg + arg2);
            }
        };
        final byte MAGIC = 0x11;
        byte value = lib.callInt8Callback(cb, MAGIC, (byte)(MAGIC*2));
        assertTrue("Callback not called", called[0]);
        assertEquals("Wrong first callback argument",
                     Integer.toHexString(MAGIC),
                     Integer.toHexString(cbargs[0]));
        assertEquals("Wrong second callback argument",
                     Integer.toHexString(MAGIC*2),
                     Integer.toHexString(cbargs[1]));
        assertEquals("Wrong byte return",
                     Integer.toHexString(MAGIC*3),
                     Integer.toHexString(value));

        value = lib.callInt8Callback(cb, (byte)-1, (byte)-2);
        assertEquals("Wrong byte return (hi bit)", (byte)-3, value);
    }

    public void testCallInt16Callback() {
        final boolean[] called = {false};
        final short[] cbargs = { 0, 0 };
        TestLibrary.ShortCallback cb = new TestLibrary.ShortCallback() {
            @Override
            public short callback(short arg, short arg2) {
                called[0] = true;
                cbargs[0] = arg;
                cbargs[1] = arg2;
                return (short)(arg + arg2);
            }
        };
        final short MAGIC = 0x1111;
        short value = lib.callInt16Callback(cb, MAGIC, (short)(MAGIC*2));
        assertTrue("Callback not called", called[0]);
        assertEquals("Wrong first callback argument",
                     Integer.toHexString(MAGIC),
                     Integer.toHexString(cbargs[0]));
        assertEquals("Wrong second callback argument",
                     Integer.toHexString(MAGIC*2),
                     Integer.toHexString(cbargs[1]));
        assertEquals("Wrong short return",
                     Integer.toHexString(MAGIC*3),
                     Integer.toHexString(value));

        value = lib.callInt16Callback(cb, (short)-1, (short)-2);
        assertEquals("Wrong short return (hi bit)", (short)-3, value);
    }

    public void testCallNativeLongCallback() {
        final boolean[] called = {false};
        final NativeLong[] cbargs = { null, null};
        TestLibrary.NativeLongCallback cb = new TestLibrary.NativeLongCallback() {
            @Override
            public NativeLong callback(NativeLong arg, NativeLong arg2) {
                called[0] = true;
                cbargs[0] = arg;
                cbargs[1] = arg2;
                return new NativeLong(arg.intValue() +  arg2.intValue());
            }
        };
        NativeLong value = lib.callNativeLongCallback(cb, new NativeLong(1), new NativeLong(2));
        assertTrue("Callback not called", called[0]);
        assertEquals("Wrong first callback argument", new NativeLong(1), cbargs[0]);
        assertEquals("Wrong second callback argument", new NativeLong(2), cbargs[1]);
        assertEquals("Wrong boolean return", new NativeLong(3), value);
    }

    public void testCallNativeMappedCallback() {
        final boolean[] called = {false};
        final Custom[] cbargs = { null, null};
        TestLibrary.CustomCallback cb = new TestLibrary.CustomCallback() {
            @Override
            public Custom callback(Custom arg, Custom arg2) {
                called[0] = true;
                cbargs[0] = arg;
                cbargs[1] = arg2;
                return new Custom(arg.value + arg2.value);
            }
        };
        int value = lib.callInt32Callback(cb, 1, 2);
        assertTrue("Callback not called", called[0]);
        assertEquals("Wrong first callback argument", new Custom(1), cbargs[0]);
        assertEquals("Wrong second callback argument", new Custom(2), cbargs[1]);
        assertEquals("Wrong NativeMapped return", 3, value);
    }

    public void testCallStringCallback() {
        final boolean[] called = {false};
        final String[] cbargs = { null, null };
        TestLibrary.StringCallback cb = new TestLibrary.StringCallback() {
            @Override
            public String callback(String arg, String arg2) {
                called[0] = true;
                cbargs[0] = arg;
                cbargs[1] = arg2;
                return arg + arg2;
            }
        };
        Charset charset = Charset.forName(Native.getDefaultStringEncoding());
        final String VALUE = "value" + charset.decode(charset.encode(UNICODE));
        final String VALUE2 = getName() + charset.decode(charset.encode(UNICODE));
        String value = lib.callStringCallback(cb, VALUE, VALUE2);
        assertTrue("Callback not called", called[0]);
        assertEquals("Wrong String callback argument 0", VALUE, cbargs[0]);
        assertEquals("Wrong String callback argument 1", VALUE2, cbargs[1]);
        assertEquals("Wrong String return", VALUE + VALUE2, value);
    }

    public void testStringCallbackMemoryReclamation() throws Exception {
        TestLibrary.StringCallback cb = new TestLibrary.StringCallback() {
            @Override
            public String callback(String arg, String arg2) {
                return arg + arg2;
            }
        };

        // A little internal groping
        final Map<?, ?> m = CallbackReference.allocations;
        m.clear();

        Charset charset = Charset.forName(Native.getDefaultStringEncoding());
        String arg = getName() + "1" + charset.decode(charset.encode(UNICODE));
        String arg2 = getName() + "2" + charset.decode(charset.encode(UNICODE));
        String value = lib.callStringCallback(cb, arg, arg2);
        final WeakReference<Object> ref = new WeakReference<>(value);

        arg = null;
        value = null;
        waitForGc(new Condition<Object>(null) {
            public boolean evaluate(Object o) {
                return m.size() > 0 || ref.get() != null;
            }
        });
        assertNull("NativeString reference not GC'd", ref.get());
        assertEquals("NativeString reference still held: " + m.values(), 0, m.size());
    }

    public void testCallWideStringCallback() {
        final boolean[] called = {false};
        final WString[] cbargs = { null, null };
        TestLibrary.WideStringCallback cb = new TestLibrary.WideStringCallback() {
            @Override
            public WString callback(WString arg, WString arg2) {
                called[0] = true;
                cbargs[0] = arg;
                cbargs[1] = arg2;
                return new WString(arg.toString() + arg2.toString());
            }
        };
        final WString VALUE = new WString("magic" + UNICODE);
        final WString VALUE2 = new WString(getName() + UNICODE);
        WString value = lib.callWideStringCallback(cb, VALUE, VALUE2);
        assertTrue("Callback not called", called[0]);
        assertEquals("Wrong first callback argument", VALUE, cbargs[0]);
        assertEquals("Wrong second callback argument", VALUE2, cbargs[1]);
        assertEquals("Wrong wide string return", new WString(VALUE.toString() + VALUE2.toString()), value);
    }

    public void testCallStringArrayCallback() {
        final boolean[] called = {false};
        final String[][] cbargs = { null };
        TestLibrary.StringArrayCallback cb = new TestLibrary.StringArrayCallback() {
            @Override
            public String[] callback(String[] arg) {
                called[0] = true;
                cbargs[0] = arg;
                return arg;
            }
        };
        Charset charset = Charset.forName(Native.getDefaultStringEncoding());
        final String VALUE = "value" + charset.decode(charset.encode(UNICODE));
        final String[] VALUE_ARRAY = { VALUE, null };
        Pointer value = lib.callStringArrayCallback(cb, VALUE_ARRAY);
        assertTrue("Callback not called", called[0]);
        assertEquals("String[] array should not be modified",
                     VALUE, VALUE_ARRAY[0]);
        assertEquals("Terminating null should be removed from incoming arg",
                     VALUE_ARRAY.length-1, cbargs[0].length);
        assertEquals("String[] argument index 0 mismatch",
                     VALUE_ARRAY[0], cbargs[0][0]);
        String[] result = value.getStringArray(0);
        assertEquals("Wrong String[] return", VALUE_ARRAY[0], result[0]);
        assertEquals("Terminating null should be removed from return value",
                     VALUE_ARRAY.length-1, result.length);
    }

    public void testCallCallbackWithByReferenceArgument() {
        final boolean[] called = {false};
        TestLibrary.CopyArgToByReference cb = new TestLibrary.CopyArgToByReference() {
            @Override
            public int callback(int arg, IntByReference result) {
                called[0] = true;
                result.setValue(arg);
                return result.getValue();
            }
        };
        final int VALUE = 0;
        IntByReference ref = new IntByReference(~VALUE);
        int value = lib.callCallbackWithByReferenceArgument(cb, VALUE, ref);
        assertEquals("Wrong value returned", VALUE, value);
        assertEquals("Wrong value in by reference memory", VALUE, ref.getValue());
    }

    public void testCallCallbackWithStructByValue() throws Exception {
        final boolean[] called = { false };
        final TestStructure.ByValue[] arg = { null };
        final TestStructure.ByValue s = new TestStructure.ByValue();
        TestStructure.TestCallback cb = new TestStructure.TestCallback() {
            @Override
            public TestStructure.ByValue callback(TestStructure.ByValue s) {
                // Copy the argument value for later comparison
                called[0] = true;
                return arg[0] = s;
            }
        };
        s.c = (byte)0x11;
        s.s = 0x2222;
        s.i = 0x33333333;
        s.j = 0x4444444444444444L;
        s.inner.value = 5;

        TestStructure result = lib.callCallbackWithStructByValue(cb, s);
        assertTrue("Callback not called", called[0]);
        assertTrue("ByValue argument should own its own memory, instead was "
                   + arg[0].getPointer(), arg[0].getPointer() instanceof Memory);
        assertTrue("ByValue result should own its own memory, instead was "
                   + result.getPointer(), result.getPointer() instanceof Memory);
        if (!s.dataEquals(arg[0], true)) {
            System.out.println("Mismatch: " + s);
            System.out.println("  versus: " + arg[0]);
        }
        assertTrue("Wrong value for callback argument", s.dataEquals(arg[0], true));
        if (!s.dataEquals(result, true)) {
            System.out.println("Mismatch: " + s);
            System.out.println("  versus: " + result);
        }
        assertTrue("Wrong value for callback result", s.dataEquals(result, true));
    }

    public void testUnionByValueCallbackArgument() throws Exception{
        TestLibrary.TestUnion arg = new TestLibrary.TestUnion();
        arg.setType(String.class);
        Charset charset = Charset.forName(arg.getStringEncoding());
        final String VALUE = getName() + charset.decode(charset.encode(UNICODE));
        arg.f1 = VALUE;
        final boolean[] called = { false };
        final TestLibrary.TestUnion[] cbvalue = { null };
        TestLibrary.TestUnion result = lib.testUnionByValueCallbackArgument(new TestLibrary.UnionCallback() {
            @Override
            public TestLibrary.TestUnion invoke(TestLibrary.TestUnion v) {
                called[0] = true;
                v.setType(String.class);
                v.read();
                cbvalue[0] = v;
                return v;
            }
        }, arg);
        assertTrue("Callback not called", called[0]);
        assertTrue("ByValue argument should have its own allocated memory, instead was "
                   + cbvalue[0].getPointer(),
                   cbvalue[0].getPointer() instanceof Memory);
        assertEquals("Wrong value for callback argument", VALUE, cbvalue[0].f1);
        assertEquals("Wrong value for callback result", VALUE, result.getTypedValue(String.class));
    }

    public void testCallCallbackWithCallbackArgumentAndResult() {
        TestLibrary.CbCallback cb = new TestLibrary.CbCallback() {
            @Override
            public CbCallback callback(CbCallback arg) {
                return arg;
            }
        };
        TestLibrary.CbCallback cb2 = lib.callCallbackWithCallback(cb);
        assertEquals("Callback reference should be reused", cb, cb2);
    }

    public void testDefaultCallbackExceptionHandler() {
        final RuntimeException ERROR = new RuntimeException(getName());
        PrintStream ps = System.err;
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        System.setErr(new PrintStream(s));
        try {
            TestLibrary.CbCallback cb = new TestLibrary.CbCallback() {
                @Override
                public CbCallback callback(CbCallback arg) {
                    throw ERROR;
                }
            };
            TestLibrary.CbCallback cb2 = lib.callCallbackWithCallback(cb);
            String output = s.toString();
            assertTrue("Default handler not called", output.length() > 0);
        }
        finally {
            System.setErr(ps);
        }
    }

    // Most Callbacks are wrapped in DefaultCallbackProxy, which catches their
    // exceptions.
    public void testCallbackExceptionHandler() {
        final RuntimeException ERROR = new RuntimeException(getName());
        final Throwable CAUGHT[] = { null };
        final Callback CALLBACK[] = { null };
        UncaughtExceptionHandler old = Native.getCallbackExceptionHandler();
        UncaughtExceptionHandler handler = new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Callback cb, Throwable e) {
                CALLBACK[0] = cb;
                CAUGHT[0] = e;
            }
        };
        Native.setCallbackExceptionHandler(handler);
        try {
            TestLibrary.CbCallback cb = new TestLibrary.CbCallback() {
                @Override
                public CbCallback callback(CbCallback arg) {
                    throw ERROR;
                }
            };
            TestLibrary.CbCallback cb2 = lib.callCallbackWithCallback(cb);
            assertNotNull("Exception handler not called", CALLBACK[0]);
            assertEquals("Wrong callback argument to handler", cb, CALLBACK[0]);
            assertEquals("Wrong exception passed to handler",
                         ERROR, CAUGHT[0]);
        }
        finally {
            Native.setCallbackExceptionHandler(old);
        }
    }

    // CallbackProxy is called directly from native.
    public void testCallbackExceptionHandlerWithCallbackProxy() throws Throwable {
        final RuntimeException ERROR = new RuntimeException(getName());
        final Throwable CAUGHT[] = { null };
        final Callback CALLBACK[] = { null };
        UncaughtExceptionHandler old = Native.getCallbackExceptionHandler();
        UncaughtExceptionHandler handler = new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Callback cb, Throwable e) {
                CALLBACK[0] = cb;
                CAUGHT[0] = e;
            }
        };
        Native.setCallbackExceptionHandler(handler);
        try {
            class TestProxy implements CallbackProxy, TestLibrary.CbCallback {
                @Override
                public CbCallback callback(CbCallback arg) {
                    throw new Error("Should never be called");
                }
                @Override
                public Object callback(Object[] args) {
                    throw ERROR;
                }
                @Override
                public Class<?>[] getParameterTypes() {
                    return new Class[] { CbCallback.class };
                }
                @Override
                public Class<?> getReturnType() {
                    return CbCallback.class;
                }
            };
            TestLibrary.CbCallback cb = new TestProxy();
            TestLibrary.CbCallback cb2 = lib.callCallbackWithCallback(cb);
            assertNotNull("Exception handler not called", CALLBACK[0]);
            assertEquals("Wrong callback argument to handler", cb, CALLBACK[0]);
            assertEquals("Wrong exception passed to handler",
                         ERROR, CAUGHT[0]);
        }
        finally {
            Native.setCallbackExceptionHandler(old);
        }
    }

    public void testResetCallbackExceptionHandler() {
        Native.setCallbackExceptionHandler(null);
        assertNotNull("Should not be able to set callback EH null",
                      Native.getCallbackExceptionHandler());
    }

    public void testInvokeCallback() {
        TestLibrary.Int32CallbackX cb = lib.returnCallback();
        assertNotNull("Callback should not be null", cb);
        assertEquals("Callback should be callable", 1, cb.callback(1));

        TestLibrary.Int32CallbackX cb2 = new TestLibrary.Int32CallbackX() {
            @Override
            public int callback(int arg) {
                return 0;
            }
        };
        assertSame("Java callback should be looked up",
                   cb2, lib.returnCallbackArgument(cb2));
        assertSame("Existing native function wrapper should be reused",
                   cb, lib.returnCallbackArgument(cb));
    }

    public void testCallCallbackInStructure() {
        final boolean[] flag = {false};
        final TestLibrary.CbStruct s = new TestLibrary.CbStruct();
        s.cb = new Callback() {
            public void callback() {
                flag[0] = true;
            }
        };
        lib.callCallbackInStruct(s);
        assertTrue("Callback not invoked", flag[0]);
    }

    public void testCustomCallbackMethodName() {
        final boolean[] called = {false};
        TestLibrary.VoidCallbackCustom cb = new TestLibrary.VoidCallbackCustom() {
            @Override
            public void customMethodName() {
                called[0] = true;
            }
            @Override
            public String toString() {
                return "Some debug output";
            }
        };
        lib.callVoidCallback(cb);
        assertTrue("Callback with custom method name not called", called[0]);
    }

    public void testDisallowDetachFromJVMThread() {
        final boolean[] called = {false};
        final boolean[] exceptionThrown = {true};
        TestLibrary.VoidCallback cb = new TestLibrary.VoidCallback() {
            @Override
            public void callback() {
                called[0] = true;
                try {
                    Native.detach(true);
                }
                catch(IllegalStateException e) {
                }
            }
        };
        lib.callVoidCallback(cb);
        assertTrue("Callback not called", called[0]);
        assertTrue("Native.detach(true) should throw IllegalStateException when called from JVM thread", exceptionThrown[0]);
    }

    public void testCustomCallbackVariedInheritance() {
        final boolean[] called = {false};
        TestLibrary.VoidCallbackCustom cb =
            new TestLibrary.VoidCallbackCustomDerived();
        lib.callVoidCallback(cb);
    }

    protected static class CallbackTypeMapper extends DefaultTypeMapper {
        public int fromNativeConversions = 0;
        public int toNativeConversions = 0;
        public void clear() {
            fromNativeConversions = 0;
            toNativeConversions = 0;
        }
        {
            // Convert java doubles into native integers and back
            TypeConverter converter = new TypeConverter() {
                @Override
                public Object fromNative(Object value, FromNativeContext context) {
                    ++fromNativeConversions;
                    return Double.valueOf(((Integer)value).intValue());
                }
                @Override
                public Class<?> nativeType() {
                    return Integer.class;
                }
                @Override
                public Object toNative(Object value, ToNativeContext ctx) {
                    ++toNativeConversions;
                    return Integer.valueOf(((Double)value).intValue());
                }
            };
            addTypeConverter(double.class, converter);
            converter = new TypeConverter() {
                @Override
                public Object fromNative(Object value, FromNativeContext context) {
                    ++fromNativeConversions;
                    return Float.valueOf(((Long)value).intValue());
                }
                @Override
                public Class<?> nativeType() {
                    return Long.class;
                }
                @Override
                public Object toNative(Object value, ToNativeContext ctx) {
                    ++toNativeConversions;
                    return Long.valueOf(((Float)value).longValue());
                }
            };
            addTypeConverter(float.class, converter);
            converter = new TypeConverter() {
                @Override
                public Object fromNative(Object value, FromNativeContext context) {
                    ++fromNativeConversions;
                    if (value == null) {
                        return null;
                    }
                    if (value instanceof Pointer) {
                        return ((Pointer)value).getWideString(0);
                    }
                    return value.toString();
                }
                @Override
                public Class<?> nativeType() {
                    return WString.class;
                }
                @Override
                public Object toNative(Object value, ToNativeContext ctx) {
                    ++toNativeConversions;
                    return new WString(value.toString());
                }
            };
            addTypeConverter(String.class, converter);
        }
    }

    public static interface CallbackTestLibrary extends Library {
        final CallbackTypeMapper _MAPPER = new CallbackTypeMapper();
        final Map<String, ?> _OPTIONS = Collections.singletonMap(Library.OPTION_TYPE_MAPPER, _MAPPER);

        interface DoubleCallback extends Callback {
            double callback(double arg, double arg2);
        }
        double callInt32Callback(DoubleCallback c, double arg, double arg2);
        interface FloatCallback extends Callback {
            float callback(float arg, float arg2);
        }
        float callInt64Callback(FloatCallback c, float arg, float arg2);
        interface WStringCallback extends Callback {
            String callback(String arg, String arg2);
        }
        String callWideStringCallback(WStringCallback c, String arg, String arg2);
    }

    protected CallbackTestLibrary loadCallbackTestLibrary() {
        return Native.load("testlib", CallbackTestLibrary.class, CallbackTestLibrary._OPTIONS);
    }

    /** This test is here instead of NativeTest in order to facilitate running
        the exact same test on a direct-mapped library without the tests
        interfering with one another due to persistent/cached state in library
        loading.
    */
    public void testCallbackUsesTypeMapper() throws Exception {
        CallbackTestLibrary lib = loadCallbackTestLibrary();
        CallbackTestLibrary._MAPPER.clear();

        final double[] ARGS = new double[2];

        CallbackTestLibrary.DoubleCallback cb = new CallbackTestLibrary.DoubleCallback() {
            @Override
            public double callback(double arg, double arg2) {
                ARGS[0] = arg;
                ARGS[1] = arg2;
                return arg + arg2;
            }
        };
        assertEquals("Wrong type mapper for callback class", CallbackTestLibrary._MAPPER,
                     Native.getTypeMapper(CallbackTestLibrary.DoubleCallback.class));
        assertEquals("Wrong type mapper for callback object", CallbackTestLibrary._MAPPER,
                     Native.getTypeMapper(cb.getClass()));

        double result = lib.callInt32Callback(cb, -1, -2);
        assertEquals("Wrong first callback argument", -1, ARGS[0], 0);
        assertEquals("Wrong second callback argument", -2, ARGS[1], 0);
        assertEquals("Incorrect result of callback invocation", -3, result, 0);

        // Once per argument, then again for return value (convert native int->Java double)
        assertEquals("Type mapper not called for arguments", 3, CallbackTestLibrary._MAPPER.fromNativeConversions);
        // Once per argument, then again for return value (convert Java double->native int)
        assertEquals("Type mapper not called for result", 3, CallbackTestLibrary._MAPPER.toNativeConversions);
    }

    public void testTypeMapperWithWideStrings() throws Exception {
        CallbackTestLibrary lib = loadCallbackTestLibrary();
        CallbackTestLibrary._MAPPER.clear();

        final String[] ARGS = new String[2];

        CallbackTestLibrary.WStringCallback cb = new CallbackTestLibrary.WStringCallback() {
            @Override
            public String callback(String arg, String arg2) {
                ARGS[0] = arg;
                ARGS[1] = arg2;
                return arg + arg2;
            }
        };
        assertEquals("Wrong type mapper for callback class", CallbackTestLibrary._MAPPER,
                     Native.getTypeMapper(CallbackTestLibrary.WStringCallback.class));
        assertEquals("Wrong type mapper for callback object", CallbackTestLibrary._MAPPER,
                     Native.getTypeMapper(cb.getClass()));

        final String[] EXPECTED = { "magic" + UNICODE, getName() + UNICODE };
        String result = lib.callWideStringCallback(cb, EXPECTED[0], EXPECTED[1]);
        assertEquals("Wrong first callback argument", EXPECTED[0], ARGS[0]);
        assertEquals("Wrong second callback argument", EXPECTED[1], ARGS[1]);
        assertEquals("Incorrect result of callback invocation", EXPECTED[0] + EXPECTED[1], result);

        // Once per argument, then again for return value (convert const wchar_t*->Java String)
        assertEquals("Type mapper not called for arguments", 3, CallbackTestLibrary._MAPPER.fromNativeConversions);
        // Once per argument, then again for return value (convert Java String->const wchar_t*)
        assertEquals("Type mapper not called for result", 3, CallbackTestLibrary._MAPPER.toNativeConversions);
    }

    public void testCallbackUsesTypeMapperWithDifferentReturnTypeSize() throws Exception {
        CallbackTestLibrary lib = loadCallbackTestLibrary();

        final float[] ARGS = new float[2];

        CallbackTestLibrary.FloatCallback cb = new CallbackTestLibrary.FloatCallback() {
            @Override
            public float callback(float arg, float arg2) {
                ARGS[0] = arg;
                ARGS[1] = arg2;
                return arg + arg2;
            }
        };
        assertEquals("Wrong type mapper for callback class", CallbackTestLibrary._MAPPER,
                     Native.getTypeMapper(CallbackTestLibrary.FloatCallback.class));
        assertEquals("Wrong type mapper for callback object", CallbackTestLibrary._MAPPER,
                     Native.getTypeMapper(cb.getClass()));

        float result = lib.callInt64Callback(cb, -1, -2);
        assertEquals("Wrong first callback argument", -1, ARGS[0], 0);
        assertEquals("Wrong second callback argument", -2, ARGS[1], 0);
        assertEquals("Incorrect result of callback invocation", -3, result, 0);
    }

    protected void callThreadedCallback(TestLibrary.VoidCallback cb,
                                        CallbackThreadInitializer cti,
                                        int repeat, int sleepms,
                                        int[] called) throws Exception {
        callThreadedCallback(cb, cti, repeat, sleepms, called, repeat);
    }

    protected void callThreadedCallback(TestLibrary.VoidCallback cb,
                                        CallbackThreadInitializer cti,
                                        int repeat, int sleepms,
                                        int[] called, int returnAfter) throws Exception {
        if (cti != null) {
            Native.setCallbackThreadInitializer(cb, cti);
        }
        lib.callVoidCallbackThreaded(cb, repeat, sleepms, getName(), 0);

        long start = System.currentTimeMillis();
        while (called[0] < returnAfter) {
            Thread.sleep(10);
            if (System.currentTimeMillis() - start > THREAD_TIMEOUT) {
                fail("Timed out waiting for callback, invoked " + called[0] + " times so far");
            }
        }
    }

    public void testCallbackThreadDefaults() throws Exception {
        final int[] called = {0};
        final boolean[] daemon = {false};
        final String[] name = { null };
        final ThreadGroup[] group = { null };
        final Thread[] t = { null };

        ThreadGroup testGroup = new ThreadGroup(getName() + UNICODE);
        TestLibrary.VoidCallback cb = new TestLibrary.VoidCallback() {
            @Override
            public void callback() {
                Thread thread = Thread.currentThread();
                daemon[0] = thread.isDaemon();
                name[0] = thread.getName();
                group[0] = thread.getThreadGroup();
                t[0] = thread;
                ++called[0];
            }
        };
        callThreadedCallback(cb, null, 1, 100, called);

        assertFalse("Callback thread default should not be attached as daemon", daemon[0]);
        // thread name and group are not defined
    }

    public void testCustomizeCallbackThread() throws Exception {
        final int[] called = {0};
        final boolean[] daemon = {false};
        final String[] name = { null };
        final ThreadGroup[] group = { null };
        final Thread[] t = { null };
        // Ensure unicode is properly handled
        final String tname = "NAME: " + getName() + UNICODE;
        final boolean[] alive = {false};

        ThreadGroup testGroup = new ThreadGroup("Thread group for " + getName());
        CallbackThreadInitializer init = new CallbackThreadInitializer(true, false, tname, testGroup);
        TestLibrary.VoidCallback cb = new TestLibrary.VoidCallback() {
            @Override
            public void callback() {
                Thread thread = Thread.currentThread();
                daemon[0] = thread.isDaemon();
                name[0] = thread.getName();
                group[0] = thread.getThreadGroup();
                t[0] = thread;
                if (thread.isAlive()) {
                    // NOTE: older phoneME incorrectly reports thread "alive" status
                    alive[0] = true;
                }

                ++called[0];
                if (THREAD_DETACH_BUG && called[0] == 2) {
                    Native.detach(true);
                }
            }
        };
        callThreadedCallback(cb, init, 2, 2000, called, 1);

        assertTrue("Callback thread not attached as daemon", daemon[0]);
        assertEquals("Callback thread name not applied", tname, name[0]);
        assertEquals("Callback thread group not applied", testGroup, group[0]);
        // NOTE: older phoneME incorrectly reports thread "alive" status
        if (!alive[0]) {
            throw new Error("VM incorrectly reports Thread.isAlive() == false within callback");
        }
        assertTrue("Thread should still be alive", t[0].isAlive());

        long start = System.currentTimeMillis();
        while (called[0] < 2) {
            Thread.sleep(10);
            if (System.currentTimeMillis() - start > THREAD_TIMEOUT) {
                fail("Timed out waiting for second callback invocation, which indicates detach");
            }
        }

        waitFor(t[0]);
    }

    public void testSmallStackCallback() throws Exception {
        // This test runs the callback in a thread, that is allocated a very
        // small size. It was observed on linux amd64, that a library allocated
        // a stack size of 64kB, this prevented the JVM to attach to that
        // thread. The JNIEnv pointer was not checked and this lead to a
        // hard crash of the JVM.
        TestLibrary.VoidCallback cb = new TestLibrary.VoidCallback() {
            @Override
            public void callback() {
                System.out.println("Callback called");
            }
        };

        lib.callVoidCallbackThreaded(cb, 1, 0, "Test Callback", 64 * 1024);

        // Give the JVM enough time to run the call back
        Thread.sleep(1 * 1000);
    }

    // Detach preference is indicated by the initializer.  Thread is attached
    // as daemon to avoid VM having to wait for it.
    public void testCallbackThreadPersistence() throws Exception {
        final int[] called = {0};
        final Set<Thread> threads = new HashSet<>();

        final int COUNT = 5;
        CallbackThreadInitializer init = new CallbackThreadInitializer(true, false) {
            @Override
            public String getName(Callback cb) {
                return "Test thread (native) for " + CallbacksTest.this.getName() + " (call count: " + called[0] + ")";
            }
        };
        TestLibrary.VoidCallback cb = new TestLibrary.VoidCallback() {
            @Override
            public void callback() {
                threads.add(Thread.currentThread());
                ++called[0];
                if (THREAD_DETACH_BUG && called[0] == COUNT) {
                    Native.detach(true);
                }
            }
        };
        callThreadedCallback(cb, init, COUNT, 100, called);

        assertEquals("Multiple callbacks on a given native thread should use the same Thread mapping: " + threads,
                     1, threads.size());

        waitFor(threads.iterator().next());
    }

    // Thread object is never GC'd on linux-amd64 and darwin-amd64 (w/openjdk7)
    public void testCleanupUndetachedThreadOnThreadExit() throws Exception {
        final Set<Reference<Thread>> threads = new HashSet<>();
        final int[] called = { 0 };
        TestLibrary.VoidCallback cb = new TestLibrary.VoidCallback() {
            @Override
            public void callback() {
                threads.add(new WeakReference<>(Thread.currentThread()));
                if (++called[0] == 1) {
                    Thread.currentThread().setName(getName() + " (Thread to be cleaned up)");
                }
                Native.detach(false);
            }
        };
        // Always attach as daemon to ensure tests will exit
        CallbackThreadInitializer asDaemon = new CallbackThreadInitializer(true) {
            @Override
            public String getName(Callback cb) {
                return "Test thread (native) for " + CallbacksTest.this.getName();
            }
        };
        callThreadedCallback(cb, asDaemon, 2, 100, called);
        // Wait for it to start up
        long start = System.currentTimeMillis();
        while (threads.size() == 0 && called[0] == 0) {
            Thread.sleep(10L);
            if (System.currentTimeMillis() - start > THREAD_TIMEOUT) {
                fail("Timed out waiting for thread to detach and terminate");
            }
        }
        start = System.currentTimeMillis();
        Reference<Thread> ref = threads.iterator().next();
        while (ref.get() != null) {
            System.gc();
            Thread.sleep(100);
            Thread[] remaining = new Thread[Thread.activeCount()];
            Thread.enumerate(remaining);
            if (System.currentTimeMillis() - start > THREAD_TIMEOUT) {
                Thread t = ref.get();
                Pointer terminationFlag = Native.getTerminationFlag(t);
                assertNotNull("Native thread termination flag is missing", terminationFlag);
                if (terminationFlag.getInt(0) == 0) {
                    fail("Timed out waiting for native attached thread to be GC'd: " + t + " alive: "
                         + t.isAlive() + " daemon: " + t.isDaemon() + "\n" + Arrays.asList(remaining));
                }
                System.err.println("Warning: JVM did not GC Thread mapping after native thread terminated");
                break;
            }
        }
    }

    // Callback indicates detach preference (instead of
    // CallbackThreadInitializer); thread is non-daemon (default),
    // but callback explicitly detaches it on final invocation.
    public void testCallbackIndicatedThreadDetach() throws Exception {
        final int[] called = {0};
        final Set<Thread> threads = new HashSet<>();
        final int COUNT = 5;
        TestLibrary.VoidCallback cb = new TestLibrary.VoidCallback() {
            @Override
            public void callback() {
                threads.add(Thread.currentThread());
                // detach on final invocation
                int count = called[0] + 1;
                if (count == 1) {
                    Thread.currentThread().setName("Native thread for " + getName());
                    Native.detach(false);
                }
                else if (count == COUNT) {
                    Native.detach(true);
                }
                called[0] = count;
            }
        };
        callThreadedCallback(cb, null, COUNT, 100, called);

        assertEquals("Multiple callbacks in the same native thread should use the same Thread mapping: "
                     + threads, 1, threads.size());

        waitFor(threads.iterator().next());
    }

    public void testDLLCallback() throws Exception {
        if (!Platform.HAS_DLL_CALLBACKS) {
            return;
        }

        final boolean[] called = { false };
        class TestCallback implements TestLibrary.VoidCallback, com.sun.jna.win32.DLLCallback {
            @Override
            public void callback() {
                called[0] = true;
            }
        }

        TestCallback cb = new TestCallback();
        lib.callVoidCallback(cb);
        assertTrue("Callback not called", called[0]);

        // Check module information
        Pointer fp = CallbackReference.getFunctionPointer(cb);
        NativeLibrary kernel32 = NativeLibrary.getInstance("kernel32", W32APIOptions.DEFAULT_OPTIONS);
        Function f = kernel32.getFunction("GetModuleHandleExW");
        final int GET_MODULE_HANDLE_FROM_ADDRESS = 0x4;
        PointerByReference pref = new PointerByReference();
        int result = f.invokeInt(new Object[] { Integer.valueOf(GET_MODULE_HANDLE_FROM_ADDRESS), fp, pref });
        assertTrue("GetModuleHandleEx(fptr) failed: " + Native.getLastError(), result != 0);

        f = kernel32.getFunction("GetModuleFileNameW");
        char[] buf = new char[1024];
        result = f.invokeInt(new Object[] { pref.getValue(), buf, buf.length });
        assertTrue("GetModuleFileName(fptr) failed: " + Native.getLastError(), result != 0);

        f = kernel32.getFunction("GetModuleHandleW");
        Pointer handle = f.invokePointer(new Object[] { Native.jnidispatchPath != null ? Native.jnidispatchPath : "jnidispatch" });
        assertNotNull("GetModuleHandle(\"jnidispatch\") failed: " + Native.getLastError(), handle);
        assertEquals("Wrong module HANDLE for DLL function pointer", handle, pref.getValue());

        // Check slot re-use
        assertTrue("Callback not cached", callbackCache().containsKey(cb));
        final Map<Callback, CallbackReference> refs = callbackCache();
        CallbackReference ref = refs.get(cb);
        Pointer cbstruct = ref.cbstruct;
        Pointer first_fptr = cbstruct.getPointer(0);

        cb = null;
        waitForGc(new Condition<CallbackReference>(ref) {
            public boolean evaluate(CallbackReference _ref) {
                return _ref.get() != null || refs.containsValue(_ref);
            }
        });
        assertNull("Callback not GC'd", ref.get());
        assertFalse("Callback still in map", refs.containsValue(ref));

        ref = null;
        waitForGc(new Condition<Pointer>(cbstruct) {
            public boolean evaluate(Pointer p) {
                return refs.size() > 0 || p.peer != 0;
            }
        });
        assertEquals("Callback trampoline not freed", 0, cbstruct.peer);

        // Next allocation should be at same place
        called[0] = false;
        cb = new TestCallback();

        lib.callVoidCallback(cb);
        ref = refs.get(cb);
        cbstruct = ref.cbstruct;

        assertTrue("Callback not called", called[0]);
        assertEquals("Same (in-DLL) address should be re-used for DLL callbacks after callback is GCd",
                     first_fptr, cbstruct.getPointer(0));
    }

    public void testThrowOutOfMemoryWhenDLLCallbacksExhausted() throws Exception {
        if (!Platform.HAS_DLL_CALLBACKS) {
            return;
        }

        final boolean[] called = { false };
        class TestCallback implements TestLibrary.VoidCallback, com.sun.jna.win32.DLLCallback {
            @Override
            public void callback() {
                called[0] = true;
            }
        }

        // Exceeding allocations should result in OOM error
        try {
            for (int i=0;i <= TestCallback.DLL_FPTRS;i++) {
                lib.callVoidCallback(new TestCallback());
            }
            fail("Expected out of memory error when all DLL callbacks used");
        }
        catch(OutOfMemoryError e) {
        }
    }

    public interface TaggedCallingConventionTestLibrary extends Library, AltCallingConvention {
        interface TestCallbackTagged extends Callback, AltCallingConvention {
            void invoke();
        }
    }

    public void testCallingConventionFromInterface() {
        TaggedCallingConventionTestLibrary lib = Native.load("testlib", TaggedCallingConventionTestLibrary.class);
        TaggedCallingConventionTestLibrary.TestCallbackTagged cb = new TaggedCallingConventionTestLibrary.TestCallbackTagged() {
            @Override
            public void invoke() { }
        };
        try {
            Pointer p = CallbackReference.getFunctionPointer(cb);
            CallbackReference ref = CallbackReference.callbackMap.get(cb);
            assertNotNull("CallbackReference not found", ref);
            assertEquals("Tag-based calling convention not applied", Function.ALT_CONVENTION, ref.callingConvention);
        }
        catch (IllegalArgumentException e) {
            // Alt convention not supported
        }
    }

    public interface OptionCallingConventionTestLibrary extends Library {
        interface TestCallback extends Callback {
            void invoke();
        }
    }

    public void testCallingConventionFromOptions() {
        OptionCallingConventionTestLibrary lib =
                Native.load("testlib", OptionCallingConventionTestLibrary.class, Collections.singletonMap(Library.OPTION_CALLING_CONVENTION, Function.ALT_CONVENTION));
        assertNotNull("Library not loaded", lib);
        OptionCallingConventionTestLibrary.TestCallback cb = new OptionCallingConventionTestLibrary.TestCallback() {
            @Override
            public void invoke() { }
        };
        try {
            Pointer p = CallbackReference.getFunctionPointer(cb);
            assertNotNull("No function pointer", p);
            CallbackReference ref = CallbackReference.callbackMap.get(cb);
            assertNotNull("CallbackReference not found", ref);
            assertEquals("Option-based calling convention not applied", Function.ALT_CONVENTION, ref.callingConvention);
        } catch(IllegalArgumentException e) {
            // Alt convention not supported
        }
    }

    public static void main(java.lang.String[] argList) {
        junit.textui.TestRunner.run(CallbacksTest.class);
    }
}
