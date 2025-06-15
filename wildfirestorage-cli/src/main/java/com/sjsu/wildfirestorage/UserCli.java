package com.sjsu.wildfirestorage;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import picocli.CommandLine;

import java.util.List;
import java.util.Map;

@CommandLine.Command(name = "user", description = "manage users. admin role required.", mixinStandardHelpOptions = true, hidden = true)
class UserCli {
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @CommandLine.Command(description = "List all users with their roles")
    public void list(@CommandLine.Mixin CliOptions cliOptions) throws CommandLine.PicocliException {
        try {
            List<Map> users = Client.get(cliOptions.metadataURL + "/api/userlist/", cliOptions.token);
            if (users == null) {
                throw new CommandLine.ExecutionException(spec.commandLine(), "Error retrieving user list", null);
            } else if (users.isEmpty()) {
                System.out.println("No users found");
            } else {
                users.forEach(u -> System.out.printf("%s: %s %s%n", u.get("email"), u.get("role"), u.get("name")));
            }
        } catch (WebClientResponseException e) {
            throw new CommandLine.ExecutionException(spec.commandLine(), e.getMessage(), null);
        }
    }

    @CommandLine.Command(description = "Update or Add and email with a role")
    public void update(@CommandLine.Parameters(paramLabel = "email", description = "email to add/update", index = "0")
                       String email,
                       @CommandLine.Option(names = "--role", description = "role to assign") String role,
                       @CommandLine.Mixin CliOptions cliOptions) {
        var canonicalRole = switch (role) {
            case "admin", "ADMIN", "ROLE_ADMIN" -> "ROLE_ADMIN";
            case "user", "USER", "ROLE_USER" -> "ROLE_USER";
            case "guest", "GUEST", "ROLE_GUEST" -> "ROLE_GUEST";
            default -> {
                System.out.println("Invalid role, must be one of admin, user, guest");
                System.exit(2);
                yield null;
            }
        };
        Boolean result = Client.post(
                cliOptions.metadataURL + "/api/userlist/" + email,
                Map.of("role", role),
                cliOptions.token
        );
        if (result == null || !result) {
            System.out.println("Error updating user");
        } else {
            System.out.printf("User %s updated to role %s%n", email, canonicalRole);
        }
    }

    @CommandLine.Command(description = "Remove a user by email")
    public void remove(@CommandLine.Parameters(paramLabel = "email", description = "email to add/update", index = "0")
                       String email, @CommandLine.Mixin CliOptions cliOptions) {
        WebClient webClient = Client.getWebClient(cliOptions.metadataURL + "/api/userlist/" + email, cliOptions.token);
        var result = webClient.delete().retrieve().bodyToMono(Boolean.class).retry(1).onErrorComplete(e -> {
            System.err.println(e.getMessage());
            return true;
        }).block();
        if (result == null || !result) {
            System.out.printf("Error deleting %s%n", email);
        } else {
            System.out.printf("%s deleted%n", email);
        }
    }
}