package com.sjsu.wildfirestorage;

import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;

public class WildfireFilesCrawler
{
    public static void main( String[] args )
    {
        String ncFilePath = "/Users/spartan/Applications/WildStorage/datafiles/met_em.d01.2019-06-21_09:00:00.nc";

        NetcdfFile ncFile = null;
        try {
            ncFile = NetcdfFile.open(ncFilePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Variable var = ncFile.findVariable("PRES");
        List<Attribute> attributes = ncFile.getGlobalAttributes();
        System.out.println(attributes);
    }
}
