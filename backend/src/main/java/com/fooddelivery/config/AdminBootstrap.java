package com.fooddelivery.config;

import com.fooddelivery.model.Role;
import com.fooddelivery.model.User;
import com.fooddelivery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String name;
    private final String phone;

    public AdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String email,
            @Value("${app.admin.password:}") String password,
            @Value("${app.admin.name:Rasoi Admin}") String name,
            @Value("${app.admin.phone:}") String phone
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank()) {
            return;
        }
        String normalized = email.trim().toLowerCase();
        if (userRepository.findByEmail(normalized).isPresent()) {
            return;
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("ADMIN_PASSWORD is required to create the first admin account");
        }
        User admin = new User();
        admin.setName(name == null || name.isBlank() ? "Rasoi Admin" : name.trim());
        admin.setEmail(normalized);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
    }
}
