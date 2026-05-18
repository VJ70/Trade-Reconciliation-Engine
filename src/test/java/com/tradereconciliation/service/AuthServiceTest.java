package com.tradereconciliation.service;

import com.tradereconciliation.dto.request.RegisterRequest;
import com.tradereconciliation.dto.response.AuthResponse;
import com.tradereconciliation.exception.GlobalExceptionHandler.DuplicateResourceException;
import com.tradereconciliation.model.User;
import com.tradereconciliation.repository.UserRepository;
import com.tradereconciliation.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @InjectMocks AuthService authService;

    @Test
    @DisplayName("Register with new username: returns token")
    void register_newUser_returnsToken() {
        ReflectionTestUtils.setField(authService, "jwtExpiration", 86400000L);
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("jwt.token.here");

        RegisterRequest req = new RegisterRequest("alice", "password123", User.Role.TRADER);
        AuthResponse response = authService.register(req);

        assertThat(response.getToken()).isEqualTo("jwt.token.here");
        assertThat(response.getUsername()).isEqualTo("alice");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Register with duplicate username: throws DuplicateResourceException")
    void register_duplicate_throwsException() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);
        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("alice", "pass", User.Role.TRADER)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("alice");
    }
}
