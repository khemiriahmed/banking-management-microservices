package com.ahmedkh.banking.user.controller;

import com.ahmedkh.banking.user.dto.CustomerProfileResponse;
import com.ahmedkh.banking.user.dto.PageResponse;
import com.ahmedkh.banking.user.service.CustomerProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final CustomerProfileService service;

    public AdminUserController(CustomerProfileService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<CustomerProfileResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.list(page, size);
    }

    @GetMapping("/{authUserId}")
    public CustomerProfileResponse getByAuthUserId(@PathVariable UUID authUserId) {
        return service.getByAuthUserId(authUserId);
    }
}
