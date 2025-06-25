package edu.sjsu.wildstore;

import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.BiFunction;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
@CommandLine.Command
public class TestDataDuplicator implements Runnable {
    @CommandLine.Parameters
    File fileToDuplicate;

    @CommandLine.Option(names="--count", description="Number of times to duplicate the file", defaultValue = "100",
    showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    int count;
    public final static int MAX_COUNT = 24*60; // 24 hours in minutes

    @Override
    public void run() {
        if (count < 1 || count > MAX_COUNT) {
            System.err.printf("Count must be between 1 and %s%n", MAX_COUNT);
            System.exit(2);
            return;
        }
        try {
            duplicateFiles(fileToDuplicate, count, (file, i) -> {
                    var name = file.getPath();
                    var extIndex = name.lastIndexOf('.');
                    String newName = String.format("%s-%d%s", name.substring(0, extIndex), i, name.substring(extIndex));
                    System.out.printf("Creating file: %s%n", newName);
                    return new File(newName);
            });
        } catch (IOException e) {
            System.err.println("Error reading or writing the file: " + e.getMessage());
            System.exit(2);
        }
    }

    public static void duplicateFiles(File fileToDuplicate, int count, BiFunction<File, Integer, File> getNewFileName) throws IOException {
        var data = Files.readAllBytes(fileToDuplicate.toPath());
        int timeOffset = findTimeOffset(data);
        if (timeOffset == -1) {
            throw new IOException("Time offset not found in the file. Please ensure the file contains a time in HH:MM format.");
        }
        for (int i = 0; i < count; i++) {
            // create a new file with the same name but with a number appended
            var hours = String.format("%02d", i / 60);
            var mins = String.format("%02d", i % 60);
            System.arraycopy(hours.getBytes(), 0, data, timeOffset, 2);
            // skip over the colon
            System.arraycopy(mins.getBytes(), 0, data, timeOffset+3, 2);
            var newFileName = getNewFileName.apply(fileToDuplicate, i);
            Files.write(newFileName.toPath(), data);
        }
    }
    private static int findTimeOffset(byte[] data) {
        // quick and dirty way to find the time offset in the data
        // we are looking for the form HH:MM
        for (int i = 0; i < data.length - 5; i++) {
            if (Character.isDigit(data[i]) && Character.isDigit(data[i + 1]) && data[i + 2] == ':' && Character.isDigit(data[i + 3])
                    && Character.isDigit(data[i+4])) {
                return i;
            }
        }
        return -1; // not found
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new TestDataDuplicator()).execute(args));
    }
}