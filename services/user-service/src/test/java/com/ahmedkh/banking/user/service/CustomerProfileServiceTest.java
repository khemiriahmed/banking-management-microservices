package com.ahmedkh.banking.user.service;

import com.ahmedkh.banking.user.dto.CustomerProfileResponse;
import com.ahmedkh.banking.user.dto.PageResponse;
import com.ahmedkh.banking.user.dto.UpdateProfileRequest;
import com.ahmedkh.banking.user.entity.CustomerProfile;
import com.ahmedkh.banking.user.exception.ProfileNotFoundException;
import com.ahmedkh.banking.user.repository.CustomerProfileRepository;
import com.ahmedkh.banking.user.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceTest {

    @Mock
    private CustomerProfileRepository repository;

    @InjectMocks
    private CustomerProfileService service;

    @Test
    void shouldCreateMissingProfileFromAuthenticatedIdentity() {
        UUID authUserId = UUID.randomUUID();
        AuthenticatedUser user = user(authUserId, "client@example.com");
        when(repository.findByAuthUserId(authUserId)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(CustomerProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerProfileResponse response = service.getMe(user);

        ArgumentCaptor<CustomerProfile> captor = ArgumentCaptor.forClass(CustomerProfile.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getAuthUserId()).isEqualTo(authUserId);
        assertThat(captor.getValue().getFirstName()).isEqualTo("client");
        assertThat(captor.getValue().getLastName()).isEmpty();
        assertThat(response.authUserId()).isEqualTo(authUserId);
    }

    @Test
    void shouldUpdateProfileWithoutChangingAuthUserId() {
        UUID authUserId = UUID.randomUUID();
        CustomerProfile profile = new CustomerProfile(authUserId, "Old", "Name", null);
        when(repository.findByAuthUserId(authUserId)).thenReturn(Optional.of(profile));

        CustomerProfileResponse response = service.updateMe(
                user(authUserId, "client@example.com"),
                new UpdateProfileRequest(" Ahmed ", " Khemiri ", " 0600000000 ")
        );

        assertThat(response.authUserId()).isEqualTo(authUserId);
        assertThat(response.firstName()).isEqualTo("Ahmed");
        assertThat(response.lastName()).isEqualTo("Khemiri");
        assertThat(response.phone()).isEqualTo("0600000000");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void shouldClampPaginationAndMapResponse() {
        CustomerProfile profile = new CustomerProfile(UUID.randomUUID(), "A", "B", null);
        PageImpl<CustomerProfile> page = new PageImpl<>(
                List.of(profile),
                PageRequest.of(0, 200, Sort.by("createdAt").descending()),
                1
        );
        when(repository.findAll(any(PageRequest.class))).thenReturn(page);

        PageResponse<CustomerProfileResponse> response = service.list(-5, 999);

        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(200);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
    }

    @Test
    void shouldFailWhenAdminLooksUpUnknownAuthUserId() {
        UUID authUserId = UUID.randomUUID();
        when(repository.findByAuthUserId(authUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByAuthUserId(authUserId))
                .isInstanceOf(ProfileNotFoundException.class);
    }

    private AuthenticatedUser user(UUID authUserId, String email) {
        return new AuthenticatedUser(authUserId, email, List.of());
    }
}
