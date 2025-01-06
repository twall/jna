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

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinBase.FILETIME;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/**
 * Provides access to the w32 MSI installer library.
 */
public interface Msi extends StdCallLibrary {

    Msi INSTANCE = Native.load("msi", Msi.class, W32APIOptions.UNICODE_OPTIONS);

    /**
     * The component being requested is disabled on the computer.
     */
    int INSTALLSTATE_NOTUSED = -7;

    /**
     * The configuration data is corrupt.
     */
    int INSTALLSTATE_BADCONFIG = -6;

    /**
     * The installation is suspended or in progress.
     */
    int INSTALLSTATE_INCOMPLETE = -5;

    /**
     * The feature must run from the source, and the source is unavailable.
     */
    int INSTALLSTATE_SOURCEABSENT = -4;

    /**
     * The return buffer is full.
     */
    int INSTALLSTATE_MOREDATA = -3;

    /**
     * An invalid parameter was passed to the function.
     */
    int INSTALLSTATE_INVALIDARG = -2;

    /**
     * An unrecognized product or feature was specified.
     */
    int INSTALLSTATE_UNKNOWN = -1;

    /**
     * The feature is broken.
     */
    int INSTALLSTATE_BROKEN =  0;

    /**
     * The advertised feature.
     */
    int INSTALLSTATE_ADVERTISED =  1;

    /**
     * The component is being removed.
     */
    int INSTALLSTATE_REMOVED =  1;

    /**
     * The feature was uninstalled.
     */
    int INSTALLSTATE_ABSENT =  2;

    /**
     * The feature was installed on the local drive.
     */
    int INSTALLSTATE_LOCAL =  3;

    /**
     * The feature must run from the source, CD-ROM, or network.
     */
    int INSTALLSTATE_SOURCE =  4;

    /**
     * The feature is installed in the default location: local or source.
     */
    int INSTALLSTATE_DEFAULT =  5;

    /**
     * Create a new database, direct mode read/write.
     */
    static final String MSIDBOPEN_CREATEDIRECT = "MSIDBOPEN_CREATEDIRECT";

    /**
     * Create a new database, transact mode read/write.
     */
    static final String MSIDBOPEN_CREATE = "MSIDBOPEN_CREATE";

    /**
     * Open a database direct read/write without transaction.
     */
    static final String MSIDBOPEN_DIRECT = "MSIDBOPEN_DIRECT";

    /**
     * Open a database read-only, no persistent changes.
     */
    static final String MSIDBOPEN_READONLY = "MSIDBOPEN_READONLY";

    /**
     * Open a database read/write in transaction mode.
     */
    static final String MSIDBOPEN_TRANSACT = "MSIDBOPEN_TRANSACT";

    /**
     * Add this flag to indicate a patch file.
     */
    static final String MSIDBOPEN_PATCHFILE = "MSIDBOPEN_PATCHFILE";

    /**
     * The MsiGetComponentPath function returns the full path to an installed component. If the key path for the
     * component is a registry key then the registry key is returned.
     *
     * @param szProduct
     *   Specifies the product code for the client product.
     *
     * @param szComponent
     *   Specifies the component ID of the component to be located.
     *
     * @param lpPathBuf
     *   Pointer to a variable that receives the path to the component. This parameter can be null. If the component is
     *   a registry key, the registry roots are represented numerically. If this is a registry subkey path, there is a
     *   backslash at the end of the Key Path. If this is a registry value key path, there is no backslash at the end.
     *   For example, a registry path on a 32-bit operating system of HKEY_CURRENT_USER\SOFTWARE\Microsoft is returned
     *   as "01:\SOFTWARE\Microsoft\".
     *
     * @param pcchBuf
     *   Pointer to a variable that specifies the size, in characters, of the buffer pointed to by the lpPathBuf
     *   parameter. On input, this is the full size of the buffer, including a space for a terminating null character.
     *   If the buffer passed in is too small, the count returned does not include the terminating null character.
     *
     *   If lpPathBuf is null, pcchBuf can be null.
     *
     * @return
     *   The MsiGetComponentPath function returns the following values.
     *   INSTALLSTATE_NOTUSED - The component being requested is disabled on the computer.
     *   INSTALLSTATE_ABSENT - The component is not installed.
     *   INSTALLSTATE_INVALIDARG - One of the function parameters is invalid.
     *   INSTALLSTATE_LOCAL - The component is installed locally.
     *   INSTALLSTATE_SOURCE - The component is installed to run from source.
     *   INSTALLSTATE_SOURCEABSENT - The component source is inaccessible.
     *   INSTALLSTATE_UNKNOWN - The product code or component ID is unknown.
     */
    int MsiGetComponentPath(String szProduct, String szComponent, char[] lpPathBuf, IntByReference pcchBuf);

