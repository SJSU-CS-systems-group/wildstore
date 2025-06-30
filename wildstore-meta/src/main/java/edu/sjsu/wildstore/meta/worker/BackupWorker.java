package edu.sjsu.wildstore.meta.worker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class BackupWorker {

    String backupDirectory;
    String mongoUri;

    private static final Logger logger = Logger.getLogger(BackupWorker.class.getName());

    public BackupWorker(@Value("${wildstore.backupDirectory:#{null}}") String backupDirectory,
                        @Value("${spring.data.mongodb.uri:#{null}}") String mongoUri){
        this.backupDirectory = backupDirectory;
        this.mongoUri = mongoUri;
    }

    @Scheduled(fixedRateString = "#{'${backup.interval:86400000}'}") // Default to 24 hours if not specified
    public void backup() throws IOException, InterruptedException {
        if (backupDirectory == null) {
            logger.warning("❗Backup directory is not specified. Skipping backup.");
            return;
        }
        logger.log(Level.INFO, "ℹ️Starting backup to directory: " + backupDirectory);
        backup("userData");
        backup("share-links");
    }

    private void backup(String collection) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(
                "mongoexport",
                "--uri", mongoUri,
                "--collection", "collection",
                "--out", backupDirectory + "/" + collection + ".json");
        builder.redirectErrorStream(true);
        Process process = builder.start();
        int exitCode = process.waitFor();
        if (exitCode == 0) {
            logger.log(Level.INFO, "✅" + collection + " collection backed up to: " + backupDirectory);
        } else {
            logger.warning("❗Failed to backup " + collection + " collection. Exit code: " + exitCode);
        }
    }
}