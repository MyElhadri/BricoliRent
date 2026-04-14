package com.bricolirent.service;

public interface MailService {
    /**
     * Envoie un email texte brut à un destinataire de façon synchrone.
     */
    void sendTextMail(String to, String subject, String content);
}
