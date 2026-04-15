package com.bricolirent.service;

import java.util.ArrayList;
import java.util.List;

public interface DashboardService {

    DashboardViewData buildAdminDashboard();

    DashboardViewData buildAgentDashboard(Long agentUserId);

    DashboardViewData buildClientDashboard(Long clientUserId);

    class DashboardMetric {
        private final String label;
        private final String value;
        private final String detail;
        private final String tone;

        public DashboardMetric(String label, String value, String detail, String tone) {
            this.label = label;
            this.value = value;
            this.detail = detail;
            this.tone = tone;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }

        public String getDetail() {
            return detail;
        }

        public String getTone() {
            return tone;
        }
    }

    class DashboardLink {
        private final String label;
        private final String description;
        private final String outcome;

        public DashboardLink(String label, String description, String outcome) {
            this.label = label;
            this.description = description;
            this.outcome = outcome;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }

        public String getOutcome() {
            return outcome;
        }
    }

    class DashboardActivity {
        private final String title;
        private final String subtitle;
        private final String badgeLabel;
        private final String badgeTone;

        public DashboardActivity(String title, String subtitle, String badgeLabel, String badgeTone) {
            this.title = title;
            this.subtitle = subtitle;
            this.badgeLabel = badgeLabel;
            this.badgeTone = badgeTone;
        }

        public String getTitle() {
            return title;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public String getBadgeLabel() {
            return badgeLabel;
        }

        public String getBadgeTone() {
            return badgeTone;
        }
    }

    class DashboardViewData {
        private String roleLabel;
        private String subtitle;
        private String metricsTitle;
        private String linksTitle;
        private String activitiesTitle;
        private String activitiesEmptyMessage;
        private final List<DashboardMetric> metrics = new ArrayList<>();
        private final List<DashboardLink> quickLinks = new ArrayList<>();
        private final List<DashboardActivity> activities = new ArrayList<>();

        public String getRoleLabel() {
            return roleLabel;
        }

        public void setRoleLabel(String roleLabel) {
            this.roleLabel = roleLabel;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public void setSubtitle(String subtitle) {
            this.subtitle = subtitle;
        }

        public String getMetricsTitle() {
            return metricsTitle;
        }

        public void setMetricsTitle(String metricsTitle) {
            this.metricsTitle = metricsTitle;
        }

        public String getLinksTitle() {
            return linksTitle;
        }

        public void setLinksTitle(String linksTitle) {
            this.linksTitle = linksTitle;
        }

        public String getActivitiesTitle() {
            return activitiesTitle;
        }

        public void setActivitiesTitle(String activitiesTitle) {
            this.activitiesTitle = activitiesTitle;
        }

        public String getActivitiesEmptyMessage() {
            return activitiesEmptyMessage;
        }

        public void setActivitiesEmptyMessage(String activitiesEmptyMessage) {
            this.activitiesEmptyMessage = activitiesEmptyMessage;
        }

        public List<DashboardMetric> getMetrics() {
            return metrics;
        }

        public List<DashboardLink> getQuickLinks() {
            return quickLinks;
        }

        public List<DashboardActivity> getActivities() {
            return activities;
        }
    }
}
