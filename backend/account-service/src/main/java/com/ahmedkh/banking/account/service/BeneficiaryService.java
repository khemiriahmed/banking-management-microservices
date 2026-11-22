package com.ahmedkh.banking.account.service;

import com.ahmedkh.banking.account.dto.BeneficiaryResponse;
import com.ahmedkh.banking.account.dto.CreateBeneficiaryRequest;
import com.ahmedkh.banking.account.dto.UpdateBeneficiaryRequest;
import com.ahmedkh.banking.account.entity.Beneficiary;
import com.ahmedkh.banking.account.entity.BeneficiaryStatus;
import com.ahmedkh.banking.account.exception.BeneficiaryAlreadyExistsException;
import com.ahmedkh.banking.account.exception.BeneficiaryNotFoundException;
import com.ahmedkh.banking.account.repository.BeneficiaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class BeneficiaryService {

    private static final Pattern DESTINATION_REFERENCE =
            Pattern.compile("^FR76 \\*{4} \\*{4} \\*{4} \\d{4}$");

    private final BeneficiaryRepository repository;

    public BeneficiaryService(BeneficiaryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> listForOwner(UUID ownerUserId) {
        return repository.findByOwnerUserIdOrderByCreatedAtDesc(ownerUserId).stream()
                .map(this::toBeneficiaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BeneficiaryResponse getForOwner(UUID beneficiaryId, UUID ownerUserId) {
        return toBeneficiaryResponse(findForOwner(beneficiaryId, ownerUserId));
    }

    @Transactional
    public BeneficiaryResponse create(CreateBeneficiaryRequest request, UUID ownerUserId) {
        String displayName = validateDisplayName(request.displayName());
        String reference = validateDestinationReference(request.destinationAccountReference());
        rejectDuplicate(ownerUserId, reference, null);

        Beneficiary beneficiary = new Beneficiary(
                UUID.randomUUID(),
                ownerUserId,
                displayName,
                reference,
                BeneficiaryStatus.ACTIVE,
                Instant.now()
        );
        return toBeneficiaryResponse(repository.save(beneficiary));
    }

    @Transactional
    public BeneficiaryResponse update(
            UUID beneficiaryId,
            UpdateBeneficiaryRequest request,
            UUID ownerUserId
    ) {
        Beneficiary beneficiary = findForOwner(beneficiaryId, ownerUserId);

        if (request.displayName() != null) {
            beneficiary.setDisplayName(validateDisplayName(request.displayName()));
        }
        if (request.destinationAccountReference() != null) {
            String reference = validateDestinationReference(request.destinationAccountReference());
            rejectDuplicate(ownerUserId, reference, beneficiaryId);
            beneficiary.setDestinationAccountReference(reference);
        }
        if (request.status() != null) {
            beneficiary.setStatus(request.status());
        }
        return toBeneficiaryResponse(repository.save(beneficiary));
    }

    @Transactional
    public void delete(UUID beneficiaryId, UUID ownerUserId) {
        repository.delete(findForOwner(beneficiaryId, ownerUserId));
    }

    private Beneficiary findForOwner(UUID beneficiaryId, UUID ownerUserId) {
        return repository.findByIdAndOwnerUserId(beneficiaryId, ownerUserId)
                .orElseThrow(BeneficiaryNotFoundException::new);
    }

    private void rejectDuplicate(UUID ownerUserId, String reference, UUID excludedId) {
        boolean exists = excludedId == null
                ? repository.existsByOwnerUserIdAndDestinationAccountReference(ownerUserId, reference)
                : repository.existsByOwnerUserIdAndDestinationAccountReferenceAndIdNot(
                ownerUserId,
                reference,
                excludedId
        );
        if (exists) {
            throw new BeneficiaryAlreadyExistsException();
        }
    }

    private String validateDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank() || displayName.length() > 100) {
            throw new IllegalArgumentException("Display name is required");
        }
        return displayName.trim();
    }

    private String validateDestinationReference(String reference) {
        if (reference == null) {
            throw new IllegalArgumentException("Destination reference must be masked");
        }
        String normalized = reference.trim();
        if (!DESTINATION_REFERENCE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Destination reference must use FR76 **** **** **** 1234 format");
        }
        return normalized;
    }

    private BeneficiaryResponse toBeneficiaryResponse(Beneficiary beneficiary) {
        return new BeneficiaryResponse(
                beneficiary.getId(),
                beneficiary.getOwnerUserId(),
                beneficiary.getDisplayName(),
                beneficiary.getDestinationAccountReference(),
                beneficiary.getStatus(),
                beneficiary.getCreatedAt()
        );
    }
}
