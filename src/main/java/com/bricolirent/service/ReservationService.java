package com.bricolirent.service;

import com.bricolirent.domain.entity.Reservation;

import java.time.LocalDate;

/**
 * Contrat métier pour la gestion des réservations.
 *
 * <p>Implémenté par {@link com.bricolirent.service.impl.ReservationServiceImpl}.</p>
 *
 * <p><strong>Bloc 1 — périmètre :</strong> création d'une demande de réservation.</p>
 */
public interface ReservationService {

    /**
     * Crée une nouvelle demande de réservation pour un client donné.
     *
     * <p>Règles métier appliquées par l'implémentation :</p>
     * <ul>
     *   <li>La date de début doit être aujourd'hui ou dans le futur.</li>
     *   <li>La date de fin doit être strictement après la date de début.</li>
     *   <li>La quantité demandée ne doit pas dépasser la quantité disponible de l'outil.</li>
     *   <li>Montant location estimé = pricePerDay × quantity × nbJours.</li>
     *   <li>Caution estimée = depositAmount × quantity.</li>
     *   <li>Le statut initial est automatiquement positionné à {@code PENDING}.</li>
     * </ul>
     *
     * @param clientId  Identifiant du client demandeur
     * @param toolId    Identifiant de l'outil souhaité
     * @param quantity  Quantité demandée (≥ 1)
     * @param startDate Date de début de location
     * @param endDate   Date de fin de location (strictement après startDate)
     * @throws IllegalArgumentException si les paramètres sont invalides
     * @throws IllegalStateException    si l'outil n'est pas disponible en quantité suffisante
     */
    void creerDemande(Long clientId, Long toolId, int quantity,
                      LocalDate startDate, LocalDate endDate);
}
