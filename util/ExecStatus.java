package edu.caltech.ipac.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.FileInputStream;
import java.io.File;
import java.util.Properties;

/**
 * ExecStatus represents the status of the execution of a operation on a class.
 * It contains an error major and minor number and the corresponding message,
 * description information.
 * When appropriate information pertaining to the object and method that generated
 * the error is also specified.
 * There are not set methods for this class, the proper method of construction is to use the
 * <PRE> ExecStatus(int statusMajor, int statusMinor, String instanceName, String operation) </PRE>
 * constructor to create and instance.
 * <PRE>ExecStatus(int statusMajor, int statusMinor, String statusMsg, String statusDesc,
 * String instanceName, String operation)</PRE>
 * should only be used when no existing status major/minor codes exist or the instance does not represent an
 * error.
 * The <PRE> ExecStatus()</PRE> constructor should be used to create an ExecStatus instance that does not represent an error
 * and no additional information is to be provided about the status of the operation.
 * <BR>
 * This API relies on the execstatus.properties file for configuration information. This file
 * should be located in the <B>SOS_ROOT/config</B> directory.
 * <BR>
 * <h3>CVS Info</h3>
 * <div align="left">
 * <p/>
 * <table border="1" width="366">
 * <tr>
 * $Header: /ssc/cvs/root/uplink/java/src/edu/caltech/ipac/sirtf/util/ExecStatus.java,v 1.6
 * 2001/05/10 01:12:09 booth Exp $
 * </tr>
 * </table>
 * <BR>
 * <BR>
 *
 * @author <a href="mailto:jchavez@ipac.caltech.edu?subject=Java Docs">Joe Chavez</a>
 * @version 0.2
 * @see ExecStatusConst
 */
public class ExecStatus implements Serializable {

    static final long serialVersionUID = -7997542505665836058L;

    /**
     * Name of the file containg the execution status messages.
     */
    private static final String fileName = "execstatus.properties";

    /**
     * Maximum length of the execution status message.
     */
    public static final short STATUS_MSG_LEN = 50;

    /**
     * Maximum length of the execution status description.
     */
    public static final short STATUS_DESC_LEN = 255;

    /**
     * Maximum length of the process specific message.
     */
    public static final short PROCESS_ERROR_LEN = 512;


    /**
     * Maximum length of the instance name.
     */
    public static final short INSTANCE_NAME_LEN = 255;

    /**
     * Maximum length of the operation name.
     */
    public static final short OPERATION_LEN = 80;

    /**
     * Major status.
     */
    private int statusMajor;

    /**
     * Minor status.
     */
    private int statusMinor;

    /**
     * Status message, short form.
     */
    private char statusMsg[];

    /**
     * Status description, long form.
     */
    private char statusDesc[];

    /**
     * Process specific message.
     */
    private char processMsg[];


    /**
     * Instance name.
     */
    private char instanceName[];

    /**
     * Operation name.
     */
    private char operation[];

    /**
     * Holds the execption related to this status.
     */
    private Exception ex = null;

    /**
     * Default Constructor
     * sets status to no error.
     */
    public ExecStatus() {
        this.statusMajor = ExecStatusConst.STATUS_OK;
        this.statusMinor = ExecStatusConst.STATUS_OK;
    }

    /**
     * Used to contruct an ExecStatus object with a known Major and Minor status number. The status numbers
     * are contained in the ExecStatusConst class.
     *
     * @param statusMajor  Major number of the execution status.
     * @param statusMinor  Minor number of the execution status.
     * @param instanceName Optional object/instance name. Must not be null.
     * @param operation    Optional operation name for object/instance. Must not be null.
     * @param processMsg   Optional process specific message. Must not be null.
     * @see ExecStatusConst
     */
    public ExecStatus(int statusMajor, int statusMinor,
                      String instanceName, String operation, String processMsg) {
        this.statusMajor = statusMajor;
        this.statusMinor = statusMinor;
        lookupMessages();

        if (this.instanceName != null && instanceName != null) {
            instanceName.getChars(0, (this.INSTANCE_NAME_LEN < instanceName.length()) ? this.INSTANCE_NAME_LEN : instanceName.length(),
                    this.instanceName, 0);
        }

        if (this.operation != null && operation != null) {
            operation.getChars(0, (this.OPERATION_LEN < operation.length()) ? this.OPERATION_LEN : operation.length(),
                    this.operation, 0);
        }

        processMsg.getChars(0, (this.PROCESS_ERROR_LEN < processMsg.length()) ? this.PROCESS_ERROR_LEN : processMsg.length(),
                this.processMsg, 0);

    }

