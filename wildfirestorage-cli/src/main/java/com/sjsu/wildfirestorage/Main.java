package com.sjsu.wildfirestorage;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;
import org.springframework.util.LinkedMultiValueMap;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        System.exit(new CommandLine(new Cli()).execute(args));
    }

    @CommandLine.Command(mixinStandardHelpOptions = true)
    static class Cli {

        @CommandLine.Command
        public void datasetInfo(@CommandLine.Parameters(paramLabel = "fileName") String fileName, @CommandLine.Parameters(paramLabel = "hostname") String hostname) throws InterruptedException {
            System.out.println("GET returned: " + Client.get(hostname + "/api/metadata",
                    new LinkedMultiValueMap<String, String>(Map.of("filename", List.of(fileName))),
                    new ParameterizedTypeReference<ArrayList<Metadata>>(){}));
        }

        @CommandLine.Command
        public void search(@CommandLine.Parameters(paramLabel = "query") String query, @CommandLine.Parameters(paramLabel = "hostname") String hostname,
                           @CommandLine.Parameters(paramLabel = "<option>", defaultValue = "all", description = "Which information to print - 'all' or 'basic'") String option) throws InterruptedException {

            MetadataRequest metadataRequest = new MetadataRequest();
            metadataRequest.searchQuery = query;
            var res = (ArrayList<Metadata>)Client.post(hostname + "/api/metadata/search", metadataRequest, new ParameterizedTypeReference<ArrayList<Metadata>>(){});
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
    }
}
