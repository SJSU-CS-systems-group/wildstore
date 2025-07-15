package edu.sjsu.wildstore;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@Command(name = "user", description = "manage users. admin role required.", mixinStandardHelpOptions = true)
class UserCli {

    @Command(description = "List all users with their roles")
    public void list(@Mixin CliOptions cliOptions) throws CommandLine.PicocliException {
        try {
            List<Map> users = Client.get(cliOptions.metadataURL + "/api/userlist/", cliOptions.token);
            if (users == null) {
                throw new CommandLine.ExecutionException(cliOptions.cmd(), "Error retrieving user list", null);
            } else if (users.isEmpty()) {
                cliOptions.out().println("No users found");
            } else {
                users.forEach(u -> cliOptions.out().printf("%s: %s %s%n", u.get("email"), u.get("role"), u.get("name")));
            }
        } catch (WebClientResponseException e) {
            throw new CommandLine.ExecutionException(cliOptions.cmd(), e.getMessage(), null);
        }
    }

    @Command(description = "Get token for a user")
    public void getToken(@Mixin CliOptions cliOptions,
                         @Parameters(paramLabel = "email", description = "email to add/update", index = "0")
                         String email) throws CommandLine.PicocliException {
        try {
            List<Map> result = Client.get(cliOptions.metadataURL + "/api/userlist/" + email, cliOptions.token);
            if (result == null || result.isEmpty()) {
                throw new CommandLine.ExecutionException(cliOptions.cmd(), "User not found", null);
            }
            String token = (String)result.get(0).get("token");
            if (token == null) {
                throw new CommandLine.ExecutionException(cliOptions.cmd(), "User token not found", null);
            } else {
                cliOptions.out().printf("%s: %s%n", email, token);
            }
        } catch (WebClientResponseException e) {
            throw new CommandLine.ExecutionException(cliOptions.cmd(), e.getMessage(), null);
        }
    }

    @Command(description = "Update or Add and email with a role")
    public void update(@Parameters(paramLabel = "email", description = "email to add/update", index = "0")
                       String email,
                       @Option(names = "--role", description = "role to assign",  defaultValue =  "user") String role,
                       @Mixin CliOptions cliOptions) {
        var canonicalRole = switch (role) {
            case "admin", "ADMIN", "ROLE_ADMIN" -> "ROLE_ADMIN";
            case "user", "USER", "ROLE_USER" -> "ROLE_USER";
            case "guest", "GUEST", "ROLE_GUEST" -> "ROLE_GUEST";
            default -> throw new CommandLine.ExecutionException(cliOptions.cmd(), "Invalid role, must be one of admin, user, guest");
        };
        try {
            Boolean result = Client.post(cliOptions.metadataURL + "/api/userlist/" + email,
                                         Map.of("role", canonicalRole),
                                         cliOptions.token);
            if (result == null || !result) {
                throw new CommandLine.ExecutionException(cliOptions.cmd(), "Error updating user");
            } else {
                cliOptions.out().printf("User %s updated to role %s%n", email, canonicalRole);
            }
        } catch (WebClientResponseException e) {
            throw new CommandLine.ExecutionException(cliOptions.cmd(), e.getMessage(), null);
        }
    }

    @Command(description = "Remove a user by email")
    public void remove(@Parameters(paramLabel = "email", description = "email to add/update", index = "0")
                       String email, @Mixin CliOptions cliOptions) {
        WebClient webClient = Client.getWebClient(cliOptions.metadataURL + "/api/userlist/" + email, cliOptions.token);
        var result = webClient.delete().retrieve().bodyToMono(Boolean.class).retry(1).onErrorComplete(e -> {
            cliOptions.err().println(e.getMessage());
            return true;
        }).block();
        if (result == null || !result) {
            cliOptions.out().printf("Error deleting %s%n", email);
        } else {
            cliOptions.out().printf("%s deleted%n", email);
        }
    }
}