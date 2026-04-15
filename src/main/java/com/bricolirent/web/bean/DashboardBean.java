package com.bricolirent.web.bean;

import com.bricolirent.service.DashboardService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

@Named("dashboardBean")
@ViewScoped
public class DashboardBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private LoginBean loginBean;

    @Inject
    private DashboardService dashboardService;

    private DashboardService.DashboardViewData data;

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