    /**
     * The MsiLocateComponent function returns the full path to an installed component without a product code. This
     * function attempts to determine the product using MsiGetProductCode, but is not guaranteed to find the correct
     * product for the caller. MsiGetComponentPath should always be called when possible.
     *
     * @param szComponent
     *   Specifies the component ID of the component to be located.
     *
     * @param lpPathBuf
     *   Pointer to a variable that receives the path to the component. The variable includes the terminating null
     *   character.
     *
     * @param pcchBuf
     *   Pointer to a variable that specifies the size, in characters, of the buffer pointed to by the lpPathBuf
     *   parameter. On input, this is the full size of the buffer, including a space for a terminating null character.
     *   Upon success of the MsiLocateComponent function, the variable pointed to by pcchBuf contains the count of
     *   characters not including the terminating null character. If the size of the buffer passed in is too small, the
     *   function returns INSTALLSTATE_MOREDATA.
     *
     *   If lpPathBuf is null, pcchBuf can be null.
     *
     * @return
     *   The MsiGetComponentPath function returns the following values.
     *   INSTALLSTATE_NOTUSED - The component being requested is disabled on the computer.
     *   INSTALLSTATE_ABSENT - The component is not installed.
     *   INSTALLSTATE_INVALIDARG - One of the function parameters is invalid.
     *   INSTALLSTATE_LOCAL - The component is installed locally.
     *   INSTALLSTATE_MOREDATA - The buffer provided was too small.
     *   INSTALLSTATE_SOURCE - The component is installed to run from source.
     *   INSTALLSTATE_SOURCEABSENT - The component source is inaccessible.
     *   INSTALLSTATE_UNKNOWN - The product code or component ID is unknown.
     */
    int MsiLocateComponent(String szComponent, char[] lpPathBuf, IntByReference pcchBuf);

    /**
     * The MsiGetProductCode function returns the product code of an application by using the component code of an
     * installed or advertised component of the application. During initialization, an application must determine under
     * which product code it has been installed or advertised.
     *
     * @param szComponent
     *   This parameter specifies the component code of a component that has been installed by the application. This
     *   will be typically the component code of the component containing the executable file of the application.
     *
     * @param lpProductBuf
     *   Pointer to a buffer that receives the product code. This buffer must be 39 characters long. The first 38
     *   characters are for the GUID, and the last character is for the terminating null character.
     *
     * @return
     * ERROR_BAD_CONFIGURATION - The configuration data is corrupt.
     * ERROR_INSTALL_FAILURE - The product code could not be determined.
     * ERROR_INVALID_PARAMETER - An invalid parameter was passed to the function.
     * ERROR_SUCCESS - The function completed successfully.
     * ERROR_UNKNOWN_COMPONENT - The specified component is unknown.
     */
    int MsiGetProductCode(String szComponent, char[] lpProductBuf);

    /**
     * The MsiEnumComponents function enumerates the installed components for all products. This function retrieves one
     * component code each time it is called.
     *
     * @param iComponentIndex
     *   Specifies the index of the component to retrieve. This parameter should be zero for the first call to the
     *   MsiEnumComponents function and then incremented for subsequent calls. Because components are not ordered, any
     *   new component has an arbitrary index. This means that the function can return components in any order.
     *
     * @param lpComponentBuf
     *   Pointer to a buffer that receives the component code. This buffer must be 39 characters long. The first 38
     *   characters are for the GUID, and the last character is for the terminating null character.
     *
     * @return
     *  ERROR_BAD_CONFIGURATION - The configuration data is corrupt.
     *  ERROR_INVALID_PARAMETER - An invalid parameter was passed to the function.
     *  ERROR_NO_MORE_ITEMS - There are no components to return.
     *  ERROR_NOT_ENOUGH_MEMORY - The system does not have enough memory to complete the operation. Available with
     *    Windows Server 2003.
     *  ERROR_SUCCESS - A value was enumerated.
     */
    int MsiEnumComponents(WinDef.DWORD iComponentIndex, char[] lpComponentBuf);

    /**
     * The MsiCloseHandle function closes an open installation handle.
     *
     * @param hAny
     *   Specifies any open installation handle.
     *
     * @return
     *   ERROR_INVALID_HANDLE - An invalid handle was passed to the function.
     *   ERROR_SUCCESS - The function succeeded.
     */
    int MsiCloseHandle(Pointer hAny);

