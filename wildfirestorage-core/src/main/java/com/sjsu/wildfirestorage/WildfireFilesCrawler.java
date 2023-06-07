package com.sjsu.wildfirestorage;

import picocli.CommandLine;

public class WildfireFilesCrawler {
    public static void main( String[] args ) {
        CrawlerArgs crawlerArgs = new CrawlerArgs();
        new CommandLine(crawlerArgs).parseArgs(args);

        System.out.println("Using NetcdfFile: " + crawlerArgs.ncFilePath);

        NetcdfFileReader fileReader = new NetcdfFileReader(crawlerArgs.ncFilePath);
        Metadata metadata = fileReader.processFile();
        Client.post(crawlerArgs.hostname + "/api/metadata", metadata);
        System.out.println("End of main");
    }

    static class CrawlerArgs {
        @CommandLine.Option(names = "--hostname", description = "Host name of the API server")
        String hostname = "http://localhost:8080";

        @CommandLine.Option(names = "--ncFilePath", description = "Path of the file to be crawled", required = true)
//        String ncFilePath = "/Users/spartan/Applications/WildStorage/datafiles/met_em.d01.2019-06-21_09:00:00.nc";
        String ncFilePath = "C:\\Users\\apoor\\wildfire\\wrfout.nc";
    }
}
