package com.ahmedkh.banking.transfer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountView(UUID id, String currency, String status) {
}
