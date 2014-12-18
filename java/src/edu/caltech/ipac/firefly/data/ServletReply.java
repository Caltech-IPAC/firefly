package edu.caltech.ipac.firefly.data;

/**
 * Date: May 5, 2009
 *
 * @author loi
 * @version $Id: ServletReply.java,v 1.5 2009/11/19 20:43:47 balandra Exp $
 */
public class ServletReply {
    private int status;
    private String message;
    private String value;

    public ServletReply(int status, String message, String value) {
        this.status = status;
        this.message = message;
        this.value = value;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getValue() {
        return value;
    }

    public static ServletReply parse(String serverReturnedString) {
        if (serverReturnedString != null) {
            String[] vals = serverReturnedString.split("::", 3);
            if (vals.length == 3) {
                try {
                    int status = Integer.parseInt(vals[0]);
                    return new ServletReply(status, vals[1], vals[2]);
                } catch(Exception ex) {}
            } else {
                return new ServletReply(500, serverReturnedString, null);
            }
        }
        return null;
    }
}
