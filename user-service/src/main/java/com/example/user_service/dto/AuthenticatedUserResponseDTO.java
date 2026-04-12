package com.example.user_service.dto;

import java.util.List;

public record AuthenticatedUserResponseDTO(
        String subject,
        String username,
        String email,
        String displayName,
        String tenantId,
        List<String> authorities
) {
}
