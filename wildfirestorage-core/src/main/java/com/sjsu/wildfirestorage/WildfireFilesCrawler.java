package com.sjsu.wildfirestorage;

public class WildfireFilesCrawler
{
    public static void main( String[] args )
    {
        // Get file name
//        String ncFilePath = "/Users/alow/Desktop/CSProjects/WildStorage/wrfout_d04_2018-11-14_20:00:00";
         String ncFilePath = "/Users/alow/Desktop/CSProjects/WildStorage/wrfout_d01_2019-06-20_18:00:00";

        if (args.length > 0) {
            ncFilePath = args[0];
            System.out.println("Using NetcdfFile: " + args[0]);
        }

        NetcdfFileReader fileReader = new NetcdfFileReader(ncFilePath);
        fileReader.processFile();
        System.out.println("End of main");
    }
}
