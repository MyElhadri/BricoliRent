package com.bricolirent.service;

import com.bricolirent.domain.entity.Payment;
import com.bricolirent.domain.entity.Reservation;

import java.math.BigDecimal;
import java.util.List;

public interface AdminSupervisionService {

    AdminDashboardSnapshot getDashboardSnapshot();

    List<Reservation> getAllReservations();

    List<Payment> getAllPayments();

    class AdminDashboardSnapshot {
        private long totalReservations;
        private long pendingReservations;
        private long approvedReservations;
        private long rejectedReservations;
        private long checkedOutReservations;
        private long returnedReservations;
        private long totalPayments;
        private BigDecimal totalRentalRevenue = BigDecimal.ZERO;
        private BigDecimal totalDepositsCollected = BigDecimal.ZERO;
        private BigDecimal totalLatePenalties = BigDecimal.ZERO;
        private BigDecimal totalRefunds = BigDecimal.ZERO;
        private BigDecimal outstandingDeposits = BigDecimal.ZERO;
        private long totalUsers;
        private long totalClients;
        private long totalAgents;
        private long totalActiveTools;
        private long totalCategories;

        public long getTotalReservations() {
            return totalReservations;
        }

        public void setTotalReservations(long totalReservations) {
            this.totalReservations = totalReservations;
        }

        public long getPendingReservations() {
            return pendingReservations;
        }

        public void setPendingReservations(long pendingReservations) {
            this.pendingReservations = pendingReservations;
        }

        public long getApprovedReservations() {
            return approvedReservations;
        }

        public void setApprovedReservations(long approvedReservations) {
            this.approvedReservations = approvedReservations;
        }

        public long getRejectedReservations() {
            return rejectedReservations;
        }

        public void setRejectedReservations(long rejectedReservations) {
            this.rejectedReservations = rejectedReservations;
        }

        public long getCheckedOutReservations() {
            return checkedOutReservations;
        }

        public void setCheckedOutReservations(long checkedOutReservations) {
            this.checkedOutReservations = checkedOutReservations;
        }

        public long getReturnedReservations() {
            return returnedReservations;
        }

        public void setReturnedReservations(long returnedReservations) {
            this.returnedReservations = returnedReservations;
        }

        public long getTotalPayments() {
            return totalPayments;
        }

        public void setTotalPayments(long totalPayments) {
            this.totalPayments = totalPayments;
        }

        public BigDecimal getTotalRentalRevenue() {
            return totalRentalRevenue;
        }

        public void setTotalRentalRevenue(BigDecimal totalRentalRevenue) {
            this.totalRentalRevenue = totalRentalRevenue;
        }

        public BigDecimal getTotalDepositsCollected() {
            return totalDepositsCollected;
        }

        public void setTotalDepositsCollected(BigDecimal totalDepositsCollected) {
            this.totalDepositsCollected = totalDepositsCollected;
        }

        public BigDecimal getTotalLatePenalties() {
            return totalLatePenalties;
        }

        public void setTotalLatePenalties(BigDecimal totalLatePenalties) {
            this.totalLatePenalties = totalLatePenalties;
        }

        public BigDecimal getTotalRefunds() {
            return totalRefunds;
        }

        public void setTotalRefunds(BigDecimal totalRefunds) {
            this.totalRefunds = totalRefunds;
        }

        public BigDecimal getOutstandingDeposits() {
            return outstandingDeposits;
        }

        public void setOutstandingDeposits(BigDecimal outstandingDeposits) {
            this.outstandingDeposits = outstandingDeposits;
        }

        public long getTotalUsers() {
            return totalUsers;
        }

        public void setTotalUsers(long totalUsers) {
            this.totalUsers = totalUsers;
        }

        public long getTotalClients() {
            return totalClients;
        }

        public void setTotalClients(long totalClients) {
            this.totalClients = totalClients;
        }

        public long getTotalAgents() {
            return totalAgents;
        }

        public void setTotalAgents(long totalAgents) {
            this.totalAgents = totalAgents;
        }

        public long getTotalActiveTools() {
            return totalActiveTools;
        }

        public void setTotalActiveTools(long totalActiveTools) {
            this.totalActiveTools = totalActiveTools;
        }

        public long getTotalCategories() {
            return totalCategories;
        }

        public void setTotalCategories(long totalCategories) {
            this.totalCategories = totalCategories;
        }
    }
}
