package com.ids.ms_products.model;

import com.ids.ms_products.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserEntityTest {

    @Test
    void userExposesAllFields() {
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setId(1L);
        user.setName("Juan");
        user.setPaternalLastname("Pérez");
        user.setMaternalLastname("López");
        user.setEmail("juan@example.com");
        user.setPassword("secret");
        user.setRole("CLIENT");
        user.setIsActive(true);
        user.setAutenticado(1);
        user.setCreatedAt(now);

        assertEquals(1L, user.getId());
        assertEquals("Juan", user.getName());
        assertEquals("Pérez", user.getPaternalLastname());
        assertEquals("López", user.getMaternalLastname());
        assertEquals("juan@example.com", user.getEmail());
        assertEquals("secret", user.getPassword());
        assertEquals("CLIENT", user.getRole());
        assertTrue(user.getIsActive());
        assertEquals(1, user.getAutenticado());
        assertEquals(now, user.getCreatedAt());
    }
}