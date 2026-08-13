/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 9/11/14
 * Time: 2:19 PM
 */


import java.io.Serializable;
import java.util.List;

/**
 * @author Trey Roby
 */
public class ProgressStat implements Serializable {
    public enum PType { DOWNLOADING, READING, CREATING, OTHER, GROUP, SUCCESS, FAIL }

    private final PType type;
    private final String key;
    private final String message;
    private final String id;
    private final List<String> memberIDList;

    protected ProgressStat() {this(null, null, null, null);}

    public ProgressStat(String key, String id, PType type, String message) {
        this.key = key;
        this.id = id;
        this.message = message;
        this.memberIDList= null;
        this.type= type;
    }

    public ProgressStat(String key, List<String> memberIDList) {
        this.key = key;
        this.memberIDList = memberIDList;
        this.message= "";
        this.id = "";
        this.type= PType.GROUP;
    }

    public boolean isGroup() { return memberIDList!=null; }

    public PType getType() { return type; }

    public String getMessage() { return message; }

    public String getKey() { return key; }

    public String getId() { return id; }

    public List<String> getMemberIDList() { return memberIDList; }

    public boolean isDone() {
        return (type==PType.SUCCESS || type==PType.FAIL);
    }
}


