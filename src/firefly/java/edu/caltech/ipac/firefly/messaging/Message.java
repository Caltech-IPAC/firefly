/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.messaging;

import edu.caltech.ipac.firefly.core.background.JobInfo;
import edu.caltech.ipac.firefly.core.background.JobManager;
import edu.caltech.ipac.firefly.data.ServerEvent.Scope;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.util.AppProperties;

/**
 * The message object sent or received by Messenger.  As implemented, Messenger
 * uses JSON when serialize/deserialize is needed.  However, this is an internal
 * implementation, and it can be changed without breaking Messenger/Message contract.
 *
 * Date: 2019-03-15
 *
 * @author loi
 * @version $Id: $
 */
public class Message {
    protected JsonHelper helper = new JsonHelper();

    public MsgHeader getHeader() {
        Scope scope = Scope.valueOf(helper.getValue(Scope.SELF.name(), "header", "scope"));
        String to = helper.getValue("", "header", "to");
        String from = helper.getValue("", "header", "from");
        String subject = helper.getValue("", "header", "subject");
        MsgHeader header = new MsgHeader(scope, to, subject);
        header.setFrom(from);
        return header;
    }

    public void setHeader(Scope scope, String to, String subject, String from) {
        helper.setValue(scope.name(), "header", "scope");
        if (to != null) helper.setValue(to, "header", "to");
        if (from != null) helper.setValue(from, "header", "from");
        if (subject != null) helper.setValue(subject, "header", "subject");
    }

    public Message setValue(Object value, String... paths) {
        helper.setValue(value, paths);
        return this;
    }

    public <T> T getValue(T def, String... paths) throws ClassCastException {
        return helper.getValue(def, paths);
    }

    public static Message parse(String json) {
        Message message = new Message();
        message.helper = JsonHelper.parse(json);
        return message;
    }

    public String toJson() {
        return helper.toJson();
    }

//====================================================================
//  Predefined messages
//====================================================================

    public static final class JobCompleted extends Message {
        public static final String TOPIC = "JobCompleted";
        static final String FROM = AppProperties.getProperty("mail.smtp.from", "donotreply@ipac.caltech.edu");
        JobInfo job;
        public JobCompleted(JobInfo jobInfo) {
            job = jobInfo;
            setHeader(Scope.CHANNEL, jobInfo.getOwner(), TOPIC, FROM);
            helper.setValue(JobManager.toJsonObject(jobInfo), "jobInfo");
            var user = ServerContext.getRequestOwner().getUserInfo();
            if (user != null) {
                helper.setValue(user.getName(), "user", "name");
                helper.setValue(user.getEmail(), "user", "email");
                helper.setValue(user.getLoginName(), "user", "loginName");
            }
            var ssoAdpt = ServerContext.getRequestOwner().getSsoAdapter();
            if (ssoAdpt != null) {
                helper.setValue(ssoAdpt.getAuthToken(), "user", "authToken");
            }
        }
    }
}