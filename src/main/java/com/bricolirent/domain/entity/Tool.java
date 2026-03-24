package com.bricolirent.domain.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Tool entity — represents a rentable tool/equipment.
 */
@Entity
@Table(name = "tools")
public class Tool implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String brand;

    @Column(name = "daily_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyPrice;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "image_url")
    private String imageUrl;

    private boolean available = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    // ==================== Constructors ====================

    public Tool() {
    }

    // ==================== Getters & Setters ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public BigDecimal getDailyPrice() {
        return dailyPrice;
    }

    public void setDailyPrice(BigDecimal dailyPrice) {
        this.dailyPrice = dailyPrice;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
}