    /**
     * The MsiDatabaseOpenView function prepares a database query and creates a view object. This function
     * returns a handle that should be closed using {@link #MsiCloseHandle}.
     *
     * @param hDatabase
     *   Handle to the database to which you want to open a view object.
     *
     * @param szQuery
     *   Specifies a SQL query string for querying the database.
     *
     * @param phView
     *   Pointer to a handle for the returned view.
     *
     * @return
     *  ERROR_BAD_QUERY_SYNTAX - An invalid SQL query string was passed to the function.
     *  ERROR_INVALID_HANDLE - An invalid or inactive handle was supplied.
     *  ERROR_SUCCESS - The function succeeded, and the handle is to a view object.
     */
    int MsiDatabaseOpenView(Pointer hDatabase, String szQuery, PointerByReference phView);

    /**
     * The MsiGetSummaryInformation function obtains a handle to the _SummaryInformation stream for an installer
     * database. This function returns a handle that should be closed using {@link #MsiCloseHandle}.
     *
     * @param hDatabase
     *   Handle to the database.
     *
     * @param szDatabasePath
     *   Specifies the path to the database.
     *
     * @param uiUpdateCount
     *   Specifies the maximum number of updated values.
     *
     * @param phSummaryInfo
     *   Pointer to the location from which to receive the summary information handle.
     *
     * @return
     *   ERROR_INSTALL_PACKAGE_INVALID - The installation package is invalid.
     *   ERROR_INVALID_HANDLE - An invalid or inactive handle was supplied.
     *   ERROR_INVALID_PARAMETER - An invalid parameter was passed to the function.
     *   ERROR_SUCCESS - The function succeeded.
     */
    int MsiGetSummaryInformation(Pointer hDatabase, String szDatabasePath, int uiUpdateCount,
        PointerByReference phSummaryInfo);

    /**
     * The MsiOpenDatabase function opens a database file for data access. This function returns a handle that should be
     * closed using {@link #MsiCloseHandle}.
     *
     * @param szDatabasePath
     *   Specifies the full path or relative path to the database file.
     *
     * @param szPersist
     *   Receives the full path to the file or the persistence mode. You can use the persist parameter to direct the
     *   persistent output to a new file or to specify one of the following predefined persistence modes:
     *   {@link #MSIDBOPEN_CREATEDIRECT} - Create a new database, direct mode read/write.
     *   {@link #MSIDBOPEN_CREATE} - Create a new database, transact mode read/write.
     *   {@link #MSIDBOPEN_DIRECT} - Open a database direct read/write without transaction.
     *   {@link #MSIDBOPEN_READONLY} - Open a database read-only, no persistent changes.
     *   {@link #MSIDBOPEN_TRANSACT} - Open a database read/write in transaction mode.
     *   {@link #MSIDBOPEN_PATCHFILE} - Add this flag to indicate a patch file.
     *
     * @param phDatabase
     *   Pointer to the location of the returned database handle.
     *
     * @return
     *  ERROR_CREATE_FAILED - The database could not be created.
     *  ERROR_INVALID_PARAMETER - One of the parameters was invalid.
     *  ERROR_OPEN_FAILED - The database could not be opened as requested.
     *  ERROR_SUCCESS - The function succeeded.
     */
    int MsiOpenDatabase(String szDatabasePath, String szPersist, PointerByReference phDatabase);

    /**
     * The MsiRecordGetString function returns the string value of a record field.
     *
     * @param hRecord
     *   Handle to the record.
     *
     * @param iField
     *   Specifies the field requested.
     *
     * @param szValueBuf
     *   Pointer to the buffer that receives the null terminated string containing the value of the record field. Do not
     *   attempt to determine the size of the buffer by passing in a null (value=0) for szValueBuf. You can get the size
     *   of the buffer by passing in an empty string (for example ""). The function then returns ERROR_MORE_DATA and
     *   pcchValueBuf contains the required buffer size in TCHARs, not including the terminating null character. On
     *   return of ERROR_SUCCESS, pcchValueBuf contains the number of TCHARs written to the buffer, not including the
     *   terminating null character.
     *
     * @param pcchValueBuf
     *   Pointer to the variable that specifies the size, in TCHARs, of the buffer pointed to by the variable
     *   szValueBuf. When the function returns ERROR_SUCCESS, this variable contains the size of the data copied to
     *   szValueBuf, not including the terminating null character. If szValueBuf is not large enough, the function
     *   returns ERROR_MORE_DATA and stores the required size, not including the terminating null character, in the
     *   variable pointed to by pcchValueBuf.
     *
     * @return
     *   ERROR_INVALID_HANDLE - An invalid or inactive handle was supplied.
     *   ERROR_INVALID_PARAMETER - An invalid parameter was supplied.
     *   ERROR_MORE_DATA - The provided buffer was too small to hold the entire value.
     *   ERROR_SUCCESS - The function succeeded.
     */
    int MsiRecordGetString(Pointer hRecord, int iField, char[] szValueBuf, IntByReference pcchValueBuf);

