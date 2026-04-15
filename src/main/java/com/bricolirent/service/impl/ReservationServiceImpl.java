package com.bricolirent.service.impl;

import com.bricolirent.domain.entity.Client;
import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.domain.entity.Tool;
import com.bricolirent.domain.enums.ReservationStatus;
import com.bricolirent.repository.ClientRepository;
import com.bricolirent.repository.ReservationRepository;
import com.bricolirent.repository.ToolRepository;
import com.bricolirent.service.ReservationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implémentation CDI de {@link ReservationService}.
 *
 * <p>Portée {@code @ApplicationScoped} : une seule instance partagée.
 * Aucun état mutable — tous les paramètres sont passés en argument.</p>
 *
 * <p>Utilise exclusivement les repositories Hibernate natifs existants,
 * sans EJB, ni Spring, ni EntityManager direct.</p>
 */
@Named("reservationService")
@ApplicationScoped
public class ReservationServiceImpl implements ReservationService {

    private static final Logger LOGGER = Logger.getLogger(ReservationServiceImpl.class.getName());

    /* ------------------------------------------------------------------ */
    /* Repositories — instanciés ici car non-injectable sans conteneur EJB */
    /* ------------------------------------------------------------------ */

    private final ReservationRepository reservationRepository = new ReservationRepository();
    private final ToolRepository        toolRepository        = new ToolRepository();
    private final ClientRepository      clientRepository      = new ClientRepository();

    // ====================================================================
    // Implémentation
    // ====================================================================

    /**
     * {@inheritDoc}
     *
     * <p>Déroulement détaillé :</p>
     * <ol>
     *   <li>Validation des dates (non nulles, cohérence, non passées).</li>
     *   <li>Validation de la quantité (≥ 1).</li>
     *   <li>Chargement et vérification de l'outil (actif, quantité dispo).</li>
     *   <li>Chargement du client.</li>
     *   <li>Calcul des montants estimés.</li>
     *   <li>Persistance via {@link ReservationRepository#save(Object)}.</li>
     * </ol>
     */
    @Override
    public void creerDemande(Long clientId, Long toolId, int quantity,
                             LocalDate startDate, LocalDate endDate) {

        // ---- 1. Validation des dates ----------------------------------------
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires.");
        }
        if (!startDate.isBefore(endDate)) {
            throw new IllegalArgumentException(
                    "La date de fin doit être strictement postérieure à la date de début.");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La date de début ne peut pas être dans le passé.");
        }

        // ---- 2. Validation de la quantité ----------------------------------
        if (quantity < 1) {
            throw new IllegalArgumentException("La quantité doit être d'au moins 1.");
        }

        // ---- 3. Chargement et vérification de l'outil ----------------------
        Optional<Tool> toolOpt = toolRepository.findById(toolId);
        if (toolOpt.isEmpty()) {
            throw new IllegalArgumentException("L'outil sélectionné n'existe pas.");
        }
        Tool tool = toolOpt.get();

        if (!Boolean.TRUE.equals(tool.getActive())) {
            throw new IllegalStateException("Cet outil n'est plus disponible à la location.");
        }
        if (tool.getAvailableQuantity() < quantity) {
            throw new IllegalStateException(
                    "Quantité insuffisante. Quantité disponible : " + tool.getAvailableQuantity() + ".");
        }

        // ---- 4. Chargement du client ----------------------------------------
        Optional<Client> clientOpt = clientRepository.findById(clientId);
        if (clientOpt.isEmpty()) {
            throw new IllegalArgumentException("Client introuvable (ID=" + clientId + ").");
        }
        Client client = clientOpt.get();

        // ---- 5. Calcul des montants estimés ---------------------------------
        long nbJours = ChronoUnit.DAYS.between(startDate, endDate);

        BigDecimal estimatedRental = tool.getPricePerDay()
                .multiply(BigDecimal.valueOf(quantity))
                .multiply(BigDecimal.valueOf(nbJours));

        BigDecimal estimatedDeposit = tool.getDepositAmount()
                .multiply(BigDecimal.valueOf(quantity));

        // ---- 6. Construction de la réservation ------------------------------
        Reservation reservation = new Reservation();
        reservation.setClient(client);
        reservation.setTool(tool);
        reservation.setQuantity(quantity);
        reservation.setStartDate(startDate);
        reservation.setEndDate(endDate);
        reservation.setReservationDate(Instant.now());
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setEstimatedRentalAmount(estimatedRental);
        reservation.setEstimatedDepositAmount(estimatedDeposit);
        reservation.setApprovedAutomatically(false);

        // ---- 7. Persistance -------------------------------------------------
        try {
            reservationRepository.save(reservation);
            LOGGER.info("[ReservationService] Demande créée — client=" + clientId
                    + ", outil=" + tool.getName()
                    + ", du=" + startDate + " au=" + endDate
                    + ", montant=" + estimatedRental + " MAD"
                    + ", caution=" + estimatedDeposit + " MAD");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la persistance de la réservation", e);
            throw new RuntimeException(
                    "Impossible d'enregistrer la réservation. Veuillez réessayer.", e);
        }
    }
}
