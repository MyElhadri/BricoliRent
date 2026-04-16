package com.bricolirent.service.impl;

import com.bricolirent.domain.entity.Payment;
import com.bricolirent.domain.entity.Reservation;
import com.bricolirent.domain.enums.PaymentType;
import com.bricolirent.domain.enums.ReservationStatus;
import com.bricolirent.repository.CategoryRepository;
import com.bricolirent.repository.ClientRepository;
import com.bricolirent.repository.PaymentRepository;
import com.bricolirent.repository.ReservationRepository;
import com.bricolirent.repository.ToolRepository;
import com.bricolirent.repository.UserRepository;
import com.bricolirent.service.AdminSupervisionService;
import com.bricolirent.util.HibernateUtil;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class AdminSupervisionServiceImpl implements AdminSupervisionService {

    private ReservationRepository reservationRepository;
    private PaymentRepository paymentRepository;
    private UserRepository userRepository;
    private ClientRepository clientRepository;
    private ToolRepository toolRepository;
    private CategoryRepository categoryRepository;

    @PostConstruct
    public void init() {
        this.reservationRepository = new ReservationRepository();
        this.paymentRepository = new PaymentRepository();
        this.userRepository = new UserRepository();
        this.clientRepository = new ClientRepository();
        this.toolRepository = new ToolRepository();
        this.categoryRepository = new CategoryRepository();
    }

    @Override
    public AdminDashboardSnapshot getDashboardSnapshot() {
        AdminDashboardSnapshot snapshot = new AdminDashboardSnapshot();
        snapshot.setTotalReservations(reservationRepository.count());
        snapshot.setPendingReservations(reservationRepository.countByStatus(ReservationStatus.PENDING));
        snapshot.setApprovedReservations(reservationRepository.countByStatus(ReservationStatus.APPROVED));
        snapshot.setRejectedReservations(reservationRepository.countByStatus(ReservationStatus.REJECTED));
        snapshot.setCheckedOutReservations(reservationRepository.countByStatus(ReservationStatus.CHECKED_OUT));
        snapshot.setReturnedReservations(reservationRepository.countByStatus(ReservationStatus.RETURNED));

        snapshot.setTotalPayments(paymentRepository.count());
        snapshot.setTotalRentalRevenue(paymentRepository.sumPaidAmountByType(PaymentType.RENTAL));
        snapshot.setTotalDepositsCollected(paymentRepository.sumPaidAmountByType(PaymentType.DEPOSIT));
        snapshot.setTotalLatePenalties(paymentRepository.sumPaidAmountByType(PaymentType.LATE_PENALTY));
        snapshot.setTotalRefunds(paymentRepository.sumPaidAmountByType(PaymentType.REFUND));
        snapshot.setOutstandingDeposits(
                snapshot.getTotalDepositsCollected().subtract(snapshot.getTotalRefunds())
        );

        snapshot.setTotalUsers(userRepository.count());
        snapshot.setTotalClients(clientRepository.count());
        snapshot.setTotalCategories(categoryRepository.count());
        snapshot.setTotalAgents(countAgents());
        snapshot.setTotalActiveTools(countActiveTools());
        return snapshot;
    }

    @Override
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAllDetailedForAdmin();
    }

    @Override
    public List<Payment> getAllPayments() {
        return paymentRepository.findAllDetailedForAdmin();
    }

    private long countAgents() {
        Transaction transaction = null;
        try {
            Session session = HibernateUtil.getSessionFactory().getCurrentSession();
            transaction = session.beginTransaction();
            Long count = session.createQuery("SELECT COUNT(a) FROM Agent a", Long.class).getSingleResult();
            transaction.commit();
            return count == null ? 0L : count;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Impossible de compter les agents.", e);
        }
    }

    private long countActiveTools() {
        Transaction transaction = null;
        try {
            Session session = HibernateUtil.getSessionFactory().getCurrentSession();
            transaction = session.beginTransaction();
            Long count = session.createQuery("SELECT COUNT(t) FROM Tool t WHERE t.active = true", Long.class).getSingleResult();
            transaction.commit();
            return count == null ? 0L : count;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Impossible de compter les outils actifs.", e);
        }
    }
}
