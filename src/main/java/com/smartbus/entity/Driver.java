package com.smartbus.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "drivers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_driver_phone", columnNames = "phone"),
        @UniqueConstraint(name = "uk_driver_aadhaar_fingerprint", columnNames = "aadhaar_fingerprint")
})
public class Driver {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "driver_code", unique = true, nullable = false, length = 30)
    private String driverCode;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(name = "aadhaar_last_four", length = 4)
    @JsonIgnore
    private String aadhaarLastFour;

    @Column(name = "aadhaar_fingerprint", length = 64)
    @JsonIgnore
    private String aadhaarFingerprint;

    @Column(name = "license_number", nullable = false, length = 50)
    private String licenseNumber;

    @Column(length = 20)
    private String status = "ACTIVE"; // ACTIVE, ON_TRIP, OFF_DUTY

    public Driver() {}

    public Driver(User user, String driverCode, String fullName, String phone, String licenseNumber) {
        this.user = user;
        this.driverCode = driverCode;
        this.fullName = fullName;
        this.phone = phone;
        this.licenseNumber = licenseNumber;
        this.status = "ACTIVE";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getDriverCode() { return driverCode; }
    public void setDriverCode(String driverCode) { this.driverCode = driverCode; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAadhaarLastFour() { return aadhaarLastFour; }
    public void setAadhaarLastFour(String aadhaarLastFour) { this.aadhaarLastFour = aadhaarLastFour; }

    public String getAadhaarFingerprint() { return aadhaarFingerprint; }
    public void setAadhaarFingerprint(String aadhaarFingerprint) { this.aadhaarFingerprint = aadhaarFingerprint; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
