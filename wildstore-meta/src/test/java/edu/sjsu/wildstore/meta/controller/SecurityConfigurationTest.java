package edu.sjsu.wildstore.meta.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigurationTest {

    @Test
    void testRolesConfiguration() {
        assertEquals(3, SecurityConfiguration.ROLES.size());
        assertEquals("ROLE_ADMIN", SecurityConfiguration.ROLES.get(0));
        assertEquals("ROLE_USER", SecurityConfiguration.ROLES.get(1));
        assertEquals("ROLE_GUEST", SecurityConfiguration.ROLES.get(2));
    }
}