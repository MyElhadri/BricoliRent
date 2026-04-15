package com.bricolirent.service;

import com.bricolirent.domain.entity.Agent;
import com.bricolirent.domain.entity.User;

import java.util.List;

public interface AdminUserService {

    class UserSummary {
        private final User user;
        private final String accountType;

        public UserSummary(User user, String accountType) {
            this.user = user;
            this.accountType = accountType;
        }

        public User getUser() {
            return user;
        }

        public String getAccountType() {
            return accountType;
        }
    }

    void createAgent(String fullName, String email, String rawPassword, String employeeCode);

    void deleteAgent(Long agentId);

    void toggleAgentActive(Long agentId);

    List<Agent> getAllAgents();

    List<UserSummary> getAllUserSummaries();

    void toggleUserActive(Long userId);
}
