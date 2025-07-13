package edu.sjsu.wildstore;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandLine.Command(name = "GET", mixinStandardHelpOptions = true)
public class WildfireFilesCrawler implements Runnable {
    private static final String DONE_SENTINEL = "DONE";
    @CommandLine.Option(names = "--option", defaultValue = "all", description = "Which information to print - 'all' or 'basic'")
    private String option;
    @CommandLine.Parameters(paramLabel = "<file>", description = "Path to the file containing list of NetCDF files to process", arity = "1")
    private String filesToProcessPath;
    @CommandLine.Option(names = "--metaURL", description = "Host name of the API server", required = true)
    String metaURL;
    @CommandLine.Option(names = "--log", description = "Whether to generate a log")
    Boolean log = false;
    @CommandLine.Option(names = "--enums", description = "Generate log of Enum Variable names")
    Boolean enumLog = false;
    @CommandLine.Option(names = "--parallelism", description = "Number of threads to use")
    int parallelism = 1;

    @CommandLine.Option(names = "--maxReadSize", description = "Number of data elements to read per read call")
    int maxReadSize = 1000000000;

    @CommandLine.Option(names = "--tokenFile", required = true) String tokenFile;

    @CommandLine.Option(names = "--dataset", description = "Whether to initiate dataset collection") boolean initiateDatasetCollection = false;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    final PrintWriter out() {
        return cmd().getOut();
    }

    final PrintWriter err() {
        return cmd().getErr();
    }

    final CommandLine cmd() {
        return spec.commandLine();
    }

    private class WalkerThread extends Thread {
        private final Path pathToWalk;
        private final LinkedBlockingQueue<String> pathNameQueue;

