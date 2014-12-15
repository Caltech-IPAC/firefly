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
