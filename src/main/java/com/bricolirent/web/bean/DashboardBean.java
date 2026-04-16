package com.bricolirent.web.bean;

import com.bricolirent.service.DashboardService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Named("dashboardBean")
@ViewScoped
public class DashboardBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private LoginBean loginBean;

    @Inject
    private DashboardService dashboardService;

    private DashboardService.DashboardViewData data;
    private boolean showMoreStats;

    @PostConstruct
    public void init() {
        if (!loginBean.isLoggedIn() || loginBean.getCurrentRole() == null || loginBean.getCurrentUser() == null) {
            data = new DashboardService.DashboardViewData();
            return;
        }

        String role = loginBean.getCurrentRole();
        Long userId = loginBean.getCurrentUser().getId();

        data = switch (role) {
            case "ADMIN" -> dashboardService.buildAdminDashboard();
            case "AGENT" -> dashboardService.buildAgentDashboard(userId);
            case "CLIENT" -> dashboardService.buildClientDashboard(userId);
            default -> new DashboardService.DashboardViewData();
        };
    }

    public DashboardService.DashboardViewData getData() {
        return data;
    }

    public List<DashboardService.DashboardMetric> getPrimaryMetrics() {
        if (data == null || data.getMetrics().isEmpty()) {
            return Collections.emptyList();
        }
        int endIndex = Math.min(5, data.getMetrics().size());
        return data.getMetrics().subList(0, endIndex);
    }

    public List<DashboardService.DashboardMetric> getSecondaryMetrics() {
        if (data == null || data.getMetrics().size() <= 5) {
            return Collections.emptyList();
        }
        return data.getMetrics().subList(5, data.getMetrics().size());
    }

    public boolean isShowMoreStats() {
        return showMoreStats;
    }

    public void setShowMoreStats(boolean showMoreStats) {
        this.showMoreStats = showMoreStats;
    }

    public void toggleMoreStats() {
        showMoreStats = !showMoreStats;
    }

    public String getMoreStatsLabel() {
        return showMoreStats ? "Voir moins" : "Voir plus";
    }

    public boolean hasSecondaryMetrics() {
        return !getSecondaryMetrics().isEmpty();
    }

    public boolean isAdmin() {
        return "ADMIN".equals(loginBean.getCurrentRole());
    }

    public boolean isAgent() {
        return "AGENT".equals(loginBean.getCurrentRole());
    }

    public boolean isClient() {
        return "CLIENT".equals(loginBean.getCurrentRole());
    }
}
