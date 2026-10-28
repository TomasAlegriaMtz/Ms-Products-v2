package com.ids.ms_products.security;

import com.ids.ms_products.entity.User;
import com.ids.ms_products.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void loadUserByUsernameSuccessWithClienteRole() {
        User user = new User();
        user.setEmail("cliente@demo.com");
        user.setPassword("pass123");
        user.setRole("CLIENTE");
        user.setIsActive(true);

        when(userRepository.findByEmailIgnoreCase("cliente@demo.com")).thenReturn(Optional.of(user));

        UserDetails details = userService.loadUserByUsername("cliente@demo.com");

        assertNotNull(details);
        assertEquals("cliente@demo.com", details.getUsername());
        assertTrue(details.getPassword().startsWith("{noop}"));
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT")));
    }

    @Test
    void loadUserByUsernameSuccessWithAdminRole() {
        User user = new User();
        user.setEmail("admin@demo.com");
        user.setPassword("{noop}admin123");
        user.setRole("ROLE_ADMIN");
        user.setIsActive(true);

        when(userRepository.findByEmailIgnoreCase("admin@demo.com")).thenReturn(Optional.of(user));

        UserDetails details = userService.loadUserByUsername("admin@demo.com");
        assertNotNull(details);
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsernameThrowsWhenNotFound() {
        when(userRepository.findByEmailIgnoreCase("unknown@demo.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("unknown@demo.com"));
    }

    @Test
    void loadUserByUsernameThrowsWhenInactive() {
        User user = new User();
        user.setEmail("inactive@demo.com");
        user.setPassword("pass");
        user.setRole("CLIENT");
        user.setIsActive(false);

        when(userRepository.findByEmailIgnoreCase("inactive@demo.com")).thenReturn(Optional.of(user));

        assertThrows(DisabledException.class, () -> userService.loadUserByUsername("inactive@demo.com"));
    }
}