package com.example.bankcards.initializer;

import com.example.bankcards.entity.*;
import com.example.bankcards.exception.InternalException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardEncryptionUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@Profile("h2")
@RequiredArgsConstructor
public class H2DataInitializer {

    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final RoleRepository roleRepository;
    private final CardEncryptionUtil cardEncryptionUtil;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new InternalException("Role 'USER' not found in database. Please initialize roles."));

        User user1 = new User("user1", passwordEncoder.encode("pass1"));
        User user2 = new User("user2", passwordEncoder.encode("pass2"));
        User user3 = new User("user3", passwordEncoder.encode("pass3"));
        User admin = new User("admin", passwordEncoder.encode("password"));
        user1.getRoles().add(userRole);
        user2.getRoles().add(userRole);
        user3.getRoles().add(userRole);
        admin.getRoles().add(roleRepository.findByName(RoleName.ADMIN).get());
        admin.getRoles().add(userRole);
        userRepository.saveAll(List.of(user1, user2, user3, admin));
        userRepository.flush();

        String encrypted1 = cardEncryptionUtil.encrypt("2380328656218459");
        String encrypted2 = cardEncryptionUtil.encrypt("7205674277714399");
        String encrypted3 = cardEncryptionUtil.encrypt("8706647592287922");
        String encrypted4 = cardEncryptionUtil.encrypt("0613192986491444");
        String encrypted5 = cardEncryptionUtil.encrypt("7163082819181661");

        Card card1 = new Card(
                encrypted1,
                user1,
                LocalDate.of(2028, 6, 10),
                CardStatus.ACTIVE,
                new BigDecimal("22.10")
        );
        Card card2 = new Card(
                encrypted2,
                user2,
                LocalDate.of(2028, 6, 10),
                CardStatus.ACTIVE,
                new BigDecimal("22.10")
        );
        Card card3 = new Card(
                encrypted3,
                user3,
                LocalDate.of(2024, 6, 10),
                CardStatus.EXPIRED,
                new BigDecimal("22.10")
        );
        Card card4 = new Card(
                encrypted4,
                user3,
                LocalDate.of(2028, 6, 10),
                CardStatus.BLOCKED,
                new BigDecimal("22.10")
        );
        Card card5 = new Card(
                encrypted5,
                user3,
                LocalDate.of(2028, 6, 10),
                CardStatus.ACTIVE,
                new BigDecimal("22.10")
        );

        cardRepository.saveAll(List.of(card1, card2, card3, card4, card5));
        cardRepository.flush();
    }
}
