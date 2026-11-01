package com.smartbus.repository;

import com.smartbus.entity.Driver;
import com.smartbus.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByUser(User user);
    Optional<Driver> findByDriverCode(String driverCode);
    Optional<Driver> findByDriverCodeIgnoreCase(String driverCode);
    Optional<Driver> findByLicenseNumber(String licenseNumber);
    Optional<Driver> findFirstByFullNameIgnoreCase(String fullName);
    Optional<Driver> findByAadhaarFingerprint(String aadhaarFingerprint);
    boolean existsByPhoneAndIdNot(String phone, Long id);
    boolean existsByLicenseNumberIgnoreCaseAndIdNot(String licenseNumber, Long id);
}
