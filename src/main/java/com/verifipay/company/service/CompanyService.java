package com.verifipay.company.service;

import com.verifipay.common.exception.BusinessException;
import com.verifipay.common.exception.ResourceNotFoundException;
import com.verifipay.company.dto.CompanyResponse;
import com.verifipay.company.dto.CreateCompanyRequest;
import com.verifipay.company.entity.Company;
import com.verifipay.company.mapper.CompanyMapper;
import com.verifipay.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;

    @Transactional
    public CompanyResponse create(CreateCompanyRequest request) {

        if (request.ruc() != null &&
                companyRepository.existsByRuc(request.ruc())) {

            throw new BusinessException(
                    "Ya existe una empresa registrada con este RUC"
            );
        }

        Company company =
                companyMapper.toEntity(request);

        Company savedCompany =
                companyRepository.save(company);

        return companyMapper.toResponse(savedCompany);
    }

    @Transactional(readOnly = true)
    public CompanyResponse findById(Long id) {

        Company company = companyRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Empresa no encontrada"
                        )
                );

        return companyMapper.toResponse(company);
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> findAll() {

        return companyRepository
                .findAll()
                .stream()
                .map(companyMapper::toResponse)
                .toList();
    }
}