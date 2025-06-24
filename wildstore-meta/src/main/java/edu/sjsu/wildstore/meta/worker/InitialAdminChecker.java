package edu.sjsu.wildstore.meta.worker;

import edu.sjsu.wildstore.meta.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Configuration
public class InitialAdminChecker implements ApplicationRunner {

    UserService userService;
    List<String> initialAdmins;
    private static final Logger logger = Logger.getLogger(InitialAdminChecker.class.getName());

    public InitialAdminChecker(@Value("${wildstore.initialAdmins:#{null}}") List<String> initialAdmins, UserService userService) {
        this.userService = userService;
        this.initialAdmins = initialAdmins;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (initialAdmins != null) {
            for (String email : initialAdmins) {
                var result = userService.getUser(email);
                if (result.isEmpty()) {
                    logger.log(Level.INFO, "ℹ️Will create initial admin user: " + email);
                } else if ("ROLE_ADMIN".equals(result.iterator().next().get("role"))) {
                    logger.log(Level.INFO, "✅Already admin: " + email);
                    continue;
                }
                if (userService.updateUserRole(email, "ROLE_ADMIN")) {
                    logger.log(Level.INFO, "✅Created initial admin user: " + email);
                } else {
                    logger.log(Level.WARNING, "❗Failed to create initial admin user: " + email);
                }
            }
        }
    }
}
