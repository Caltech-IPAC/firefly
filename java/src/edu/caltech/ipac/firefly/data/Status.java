/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data;

import java.io.Serializable;

public class Status implements Serializable {
    public static final int OK_STATUS = 0;
	private int status;
	private String message;

    public Status(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public Status() {
        this(0, null);
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isOkStatus() {
        return status == OK_STATUS;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Status && ((Status) obj).status == status;
    }

    @Override
    public int hashCode() {
        return String.valueOf(status).hashCode();
    }
}