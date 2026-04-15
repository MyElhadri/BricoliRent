package com.bricolirent.web.bean;

import com.bricolirent.domain.entity.Client;
import com.bricolirent.domain.entity.Tool;
import com.bricolirent.domain.entity.User;
import com.bricolirent.repository.ClientRepository;
import com.bricolirent.repository.ToolRepository;
import com.bricolirent.service.ReservationService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bean JSF gérant le formulaire de nouvelle demande de réservation (côté client).
 *
 * <p>Portée {@code @ViewScoped} : une instance par vue, détruite à la navigation.
 * Doit implémenter {@link Serializable} (requis par CDI pour les beans view-scoped).</p>
 *
 * <p><strong>Responsabilités :</strong></p>
 * <ul>
 *   <li>Charger la liste des outils disponibles au {@code @PostConstruct}</li>
 *   <li>Porter les champs du formulaire (outil, quantité, dates)</li>
 *   <li>Déléguer la création de la demande au {@link ReservationService}</li>
 *   <li>Afficher un message JSF de succès ou d'erreur</li>
 * </ul>
 */
@Named("reservationBean")
@ViewScoped
public class ReservationBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ReservationBean.class.getName());

    // =====================================================================
    // Injection CDI
    // =====================================================================

    /** Bean de session portant l'utilisateur connecté. */
    @Inject
    private LoginBean loginBean;

    /** Service métier de réservation. */
    @Inject
    private ReservationService reservationService;

    // =====================================================================
    // Repositories (lecture seule, pas de service dédié pour ce bloc)
    // =====================================================================

    private final ToolRepository   toolRepository   = new ToolRepository();
    private final ClientRepository clientRepository = new ClientRepository();

    // =====================================================================
    // Données chargées à l'init
    // =====================================================================

    /** Outils actifs avec quantité disponible > 0, affichés dans le menu déroulant. */
    private List<Tool> outilsDisponibles = Collections.emptyList();

    // =====================================================================
    // Champs du formulaire
    // =====================================================================

    /** Identifiant de l'outil choisi dans le select. */
    private Long outilSelectionneId;

    /** Quantité demandée (défaut : 1). */
    private int quantite = 1;

    /** Date de début de la période de location. */
    private LocalDate dateDebut;

    /** Date de fin de la période de location. */
    private LocalDate dateFin;

    // =====================================================================
    // Initialisation
    // =====================================================================

    /**
     * Charge la liste des outils disponibles après injection des dépendances.
     * En cas d'erreur (base inaccessible), la liste reste vide sans bloquer la page.
     */
    @PostConstruct
    public void init() {
        try {
            outilsDisponibles = toolRepository.findAvailableTools();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Impossible de charger les outils disponibles", e);
            outilsDisponibles = Collections.emptyList();
        }
    }

    // =====================================================================
    // Action JSF
    // =====================================================================

    /**
     * Soumet la demande de réservation.
     *
     * <ol>
     *   <li>Vérifie que l'utilisateur connecté est bien un client.</li>
     *   <li>Délègue la validation métier et la persistance au service.</li>
     *   <li>Affiche un {@link FacesMessage} de succès ou d'erreur.</li>
     *   <li>Réinitialise le formulaire en cas de succès.</li>
     * </ol>
     *
     * @return {@code null} — reste sur la même page dans tous les cas
     */
    public String soumettreDemande() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        // Récupération du client connecté
        Client client = getClientConnecte();
        if (client == null) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Accès refusé",
                    "Vous devez être connecté en tant que client pour soumettre une demande."
            ));
            return null;
        }

        // Vérification que l'outil a bien été sélectionné
        if (outilSelectionneId == null) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Outil manquant",
                    "Veuillez sélectionner un outil dans la liste."
            ));
            return null;
        }

        try {
            reservationService.creerDemande(
                    client.getId(),
                    outilSelectionneId,
                    quantite,
                    dateDebut,
                    dateFin
            );

            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Demande envoyée",
                    "Votre demande est enregistrée et en attente de validation par un agent."
            ));

            reinitialiserFormulaire();

        } catch (IllegalArgumentException | IllegalStateException e) {
            // Erreur métier prévisible → message lisible pour l'utilisateur
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN,
                    "Demande non enregistrée",
                    e.getMessage()
            ));
        } catch (Exception e) {
            // Erreur technique → message générique
            LOGGER.log(Level.SEVERE, "Erreur inattendue lors de la création d'une réservation", e);
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Erreur technique",
                    "Une erreur est survenue. Veuillez réessayer ultérieurement."
            ));
        }

        return null; // rester sur new-request.xhtml
    }

    // =====================================================================
    // Méthodes utilitaires
    // =====================================================================

    /**
     * Retourne l'entité {@link Client} associée à l'utilisateur actuellement connecté.
     * Retourne {@code null} si personne n'est connecté ou si l'utilisateur n'est pas client.
     */
    public Client getClientConnecte() {
        if (!loginBean.isLoggedIn()) {
            return null;
        }
        User user = loginBean.getCurrentUser();
        Optional<Client> opt = clientRepository.findById(user.getId());
        return opt.orElse(null);
    }

    /**
     * Réinitialise les champs du formulaire après une soumission réussie.
     */
    private void reinitialiserFormulaire() {
        outilSelectionneId = null;
        quantite           = 1;
        dateDebut          = null;
        dateFin            = null;
    }

    // =====================================================================
    // Getters / Setters
    // =====================================================================

    public List<Tool> getOutilsDisponibles()          { return outilsDisponibles; }

    public Long getOutilSelectionneId()               { return outilSelectionneId; }
    public void setOutilSelectionneId(Long id)        { this.outilSelectionneId = id; }

    public int getQuantite()                          { return quantite; }
    public void setQuantite(int quantite)             { this.quantite = quantite; }

    public LocalDate getDateDebut()                   { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut)     { this.dateDebut = dateDebut; }

    public LocalDate getDateFin()                     { return dateFin; }
    public void setDateFin(LocalDate dateFin)         { this.dateFin = dateFin; }
}
