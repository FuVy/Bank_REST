package com.example.bankcards.service.jpa;

import com.example.bankcards.dto.security.JwtAuthResponse;
import com.example.bankcards.dto.security.LoginRequest;
import com.example.bankcards.dto.security.RegisterRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleName;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.InternalException;
import com.example.bankcards.exception.user.UserAlreadyExistsException;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtTokenProvider;
import com.example.bankcards.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JpaAuthService implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public JwtAuthResponse login(LoginRequest loginRequest) {
        return login(loginRequest.getUsername(), loginRequest.getPassword());
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    private JwtAuthResponse login(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return new JwtAuthResponse(jwtTokenProvider.generateToken(authentication.getName()));
    }

    @Override
    @Transactional
    public JwtAuthResponse register(RegisterRequest registerRequest) {
        String username = registerRequest.getUsername();
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException(username);
        }
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new InternalException("Role 'USER' not found in database. Please initialize roles."));
        newUser.getRoles().add(userRole);
        userRepository.save(newUser);
        return login(registerRequest.getUsername(), registerRequest.getPassword());
    }
}
