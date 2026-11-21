package com.ahmedkh.banking.auth;

import com.ahmedkh.banking.auth.dto.LoginRequest;
import com.ahmedkh.banking.auth.dto.RegisterRequest;
import com.ahmedkh.banking.auth.dto.UserResponse;
import com.ahmedkh.banking.auth.entity.Role;
import com.ahmedkh.banking.auth.entity.UserCredential;
import com.ahmedkh.banking.auth.exception.EmailAlreadyUsedException;
import com.ahmedkh.banking.auth.exception.InvalidCredentialsException;
import com.ahmedkh.banking.auth.repository.RoleRepository;
import com.ahmedkh.banking.auth.repository.UserRepository;
import com.ahmedkh.banking.auth.security.JwtTokenProvider;
import com.ahmedkh.banking.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerNormalizesEmailHashesPasswordAndAssignsClientRole() {
        when(userRepository.existsByEmail("client@example.com")).thenReturn(false);
        when(roleRepository.findById("CLIENT")).thenReturn(Optional.of(new Role("CLIENT")));
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(userRepository.saveAndFlush(any(UserCredential.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(
                new RegisterRequest("  Client@Example.COM ", "password123")
        );

        assertEquals("client@example.com", response.email());
        assertEquals(List.of("CLIENT"), response.roles());
        ArgumentCaptor<UserCredential> captor = ArgumentCaptor.forClass(UserCredential.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertEquals("hashed-password", captor.getValue().getPasswordHash());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("client@example.com")).thenReturn(true);

        assertThrows(
                EmailAlreadyUsedException.class,
                () -> authService.register(new RegisterRequest("client@example.com", "password123"))
        );
        verifyNoInteractions(roleRepository, passwordEncoder);
    }

    @Test
    void loginRejectsInvalidPassword() {
        UserCredential user = new UserCredential(
                "client@example.com",
                "hashed-password",
                Set.of(new Role("CLIENT"))
        );
        when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("client@example.com", "wrong-password"))
        );
    }

    @Test
    void loginReturnsJwtWithRoles() {
        UserCredential user = new UserCredential(
                "client@example.com",
                "hashed-password",
                Set.of(new Role("CLIENT"))
        );
        when(userRepository.findByEmail("client@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(tokenProvider.generateToken(any(), eq("client@example.com"), anyCollection())).thenReturn("jwt-token");
        when(tokenProvider.getExpirationSeconds()).thenReturn(3600L);

        var response = authService.login(new LoginRequest("client@example.com", "password123"));

        assertEquals("jwt-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(3600L, response.expiresIn());
        assertEquals(List.of("CLIENT"), response.roles());
    }
}
