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
    public enum PType { DOWNLOADING, CREATING, OTHER, GROUP }

    private final PType type;
    private final String id;
    private final String message;
    private final List<String> memberIDList;

    public ProgressStat(String id, PType type, String message) {
        this.id = id;
        this.message = message;
        this.memberIDList= null;
        this.type= type;
    }

    public ProgressStat(List<String> memberIDList, String id) {
        this.memberIDList = memberIDList;
        this.id = id;
        this.message= "";
        this.type= PType.GROUP;
    }

    public boolean isGroup() { return memberIDList!=null; }

    public PType getType() { return type; }

    public String getMessage() { return message; }

    public String getId() { return id; }

    public List<String> getMemberIDList() { return memberIDList; }
}


/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
