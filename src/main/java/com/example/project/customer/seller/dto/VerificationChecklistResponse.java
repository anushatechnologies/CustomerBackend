package com.example.project.customer.seller.dto;

public class VerificationChecklistResponse {

    private boolean companyProfile;
    private boolean businessAddress;
    private boolean gstin;
    private boolean pan;
    private boolean cin;
    private boolean msme;
    private boolean serviceAreas;
    private boolean warehouseLocations;
    private boolean gstCertificate;
    private boolean panDocument;
    private boolean msmeDocument;
    private boolean cancelledCheque;

    public boolean isCompanyProfile() {
        return companyProfile;
    }

    public void setCompanyProfile(boolean companyProfile) {
        this.companyProfile = companyProfile;
    }

    public boolean isBusinessAddress() {
        return businessAddress;
    }

    public void setBusinessAddress(boolean businessAddress) {
        this.businessAddress = businessAddress;
    }

    public boolean isGstin() {
        return gstin;
    }

    public void setGstin(boolean gstin) {
        this.gstin = gstin;
    }

    public boolean isPan() {
        return pan;
    }

    public void setPan(boolean pan) {
        this.pan = pan;
    }

    public boolean isCin() {
        return cin;
    }

    public void setCin(boolean cin) {
        this.cin = cin;
    }

    public boolean isMsme() {
        return msme;
    }

    public void setMsme(boolean msme) {
        this.msme = msme;
    }

    public boolean isServiceAreas() {
        return serviceAreas;
    }

    public void setServiceAreas(boolean serviceAreas) {
        this.serviceAreas = serviceAreas;
    }

    public boolean isWarehouseLocations() {
        return warehouseLocations;
    }

    public void setWarehouseLocations(boolean warehouseLocations) {
        this.warehouseLocations = warehouseLocations;
    }

    public boolean isGstCertificate() {
        return gstCertificate;
    }

    public void setGstCertificate(boolean gstCertificate) {
        this.gstCertificate = gstCertificate;
    }

    public boolean isPanDocument() {
        return panDocument;
    }

    public void setPanDocument(boolean panDocument) {
        this.panDocument = panDocument;
    }

    public boolean isMsmeDocument() {
        return msmeDocument;
    }

    public void setMsmeDocument(boolean msmeDocument) {
        this.msmeDocument = msmeDocument;
    }

    public boolean isCancelledCheque() {
        return cancelledCheque;
    }

    public void setCancelledCheque(boolean cancelledCheque) {
        this.cancelledCheque = cancelledCheque;
    }
}
