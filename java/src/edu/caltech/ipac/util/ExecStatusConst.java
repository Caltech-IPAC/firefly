package edu.caltech.ipac.util;

/**
 * The following execution status categories are represented:
 * <OL>
 * <LI>General</LI>
 * <LI>AIRE</LI>
 * <LI>PERCY</LI>
 * <LI>COORD</LI>
 * </OL>
 * Each category contains any number of status codes and corresponding messages.
 * The message strings are stored in the <B>execstatus.properties</B> file in the following format:
 * <PRE>
 * nnnnn.nnnnn.msg = status message here
 * nnnnn.nnnnn.desc = status description here
 * nnnnn.nnnnn.inst = status instance (optional)
 * nnnnn.nnnnn.op = status operation (optional)
 * </PRE>
 * The first two entries (msg and desc) are required. The second two are optional and can be specified at runtime.
 * (If known they should be included here, the values can be overridded at runtime).
 * The first 'nnnnn' sequence represents the major status code. The second 'nnnnn' sequence represents
 * minor status code.
 * <UL>
 * <LI>msg - the status message, short form.</LI>
 * <LI>desc - the full description of the the status.</LI>
 * <LI>inst - the class instance name that created the status.</LI>
 * <LI>op - the operation of the instance that generated the status.</LI>
 * </UL>
 * There are a maximum of 65536 major and 65536 minor error status codes. Do not code to the numeric value
 * of the status code, these are subject to change.
 * <BR>
 * <h3>CVS Info</h3>
 * <div align="left">
 * <p/>
 * <table border="1" width="366">
 * <tr>
 * $Header: /ssc/cvs/root/common/java/src/edu/caltech/ipac/util/ExecStatusConst.java,v 1.3 2006/02/14 22:05:19 jchavez Exp $
 * </tr>
 * </table>
 * <BR>
 * <BR>
 *
 * @author <a href="mailto:jchavez@ipac.caltech.edu?subject=Java Docs">Joe Chavez</a>
 * @version 0.2
 * @see edu.caltech.ipac.util.ExecStatus
 */
public final class ExecStatusConst {

    /**
     * Status of the operation is not an error.
     */
    public static final int STATUS_OK = 0;

    /**
     * General server error.
     */
    public static final int SERVER_ERROR_GENERAL = 100;

    /**
     * The server attempted to deserialize an object, but the class is not known by the server.
     */
    public static final int GENERAL_UNKNOWN_OBJECT_TYPE = 1;

    /**
     * General EJB processing error.
     */
    public static final int GENERAL_EJB = 2;

    /**
     * General exception. The ExecStatus instance will contain the Exception object.
     */
    public static final int GENERAL_EXCEPTION = 3;


    /**
     * The API did not receive the expected object type.
     */
    public static final int GENERAL_INCORRECT_OBJECT_TYPE = 4;

    /**
     * PERCY server error.
     */
    public static final int SERVER_ERROR_PERCY = 200;

    /**
     * PERCY server returned an error.
     */
    public static final int PERCY_SERVER_PROCESSING_ERROR = 1;

    /**
     * Timeout waiting for PERCY server to process a request.
     */
    public static final int PERCY_TIMEOUT_WAITING_FOR_SERVER = 2;

    /**
     * PERCY server is not available to process requests.
     */
    public static final int PERCY_SERVER_NOT_AVAILALABLE = 3;

    /**
     * PERCY server refused a connection.
     */
    public static final int PERCY_SERVER_CONNECTION_REFUSED = 4;

    /**
     * AIRE server error.
     */
    public static final int SERVER_ERROR_AIRE = 300;

    /**
     * AIRE Server processing error
     */
    public static final int AIRE_SERVER_PROCESSING_ERROR = 1;

    /**
     * AIRE Services not available. The aire component of the SutAPI has not been loaded
     * or is disabled for maintenance.
     */
    public static final int AIRE_SERVER_NOT_AVAILABLE = 2;

    /**
     * COORD server error.
     */
    public static final int SERVER_ERROR_COORD = 400;

    /**
     * COORD Server processing error
     */
    public static final int COORD_SERVER_PROCESSING_ERROR = 1;

