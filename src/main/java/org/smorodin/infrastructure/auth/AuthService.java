package org.smorodin.infrastructure.auth;

import org.smorodin.core.model.Player;
import org.smorodin.core.model.User;
import org.smorodin.core.service.PlayerService;
import org.smorodin.infrastructure.auth.JwtService;
import org.smorodin.infrastructure.persistence.UserEntity;
import org.smorodin.infrastructure.persistence.UserRepository;
import org.smorodin.infrastructure.web.dto.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PlayerService playerService;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PlayerService playerService,
                       JwtService jwtService, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.playerService = playerService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(String username, String password) {
        if (userRepository.findByUserName(username).isEmpty()){
            String passwordHash = passwordEncoder.encode(password);
            UserEntity user = UserEntity.createNewUser(username,passwordHash);
            userRepository.save(user);
            Player player = playerService.createPlayer(user.getId(), username);
            String token = jwtService.generateToken(user.getId());
            return new AuthResponse(username,user.getId(),token);
        }
        else{
            throw new IllegalArgumentException("User with this name already exists: " + username);
        }
    }

    public AuthResponse login(String username, String password) {
        Optional<UserEntity> user = userRepository.findByUserName(username);
        if (user.isEmpty()){
            throw new IllegalArgumentException();
        }
        else{
            UserEntity userEntity = user.get();
            if (passwordEncoder.matches(password, userEntity.getPasswordHash())){
                return new AuthResponse(username, userEntity.getId(), jwtService.generateToken(userEntity.getId()));
            }
            else{
                throw new IllegalArgumentException("Wrong password");
            }
        }
    }
}