package com.sjsu.wildfirestorage;

public class WildfireFilesCrawler
{
    public static void main( String[] args )
    {
        // Get file name
        String ncFilePath = "/Users/spartan/Applications/WildStorage/datafiles/met_em.d01.2019-06-21_09:00:00.nc";
        // String ncFilePath = "/Users/spartan/Applications/WildStorage/datafiles/wrfout_d05_2012-09-11_00:00:0069:00:00.nc";

        NetcdfFileReader fileReader = new NetcdfFileReader(ncFilePath);
        fileReader.processFile();

        System.out.println("End of main");
    }
}