    /**
     * Used to contruct an ExecStatus object with a known Major and Minor status number. The status numbers
     * are contained in the ExecStatusConst class.
     *
     * @param statusMajor  Major number of the execution status.
     * @param statusMinor  Minor number of the execution status.
     * @param instanceName Optional object/instance name. Must not be null.
     * @param operation    Optional operation name for object/instance. Must not be null.
     * @see ExecStatusConst
     */
    public ExecStatus(int statusMajor, int statusMinor,
                      String instanceName, String operation) {
        this.statusMajor = statusMajor;
        this.statusMinor = statusMinor;
        lookupMessages();

        if (this.instanceName != null && instanceName != null) {
            instanceName.getChars(0, (this.INSTANCE_NAME_LEN < instanceName.length()) ? this.INSTANCE_NAME_LEN : instanceName.length(),
                    this.instanceName, 0);
        }

        if (this.operation != null && operation != null) {
            operation.getChars(0, (this.OPERATION_LEN < operation.length()) ? this.OPERATION_LEN : operation.length(),
                    this.operation, 0);
        }


    }


    /**
     * Checks if the execution status is an error.
     *
     * @return true if ExecStatus does not contain an error;
     *         false if ExecStatus contains an error
     */
    public boolean isOK() {
        if (this.statusMajor == ExecStatusConst.STATUS_OK) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Construct an ExecStatus object specifying all characteristics.
     * A message will be stored in the application log when this constructor
     * is used to create an ExecStatus instance. This message will specify
     * the contents of the ExecStatus object and record the fact that
     * a non-standard status was reported.
     *
     * @param statusMajor  Major number of the execution status.
     * @param statusMinor  Minor number of the execution status.
     * @param statusMsg    Exectuion status message.
     * @param statusDesc   Execution status description.
     * @param instanceName Optional object/instance name. Must not be null.
     * @param operation    Optional operation name for object/instance. Must not be null.
     * @see ExecStatusConst
     */
    public ExecStatus(int statusMajor, int statusMinor,
                      String statusMsg, String statusDesc,
                      String instanceName, String operation) {
        initMsgs();
        this.statusMajor = statusMajor;
        this.statusMinor = statusMinor;

        if (this.statusMsg != null && statusMsg != null) {
            statusMsg.getChars(0, (this.STATUS_MSG_LEN < statusMsg.length()) ? this.STATUS_MSG_LEN : statusMsg.length(),
                    this.statusMsg, 0);
        }

        if (this.statusDesc != null && statusDesc != null) {
            statusDesc.getChars(0, (this.STATUS_DESC_LEN < statusDesc.length()) ? this.STATUS_DESC_LEN : statusDesc.length(),
                    this.statusDesc, 0);
        }

        if (this.instanceName != null && instanceName != null) {
            instanceName.getChars(0, (this.INSTANCE_NAME_LEN < instanceName.length()) ? this.INSTANCE_NAME_LEN : instanceName.length(),
                    this.instanceName, 0);
        }

        if (this.operation != null && operation != null) {
            operation.getChars(0, (this.OPERATION_LEN < operation.length()) ? this.OPERATION_LEN : operation.length(),
                    this.operation, 0);
        }


    }

    private void initMsgs() {
        statusMsg = new char[this.STATUS_MSG_LEN];
        statusDesc = new char[this.STATUS_DESC_LEN];
        processMsg = new char[this.PROCESS_ERROR_LEN];
        instanceName = new char[this.INSTANCE_NAME_LEN];
        operation = new char[this.OPERATION_LEN];
    }

    /**
     * Lookup the messages corresponding to the Major and Minor error status codes.
     */
    protected void lookupMessages() {
        try {
            Properties msgs = new Properties();

/*
            FileInputStream msgFile =
                    new FileInputStream(System.getProperty("SOS_CONFIG", "")
                    + File.separator + "uplink"
                    + File.separator + this.fileName);
*/
            InputStream msgFile = this.getClass().getResourceAsStream("resources/" + this.fileName);
	        if(msgFile == null) {
		        msgFile =
		                new FileInputStream(System.getProperty("SOS_CONFIG", "")
		                + File.separator + "uplink"
		                + File.separator + this.fileName);
	        }

	        if(msgFile == null) {
		        return;
	        }
	        
            msgs.load(msgFile);

            initMsgs();

            String msgBase = this.statusMajor + "." + this.statusMinor + ".";

            String statusMsg = msgs.getProperty(msgBase + "msg", "");
            String statusDesc = msgs.getProperty(msgBase + "desc", "");
            String instanceName = msgs.getProperty(msgBase + "inst", "");
            String operation = msgs.getProperty(msgBase + "op", "");


            statusMsg.getChars(0, (this.STATUS_MSG_LEN < statusMsg.length()) ? this.STATUS_MSG_LEN : statusMsg.length(),
                    this.statusMsg, 0);
            statusDesc.getChars(0, (this.STATUS_DESC_LEN < statusDesc.length()) ? this.STATUS_DESC_LEN : statusDesc.length(),
                    this.statusDesc, 0);
            instanceName.getChars(0, (this.INSTANCE_NAME_LEN < instanceName.length()) ? this.INSTANCE_NAME_LEN : instanceName.length(),
                    this.instanceName, 0);
            operation.getChars(0, (this.OPERATION_LEN < operation.length()) ? this.OPERATION_LEN : operation.length(),
                    this.operation, 0);


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * Get the execution status message.
     * This is the short form of the message.
     *
     * @return String containing the status message.
     */
    public String getStatusMsg() {
        String str = "";
        if (statusMsg != null) {
            str = new String(statusMsg);
        }
        return str.trim();
    }

    /**
     * Get the minor status value of execution status.
     *
     * @return int containing the minor status.
     */
    public int getStatusMinor() {
        return statusMinor;
    }

    /**
     * Get the major status value of the execution status.
     *
     * @return int containing the major status.
     */
    public int getStatusMajor() {
        return statusMajor;
    }

    /**
     * Get the full description of the execution status.
     *
     * @return String containing the description.
     */
    public String getStatusDesc() {
        String str = "";
        if (statusDesc != null) {
            str = new String(statusDesc);
        }
        return str.trim();
    }

    /**
     * Get the operation name of the execution status.
     *
     * @return String containing the operation name.
     */
    public String getOperation() {
        String str = "";
        if (operation != null) {
            str = new String(operation);
        }
        return str.trim();
    }

    /**
     * Get the instance name of the execution status.
     *
     * @return String containing the instance name.
     */
    public String getInstanceName() {
        String str = "";
        if (instanceName != null) {
            str = new String(instanceName);
        }
        return str.trim();
    }


    /**
     * Get the process specific message.
     *
     * @return String containing the process specific message.
     */
    public String getProcessMsg() {
        String str = "";
        if (processMsg != null) {
            str = new String(processMsg);
        }
        return str.trim();
    }

    /**
     * Set the corresponding execption object for this message.
     *
     * @param ex Exception object as a result of a Java exception.
     */

    public void setException(Exception ex) {
        this.ex = ex;
    }

    /**
     * Get the Exception object for this status.
     *
     * @return Exception object for this status.
     */
    public Exception getException() {
        return this.ex;
    }

    /**
     * Return a string version of the execution status. Combines the following members: major, minor, msg, desc,
     * instance name, operation, and process specific message.
     *
     * @return String containing the execution status.
     */


    public String toString() {
        String retVal;
        retVal = "\nStatus code " + statusMajor + "." + statusMinor + "\n";
        retVal += "Message: " + this.getStatusMsg() + "\n";
        retVal += "Description: " + this.getStatusDesc() + "\n";
        retVal += "Instance: " + this.getInstanceName() + "\n";
        retVal += "Operation: " + this.getOperation() + "\n";
        retVal += "Server Msg: " + this.getProcessMsg() + "\n";
        return retVal;
    }

}



