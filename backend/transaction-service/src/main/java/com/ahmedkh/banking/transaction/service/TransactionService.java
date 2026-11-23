package com.ahmedkh.banking.transaction.service;

import com.ahmedkh.banking.transaction.dto.CreateTransactionEntryRequest;
import com.ahmedkh.banking.transaction.dto.CreateTransactionRequest;
import com.ahmedkh.banking.transaction.dto.CreateTransactionResponse;
import com.ahmedkh.banking.transaction.dto.EntrySideDto;
import com.ahmedkh.banking.transaction.dto.TransactionEntryResponse;
import com.ahmedkh.banking.transaction.dto.TransactionPageResponse;
import com.ahmedkh.banking.transaction.dto.TransactionResponse;
import com.ahmedkh.banking.transaction.entity.EntrySide;
import com.ahmedkh.banking.transaction.entity.TransactionEntry;
import com.ahmedkh.banking.transaction.entity.TransactionRecord;
import com.ahmedkh.banking.transaction.entity.TransactionStatus;
import com.ahmedkh.banking.transaction.entity.TransactionType;
import com.ahmedkh.banking.transaction.exception.TransactionNotFoundException;
import com.ahmedkh.banking.transaction.repository.TransactionRecordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class TransactionService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("occurredAt", "createdAt", "amount");
    private static final int MAX_PAGE_SIZE = 50;

    private final TransactionRecordRepository repository;

    public TransactionService(TransactionRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CreateTransactionResponse record(CreateTransactionRequest request) {
        var existing = repository.findByExternalReference(request.externalReference());
        if (existing.isPresent()) {
            return new CreateTransactionResponse(toResponse(existing.get()), true);
        }

        validateEntries(request);
        Instant now = Instant.now();
        TransactionRecord record = new TransactionRecord(
                UUID.randomUUID(),
                request.externalReference(),
                request.transferId(),
                request.ownerUserId(),
                TransactionType.valueOf(request.type().name()),
                TransactionStatus.CONFIRMED,
                request.amount(),
                request.currency(),
                request.occurredAt(),
                now
        );

        for (CreateTransactionEntryRequest entry : request.entries()) {
            record.addEntry(new TransactionEntry(
                    UUID.randomUUID(),
                    record,
                    entry.accountId(),
                    entry.destinationReference(),
                    EntrySide.valueOf(entry.side().name()),
                    entry.amount(),
                    entry.currency()
            ));
        }

        try {
            return new CreateTransactionResponse(toResponse(repository.saveAndFlush(record)), false);
        } catch (DataIntegrityViolationException exception) {
            return repository.findByExternalReference(request.externalReference())
                    .map(value -> new CreateTransactionResponse(toResponse(value), true))
                    .orElseThrow(() -> exception);
        }
    }

    @Transactional(readOnly = true)
    public TransactionResponse findForOwner(UUID transactionId, UUID ownerUserId) {
        return repository.findByIdAndOwnerUserId(transactionId, ownerUserId)
                .map(this::toResponse)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));
    }

    @Transactional(readOnly = true)
    public TransactionPageResponse listForOwner(
            UUID ownerUserId,
            UUID accountId,
            Instant from,
            Instant to,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be zero or positive");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Unsupported sort field: " + sortBy);
        }

        Instant start = from == null ? Instant.EPOCH : from;
        Instant end = to == null ? Instant.now().plusSeconds(365L * 24 * 60 * 60) : to;
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("from must be before to");
        }

        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toLowerCase(Locale.ROOT));
        PageRequest pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<TransactionRecord> result = accountId == null
                ? repository.findByOwnerUserIdAndOccurredAtBetween(ownerUserId, start, end, pageable)
                : repository.findByOwnerUserIdAndEntries_AccountIdAndOccurredAtBetween(
                        ownerUserId, accountId, start, end, pageable
                );

        return new TransactionPageResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private void validateEntries(CreateTransactionRequest request) {
        for (CreateTransactionEntryRequest entry : request.entries()) {
            boolean hasAccount = entry.accountId() != null;
            boolean hasReference = entry.destinationReference() != null && !entry.destinationReference().isBlank();
            if (hasAccount == hasReference) {
                throw new IllegalArgumentException(
                        "Each entry must contain exactly one accountId or destinationReference"
                );
            }
            if (!request.currency().equals(entry.currency())) {
                throw new IllegalArgumentException("Entry currency must match transaction currency");
            }
            if (entry.amount().compareTo(request.amount()) > 0) {
                throw new IllegalArgumentException("Entry amount cannot exceed transaction amount");
            }
        }
    }

    private TransactionResponse toResponse(TransactionRecord record) {
        List<TransactionEntryResponse> entries = record.getEntries().stream()
                .map(entry -> new TransactionEntryResponse(
                        entry.getId(),
                        entry.getAccountId(),
                        entry.getDestinationReference(),
                        EntrySideDto.valueOf(entry.getSide().name()),
                        entry.getAmount(),
                        entry.getCurrency()
                ))
                .toList();
        return new TransactionResponse(
                record.getId(),
                record.getExternalReference(),
                record.getTransferId(),
                record.getOwnerUserId(),
                com.ahmedkh.banking.transaction.dto.TransactionTypeDto.valueOf(record.getType().name()),
                record.getStatus().name(),
                record.getAmount(),
                record.getCurrency(),
                record.getOccurredAt(),
                record.getCreatedAt(),
                entries
        );
    }
}
