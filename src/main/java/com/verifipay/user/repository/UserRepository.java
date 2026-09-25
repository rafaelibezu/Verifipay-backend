package com.verifipay.user.repository;

import com.verifipay.common.enums.UserRole;
import com.verifipay.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRole(UserRole role);

    List<User> findAllByCompanyId(Long companyId);

    List<User> findAllByBranchId(Long branchId);

    long countByCompanyIdAndRoleAndActiveTrue(
            Long companyId,
            UserRole role
    );

    boolean existsByEmailIgnoreCaseAndIdNot(
            String email,
            Long id
    );

    @EntityGraph(attributePaths = {"company", "branch"})
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdWithTenantContext(@Param("id") Long id);
}
