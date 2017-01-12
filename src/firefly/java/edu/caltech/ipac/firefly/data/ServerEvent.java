/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.data;
/**
 * User: roby
 * Date: 2/18/14
 * Time: 2:35 PM
 */


import edu.caltech.ipac.firefly.util.event.Name;

import java.io.Serializable;
import java.lang.String;

/**
 * @author Trey Roby
 */
public class ServerEvent implements Serializable {

    public static final String SERVER_CONN_ID = "-1";
    private Name name;
    private EventTarget target;
    private DataType dataType = DataType.STRING;
    private Serializable data;
    private String from;
//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================
    public ServerEvent() {}
    public ServerEvent(Name name, Scope scope, Serializable data) {
        this(name, new EventTarget(scope), DataType.JSON, data, null);
    }
    public ServerEvent(Name name, Scope scope, DataType dataType, Serializable data) {
        this(name, new EventTarget(scope), dataType, data, null);
    }
    public ServerEvent(Name name, EventTarget target, Serializable data) {
        this(name, target, DataType.BG_STATUS, data, SERVER_CONN_ID);
    }
    public ServerEvent(Name name, EventTarget target, DataType dataType, Serializable data) {
        this(name, target, dataType, data, SERVER_CONN_ID);
    }
    public ServerEvent(Name name, EventTarget target, DataType dataType, Serializable data, String from) {
        this.name = name;
        this.target = target;
        this.dataType = dataType;
        this.data = data;
        this.from = from;
    }

    public DataType getDataType() {
        return dataType;
    }

    public Name getName() {
        return name;
    }

    public EventTarget getTarget() {
        return target;
    }

    public Serializable getData() {
        return data;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String toJsonString() {
        StringBuffer sb = new StringBuffer("{");
        sb.append("\"name\":\"").append(name.getName()).append("\", ");
        sb.append("\"scope\":\"").append(target.getScope().name()).append("\", ");
        sb.append("\"dataType\":\"").append(dataType == null ? DataType.STRING.name() : dataType.name()).append("\", ");
        sb.append("\"data\":\"").append(data == null ? "" : String.valueOf(data) + "\"").append("}");
        return sb.toString();
    }

    public enum Scope {SELF, CHANNEL, USER, WORLD}

    public enum DataType {JSON, BG_STATUS, STRING}

//====================================================================
//
//====================================================================

    public static class EventTarget implements Serializable {

        private Scope scope;
        private String connID;
        private String channel;
        private String userKey;

        public EventTarget() {}

        public EventTarget(Scope scope) {
            this(scope, null, null, null);
        }

        /**
         * Creates an EventTarget based on the given information.
         * scope is required.  If other information are missing, it
         * will get it from the request owner.
         * @param scope
         * @param connID
         * @param channel
         * @param userKey
         */
        public EventTarget(Scope scope, String connID, String channel, String userKey) {
            this.scope = scope;
            this.connID = connID;
            this.channel = channel;
            this.userKey = userKey;
        }

        /**
         * returns true if a destination information can be resolved.
         * @return
         */
        public boolean hasDestination() {
            return scope != null;
        }

        public Scope getScope() {
            return scope;
        }

        public String getConnID() {
            return connID;
        }

        public void setConnID(String connID) {
            this.connID = connID;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getUserKey() {
            return userKey;
        }
        public void setUserKey(String userKey) {
            this.userKey = userKey;
        }
    }
}

