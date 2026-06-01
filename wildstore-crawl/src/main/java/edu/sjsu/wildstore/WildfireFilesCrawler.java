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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@CommandLine.Command(name = "GET", mixinStandardHelpOptions = true)
public class WildfireFilesCrawler implements Runnable {
    private static final Map<String, Function<String, FileReader>> FILE_READERS = Map.of(
        ".nc",   NetcdfFileReader::new,
        ".hdf5", BasicFileReader::new,
        ".h5",   BasicFileReader::new,
        ".shp",  BasicFileReader::new,
        ".tif",  BasicFileReader::new,
        ".tiff", BasicFileReader::new,
        ".kmz",  BasicFileReader::new
    );
    @CommandLine.Option(names = "--metaURL", description = "Host name of the API server", required = true)
    String metaURL;
    @CommandLine.Option(names = "--relationalURL", description = "Host name of the permission API server", required = true)
    String relationalURL;
    @CommandLine.Option(names = "--log", description = "Whether to generate a log")
    Boolean log = false;
    @CommandLine.Option(names = "--enums", description = "Generate log of Enum Variable names")
    Boolean enumLog = false;
    @CommandLine.Option(names = "--parallelism", description = "Number of threads to use")
    int parallelism = 1;
    @CommandLine.Option(names = "--maxReadSize", description = "Number of data elements to read per read call")
    int maxReadSize = 1000000000;
    @CommandLine.Option(names = "--tokenFile", required = true)
    String tokenFile;
    @CommandLine.Option(names = "--dataset", description = "Whether to initiate dataset collection")
    boolean initiateDatasetCollection = false;
    @CommandLine.Option(names = "--option", defaultValue = "all", description = "Which information to print - 'all' " +
            "or 'basic'")
    private String option;
    @CommandLine.Parameters(paramLabel = "<file>", description = "Path to the file containing list of NetCDF files to" +
            " process", arity = "1")
    private String filesToProcessPath;
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new WildfireFilesCrawler());
        commandLine.setExecutionExceptionHandler((ex, cl, pr) -> {
            System.err.println(ex.getMessage());
            checkDebugEnv(ex);
            return 2;
        });
        commandLine.setParameterExceptionHandler((ex, as) -> {
            var t = ex.getCause() != null ? ex.getCause() : ex;
            System.err.println(t.getMessage());
            if (t instanceof CommandLine.ParameterException) {
                commandLine.usage(System.err);
            }
            checkDebugEnv(ex);
            return 1;
        });
        System.exit(commandLine.execute(args));
    }

    private static void checkDebugEnv(Exception ex) {
        if (System.getenv("DEBUG") != null) {
            ex.printStackTrace();
        }
    }

    public static boolean crawl(String file,
                                WebClient webClient,
                                WebClient fileNodeClient,
                                Map<String, Long> directoryFileNodeIdMap,
                                int maxReadSize,
                                String option,
                                boolean enumLog) throws InterruptedException, ExecutionException {
        var supportedFile = FILE_READERS.entrySet().stream()
                .filter(e -> file.endsWith(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
        if (supportedFile == null) {return false;}
        var metadata = supportedFile.apply(file).processFile(maxReadSize);

        System.out.println("Crawling file: " + file);
        if (option.equals("all")) {
            PrintData.printAllData(metadata);
        } else if (option.equals("basic")) {
            PrintData.printBasic(metadata);
        }
        if (enumLog) {
            Path enumFile = Paths.get("enumVarList.txt");
            try {
                Files.createFile(enumFile);
            } catch (FileAlreadyExistsException e) {
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            PrintData.printEnums(enumFile, metadata);
        }

        boolean posted = (boolean) Client.post(webClient, metadata, new ParameterizedTypeReference<Boolean>() {});
        if (posted) {
            postFileNodes(file, metadata.fileSize, metadata.digestString, directoryFileNodeIdMap, fileNodeClient);
        }
        return posted;
    }

    final PrintWriter out() {
        return cmd().getOut();
    }

    final PrintWriter err() {
        return cmd().getErr();
    }

    final CommandLine cmd() {
        return spec.commandLine();
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
        WebClient fileNodeClient = Client.getWebClient(relationalURL + "/file_contents");
        Map<String, Long> directoryFileNodeIdMap = new ConcurrentHashMap<>();
        AtomicInteger filesCount = new AtomicInteger();
        AtomicInteger crawledCount = new AtomicInteger();
        AtomicInteger skippedCount = new AtomicInteger();

        List<Map<String, Object>> fileNames = new ArrayList<>();
        List<Map<String, Object>> newNames;
        var limit = 10000;
        var offset = 0;
        try {
            do {
                LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
                parameters.add("limit", String.valueOf(limit));
                parameters.add("offset", String.valueOf(offset));
                newNames = Client.get(Client.getWebClient(metaURL + "/api/metadata/filenames", token),
                                      parameters,
                                      new ParameterizedTypeReference<>() {});
                fileNames.addAll(newNames);
                offset += limit;
            } while (newNames.size() == limit);
        } catch (Exception e) {
            err().println("Error fetching file names from metadata service: " + e.getClass().getName());
            if (e instanceof WebClientResponseException webException &&
                    webException.getStatusCode().is4xxClientError()) {
                err().println("Unrecoverable authorization error: " + webException.getMessage());
            }
            throw new CommandLine.ExecutionException(cmd(),
                                                     "Error fetching file names from metadata service: " +
                                                             e.getMessage(),
                                                     e);
        }

        Map<String, Long> fileNamesMap = fileNames.stream()
                .filter(map -> map.get("fileName") instanceof String)
                .collect(Collectors.toMap(map -> map.get("fileName").toString(),
                                          map -> (Long) map.get("lastModified"),
                                          Long::min));

        ForkJoinPool customPool = new ForkJoinPool(parallelism); // set desired parallelism

        var poolResult = customPool.submit(() -> {
            try (Stream<String> stream = Files.lines(Paths.get(filesToProcessPath))) {
                var exceptions = stream.map(fileName -> {
                    File file = new File(fileName);
                    if (file.isDirectory()) {
                        return StreamSupport.stream(new FileSpliterator(List.of(file.toPath())), true)
                                .map(Path::toString);
                    } else {
                        return Stream.of(fileName);
                    }
                }).reduce(Stream::concat).orElse(Stream.empty()).parallel().filter(file -> {
                    try {
                        if (fileNamesMap.containsKey(file) &&
                                fileNamesMap.get(file) >= Files.getLastModifiedTime(Paths.get(file)).toMillis()) {
                            skippedCount.getAndIncrement();
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
                }).map(file -> {
                    try {
                        if (crawl(file, webClient, fileNodeClient, directoryFileNodeIdMap, maxReadSize, option, enumLog)) {
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
                        err().println("Error processing file: " + exception + " - " + exception.getClass().getName());
                    }
                    return true;
                }).toList();
            } catch (IOException e) {
                out().println("There was an exception: " + e.getMessage());
            } finally {
                out().println(filesCount.get() + " valid files found.");
                out().println("Crawled " + crawledCount.get() + " new files.");
                out().println("Skipped " + skippedCount.get() + " files already crawled.");
            }
        }).join();
        customPool.shutdown();
        boolean exceptionFound = false;
        for (var entry : status.entrySet()) {
            if (entry.getValue() != okayException) {
                exceptionFound = true;
                err().println("Error processing file: " + entry.getKey() + " - " + entry.getValue());
                if (entry.getValue() instanceof WebClientResponseException webException &&
                        webException.getStatusCode().is4xxClientError()) {
                    break;
                }
            } else {
                out().println("Successfully processed file: " + entry.getKey());
            }
        }
        if (log) {
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
        out().println("Execution Completed in: " + Duration.between(start, finish).toMillis() + "ms");

        if (initiateDatasetCollection) {
            out().println("Initiating dataset collection");
            //Write API service to call dataset routing
            WebClient datasetWebClient = Client.getWebClient(metaURL + "/api/dataset");
            try {
                if (metaURL == null) {
                    out().println("No hostname specified. Skipping dataset update.");
                } else {
                    var res = Client.post(datasetWebClient,
                                          "",
                                          new ParameterizedTypeReference<Integer>() {},
                                          httpHeaders -> {
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
            throw new CommandLine.ExecutionException(cmd(),
                                                     "Error(s) occurred during processing. See above for details.",
                                                     null);
        }
    }

    private static Long postFileNodes(String fileName, Long fileSize, String digest, Map<String, Long> directoryFileNodeIdMap, WebClient fileNodeClient) throws InterruptedException, ExecutionException {
        if (fileName == null || fileName.equals(System.getenv("ROOT_NODE_DIRECTORY")) || fileName.equals("null")) {
          return null;
        }
        Long fileNodeId = directoryFileNodeIdMap.get(fileName);
        if (fileNodeId != null) {
          return fileNodeId;
        }
        Path filePath = Paths.get(fileName);
        Path parentPath = filePath.getParent();

        Long parentId = postFileNodes(String.valueOf(parentPath), null, null, directoryFileNodeIdMap, fileNodeClient);

        File file = new File(fileName);
                    
        FileType fileType = FileType.FILE;
        if (file.isDirectory()) {
          fileType = FileType.DIRECTORY;
        } 

        FileNodeParams fileNodeParams = new FileNodeParams();
        fileNodeParams.fileName = filePath.toString();
        fileNodeParams.size = fileSize;
        fileNodeParams.fileType = fileType;
        fileNodeParams.parentId = parentId;
        fileNodeParams.digest = digest;

        fileNodeId = (Long) Client.post(fileNodeClient, fileNodeParams, new ParameterizedTypeReference<Long>() {});
        directoryFileNodeIdMap.put(fileName, fileNodeId);
        return fileNodeId;
    }

    /**
     * this is our own file walker that will skip files that are not accessible, unlike Files.walk().
     * we use a Spliterator to allow parallel processing of files.
     */
    private static class FileSpliterator extends java.util.Spliterators.AbstractSpliterator<Path> {
        private static final long POPULATE_QUEUE_SIZE = 1000;
        // all FileSpliterator instances share the same queue of directories to walk
        private final ConcurrentLinkedQueue<Path> dirQueue;
        // each instance has its own queue of files to process
        private final LinkedList<Path> fileQueue = new LinkedList<>();

        FileSpliterator(List<Path> pathsToWalk) {
            super(Long.MAX_VALUE, java.util.Spliterator.NONNULL);
            // the parent creates the queue that everyone else will share
            this.dirQueue = new ConcurrentLinkedQueue<>();
            this.dirQueue.addAll(pathsToWalk);
            populateFileQueue();
        }

        private FileSpliterator(ConcurrentLinkedQueue<Path> dirQueue) {
            super(Long.MAX_VALUE, java.util.Spliterator.NONNULL);
            this.dirQueue = dirQueue;
        }

        private long populateFileQueue() {
            long count = 0;
            // try to get 1000 files into the fileQueue
            while (count < POPULATE_QUEUE_SIZE && !dirQueue.isEmpty()) {
                Path dir = dirQueue.poll();
                if (dir == null) continue;
                try (var stream = Files.list(dir)) {
                    count += stream.filter(p -> {
                        // we don't really need the symlink check here, but it makes it clear
                        // that we are not following symlinks and in the future if another
                        // check is added, it will not follow symlinks
                        if (Files.isSymbolicLink(p)) {
                            // symbolic links can take us into cycles
                            return false;
                        } else if (Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)) {
                            dirQueue.add(p);
                            return false;
                        } else if (Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS) &&
                                Files.isReadable(p) &&
                                FILE_READERS.keySet().stream().anyMatch(p.toString()::endsWith)) {
                            fileQueue.add(p);
                            return true;
                        } else {
                            return false;
                        }
                    }).count();
                } catch (IOException e) {
                    System.err.println("Error listing directory: " + dir + " - " + e.getClass().getName());
                }
            }
            return count;
        }

        @Override
        public synchronized boolean tryAdvance(java.util.function.Consumer<? super Path> action) {
            var path = fileQueue.pollFirst();
            if (path != null) {
                action.accept(path);
                return true;
            }
            while (true) {
                populateFileQueue();
                path = fileQueue.pollFirst();
                if (path == null && dirQueue.isEmpty()) return false;
                if (path != null) {
                    action.accept(path);
                    return true;
                }
            }
        }

        @Override
        public synchronized FileSpliterator trySplit() {
            var newSpliterator = new FileSpliterator(this.dirQueue);
            if (newSpliterator.populateFileQueue() == 0) {
                return null; // No files to split
            }
            return newSpliterator;
        }
    }
}
