package com.fooddelivery.config;

import com.fooddelivery.model.Role;
import com.fooddelivery.model.User;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final boolean enabled;
    private final String demoPassword;
    private final DataSource dataSource;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(
            @Value("${app.demo-seed:false}") boolean enabled,
            @Value("${app.demo.user-password:}") String demoPassword,
            DataSource dataSource,
            RestaurantRepository restaurantRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.enabled = enabled;
        this.demoPassword = demoPassword;
        this.dataSource = dataSource;
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        if (restaurantRepository.count() == 0) {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("data.sql"));
            populator.setContinueOnError(false);
            populator.execute(dataSource);
            log.info("Loaded demo restaurants and dishes");
        } else {
            log.info("Restaurants already exist, so demo menu rows were left unchanged");
        }
        createCustomerIfMissing("user@fooddelivery.com", "Rahul Mehta", "9876501234");
        createCustomerIfMissing("anita.desai@fooddelivery.com", "Anita Desai", "9811122233");
    }

    private void createCustomerIfMissing(String email, String name, String phone) {
        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }
        if (demoPassword == null || demoPassword.isBlank()) {
            log.warn("Skipped demo user {} because DEMO_USER_PASSWORD is empty", email);
            return;
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(demoPassword));
        user.setRole(Role.USER);
        userRepository.save(user);
    }
}
