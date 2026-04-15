package com.bricolirent.service;

/**
 * Exception metier utilisee pour signaler une validation invalide
 * sur le bloc d'administration des outils.
 */
public class ToolValidationException extends RuntimeException {

    public ToolValidationException(String message) {
        super(message);
    }
}
