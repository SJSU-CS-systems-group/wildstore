package com.sjsu.wildfirestorage;

public class WildfireFilesCrawler
{
    public static void main( String[] args )
    {
        String ncFilePath = "/Users/spartan/Applications/WildStorage/datafiles/met_em.d01.2019-06-21_09:00:00.nc";
        // String ncFilePath = "/Users/spartan/Applications/WildStorage/datafiles/wrfout_d05_2012-09-11_00:00:0069:00:00.nc";

        if (args.length > 0) {
            ncFilePath = args[0];
            System.out.println("Using NetcdfFile: " + args[0]);
        }

        NetcdfFileReader fileReader = new NetcdfFileReader(ncFilePath);
        fileReader.processFile();
    }
}
