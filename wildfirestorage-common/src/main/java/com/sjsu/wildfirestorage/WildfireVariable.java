package com.sjsu.wildfirestorage;
import org.springframework.data.mongodb.core.mapping.Document;

import ucar.ma2.DataType;
import java.util.List;

@Document
public class WildfireVariable {
    public String variableName;
    public List<VarDimension> varDimensionList;
    public List<WildfireAttribute> attributeList;
    public DataType type;
    public float minValue;
    public float maxValue;
    public float average;
    record VarDimension(String name, int value) {}
}