package com.mayureshpatel.pfdataservice.repository.specification;

import com.mayureshpatel.pfdataservice.model.Transaction;
import com.mayureshpatel.pfdataservice.model.TransactionType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransactionSpecification {

    public static Specification<Transaction> withFilter(Long userId, TransactionFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by User ID (Security)
            predicates.add(cb.equal(root.get("account").get("user").get("id"), userId));

            if (filter == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (filter.accountId() != null) {
                predicates.add(cb.equal(root.get("account").get("id"), filter.accountId()));
            }

            if (filter.type() != null) {
                if (filter.type() == TransactionType.TRANSFER) {
                    predicates.add(root.get("type").in(
                            TransactionType.TRANSFER,
                            TransactionType.TRANSFER_IN,
                            TransactionType.TRANSFER_OUT
                    ));
                } else {
                    predicates.add(cb.equal(root.get("type"), filter.type()));
                }
            }

            if (filter.description() != null && !filter.description().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("description")), "%" + filter.description().toLowerCase() + "%"));
            }

            if (filter.categoryName() != null && !filter.categoryName().isBlank()) {
                if ("null".equalsIgnoreCase(filter.categoryName())) {
                    predicates.add(cb.isNull(root.get("category")));
                } else {
                    predicates.add(cb.like(cb.lower(root.get("category").get("name")), "%" + filter.categoryName().toLowerCase() + "%"));
                }
            }

            if (filter.vendorName() != null && !filter.vendorName().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("vendorName")), "%" + filter.vendorName().toLowerCase() + "%"));
            }

            if (filter.minAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), filter.minAmount()));
            }

            if (filter.maxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), filter.maxAmount()));
            }

            if (filter.startDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), filter.startDate()));
            }

            if (filter.endDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), filter.endDate()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public record TransactionFilter(
            Long accountId,
            TransactionType type,
            String description,
            String categoryName,
            String vendorName,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            LocalDate startDate,
            LocalDate endDate
    ) {}
}