    /**
     * The MsiSummaryInfoGetProperty function gets a single property from the summary information stream.
     *
     * @param hSummaryInfo
     *   Handle to summary information.
     *
     * @param uiProperty
     *   Specifies the property ID of the summary property. This parameter can be a property ID listed in the Summary
     *   Information Stream Property Set. This function does not return values for PID_DICTIONARY OR PID_THUMBNAIL
     *   property.
     *
     * @param puiDataType
     *   Receives the returned property type. This parameter can be a type listed in the Summary Information Stream
     *   Property Set.
     *
     * @param piValue
     *   Receives the returned integer property data.
     *
     * @param pftValue
     *   Pointer to a file value.
     *
     * @param szValueBuf
     *   Pointer to the buffer that receives the null terminated summary information property value. Do not attempt to
     *   determine the size of the buffer by passing in a null (value=0) for szValueBuf. You can get the size of the
     *   buffer by passing in an empty string (for example ""). The function then returns ERROR_MORE_DATA and
     *   pcchValueBuf contains the required buffer size in TCHARs, not including the terminating null character. On
     *   return of ERROR_SUCCESS, pcchValueBuf contains the number of TCHARs written to the buffer, not including the
     *   terminating null character. This parameter is an empty string if there are no errors.
     *
     * @param pcchValueBuf
     *   Pointer to the variable that specifies the size, in TCHARs, of the buffer pointed to by the variable
     *   szValueBuf. When the function returns ERROR_SUCCESS, this variable contains the size of the data copied to
     *   szValueBuf, not including the terminating null character. If szValueBuf is not large enough, the function
     *   returns ERROR_MORE_DATA and stores the required size, not including the terminating null character, in the
     *   variable pointed to by pcchValueBuf.
     *
     * @return
     *   ERROR_INVALID_HANDLE - An invalid or inactive handle was supplied.
     *   ERROR_INVALID_PARAMETER - An invalid parameter was passed to the function.
     *   ERROR_MORE_DATA - The buffer passed in was too small to hold the entire value.
     *   ERROR_SUCCESS - The function succeeded.
     *   ERROR_UNKNOWN_PROPERTY - The property is unknown.
     */
    int MsiSummaryInfoGetProperty(Pointer hSummaryInfo, int uiProperty, IntByReference puiDataType,
        IntByReference piValue, FILETIME pftValue, char[] szValueBuf, IntByReference pcchValueBuf);

    /**
     * The MsiViewExecute function executes a SQL view query and supplies any required parameters. The query uses
     * the question mark token to represent parameters as described in SQL Syntax. The values of these parameters
     * are passed in as the corresponding fields of a parameter record.
     * <p>
     * Note: In low memory situations, this function can raise a STATUS_NO_MEMORY exception.
     *
     * @param hView
     *   Handle to the view upon which to execute the query.
     *
     * @param hRecord
     *   Handle to a record that supplies the parameters. This parameter contains values to replace the parameter
     *   tokens in the SQL query. It is optional, so hRecord can be zero.
     *
     * @return
     *   ERROR_FUNCTION_FAILED - A view could not be executed.
     *   ERROR_INVALID_HANDLE - An invalid or inactive handle was supplied.
     *   ERROR_SUCCESS - The function succeeded.
     */
    int MsiViewExecute(Pointer hView, Pointer hRecord);

    /**
     * The MsiViewFetch function fetches the next sequential record from the view. This function returns a handle that
     * should be closed using {@link #MsiCloseHandle}.
     * <p>
     * Note: In low memory situations, this function can raise a STATUS_NO_MEMORY exception.
     *
     * @param hView
     *   Handle to the view to fetch from.
     *
     * @param phRecord
     *   Pointer to the handle for the fetched record.
     *
     * @return
     *   ERROR_FUNCTION_FAILED - An error occurred during fetching.
     *   ERROR_INVALID_HANDLE - An invalid or inactive handle was supplied.
     *   ERROR_INVALID_HANDLE_STATE - The handle was in an invalid state.
     *   ERROR_NO_MORE_ITEMS - No records remain, and a null handle is returned.
     *   ERROR_SUCCESS - The function succeeded, and a handle to the record is returned.
     */
    int MsiViewFetch(Pointer hView, PointerByReference phRecord);
}
