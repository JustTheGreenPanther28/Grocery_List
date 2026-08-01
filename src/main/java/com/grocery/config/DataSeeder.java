package com.grocery.config;

import com.grocery.model.AppUser;
import com.grocery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Runs once on every startup. Only INSERTS a user if that username doesn't
// already exist in Supabase yet - so after the first run, this has no effect
// even if the env var passwords change. To rotate a password, delete the row
// in Supabase (or update the hash directly) rather than relying on this.
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.user1.username}")
    private String user1Username;
    @Value("${app.seed.user1.password}")
    private String user1Password;

    @Value("${app.seed.user2.username}")
    private String user2Username;
    @Value("${app.seed.user2.password}")
    private String user2Password;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedIfMissing(user1Username, user1Password);
        seedIfMissing(user2Username, user2Password);
    }

    private void seedIfMissing(String username, String rawPassword) {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            return; // env vars not set - skip rather than seed a broken account
        }
        if (!userRepository.existsByUsername(username)) {
            userRepository.save(new AppUser(username, passwordEncoder.encode(rawPassword)));
        }
    }
}
