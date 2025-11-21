package edu.sjsu.wildstore.meta.controller;

import com.mongodb.DBObject;
import edu.sjsu.wildstore.meta.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Service
@RequestMapping("/api/profile")
public class UserController {
    UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public Map<String, Object> getUserProfile(Authentication authentication) {
        String email = authentication.getName();
        List<Map> result = userService.getUser(email);

        if (result == null || result.isEmpty()) {
            throw new IllegalArgumentException("User profile not found for " + email);
        }

        return result.get(0); // first (and only) user
    }
}