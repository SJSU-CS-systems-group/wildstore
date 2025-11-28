package edu.sjsu.wildstore.meta.controller;

import edu.sjsu.wildstore.meta.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public Map<String, Object> getUserProfile(OAuth2AuthenticationToken auth) {
        OAuth2User principal = auth.getPrincipal();
        String provider = auth.getAuthorizedClientRegistrationId();
        String email = "";

        switch (provider.toLowerCase()) {
            case "google":
                email = principal.getAttribute("email");
                break;
            case "github":
                email = principal.getAttribute("email");
                if (email == null) {
                    email = principal.getAttribute("login") + "@github";
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported provider: " + provider);
        }

        var result = userService.getUser(email);
        if (result == null || result.isEmpty()) {
            throw new IllegalArgumentException("User profile not found. email=" + email);
        }
        return result.get(0); // first (and only) user
    }
}