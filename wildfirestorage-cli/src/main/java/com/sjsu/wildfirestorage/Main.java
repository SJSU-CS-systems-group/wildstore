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
        public void datasetInfo(@CommandLine.Parameters(paramLabel = "fileName") String fileName) throws InterruptedException {
            System.out.println("GET returned: " + Client.get("http://cloud.homeofcode.com:27777/api/metadata",
                    new LinkedMultiValueMap<String, String>(Map.of("filename", List.of(fileName))),
                    new ParameterizedTypeReference<ArrayList<Metadata>>(){}));
        }

        @CommandLine.Command
        public void search(@CommandLine.Parameters(paramLabel = "query") String query) throws InterruptedException {
            MetadataRequest metadataRequest = new MetadataRequest();
            metadataRequest.searchQuery = query;
            System.out.println("GET returned: " + Client.post("http://localhost:8080/api/metadata/search", metadataRequest));
        }

        // Dummy method for post
        @CommandLine.Command
        public void addDataset() throws InterruptedException {
            ArrayList<Metadata> mdtList = (ArrayList<Metadata>)Client.get("http://localhost:8080/api/metadata",
                    new LinkedMultiValueMap<String, String>(Map.of("filename", List.of("wrfout"))),
                    new ParameterizedTypeReference<ArrayList<Metadata>>(){});

            Metadata metadata = mdtList.get(0);

            // Modify metadata filename
            metadata.fileName = "newfilename" + System.currentTimeMillis();

            // Set location
            Point p1 = new Point(10,10);
            Point p2 = new Point(20,30);
            Point p3 = new Point(30,10);
            Point p4 = new Point(10,10);
            GeoJsonPolygon polygon =new GeoJsonPolygon(List.of(p1, p2, p3, p4));
            metadata.location = polygon;

            // Send POST request
            System.out.println("POST returned: " +  Client.post("http://localhost:8080/api/metadata", metadata));
        }
    }
}