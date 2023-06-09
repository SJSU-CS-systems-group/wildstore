package com.sjsu.wildfirestorage;

import picocli.CommandLine;

@CommandLine.Command(name = "GET", mixinStandardHelpOptions = true)
public class WildfireFilesCrawler implements Runnable {
    @CommandLine.Parameters(paramLabel = "<option>", defaultValue = "all", description = "Which information to print.")
    private String option;
    @CommandLine.Parameters(paramLabel = "<file>", description = "File to process")
    private String ncFilePath = "/Users/ysabella/Downloads/wrfout_d01_2019-06-20_18_00_00";
    // private String ncFilePath = "/Users/spartan/Applications/WildStorage/datafiles/met_em.d01.2019-06-21_09:00:00.nc";
    // private String ncFilePath = "/Users/spartan/Applications/WildStorage/datafiles/wrfout_d05_2012-09-11_00:00:0069:00:00.nc";
    public void run() {
        NetcdfFileReader fileReader = new NetcdfFileReader(ncFilePath);
        fileReader.processFile();

        if (option.equals("all")) {
            fileReader.printAllData(fileReader.meta);
        }
        else if (option.equals("basic")) {
            fileReader.printBasic(fileReader.meta);
        }
    }
    public static void main(String[] args) {
        if (args.length > 0) {
            //ncFilePath = args[0];
            System.out.println("Using NetcdfFile: " + args[1]);
        }
        System.exit(new CommandLine(new WildfireFilesCrawler()).execute(args));
    }
}
