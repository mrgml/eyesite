package com.arantech.eyesite;

// File manipulation imports


// email imports
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.activation.MailcapCommandMap;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class TrafficMail{

	String host = "unavailable";
	String from = null;
//	String to = "trafficreport@arantech.com";
	String to = "glyons@arantech.com";
	String isDebugSet = "true";

	String customer = "unknown";
	Session session = null;
	Message msg = null;
	Transport bus = null;
	InternetAddress[] address = null;
	
	public TrafficMail() {
		
		// add handlers for main MIME types
		MailcapCommandMap mc = (MailcapCommandMap)CommandMap.getDefaultCommandMap();
		mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
		mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
		mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
		mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
		mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
		CommandMap.setDefaultCommandMap(mc);
		
		this.host = PropertyManager.getProperty("mailHost");
		this.from = PropertyManager.getProperty("mailSystemAddress");
		
		// Create properties for the Session
	    Properties props = new Properties();

	    // If using static Transport.send(),
	    // need to specify the mail server here
	    props.setProperty("mail.smtp.host", host);
	    // To see what is going on behind the scenes
	    props.put("mail.debug", isDebugSet);

	    // Get a session
	    this.session = Session.getInstance(props);
	    this.setup();
	}
	
	private void setup() {

		customer = PropertyManager.getProperty("customer.name");
		try {
			// Get a Transport object to send e-mail
			this.bus = session.getTransport("smtp");

			// Connect only once here
			// Transport.send() disconnects after each send
			// Usually, no username and password is required for SMTP
			bus.connect();
			//bus.connect("smtpserver.yourisp.net", "username", "password");

			// Instantiate a message
			msg = new MimeMessage(session);

			// Set message attributes
			msg.setFrom(new InternetAddress(from));
			InternetAddress[] toAddress = {new InternetAddress(to)};
			this.address = toAddress;
			msg.setRecipients(Message.RecipientType.TO, address);

			// insert customer name in subject
			msg.setSubject("Traffic Report from " + customer);
			msg.setSentDate(new Date());
		}
		catch (MessagingException mex) {
			// Prints all nested (chained) exceptions as well
			mex.printStackTrace();
			// How to access nested exceptions
			while (mex.getNextException() != null) {
				// Get next exception in chain
				Exception ex = mex.getNextException();
				ex.printStackTrace();
				if (!(ex instanceof MessagingException)) break;
				else mex = (MessagingException)ex;
			}
		}
	}
	
	public void send(){
		
		try {
			bus.sendMessage(msg, address);
			bus.close();
		}
		catch (MessagingException mex) {
			// Prints all nested (chained) exceptions as well
			mex.printStackTrace();
			// How to access nested exceptions
			while (mex.getNextException() != null) {
				// Get next exception in chain
				Exception ex = mex.getNextException();
				ex.printStackTrace();
				if (!(ex instanceof MessagingException)) break;
				else mex = (MessagingException)ex;
			}
		}
    }
	
	public void attach(String filename) {
		try {
			attach(this.msg, filename);
		}
		catch (MessagingException mex) {
			// Prints all nested (chained) exceptions as well
			mex.printStackTrace();
			// How to access nested exceptions
			while (mex.getNextException() != null) {
				// Get next exception in chain
				Exception ex = mex.getNextException();
				ex.printStackTrace();
				if (!(ex instanceof MessagingException)) break;
				else mex = (MessagingException)ex;
			}
		}
	}
		
	// Set a file as an attachment.  Uses JAF FileDataSource.
    public void attach(Message msg, String filename)
             throws MessagingException {

        // Create and fill first part
        MimeBodyPart p1 = new MimeBodyPart();
        p1.setText("This is part one of a test multipart e-mail." +
                    "The second part is file as an attachment");

        // Create second part
        MimeBodyPart p2 = new MimeBodyPart();

        // Put a file in the second part
        FileDataSource fds = new FileDataSource(filename);
        p2.setDataHandler(new DataHandler(fds));
        p2.setFileName(fds.getName());

        // Create the Multipart.  Add BodyParts to it.
        Multipart mp = new MimeMultipart();
        mp.addBodyPart(p1);
        mp.addBodyPart(p2);

        // Set Multipart as the message's content
        msg.setContent(mp);
		msg.saveChanges(); 
    }
	
}