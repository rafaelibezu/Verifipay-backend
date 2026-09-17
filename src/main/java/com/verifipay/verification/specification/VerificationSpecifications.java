package com.verifipay.verification.specification;

import com.verifipay.common.enums.PaymentMethod;
import com.verifipay.common.enums.VerificationStatus;
import com.verifipay.verification.entity.Verification;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class VerificationSpecifications {

    private VerificationSpecifications() {
    }

    public static Specification<Verification> belongsToCompany(
            Long companyId
    ) {
        return (root, query, cb) ->
                cb.equal(
                        root.get("company").get("id"),
                        companyId
                );
    }

    public static Specification<Verification> hasBranch(
            Long branchId
    ) {
        return (root, query, cb) -> {

            if (branchId == null) {
                return cb.conjunction();
            }

            return cb.equal(
                    root.get("branch").get("id"),
                    branchId
            );
        };
    }

    public static Specification<Verification> hasCashier(
            Long cashierId
    ) {
        return (root, query, cb) -> {

            if (cashierId == null) {
                return cb.conjunction();
            }

            return cb.equal(
                    root.get("cashier").get("id"),
                    cashierId
            );
        };
    }

    public static Specification<Verification> hasStatus(
            VerificationStatus status
    ) {
        return (root, query, cb) -> {

            if (status == null) {
                return cb.conjunction();
            }

            return cb.equal(
                    root.get("status"),
                    status
            );
        };
    }

    public static Specification<Verification> hasMethod(
            PaymentMethod method
    ) {
        return (root, query, cb) -> {

            if (method == null) {
                return cb.conjunction();
            }

            return cb.equal(
                    root.get("method"),
                    method
            );
        };
    }

    public static Specification<Verification> createdFrom(
            LocalDateTime from
    ) {
        return (root, query, cb) -> {

            if (from == null) {
                return cb.conjunction();
            }

            return cb.greaterThanOrEqualTo(
                    root.get("createdAt"),
                    from
            );
        };
    }

    public static Specification<Verification> createdBefore(
            LocalDateTime to
    ) {
        return (root, query, cb) -> {

            if (to == null) {
                return cb.conjunction();
            }

            return cb.lessThan(
                    root.get("createdAt"),
                    to
            );
        };
    }
}