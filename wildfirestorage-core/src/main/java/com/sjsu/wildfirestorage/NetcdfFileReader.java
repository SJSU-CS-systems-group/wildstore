package com.sjsu.wildfirestorage;

import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.util.CancelTask;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetcdfFileReader {
    private final String netcdfFilepath;
    private NetcdfFile netcdfFile;
    private DigestingRandomAccessFile randomAccessFile;
    public DigestingRandomAccessFile getRandomAccessFile() { return randomAccessFile; }
    private final int ENUM_THRESHOLD = 20;

    public NetcdfFileReader(String netcdfFilepath) {
        this.netcdfFilepath = netcdfFilepath;
    }

    public Metadata processFile() {
        // Try and read the contents of the netCDF file
        try {
            var method = NetcdfFile.class.getDeclaredMethod("open", RandomAccessFile.class, String.class, CancelTask.class, Object.class);
            method.setAccessible(true);
            randomAccessFile = new DigestingRandomAccessFile(netcdfFilepath);
            this.netcdfFile =
                    (NetcdfFile) method.invoke(null, randomAccessFile, netcdfFilepath, null, null);
        } catch (IOException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        Metadata metadata = new Metadata();
        String fileNameStr = netcdfFilepath.substring(netcdfFilepath.lastIndexOf('/')+1);
        metadata.fileName = Set.of(fileNameStr);
        metadata.filePath = Set.of(netcdfFilepath);

        metadata.globalAttributes = readGlobalAttributes();

        metadata.variables = readVariables();

        //Special processing @Todo: Find a more efficient way to do this
        //FileName
        String[] fileNameParsed = parseName(fileNameStr);
        metadata.fileType = (fileNameParsed[0] != null) ? Set.of(fileNameParsed[0]) : null;
        metadata.domain = (fileNameParsed[1] != null && !fileNameParsed[1].equals("")) ? Integer.parseInt(fileNameParsed[1]) : 0;
        Date startDateValue = null;
        if (fileNameParsed[2] != null && !fileNameParsed[2].startsWith("0000")){
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy MM dd HH mm ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                startDateValue = dateFormat.parse(fileNameParsed[2]);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }

        }
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
        else if (startDateValue != null) {
            WildfireAttribute startDate = new WildfireAttribute();
            startDate.attributeName = "StartDate";
            startDate.type = "Date";
            startDate.value = startDateValue;
            metadata.globalAttributes.add(startDate);
        }
        //Corners
        Variable xlat = netcdfFile.findVariable("XLAT");
        Variable xlong = netcdfFile.findVariable("XLONG");
        Attribute corner_lat = netcdfFile.findGlobalAttribute("corner_lats");
        Attribute corner_lon = netcdfFile.findGlobalAttribute("corner_lons");
        if (corner_lat != null && corner_lon != null) {
            Array lat_values = corner_lat.getValues();
            Array lon_values = corner_lon.getValues();
            List <Point> points = new ArrayList<>();

            for (int i = (int) (lat_values.getSize()-4); i < lat_values.getSize(); i++) {
                points.add(new Point(lon_values.getFloat(i), lat_values.getFloat(i)));
            }
            points.add(new Point(lon_values.getFloat((int) (lat_values.getSize()-4)), lat_values.getFloat((int) (lat_values.getSize()-4))));

            metadata.location = new GeoJsonPolygon(points);
        }
        else if (xlat != null && xlong != null) {
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
            metadata.location = calculateCorners(xMin, xMax, yMin, yMax);
        }
        else {
            metadata.location = null;
        }
        try {
            metadata.digestString = randomAccessFile.getDigestString(true);
        } catch (IOException e) {
            metadata.digestString = null;
            System.out.println("No digest was found for this file.");
            throw new RuntimeException(e);
        }

        return metadata;
    }

    public List<WildfireAttribute> readGlobalAttributes() {

        List<Attribute> attributes = this.netcdfFile.getGlobalAttributes();

        return processAttributes(attributes);
    }

    // Record to store processed attributes
    public List<WildfireVariable> readVariables() {
        List<Variable> variables = netcdfFile.getVariables();

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
            float fillValue = variable.findAttributeIgnoreCase("_fillvalue") != null ? (float) variable.findAttribute("_FillValue").getNumericValue() : Float.MAX_VALUE;
            float missingValue = variable.findAttributeIgnoreCase("missing_value") != null ? (float) variable.findAttribute("missing_value").getNumericValue() : Float.MAX_VALUE;
            float max = -Float.MAX_VALUE;
            float min = Float.MAX_VALUE;
            float avg = 0;
            HashSet<Float> uniqueElements = new HashSet<>();

            // Read data from the variable
            try {
                data = variable.read();

                for(int i = 0; i < data.getSize(); i++) {

                    float num = data.getFloat(i);
                    if (num != fillValue && num != missingValue) {
                        max = Math.max(max, num);
                        min = Math.min(min, num);
                        avg += num;
                        if (uniqueElements.size() < ENUM_THRESHOLD + 1 ) {
                            uniqueElements.add(num);
                        }
                    }
                }
                avg = avg/data.getSize();

                tempVar.minValue = min;
                tempVar.maxValue = max;
                tempVar.average = avg;

                if (uniqueElements.size() > 1 && uniqueElements.size() < ENUM_THRESHOLD && !variable.getDataType().toString().equalsIgnoreCase("char")) {
                    tempVar.elementSet = uniqueElements;
                }
                else {
                    tempVar.elementSet = new HashSet<>();
                }
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
        }

        String [] stringWindDir = {"NorthWind", "NorthEastWind", "EastWind", "SouthEastWind", "SouthWind", "SouthWestWind", "WestWind", "NorthWestWind"};
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

        String[] dateName = {"StartDate", "EndDate"};
        String[] times = {firstTime, lastTime};
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy MM dd HH mm ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        List<WildfireAttribute> dates = new ArrayList<>();

        for (int i = 0; i < dateName.length; i++) {
            Matcher timeMatch = pattern.matcher(times[i]);

            String year = "0000";
            String month = "00";
            String day = "00";
            String hour = "00";
            String min = "00";
            String sec = "00";
            if (timeMatch.find()) {
                year = (timeMatch.group(1) == null) ? "0000" : timeMatch.group(1); //Year
                month = (timeMatch.group(2) == null) ? "00" : timeMatch.group(2); //Month
                day = (timeMatch.group(3) == null) ? "00" : timeMatch.group(3); //Day
                hour = (timeMatch.group(4) == null) ? "00" : timeMatch.group(4); //Hour
                min = (timeMatch.group(5) == null) ? "00" : timeMatch.group(5); //Minute
                sec = (timeMatch.group(6) == null) ? "00" : timeMatch.group(6); //Seconds
            }
            String stringDate = String.format("%s %s %s %s %s %s", year, month, day, hour, min, sec);

            Date dateValue;
            try {
                dateValue = dateFormat.parse(stringDate);
            } catch (ParseException e) {
                dateValue = null;
            }

            WildfireAttribute date = new WildfireAttribute();
            date.attributeName = dateName[i];
            date.type = "Date";
            date.value = dateValue;
            if (!year.equals("0000"))
                dates.add(date);
        }

        return dates;
    }

    /**
     * Calculates a Polygon object from the corners of the minimum and maximum XLAT, XLONG variables.
     * @param latMin Latitude Minimum
     * @param latMax Latitude Maximum
     * @param lonMin Longitude Minimum
     * @param lonMax Longitude Maximum
     * @return Polygon GeoJSON Object of corners
     */
    private GeoJsonPolygon calculateCorners(float latMin, float latMax, float lonMin, float lonMax) {
        Point topLeft = new Point(lonMax, latMin);
        Point topRight = new Point(lonMax, latMax);
        Point botRight = new Point(lonMin, latMax);
        Point botLeft = new Point(lonMin, latMin);

        return new GeoJsonPolygon(List.of(topLeft, topRight, botRight, botLeft, topLeft));
    }

    private String[] parseName(String fileName) {
        Pattern pattern = Pattern.compile("(.*).+?d([0-9][0-9]).?([0-9][0-9][0-9][0-9])?.?([0-9][0-9])?.?([0-9][0-9])?.?([0-9][0-9])?.?([0-9][0-9])?.?([0-9][0-9])?");
        Matcher matcher = pattern.matcher(fileName);

        String fileType = null;
        String domain = null;
        String year = "0000";
        String month="00";
        String day="00";
        String hour="00";
        String min="00";
        String sec="00";
        if(matcher.find()) {
            year = (matcher.group(3) == null) ? "0000" : matcher.group(3); //Year
            month = (matcher.group(4) == null) ? "00" : matcher.group(4); //Month
            day = (matcher.group(5) == null) ? "00" : matcher.group(5); //Day
            hour = (matcher.group(6) == null) ? "00" : matcher.group(6); //Hour
            min = (matcher.group(7) == null) ? "00" : matcher.group(7); //Minute
            sec = (matcher.group(8) == null) ? "00" : matcher.group(8); //Seconds
        }
        String date = String.format("%s %s %s %s %s %s", year, month, day, hour, min, sec);
        return new String[]{fileType, domain, date};
    }
}
