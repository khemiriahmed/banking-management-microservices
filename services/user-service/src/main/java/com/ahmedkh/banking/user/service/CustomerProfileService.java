package com.ahmedkh.banking.user.service;

import com.ahmedkh.banking.user.dto.CustomerProfileResponse;
import com.ahmedkh.banking.user.dto.PageResponse;
import com.ahmedkh.banking.user.dto.UpdateProfileRequest;
import com.ahmedkh.banking.user.entity.CustomerProfile;
import com.ahmedkh.banking.user.exception.ProfileNotFoundException;
import com.ahmedkh.banking.user.repository.CustomerProfileRepository;
import com.ahmedkh.banking.user.security.AuthenticatedUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class CustomerProfileService {

    private static final int MAX_PAGE_SIZE = 200;

    private final CustomerProfileRepository repository;

    public CustomerProfileService(CustomerProfileRepository repository) {
        this.repository = repository;
    }

    public CustomerProfileResponse getMe(AuthenticatedUser user) {
        return toResponse(findOrCreate(user));
    }

    public CustomerProfileResponse updateMe(AuthenticatedUser user, UpdateProfileRequest request) {
        CustomerProfile profile = findOrCreate(user);
        profile.setFirstName(request.firstName().trim());
        profile.setLastName(request.lastName().trim());
        profile.setPhone(normalizePhone(request.phone()));
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerProfileResponse> list(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<CustomerProfile> result = repository.findAll(
                PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending())
        );
        List<CustomerProfileResponse> content = result.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse getByAuthUserId(UUID authUserId) {
        return repository.findByAuthUserId(authUserId)
                .map(this::toResponse)
                .orElseThrow(ProfileNotFoundException::new);
    }

    private CustomerProfile findOrCreate(AuthenticatedUser user) {
        return repository.findByAuthUserId(user.getAuthUserId())
                .orElseGet(() -> repository.saveAndFlush(new CustomerProfile(
                        user.getAuthUserId(),
                        deriveFirstName(user.getEmail()),
                        "",
                        null
                )));
    }

    private String deriveFirstName(String email) {
        int separator = email.indexOf('@');
        String localPart = (separator > 0 ? email.substring(0, separator) : email).trim();
        if (localPart.isEmpty()) {
            localPart = "Customer";
        }
        return localPart.substring(0, Math.min(localPart.length(), 100));
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String normalized = phone.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private CustomerProfileResponse toResponse(CustomerProfile profile) {
        return new CustomerProfileResponse(
                profile.getId(),
                profile.getAuthUserId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getPhone(),
                profile.getCreatedAt()
        );
    }
}
