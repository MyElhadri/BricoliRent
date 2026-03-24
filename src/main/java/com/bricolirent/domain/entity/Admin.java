package com.bricolirent.domain.entity;

import jakarta.persistence.*;

/**
 * Admin entity — a user with full system administration privileges.
 */
@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends User {

    @Column(name = "admin_level")
    private String adminLevel;

    // ==================== Constructors ====================

    public Admin() {
    }

    // ==================== Getters & Setters ====================

    public String getAdminLevel() {
        return adminLevel;
    }

    public void setAdminLevel(String adminLevel) {
        this.adminLevel = adminLevel;
    }
}
