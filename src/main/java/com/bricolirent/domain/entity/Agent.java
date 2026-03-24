package com.bricolirent.domain.entity;

import jakarta.persistence.*;

/**
 * Agent entity — a user who manages check-outs and returns.
 * Tracks which agent processed each operation.
 */
@Entity
@DiscriminatorValue("AGENT")
public class Agent extends User {

    @Column(name = "employee_id", unique = true)
    private String employeeId;

    @Column(name = "department")
    private String department;

    // ==================== Constructors ====================

    public Agent() {
    }

    // ==================== Getters & Setters ====================

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
