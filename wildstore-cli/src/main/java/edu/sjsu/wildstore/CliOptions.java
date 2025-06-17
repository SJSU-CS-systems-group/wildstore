package edu.sjsu.wildstore;

import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

class CliOptions {
    @CommandLine.Option(names = "--token", description = "Authentication token file", required = true)
    void setTokenFile(File tokenFile) {
        try (var br = new BufferedReader(new FileReader(tokenFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("token=")) {
                    token = line.substring(6).trim();
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        if (token == null) {
            throw new CommandLine.PicocliException("No token found in " + tokenFile);
        }
    }

    @CommandLine.Option(names = "--metaURL", description = "URL of the metadata server", showDefaultValue =
            CommandLine.Help.Visibility.ALWAYS, defaultValue = "http://localhost:27777")
    void setMetadataURL(String metadataURL) {
        this.metadataURL = metadataURL;
        try {
            new URL(metadataURL);
        } catch (MalformedURLException e) {
            System.out.printf("%s%n", e.getMessage());
            System.exit(2);
        }
        if (this.metadataURL.endsWith("/")) {
            this.metadataURL = this.metadataURL.substring(0, this.metadataURL.length() - 1);
        }
    }

    String token;
    String metadataURL;
}
