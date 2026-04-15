package com.bricolirent.service.impl;

import com.bricolirent.domain.entity.Client;
import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.domain.entity.Tool;
import com.bricolirent.domain.enums.ReservationStatus;
import com.bricolirent.repository.ClientRepository;
import com.bricolirent.repository.ReservationRepository;
import com.bricolirent.repository.ToolRepository;
import com.bricolirent.service.ReservationService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named("reservationService")
@ApplicationScoped
public class ReservationServiceImpl implements ReservationService {

    private static final Logger LOGGER = Logger.getLogger(ReservationServiceImpl.class.getName());
    private static final int SCORE_MINIMUM_APPROBATION = 60;
    private static final int LIMITE_LOCATIONS_ACTIVES = 3;
    private static final int QUANTITE_RAISONNABLE = 2;

    private ReservationRepository reservationRepository;
    private ToolRepository toolRepository;
    private ClientRepository clientRepository;

    @PostConstruct
    public void init() {
        this.reservationRepository = new ReservationRepository();
        this.toolRepository = new ToolRepository();
        this.clientRepository = new ClientRepository();
    }

    @Override
    public ReservationEstimate estimerDemande(Long toolId, int quantity, LocalDate startDate, LocalDate endDate) {
        if (toolId == null) {
            return new ReservationEstimate(false, "Selectionnez un outil pour obtenir une estimation.", 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        if (quantity < 1) {
            return new ReservationEstimate(false, "La quantite doit etre d'au moins 1.", 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        if (startDate == null || endDate == null) {
            return new ReservationEstimate(false, "Renseignez les dates pour calculer l'estimation.", 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        if (!startDate.isBefore(endDate)) {
            return new ReservationEstimate(false, "La date de fin doit etre strictement posterieure a la date de debut.", 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        if (startDate.isBefore(LocalDate.now())) {
            return new ReservationEstimate(false, "La date de debut ne peut pas etre dans le passe.", 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        Tool tool = loadTool(toolId);
        if (!Boolean.TRUE.equals(tool.getActive())) {
            return new ReservationEstimate(false, "Cet outil n'est plus disponible a la location.", 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        if (tool.getAvailableQuantity() < quantity) {
            return new ReservationEstimate(false, "Quantite insuffisante. Quantite disponible : " + tool.getAvailableQuantity() + ".", 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        long nbJours = ChronoUnit.DAYS.between(startDate, endDate);
        BigDecimal estimatedRental = calculateEstimatedRental(tool, quantity, nbJours);
        BigDecimal estimatedDeposit = calculateEstimatedDeposit(tool, quantity);

        return new ReservationEstimate(true, "Quantite disponible pour cette demande.", nbJours, estimatedRental, estimatedDeposit);
    }

    @Override
    public ReservationCreationResult creerDemande(Long clientId, Long toolId, int quantity, LocalDate startDate, LocalDate endDate) {
        validateDates(startDate, endDate);
        validateQuantity(quantity);

        Tool tool = loadTool(toolId);
        validateToolAvailability(tool, quantity);
        Client client = loadClient(clientId);

        long nbJours = ChronoUnit.DAYS.between(startDate, endDate);
        BigDecimal estimatedRental = calculateEstimatedRental(tool, quantity, nbJours);
        BigDecimal estimatedDeposit = calculateEstimatedDeposit(tool, quantity);
        Decision decision = determineDecision(client, tool, quantity, startDate, endDate, nbJours);

        Reservation reservation = new Reservation();
        reservation.setQuantity(quantity);
        reservation.setStartDate(startDate);
        reservation.setEndDate(endDate);
        reservation.setReservationDate(Instant.now());
        reservation.setStatus(decision.status());
        reservation.setEstimatedRentalAmount(estimatedRental);
        reservation.setEstimatedDepositAmount(estimatedDeposit);
        reservation.setApprovedAutomatically(decision.automatic());
        reservation.setApprovalReason(decision.reason());
        if (decision.status() != ReservationStatus.PENDING) {
            reservation.setApprovedAt(Instant.now());
        }

        try {
            reservationRepository.saveForClientAndTool(clientId, toolId, reservation);
            LOGGER.info("[ReservationService] Demande creee - client=" + clientId
                    + ", outil=" + tool.getName()
                    + ", du=" + startDate + " au=" + endDate
                    + ", montant=" + estimatedRental
                    + ", caution=" + estimatedDeposit
                    + ", statut=" + decision.status());
            return new ReservationCreationResult(
                    reservation.getId(),
                    decision.status(),
                    decision.clientMessage(),
                    decision.reason());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la persistance de la reservation", e);
            throw new RuntimeException("Impossible d'enregistrer la reservation. Veuillez reessayer.", e);
        }
    }

    @Override
    public List<Reservation> getReservationsByClient(Long clientId) {
        if (clientId == null) {
            throw new IllegalArgumentException("Client invalide.");
        }
        return reservationRepository.findByClientIdWithTool(clientId);
    }

    @Override
    public Reservation getReservationForClient(Long reservationId, Long clientId) {
        if (reservationId == null || clientId == null) {
            throw new IllegalArgumentException("Reservation invalide.");
        }
        return reservationRepository.findByIdAndClientId(reservationId, clientId)
                .orElseThrow(() -> new IllegalStateException("La demande selectionnee est introuvable."));
    }

    @Override
    public void annulerDemande(Long reservationId, Long clientId) {
        Reservation reservation = getReservationForClient(reservationId, clientId);

        if (reservation.getStatus() == ReservationStatus.CHECKED_OUT || reservation.getStatus() == ReservationStatus.RETURNED) {
            throw new IllegalStateException("Cette demande ne peut plus etre annulee car le check-out a deja ete effectue.");
        }
        if (reservation.getStatus() == ReservationStatus.REJECTED) {
            throw new IllegalStateException("Cette demande est deja annulee ou refusee.");
        }

        reservation.setStatus(ReservationStatus.REJECTED);
        reservation.setApprovalReason("Demande annulee par le client.");

        try {
            reservationRepository.update(reservation);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'annulation de la reservation", e);
            throw new RuntimeException("Impossible d'annuler la demande pour le moment.", e);
        }
    }

    @Override
    public List<Reservation> getPendingReservations() {
        return reservationRepository.findByStatusWithToolAndClient(ReservationStatus.PENDING);
    }

    @Override
    public List<Reservation> getHandledReservationsByAgent(Long agentId) {
        if (agentId == null) {
            throw new IllegalArgumentException("Agent invalide.");
        }
        return reservationRepository.findHandledByAgent(agentId);
    }

    @Override
    public void approuverDemande(Long reservationId, Long agentId) {
        Reservation reservation = reservationRepository.findByIdWithToolAndClient(reservationId)
                .orElseThrow(() -> new IllegalStateException("La demande selectionnee est introuvable."));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Seules les demandes en attente peuvent etre approuvees.");
        }

        String reason = "Demande approuvee manuellement par un agent.";
        reservationRepository.updateDecision(reservationId, agentId, ReservationStatus.APPROVED, reason, false);
    }

    @Override
    public void rejeterDemande(Long reservationId, Long agentId, String reason) {
        Reservation reservation = reservationRepository.findByIdWithToolAndClient(reservationId)
                .orElseThrow(() -> new IllegalStateException("La demande selectionnee est introuvable."));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Seules les demandes en attente peuvent etre rejetees.");
        }

        String normalizedReason = (reason == null || reason.trim().isEmpty())
                ? "Demande rejetee manuellement par un agent."
                : reason.trim();

        reservationRepository.updateDecision(reservationId, agentId, ReservationStatus.REJECTED, normalizedReason, false);
    }

    private Tool loadTool(Long toolId) {
        Optional<Tool> toolOpt = toolRepository.findById(toolId);
        if (toolOpt.isEmpty()) {
            throw new IllegalArgumentException("L'outil selectionne n'existe pas.");
        }
        return toolOpt.get();
    }

    private Client loadClient(Long clientId) {
        Optional<Client> clientOpt = clientRepository.findByIdWithUser(clientId);
        if (clientOpt.isEmpty()) {
            throw new IllegalArgumentException("Client introuvable (ID=" + clientId + ").");
        }
        return clientOpt.get();
    }

    private Decision determineDecision(Client client, Tool tool, int quantity, LocalDate startDate, LocalDate endDate, long nbJours) {
        if (!Boolean.TRUE.equals(client.getUsers().getActive())) {
            return new Decision(
                    ReservationStatus.REJECTED,
                    "Demande rejetee automatiquement : compte client inactif.",
                    "Compte client inactif.",
                    true
            );
        }

        int score = client.getScore() == null ? 0 : client.getScore();
        if (score < SCORE_MINIMUM_APPROBATION) {
            return new Decision(
                    ReservationStatus.REJECTED,
                    "Demande rejetee automatiquement : votre score client est insuffisant.",
                    "Score client insuffisant (" + score + ").",
                    true
            );
        }

        long activeReservations = reservationRepository.countActiveReservationsForClient(client.getId());
        if (activeReservations >= LIMITE_LOCATIONS_ACTIVES) {
            return new Decision(
                    ReservationStatus.REJECTED,
                    "Demande rejetee automatiquement : la limite simple de locations actives est atteinte.",
                    "Limite de locations actives atteinte (" + activeReservations + ").",
                    true
            );
        }

        boolean stockConfortable = (tool.getAvailableQuantity() - quantity) >= 2;
        boolean quantiteRaisonnable = quantity <= QUANTITE_RAISONNABLE;
        boolean quantiteProcheDuStock = quantity >= tool.getAvailableQuantity() || quantity >= Math.max(1, tool.getTotalQuantity() - 1);

        if (nbJours <= 2 && quantiteRaisonnable && stockConfortable) {
            return new Decision(
                    ReservationStatus.APPROVED,
                    "Votre demande a ete approuvee automatiquement.",
                    "Approbation automatique : duree courte, stock confortable et quantite raisonnable.",
                    true
            );
        }

        if (nbJours > 2 || quantiteProcheDuStock) {
            return new Decision(
                    ReservationStatus.PENDING,
                    "Votre demande est en attente de validation par un agent.",
                    "Validation manuelle requise : duree longue ou quantite proche du stock disponible.",
                    false
            );
        }

        return new Decision(
                ReservationStatus.PENDING,
                "Votre demande est en attente de validation par un agent.",
                "Validation manuelle requise pour ce profil de demande.",
                false
        );
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Les dates de debut et de fin sont obligatoires.");
        }
        if (!startDate.isBefore(endDate)) {
            throw new IllegalArgumentException("La date de fin doit etre strictement posterieure a la date de debut.");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La date de debut ne peut pas etre dans le passe.");
        }
    }

    private void validateQuantity(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("La quantite doit etre d'au moins 1.");
        }
    }

    private void validateToolAvailability(Tool tool, int quantity) {
        if (!Boolean.TRUE.equals(tool.getActive())) {
            throw new IllegalStateException("Cet outil n'est plus disponible a la location.");
        }
        if (tool.getAvailableQuantity() < quantity) {
            throw new IllegalStateException("Quantite insuffisante. Quantite disponible : " + tool.getAvailableQuantity() + ".");
        }
    }

    private BigDecimal calculateEstimatedRental(Tool tool, int quantity, long rentalDays) {
        return tool.getPricePerDay()
                .multiply(BigDecimal.valueOf(quantity))
                .multiply(BigDecimal.valueOf(rentalDays));
    }

    private BigDecimal calculateEstimatedDeposit(Tool tool, int quantity) {
        return tool.getDepositAmount()
                .multiply(BigDecimal.valueOf(quantity));
    }

    private record Decision(ReservationStatus status, String clientMessage, String reason, boolean automatic) {
    }
}
