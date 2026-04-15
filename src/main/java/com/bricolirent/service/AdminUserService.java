package com.bricolirent.service;

import com.bricolirent.domain.entity.Agent;

import java.util.List;

public interface AdminUserService {

    void createAgent(String fullName, String email, String rawPassword, String employeeCode);

    void deleteAgent(Long agentId);

    void toggleAgentActive(Long agentId);

    List<Agent> getAllAgents();
}
