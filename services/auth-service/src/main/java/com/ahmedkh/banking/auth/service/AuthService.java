package com.ahmedkh.banking.auth.service;

import com.ahmedkh.banking.auth.dto.AuthResponse;
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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class AuthService implements UserDetailsService {

    private static final String DEFAULT_ROLE = "CLIENT";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public UserResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException(email);
        }

        Role clientRole = roleRepository.findById(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException("Default role CLIENT is missing"));
        UserCredential user = new UserCredential(
                email,
                passwordEncoder.encode(request.password()),
                Set.of(clientRole)
        );

        return toResponse(userRepository.saveAndFlush(user));
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalize(request.email());
        UserCredential user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        List<String> roles = roleNames(user);
        String token = tokenProvider.generateToken(user.getId(), user.getEmail(), roles);
        return new AuthResponse(token, "Bearer", tokenProvider.getExpirationSeconds(), roles);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String email) {
        UserCredential user = userRepository.findByEmail(normalize(email))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email = normalize(username);
        UserCredential user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .disabled(!user.isEnabled())
                .authorities(roleNames(user).toArray(String[]::new))
                .build();
    }

    private UserResponse toResponse(UserCredential user) {
        return new UserResponse(user.getId(), user.getEmail(), roleNames(user));
    }

    private List<String> roleNames(UserCredential user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .sorted()
                .toList();
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
