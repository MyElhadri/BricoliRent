package com.bricolirent.service.impl;

import com.bricolirent.domain.entity.Client;
import com.bricolirent.domain.entity.Payment;
import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.domain.entity.ReturnRecord;
import com.bricolirent.domain.enums.PaymentStatus;
import com.bricolirent.domain.enums.ReservationStatus;
import com.bricolirent.repository.ClientRepository;
import com.bricolirent.repository.PaymentRepository;
import com.bricolirent.repository.ReservationRepository;
import com.bricolirent.repository.ReturnRecordRepository;
import com.bricolirent.service.DashboardService;
import com.bricolirent.util.HibernateUtil;
import jakarta.enterprise.context.ApplicationScoped;
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
    private final ReservationRepository reservationRepository = new ReservationRepository();
    private final PaymentRepository paymentRepository = new PaymentRepository();
    private final ReturnRecordRepository returnRecordRepository = new ReturnRecordRepository();
    private final ClientRepository clientRepository = new ClientRepository();

    @Override
    public DashboardViewData buildAdminDashboard() {
        Transaction transaction = null;
        try {
            Session session = HibernateUtil.getSessionFactory().getCurrentSession();
            transaction = session.beginTransaction();

            DashboardViewData data = new DashboardViewData();
            data.setRoleLabel("ADMIN");
            data.setSubtitle("Vue de synthese sur le catalogue, les comptes et les principaux volumes de la plateforme.");
            data.setMetricsTitle("Indicateurs d'administration");
            data.setLinksTitle("Acces rapides");
            data.setActivitiesTitle("Derniers comptes enregistres");
            data.setActivitiesEmptyMessage("Aucun compte recent a afficher.");

            long categoryCount = count(session, "SELECT count(c) FROM Category c");
            long toolCount = count(session, "SELECT count(t) FROM Tool t");
            long activeToolCount = count(session, "SELECT count(t) FROM Tool t WHERE t.active = true");
            long agentCount = count(session, "SELECT count(a) FROM Agent a");
            long userCount = count(session, "SELECT count(u) FROM User u");
            long clientCount = count(session, "SELECT count(c) FROM Client c");

            data.getMetrics().add(new DashboardMetric("Categories", String.valueOf(categoryCount), "Categories actuellement configurees", "info"));
            data.getMetrics().add(new DashboardMetric("Outils", String.valueOf(toolCount), activeToolCount + " outil(s) actif(s)", "success"));
            data.getMetrics().add(new DashboardMetric("Agents", String.valueOf(agentCount), "Comptes agents disponibles", "warning"));
            data.getMetrics().add(new DashboardMetric("Utilisateurs", String.valueOf(userCount), clientCount + " client(s) inscrits", "neutral"));

            data.getQuickLinks().add(new DashboardLink("Ajouter un outil", "Acceder a la gestion du catalogue interne.", "/app/admin/tools.xhtml"));
            data.getQuickLinks().add(new DashboardLink("Gerer les categories", "Organiser les familles d'outils.", "/app/admin/categories.xhtml"));
            data.getQuickLinks().add(new DashboardLink("Gerer les agents", "Creer, activer ou supprimer un agent.", "/app/admin/agents.xhtml"));
            data.getQuickLinks().add(new DashboardLink("Gerer les utilisateurs", "Consulter les comptes et leur statut.", "/app/admin/users.xhtml"));
            data.getQuickLinks().add(new DashboardLink("Voir le catalogue", "Consulter le catalogue client/public.", "/app/catalog/tools.xhtml"));

            List<Object[]> recentUsers = session.createQuery(
                            "SELECT u.id, u.fullName, u.email FROM User u ORDER BY u.id DESC",
                            Object[].class)
                    .setMaxResults(5)
                    .getResultList();

            Set<Long> adminIds = new HashSet<>(session.createQuery("SELECT a.id FROM Admin a", Long.class).getResultList());
            Set<Long> agentIds = new HashSet<>(session.createQuery("SELECT a.id FROM Agent a", Long.class).getResultList());
            Set<Long> clientIds = new HashSet<>(session.createQuery("SELECT c.id FROM Client c", Long.class).getResultList());

            for (Object[] row : recentUsers) {
                Long userId = (Long) row[0];
                String fullName = (String) row[1];
                String email = (String) row[2];
                String accountType = resolveAccountType(userId, adminIds, agentIds, clientIds);
                data.getActivities().add(new DashboardActivity(fullName, email, accountType, accountType.toLowerCase(Locale.ROOT)));
            }

            transaction.commit();
            return data;
        } catch (Exception e) {
            rollbackQuietly(transaction);
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
