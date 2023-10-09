package com.sjsu.wildfirestorage;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
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
    @CommandLine.Option(names = "--enums", description = "Generate log of Enum Variable names")
    Boolean enumLog = false;
    @CommandLine.Option(names = "--parallelism", description = "Number of threads to use")
    int parallelism = 1;

    @CommandLine.Option(names = "--maxReadSize", description = "Number of data elements to read per read call")
    int maxReadSize = 1000000000;

    public void run() {
        Instant start = Instant.now();
        ConcurrentHashMap<String, String> status = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(parallelism);
        WebClient webClient = Client.getWebClient(hostname + "/api/metadata");
        Semaphore semaphore = new Semaphore(parallelism);
        try (Stream<String> stream = Files.lines(Paths.get(filesToProcessPath))) {
            stream.forEach(file -> {
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                executorService.submit(()->{
                    try {
                        crawl(file, webClient, status);
                    } finally {
                        semaphore.release();
                    }
                });
            });
            semaphore.acquire(parallelism);
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.SECONDS);

        } catch (IOException e) {
            System.out.println("There was an exception: " + e.getMessage());
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
        Instant finish = Instant.now();
        System.out.println("Execution Completed in: "+ Duration.between(start, finish).toMillis() + "ms");

        //Write API service to call dataset routing
        WebClient datasetWebClient = Client.getWebClient(hostname + "/api/dataset");
        try {
            if (hostname == null) {
                System.out.println("No hostname specified. Skipping dataset update.");
            } else {
                System.out.println("POSTING HERE");
                var res = Client.post(datasetWebClient, "", new ParameterizedTypeReference<Integer>(){});
                System.out.println("RESULT: " + res);
            }
        }
        catch (WebClientRequestException ex) {
            System.out.println("Dataset API call: "+ ex.getMostSpecificCause() + ex.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static void main(String[] args) {
        System.exit(new CommandLine(new WildfireFilesCrawler()).execute(args));
    }

    private void crawl(String file, WebClient webClient, ConcurrentHashMap<String,String> status){
        try {
            NetcdfFileReader fileReader = new NetcdfFileReader(file);
            var metadata = fileReader.processFile(maxReadSize);

            if (option.equals("all")) {
                PrintData.printAllData(metadata);
            }
            else if (option.equals("basic")) {
                PrintData.printBasic(metadata);
            }
            if(enumLog) {
                Path enumFile = Paths.get("enumVarList.txt");;
                try {
                    Files.createFile(enumFile);
                } catch (FileAlreadyExistsException e) {
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                PrintData.printEnums(enumFile, metadata);

            }
            if (hostname == null) {
                System.out.println("No hostname specified. Skipping metadata update.");
            } else {
                var res = Client.post(webClient, metadata, new ParameterizedTypeReference<Integer>(){});
                System.out.println("FILE: "+file+" DIGEST: " + metadata.digestString+" RESULT: " + res);
            }
        } catch (WebClientRequestException ex) {
            System.out.println(file + " -> " + ex.getMostSpecificCause() + ex.getMessage());
            status.put(file, ex.toString());
        } catch (Exception ex) {
            System.out.println(file + " -> " + ex.getMessage());
            status.put(file, ex.toString());
        }
    }
}
