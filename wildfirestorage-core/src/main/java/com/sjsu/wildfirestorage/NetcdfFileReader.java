package com.sjsu.wildfirestorage;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NetcdfFileReader {
    private final String netcdfFilepath;

    private NetcdfFile netcdfFile;

    private List<ProcessedAttribute> processedAttributes;

    private List<ProcessedVariable> processedVariables;

    public NetcdfFileReader(String netcdfFilepath) {
        this.netcdfFilepath = netcdfFilepath;
    }

    public void processFile() {
        System.out.println("Processing file: " + netcdfFilepath);

        // Try and read the contents of the netCDF file
        try {
            this.netcdfFile = NetcdfFile.open(this.netcdfFilepath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // @Todo: Try to read the date from the file metadata
        // System.out.println(this.netcdfFile.getDetailInfo());

        readGlobalAttributes();

        readVariables();
    }

    public void readGlobalAttributes() {
        List<Attribute> attributes = this.netcdfFile.getGlobalAttributes();
        System.out.println("Global attributes are: " + attributes);

        this.processedAttributes = processAttributes(attributes);
        System.out.println(this.processedAttributes);   // Logging for debug purposes
    }

    // Record to store processed attributes
    record ProcessedAttribute(String fullname, Array value, DataType dataType) {}

    public void readVariables() {
        List<Variable> variables = netcdfFile.getVariables();
        System.out.println("Variables from file: " + variables);

        List<ProcessedVariable> processedVariables = new ArrayList<>();
        for (Variable variable : variables) {

            String variableName = variable.getFullName() != null ? variable.getFullName() : variable.getShortName();

            // Read dimensions of variable
            List<ProcessedVariable.VarDimension> varDimensions = new ArrayList<>();
            for (Dimension dimension : variable.getDimensions()) {
                String dimensionName = dimension.getFullName() != null ? dimension.getFullName() : dimension.getShortName();
                varDimensions.add(new ProcessedVariable.VarDimension(dimensionName, dimension.getLength()));
            }

            List<ProcessedAttribute> attributes = processAttributes(variable.getAttributes());

            Array data = null;
            // Read data from the variable
            try {
                data = variable.read();
            } catch (IOException e) {
                System.out.println("Failed to read data for variable: " + variableName);
                continue;
            }

            processedVariables.add(new ProcessedVariable(variableName, varDimensions, attributes, variable.getDataType(), data));
        }
        this.processedVariables = processedVariables;
    }

    /**
     * This method processes attributes found in the NetcdfFile and returns a list of {@link ProcessedAttribute}
     */
    private List<ProcessedAttribute> processAttributes(List<Attribute> attributes) {
        List<ProcessedAttribute> attributeList = new ArrayList<>();
        for (Attribute attribute : attributes) {
            // @Todo: If data is byte[], convert it to string
            attributeList.add(new ProcessedAttribute(attribute.getFullName(), attribute.getValues(), attribute.getDataType()));
        }

        this.processedAttributes = attributeList;
        return attributeList;
    }

    record ProcessedVariable(String name, List<ProcessedVariable.VarDimension> dimensions, List<ProcessedAttribute> attributes, DataType dataType, Array data) {
        record VarDimension(String name, int value) {}
    }
}
