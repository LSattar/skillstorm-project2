package com.skillstorm.reserveone.services;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import com.skillstorm.reserveone.dto.PaymentTransactionDto;
import com.skillstorm.reserveone.dto.PaymentTransactionListResponseDto;
import com.skillstorm.reserveone.mappers.PaymentTransactionMapper;
import com.skillstorm.reserveone.models.PaymentTransaction;
import com.skillstorm.reserveone.repositories.PaymentTransactionRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class PaymentTransactionService {

    private final PaymentTransactionRepository repository;
    private final PaymentTransactionMapper mapper;

    public PaymentTransactionService(
            PaymentTransactionRepository repository,
            PaymentTransactionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public PaymentTransactionListResponseDto findAll(
            String query,
            String status,
            LocalDate from,
            LocalDate to,
            int page,
            int size,
            String sort) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));

        OffsetDateTime fromInclusive = (from == null) ? null : from.atStartOfDay().atOffset(ZoneOffset.UTC);

        // exclusive upper bound so the entire "to" date is included
        OffsetDateTime toExclusive = (to == null) ? null : to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        Specification<PaymentTransaction> spec = buildSpec(query, status, fromInclusive, toExclusive);

        Page<PaymentTransaction> result = repository.findAll(spec, pageable);

        List<PaymentTransactionDto> content = result.getContent()
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());

        PaymentTransactionListResponseDto dto = new PaymentTransactionListResponseDto();
        dto.setContent(content);
        dto.setTotalElements(result.getTotalElements());
        dto.setTotalPages(result.getTotalPages());
        dto.setPage(result.getNumber());
        dto.setSize(result.getSize());
        return dto;
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String[] parts = sort.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = (parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return Sort.by(direction, property);
    }

    private Specification<PaymentTransaction> buildSpec(
            String query,
            String status,
            OffsetDateTime fromInclusive,
            OffsetDateTime toExclusive) {
        return (root, cq, cb) -> {
            var predicates = cb.conjunction();

            if (status != null && !status.isBlank()) {
                predicates = cb.and(predicates, cb.equal(root.get("status"), status));
            }

            if (fromInclusive != null) {
                predicates = cb.and(
                        predicates,
                        cb.greaterThanOrEqualTo(root.get("createdAt"), fromInclusive));
            }

            if (toExclusive != null) {
                predicates = cb.and(
                        predicates,
                        cb.lessThan(root.get("createdAt"), toExclusive));
            }

            if (query != null && !query.isBlank()) {
                String like = "%" + query.toLowerCase() + "%";
                predicates = cb.and(predicates, cb.or(
                        cb.like(cb.lower(root.get("transactionId")), like),
                        cb.like(cb.lower(root.get("reservationId").as(String.class)), like)));
            }

            return predicates;
        };
    }

    public List<PaymentTransaction> findByDateRange(LocalDate from, LocalDate to) {
        OffsetDateTime fromInclusive = (from == null) ? null : from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toExclusive = (to == null) ? null : to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        Specification<PaymentTransaction> spec = buildSpec(null, null, fromInclusive, toExclusive);
        return repository.findAll(spec);
    }
}
