/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.messaging;

import edu.caltech.ipac.firefly.core.Util.Try;
import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.data.ServerEvent.Scope;
import edu.caltech.ipac.firefly.util.event.Name;

import java.io.Serializable;

import static edu.caltech.ipac.util.StringUtils.applyIfNotEmpty;
import static edu.caltech.ipac.firefly.data.ServerEvent.*;

/**
 * The message object sent or received by Messenger.  As implemented, Messenger
 * uses JSON when serialize/deserialize is needed.  However, this is an internal
 * implementation, and it can be changed without breaking Messenger/Message contract.
 *
 * @author loi
 * @version $Id: $
 */
public class Message {
    public static final String TOPIC_KEY = "msg_topic";
    protected JsonHelper helper = new JsonHelper();

    public void setTopic(String topic) {
        setValue(topic, TOPIC_KEY);
    }

    public String getTopic() {
        return getValue(null, TOPIC_KEY);
    }

    public Message setValue(Object value, String... paths) {
        helper.setValue(value, paths);
        return this;
    }

    public <T> T getValue(T def, String... paths) throws ClassCastException {
        return helper.getValue(def, paths);
    }

    public int getInt(int def, String... paths) throws ClassCastException {
        return Try.it(() -> helper.getValue(def, paths)).getOrElse(def);
    }

    public static Message parse(String json) {
        Message message = new Message();
        message.helper = JsonHelper.parse(json);
        return message;
    }

    public String toJson() {
        return helper.toJson();
    }

    protected void replaceWith(Message msg) {
        this.helper = msg.helper;
    }


//====================================================================
//  Predefined messages
//====================================================================

    /**
     * Represents a server event message that can be sent or received by the Messenger.
     * This class encapsulates the details of a server event, including its name, target, data type, data, and source.
     * It provides methods to construct an event from a ServerEvent object and to parse a Message object back into a ServerEvent.
     *
     * <p>Example usage:</p>
     * <pre>
     * {@code
     * ServerEvent serverEvent = new ServerEvent(name, scope, data);
     * Message.Event eventMessage = new Message.Event(serverEvent);
     * }
     * </pre>
     **/
    public static final class Event extends Message {
        public static final String TOPIC = "firefly-events";

        public Event(ServerEvent se) {
            setTopic(TOPIC);
            setValue(se.getName().getName(), "name");
            applyIfNotEmpty(se.getTarget().getScope(), (s) -> setValue(s.name(), "target", "scope"));
            applyIfNotEmpty(se.getTarget().getChannel(), (s) -> setValue(s, "target", "channel"));
            applyIfNotEmpty(se.getTarget().getConnID(),  (s) -> setValue(s, "target", "connID"));
            applyIfNotEmpty(se.getTarget().getUserKey(), (s) -> setValue(s, "target", "userKey"));
            setValue(se.getDataType().name(), "dataType");
            setValue(se.getData(), "data");
            setValue(se.getFrom(), "from");
        }
        public static ServerEvent parse(Message msg) {
            try {
                Name name = Name.parse(msg.getValue(null, "name"));
                if (name == null) return null;

                Scope scope = Scope.valueOf(msg.getValue(null, "target", "scope"));
                String connID = msg.getValue(null, "target", "connID");
                String channel = msg.getValue(null, "target", "channel");
                String userKey = msg.getValue(null, "target", "userKey");
                DataType dtype = DataType.valueOf(msg.getValue(null, "dataType"));
                Serializable data = msg.getValue(null, "data");
                String from = msg.getValue(null, "from");
                return new ServerEvent(name, new EventTarget(scope, connID, channel, userKey), dtype, data, from);
            } catch (Exception e) {
                return null;
            }
        }
    }

}