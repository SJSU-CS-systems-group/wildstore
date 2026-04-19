package edu.sjsu.wildstore.meta.controller;

import edu.sjsu.wildstore.meta.MongoCollections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigurationTest extends BaseControllerTest {

    @Autowired
    private SecurityConfiguration securityConfiguration;

    @AfterEach
    void cleanup() {
        mongoTemplate.remove(new Query(Criteria.where("email").is("tokenuser@example.com")), MongoCollections.USER_DATA);
        mongoTemplate.remove(new Query(Criteria.where("email").is("githubuser@github")), MongoCollections.USER_DATA);
        mongoTemplate.remove(new Query(Criteria.where("email").is("oidcuser@example.com")), MongoCollections.USER_DATA);
    }

    @Test
    void testRolesConfiguration() {
        assertEquals(3, SecurityConfiguration.ROLES.size());
        assertEquals("ROLE_ADMIN", SecurityConfiguration.ROLES.get(0));
        assertEquals("ROLE_USER", SecurityConfiguration.ROLES.get(1));
        assertEquals("ROLE_GUEST", SecurityConfiguration.ROLES.get(2));
    }

    @Test
    void testOpaqueAndUserAuthoritiesMapper() throws Exception {
        // opaque(): valid token in MongoDB, expect: authenticated with role from DB
        String validToken = "validtoken123";
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", "tokenuser@example.com");
        userData.put("name", "Token User");
        userData.put("role", "ROLE_USER");
        userData.put("token", validToken);
        mongoTemplate.insert(userData, MongoCollections.USER_DATA);

        AuthenticationManager authManager = securityConfiguration.opaque();
        Authentication auth = authManager.authenticate(new BearerTokenAuthenticationToken(validToken));
        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));

        // opaque(): invalid token, expect: OAuth2AuthenticationException
        assertThrows(OAuth2AuthenticationException.class, () ->
                authManager.authenticate(new BearerTokenAuthenticationToken("invalidtoken")));

        // userAuthoritiesMapper(): OAUTH2_USER with known login, expect: role from DB
        Map<String, Object> githubUser = new HashMap<>();
        githubUser.put("email", "githubuser@github");
        githubUser.put("name", "GitHub User");
        githubUser.put("role", "ROLE_ADMIN");
        mongoTemplate.insert(githubUser, MongoCollections.USER_DATA);

        Method mapperMethod = SecurityConfiguration.class.getDeclaredMethod("userAuthoritiesMapper");
        mapperMethod.setAccessible(true);
        GrantedAuthoritiesMapper mapper = (GrantedAuthoritiesMapper) mapperMethod.invoke(securityConfiguration);

        Map<String, Object> oauth2Attrs = new HashMap<>();
        oauth2Attrs.put("login", "githubuser");
        Collection<? extends GrantedAuthority> mapped = mapper.mapAuthorities(
                List.of(new OAuth2UserAuthority(oauth2Attrs)));
        assertTrue(mapped.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        // userAuthoritiesMapper(): OAUTH2_USER with unknown login, expect: ROLE_GUEST
        Map<String, Object> unknownAttrs = new HashMap<>();
        unknownAttrs.put("login", "unknownlogin");
        Collection<? extends GrantedAuthority> unknownMapped = mapper.mapAuthorities(
                List.of(new OAuth2UserAuthority(unknownAttrs)));
        assertTrue(unknownMapped.stream().anyMatch(a -> a.getAuthority().equals("ROLE_GUEST")));

        // userAuthoritiesMapper(): OIDC_USER with known email, expect: role from DB
        Map<String, Object> oidcUser = new HashMap<>();
        oidcUser.put("email", "oidcuser@example.com");
        oidcUser.put("name", "OIDC User");
        oidcUser.put("role", "ROLE_ADMIN");
        mongoTemplate.insert(oidcUser, MongoCollections.USER_DATA);

        Instant now = Instant.now();
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "oidcuser-sub");
        claims.put("iss", "https://accounts.google.com");
        claims.put("iat", now);
        claims.put("exp", now.plusSeconds(3600));
        claims.put("email", "oidcuser@example.com");
        OidcIdToken idToken = new OidcIdToken("idtoken", now, now.plusSeconds(3600), claims);
        Collection<? extends GrantedAuthority> oidcMapped = mapper.mapAuthorities(
                List.of(new OidcUserAuthority(idToken)));
        assertTrue(oidcMapped.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));

        // userAuthoritiesMapper(): OIDC_USER with unknown email, expect: ROLE_GUEST
        Map<String, Object> unknownClaims = new HashMap<>();
        unknownClaims.put("sub", "unknown-sub");
        unknownClaims.put("iss", "https://accounts.google.com");
        unknownClaims.put("iat", now);
        unknownClaims.put("exp", now.plusSeconds(3600));
        unknownClaims.put("email", "nobody@unknown.com");
        OidcIdToken unknownIdToken = new OidcIdToken("idtoken2", now, now.plusSeconds(3600), unknownClaims);
        Collection<? extends GrantedAuthority> oidcUnknownMapped = mapper.mapAuthorities(
                List.of(new OidcUserAuthority(unknownIdToken)));
        assertTrue(oidcUnknownMapped.stream().anyMatch(a -> a.getAuthority().equals("ROLE_GUEST")));

        // userAuthoritiesMapper(): other authority, expect: passthrough
        Collection<? extends GrantedAuthority> passthroughMapped = mapper.mapAuthorities(
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOM")));
        assertTrue(passthroughMapped.stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOM")));
    }
}