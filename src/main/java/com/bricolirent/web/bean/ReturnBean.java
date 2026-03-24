package com.bricolirent.web.bean;

import com.bricolirent.domain.entity.ReturnRecord;
import com.bricolirent.service.ReturnService;
import com.bricolirent.service.impl.ReturnServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;

/**
 * JSF managed bean for return operations.
 */
@Named("returnBean")
@RequestScoped
public class ReturnBean implements Serializable {

    private List<ReturnRecord> returnRecords;
    private ReturnRecord newReturnRecord = new ReturnRecord();

    private final ReturnService returnService = new ReturnServiceImpl();

    @PostConstruct
    public void init() {
        returnRecords = returnService.getAllReturnRecords();
    }

    // ==================== Getters & Setters ====================

    public List<ReturnRecord> getReturnRecords() {
        return returnRecords;
    }

    public void setReturnRecords(List<ReturnRecord> returnRecords) {
        this.returnRecords = returnRecords;
    }

    public ReturnRecord getNewReturnRecord() {
        return newReturnRecord;
    }

    public void setNewReturnRecord(ReturnRecord newReturnRecord) {
        this.newReturnRecord = newReturnRecord;
    }
}
