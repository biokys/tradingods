package cz.tradingods.common;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


public class MailHelper {

	private static final String SMTP_HOST_NAME = PropertyHelper.getSmtpHostName();
	private static final String SMTP_AUTH_USER = PropertyHelper.getSmtpUsername();
	private static final String SMTP_AUTH_PWD  = PropertyHelper.getSmtpPassword();

	private static final String SEND_FROM  = PropertyHelper.getFromEmail();

	static String[] recipients = PropertyHelper.getToEmails();

	private static void postMail( String subject, String message) throws MessagingException
	{
		boolean debug = false;

		//Set the host smtp address
		Properties props = new Properties();
		props.put("mail.smtp.host", SMTP_HOST_NAME);
		props.put("mail.smtp.auth", true);

		// create some properties and get the default Session
		Authenticator auth = new SMTPAuthenticator(); 
		Session session = Session.getDefaultInstance(props, auth);
		session.setDebug(debug);

		// create a message
		Message msg = new MimeMessage(session);

		// set the from and to address
		InternetAddress addressFrom = new InternetAddress(SEND_FROM);
		msg.setFrom(addressFrom);

		InternetAddress[] addressTo = new InternetAddress[recipients.length]; 
		for (int i = 0; i < recipients.length; i++)  {
			addressTo[i] = new InternetAddress(recipients[i]);
		}
		msg.setRecipients(Message.RecipientType.TO, addressTo);


		// Setting the Subject and Content Type
		msg.setSubject(subject);
		msg.setContent(message, "text/plain");
		Transport.send(msg);
	}
	
	public static void sendEmail(String msg) {
		if (!PropertyHelper.sendEmails())
			return;
		try {
			MailHelper.postMail("FX", msg);
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}

	private static class SMTPAuthenticator extends javax.mail.Authenticator {

		public PasswordAuthentication getPasswordAuthentication()
		{
			String username = SMTP_AUTH_USER;
			String password = SMTP_AUTH_PWD;
			return new PasswordAuthentication(username, password);
		}
	}

}
