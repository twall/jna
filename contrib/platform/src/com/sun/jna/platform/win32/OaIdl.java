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

import java.util.Calendar;

import com.sun.jna.IntegerType;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.Union;
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTR;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.COM.Dispatch;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Variant.VARIANT;
import com.sun.jna.platform.win32.Variant.VariantArg;
import com.sun.jna.platform.win32.WTypes.BSTR;
import com.sun.jna.platform.win32.WTypes.LPOLESTR;
import com.sun.jna.platform.win32.WTypes.VARTYPE;
import com.sun.jna.platform.win32.WinDef.BYTE;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.platform.win32.WinDef.LCID;
import com.sun.jna.platform.win32.WinDef.LONG;
import com.sun.jna.platform.win32.WinDef.LONGLONG;
import com.sun.jna.platform.win32.WinDef.PVOID;
import com.sun.jna.platform.win32.WinDef.SCODE;
import com.sun.jna.platform.win32.WinDef.SHORT;
import com.sun.jna.platform.win32.WinDef.ULONG;
import com.sun.jna.platform.win32.WinDef.ULONGLONG;
import com.sun.jna.platform.win32.WinDef.USHORT;
import com.sun.jna.platform.win32.WinDef.WORD;
import com.sun.jna.platform.win32.COM.TypeComp;
import com.sun.jna.platform.win32.COM.Unknown;
import static com.sun.jna.platform.win32.Variant.VT_BOOL;
import static com.sun.jna.platform.win32.Variant.VT_BSTR;
import static com.sun.jna.platform.win32.Variant.VT_CY;
import static com.sun.jna.platform.win32.Variant.VT_DATE;
import static com.sun.jna.platform.win32.Variant.VT_DECIMAL;
import static com.sun.jna.platform.win32.Variant.VT_DISPATCH;
import static com.sun.jna.platform.win32.Variant.VT_ERROR;
import static com.sun.jna.platform.win32.Variant.VT_I1;
import static com.sun.jna.platform.win32.Variant.VT_I2;
import static com.sun.jna.platform.win32.Variant.VT_I4;
import static com.sun.jna.platform.win32.Variant.VT_INT;
import static com.sun.jna.platform.win32.Variant.VT_R4;
import static com.sun.jna.platform.win32.Variant.VT_R8;
import static com.sun.jna.platform.win32.Variant.VT_RECORD;
import static com.sun.jna.platform.win32.Variant.VT_UI1;
import static com.sun.jna.platform.win32.Variant.VT_UI2;
import static com.sun.jna.platform.win32.Variant.VT_UI4;
import static com.sun.jna.platform.win32.Variant.VT_UINT;
import static com.sun.jna.platform.win32.Variant.VT_UNKNOWN;
import static com.sun.jna.platform.win32.Variant.VT_VARIANT;
import com.sun.jna.ptr.ByReference;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.PointerByReference;
import java.io.Closeable;
import java.util.Date;

/**
 * The Interface OaIdl.
 */
public interface OaIdl {

    // The DATE Type is defined in localtime and the java Date type always contains
    // a a timezone offset, so the difference has to be calculated and can't be
    // predetermined
    @SuppressWarnings("deprecation")
    public static final long DATE_OFFSET = new Date(1899 - 1900, 12 - 1, 30, 0, 0, 0).getTime();

    /**
     * The Class EXCEPINFO.
     */
    @FieldOrder({"wCode", "wReserved", "bstrSource", "bstrDescription",
        "bstrHelpFile", "dwHelpContext", "pvReserved", "pfnDeferredFillIn",
        "scode"})
    public static class EXCEPINFO extends Structure {

        /**
         * The Class ByReference.
         */
        public static class ByReference extends EXCEPINFO implements
                Structure.ByReference {
        }

        /** The w code. */
        public WORD wCode;

        /** The w reserved. */
        public WORD wReserved;

        /** The bstr source. */
        public BSTR bstrSource;

        /** The bstr description. */
        public BSTR bstrDescription;

        /** The bstr help file. */
        public BSTR bstrHelpFile;

        /** The dw help context. */
        public DWORD dwHelpContext;

        /** The pv reserved. */
        public PVOID pvReserved;

        /** The pfn deferred fill in. */
        public EXCEPINFO.ByReference pfnDeferredFillIn;

        /** The scode. */
        public SCODE scode;

        /**
         * Instantiates a new excepinfo.
         */
        public EXCEPINFO() {
            super();
        }

        /**
         * Instantiates a new excepinfo.
         *
         * @param p
         *            the p
         */
        public EXCEPINFO(Pointer p) {
            super(p);
        }
    }

    public static class VARIANT_BOOL extends IntegerType {
        private static final long serialVersionUID = 1L;
        public static final int SIZE = 2;

        public VARIANT_BOOL() {
            this(0);
        }

        public VARIANT_BOOL(long value) {
            super(2, value);
        }

        public VARIANT_BOOL(boolean value) {
            this(value ? 0xFFFF : 0x0000);
        }

        public boolean booleanValue() {
            return shortValue() != 0x0000;
        }
    }

    public static class _VARIANT_BOOL extends VARIANT_BOOL {
        private static final long serialVersionUID = 1L;

        public _VARIANT_BOOL() {
            this(0);
        }

        public _VARIANT_BOOL(long value) {
            super(value);
        }
    }

    public static class VARIANT_BOOLByReference extends ByReference {
        public VARIANT_BOOLByReference() {
            this(new VARIANT_BOOL(0));
        }

        public VARIANT_BOOLByReference(VARIANT_BOOL value) {
            super(VARIANT_BOOL.SIZE);
            setValue(value);
        }

        public void setValue(VARIANT_BOOL value) {
            getPointer().setShort(0, value.shortValue());
        }

        public VARIANT_BOOL getValue() {
            return new VARIANT_BOOL(getPointer().getShort(0));
        }
    }

    public static class _VARIANT_BOOLByReference extends ByReference {
        public _VARIANT_BOOLByReference() {
            this(new VARIANT_BOOL(0));
        }

        public _VARIANT_BOOLByReference(VARIANT_BOOL value) {
            super(VARIANT_BOOL.SIZE);
            setValue(value);
        }

        public void setValue(VARIANT_BOOL value) {
            getPointer().setShort(0, value.shortValue());
        }

        public VARIANT_BOOL getValue() {
            return new VARIANT_BOOL(getPointer().getShort(0));
        }
    }

    @FieldOrder({"date"})
    public static class DATE extends Structure {
        private static final double MILLISECONDS_PER_DAY = 24L * 60L * 60L * 1000L;

        public static class ByReference extends DATE implements
                Structure.ByReference {
        }

        public double date;

        public DATE() {
            super();
        }

        public DATE(double date) {
            this.date = date;
        }

        public DATE(Date javaDate) {
            setFromJavaDate(javaDate);
        }

        public Date getAsJavaDate() {
            WinBase.SYSTEMTIME systemtime = new WinBase.SYSTEMTIME();
            OleAuto.INSTANCE.VariantTimeToSystemTime(date, systemtime);
            Calendar calendar = systemtime.toCalendar();

            // Fix milliseconds, as VariantTimeToSystemTime rounds them off
            int millis = (int) ((long) (Math.abs(date) * MILLISECONDS_PER_DAY + 0.5) % 1000L);
            if (date > 0 && millis > 500 || date < 0 && millis > 499) {
                millis -= 1000;
            }
            calendar.set(Calendar.MILLISECOND, millis);
            return calendar.getTime();
        }

