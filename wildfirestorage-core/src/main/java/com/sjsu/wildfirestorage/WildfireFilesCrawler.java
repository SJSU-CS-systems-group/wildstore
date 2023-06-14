package com.sjsu.wildfirestorage;

import org.springframework.core.ParameterizedTypeReference;
import picocli.CommandLine;

import java.util.ArrayList;

@CommandLine.Command(name = "GET", mixinStandardHelpOptions = true)
public class WildfireFilesCrawler implements Runnable {
    @CommandLine.Parameters(paramLabel = "<option>", defaultValue = "all", description = "Which information to print - 'all' or 'basic'")
    private String option;
    @CommandLine.Parameters(paramLabel = "<file>", description = "File to process")
    private String ncFilePath = "/Users/ysabella/Downloads/wrfout_d01_2019-06-20_18_00_00";
    @CommandLine.Option(names = "--hostname", description = "Host name of the API server")
    String hostname;

    public void run() {
        NetcdfFileReader fileReader = new NetcdfFileReader(ncFilePath);
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
            System.out.println("POST response: " + Client.post(hostname + "/api/metadata", metadata, new ParameterizedTypeReference<Integer>(){}));
        }
    }
    public static void main(String[] args) {
        System.exit(new CommandLine(new WildfireFilesCrawler()).execute(args));
    }
}