        WalkerThread(Path pathToWalk, LinkedBlockingQueue<String> pathNameQueue) {
            this.pathToWalk = pathToWalk;
            this.pathNameQueue = pathNameQueue;
        }
        @Override
        public void run() {
            try {
                Files.walkFileTree(pathToWalk, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (file.toFile().isFile() && file.toString().endsWith(".nc")) {
                            pathNameQueue.offer(file.toString());
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        err().println("Error visiting file: " + file + " - " + exc.getClass().getName());
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                err().println("Error walking file tree: " + e.getMessage());
            } finally {
                pathNameQueue.offer(DONE_SENTINEL);
            }
        }
    }
    public void run() {
        var okayException = new Exception("No exception");
        Properties appProps = new Properties();
        try {
            appProps.load(new FileInputStream(tokenFile));
        } catch (FileNotFoundException e) {
            throw new CommandLine.PicocliException(tokenFile + " is not a valid file", e);
        } catch (IOException e) {
            throw new CommandLine.PicocliException("Error loading properties from token file: " + tokenFile, e);
        }
        String token = appProps.getProperty("token");
        Instant start = Instant.now();
        ConcurrentHashMap<String, Exception> status = new ConcurrentHashMap<>();
        WebClient webClient = Client.getWebClient(metaURL + "/api/metadata", token);
        AtomicInteger filesCount = new AtomicInteger();
        AtomicInteger crawledCount = new AtomicInteger();

        List<Map<String, Object>> fileNames =  new ArrayList<>();
        List<Map<String, Object>> newNames;
        var limit = 10000;
        var offset = 0;
        try {
            do {
                LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
                parameters.add("limit", String.valueOf(limit));
                parameters.add("offset", String.valueOf(offset));
                newNames = Client.get(Client.getWebClient(metaURL + "/api/metadata/filenames", token), parameters, new ParameterizedTypeReference<>() {});
                fileNames.addAll(newNames);
                offset += limit;
            } while (newNames.size() == limit);
        } catch (Exception e) {
            err().println("Error fetching file names from metadata service: " + e.getClass().getName());
            if (e instanceof WebClientResponseException webException &&
                    webException.getStatusCode().is4xxClientError()) {
                err().println("Unrecoverable authorization error: " + webException.getMessage());
            }
            throw new CommandLine.ExecutionException(cmd(), "Error fetching file names from metadata service: " + e.getMessage(), e);
        }

        Map<String, Long> fileNamesMap = fileNames.stream()
                .collect(Collectors.toMap(
                        map -> map.get("fileName").toString(),
                        map -> (Long) map.get("lastModified"),
                        Long::min));

        ForkJoinPool customPool = new ForkJoinPool(parallelism); // set desired parallelism

        var poolResult = customPool.submit(() -> {
            try (Stream<String> stream = Files.lines(Paths.get(filesToProcessPath))) {
                var exceptions = stream.flatMap(fileName -> {
                    File file = new File(fileName);
                    if (file.isDirectory()) {
                        var nameQueue = new LinkedBlockingQueue<String>();
                        new WalkerThread(file.toPath(), nameQueue).start();
                        return Stream.generate(() -> {
                            try {
                                return nameQueue.take();
                            } catch (InterruptedException e) {
                                return DONE_SENTINEL;
                            }
                            // for sentinel value use == not equals!
                        }).takeWhile(s -> s != DONE_SENTINEL);
                    } else {
                        return Stream.of(fileName);
                    }
                }).filter(file -> {
                    try {
                        if (fileNamesMap.containsKey(file) &&
                                fileNamesMap.get(file) >= Files.getLastModifiedTime(Paths.get(file)).toMillis()) {
                            return false;
                        }
                        return true;
                    } catch (IOException e) {
                        err().println("Error fetching fileNames or lastModified from map:" + e.getClass().getName());
                        throw new CommandLine.ExecutionException(cmd(),
                                                                 "Error fetching fileNames or lastModified from map: " +
                                                                         e.getMessage(),
                                                                 e);
                    }
                }).parallel().map(file -> {
                    try {
                        if (crawl(file, webClient, maxReadSize, option, enumLog)) {
                            crawledCount.getAndIncrement();
                        }
                        filesCount.getAndIncrement();
                        status.put(file, okayException);
                        return null;
                    } catch (Exception ex) {
                        status.put(file, ex);
                        return ex;
                    }
                }).takeWhile(exception -> {
                    if (exception != null) {
                        if (exception instanceof WebClientResponseException webException &&
                                webException.getStatusCode().is4xxClientError()) {
                            err().println("Unrecoverable authorization error: " + webException.getMessage());
                            return false;
                        }
                        err().println("Error processing file: " + exception.getMessage());
                    }
                    return true;
                }).toList();
            } catch (IOException e) {
                out().println("There was an exception: " + e.getMessage());
            } finally {
                out().println(filesCount.get() + " valid files found.");
                out().println("Crawled " + crawledCount.get() + " new files.");
            }
        }).join();
        customPool.shutdown();
        boolean exceptionFound = false;
        for(var entry : status.entrySet()) {
            if (entry.getValue() != okayException) {
                exceptionFound = true;
                err().println("Error processing file: " + entry.getKey() + " - " + entry.getValue());
                if (entry.getValue() instanceof WebClientResponseException webException && webException.getStatusCode().is4xxClientError()) {
                    break;
                }
            } else {
                out().println("Successfully processed file: " + entry.getKey());
            }
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
        out().println("Execution Completed in: "+ Duration.between(start, finish).toMillis() + "ms");

        if(initiateDatasetCollection) {
            out().println("Initiating dataset collection");
            //Write API service to call dataset routing
            WebClient datasetWebClient = Client.getWebClient(metaURL + "/api/dataset");
            try {
                if (metaURL == null) {
                    out().println("No hostname specified. Skipping dataset update.");
                } else {
                    var res = Client.post(datasetWebClient, "", new ParameterizedTypeReference<Integer>() {
                    }, httpHeaders -> {
                        httpHeaders.setBearerAuth(token);
                    });
                    out().println("RESULT: " + res);
                }
            } catch (WebClientRequestException ex) {
                out().println("Dataset API call: " + ex.getMostSpecificCause() + ex.getMessage());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (exceptionFound) {
            throw new CommandLine.ExecutionException(cmd(), "Error(s) occurred during processing. See above for details.", null);
        }
    }
    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new WildfireFilesCrawler());
        commandLine.setExecutionExceptionHandler((ex, cl, pr) -> {
            System.err.println(ex.getMessage());
            ex.printStackTrace();
            return 2;
        });
        commandLine.setParameterExceptionHandler((ex, as) -> {
            var t = ex.getCause() != null ? ex.getCause() : ex;
            System.err.println(t.getMessage());
            if (t instanceof CommandLine.ParameterException) {
                commandLine.usage(System.err);
            }
            return 1;
        });
        System.exit(commandLine.execute(args));
    }

    public static boolean crawl(String file, WebClient webClient, int maxReadSize, String option, boolean enumLog) throws
            InterruptedException, ExecutionException {
        NetcdfFileReader fileReader = new NetcdfFileReader(file);
        var metadata = fileReader.processFile(maxReadSize);

        if (option.equals("all")) {
            PrintData.printAllData(metadata);
        } else if (option.equals("basic")) {
            PrintData.printBasic(metadata);
        }
        if (enumLog) {
            Path enumFile = Paths.get("enumVarList.txt");
            ;
            try {
                Files.createFile(enumFile);
            } catch (FileAlreadyExistsException e) {
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            PrintData.printEnums(enumFile, metadata);
        }

        Client.delete(webClient, file);
        return (boolean) Client.post(webClient, metadata, new ParameterizedTypeReference<Boolean>() {});
    }
}
