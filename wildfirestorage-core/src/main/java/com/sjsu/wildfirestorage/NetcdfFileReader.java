package com.sjsu.wildfirestorage;

import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.PolygonCoordinates;
import com.mongodb.client.model.geojson.Position;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetcdfFileReader {
    private final String netcdfFilepath;

    private NetcdfFile netcdfFile;

    public NetcdfFileReader(String netcdfFilepath) {
        this.netcdfFilepath = netcdfFilepath;
    }

    public void processFile() {
        // Try and read the contents of the netCDF file
        try {
            this.netcdfFile = NetcdfFile.open(this.netcdfFilepath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // @Todo: Try to read the date from the file metadata

        Metadata metadata = new Metadata();
        metadata.fileName = netcdfFilepath.substring(netcdfFilepath.lastIndexOf('/')+1);
        metadata.filePath = netcdfFilepath;

        metadata.globalAttributes = readGlobalAttributes();

        metadata.variables = readVariables();

        //Special processing @Todo: Find a more efficient way to do this
        //Wind
        Variable U = netcdfFile.findVariable("U");
        Variable V = netcdfFile.findVariable("V");
        if (U != null && V != null) {
            int west_east_stag = netcdfFile.findDimension("west_east_stag").getLength();
            int south_north_stag = netcdfFile.findDimension("south_north_stag").getLength();

            metadata.variables.addAll(findWind(U, V, west_east_stag, south_north_stag));
        }
        //Time
        Variable times = netcdfFile.findVariable("Times");
        if (times != null) {
            metadata.globalAttributes.addAll(findTime(times));
        }
        //Corners
        Variable xlat = netcdfFile.findVariable("XLAT");
        Variable xlong = netcdfFile.findVariable("XLONG");
        if (xlat != null && xlong != null) {
            float xMin = 0, xMax = 0, yMin = 0, yMax = 0;
            for (WildfireVariable v : metadata.variables) {
                if (v.variableName.equals("XLAT")) {
                    xMin = v.minValue;
                    xMax = v.maxValue;
                    break;
                }
            }
            for (WildfireVariable v : metadata.variables) {
                if (v.variableName.equals("XLONG")) {
                    yMin = v.minValue;
                    yMax = v.maxValue;
                    break;
                }
            }
            metadata.globalAttributes.add(calculateCorners(xMin, xMax, yMin, yMax));
        }

//        printAllData(metadata);
    }
    public void printAllData(Metadata metadata)
    {
        //Print Name and filepath
        System.out.println("Filename: " + metadata.fileName);
        System.out.println("FilePath: " + metadata.filePath);

        //Print All Attributes
        System.out.println("\nAttributes");
        for (WildfireAttribute a : metadata.globalAttributes)
        {
            System.out.print(a.attributeName + "\t" + a.type + "\t");
            if (a.type.equalsIgnoreCase("int"))
                System.out.println(Arrays.toString((int[]) a.value));
            else if (a.type.equalsIgnoreCase("float"))
                System.out.println(Arrays.toString((float[]) a.value));
            else if (a.type.equalsIgnoreCase("string"))
                System.out.println(Arrays.toString((Object[]) a.value));
        }

        //Print All Variables
        System.out.println("\nVariables");
        for (WildfireVariable v : metadata.variables)
        {
            System.out.println(v.variableName + "\t" + v.type + "\t" + v.minValue +"\t" + v.maxValue + "\t" + v.average);
            for (WildfireAttribute a : v.attributeList)
            {
                System.out.print(a.attributeName + "\t" + a.type + "\t");
                if (a.type.equalsIgnoreCase("int"))
                    System.out.println(Arrays.toString((int[]) a.value));
                else if (a.type.equalsIgnoreCase("float"))
                    System.out.println(Arrays.toString((float[]) a.value));
                else
                    System.out.println(Arrays.toString((Object[]) a.value));
            }
            System.out.println();
        }
    }

    public List<WildfireAttribute> readGlobalAttributes() {

        List<Attribute> attributes = this.netcdfFile.getGlobalAttributes();

        return processAttributes(attributes);
    }

    // Record to store processed attributes
    public List<WildfireVariable> readVariables() {
        List<Variable> variables = netcdfFile.getVariables();
//        System.out.println("Variables from file: " + variables);

        //@Todo: Need to configure for special variables and attributes
        List<WildfireVariable> processedVariables = new ArrayList<>();
        for (Variable variable : variables) {

            String varName = variable.getFullName() != null ? variable.getFullName() : variable.getShortName();

            // Read dimensions of variable
            List<WildfireVariable.VarDimension> varDimensions = new ArrayList<>();
            for (Dimension dimension : variable.getDimensions()) {
                String dimensionName = dimension.getFullName() != null ? dimension.getFullName() : dimension.getShortName();
                varDimensions.add(new WildfireVariable.VarDimension(dimensionName, dimension.getLength()));
            }

            List <WildfireAttribute> attrList = processAttributes(variable.getAttributes());

            DataType variableType = variable.getDataType();

            WildfireVariable tempVar = new WildfireVariable();

            Array data = null;
            float[] stats; //stats = [min, max, avg]
            float fillValue = variable.findAttributeIgnoreCase("_fillvalue") != null ? (float) variable.findAttribute("_FillValue").getNumericValue() : Float.MAX_VALUE;
            float missingValue = variable.findAttributeIgnoreCase("missing_value") != null ? (float) variable.findAttribute("missing_value").getNumericValue() : Float.MAX_VALUE;
            // Read data from the variable
            try {
                data = variable.read();
                stats = floatRange(data, fillValue, missingValue); //@Todo: Need to adjust for chars

                tempVar.minValue = stats[0];
                tempVar.maxValue = stats[1];
                tempVar.average = stats[2];
            } catch (IOException e) {
                System.out.println("Failed to read data for variable: " + varName);
                continue;
            }
            tempVar.variableName = varName;
            tempVar.varDimensionList = varDimensions;
            tempVar.attributeList = attrList;
            tempVar.type = variableType;

            processedVariables.add(tempVar);
        }

        return processedVariables;
    }

    /**
     * This method processes attributes found in the NetcdfFile and returns a list of Wildfire Attributes
     */
    private List<WildfireAttribute> processAttributes(List<Attribute> attributes) {
        List<WildfireAttribute> attributeList = new ArrayList<>();
        for (Attribute attribute : attributes) {
            // @Todo: If data is byte[], convert it to string
            WildfireAttribute tempAttr = new WildfireAttribute();
            tempAttr.attributeName = attribute.getFullName();
            tempAttr.type = attribute.getDataType().toString(); //float, char, or int
            //@Todo: Convert it to an java type array instead of object?
            tempAttr.value = attribute.getValues().get1DJavaArray(attribute.getDataType().getPrimitiveClassType()); //Not sure if we want to keep this as an Array or convert
            attributeList.add(tempAttr);
        }

        return attributeList;
    }

    /**
     * @Todo: Need To Create different one for char types
     * Returns the min, max, and avg of Array of data
     * @param data Array data read from variable
     * @return float[] min, max, avg of data
     */
    public static float[] floatRange(Array data, float fillValue, float missingValue) {
        float max = -Float.MAX_VALUE;
        float min = Float.MAX_VALUE;
        float avg = 0;

        for(int i = 0; i < data.getSize(); i++) {

            float num = data.getFloat(i);
            if (num != fillValue && num != missingValue)
            {
                max = Math.max(max, data.getFloat(i));
                min = Math.min(min, data.getFloat(i));
                avg += data.getFloat(i);
            }
        }
        avg = avg/data.getSize();

        return new float [] {min, max, avg};
    }

    /**
     * Calculates the windspeed from the U and V vectors, returns an array of 8 directions in order of ("North",
     * "North East", "East", "South East", "South", "South West", "West", "North West") with 3 values each of min,
     * max, avg
     * @param u Variable u
     * @param v Variable v
     * @param west_east_stag Length of West to east dimension
     * @param south_north_stag Length of South to north dimension
     * @return double[] Array of 8 directions min, max, avg speeds
     */
    private static List<WildfireVariable> findWind(Variable u, Variable v, int west_east_stag, int south_north_stag) {

        Array uData = null;
        Array vData = null;
        try {
            uData = u.read();
            vData = v.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Get the shape of the arrays
        int west_east_dim = west_east_stag -1;
        int south_north_dim = south_north_stag -1;

        //Stored Data
        double [] windSpeedMax = new double[8];
        double [] windSpeedMin = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE,
                Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        double [] windSpeedSum = new double[8];
        double [] windSpeedCount = new double[8];

        int[] shape = uData.getShape();
        shape[shape.length-1]--; //Go from stag to unstag

        //Calculate size of the whole area from dimension
        int arraySize = 1;
        for (int dimLength : shape) {
            arraySize *= dimLength;
        }

        int uIndex = 0;
        int vIndex = 0;
        for (int i = 0; i < arraySize; i++) {
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

        String [] stringWindDir = {"North Wind", "North East Wind", "East Wind", "South East Wind", "South Wind", "South West Wind", "West Wind", "North West Wind"};
        List<WildfireVariable> windSpeeds = new ArrayList<>();

        for (int i = 0; i < stringWindDir.length; i++) {
            WildfireVariable temp = new WildfireVariable();
            temp.variableName = stringWindDir[i];
            temp.type = DataType.FLOAT;

            temp.maxValue = (float) windSpeedMax[i];
            temp.minValue = (float) windSpeedMin[i];
            temp.average = (float) (windSpeedSum[i]/windSpeedCount[i]);

            temp.attributeList = new ArrayList<>();
            temp.varDimensionList = new ArrayList<>();

            windSpeeds.add(temp);

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
    private static List<WildfireAttribute> findTime(Variable time) {

        int[] dim = time.getShape();
        Array uData;
        try {
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

        try {
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
            System.out.println("No Date Found");
            return new ArrayList<>();
        }


        Date date1 = new Calendar.Builder().setDate(time1Year, time1Month, time1Day)
                .setTimeOfDay(time1Hour, time1Minute, time1Second,0).build().getTime();

        Date date2 = new Calendar.Builder().setDate(time2Year, time2Month, time2Day)
                .setTimeOfDay(time2Hour, time2Minute, time2Second,0).build().getTime();

        WildfireAttribute startDate = new WildfireAttribute();
        startDate.attributeName = "Start Date";
        startDate.type = "Date";
        startDate.value = date1;
        WildfireAttribute endDate = new WildfireAttribute();
        endDate.attributeName = "End Date";
        endDate.type = "Date";
        endDate.value = date2;

        List<WildfireAttribute> dates = new ArrayList<>();
        dates.add(startDate);
        dates.add(endDate);

        return dates;
    }

    /**
     * Calculates a Polygon object from the corners of the minimum and maximum XLAT, XLONG variables.
     * @param latMin Latitude Minimum
     * @param latMax Latitude Maximum
     * @param lonMin Longitude Minimum
     * @param lonMax Longitude Maximum
     * @return Polygon GeoJSON Object of corners (@Todo: Can be changed to Feature)
     */
    private WildfireAttribute calculateCorners(float latMin, float latMax, float lonMin, float lonMax) {
        Position topLeft = new Position(latMin, lonMin);
        Position topRight = new Position(latMin, lonMax);
        Position botRight = new Position(latMax, lonMax);
        Position botLeft = new Position(latMin, lonMax);

        Position[] positions = {topLeft, topRight, botRight, botLeft, topLeft};
        PolygonCoordinates corners = new PolygonCoordinates(Arrays.asList(positions));

        WildfireAttribute cornerAttribute = new WildfireAttribute();

        cornerAttribute.attributeName = "Corners";
        cornerAttribute.type = "Polygon";
        cornerAttribute.value = new Polygon(corners);

        return cornerAttribute;
    }
}
