package com.ahmedkh.banking.transfer.service;

import com.ahmedkh.banking.transfer.dto.CreateTransferRequest;
import com.ahmedkh.banking.transfer.dto.StatusChangeResponse;
import com.ahmedkh.banking.transfer.dto.TransferPageResponse;
import com.ahmedkh.banking.transfer.dto.TransferResponse;
import com.ahmedkh.banking.transfer.entity.Transfer;
import com.ahmedkh.banking.transfer.entity.TransferStatus;
import com.ahmedkh.banking.transfer.entity.TransferStatusHistory;
import com.ahmedkh.banking.transfer.exception.IdempotencyConflictException;
import com.ahmedkh.banking.transfer.exception.TransferNotFoundException;
import com.ahmedkh.banking.transfer.repository.TransferRepository;
import com.ahmedkh.banking.transfer.repository.TransferStatusHistoryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class TransferService {

    private final TransferRepository repository;
    private final TransferStatusHistoryRepository historyRepository;
    private final TransferOrchestrator orchestrator;
    private final TransferResponseMapper mapper;

    public TransferService(
            TransferRepository repository,
            TransferStatusHistoryRepository historyRepository,
            TransferOrchestrator orchestrator,
            TransferResponseMapper mapper
    ) {
        this.repository = repository;
        this.historyRepository = historyRepository;
        this.orchestrator = orchestrator;
        this.mapper = mapper;
    }

    @Transactional
    public TransferResponse create(
            CreateTransferRequest request,
            String idempotencyKey,
            UUID ownerUserId,
            String authorization
    ) {
        String fingerprint = fingerprint(request, ownerUserId);
        Transfer existing = repository.findByOwnerUserIdAndIdempotencyKey(ownerUserId, idempotencyKey)
                .orElse(null);
        if (existing != null) {
            return replay(existing, fingerprint);
        }

        Instant now = Instant.now();
        Transfer transfer = new Transfer(
                UUID.randomUUID(),
                ownerUserId,
                request.sourceAccountId(),
                request.beneficiaryId(),
                "PENDING",
                request.amount().setScale(2),
                request.currency(),
                request.reason(),
                TransferStatus.REQUESTED,
                idempotencyKey,
                fingerprint,
                now
        );

        Transfer saved;
        try {
            saved = repository.saveAndFlush(transfer);
        } catch (DataIntegrityViolationException exception) {
            Transfer concurrent = repository.findByOwnerUserIdAndIdempotencyKey(ownerUserId, idempotencyKey)
                    .orElseThrow(() -> exception);
            return replay(concurrent, fingerprint);
        }

        historyRepository.save(new TransferStatusHistory(
                UUID.randomUUID(),
                saved.getId(),
                null,
                TransferStatus.REQUESTED,
                "Transfer requested",
                now
        ));

        orchestrator.execute(saved, authorization);
        return mapper.toResponse(repository.findById(saved.getId()).orElseThrow(TransferNotFoundException::new));
    }

    @Transactional(readOnly = true)
    public TransferResponse get(UUID transferId, UUID ownerUserId) {
        return mapper.toResponse(findOwned(transferId, ownerUserId));
    }

    @Transactional(readOnly = true)
    public List<StatusChangeResponse> history(UUID transferId, UUID ownerUserId) {
        findOwned(transferId, ownerUserId);
        return mapper.history(transferId);
    }

    @Transactional(readOnly = true)
    public TransferPageResponse list(UUID ownerUserId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(1, size));
        List<Transfer> transfers = repository.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId);
        int from = Math.min(safePage * safeSize, transfers.size());
        int to = Math.min(from + safeSize, transfers.size());
        List<TransferResponse> content = transfers.subList(from, to).stream()
                .map(mapper::toResponse)
                .toList();
        return new TransferPageResponse(content, safePage, safeSize, transfers.size(), to >= transfers.size());
    }

    private Transfer findOwned(UUID transferId, UUID ownerUserId) {
        return repository.findByIdAndOwnerUserId(transferId, ownerUserId)
                .orElseThrow(TransferNotFoundException::new);
    }

    private TransferResponse replay(Transfer transfer, String fingerprint) {
        if (!fingerprint.equals(transfer.getRequestFingerprint())) {
            throw new IdempotencyConflictException();
        }
        return mapper.toResponse(transfer);
    }

    private String fingerprint(CreateTransferRequest request, UUID ownerUserId) {
        String canonical = String.join("|",
                ownerUserId.toString(),
                request.sourceAccountId().toString(),
                request.beneficiaryId().toString(),
                request.amount().setScale(2).toPlainString(),
                request.currency(),
                request.reason() == null ? "" : request.reason()
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
