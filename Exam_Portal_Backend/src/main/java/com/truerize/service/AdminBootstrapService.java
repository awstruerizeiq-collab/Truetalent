package com.truerize.service;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.truerize.config.FixedAdminCredentials;
import com.truerize.entity.Role;
import com.truerize.entity.User;
import com.truerize.repository.RoleRepository;
import com.truerize.repository.UserRepository;

@Component
public class AdminBootstrapService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(String... args) {
        ensureFixedAdminUser();
    }

    @Transactional
    public User ensureFixedAdminUser() {
        String normalizedAdminEmail = FixedAdminCredentials.EMAIL.trim().toLowerCase(Locale.ROOT);

        Role adminRole = roleRepository.findByName("ADMIN")
            .orElseGet(() -> roleRepository.save(new Role("ADMIN")));

        User adminUser = userRepository.findByEmailIgnoreCase(normalizedAdminEmail).orElse(null);

        if (adminUser == null) {
            User user = new User();
            user.setName(FixedAdminCredentials.NAME);
            user.setEmail(normalizedAdminEmail);
            user.setPassword(FixedAdminCredentials.PASSWORD);
            user.setStatus("Active");
            user.setRoles(Set.of(adminRole));
            user.setAssignedExams(new HashSet<>());

            User savedUser = userRepository.save(user);
            log.info("Bootstrapped admin user: {}", normalizedAdminEmail);
            return savedUser;
        }

        boolean shouldSave = false;
        if (adminUser.getRoles() == null) {
            adminUser.setRoles(new HashSet<>());
            shouldSave = true;
        }

        boolean hasAdminRole = adminUser.getRoles().stream()
            .anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getName()));

        if (!hasAdminRole) {
            adminUser.getRoles().add(adminRole);
            shouldSave = true;
        }

        if (adminUser.getStatus() == null || adminUser.getStatus().isBlank()) {
            adminUser.setStatus("Active");
            shouldSave = true;
        }

        if (adminUser.getPassword() == null || adminUser.getPassword().isBlank() || !FixedAdminCredentials.PASSWORD.equals(adminUser.getPassword())) {
            adminUser.setPassword(FixedAdminCredentials.PASSWORD);
            shouldSave = true;
        }

        if (shouldSave) {
            adminUser = userRepository.save(adminUser);
            log.info("Updated admin user bootstrap data: {}", normalizedAdminEmail);
        }

        return adminUser;
    }
}
