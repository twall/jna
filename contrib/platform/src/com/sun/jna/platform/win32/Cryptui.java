/* Copyright (c) 2018 Roshan Muralidharan, All Rights Reserved
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
package com.sun.jna.platform.win32;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;
import com.sun.jna.Union;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import com.sun.jna.platform.win32.WinCrypt.*;
import com.sun.jna.platform.win32.WinDef.BOOL;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.DWORDByReference;
import com.sun.jna.win32.W32APITypeMapper;

/**
 * Cryptui.dll Interface.
 * @author roshan[dot]muralidharan[at]cerner[dot]com
 * @author Fagner Granella
 */
public interface Cryptui extends StdCallLibrary {

    Cryptui INSTANCE = (Cryptui) Native.load("Cryptui", Cryptui.class, W32APIOptions.UNICODE_OPTIONS);

    public interface ICryptuiWizFlag extends FlagEnum {
    }

    public enum CryptuiWizFlag implements ICryptuiWizFlag {
        /**
         * No UI will be shown, otherwise, user will be prompted by a wizard.
         */
        CRYPTUI_WIZ_NO_UI(0x0001),
        /**
         * Suppress all user interfaces generated by cryptographic service
         * providers (CSPs). This option can be overridden by the
         * CRYPTUI_WIZ_NO_UI_EXCEPT_CSP option.
         */
        CRYPTUI_WIZ_IGNORE_NO_UI_FLAG_FOR_CSPS(0x0002),
        /**
         * Suppress all user interfaces except those generated by CSPs. This
         * option overrides the CRYPTUI_WIZ_IGNORE_NO_UI_FLAG_FOR_CSPS option.
         */
        CRYPTUI_WIZ_NO_UI_EXCEPT_CSP(0x0003);

        private final int flag;

        CryptuiWizFlag(int flag) {
            this.flag = flag;
        }

        @Override
        public int getFlag() {
            return flag;
        }
    }

    public enum CryptuiWizImportFlag implements ICryptuiWizFlag {
        CRYPTUI_WIZ_NO_UI(CryptuiWizFlag.CRYPTUI_WIZ_NO_UI),
        CRYPTUI_WIZ_IGNORE_NO_UI_FLAG_FOR_CSPS(CryptuiWizFlag.CRYPTUI_WIZ_IGNORE_NO_UI_FLAG_FOR_CSPS),
        CRYPTUI_WIZ_NO_UI_EXCEPT_CSP(CryptuiWizFlag.CRYPTUI_WIZ_NO_UI_EXCEPT_CSP),
        /**
         * Allow certificates to be imported.
         */
        CRYPTUI_WIZ_IMPORT_ALLOW_CERT(0x00020000),
        /**
         * Allow CRLs to be imported.
         */
        CRYPTUI_WIZ_IMPORT_ALLOW_CRL(0x00040000),
        /**
         * Allow CTLs to be imported.
         */
        CRYPTUI_WIZ_IMPORT_ALLOW_CTL(0x00080000),
        /**
         * Do not allow the user to change the destination certificate store
         * represented by the hDestCertStore parameter.
         */
        CRYPTUI_WIZ_IMPORT_NO_CHANGE_DEST_STORE(0x00010000),
        /**
         * Import the object to the certificate store for the local computer.
         * This applies only to Personal Information Exchange (PFX) imports.
         */
        CRYPTUI_WIZ_IMPORT_TO_LOCALMACHINE(0x00100000),
        /**
         * Import the object to the certificate store for the current user. This
         * applies only to PFX imports.
         */
        CRYPTUI_WIZ_IMPORT_TO_CURRENTUSER(0x00200000),
        /**
         * Import the object to a remote certificate store. Set this flag if the
         * hDestCertStore parameter represents a remote certificate store.
         */
        CRYPTUI_WIZ_IMPORT_REMOTE_DEST_STORE(0x00400000);

        private final int flag;

        CryptuiWizImportFlag(int flag) {
            this.flag = flag;
        }

        CryptuiWizImportFlag(CryptuiWizFlag flag) {
            this.flag = flag.getFlag();
        }

        @Override
        public int getFlag() {
            return flag;
        }
    }

