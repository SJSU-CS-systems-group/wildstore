package edu.sjsu.wildstore.meta.controller;

import edu.sjsu.wildstore.meta.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/userlist")
public class UsersController {
    
    UserService userService;
    
    public UsersController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/")
    public List<Map> getUserList() {
        return userService.getUserList();
    }


    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{email}")
    public boolean deleteUser(@PathVariable String email) {
        return userService.deleteUser(email);
    }



    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{email}")
    public List<Map> getUser(@PathVariable String email) {
        return userService.getUser(email);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{email}")
    public boolean updateUserRole(@RequestBody Map<String, String> request, @PathVariable String email) {
        var role = request.get("role");

        if (role == null) {
            throw new IllegalArgumentException("Missing role in request");
        }
        if (!SecurityConfiguration.ROLES.contains(role)) {
            throw new IllegalArgumentException(role + " is not one of " + String.join(",", SecurityConfiguration.ROLES));
        }
        return userService.updateUserRole(email, role);
    }
}

