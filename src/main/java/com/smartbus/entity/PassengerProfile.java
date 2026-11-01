package com.smartbus.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "passenger_profiles")
public class PassengerProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "passenger_id", unique = true, nullable = false, length = 30)
    private String passengerId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String address;

    @Column(name = "emergency_contact", length = 50)
    private String emergencyContact;

    public PassengerProfile() {}

    public PassengerProfile(User user, String passengerId, String fullName, String phone, String address, String emergencyContact) {
        this.user = user;
        this.passengerId = passengerId;
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
        this.emergencyContact = emergencyContact;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
}