    public enum CryptuiWizExportFlag implements ICryptuiWizFlag {
        CRYPTUI_WIZ_NO_UI(CryptuiWizFlag.CRYPTUI_WIZ_NO_UI),
        CRYPTUI_WIZ_IGNORE_NO_UI_FLAG_FOR_CSPS(CryptuiWizFlag.CRYPTUI_WIZ_IGNORE_NO_UI_FLAG_FOR_CSPS),
        CRYPTUI_WIZ_NO_UI_EXCEPT_CSP(CryptuiWizFlag.CRYPTUI_WIZ_NO_UI_EXCEPT_CSP),
        /**
         * Skip the Export Private Key page and assume that the private key is
         * to be exported.
         */
        CRYPTUI_WIZ_EXPORT_PRIVATE_KEY(0x0100),
        /**
         * Disable the Delete the private key check box in the Export File
         * Format page.
         */
        CRYPTUI_WIZ_EXPORT_NO_DELETE_PRIVATE_KEY(0x0200);

        private final int flag;

        CryptuiWizExportFlag(int flag) {
            this.flag = flag;
        }

        CryptuiWizExportFlag(CryptuiWizFlag flag) {
            this.flag = flag.getFlag();
        }

        @Override
        public int getFlag() {
            return flag;
        }
    }

    public enum CryptuiWizImportSubjectFlag implements ICryptuiWizFlag {

        /**
         * Import the certificate stored in the file referenced in the
         * pwszFileName member.
         */
        CRYPTUI_WIZ_IMPORT_SUBJECT_FILE(1),
        /**
         * Import the certificate referenced in the pCertContext member.
         */
        CRYPTUI_WIZ_IMPORT_SUBJECT_CERT_CONTEXT(2),
        /**
         * Import the CTL referenced in the pCTLContext member.
         */
        CRYPTUI_WIZ_IMPORT_SUBJECT_CTL_CONTEXT(3),
        /**
         * Import the CRL referenced in the pCRLContext member.
         */
        CRYPTUI_WIZ_IMPORT_SUBJECT_CRL_CONTEXT(4),
        /**
         * Import the certificate store referenced in the hCertStore member.
         */
        CRYPTUI_WIZ_IMPORT_SUBJECT_CERT_STORE(5);

        private final int flag;

        CryptuiWizImportSubjectFlag(int flag) {
            this.flag = flag;
        }

        @Override
        public int getFlag() {
            return flag;
        }

    }

    /**
     * The CryptUIDlgSelectCertificateFromStore function displays a dialog box
     * that allows the selection of a certificate from a specified store.
     *
     * @param hCertStore Handle of the certificate store to be searched.
     * @param hwnd Handle of the window for the display. If NULL, defaults to
     * the desktop window.
     * @param pwszTitle String used as the title of the dialog box. If NULL, the
     * default title, "Select Certificate," is used.
     * @param pwszDisplayString Text statement in the selection dialog box. If
     * NULL, the default phrase, "Select a certificate you want to use," is
     * used.
     * @param dwDontUseColumn Flags that can be combined to exclude columns of
     * the display.
     * @param dwFlags Currently not used and should be set to 0.
     * @param pvReserved Reserved for future use.
     * 
     * @return Returns a pointer to the selected certificate context. If no
     * certificate was selected, NULL is returned. When you have finished using
     * the certificate, free the certificate context by calling the
     * CertFreeCertificateContext function.
     */
    CERT_CONTEXT.ByReference CryptUIDlgSelectCertificateFromStore(HCERTSTORE hCertStore, HWND hwnd, String pwszTitle,
            String pwszDisplayString, int dwDontUseColumn, int dwFlags, PointerType pvReserved);

