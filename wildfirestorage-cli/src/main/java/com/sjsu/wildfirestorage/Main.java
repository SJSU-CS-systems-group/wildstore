package com.sjsu.wildfirestorage;
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
        public void datasetInfo(@CommandLine.Parameters(paramLabel = "fileName") String fileName) throws InterruptedException {
            System.out.println("GET returned: " + Client.get("http://localhost:8080/api/metadata", new LinkedMultiValueMap<String, String>(Map.of("filename", List.of(fileName)))));
        }

        // Dummy method for post
        @CommandLine.Command
        public void addDataset() throws InterruptedException {
            ArrayList<Metadata> mdt = (ArrayList<Metadata>)Client.get("http://localhost:8080/api/metadata", new LinkedMultiValueMap<String, String>(Map.of("filename", List.of("wrfout"))));
            System.out.println("POST returned: " + (Integer) Client.post("http://localhost:8080/api/metadata", mdt.get(0)));
        }
    }
}