    /**
     * COORD Services not available. The coord component of the SutAPI has not been loaded
     * or is disabled for maintenance.
     */
    public static final int COORD_SERVER_NOT_AVAILABLE = 2;

    /**
     * AOR Parse error
     */
    public static final int AOR_PARSE_ERROR = 500;

    /**
     * AOR: General error parsing the AOR file
     */
    public static final int AOR_PARSE_ERROR_GENERAL = 1;

    /**
     * AOR: error parsing the AOR file
     */
    public static final int AOR_PARSE_ERROR_PARSING = 2;

    /**
     * AOR: Invalid version in AOR file
     */
    public static final int AOR_PARSE_ERROR_INVALID_VERSION = 3;

    /**
     * DBMS Store error
     */
    public static final int DMBS_STORE_ERROR = 600;

    /**
     * General exception store data in DBMS
     */
    public static final int DBMS_GENERAL_EXCEPTION = 1;


    /**
     * VIS server error.
     */
    public static final int SERVER_ERROR_VIS = 700;

    /**
     * VIS server returned an error.
     */
    public static final int VIS_SERVER_PROCESSING_ERROR = 1;

    /**
     * Timeout waiting for VIS server to process a request.
     */
    public static final int VIS_TIMEOUT_WAITING_FOR_SERVER = 2;

    /**
     * VIS server is not available to process requests.
     */
    public static final int VIS_SERVER_NOT_AVAILALABLE = 3;

    /**
     * VIS server refused a connection.
     */
    public static final int VIS_SERVER_CONNECTION_REFUSED = 4;

    /**
     * Security Server Errors
     */
    public static final int SERVER_ERROR_SECURITY = 800;

    /**
     * Security error for user login
     */
    public static final int SECURITY_USER_AUTHENTICATION = 1;

    /**
     * Security for user authorization
     */
    public static final int SECURITY_USER_AUTHORIZATION = 2;

    /**
     * Error processing security request
     */
    public static final int SECURITY_PROCESSING_ERROR = 3;

    /**
     * Persistence Server Errors
     */
    public static final int SERVER_ERROR_PERSISTENCE = 900;

    /**
     * General exception store data in DBMS
     */
    public static final int PERSISTENCE_GENERAL_EXCEPTION = 1;

    /**
     * Returned when trying to get a Program from the sodb
     * and the program name is not found in the database
     */
    public static final int PERSISTENCE_PROGRAM_DOES_NOT_EXIST = 2;

    /**
     * Returned with trying to get a Program from the sodb
     * and the password is not correct
     */
    public static final int PERSISTENCE_INCORRECT_PASSWORD_FOR_PROGRAM = 3;

    /**
     * Returned when there are no available connections to the database
     */
    public static final int PERSISTENCE_NO_CONNECTION_AVAILABLE = 4;

    /**
     * Software Update Server Errors
     */
    public static final int SERVER_ERROR_SOFTWARE_UPDATE = 1000;


    /**
     * Client version is not supported by automatic software update
     */
    public static final int SOFTWARE_UPDATE_VERSION_NOT_SUPPORTED = 1;

    /**
     * E-Mail Server Errors
     */
    public static final int SERVER_ERROR_EMAIL = 1100;

    /**
     * General exception
     */
    public static final int EMAIL_GENERAL_EXCEPTION = 1;

    /**
     * Send failed error
     */
    public static final int EMAIL_SEND_FAILED = 2;

    /**
     * E-Mail configuration error on server
     */
    public static final int EMAIL_CONFIG = 3;

    /**
     * E-Mail address is invalid
     */
    public static final int EMAIL_ADDRESS = 4;


    /**
     * Proposal Submission Server Errors
     */
    public static final int SERVER_ERROR_PROPOSAL_SUBMISSION = 1200;

    /**
     * General exception
     */
    public static final int PROPSOSAL_SUBMISSION_GENERAL_EXCEPTION = 1;

    /**
     * Proposal Submission Window Not Defined
     */
    public static final int PROPSOSAL_SUBMISSION_WINDOW_NOT_DEFINED = 2;

    /**
     * Proposal Submission System Closed at Server
     */
    public static final int PROPSOSAL_SUBMISSION_SUBMISSION_SYSTEM_CLOSED = 3;


}



