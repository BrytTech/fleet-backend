package org.fleet.backend.config;

import org.fleet.backend.entity.Role;
import org.fleet.backend.entity.User;
import org.fleet.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DatabaseSeeder implements CommandLineRunner {
    private static final Logger logger =LoggerFactory.getLogger(DatabaseSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public void run(String... args) throws Exception {


        if (!userRepository.existsByEmail("admin@fleet.com")){
            User admin = new User();

            admin.setFirstName("System");
            admin.setLastName("Admin");
            admin.setPassword(passwordEncoder.encode("fleet123"));
            admin.setEmail("admin@fleet.com");
            admin.setPhone("0000000000");
            admin.setRole(Role.ADMIN);
            admin.setActive(true);

            userRepository.save(admin);

            logger.info("Admin created successfully");
        } else {
            logger.info("Admin user already exist, skipping seed");
        }
    }
}
