package com.sample.workflownotification;

import java.util.Properties;
import java.util.Set;

import javax.mail.*;
import javax.mail.internet.*;

public class EmailSender {
    
    public String sender;
    public String senderPassword;
    public EmailSender(String sender, String senderPassword) {
        this.sender = sender;
        this.senderPassword = senderPassword;
    }

    public void sendMail(Set<String> emailNotifierList, String messageText, String subject ) {
        // Set properties for the SMTP server
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        
        String[] to = emailNotifierList.toArray(new String[emailNotifierList.size()]);

        // Create a Session object to authenticate the sender's email and password
        Session session = Session.getInstance(props,
            new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(sender, senderPassword);
                }
            });

        try {
            // Create a MimeMessage object
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(sender));
            // Set BCC recipients
            InternetAddress[] toAddresses = new InternetAddress[to.length];
            for (int i = 0; i < to.length; i++) {
                toAddresses[i] = new InternetAddress(to[i]);
            }
            message.setRecipients(Message.RecipientType.TO, toAddresses);
            message.setSubject(subject);
            message.setContent(messageText, "text/html");

            // Send the message
            Transport.send(message);

            System.out.println("Email sent to " + String.join(",", to) + " successfully!");

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

}
