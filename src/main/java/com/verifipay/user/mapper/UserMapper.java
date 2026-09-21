package com.verifipay.user.mapper;

import com.verifipay.branch.entity.Branch;
import com.verifipay.company.entity.Company;
import com.verifipay.user.dto.CreateUserRequest;
import com.verifipay.user.dto.UserResponse;
import com.verifipay.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(
            CreateUserRequest request,
            Company company,
            Branch branch,
            String encodedPassword
    ) {

        return User.builder()
                .company(company)
                .branch(branch)
                .name(request.name().trim())
                .email(request.email().trim().toLowerCase())
                .password(encodedPassword)
                .role(request.role())
                .active(true)
                .build();
    }

    public UserResponse toResponse(User user) {

        Long branchId = null;
        String branchName = null;

        if (user.getBranch() != null) {
            branchId = user.getBranch().getId();
            branchName = user.getBranch().getName();
        }

        return new UserResponse(
                user.getId(),
                user.getCompany() == null ? null : user.getCompany().getId(),
                user.getCompany() == null ? null : user.getCompany().getName(),
                branchId,
                branchName,
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
