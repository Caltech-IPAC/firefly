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

    public abstract boolean matches(ServerSentEvent ev);

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
        public boolean matches(ServerSentEvent ev) {
            boolean retval= false;
            if (ev.getEvTarget() instanceof Workspace) {
                retval= ComparisonUtil.equals(id,((Workspace)ev.getEvTarget()).id);
            }
            return retval;
        }

    }

    public static class Session extends EventTarget {
        final String id;
        final String windowID;

        public Session(String id, String windowID) {
            this.id = id;
            this.windowID = windowID;
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
        public boolean matches(ServerSentEvent ev) {
            boolean retval= false;
            if (ev.getEvTarget() instanceof Session) {
                retval= ComparisonUtil.equals(id,((Session)ev.getEvTarget()).id);
            }
            return retval;
        }
    }

    public static class AllClients extends EventTarget {
        private AllClients() {}
        public boolean equals(Object o) {
            return (o instanceof AllClients);
        }

        @Override
        public boolean matches(ServerSentEvent ev) { return true; }

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
