package com.sjsu.wildfirestorage;

import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Variable {
    public String variableName;
    public String type;
    public String unit;
    public float minValue;
    public float maxValue;
    public float average;
}
