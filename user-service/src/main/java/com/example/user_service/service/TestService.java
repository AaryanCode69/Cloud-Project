package com.example.user_service.service;

import com.example.user_service.dto.TestResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TestService {
    private final String serviceName;

    public TestService(@Value("${spring.application.name}") String serviceName) {
        this.serviceName = serviceName;
    }

    public TestResponse getStatus() {
        return new TestResponse(serviceName, "running");
    }
}

