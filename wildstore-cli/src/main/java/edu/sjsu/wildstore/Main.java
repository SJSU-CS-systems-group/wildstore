package edu.sjsu.wildstore;

import com.mongodb.DBObject;
import edu.sjsu.wildstore.meta.controller.ShareLinkController;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new Cli());
        cmd.setExecutionExceptionHandler((ex, cl, pr) -> {
            System.err.println(ex.getMessage());
            return 2;
        });
        cmd.setParameterExceptionHandler((ex, as) -> {
            var t = ex.getCause() != null ? ex.getCause() : ex;
            System.err.println(t.getMessage());
            if (t instanceof CommandLine.ParameterException) {
                cmd.usage(System.err);
            }
            return 1;
        });
        System.exit(cmd.execute(args));
    }

    @CommandLine.Command(mixinStandardHelpOptions = true, subcommands = { UserCli.class})
    static class Cli {
        @CommandLine.Option(names = "--xhelp", description = "show admin commands")
        public void setXHelp(boolean xhelp) {
            if (xhelp) {
                var cmd = new CommandLine(new Cli());
                cmd.getSubcommands().forEach((k,v) -> v.getCommandSpec().usageMessage().hidden(false));
                cmd.usage(System.out);
                System.exit(0);
            }
        }
        @CommandLine.Command
        public void datasetInfo(@CommandLine.Parameters(paramLabel = "fileName") String fileName,
                                @CommandLine.Parameters(paramLabel = "hostname") String hostname) throws
                InterruptedException {
            System.out.println("GET returned: " + Client.get(Client.getWebClient(hostname + "/api/metadata"),
                                                             new LinkedMultiValueMap<String, String>(Map.of("filename",
                                                                                                            List.of(fileName))),
                                                             new ParameterizedTypeReference<ArrayList<Metadata>>() {}));
        }

        @CommandLine.Command
        public void share(
                @CommandLine.Mixin CliOptions co,
                @CommandLine.Parameters(description = "Absolute file name", index = "0..*", arity = "1..*") String[] fileNames,
                @CommandLine.Option(names = "--email", split = ",", required = true, description = "Email addresses to share with " +
                        "separated with comma") String[] emails,
                @CommandLine.Option(names = "--validFor", description = "Validity of share link, values are: day, " +
                        "week, month, year", defaultValue = "month") String validFor) throws InterruptedException,
                ExecutionException {
            try {
                var result = Client.post(Client.getWebClient(co.metadataURL + "/api/share-link/create"),
                                               Map.of("fileNames",
                                                      fileNames,
                                                      "emailAddresses",
                                                      emails,
                                                      "validFor",
                                                      validFor),
                                               new ParameterizedTypeReference<ShareLinkController.CreatedLinks>() {},
                                               httpHeaders -> httpHeaders.setBearerAuth(co.token));
                result.created.forEach(co.out()::println);
                if (!result.missing.isEmpty()) co.err().println("Missing Files:");
                result.missing.forEach(co.err()::println);
            } catch (ExecutionException e) {
                var message = e.getMessage();
                // if this is a message about a connection problem, drop all the text before connection
                if (message.contains("Connection")) message = message.substring(message.indexOf("Connection"));
                co.err().printf("%s: %s\n", message, Arrays.toString(fileNames));
            } catch (WebClientResponseException e) {
                throw new CommandLine.ExecutionException(co.cmd(), e.getMessage(), null);
            }
        }

        @CommandLine.Command
        public void search(@CommandLine.Parameters(paramLabel = "query") String query,
                           @CommandLine.Parameters(paramLabel = "hostname") String hostname,
                           @CommandLine.Parameters(paramLabel = "<option>", defaultValue = "all", description =
                                   "Which information to print - 'all' or 'basic'")
                           String option,
                           @CommandLine.Option(names = "--limit", defaultValue = "10") int limit,
                           @CommandLine.Option(names = "--offset", defaultValue = "0") int offset,
                           @CommandLine.Option(names = "--token") String token) throws InterruptedException,
                ExecutionException {
            MetadataRequest metadataRequest = new MetadataRequest();
            metadataRequest.searchQuery = query;
            metadataRequest.limit = limit;
            metadataRequest.offset = offset;
            metadataRequest.excludeFields = new String[] { "globalAttributes", "variables" };
            WebClient webClient =
                    Client.getWebClient(hostname + "/api/metadata/search?excludeFields=globalAttributes,variables");
            var res = (ArrayList<Metadata>) (Client.post(webClient,
                                                         metadataRequest,
                                                         new ParameterizedTypeReference<ArrayList<Metadata>>() {},
                                                         httpHeaders -> httpHeaders.setBearerAuth(token)));
            System.out.println("SEARCH returned: " + res.size() + " results");
            if (option.equals("all")) {
                for (Metadata m : res) {
                    System.out.println("========================================================================");
                    PrintData.printAllData(m);
                }
            } else {
                for (Metadata m : res) {
                    System.out.println("========================================================================");
                    PrintData.printBasic(m);
                }
            }
            System.out.println("SEARCH returned: " + res.size() + " results");
        }

        @CommandLine.Command
        public void clean(@CommandLine.Mixin CliOptions co,
                          @CommandLine.Parameters(paramLabel = "limit", defaultValue = "10000") int limit,
                          @CommandLine.Option(names = "--no" + "-dryrun", negatable = true, defaultValue = "true") boolean dryrun)
                throws InterruptedException, ExecutionException {
            int offset = 0;
            LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();
            parameters.add("limit", String.valueOf(limit));
            List<Map<String, Object>> result;
            WebClient webClient = Client.getWebClient(co.metadataURL + "/api/metadata/filenames", co.token);

            do {
                parameters.add("offset", String.valueOf(offset));
                result = Client.get(webClient,
                                    parameters,
                                    new ParameterizedTypeReference<>() {});
                List<String> deletedFiles = result.stream().map(i -> i.get("fileName").toString()).filter(item -> !Files.exists(Paths.get(item))).toList();
                if (!dryrun) System.out.println("DELETE RESULT:" + Client.post(webClient,
                                                                  deletedFiles,
                                                                  new ParameterizedTypeReference<Integer>() {},
                                                                  httpHeaders -> httpHeaders.setBearerAuth(co.token)));
                co.out().println("Deleted Files: " + String.join("\n", deletedFiles));
                offset += limit;
            } while (result.size() == offset);
            if (dryrun) System.out.println("DRYRUN: NO FILES WERE DELETED.");
        }

        @CommandLine.Command
        public void cleanlinks(@CommandLine.Mixin CliOptions co,
                               @CommandLine.Option(names = "--no" + "-dryrun", negatable = true, defaultValue = "true") boolean dryrun) throws
                ExecutionException, InterruptedException {

            List<Object> shareLinks = new ArrayList<>();
            LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();
            int limit = 10000;
            int offset = 0;
            parameters.add("limit", "10000");
            parameters.add("offset", "0");
            do {
                shareLinks = Client.get(Client.getWebClient(co.metadataURL + "/api/share-link/", co.token),
                                                         parameters,
                                                         new ParameterizedTypeReference<List<Object>>() {});
                List<Map<String, String>> deletedLinks = shareLinks.stream()
                        .map(obj -> (LinkedHashMap<?, ?>) obj)
                        .filter(obj -> OffsetDateTime.parse(obj.get("expiry").toString()).toLocalDateTime().isBefore(LocalDateTime.now()))
                        .map(obj -> {
                            String filePath = obj.get("filePath").toString();
                            String emailAddresses = obj.get("emailAddresses").toString();
                            return Map.of(
                                    "filePath", filePath,
                                    "emailAddresses", emailAddresses
                            );
                        })
                        .collect(Collectors.toList());

                if (!dryrun) System.out.println("DELETE RESULT:" + Client.post(Client.getWebClient(co.metadataURL + "/api/share-link/delete", co.token),
                                                                               deletedLinks,
                                                                               new ParameterizedTypeReference<Integer>() {},
                                                                               httpHeaders -> httpHeaders.setBearerAuth(co.token)));
                co.out().println("Deleted Links: " + deletedLinks.stream().map(map -> map.get("filePath") + " : " + map.get("emailAddresses")).collect(Collectors.joining("\n")));
                offset += limit;
            } while (shareLinks.size() == limit);
            if (dryrun) System.out.println("DRYRUN: NO LINKS WERE DELETED.");
        }
    }

}
