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
    // private String ncFilePath = "/Users/spartan/Applications/WildStorage/datafiles/met_em.d01.2019-06-21_09:00:00.nc";
    // private String ncFilePath = "/Users/spartan/Applications/WildStorage/datafiles/wrfout_d05_2012-09-11_00:00:0069:00:00.nc";
    @CommandLine.Option(names = "--hostname", description = "Host name of the API server")
    String hostname;

    public void run() {
        NetcdfFileReader fileReader = new NetcdfFileReader(ncFilePath);
        var metadata = fileReader.processFile();

        if (option.equals("all")) {
            fileReader.printAllData(metadata);
        }
        else if (option.equals("basic")) {
            fileReader.printBasic(metadata);
        }
        if (hostname == null) {
            System.out.println("No hostname specified. Skipping metadata update.");
        } else {
            Client.post(hostname + "/api/metadata", metadata, new ParameterizedTypeReference<Integer>(){});
        }
    }
    public static void main(String[] args) {
        System.exit(new CommandLine(new WildfireFilesCrawler()).execute(args));
    }
}
