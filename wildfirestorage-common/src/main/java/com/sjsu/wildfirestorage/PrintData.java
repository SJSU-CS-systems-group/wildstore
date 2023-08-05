package com.sjsu.wildfirestorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

public class PrintData {
    public static void printAllData(Metadata metadata)
    {
        //Print Name and filepath
        System.out.println("Filename: " + metadata.fileName);
        System.out.println("FilePath: " + metadata.filePath);
        System.out.println("FileType: " + metadata.fileType);
        System.out.println("Domain: " + metadata.domain);
        System.out.println("Digest String: " + metadata.digestString);
        if(metadata.location != null)
            System.out.println("Corners: " + metadata.location.toString());
        else
            System.out.println("Corners: Null");

        //Print All Attributes
        System.out.println("\nAttributes");
        for (WildfireAttribute a : metadata.globalAttributes)
        {
            System.out.print(a.attributeName + "\t" + a.type + "\t");
            System.out.print(a.value);
            System.out.print("\n");
        }

        //Print All Variables
        System.out.println("\nVariables");
        for (WildfireVariable v : metadata.variables)
        {
            System.out.println(v.variableName + "\t" + v.type + "\t" + v.minValue +"\t" + v.maxValue + "\t" + v.average);
            System.out.println("Unique Elements (HashMap): " + v.elementMap);
            for (WildfireAttribute a : v.attributeList)
            {
                System.out.print(a.attributeName + "\t" + a.type + "\t");
                System.out.print(a.value);
            }
            System.out.print("\n");
        }
    }

    public static void printBasic(Metadata metadata) {
        System.out.println("Filename: " + metadata.fileName);
        System.out.println("FilePath: " + metadata.filePath);
        System.out.println("FileType: " + metadata.fileType);
        System.out.println("Domain: " + metadata.domain);
        if(metadata.location != null)
            System.out.println("Corners: " + metadata.location.toString());
        else
            System.out.println("Corners: Null");

        //Attributes
        System.out.println("\nAttributes:");
        for (WildfireAttribute a : metadata.globalAttributes)
        {
            System.out.print(a.attributeName + "\t");
            System.out.print(a.value);
            System.out.print("\n");
        }

        //Variables
        System.out.println("\nVariables:");
        for (WildfireVariable v : metadata.variables)
        {
            System.out.print(v.variableName + "\t" + v.average + "\t" + v.elementMap);
            for (WildfireAttribute a : v.attributeList)
            {
                if (a.attributeName.equals("units")) {
                    System.out.print(a.value);
                }
            }
            System.out.print("\n");
        }
    }

    public static void printEnums(Path enumFile, Metadata metadata) {

        List<String> enumVars;

        try {
            enumVars = Files.readAllLines(enumFile);
        } catch (IOException e) {
            System.out.println("Failed to read from Enum Variable Files");
            throw new RuntimeException(e);
        }
        try {
            for (WildfireVariable v : metadata.variables)
            {
                if (v.elementMap != null && v.elementMap.size() != 0 && !enumVars.contains(v.variableName)) {
                    Files.writeString(enumFile, v.variableName+"\n", StandardOpenOption.APPEND);
                }
            }
        } catch (IOException e) {
            System.out.println("Failed to write to EnumVariable Files");
            throw new RuntimeException(e);
        }
    }
}
