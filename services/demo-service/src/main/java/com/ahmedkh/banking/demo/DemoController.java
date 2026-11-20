package com.ahmedkh.banking.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DemoController {

    private final String message;

    public DemoController(@Value("${app.message:pong}") String message) {
        this.message = message;
    }

    @GetMapping("/demo/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "message", message);
    }
}
