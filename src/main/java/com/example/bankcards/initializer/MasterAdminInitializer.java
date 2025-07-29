package com.example.bankcards.initializer;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.RoleName;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.InternalException;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MasterAdminInitializer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private final String USERNAME = "owner";

    @Value("${app.master-access.password}")
    private String password;

    @PostConstruct
    public void init() {
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new InternalException("Role 'USER' not found in database. Please initialize roles."));
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new InternalException("Role 'USER' not found in database. Please initialize roles."));
        User masterAdmin = userRepository.findByUsername(USERNAME)
                .orElseGet(() -> new User(USERNAME));
        masterAdmin.setPassword(passwordEncoder.encode(password));
        addRoles(masterAdmin, List.of(adminRole, userRole));
        userRepository.save(masterAdmin);
        userRepository.flush();
    }

    private void addRoles(User user, List<Role> roles) {
        user.getRoles().addAll(roles);
    }
}
