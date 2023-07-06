package com.sjsu.wildfirestorage;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import picocli.CommandLine;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@CommandLine.Command(name = "GET", mixinStandardHelpOptions = true)
public class WildfireFilesCrawler implements Runnable {
    @CommandLine.Option(names = "--option", defaultValue = "all", description = "Which information to print - 'all' or 'basic'")
    private String option;
    @CommandLine.Parameters(paramLabel = "<file>", description = "Path to the file containing list of NetCDF files to process")
    private String filesToProcessPath;
    @CommandLine.Option(names = "--hostname", description = "Host name of the API server")
    String hostname;

    @CommandLine.Option(names = "--log", description = "Whether to generate a log")
    Boolean log = false;

    public void run() {
        Map<String, String> status = new ConcurrentHashMap<>();
        try (Stream<String> stream = Files.lines(Paths.get(filesToProcessPath))) {
            stream.parallel().forEach(file -> {
                try {
                    NetcdfFileReader fileReader = new NetcdfFileReader(file);
                    var metadata = fileReader.processFile();

                    if (option.equals("all")) {
                        PrintData.printAllData(metadata);
                    }
                    else if (option.equals("basic")) {
                        PrintData.printBasic(metadata);
                    }
                    if (hostname == null) {
                        System.out.println("No hostname specified. Skipping metadata update.");
                    } else {

                        Client.post(hostname + "/api/metadata", metadata, new ParameterizedTypeReference<Integer>(){}, error -> {
                            try {
                                var errBody = error.bodyToMono(String.class).toFuture().get();
                                System.out.println("POST Error: "+errBody);
                                status.put(file, errBody);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            return null;
                        }).subscribe(response -> {
                            System.out.println("POST response: " + (Integer)response);
                        });
                    }
                } catch (WebClientRequestException ex) {
                    StringWriter errors = new StringWriter();
                    ex.printStackTrace(new PrintWriter(errors));
                    System.out.println(file + " -> " + ex.getMostSpecificCause() + ex.getMessage());
                    ex.printStackTrace();
                    status.put(file, errors.toString());
                } catch (Exception ex) {
                    StringWriter errors = new StringWriter();
                    ex.printStackTrace(new PrintWriter(errors));
                    System.out.println(file + " -> " + ex.getMessage());
                    ex.printStackTrace();
                    status.put(file, errors.toString());
                }
            });
        } catch (IOException e) {
            System.out.println("There was an exception: " + e.getMessage());
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if(log) {
            try {
                Path statusFile = Paths.get(filesToProcessPath + ".log");
                Files.deleteIfExists(statusFile);
                Files.createFile(statusFile);
                StringBuilder statusStr = new StringBuilder();
                status.entrySet().stream().forEach(entry -> {
                    statusStr.append(entry.getKey() + " -> " + entry.getValue() + System.lineSeparator());
                });
                Files.writeString(statusFile, statusStr.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public static void main(String[] args) {
        System.exit(new CommandLine(new WildfireFilesCrawler()).execute(args));
    }
}
