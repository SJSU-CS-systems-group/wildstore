package com.sjsu.wildfirestorage;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mongodb.client.model.geojson.Position;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.PolygonCoordinates;

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
//         System.out.println(this.netcdfFile.getDetailInfo());

        readGlobalAttributes();

        readVariables();

//        double[] winds = findWind(netcdfFile); //Need to first check if U and V are in file

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

    /**
     * Calculates the windspeed from the U and V vectors, returns an array of 8 directions in order of ("North",
     * "North East", "East", "South East", "South", "South West", "West", "North West") with 3 values each of min,
     * max, avg
     * @param ncfile NetCDFFile being processes (@Todo can be changed to send Variables and Stagger Dimensions)
     * @return double[] Array of 8 directions min, max, avg speeds
     */
    private static double[] findWind(NetcdfFile ncfile) {
        //Wind
        Variable u = ncfile.findVariable("U");
        Variable v = ncfile.findVariable("V");

        Array uData = null;
        Array vData = null;
        try {
            uData = u.read();
            vData = v.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Get the shape of the arrays
        int west_east_stag = ncfile.findDimension("west_east_stag").getLength();
        int south_north_stag = ncfile.findDimension("south_north_stag").getLength();
        int west_east_dim = west_east_stag -1;
        int south_north_dim = south_north_stag -1;

        //Stored Data
        double [] windSpeedMax = new double[8];
        double [] windSpeedMin = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE,
                Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        double [] windSpeedSum = new double[8];
        double [] windSpeedCount = new double[8];
        double [] windSpeedAvg = new double[8];
        String [] stringWindDir = {"North", "North East", "East", "South East", "South", "South West", "West", "North West"};

        int[] shape = uData.getShape();
        shape[shape.length-1]--; //Go from stag to unstag

        //Calculate size of the whole area from dimension
        int arraySize = 1;
        for (int dimLength : shape) {
            arraySize *= dimLength;
        }

        int uIndex = 0;
        int vIndex = 0;
        for (int i = 0; i < arraySize; i++)
        {
//				System.out.print(i + "\t" +uIndex + "\t" + vIndex + "\t" + uData.getFloat(uIndex) + "\t"+vData.getFloat(vIndex) +"\t");
            double uUnstag = 0.5 * (uData.getFloat(uIndex) + uData.getFloat(uIndex+1));
            double vUnstag = 0.5 * (vData.getFloat(vIndex) + vData.getFloat(vIndex + west_east_dim));

            double windSpeed = Math.sqrt(uUnstag * uUnstag + vUnstag * vUnstag);
            double windDir = (270 - (Math.atan2(uUnstag, vUnstag) * 180 / Math.PI)) % 360;
            int windDirIndex = (int) ((22.5 + windDir) % 360 / 45);

            windSpeedMax[windDirIndex] = Math.max(windSpeedMax[windDirIndex], windSpeed);
            windSpeedMin[windDirIndex] = Math.min(windSpeedMin[windDirIndex], windSpeed);

            windSpeedSum[windDirIndex] += windSpeed;
            windSpeedCount[windDirIndex]++;

            uIndex++;
            vIndex++;

            if((i+1) % (west_east_dim)== 0) //Skip the last element of each row
                uIndex++;
            if((i+1) % (west_east_dim*south_north_dim)== 0) //Skip the last row of each rectangle
                vIndex+=west_east_dim;

//				System.out.println(windSpeed + "\t" + uUnstag + "\t" + vUnstag);
        }
        double[] windSpeeds = new double[24];
        for (int i = 0; i < 8; i++)
        {
            windSpeedAvg[i] = windSpeedSum[i]/windSpeedCount[i];

            windSpeeds[i*3] = windSpeedMin[i];
            windSpeeds[i*3+1] = windSpeedMax[i];
            windSpeeds[i*3+2] = windSpeedAvg[i];
//            System.out.printf("%10s\t%3.5f\t%3.5f\t%3.5f\n",stringWindDir[i], windSpeedMin[i], windSpeedMax[i], windSpeedAvg[i]);
        }
        return windSpeeds;
    }

    /**
     * Calculates the start and finish date based on the Times Variable. Returns a 2-element array of Dates with
     * first element being start date and second element being finish date
     * @param time Variable Time from NetCDFFile
     * @return Date[] Array of 2 element (Start Date, Finish Date)
     */
    private static Date[] findTime(Variable time)
    {

        int[] dim = time.getShape();
        Array uData;
        try
        {
            uData = time.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String firstTime = uData.slice(0, 0).toString();
        String lastTime = uData.slice(0, dim[0]-1).toString();

        Pattern pattern = Pattern.compile("(\\d\\d\\d\\d)-(\\d\\d)-(\\d\\d)_(\\d\\d):(\\d\\d):(\\d\\d)");

        Matcher time1Matcher = pattern.matcher(firstTime);
        Matcher time2Matcher = pattern.matcher(lastTime);

        int time1Year=0, time1Month=0, time1Day = 0, time1Hour=0, time1Minute=0, time1Second=0;
        int time2Year=0, time2Month=0, time2Day=0, time2Hour=0, time2Minute=0, time2Second=0;

        try
        {
            if(time1Matcher.find()) {
                time1Year = Integer.parseInt(time1Matcher.group(1));
                time1Month = Integer.parseInt(time1Matcher.group(2));
                time1Day = Integer.parseInt(time1Matcher.group(3));
                time1Hour = Integer.parseInt(time1Matcher.group(4));
                time1Minute = Integer.parseInt(time1Matcher.group(5));
                time1Second = Integer.parseInt(time1Matcher.group(6));
            }

            if(time2Matcher.find()) {
                time2Year = Integer.parseInt(time2Matcher.group(1));
                time2Month = Integer.parseInt(time2Matcher.group(2));
                time2Day = Integer.parseInt(time2Matcher.group(3));
                time2Hour = Integer.parseInt(time2Matcher.group(4));
                time2Minute = Integer.parseInt(time2Matcher.group(5));
                time2Second = Integer.parseInt(time2Matcher.group(6));
            }
        }
        catch (NumberFormatException e) {
        }


        Date date1 = new Calendar.Builder().setDate(time1Year, time1Month, time1Day)
                .setTimeOfDay(time1Hour, time1Minute, time1Second,0).build().getTime();

        Date date2 = new Calendar.Builder().setDate(time2Year, time2Month, time2Day)
                .setTimeOfDay(time2Hour, time2Minute, time2Second,0).build().getTime();

        return new Date[]{date1, date2};
    }

    /**
     * Calculates a Polygon object from the corners of the minimum and maximum XLAT, XLONG variables.
     * @param latMin Latitude Minimum
     * @param latMax Latitude Maximum
     * @param lonMin Longitude Minimum
     * @param lonMax Longitude Maximum
     * @return Polygon GeoJSON Object of corners (@Todo: Can be changed to Feature)
     */
    private Polygon calculateCorners(float latMin, float latMax, float lonMin, float lonMax)
    {
        Position topLeft = new Position(latMin, lonMin);
        Position topRight = new Position(latMin, lonMax);
        Position botRight = new Position(latMax, lonMax);
        Position botLeft = new Position(latMin, lonMax);

        Position[] positions = {topLeft, topRight, botRight, botLeft, topLeft};
        PolygonCoordinates corners = new PolygonCoordinates(Arrays.asList(positions));

        return new Polygon(corners);
    }
}
