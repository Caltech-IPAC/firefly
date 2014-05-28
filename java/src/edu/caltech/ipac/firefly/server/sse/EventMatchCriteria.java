package edu.caltech.ipac.firefly.server.sse;
/**
 * User: roby
 * Date: 5/28/14
 * Time: 9:38 AM
 */


import java.util.Arrays;
import java.util.List;

/**
 * @author Trey Roby
 */
public class EventMatchCriteria {

    private final List<EventTarget> matchList;

    public EventMatchCriteria(EventTarget... targets) {
        this.matchList= Arrays.asList(targets);
    }


    public boolean matches(EventTarget target) {
        boolean matches=false;
        for(EventTarget  testTgt : matchList) {
            if (testTgt.matches(target)) {
                matches= true;
                break;
            }
        }
        return matches;
    }

    @Override
    public boolean equals(Object obj) {
        boolean retval= false;
        if (obj instanceof EventMatchCriteria) {
            EventMatchCriteria other= (EventMatchCriteria)obj;
            retval= other.matchList.equals(matchList);
        }
        return retval;
    }

    public  static EventMatchCriteria makeSessionCriteria(String sessionID, String windowID) {
        return new EventMatchCriteria(new EventTarget.Session(sessionID, windowID));
    }

    public  static EventMatchCriteria makeWorkspaceCriteria(String workspaceID, String windowID) {
        return new EventMatchCriteria(new EventTarget.Session(workspaceID, windowID));
    }

    @Override
    public String toString() {
        return "EventMatchCriteria: "+Arrays.deepToString(matchList.toArray());
    }
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
