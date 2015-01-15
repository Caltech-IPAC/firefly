/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.sse;
/**
 * User: roby
 * Date: 2/25/14
 * Time: 11:48 AM
 */


import edu.caltech.ipac.util.ComparisonUtil;

import java.io.Serializable;

/**
 * @author Trey Roby
 */
public abstract class EventTarget implements Serializable {

    public static final EventTarget ALL= new AllClients();

    public abstract boolean matches(EventTarget t);

    public static class Workspace extends EventTarget {
        final String id;
        final String windowID;
        public Workspace(String id, String windowID) {
            this.id = id;
            this.windowID = windowID;
        }
        public String getWorkspaceId() { return id; }

        public boolean equals(Object o) {
            boolean retval= false;
            if (o instanceof Workspace) {
                retval= ComparisonUtil.equals(id,((Workspace)o).id) &&
                        ComparisonUtil.equals(windowID,((Workspace)o).windowID);
            }
            return retval;
        }

        @Override
        public boolean matches(EventTarget t) {
            boolean retval= false;
            if (t instanceof Workspace) {
                retval= ComparisonUtil.equals(id,((Workspace)t).id);
            }
            return retval;
        }

        @Override
        public String toString() {
            return "EventTarget- workspaceId:"+id+", wId:"+windowID;
        }
    }

    public static class Session extends EventTarget {
        final String id;
        final String windowID;

        public Session(String id, String windowID) {
            this.id = id;
            this.windowID = windowID;
        }

        public Session(String id) {
            this(id,null);
        }

        public String getSessionId() { return id; }

        public boolean equals(Object o) {
            boolean retval= false;
            if (o instanceof Session) {
                retval= ComparisonUtil.equals(id,((Session)o).id) &&
                        ComparisonUtil.equals(windowID,((Session)o).windowID);
            }
            return retval;
        }

        @Override
        public boolean matches(EventTarget t) {
            boolean retval= false;
            if (t instanceof Session) {
                retval= ComparisonUtil.equals(id,((Session)t).id);
            }
            return retval;
        }

        @Override
        public String toString() {
            return "EventTarget- sId:"+id+", wId:"+windowID;
        }
    }

    public static class AllClients extends EventTarget {
        private AllClients() {}
        public boolean equals(Object o) {
            return (o instanceof AllClients);
        }

        @Override
        public boolean matches(EventTarget t) { return true; }

        @Override
        public String toString() {
            return "EventTarget- ALL";
        }
    }


}

