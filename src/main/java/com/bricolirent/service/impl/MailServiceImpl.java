package com.bricolirent.service.impl;

import com.bricolirent.service.MailService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class MailServiceImpl implements MailService {

    private static final Logger LOGGER = Logger.getLogger(MailServiceImpl.class.getName());

    // SIMULATION pour le TP: dans la vraie vie on utilise des variables d'environnement
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    // Vous pouvez remplacer par vos identifiants pour tester l'envoi réel
    private static final String SMTP_USER = "test@bricolirent.local";
    private static final String SMTP_PASSWORD = "test-password";

    @Override
    public void sendTextMail(String to, String subject, String content) {
        // En mode académique, on va d'abord afficher l'email dans la console 
        // pour que vous puissiez cliquer sur le lien sans avoir à configurer STMP !
        LOGGER.info("\n========== MOCK EMAIL SENT ==========\n" +
                    "To: " + to + "\n" +
                    "Subject: " + subject + "\n" +
                    "Content: \n" + content + "\n" +
                    "=====================================");

        // --- Code réel SMTP (Commenté en attendant votre vraie adresse Gmail/SMTP) ---
        /*
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
            @Override
            protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                return new jakarta.mail.PasswordAuthentication(SMTP_USER, SMTP_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USER));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(content);

            Transport.send(message);
            LOGGER.info("Email réellement envoyé via SMTP à " + to);
        } catch (MessagingException e) {
            LOGGER.log(Level.SEVERE, "Échec de l'envoi de l'email :", e);
        }
        */
    }
}
