package com.verifipay.company.repository;

import com.verifipay.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository
        extends JpaRepository<Company, Long> {

    boolean existsByRuc(String ruc);
}