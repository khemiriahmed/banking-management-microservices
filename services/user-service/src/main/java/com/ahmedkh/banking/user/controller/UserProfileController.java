package com.ahmedkh.banking.user.controller;

import com.ahmedkh.banking.user.dto.CustomerProfileResponse;
import com.ahmedkh.banking.user.dto.UpdateProfileRequest;
import com.ahmedkh.banking.user.security.AuthenticatedUser;
import com.ahmedkh.banking.user.service.CustomerProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserProfileController {

    private final CustomerProfileService service;

    public UserProfileController(CustomerProfileService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public CustomerProfileResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.getMe(user);
    }

    @PutMapping("/me")
    public CustomerProfileResponse update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return service.updateMe(user, request);
    }
}
