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
    private final String id;
    private final String message;
    private final String plotId;
    private final List<String> memberIDList;

    public ProgressStat(String id, String plotId, PType type, String message) {
        this.id = id;
        this.plotId = plotId;
        this.message = message;
        this.memberIDList= null;
        this.type= type;
    }

    public ProgressStat(List<String> memberIDList, String id) {
        this.memberIDList = memberIDList;
        this.id = id;
        this.message= "";
        this.plotId= "";
        this.type= PType.GROUP;
    }

    public boolean isGroup() { return memberIDList!=null; }

    public PType getType() { return type; }

    public String getMessage() { return message; }

    public String getId() { return id; }

    public String getPlotId() { return plo; }

    public List<String> getMemberIDList() { return memberIDList; }

    public boolean isDone() {
        return (type==PType.SUCCESS || type==PType.FAIL);
    }
}


