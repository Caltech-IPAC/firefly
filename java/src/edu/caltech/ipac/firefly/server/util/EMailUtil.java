/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util;

import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.ThrowableUtil;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Context;
import java.util.Date;

/**
 * @author Joe Chavez
 * @author Tatiana Goldina
 * @version $Id: EMailUtil.java,v 1.2 2012/10/16 18:18:34 loi Exp $
 */
public class EMailUtil {

    public static final String MAIL_SESSION = AppProperties.getProperty("mail.session", "MailSession");

    public static void sendMessage(String[] to, String[] cc, String[] bcc, String subject, String messageBody, boolean isHTML)
            throws EMailUtilException {

        Session mailSession = getMailSession();
        sendMessage(to, cc, bcc, subject, messageBody, mailSession, isHTML);
    }

    private static Session getMailSession() throws EMailUtilException {
        Session session;
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            session=(Session) envCtx.lookup(MAIL_SESSION);
        } catch (NamingException e) {
            String msg = "Unable to send message, mail session not found on server. Looking for JNDI name = "+MAIL_SESSION;
            ClientLog.message(msg+"; "+e.getMessage());
            throw new EMailUtilException(msg);
        }
        return session;
    }


    public static void sendMessage(String[] to, String[] cc, String[] bcc, String subject, String messageBody)
            throws EMailUtilException {

        Session mailSession = getMailSession();
        sendMessage(to, cc, bcc, subject, messageBody, mailSession, false);
    }

    public static void sendMessage(String[] to, String[] cc, String[] bcc, String subject, String messageBody, Session mailSession, boolean isHTML)
            throws EMailUtilException {

        String toList = "";
        String ccList = "";
        String bccList = "";
        try {
            if (to != null) {
                for (int i = 0; i < to.length; i++) {
                    if (i == to.length - 1) {
                        toList += to[i];
                    } else {
                        toList += to[i] + ",";
                    }
                }
            } else {
                throw new EMailUtilException("The TO address list cannot be empty, must have at least one destination address.");
            }
            if (cc != null) {
                for (int i = 0; i < cc.length; i++) {
                    if (i == cc.length - 1) {
                        ccList += cc[i];
                    } else {
                        ccList += cc[i] + ",";
                    }
                }
            }
            if (bcc != null) {
                for (int i = 0; i < bcc.length; i++) {
                    if (i == bcc.length - 1) {
                        bccList += bcc[i];
                    } else {
                        bccList += bcc[i] + ",";
                    }
                }
            }
            Message msg = new MimeMessage(mailSession);
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toList, false));
            msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(ccList, false));
            msg.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bccList, false));
            msg.setSubject(subject);
            msg.setSentDate(new Date());
            // Content is stored in a MIME multi-part message
            // with one body part
            MimeBodyPart mbp = new MimeBodyPart();
            if(isHTML) {
                mbp.setContent(messageBody, "text/html");
            }
            else {
                mbp.setText(messageBody);
            }
            Multipart mp = new MimeMultipart();
            mp.addBodyPart(mbp);
            msg.setContent(mp);
            Transport.send(msg);
        } catch (MessagingException e) {
            ClientLog.warning(ThrowableUtil.getStackTraceAsString(e));
            String msg = "Unable to send message, e-mail system not responding";
            ClientLog.warning(msg +": "+e.getMessage());
            throw new EMailUtilException(msg);
        }
    }
}
