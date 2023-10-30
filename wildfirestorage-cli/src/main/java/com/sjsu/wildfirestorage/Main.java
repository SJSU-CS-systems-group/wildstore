package com.sjsu.wildfirestorage;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) {
        System.exit(new CommandLine(new Cli()).execute(args));
    }

    @CommandLine.Command(mixinStandardHelpOptions = true)
    static class Cli {

        @CommandLine.Command
        public void datasetInfo(@CommandLine.Parameters(paramLabel = "fileName") String fileName, @CommandLine.Parameters(paramLabel = "hostname") String hostname) throws InterruptedException {
            System.out.println("GET returned: " + Client.get(Client.getWebClient(hostname + "/api/metadata"),
                    new LinkedMultiValueMap<String, String>(Map.of("filename", List.of(fileName))),
                    new ParameterizedTypeReference<ArrayList<Metadata>>(){}));
        }

        @CommandLine.Command
        public void search(@CommandLine.Parameters(paramLabel = "query") String query, @CommandLine.Parameters(paramLabel = "hostname") String hostname,
                           @CommandLine.Parameters(paramLabel = "<option>", defaultValue = "all", description = "Which information to print - 'all' or 'basic'") String option,
                           @CommandLine.Option(names="--limit", defaultValue = "10") int limit,
                           @CommandLine.Option(names = "--offset", defaultValue = "0") int offset) throws InterruptedException, ExecutionException {

            MetadataRequest metadataRequest = new MetadataRequest();
            metadataRequest.searchQuery = query;
            metadataRequest.limit = limit;
            metadataRequest.offset = offset;
            metadataRequest.excludeFields = new String[] {"globalAttributes", "variables"};
            WebClient webClient = Client.getWebClient(hostname + "/api/metadata/search?excludeFields=globalAttributes,variables");
            var res = (ArrayList<Metadata>)(Client.post(webClient, metadataRequest, new ParameterizedTypeReference<ArrayList<Metadata>>(){}));
            System.out.println("SEARCH returned: " + res.size() + " results");
            if (option.equals("all")) {
                for (Metadata m: res) {
                    System.out.println("========================================================================");
                    PrintData.printAllData(m);
                }
            }
            else {
                for (Metadata m: res){
                    System.out.println("========================================================================");
                    PrintData.printBasic(m);
                }
            }
            System.out.println("SEARCH returned: " + res.size() + " results");
        }

        @CommandLine.Command
        public void clean(@CommandLine.Parameters(paramLabel = "limit") int limit, @CommandLine.Parameters(paramLabel = "hostname") String hostname) throws InterruptedException, ExecutionException {
            int offset = 0;
            LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();
            parameters.add("limit", String.valueOf(limit));
            List<String> result;
            int i=0;
            WebClient webClient = Client.getWebClient(hostname + "/api/metadata/filepath");

            do {
                parameters.put("offset", List.of(String.valueOf(offset)));
                result = (List<String>) Client.get(webClient,
                        parameters,
                        new ParameterizedTypeReference<List<String>>() {
                        });
                System.out.println("The following Metadata documents will be removed from the database:");
                result.forEach(str -> System.out.println(str));
                List<String> deletedFiles = result.stream().filter(item -> !Files.exists(Paths.get(item))).toList();
                System.out.println("DELETE RESULT:" + Client.post(webClient, deletedFiles, new ParameterizedTypeReference<Integer>() {}));
                offset += limit;
            } while (!result.isEmpty());
        }
    }
}