        public void setFromJavaDate(Date javaDate) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(javaDate);
            DoubleByReference pvtime = new DoubleByReference();
            OleAuto.INSTANCE.SystemTimeToVariantTime(new WinBase.SYSTEMTIME(calendar), pvtime);
            double value = pvtime.getValue();

            // Add milliseconds, as SystemTimeToVariantTime truncates them
            date = value + Math.signum(value) * calendar.get(Calendar.MILLISECOND) / MILLISECONDS_PER_DAY;
        }
    }

    /**
     * The Class DISPID.
     */
    public static class DISPID extends LONG {
        private static final long serialVersionUID = 1L;

        public DISPID() {
            this(0);
        }

        public DISPID(int value) {
            super(value);
        }
    }

    public static class DISPIDByReference extends ByReference {
        public DISPIDByReference() {
            this(new DISPID(0));
        }

        public DISPIDByReference(DISPID value) {
            super(DISPID.SIZE);
            setValue(value);
        }

        public void setValue(DISPID value) {
            getPointer().setInt(0, value.intValue());
        }

        public DISPID getValue() {
            return new DISPID(getPointer().getInt(0));
        }
    }

    public static class MEMBERID extends DISPID {
        private static final long serialVersionUID = 1L;

        public MEMBERID() {
            this(0);
        }

        public MEMBERID(int value) {
            super(value);
        }
    }

    public static class MEMBERIDByReference extends ByReference {
        public MEMBERIDByReference() {
            this(new MEMBERID(0));
        }

        public MEMBERIDByReference(MEMBERID value) {
            super(MEMBERID.SIZE);
            setValue(value);
        }

        public void setValue(MEMBERID value) {
            getPointer().setInt(0, value.intValue());
        }

        public MEMBERID getValue() {
            return new MEMBERID(getPointer().getInt(0));
        }
    }

    // The Collect property. You use this property if the method you are calling
    // through Invoke is an accessor function.
    /** The Constant DISPID_COLLECT. */
    public final static DISPID DISPID_COLLECT = new DISPID(-8);

    // The C++ constructor function for the object.
    /** The Constant DISPID_CONSTRUCTOR. */
    public final static DISPID DISPID_CONSTRUCTOR = new DISPID(-6);

    // The C++ destructor function for the object.
    /** The Constant DISPID_DESTRUCTOR. */
    public final static DISPID DISPID_DESTRUCTOR = new DISPID(-7);

    // The Evaluate method. This method is implicitly invoked when the ActiveX
    // client encloses the arguments in square brackets. For example, the
    // following two lines are equivalent:
    /** The Constant DISPID_EVALUATE. */
    public final static DISPID DISPID_EVALUATE = new DISPID(-5);

    // The _NewEnum property. This special, restricted property is required for
    // collection objects. It returns an enumerator object that supports
    // IEnumVARIANT, and should have the restricted attribute specified.
    /** The Constant DISPID_NEWENUM. */
    public final static DISPID DISPID_NEWENUM = new DISPID(-4);

    // The parameter that receives the value of an assignment in a PROPERTYPUT.
    /** The Constant DISPID_PROPERTYPUT. */
    public final static DISPID DISPID_PROPERTYPUT = new DISPID(-3);

    // The value returned by IDispatch::GetIDsOfNames to indicate that a member
    // or parameter name was not found.
    /** The Constant DISPID_UNKNOWN. */
    public final static DISPID DISPID_UNKNOWN = new DISPID(-1);

    // The default member for the object. This property or method is invoked
    // when an ActiveX client specifies the object name without a property or
    // method.
    /** The Constant DISPID_VALUE. */
    public final static DISPID DISPID_VALUE = new DISPID(0);

    public final static MEMBERID MEMBERID_NIL = new MEMBERID(
            DISPID_UNKNOWN.intValue());

    /** An array that is allocated on the stack. */
    public final static int FADF_AUTO = 0x0001;

    /** An array that is statically allocated. */
    public final static int FADF_STATIC = 0x0002;

    /** An array that is embedded in a structure. */
    public final static int FADF_EMBEDDED = 0x0004;

    /** An array that may not be resized or reallocated. */
    public final static int FADF_FIXEDSIZE = 0x0010;

    /**
     * An array that contains records. When set, there will be a pointer to the
     * IRecordInfo interface at negative offset 4 in the array descriptor.
     */
    public final static int FADF_RECORD = 0x0020;

    /**
     * An array that has an IID identifying interface. When set, there will be a
     * GUID at negative offset 16 in the safe array descriptor. Flag is set only
     * when FADF_DISPATCH or FADF_UNKNOWN is also set.
     */
    public final static int FADF_HAVEIID = 0x0040;

    /**
     * An array that has a variant type. The variant type can be retrieved with
     * SafeArrayGetVartype.
     */
    public final static int FADF_HAVEVARTYPE = 0x0080;

    /** An array of BSTRs. */
    public final static int FADF_BSTR = 0x0100;

    /** An array of IUnknown*. */
    public final static int FADF_UNKNOWN = 0x0200;

    /** An array of IDispatch*. */
    public final static int FADF_DISPATCH = 0x0400;

    /** An array of VARIANTs. */
    public final static int FADF_VARIANT = 0x0800;

    /** Bits reserved for future use. */
    public final static int FADF_RESERVED = 0xF008;

    @FieldOrder({"value"})
    public static class TYPEKIND extends Structure {
        public static class ByReference extends TYPEKIND implements
                Structure.ByReference {
            public ByReference() {

            }

            public ByReference(int value) {
                super(value);
            }

            public ByReference(TYPEKIND typekind) {
                super(typekind.getPointer());
                value = typekind.value;
            }
        }

        public int value;

        public TYPEKIND() {
            super();
        }

        public TYPEKIND(int value) {
            this.value = value;
        }

        public TYPEKIND(Pointer pointer) {
            super(pointer);
            this.read();
        }

        // / <i>native declaration : line 4</i>
        public static final int TKIND_ENUM = 0;
        // / <i>native declaration : line 5</i>
        public static final int TKIND_RECORD = TYPEKIND.TKIND_ENUM + 1;
        // / <i>native declaration : line 6</i>
        public static final int TKIND_MODULE = TYPEKIND.TKIND_RECORD + 1;
        // / <i>native declaration : line 7</i>
        public static final int TKIND_INTERFACE = TYPEKIND.TKIND_MODULE + 1;
        // / <i>native declaration : line 8</i>
        public static final int TKIND_DISPATCH = TYPEKIND.TKIND_INTERFACE + 1;
        // / <i>native declaration : line 9</i>
        public static final int TKIND_COCLASS = TYPEKIND.TKIND_DISPATCH + 1;
        // / <i>native declaration : line 10</i>
        public static final int TKIND_ALIAS = TYPEKIND.TKIND_COCLASS + 1;
        // / <i>native declaration : line 11</i>
        public static final int TKIND_UNION = TYPEKIND.TKIND_ALIAS + 1;
        // / <i>native declaration : line 12</i>
        public static final int TKIND_MAX = TYPEKIND.TKIND_UNION + 1;
    }

    @FieldOrder({"value"})
    public static class DESCKIND extends Structure {
        public static class ByReference extends DESCKIND implements
                Structure.ByReference {
        }

        public int value;

        public DESCKIND() {
            super();
        }

        public DESCKIND(int value) {
            this.value = value;
        }

        public DESCKIND(Pointer pointer) {
            super(pointer);
            this.read();
        }

        // / <i>native declaration : line 4</i>
        public static final int DESCKIND_NONE = 0;
        // / <i>native declaration : line 5</i>
        public static final int DESCKIND_FUNCDESC = DESCKIND.DESCKIND_NONE + 1;
        // / <i>native declaration : line 6</i>
        public static final int DESCKIND_VARDESC = DESCKIND.DESCKIND_FUNCDESC + 1;
        // / <i>native declaration : line 7</i>
        public static final int DESCKIND_TYPECOMP = DESCKIND.DESCKIND_VARDESC + 1;
        // / <i>native declaration : line 8</i>
        public static final int DESCKIND_IMPLICITAPPOBJ = DESCKIND.DESCKIND_TYPECOMP + 1;
        // / <i>native declaration : line 9</i>
        public static final int DESCKIND_MAX = DESCKIND.DESCKIND_IMPLICITAPPOBJ + 1;
    }

    /**
     * Implementation of SAFEARRAY. Implements Closable, which in this case
     * delegates to destroy, to free native memory on close.
     *
     * <p>VARTYPE for the SAFEARRAY can be:</p>
     *
     * <ul>
     * <li>VT_BOOL</li>
     * <li>VT_BSTR</li>
     * <li>VT_CY</li>
     * <li>VT_DATE</li>
     * <li>VT_DECIMAL</li>
     * <li>VT_DISPATCH</li>
     * <li>VT_ERROR</li>
     * <li>VT_I1</li>
     * <li>VT_I2</li>
     * <li>VT_I4</li>
     * <li>VT_INT</li>
     * <li>VT_R4</li>
     * <li>VT_R8</li>
     * <li>VT_RECORD</li>
     * <li>VT_UI1</li>
     * <li>VT_UI2</li>
     * <li>VT_UI4</li>
     * <li>VT_UINT</li>
     * <li>VT_UNKNOWN</li>
     * <li>VT_VARIANT</li>
     * </ul>
     *
     * <p>General comment: All indices in the helper methods use java int.</p>
     *
     * <p>The native type for the indices is LONG, which is defined as:</p>
     *
     * <blockquote>A 32-bit signed integer. The range is �2147483648 through 2147483647 decimal.</blockquote>
     */
    @FieldOrder({"cDims", "fFeatures", "cbElements", "cLocks", "pvData", "rgsabound"})
    public static class SAFEARRAY extends Structure implements Closeable {

        public static class ByReference extends SAFEARRAY implements
                Structure.ByReference {
        }

        public USHORT cDims;
        public USHORT fFeatures;
        public ULONG cbElements;
        public ULONG cLocks;
        public PVOID pvData;

        /** The rgsabound. */
        public SAFEARRAYBOUND[] rgsabound = { new SAFEARRAYBOUND() };

        public SAFEARRAY() {
            super();
        }

        public SAFEARRAY(Pointer pointer) {
            super(pointer);
            this.read();
        }

        @Override
        public void read() {
            super.read();
            if(cDims.intValue() > 0) {
                rgsabound = (SAFEARRAYBOUND[]) rgsabound[0].toArray(cDims.intValue());
            } else {
                rgsabound = new SAFEARRAYBOUND[]{ new SAFEARRAYBOUND() };
            }
        }

        /**
         * Create a SAFEARRAY with supplied VARIANT as element type.
         *
         * <p>
         * This helper creates a basic SAFEARRAY with a base type of VT_VARIANT.
         * The array will have as many dimensions as parameters are passed in.
         * The lowerbound for each dimension is set to zero, the count to the
         * parameter value.</p>
         *
         * @param size array of dimension size
         * @return SAFEARRAYWrapper or {@code NULL} if creation fails.
         */
        public static SAFEARRAY.ByReference createSafeArray(int... size) {
            return createSafeArray(new WTypes.VARTYPE(Variant.VT_VARIANT), size);
        }

        /**
         * Create a SAFEARRAY with supplied element type.
         *
         * <p>
         * The array will have as many dimensions as parameters are passed in.
         * The lowerbound for each dimension is set to zero, the count to the
         * parameter value.</p>
         *
         * @param vartype type of array contents (see Variant.VT_* constants)
         * @param size array of dimension size
         * @return SAFEARRAYWrapper or {@code NULL} if creation fails.
         */
        public static SAFEARRAY.ByReference createSafeArray(VARTYPE vartype, int... size) {
            OaIdl.SAFEARRAYBOUND[] rgsabound = (OaIdl.SAFEARRAYBOUND[]) new OaIdl.SAFEARRAYBOUND().toArray(size.length);
            for (int i = 0; i < size.length; i++) {
                rgsabound[i].lLbound = new WinDef.LONG(0);
                rgsabound[i].cElements = new WinDef.ULONG(size[size.length - i - 1]);
            }
            SAFEARRAY.ByReference data = OleAuto.INSTANCE.SafeArrayCreate(vartype, new WinDef.UINT(size.length), rgsabound);
            return data;
        }

        /**
         * Set value at {@code indices} in {@code array} to arg.
         *
         * <p>
         * The supplied argument is copied into the array. If the value is no
         * longer needed, it needs to be freed if not handled automatically.</p>
         *
         * @param indices the index, order follows java/C convention
         * @param arg the arg
         */
        public void putElement(Object arg, int... indices) {
            WinDef.LONG[] paramIndices = new WinDef.LONG[indices.length];
            for (int i = 0; i < indices.length; i++) {
                paramIndices[i] = new WinDef.LONG(indices[indices.length - i - 1]);
            }

            WinNT.HRESULT hr;
            Memory mem;
            switch (getVarType().intValue()) {
                case VT_BOOL:
                    mem = new Memory(2);
                    if(arg instanceof Boolean) {
                        mem.setShort(0, (short) (((Boolean) arg) ? 0xFFFF : 0) );
                    } else {
                        mem.setShort(0, (short) (((Number) arg).intValue() > 0 ? 0xFFFF : 0));
                    }
                    hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, mem);
                    COMUtils.checkRC(hr);
                    break;
                case VT_UI1:
                case VT_I1:
                    mem = new Memory(1);
                    mem.setByte(0, ((Number) arg).byteValue());
                    hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, mem);
                    COMUtils.checkRC(hr);
                    break;
                case VT_UI2:
                case VT_I2:
                    mem = new Memory(2);
                    mem.setShort(0, ((Number) arg).shortValue());
                    hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, mem);
                    COMUtils.checkRC(hr);
                    break;
                case VT_UI4:
                case VT_UINT:
                case VT_I4:
                case VT_INT:
                    mem = new Memory(4);
                    mem.setInt(0, ((Number) arg).intValue());
                    hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, mem);
                    COMUtils.checkRC(hr);
                    break;
                case VT_ERROR:
                    mem = new Memory(4);
                    mem.setInt(0, ((Number) arg).intValue());
                    hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, mem);
                    COMUtils.checkRC(hr);
                    break;
                case VT_R4:
                    mem = new Memory(4);
                    mem.setFloat(0, ((Number) arg).floatValue());
                    hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, mem);
                    COMUtils.checkRC(hr);
                    break;
                case VT_R8:
                    mem = new Memory(8);
                    mem.setDouble(0, ((Number) arg).doubleValue());
                    hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, mem);
                    COMUtils.checkRC(hr);
                    break;
                case VT_DATE:
                    mem = new Memory(8);
                    mem.setDouble(0, ((DATE) arg).date);
                    hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, mem);
                    COMUtils.checkRC(hr);
                    break;
                case VT_BSTR:
                    if(arg instanceof String) {
                        BSTR bstr = OleAuto.INSTANCE.SysAllocString((String) arg);
                        hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, bstr.getPointer());
                        OleAuto.INSTANCE.SysFreeString(bstr);
                        COMUtils.checkRC(hr);
                    } else {
                        hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, ((BSTR) arg).getPointer());
                        COMUtils.checkRC(hr);
                    }
                    break;
                case VT_VARIANT:
                    hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, ((VARIANT) arg).getPointer());
                    COMUtils.checkRC(hr);
                    break;
                case VT_UNKNOWN:
                    hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, ((Unknown) arg).getPointer());
                    COMUtils.checkRC(hr);
                    break;
                case VT_DISPATCH:
                    hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, ((Dispatch) arg).getPointer());
                    COMUtils.checkRC(hr);
                    break;
                case VT_CY:
                    hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, ((CURRENCY) arg).getPointer());
                    COMUtils.checkRC(hr);
                    break;
                case VT_DECIMAL:
                    hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, ((DECIMAL) arg).getPointer());
                    COMUtils.checkRC(hr);
                    break;
                case VT_RECORD:
                default:
                    throw new IllegalStateException("Can't parse array content - type not supported: " + getVarType().intValue());
            }
        }

        /**
         * Retrieve the value at the referenced index from the SAFEARRAY.
         *
         * <p>The function creates a copy of the value. The values are
         * allocated with native functions and need to be freed accordingly.</p>
         *
         * @param indices the index, order follows java/C convention
         * @return the variant
         */
        public Object getElement(int... indices) {
            WinDef.LONG[] paramIndices = new WinDef.LONG[indices.length];
            for (int i = 0; i < indices.length; i++) {
                paramIndices[i] = new WinDef.LONG(indices[indices.length - i - 1]);
            }

            Object result;
            WinNT.HRESULT hr;
            Memory mem;
            PointerByReference pbr;
            switch (getVarType().intValue()) {
                case VT_BOOL:
                    mem = new Memory(2);
                    hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, mem);
                    COMUtils.checkRC(hr);
                    result = mem.getShort(0) != 0;
                    break;
                case VT_UI1:
                case VT_I1:
                    mem = new Memory(1);
                    hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, mem);
                    COMUtils.checkRC(hr);
                    result = mem.getByte(0);
                    break;
                case VT_UI2:
                case VT_I2:
                    mem = new Memory(2);
                    hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, mem);
                    COMUtils.checkRC(hr);
                    result = mem.getShort(0);
                    break;
                case VT_UI4:
                case VT_UINT:
                case VT_I4:
                case VT_INT:
                    mem = new Memory(4);
                    hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, mem);
                    COMUtils.checkRC(hr);
                    result = mem.getInt(0);
                    break;
                case VT_ERROR:
                    mem = new Memory(4);
                    hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, mem);
                    COMUtils.checkRC(hr);
                    result = new SCODE(mem.getInt(0));
                    break;
                case VT_R4:
                    mem = new Memory(4);
                    hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, mem);
                    COMUtils.checkRC(hr);
                    result = mem.getFloat(0);
                    break;
                case VT_R8:
                    mem = new Memory(8);
                    hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, mem);
                    COMUtils.checkRC(hr);
                    result = mem.getDouble(0);
                    break;
                case VT_DATE:
                    mem = new Memory(8);
                    hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, mem);
                    COMUtils.checkRC(hr);
                    result = new DATE(mem.getDouble(0));
                    break;
                case VT_BSTR:
                    pbr = new PointerByReference();
                    hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, pbr.getPointer());
                    COMUtils.checkRC(hr);
                    BSTR bstr = new BSTR(pbr.getValue());
                    result = bstr.getValue();
                    OleAuto.INSTANCE.SysFreeString(bstr);
                    break;
                case VT_VARIANT:
                    VARIANT holder = new Variant.VARIANT();
                    hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, holder.getPointer());
                    COMUtils.checkRC(hr);
                    result = holder;
                    break;
                case VT_UNKNOWN:
                    pbr = new PointerByReference();
                    hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, pbr.getPointer());
                    COMUtils.checkRC(hr);
                    result = new Unknown(pbr.getValue());
                    break;
                case VT_DISPATCH:
                    pbr = new PointerByReference();
                    hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, pbr.getPointer());
                    COMUtils.checkRC(hr);
                    result = new Dispatch(pbr.getValue());
                    break;
                case VT_CY:
                    CURRENCY currency = new CURRENCY();
                    hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, currency.getPointer());
                    COMUtils.checkRC(hr);
                    result = currency;
                    break;
                case VT_DECIMAL:
                    DECIMAL decimal = new DECIMAL();
                    hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, decimal.getPointer());
                    COMUtils.checkRC(hr);
                    result = decimal;
                    break;
                case VT_RECORD:
                default:
                    throw new IllegalStateException("Can't parse array content - type not supported: " + getVarType().intValue());
            }

            return result;
        }

        /**
         * Retrieve pointer to data element from array.
         *
         * <p>
         * Caller is responsible for (un)locking the array via
         * {@link OleAuto#SafeArrayLock} and {@link OleAuto#SafeArrayUnlock} or
         * the helper methods: {@link SAFEARRAY#lock} and
         * {@link SAFEARRAY#unlock}.</p>
         *
         * @param indices the index, order follows java/C convention
         * @return the pointer to the data element
         */
        public Pointer ptrOfIndex(int... indices) {
            WinDef.LONG[] paramIndices = new WinDef.LONG[indices.length];
            for (int i = 0; i < indices.length; i++) {
                paramIndices[i] = new WinDef.LONG(indices[indices.length - i - 1]);
            }
            PointerByReference pbr = new PointerByReference();
            WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayPtrOfIndex(this, paramIndices, pbr);
            COMUtils.checkRC(hr);
            return pbr.getValue();
        }

        /**
         * Destroy the underlying SAFEARRAY and free memory
         */
        public void destroy() {
            WinNT.HRESULT res = OleAuto.INSTANCE.SafeArrayDestroy(this);
            COMUtils.checkRC(res);
        }

        /**
         * Implemented to satisfy Closeable interface, delegates to destroy.
         */
        @Override
        public void close() {
            destroy();
        }

        /**
         * Retrieve lower bound for the selected dimension.
         *
         * <p>As in the all the accessor functions, that index is converted to
         * java conventions.</p>
         *
         * @param dimension zerobased index
         * @return lower bound for the selected dimension
         */
        public int getLBound(int dimension) {
            int targetDimension = getDimensionCount() - dimension;
            WinDef.LONGByReference bound = new WinDef.LONGByReference();
            WinNT.HRESULT res = OleAuto.INSTANCE.SafeArrayGetLBound(this, new WinDef.UINT(targetDimension), bound);
            COMUtils.checkRC(res);
            return bound.getValue().intValue();
        }

        /**
         * Retrieve upper bound for the selected dimension.
         *
         * <p>As in the all the accessor functions, that index is converted to
         * java conventions.</p>
         *
         * @param dimension zerobased index
         * @return upper bound for the selected dimension
         */
        public int getUBound(int dimension) {
            int targetDimension = getDimensionCount() - dimension;
            WinDef.LONGByReference bound = new WinDef.LONGByReference();
            WinNT.HRESULT res = OleAuto.INSTANCE.SafeArrayGetUBound(this, new WinDef.UINT(targetDimension), bound);
            COMUtils.checkRC(res);
            return bound.getValue().intValue();
        }

        /**
         * Return number of dimensions of the SAFEARRAY
         *
         * @return number of dimensions of the SAFEARRAY
         */
        public int getDimensionCount() {
            return OleAuto.INSTANCE.SafeArrayGetDim(this).intValue();
        }

        /**
         * Lock array and retrieve pointer to data
         *
         * @return Pointer to arraydata
         */
        public Pointer accessData() {
            PointerByReference pbr = new PointerByReference();
            WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayAccessData(this, pbr);
            COMUtils.checkRC(hr);
            return pbr.getValue();
        }

        /**
         * Unlock array and invalidate the pointer retrieved via
         * SafeArrayAccessData
         */
        public void unaccessData() {
            WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayUnaccessData(this);
            COMUtils.checkRC(hr);
        }

        /**
         * Increments the lock count of an array, and places a pointer to the
         * array data in pvData of the array descriptor.
         */
        public void lock() {
            WinNT.HRESULT res = OleAuto.INSTANCE.SafeArrayLock(this);
            COMUtils.checkRC(res);
        }

        /**
         * Decrements the lock count of an array so it can be freed or resized
         */
        public void unlock() {
            WinNT.HRESULT res = OleAuto.INSTANCE.SafeArrayUnlock(this);
            COMUtils.checkRC(res);
        }

        /**
         * Changes the right-most (least significant) bound of the specified
         * safe array.
         *
         * @param cElements
         * @param lLbound
         */
        public void redim(int cElements, int lLbound) {
            WinNT.HRESULT res = OleAuto.INSTANCE.SafeArrayRedim(this, new OaIdl.SAFEARRAYBOUND(cElements, lLbound));
            COMUtils.checkRC(res);
        }

        /**
         * Return VARTYPE of the SAFEARRAY
         *
         * @return VARTYPE of the SAFEARRAY
         */
        public VARTYPE getVarType() {
            WTypes.VARTYPEByReference resultHolder = new WTypes.VARTYPEByReference();
            WinNT.HRESULT res = OleAuto.INSTANCE.SafeArrayGetVartype(this, resultHolder);
            COMUtils.checkRC(res);
            return resultHolder.getValue();
        }

        /**
         * Get size of one element in bytes
         *
         * @return element size in bytes
         */
        public long getElemsize() {
            return OleAuto.INSTANCE.SafeArrayGetElemsize(this).longValue();
        }
    }

    @FieldOrder({"pSAFEARRAY"})
    public static class SAFEARRAYByReference extends Structure implements Structure.ByReference {

        public SAFEARRAYByReference() {
        }

        public SAFEARRAYByReference(Pointer p) {
            super(p);
            read();
        }

        public SAFEARRAYByReference(SAFEARRAY.ByReference safeArray) {
            pSAFEARRAY = safeArray;
        }

        public SAFEARRAY.ByReference pSAFEARRAY;
    }

    @FieldOrder({"cElements", "lLbound"})
    public static class SAFEARRAYBOUND extends Structure {
        public static class ByReference extends SAFEARRAYBOUND implements
                Structure.ByReference {
        }

        public ULONG cElements;
        public LONG lLbound;

        public SAFEARRAYBOUND() {
            super();
        }

        public SAFEARRAYBOUND(Pointer pointer) {
            super(pointer);
            this.read();
        }

        public SAFEARRAYBOUND(int cElements, int lLbound) {
            this.cElements = new ULONG(cElements);
            this.lLbound = new LONG(lLbound);
            this.write();
        }
    }

    public static class CURRENCY extends Union {

        public static class ByReference extends CURRENCY implements
                Structure.ByReference {
        };

        public _CURRENCY currency;
        public LONGLONG int64;

        public CURRENCY() {
            super();
        }

        public CURRENCY(Pointer pointer) {
            super(pointer);
            this.read();
        }

        @FieldOrder({"Lo", "Hi"})
        public static class _CURRENCY extends Structure {
            public ULONG Lo;
            public LONG Hi;

            public _CURRENCY() {
                super();
            }

            public _CURRENCY(Pointer pointer) {
                super(pointer);
                this.read();
            }
        }
    }

    @FieldOrder({"wReserved", "decimal1", "Hi32", "decimal2"})
    public static class DECIMAL extends Structure {
        public static class ByReference extends DECIMAL implements
                Structure.ByReference {
        };


        public static class _DECIMAL1 extends Union {

            public USHORT signscale;
            public _DECIMAL1_DECIMAL decimal1_DECIMAL;

            public _DECIMAL1() {
                this.setType("signscale");
            }

            public _DECIMAL1(Pointer pointer) {
                super(pointer);
                this.setType("signscale");
                this.read();
            }

            @FieldOrder({"scale", "sign"})
            public static class _DECIMAL1_DECIMAL extends Structure {
                public BYTE scale;
                public BYTE sign;

                public _DECIMAL1_DECIMAL() {
                    super();
                }

                public _DECIMAL1_DECIMAL(Pointer pointer) {
                    super(pointer);
                }
            }
        }

        public static class _DECIMAL2 extends Union {
            public ULONGLONG Lo64;
            public _DECIMAL2_DECIMAL decimal2_DECIMAL;

            public _DECIMAL2() {
                this.setType("Lo64");
            }

            public _DECIMAL2(Pointer pointer) {
                super(pointer);
                this.setType("Lo64");
                this.read();
            }

            @FieldOrder({"Lo32", "Mid32"})
            public static class _DECIMAL2_DECIMAL extends Structure {

                public BYTE Lo32;
                public BYTE Mid32;

                public _DECIMAL2_DECIMAL() {
                    super();
                }

                public _DECIMAL2_DECIMAL(Pointer pointer) {
                    super(pointer);
                }
            }
        }

        public short wReserved;
        public _DECIMAL1 decimal1;
        public NativeLong Hi32;
        public _DECIMAL2 decimal2;

        public DECIMAL() {
            super();
        }

        public DECIMAL(Pointer pointer) {
            super(pointer);
        }
    }

    @FieldOrder({"value"})
    public static class SYSKIND extends Structure {
        public static class ByReference extends SYSKIND implements
                Structure.ByReference {
        }

        public int value;
        public SYSKIND() {
            super();
        }

        public SYSKIND(int value) {
            this.value = value;
        }

        public SYSKIND(Pointer pointer) {
            super(pointer);
            this.read();
        }

        public static final int SYS_WIN16 = 0;
        public static final int SYS_WIN32 = SYSKIND.SYS_WIN16 + 1;
        public static final int SYS_MAC = SYSKIND.SYS_WIN32 + 1;
        public static final int SYS_WIN64 = SYSKIND.SYS_MAC + 1;
    }

    @FieldOrder({"value"})
    public static class LIBFLAGS extends Structure {
        public static class ByReference extends LIBFLAGS implements
                Structure.ByReference {
        }

        public int value;

        public LIBFLAGS() {
            super();
        }

        public LIBFLAGS(int value) {
            this.value = value;
        }

        public LIBFLAGS(Pointer pointer) {
            super(pointer);
            this.read();
        }

        public static final int LIBFLAG_FRESTRICTED = 0x1;
        public static final int LIBFLAG_FCONTROL = 0x2;
        public static final int LIBFLAG_FHIDDEN = 0x4;
        public static final int LIBFLAG_FHASDISKIMAGE = 0x8;
    };

    @FieldOrder({"guid", "lcid", "syskind", "wMajorVerNum", "wMinorVerNum", "wLibFlags"})
    public static class TLIBATTR extends Structure {
        public static class ByReference extends TLIBATTR implements
                Structure.ByReference {

            public ByReference() {
                super();
            }

            public ByReference(Pointer pointer) {
                super(pointer);
                this.read();
            }
        }

        public GUID guid;
        public LCID lcid;
        public SYSKIND syskind;
        public WORD wMajorVerNum;
        public WORD wMinorVerNum;
        public WORD wLibFlags;

        public TLIBATTR() {
            super();
        }

        public TLIBATTR(Pointer pointer) {
            super(pointer);
            this.read();
        }
    }

    public static class BINDPTR extends Union {
        public static class ByReference extends BINDPTR implements
                Structure.ByReference {
        };

        // / C type : FUNCDESC*
        public FUNCDESC lpfuncdesc;
        // / C type : VARDESC*
        public VARDESC lpvardesc;
        // / C type : ITypeComp*
        public TypeComp lptcomp;

        public BINDPTR() {
            super();
        }

        // / @param lpvardesc C type : VARDESC*
        public BINDPTR(VARDESC lpvardesc) {
            super();
            this.lpvardesc = lpvardesc;
            setType(VARDESC.class);
        }

        // / @param lptcomp C type : ITypeComp*
        public BINDPTR(TypeComp lptcomp) {
            super();
            this.lptcomp = lptcomp;
            setType(TypeComp.class);
        }

        // / @param lpfuncdesc C type : FUNCDESC*
        public BINDPTR(FUNCDESC lpfuncdesc) {
            super();
            this.lpfuncdesc = lpfuncdesc;
            setType(FUNCDESC.class);
        }
    }

    @FieldOrder({"memid", "lprgscode", "lprgelemdescParam", "funckind",
        "invkind", "callconv", "cParams", "cParamsOpt", "oVft", "cScodes",
        "elemdescFunc", "wFuncFlags"})
    public static class FUNCDESC extends Structure {
        public static class ByReference extends FUNCDESC implements
                Structure.ByReference {
        };

        public MEMBERID memid;
        public ScodeArg.ByReference lprgscode;
        public ElemDescArg.ByReference lprgelemdescParam;
        public FUNCKIND funckind;
        public INVOKEKIND invkind;
        public CALLCONV callconv;
        public SHORT cParams;
        public SHORT cParamsOpt;
        public SHORT oVft;
        public SHORT cScodes;
        public ELEMDESC elemdescFunc;
        public WORD wFuncFlags;

        public FUNCDESC() {
            super();
        }

        public FUNCDESC(Pointer pointer) {
            super(pointer);
            this.read();

            if (this.cParams.shortValue() > 1) {
                this.lprgelemdescParam.elemDescArg = new ELEMDESC[this.cParams
                        .shortValue()];
                this.lprgelemdescParam.read();
            }
        }
    }

    @FieldOrder({"elemDescArg"})
    public static class ElemDescArg extends Structure {
        public static class ByReference extends ElemDescArg implements
                Structure.ByReference {
        }

        public ELEMDESC[] elemDescArg = { new ELEMDESC() };

        public ElemDescArg() {
            super();
        }

        public ElemDescArg(Pointer pointer) {
            super(pointer);
            this.read();
        }
    }

    @FieldOrder({"scodeArg"})
    public static class ScodeArg extends Structure {
        public static class ByReference extends ScodeArg implements
                Structure.ByReference {
        }

        public SCODE[] scodeArg = { new SCODE() };

        public ScodeArg() {
            super();
        }

        public ScodeArg(Pointer pointer) {
            super(pointer);
            this.read();
        }
    }

    @FieldOrder({"memid", "lpstrSchema", "_vardesc", "elemdescVar", "wVarFlags", "varkind"})
    public class VARDESC extends Structure {
        public static class ByReference extends VARDESC implements
                Structure.ByReference {
        };

        // / C type : MEMBERID
        public MEMBERID memid;
        // / C type : LPOLESTR
        public LPOLESTR lpstrSchema;
        /**
         * [switch_is][switch_type]<br>
         * C type : _VARDESC_union
         */
        public _VARDESC _vardesc;
        // / C type : ELEMDESC
        public ELEMDESC elemdescVar;
        public WORD wVarFlags;
        // / C type : VARKIND
        public VARKIND varkind;

        // / <i>native declaration : line 6</i>
        // / <i>native declaration : line 6</i>
        public static class _VARDESC extends Union {
            public static class ByReference extends _VARDESC implements
                    Structure.ByReference {
            };

            // / [case()]
            public NativeLong oInst;
            /**
             * [case()]<br>
             * C type : VARIANT*
             */
            public VARIANT.ByReference lpvarValue;

            public _VARDESC() {
            }

            public _VARDESC(Pointer pointer) {
                super(pointer);
            }

            /**
             * @param lpvarValue
             *            [case()]<br>
             *            C type : VARIANT*
             */
            public _VARDESC(VARIANT.ByReference lpvarValue) {
                this.lpvarValue = lpvarValue;
                setType("lpvarValue");
            }

            // / @param oInst [case()]
            public _VARDESC(NativeLong oInst) {
                this.oInst = oInst;
                setType("oInst");
            }
        };

        public VARDESC() {
            super();
        }

        public VARDESC(Pointer pointer) {
            super(pointer);
            this.read();
        }

        @Override
        public void read() {
            readField("varkind");
            this._vardesc.setType(varkind.value == VARKIND.VAR_CONST ? "lpvarValue" : "oInst");
            super.read();
        }
    }

    @FieldOrder({"tdesc", "_elemdesc"})
    public static class ELEMDESC extends Structure {
        public static class ByReference extends ELEMDESC implements
                Structure.ByReference {
        };

        /**
         * the type of the element<br>
         * C type : TYPEDESC
         */
        public TYPEDESC tdesc;
        // / C type : DUMMYUNIONNAMEUnion
        public _ELEMDESC _elemdesc;

        // / <i>native declaration : line 4</i>
        // / <i>native declaration : line 4</i>
        public static class _ELEMDESC extends Union {
            public static class ByReference extends _ELEMDESC implements
                    Structure.ByReference {
            };

            /**
             * info for remoting the element<br>
             * C type : IDLDESC
             */
            public IDLDESC idldesc;
            /**
             * info about the parameter<br>
             * C type : PARAMDESC
             */
            public PARAMDESC paramdesc;

            public _ELEMDESC() {
            }

            public _ELEMDESC(Pointer pointer) {
                super(pointer);
                setType("paramdesc");
                this.read();
            }

            /**
             * @param paramdesc
             *            info about the parameter<br>
             *            C type : PARAMDESC
             */
            public _ELEMDESC(PARAMDESC paramdesc) {
                this.paramdesc = paramdesc;
                setType("paramdesc");
            }

            /**
             * @param idldesc
             *            info for remoting the element<br>
             *            C type : IDLDESC
             */
            public _ELEMDESC(IDLDESC idldesc) {
                this.idldesc = idldesc;
                setType("idldesc");
            }
        };

        public ELEMDESC() {
            super();
        }

        public ELEMDESC(Pointer pointer) {
            super(pointer);
            this.read();
        }
    }

    @FieldOrder({"value"})
    public static class FUNCKIND extends Structure {
        public static class ByReference extends FUNCKIND implements
                Structure.ByReference {
        };

        // / <i>native declaration : line 20</i>
        public static final int FUNC_VIRTUAL = 0;
        // / <i>native declaration : line 21</i>
        public static final int FUNC_PUREVIRTUAL = FUNC_VIRTUAL + 1;
        // / <i>native declaration : line 22</i>
        public static final int FUNC_NONVIRTUAL = FUNC_PUREVIRTUAL + 1;
        // / <i>native declaration : line 23</i>
        public static final int FUNC_STATIC = FUNC_NONVIRTUAL + 1;
        // / <i>native declaration : line 24</i>
        public static final int FUNC_DISPATCH = FUNC_STATIC + 1;

        public int value;

        public FUNCKIND() {
            super();
        }

        public FUNCKIND(int value) {
            this.value = value;

        }
    }

    @FieldOrder({"value"})
    public static class INVOKEKIND extends Structure {
        public static class ByReference extends INVOKEKIND implements
                Structure.ByReference {
        }

        // / <i>native declaration : line 30</i>
        public static final INVOKEKIND INVOKE_FUNC = new INVOKEKIND(1);
        // / <i>native declaration : line 31</i>
        public static final INVOKEKIND INVOKE_PROPERTYGET = new INVOKEKIND(2);
        // / <i>native declaration : line 32</i>
        public static final INVOKEKIND INVOKE_PROPERTYPUT = new INVOKEKIND(4);
        // / <i>native declaration : line 33</i>
        public static final INVOKEKIND INVOKE_PROPERTYPUTREF = new INVOKEKIND(8);

        public int value;

        public INVOKEKIND() {
            super();
        }

        public INVOKEKIND(int value) {
            this.value = value;

        }
    }

    @FieldOrder({"value"})
    public static class CALLCONV extends Structure {
        public static class ByReference extends CALLCONV implements
                Structure.ByReference {
        }

        // / <i>native declaration : line 4</i>
        public static final int CC_FASTCALL = 0;
        // / <i>native declaration : line 5</i>
        public static final int CC_CDECL = 1;
        // / <i>native declaration : line 6</i>
        public static final int CC_MSCPASCAL = CALLCONV.CC_CDECL + 1;
        // / <i>native declaration : line 7</i>
        public static final int CC_PASCAL = CALLCONV.CC_MSCPASCAL;
        // / <i>native declaration : line 8</i>
        public static final int CC_MACPASCAL = CALLCONV.CC_PASCAL + 1;
        // / <i>native declaration : line 9</i>
        public static final int CC_STDCALL = CALLCONV.CC_MACPASCAL + 1;
        // / <i>native declaration : line 10</i>
        public static final int CC_FPFASTCALL = CALLCONV.CC_STDCALL + 1;
        // / <i>native declaration : line 11</i>
        public static final int CC_SYSCALL = CALLCONV.CC_FPFASTCALL + 1;
        // / <i>native declaration : line 12</i>
        public static final int CC_MPWCDECL = CALLCONV.CC_SYSCALL + 1;
        // / <i>native declaration : line 13</i>
        public static final int CC_MPWPASCAL = CALLCONV.CC_MPWCDECL + 1;
        // / <i>native declaration : line 14</i>
        public static final int CC_MAX = CALLCONV.CC_MPWPASCAL + 1;

        public int value;

        public CALLCONV() {
        }

        public CALLCONV(int value) {
            this.value = value;
        }
    }

    @FieldOrder({"value"})
    public static class VARKIND extends Structure {
        public static class ByReference extends VARKIND implements
                Structure.ByReference {
        };

        // / <i>native declaration : line 4</i>
        public static final int VAR_PERINSTANCE = 0;
        // / <i>native declaration : line 5</i>
        public static final int VAR_STATIC = VAR_PERINSTANCE + 1;
        // / <i>native declaration : line 6</i>
        public static final int VAR_CONST = VAR_STATIC + 1;
        // / <i>native declaration : line 7</i>
        public static final int VAR_DISPATCH = VAR_CONST + 1;

        public int value;

        public VARKIND() {
            super();
        }

        public VARKIND(int value) {
            this.value = value;
        }
    }

    @FieldOrder({"_typedesc", "vt"})
    public static class TYPEDESC extends Structure {
        public static class ByReference extends TYPEDESC implements
                Structure.ByReference {
        };


        public static class _TYPEDESC extends Union {
            /**
             * [case()]<br>
             * C type : tagTYPEDESC*
             */
            public TYPEDESC.ByReference lptdesc;
            /**
             * [case()]<br>
             * C type : tagARRAYDESC*
             */
            public ARRAYDESC.ByReference lpadesc;
            /**
             * [case()]<br>
             * C type : HREFTYPE
             */
            public HREFTYPE hreftype;

            public _TYPEDESC() {
            }

            public _TYPEDESC(Pointer pointer) {
                super(pointer);
            }

            public TYPEDESC.ByReference getLptdesc() {
                this.setType("lptdesc");
                this.read();
                return this.lptdesc;
            }

            public ARRAYDESC.ByReference getLpadesc() {
                this.setType("lpadesc");
                this.read();
                return this.lpadesc;
            }

            public HREFTYPE getHreftype() {
                this.setType("hreftype");
                this.read();
                return this.hreftype;
            }
        };

        public _TYPEDESC _typedesc;
        public VARTYPE vt;

        public TYPEDESC() {
        }

        public TYPEDESC(Pointer pointer) {
            super(pointer);
        }

        public TYPEDESC(_TYPEDESC _typedesc, VARTYPE vt) {
            this._typedesc = _typedesc;
            this.vt = vt;
        }

        @Override
        public void read() {
            readField("vt");
            switch (vt.intValue()) {
                case Variant.VT_PTR:
                case Variant.VT_SAFEARRAY:
                    _typedesc.setType("lptdesc");
                    break;
                case Variant.VT_CARRAY:
                    _typedesc.setType("lpadesc");
                    break;
                case Variant.VT_USERDEFINED:
                default:
                    _typedesc.setType("hreftype");
                    break;
            }
            super.read();
        }
    }

    @FieldOrder({"dwReserved", "wIDLFlags"})
    public static class IDLDESC extends Structure {
        public static class ByReference extends IDLDESC implements
                Structure.ByReference {

            public ByReference() {
                super();
            }

            public ByReference(IDLDESC idldesc) {
                super(idldesc.dwReserved, idldesc.wIDLFlags);
            }
        };

        // / C type : ULONG_PTR
        public ULONG_PTR dwReserved;
        public USHORT wIDLFlags;

        public IDLDESC() {
            super();
        }

        public IDLDESC(Pointer pointer) {
            super(pointer);
            this.read();
        }

        // / @param dwReserved C type : ULONG_PTR
        public IDLDESC(ULONG_PTR dwReserved, USHORT wIDLFlags) {
            this.dwReserved = dwReserved;
            this.wIDLFlags = wIDLFlags;
        }
    }

    @FieldOrder({"tdescElem", "cDims", "rgbounds"})
    public class ARRAYDESC extends Structure {
        // / C type : TYPEDESC
        public TYPEDESC tdescElem;
        public short cDims;
        /**
         * [size_is]<br>
         * C type : SAFEARRAYBOUND[1]
         */
        public SAFEARRAYBOUND[] rgbounds = { new SAFEARRAYBOUND() };

        public ARRAYDESC() {
            super();
        }

        public ARRAYDESC(Pointer pointer) {
            super(pointer);
            this.read();
        }

        /**
         * @param tdescElem
         *            C type : TYPEDESC<br>
         * @param cDims dimensions
         * @param rgbounds
         *            [size_is]<br>
         *            C type : SAFEARRAYBOUND[1]
         */
        public ARRAYDESC(TYPEDESC tdescElem, short cDims, SAFEARRAYBOUND rgbounds[]) {
            this.tdescElem = tdescElem;
            this.cDims = cDims;
            if (rgbounds.length != this.rgbounds.length)
                throw new IllegalArgumentException("Wrong array size !");
            this.rgbounds = rgbounds;
        }

        public static class ByReference extends ARRAYDESC implements
                Structure.ByReference {

        };
    }

    @FieldOrder({"pparamdescex", "wParamFlags"})
    public static class PARAMDESC extends Structure {
        public static class ByReference extends PARAMDESC implements
                Structure.ByReference {
        }

        // replaced PARAMDESCEX.ByReference with Pointer
        // because of JNA 4 has a problem with ByReference
        public Pointer pparamdescex;
        public USHORT wParamFlags;

        public PARAMDESC() {
            super();
        }

        public PARAMDESC(Pointer pointer) {
            super(pointer);
            this.read();
        }
    }

    @FieldOrder({"cBytes", "varDefaultValue"})
    public static class PARAMDESCEX extends Structure {
        public static class ByReference extends PARAMDESCEX implements
                Structure.ByReference {
        };

        public ULONG cBytes;
        public VariantArg varDefaultValue;

        public PARAMDESCEX() {
            super();
        }

        public PARAMDESCEX(Pointer pointer) {
            super(pointer);
            this.read();
        }
    }

    public static class HREFTYPE extends DWORD {
        private static final long serialVersionUID = 1L;

        public HREFTYPE() {
            super();
        }

        public HREFTYPE(long value) {
            super(value);
        }
    }

    public static class HREFTYPEByReference extends DWORDByReference {
        public HREFTYPEByReference() {
            this(new HREFTYPE(0));
        }

        public HREFTYPEByReference(DWORD value) {
            super(value);
        }

        public void setValue(HREFTYPE value) {
            getPointer().setInt(0, value.intValue());
        }

        @Override
        public HREFTYPE getValue() {
            return new HREFTYPE(getPointer().getInt(0));
        }
    }

    @FieldOrder({"guid", "lcid", "dwReserved", "memidConstructor",
        "memidDestructor", "lpstrSchema", "cbSizeInstance", "typekind",
        "cFuncs", "cVars", "cImplTypes", "cbSizeVft", "cbAlignment",
        "wTypeFlags", "wMajorVerNum", "wMinorVerNum", "tdescAlias",
        "idldescType"})
    public class TYPEATTR extends Structure {
        public static class ByReference extends TYPEATTR implements
                Structure.ByReference {
        };

        // / C type : GUID
        public GUID guid;
        // / C type : LCID
        public LCID lcid;
        public DWORD dwReserved;
        // / C type : MEMBERID
        public MEMBERID memidConstructor;
        // / C type : MEMBERID
        public MEMBERID memidDestructor;
        // / C type : LPOLESTR
        public LPOLESTR lpstrSchema;
        public ULONG cbSizeInstance;
        // / C type : TYPEKIND
        public TYPEKIND typekind;
        public WORD cFuncs;
        public WORD cVars;
        public WORD cImplTypes;
        public WORD cbSizeVft;
        public WORD cbAlignment;
        // The type flags. See TYPEFLAGS_...
        public WORD wTypeFlags;
        public WORD wMajorVerNum;
        public WORD wMinorVerNum;
        // / C type : TYPEDESC
        public TYPEDESC tdescAlias;
        // / C type : IDLDESC
        public IDLDESC idldescType;

        public TYPEATTR() {
            super();
        }

        public TYPEATTR(Pointer pointer) {
            super(pointer);
            this.read();
        }

        /**
         * A type description that describes an Application object.
         */
        public final static int TYPEFLAGS_FAPPOBJECT = 0x1;

        /**
         * Instances of the type can be created by ITypeInfo::CreateInstance.
         */
        public final static int TYPEFLAGS_FCANCREATE = 0x2;

        /**
         * The type is licensed.
         */
        public final static int TYPEFLAGS_FLICENSED = 0x4;

        /**
         * The type is predefined. The client application should automatically create a single instance of the object
         * that has this attribute. The name of the variable that points to the object is the same as the class name of
         * the object.
         */
        public final static int TYPEFLAGS_FPREDECLID = 0x8;

        /**
         * The type should not be displayed to browsers.
         */
        public final static int TYPEFLAGS_FHIDDEN = 0x10;

        /**
         * The type is a control from which other types will be derived, and should not be displayed to users.
         */
        public final static int TYPEFLAGS_FCONTROL = 0x20;

        /**
         * The interface supplies both IDispatch and VTBL binding.
         */
        public final static int TYPEFLAGS_FDUAL = 0x40;

        /**
         * The interface cannot add members at run time.
         */
        public final static int TYPEFLAGS_FNONEXTENSIBLE = 0x80;

        /**
         * The types used in the interface are fully compatible with Automation, including VTBL binding support. Setting
         * dual on an interface sets this flag in addition to TYPEFLAG_FDUAL. Not allowed on dispinterfaces.
         */
        public final static int TYPEFLAGS_FOLEAUTOMATION = 0x100;

        /**
         * Should not be accessible from macro languages. This flag is intended for system-level types or types that
         * type browsers should not display.
         */
        public final static int TYPEFLAGS_FRESTRICTED = 0x200;

        /**
         * The class supports aggregation.
         */
        public final static int TYPEFLAGS_FAGGREGATABLE = 0x400;

        /**
         * The type is replaceable.
         */
        public final static int TYPEFLAGS_FREPLACEABLE = 0x800;

        /**
         * Indicates that the interface derives from IDispatch, either directly or indirectly. This flag is computed.
         * There is no Object Description Language for the flag.
         */
        public final static int TYPEFLAGS_FDISPATCHABLE = 0x1000;

        /**
         * The type has reverse binding.
         */
        public final static int TYPEFLAGS_FREVERSEBIND = 0x2000;

        /**
         * Interfaces can be marked with this flag to indicate that they will be using a proxy/stub dynamic link
         * library. This flag specifies that the typelib proxy should not be unregistered when the typelib is
         * unregistered.
         */
        public final static int TYPEFLAGS_FPROXY = 0x4000;
    }
}
