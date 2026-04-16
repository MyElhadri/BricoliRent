package com.bricolirent.web.bean;

import com.bricolirent.domain.entity.Client;
import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.domain.entity.Tool;
import com.bricolirent.domain.entity.User;
import com.bricolirent.domain.enums.ReservationStatus;
import com.bricolirent.repository.ClientRepository;
import com.bricolirent.repository.ToolRepository;
import com.bricolirent.service.ReservationService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named("reservationBean")
@ViewScoped
public class ReservationBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ReservationBean.class.getName());

    @Inject
    private LoginBean loginBean;

    @Inject
    private ReservationService reservationService;

    private final ToolRepository toolRepository = new ToolRepository();
    private final ClientRepository clientRepository = new ClientRepository();

    private List<Tool> outilsDisponibles = Collections.emptyList();
    private List<Reservation> reservations = Collections.emptyList();

    private Long outilSelectionneId;
    private int quantite = 1;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    private Long reservationId;
    private Long toolId;
    private Reservation currentReservation;

    @PostConstruct
    public void init() {
        chargerOutilsDisponibles();
        preselectionnerOutilDepuisParametre();
        chargerReservationsClient();
    }

    public void chargerDetailReservation() {
        currentReservation = null;
        Client client = getClientConnecte();
        if (client == null || reservationId == null) {
            return;
        }
        try {
            currentReservation = reservationService.getReservationForClient(reservationId, client.getId());
        } catch (IllegalArgumentException | IllegalStateException e) {
            addMessage(FacesMessage.SEVERITY_WARN, "Demande introuvable", e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement du detail reservation", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur technique", "Impossible de charger cette demande pour le moment.");
        }
    }

    public String soumettreDemande() {
        Client client = getClientConnecte();
        if (client == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Acces refuse", "Vous devez etre connecte en tant que client pour soumettre une demande.");
            return null;
        }
        if (outilSelectionneId == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Outil manquant", "Veuillez selectionner un outil dans la liste.");
            return null;
        }

        try {
            ReservationService.ReservationCreationResult result =
                    reservationService.creerDemande(client.getId(), outilSelectionneId, quantite, dateDebut, dateFin);

            FacesMessage.Severity severity = switch (result.getStatus()) {
                case APPROVED -> FacesMessage.SEVERITY_INFO;
                case PENDING -> FacesMessage.SEVERITY_WARN;
                case REJECTED -> FacesMessage.SEVERITY_ERROR;
                default -> FacesMessage.SEVERITY_INFO;
            };

            String summary = switch (result.getStatus()) {
                case APPROVED -> "Demande approuvee";
                case PENDING -> "Demande en attente";
                case REJECTED -> "Demande rejetee";
                default -> "Demande enregistree";
            };

            addMessage(severity, summary, result.getMessage());
            reinitialiserFormulaire();
            chargerOutilsDisponibles();
            chargerReservationsClient();
        } catch (IllegalArgumentException | IllegalStateException e) {
            addMessage(FacesMessage.SEVERITY_WARN, "Demande non enregistree", e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur inattendue lors de la creation d'une reservation", e);
            String fullError = e.getClass().getName() + ": " + e.getMessage();
            if (e.getCause() != null) {
                fullError += " | Cause: " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage();
            }
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur technique détaillée", fullError);
        }
        return null;
    }

    public String annulerReservationDepuisListe(Long reservationId) {
        return annulerReservation(reservationId, false);
    }

    public String annulerReservationCourante() {
        return annulerReservation(reservationId, true);
    }

    private String annulerReservation(Long targetReservationId, boolean redirectAfter) {
        Client client = getClientConnecte();
        if (client == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Acces refuse", "Vous devez etre connecte en tant que client.");
            return null;
        }

        try {
            reservationService.annulerDemande(targetReservationId, client.getId());
            addMessage(FacesMessage.SEVERITY_INFO, "Demande annulee", "La demande a bien ete annulee.");
            chargerReservationsClient();
            if (currentReservation != null && targetReservationId != null && targetReservationId.equals(currentReservation.getId())) {
                currentReservation = reservationService.getReservationForClient(targetReservationId, client.getId());
            }
            return redirectAfter ? "/app/client/my-requests.xhtml?faces-redirect=true" : null;
        } catch (IllegalArgumentException | IllegalStateException e) {
            addMessage(FacesMessage.SEVERITY_WARN, "Annulation impossible", e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'annulation de la reservation", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Erreur technique", "Impossible d'annuler la demande pour le moment.");
        }
        return null;
    }

    public ReservationService.ReservationEstimate getEstimation() {
        if (outilSelectionneId == null) {
            return new ReservationService.ReservationEstimate(false, "Sélectionnez un outil pour obtenir une estimation.", 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        try {
            return reservationService.estimerDemande(outilSelectionneId, quantite, dateDebut, dateFin);
        } catch (IllegalArgumentException e) {
            return new ReservationService.ReservationEstimate(false, e.getMessage(), 0, BigDecimal.ZERO, BigDecimal.ZERO);
        } catch (Exception e) {
            return new ReservationService.ReservationEstimate(false, "Estimation impossible (données manquantes ou invalides).", 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    public Tool getOutilSelectionne() {
        if (outilSelectionneId == null) {
            return null;
        }
        return outilsDisponibles.stream()
                .filter(tool -> outilSelectionneId.equals(tool.getId()))
                .findFirst()
                .orElseGet(() -> toolRepository.findById(outilSelectionneId).orElse(null));
    }

    public boolean canCancel(Reservation reservation) {
        if (reservation == null || reservation.getStatus() == null) {
            return false;
        }
        return reservation.getStatus() == ReservationStatus.PENDING || reservation.getStatus() == ReservationStatus.APPROVED;
    }

    public String displayStatus(Reservation reservation) {
        if (reservation == null || reservation.getStatus() == null) {
            return "Inconnue";
        }
        return switch (reservation.getStatus()) {
            case PENDING -> "En attente";
            case APPROVED -> "Approuvee";
            case REJECTED -> getDecisionMotif(reservation).toLowerCase().contains("annulee") ? "Annulee" : "Rejetee";
            case CHECKED_OUT -> "Check-out effectue";
            case RETURNED -> "Retournee";
        };
    }

    public String statusTone(Reservation reservation) {
        if (reservation == null || reservation.getStatus() == null) {
            return "neutral";
        }
        return switch (reservation.getStatus()) {
            case PENDING -> "warning";
            case APPROVED -> "info";
            case REJECTED -> "danger";
            case CHECKED_OUT, RETURNED -> "success";
        };
    }

    public Client getClientConnecte() {
        if (!loginBean.isLoggedIn()) {
            return null;
        }
        User user = loginBean.getCurrentUser();
        Optional<Client> opt = clientRepository.findById(user.getId());
        return opt.orElse(null);
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public Long getToolId() {
        return toolId;
    }

    public void setToolId(Long toolId) {
        this.toolId = toolId;
    }

    public List<Tool> getOutilsDisponibles() {
        return outilsDisponibles;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public Long getOutilSelectionneId() {
        return outilSelectionneId;
    }

    public void setOutilSelectionneId(Long outilSelectionneId) {
        this.outilSelectionneId = outilSelectionneId;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public Reservation getCurrentReservation() {
        return currentReservation;
    }

    public String getDecisionMotif(Reservation reservation) {
        if (reservation == null || reservation.getApprovalReason() == null || reservation.getApprovalReason().isBlank()) {
            return "Aucun motif detaille disponible.";
        }
        return reservation.getApprovalReason();
    }

    public String decisionMotif(Reservation reservation) {
        return getDecisionMotif(reservation);
    }

    private void chargerOutilsDisponibles() {
        try {
            outilsDisponibles = toolRepository.findAvailableTools();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Impossible de charger les outils disponibles", e);
            outilsDisponibles = Collections.emptyList();
        }
    }

    private void chargerReservationsClient() {
        Client client = getClientConnecte();
        if (client == null) {
            reservations = Collections.emptyList();
            return;
        }
        try {
            reservations = reservationService.getReservationsByClient(client.getId());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Impossible de charger les reservations du client", e);
            reservations = Collections.emptyList();
        }
    }

    private void preselectionnerOutilDepuisParametre() {
        if (outilSelectionneId != null) {
            return;
        }

        Long candidateId = toolId;
        if (candidateId == null) {
            ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
            String rawToolId = externalContext.getRequestParameterMap().get("toolId");
            if (rawToolId != null && !rawToolId.isBlank()) {
                try {
                    candidateId = Long.valueOf(rawToolId);
                } catch (NumberFormatException ignored) {
                    candidateId = null;
                }
            }
        }

        if (candidateId == null) {
            return;
        }

        final Long selectedId = candidateId;
        boolean available = outilsDisponibles.stream().anyMatch(tool -> selectedId.equals(tool.getId()));
        if (available) {
            outilSelectionneId = selectedId;
            toolId = selectedId;
        }
    }

    private void reinitialiserFormulaire() {
        outilSelectionneId = null;
        quantite = 1;
        dateDebut = null;
        dateFin = null;
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
    }
}
