package com.ids.ms_products.security;

import com.ids.ms_products.entity.User;
import com.ids.ms_products.repository.UserRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("No se encontró usuario con el correo: " + email));

        if (user.getIsActive() != null && !user.getIsActive()) {
            throw new DisabledException("El usuario se encuentra inactivo");
        }

        String dbRole = user.getRole() != null ? user.getRole().toUpperCase().replace("ROLE_", "").trim() : "CLIENT";
        if ("CLIENTE".equals(dbRole)) {
            dbRole = "CLIENT";
        }

        String password = user.getPassword();
        if (password != null && !password.startsWith("{")) {
            password = "{noop}" + password;
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(password)
                .roles(dbRole) // Genera ROLE_ADMIN o ROLE_CLIENT
                .disabled(user.getIsActive() != null && !user.getIsActive())
                .build();
    }
}