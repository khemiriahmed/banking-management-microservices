package com.ahmedkh.banking.account.service;

import com.ahmedkh.banking.account.dto.BeneficiaryResponse;
import com.ahmedkh.banking.account.dto.CreateBeneficiaryRequest;
import com.ahmedkh.banking.account.dto.UpdateBeneficiaryRequest;
import com.ahmedkh.banking.account.entity.Beneficiary;
import com.ahmedkh.banking.account.entity.BeneficiaryStatus;
import com.ahmedkh.banking.account.exception.BeneficiaryAlreadyExistsException;
import com.ahmedkh.banking.account.exception.BeneficiaryNotFoundException;
import com.ahmedkh.banking.account.repository.BeneficiaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeneficiaryServiceTest {

    @Mock
    private BeneficiaryRepository repository;

    @InjectMocks
    private BeneficiaryService service;

    @Test
    void createTrimsNameAndStartsActive() {
        UUID owner = UUID.randomUUID();
        when(repository.save(any(Beneficiary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BeneficiaryResponse response = service.create(new CreateBeneficiaryRequest(
                "  Alice  ",
                " FR76 **** **** **** 1234 "
        ), owner);

        ArgumentCaptor<Beneficiary> captor = ArgumentCaptor.forClass(Beneficiary.class);
        verify(repository).save(captor.capture());
        Beneficiary saved = captor.getValue();
        assertThat(saved.getOwnerUserId()).isEqualTo(owner);
        assertThat(saved.getDisplayName()).isEqualTo("Alice");
        assertThat(saved.getDestinationAccountReference()).isEqualTo("FR76 **** **** **** 1234");
        assertThat(saved.getStatus()).isEqualTo(BeneficiaryStatus.ACTIVE);
        assertThat(response.id()).isEqualTo(saved.getId());
    }

    @Test
    void createRejectsUnmaskedReference() {
        UUID owner = UUID.randomUUID();

        assertThatThrownBy(() -> service.create(new CreateBeneficiaryRequest(
                "Alice",
                "FR7630006000011234567890189"
        ), owner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FR76 **** **** **** 1234");
        verify(repository, never()).save(any(Beneficiary.class));
    }

    @Test
    void createRejectsDuplicateReferenceForSameOwner() {
        UUID owner = UUID.randomUUID();
        when(repository.existsByOwnerUserIdAndDestinationAccountReference(
                owner,
                "FR76 **** **** **** 1234"
        )).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateBeneficiaryRequest(
                "Alice",
                "FR76 **** **** **** 1234"
        ), owner))
                .isInstanceOf(BeneficiaryAlreadyExistsException.class);
        verify(repository, never()).save(any(Beneficiary.class));
    }

    @Test
    void getRequiresOwnership() {
        UUID beneficiaryId = UUID.randomUUID();
        UUID otherOwner = UUID.randomUUID();
        when(repository.findByIdAndOwnerUserId(beneficiaryId, otherOwner)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getForOwner(beneficiaryId, otherOwner))
                .isInstanceOf(BeneficiaryNotFoundException.class);
    }

    @Test
    void updateChangesNameReferenceAndStatus() {
        UUID owner = UUID.randomUUID();
        UUID beneficiaryId = UUID.randomUUID();
        Beneficiary beneficiary = beneficiary(beneficiaryId, owner, "Alice", "FR76 **** **** **** 1234");
        when(repository.findByIdAndOwnerUserId(beneficiaryId, owner)).thenReturn(Optional.of(beneficiary));
        when(repository.existsByOwnerUserIdAndDestinationAccountReferenceAndIdNot(
                owner,
                "FR76 **** **** **** 9876",
                beneficiaryId
        )).thenReturn(false);
        when(repository.save(beneficiary)).thenReturn(beneficiary);

        BeneficiaryResponse response = service.update(
                beneficiaryId,
                new UpdateBeneficiaryRequest(
                        "Alice Dupont",
                        "FR76 **** **** **** 9876",
                        BeneficiaryStatus.INACTIVE
                ),
                owner
        );

        assertThat(response.displayName()).isEqualTo("Alice Dupont");
        assertThat(response.destinationAccountReference()).isEqualTo("FR76 **** **** **** 9876");
        assertThat(response.status()).isEqualTo(BeneficiaryStatus.INACTIVE);
    }

    @Test
    void deleteUsesOwnershipScopedLookup() {
        UUID owner = UUID.randomUUID();
        UUID beneficiaryId = UUID.randomUUID();
        Beneficiary beneficiary = beneficiary(beneficiaryId, owner, "Alice", "FR76 **** **** **** 1234");
        when(repository.findByIdAndOwnerUserId(beneficiaryId, owner)).thenReturn(Optional.of(beneficiary));

        service.delete(beneficiaryId, owner);

        verify(repository).delete(beneficiary);
    }

    private Beneficiary beneficiary(
            UUID id,
            UUID owner,
            String displayName,
            String destinationAccountReference
    ) {
        return new Beneficiary(
                id,
                owner,
                displayName,
                destinationAccountReference,
                BeneficiaryStatus.ACTIVE,
                Instant.now()
        );
    }
}