    /**
     * The CryptUIWizImport function imports a certificate, a certificate trust
     * list (CTL), a certificate revocation list (CRL), or a certificate store
     * to a certificate store. The import can be performed with or without user
     * interaction.
     *
     * @param dwFlags Contains flags that modify the behavior of the function.
     * This can be zero or a combination of one or more of the following values.
     * @param hwndParent Handle of the window for the display. If NULL, defaults
     * to the desktop window.
     * @param pwszWizardTitle A pointer to a null-terminated Unicode string that
     * contains the title to use in the dialog box that this function creates
     * @param pImportSrc A pointer to a CRYPTUI_WIZ_IMPORT_SRC_INFO structure
     * that contains information about the object to import.
     * @param hDestCertStore A handle to the certificate store to import to. If
     * this parameter is NULL and the CRYPTUI_WIZ_NO_UI flag is not set in
     * dwFlags, the wizard will prompt the user to select a certificate store.
     * 
     * @return If the function succeeds, the function returns TRUE. If the
     * function fails, it returns FALSE. For extended error information, call
     * GetLastError function.
     */
    BOOL CryptUIWizImport(DWORD dwFlags, HWND hwndParent, String pwszWizardTitle,
                          CRYPTUI_WIZ_IMPORT_SRC_INFO pImportSrc, HCERTSTORE hDestCertStore);

    /**
     * The CryptUIWizExport function exports a certificate, a certificate trust
     * list (CTL), a certificate revocation list (CRL), or a certificate store
     * to a file. The export can be performed with or without user interaction.
     *
     * @param dwFlags
     * @param hwndParent
     * @param pwszWizardTitle
     * @param pExportInfo
     * @param pvoid Contains information about how to do the export based on
     * what is being exported. See above table for values, if this is non-NULL
     * the values are displayed to the user as the default choices.
     * 
     * @return If the function succeeds, the function returns TRUE. If the
     * function fails, it returns FALSE. For extended error information, call
     * GetLastError function.
     */
    BOOL CryptUIWizExport(DWORDByReference dwFlags, HWND hwndParent, String pwszWizardTitle,
                          CRYPTUI_WIZ_EXPORT_INFO pExportInfo, PointerType pvoid);

    /**
     * The CRYPTUI_WIZ_IMPORT_SRC_INFO structure contains the subject to import
     * into the CryptUIWizImport function. The subject can be a certificate, a
     * certificate trust list (CTL), or a certificate revocation list (CRL).
     */
    @Structure.FieldOrder({"dwSize", "dwSubjectChoice", "certificate", "dwFlags", "pwszPassword"})
    public static class CRYPTUI_WIZ_IMPORT_SRC_INFO extends Structure {
        public static class ByReference extends CRYPTUI_WIZ_IMPORT_SRC_INFO implements Structure.ByReference {
        }

        public static class UNION extends Union {
            public static class ByReference extends UNION implements Structure.ByReference {
            }

            public String pwszFileName;
            public CERT_CONTEXT.ByReference pCertContext;
            public CERT_CONTEXT.ByReference pCTLContext;
            public CERT_CONTEXT.ByReference pCRLContext;
            public HCERTSTORE hCertStore;
        }

        public DWORD cbSize;
        public DWORDByReference dwSubjectChoice;
        public UNION certificate;
        public DWORD dwFlags;
        public String pwszPassword;

        public CRYPTUI_WIZ_IMPORT_SRC_INFO() {
            super(W32APITypeMapper.DEFAULT);
        }
    }

    /**
     * The CRYPTUI_WIZ_EXPORT_INFO structure contains information that controls
     * the operation of the CryptUIWizExport function.
     */
    @Structure.FieldOrder({"dwSize", "pwszExportFileName", "dwSubjectChoice", "certificate", "cStores", "rghStores"})
    public static class CRYPTUI_WIZ_EXPORT_INFO extends Structure {
        public static class ByReference extends CRYPTUI_WIZ_EXPORT_INFO implements Structure.ByReference {
        }

        public static class UNION extends Union {
            public static class ByReference extends UNION implements Structure.ByReference {
            }

            public CERT_CONTEXT.ByReference pCertContext;
            public CERT_CONTEXT.ByReference pCTLContext;
            public CERT_CONTEXT.ByReference pCRLContext;
            public HCERTSTORE hCertStore;
        }

        public DWORD dwSize;
        public String pwszExportFileName;
        public DWORDByReference dwSubjectChoice;
        public UNION certificate;
        public DWORD cStores;
        public Pointer rghStores;

        public CRYPTUI_WIZ_EXPORT_INFO() {
            super(W32APITypeMapper.DEFAULT);
        }
    }

}
