package org.smorodin.infrastructure.auth;

import org.smorodin.core.service.PlayerService;
import org.smorodin.infrastructure.persistence.UserEntity;
import org.smorodin.infrastructure.persistence.UserRepository;
import org.smorodin.infrastructure.web.dto.AuthResponse;
import org.smorodin.infrastructure.web.exception.UserAlreadyExistsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
public class AuthService {

    private static final int MIN_USERNAME_LENGTH = 6; // Matches Player name validation.
    private static final int MAX_USERNAME_LENGTH = 100; // Matches users.user_name.
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final PlayerService playerService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PlayerService playerService,
                       JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.playerService = playerService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(String username, String password) {
        validateRegistration(username, password);

        if (userRepository.existsByUserName(username)) {
            throw new UserAlreadyExistsException(username);
        }

        String passwordHash = passwordEncoder.encode(password);
        UserEntity user = UserEntity.createNewUser(username, passwordHash);

        try {
            // Flush here to turn a concurrent unique-username conflict into 409.
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new UserAlreadyExistsException(username);
        }

        playerService.createPlayer(user.getId(), username);
        return new AuthResponse(username, user.getId(), jwtService.generateToken(user.getId()));
    }

    public AuthResponse login(String username, String password) {
        // Keep login errors indistinguishable for unknown users and wrong passwords.
        if (username == null || username.isBlank() || username.length() > MAX_USERNAME_LENGTH
                || password == null || password.isEmpty()
                || password.length() > 1024) {
            throw new IllegalArgumentException("Wrong password");
        }

        UserEntity user = userRepository.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("Wrong password"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Wrong password");
        }

        return new AuthResponse(username, user.getId(), jwtService.generateToken(user.getId()));
    }

    private static void validateRegistration(String username, String password) {
        if (username == null || username.length() < MIN_USERNAME_LENGTH
                || username.length() > MAX_USERNAME_LENGTH
                || username.isBlank() || !username.equals(username.strip())
                || username.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid registration username");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH
                || password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            throw new IllegalArgumentException("Invalid registration password");
        }
    }
}
