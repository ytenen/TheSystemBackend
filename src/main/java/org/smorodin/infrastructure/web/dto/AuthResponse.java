package org.smorodin.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String username;
    private Long userId;
    private String token;
}
