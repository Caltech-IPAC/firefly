/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.messaging;

import edu.caltech.ipac.firefly.data.ServerEvent;

/**
 * Date: 2019-03-15
 *
 * @author loi
 * @version $Id: $
 */
public class MsgHeader {
    private ServerEvent.Scope scope;
    private String to;
    private String subject;
    private String from;

    public MsgHeader(ServerEvent.Scope scope, String to, String subject) {
        this.scope = scope;
        this.to = to;
        this.subject = subject;
    }

    public ServerEvent.Scope getScope() {
        return scope;
    }

    public void setScope(ServerEvent.Scope scope) {
        this.scope = scope;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
