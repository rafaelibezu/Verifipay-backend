package com.verifipay.company.repository;

import com.verifipay.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface CompanyRepository
        extends JpaRepository<Company, Long> {

    boolean existsByRuc(String ruc);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Company c where c.id = :id")
    Optional<Company> findForUpdate(@Param("id") Long id);
}
