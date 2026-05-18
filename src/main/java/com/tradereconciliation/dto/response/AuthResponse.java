package com.tradereconciliation.dto.response;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String token;
    private String username;
    private String role;
    private long expiresIn;
}
