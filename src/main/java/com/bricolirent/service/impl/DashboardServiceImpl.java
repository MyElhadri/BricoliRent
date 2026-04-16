package com.bricolirent.service.impl;

import com.bricolirent.domain.entity.Client;
import com.bricolirent.domain.entity.Payment;
import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.domain.entity.ReturnRecord;
import com.bricolirent.domain.enums.PaymentStatus;
import com.bricolirent.domain.enums.ReservationStatus;
import com.bricolirent.service.AdminSupervisionService;
import com.bricolirent.repository.ClientRepository;
import com.bricolirent.repository.PaymentRepository;
import com.bricolirent.repository.ReservationRepository;
import com.bricolirent.repository.ReturnRecordRepository;
import com.bricolirent.service.DashboardService;
import com.bricolirent.util.HibernateUtil;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Comparator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    @Inject
    private AdminSupervisionService adminSupervisionService;
    private ReservationRepository reservationRepository;
    private PaymentRepository paymentRepository;
    private ReturnRecordRepository returnRecordRepository;
    private ClientRepository clientRepository;

    @PostConstruct
    public void init() {
        this.reservationRepository = new ReservationRepository();
        this.paymentRepository = new PaymentRepository();
        this.returnRecordRepository = new ReturnRecordRepository();
        this.clientRepository = new ClientRepository();
    }

    @Override
    public DashboardViewData buildAdminDashboard() {
        try {
            AdminSupervisionService.AdminDashboardSnapshot snapshot = adminSupervisionService.getDashboardSnapshot();
            List<Reservation> recentReservations = adminSupervisionService.getAllReservations().stream()
                    .sorted(Comparator.comparing(Reservation::getReservationDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .limit(6)
                    .toList();
            DashboardViewData data = new DashboardViewData();
            data.setRoleLabel("ADMIN");
            data.setSubtitle("Vue transverse sur les reservations, les paiements, les comptes et le catalogue de la plateforme.");
            data.setMetricsTitle("Pilotage global");
            data.setLinksTitle("Acces rapides");
            data.setActivitiesTitle("Reservations recentes");
            data.setActivitiesEmptyMessage("Aucune reservation recente a afficher.");

            data.getMetrics().add(new DashboardMetric("Reservations", String.valueOf(snapshot.getTotalReservations()), snapshot.getPendingReservations() + " en attente", "info"));
            data.getMetrics().add(new DashboardMetric("Approuvees", String.valueOf(snapshot.getApprovedReservations()), snapshot.getCheckedOutReservations() + " check-out effectue(s)", "success"));
            data.getMetrics().add(new DashboardMetric("Rejetees", String.valueOf(snapshot.getRejectedReservations()), snapshot.getReturnedReservations() + " retournee(s)", "danger"));
            data.getMetrics().add(new DashboardMetric("Paiements", String.valueOf(snapshot.getTotalPayments()), "Flux cash traces dans le systeme", "warning"));
            data.getMetrics().add(new DashboardMetric("Locations", formatMoney(snapshot.getTotalRentalRevenue()), "Locations encaissees", "info"));
            data.getMetrics().add(new DashboardMetric("Cautions", formatMoney(snapshot.getTotalDepositsCollected()), "Cautions encaissees", "warning"));
            data.getMetrics().add(new DashboardMetric("Penalites", formatMoney(snapshot.getTotalLatePenalties()), "Retards encaisses", "danger"));
            data.getMetrics().add(new DashboardMetric("Remboursements", formatMoney(snapshot.getTotalRefunds()), "Cautions remboursees", "success"));
            data.getMetrics().add(new DashboardMetric("Encours caution", formatMoney(snapshot.getOutstandingDeposits()), "Cautions encore detenues", "neutral"));
            data.getMetrics().add(new DashboardMetric("Utilisateurs", String.valueOf(snapshot.getTotalUsers()), snapshot.getTotalClients() + " client(s)", "neutral"));
            data.getMetrics().add(new DashboardMetric("Agents", String.valueOf(snapshot.getTotalAgents()), "Comptes terrain disponibles", "warning"));
            data.getMetrics().add(new DashboardMetric("Catalogue", String.valueOf(snapshot.getTotalActiveTools()), snapshot.getTotalCategories() + " categorie(s)", "success"));

            data.getQuickLinks().add(new DashboardLink("Ajouter un outil", "Acceder a la gestion du catalogue interne.", "/app/admin/tools.xhtml"));
            data.getQuickLinks().add(new DashboardLink("Gerer les categories", "Organiser les familles d'outils.", "/app/admin/categories.xhtml"));
            data.getQuickLinks().add(new DashboardLink("Gerer les agents", "Creer, activer ou supprimer un agent.", "/app/admin/agents.xhtml"));
            data.getQuickLinks().add(new DashboardLink("Gerer les utilisateurs", "Consulter les comptes et leur statut.", "/app/admin/users.xhtml"));
            data.getQuickLinks().add(new DashboardLink("Toutes les reservations", "Superviser les reservations du systeme.", "/app/admin/reservations.xhtml"));
            data.getQuickLinks().add(new DashboardLink("Tous les paiements", "Analyser les flux cash et remboursements.", "/app/admin/payments.xhtml"));
            data.getQuickLinks().add(new DashboardLink("Voir le catalogue", "Consulter le catalogue client/public.", "/app/catalog/tools.xhtml"));

            for (Reservation reservation : recentReservations) {
                String title = reservation.getTool() != null ? reservation.getTool().getName() : "Reservation";
                String clientName = reservation.getClient() != null && reservation.getClient().getUsers() != null
                        ? reservation.getClient().getUsers().getFullName()
                        : "Client inconnu";
                String subtitle = clientName + " - " + formatDate(reservation.getStartDate()) + " au " + formatDate(reservation.getEndDate());
                data.getActivities().add(new DashboardActivity(title, subtitle, formatReservationStatus(reservation.getStatus()), toneForReservation(reservation.getStatus())));
            }
            return data;
        } catch (Exception e) {
            throw new RuntimeException("Impossible de charger le dashboard administrateur.", e);
        }
    }

    @Override
    public DashboardViewData buildAgentDashboard(Long agentUserId) {
        try {
            DashboardViewData data = new DashboardViewData();
            data.setRoleLabel("AGENT");
            data.setSubtitle("Vue operationnelle sur les demandes, les locations en cours, les paiements et les retours.");
            data.setMetricsTitle("File d'activite");
            data.setActivitiesTitle("Demandes recentes a suivre");
            data.setActivitiesEmptyMessage("Aucune demande recente a afficher pour le moment.");

            List<Reservation> reservations = reservationRepository.findAll();
            List<Payment> payments = paymentRepository.findAll();
            List<ReturnRecord> returnRecords = returnRecordRepository.findAll();

            long approvedCount = reservations.stream()
                    .filter(r -> r.getStatus() == ReservationStatus.APPROVED)
                    .count();
            long checkedOutCount = reservations.stream()
                    .filter(r -> r.getStatus() == ReservationStatus.CHECKED_OUT)
                    .count();
            long pendingPaymentCount = payments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                    .count();
            long returnCount = returnRecords.size();

            data.getMetrics().add(new DashboardMetric("Demandes approuvees", String.valueOf(approvedCount), "Pretes pour check-out", "warning"));
            data.getMetrics().add(new DashboardMetric("Locations en cours", String.valueOf(checkedOutCount), "Reservations actuellement sorties", "info"));
            data.getMetrics().add(new DashboardMetric("Paiements en attente", String.valueOf(pendingPaymentCount), "Paiements encore a enregistrer", "danger"));
            data.getMetrics().add(new DashboardMetric("Retours enregistres", String.valueOf(returnCount), "Retours deja traites", "success"));

            reservations.stream()
                    .filter(r -> r.getStatus() == ReservationStatus.APPROVED || r.getStatus() == ReservationStatus.CHECKED_OUT)
                    .sorted(Comparator.comparing(Reservation::getReservationDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .limit(5)
                    .forEach(r -> {
                        String subtitle = "Periode : " + formatDate(r.getStartDate()) + " au " + formatDate(r.getEndDate());
                        data.getActivities().add(new DashboardActivity("Reservation #" + r.getId(), subtitle, formatReservationStatus(r.getStatus()), toneForReservation(r.getStatus())));
                    });
            return data;
        } catch (Exception e) {
            return buildAgentFallback();
        }
    }

    @Override
    public DashboardViewData buildClientDashboard(Long clientUserId) {
        try {
            DashboardViewData data = new DashboardViewData();
            data.setRoleLabel("CLIENT");
            data.setSubtitle("Retrouvez votre score, vos demandes et les acces utiles pour lancer une nouvelle reservation.");
            data.setMetricsTitle("Synthese de votre compte");
            data.setLinksTitle("Acces rapides");
            data.setActivitiesTitle("Dernieres demandes");
            data.setActivitiesEmptyMessage("Vous n'avez encore aucune demande enregistree.");

            Optional<Client> client = clientRepository.findById(clientUserId);
            List<Reservation> reservations = reservationRepository.findByClientId(clientUserId);

            int score = client.map(Client::getScore).orElse(0);
            long reservationCount = reservations.size();
            long pendingCount = reservations.stream().filter(r -> r.getStatus() == ReservationStatus.PENDING).count();
            long approvedCount = reservations.stream().filter(r -> r.getStatus() == ReservationStatus.APPROVED).count();
            long checkedOutCount = reservations.stream().filter(r -> r.getStatus() == ReservationStatus.CHECKED_OUT).count();

            data.getMetrics().add(new DashboardMetric("Score client", String.valueOf(score), "Niveau actuel de confiance", "success"));
            data.getMetrics().add(new DashboardMetric("Demandes totales", String.valueOf(reservationCount), "Toutes vos reservations confondues", "info"));
            data.getMetrics().add(new DashboardMetric("En attente", String.valueOf(pendingCount), "Demandes encore a valider", "warning"));
            data.getMetrics().add(new DashboardMetric("En cours", String.valueOf(checkedOutCount), approvedCount + " demande(s) approuvee(s)", "neutral"));

            data.getQuickLinks().add(new DashboardLink("Voir le catalogue", "Consulter les outils disponibles.", "/app/catalog/tools.xhtml"));
            data.getQuickLinks().add(new DashboardLink("Nouvelle demande", "Soumettre une nouvelle reservation.", "/app/client/new-request.xhtml"));

            reservations.stream()
                    .sorted(Comparator.comparing(Reservation::getReservationDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .limit(5)
                    .forEach(r -> {
                        String subtitle = "Periode : " + formatDate(r.getStartDate()) + " au " + formatDate(r.getEndDate());
                        data.getActivities().add(new DashboardActivity("Demande #" + r.getId(), subtitle, formatReservationStatus(r.getStatus()), toneForReservation(r.getStatus())));
                    });
            return data;
        } catch (Exception e) {
            return buildClientFallback();
        }
    }

    private long count(Session session, String hql) {
        Long result = session.createQuery(hql, Long.class).getSingleResult();
        return result == null ? 0L : result;
    }

    private long count(Session session, String hql, String parameterName, Object parameterValue) {
        Long result = session.createQuery(hql, Long.class)
                .setParameter(parameterName, parameterValue)
                .getSingleResult();
        return result == null ? 0L : result;
    }

    private long count(Session session, String hql, String parameterName1, Object parameterValue1, String parameterName2, Object parameterValue2) {
        Long result = session.createQuery(hql, Long.class)
                .setParameter(parameterName1, parameterValue1)
                .setParameter(parameterName2, parameterValue2)
                .getSingleResult();
        return result == null ? 0L : result;
    }

    private String resolveAccountType(Long userId, Set<Long> adminIds, Set<Long> agentIds, Set<Long> clientIds) {
        if (adminIds.contains(userId)) {
            return "ADMIN";
        }
        if (agentIds.contains(userId)) {
            return "AGENT";
        }
        if (clientIds.contains(userId)) {
            return "CLIENT";
        }
        return "INCONNU";
    }

    private String formatMoney(java.math.BigDecimal amount) {
        java.math.BigDecimal safeAmount = amount == null ? java.math.BigDecimal.ZERO : amount;
        return String.format(Locale.FRANCE, "%,.2f MAD", safeAmount);
    }

    private String formatReservationStatus(ReservationStatus status) {
        if (status == null) {
            return "Inconnu";
        }
        return switch (status) {
            case PENDING -> "En attente";
            case APPROVED -> "Approuvee";
            case REJECTED -> "Refusee";
            case CHECKED_OUT -> "En cours";
            case RETURNED -> "Retournee";
        };
    }

    private String toneForReservation(ReservationStatus status) {
        if (status == null) {
            return "neutral";
        }
        return switch (status) {
            case PENDING -> "warning";
            case APPROVED -> "info";
            case CHECKED_OUT -> "success";
            case RETURNED -> "success";
            case REJECTED -> "danger";
        };
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private DashboardViewData buildAgentFallback() {
        DashboardViewData data = new DashboardViewData();
        data.setRoleLabel("AGENT");
        data.setSubtitle("Vue operationnelle simplifiee. Certaines donnees detaillees ne sont pas encore exploitables dans le dashboard.");
        data.setMetricsTitle("Synthese");
        data.setActivitiesTitle("Activite recente");
        data.setActivitiesEmptyMessage("Aucune activite exploitable a afficher pour le moment.");
        data.getMetrics().add(new DashboardMetric("Acces", "Operationnel", "Le dashboard agent reste disponible sans lien casse.", "info"));
        return data;
    }

    private DashboardViewData buildClientFallback() {
        DashboardViewData data = new DashboardViewData();
        data.setRoleLabel("CLIENT");
        data.setSubtitle("Vue simplifiee de votre espace. Les acces essentiels restent disponibles.");
        data.setMetricsTitle("Synthese");
        data.setLinksTitle("Acces rapides");
        data.setActivitiesTitle("Dernieres demandes");
        data.setActivitiesEmptyMessage("Aucune demande exploitable a afficher pour le moment.");
        data.getMetrics().add(new DashboardMetric("Compte client", "Actif", "Votre espace reste accessible.", "success"));
        data.getQuickLinks().add(new DashboardLink("Voir le catalogue", "Consulter les outils disponibles.", "/app/catalog/tools.xhtml"));
        data.getQuickLinks().add(new DashboardLink("Nouvelle demande", "Soumettre une nouvelle reservation.", "/app/client/new-request.xhtml"));
        return data;
    }

    private void rollbackQuietly(Transaction transaction) {
        if (transaction != null && transaction.isActive()) {
            try {
                transaction.rollback();
            } catch (Exception ignored) {
                // rien de plus a faire
            }
        }
    }
}
