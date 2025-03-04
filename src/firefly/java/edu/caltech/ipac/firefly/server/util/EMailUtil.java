/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util;

import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.ThrowableUtil;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.util.Date;
import java.util.Properties;

/**
 * @author Joe Chavez
 * @author Tatiana Goldina
 * @version $Id: EMailUtil.java,v 1.2 2012/10/16 18:18:34 loi Exp $
 */
public class EMailUtil {

    public static final boolean MAIL_SESSION_BY_PROP = AppProperties.getBooleanProperty("mail.use.prop.file", false);
    public static final String MAIL_SESSION = AppProperties.getProperty("mail.session", "MailSession");

    public static void sendMessage(String[] to, String[] cc, String[] bcc, String subject, String messageBody, boolean isHTML)
            throws EMailUtilException {

        Session mailSession = getMailSession();
        sendMessage(to, cc, bcc, subject, messageBody, mailSession, isHTML);
    }

    private static Session getMailSession() throws EMailUtilException {
        Session session;
        try {
            Properties properties = new Properties();
            properties.setProperty("mail.transport.protocol", AppProperties.getProperty("mail.transport.protocol"));
            properties.setProperty("mail.smtp.host", AppProperties.getProperty("mail.smtp.host"));
            properties.setProperty("mail.smtp.auth", AppProperties.getProperty("mail.smtp.auth"));
            properties.setProperty("mail.smtp.port", AppProperties.getProperty("mail.smtp.port"));
            properties.setProperty("mail.smtp.from", AppProperties.getProperty("mail.smtp.from"));
            properties.setProperty("mail.smtp.starttls.enable", AppProperties.getProperty("mail.smtp.starttls.enable"));
            session = Session.getInstance(properties);

        } catch (Exception e) {
            String msg = "Unable to send message, mail session not found on server. Failed to look up session from " +
                    (MAIL_SESSION_BY_PROP ? "JNDI name = " + MAIL_SESSION : " property file.");
            Logger.info(msg + "; " + e.getMessage());
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

        String toList = String.join(",", to != null ? to : new String[]{});
        String ccList = String.join(",", cc != null ? cc : new String[]{});
        String bccList = String.join(",", bcc != null ? bcc : new String[]{});

        if (toList.isEmpty()) {
            throw new EMailUtilException("The TO address list cannot be empty. Must have at least one recipient.");
        }

        try {
            Message msg = new MimeMessage(mailSession);
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toList, false));
            if (!ccList.isEmpty()) {
                msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(ccList, false));
            }
            if (!bccList.isEmpty()) {
                msg.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bccList, false));
            }
            msg.setSubject(subject);
            msg.setSentDate(new Date());

            // Create message body
            MimeBodyPart mbp = new MimeBodyPart();
            if (isHTML) {
                mbp.setContent(messageBody, "text/html");
            } else {
                mbp.setText(messageBody);
            }

            Multipart mp = new MimeMultipart();
            mp.addBodyPart(mbp);
            msg.setContent(mp);

            Transport.send(msg);
        } catch (MessagingException e) {
            Logger.warn(ThrowableUtil.getStackTraceAsString(e));
            String msg = "Unable to send message, e-mail system not responding";
            Logger.warn(msg + ": " + e.getMessage());
            throw new EMailUtilException(msg);
        }
    }
}
