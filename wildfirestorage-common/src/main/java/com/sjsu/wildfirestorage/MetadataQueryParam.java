package com.sjsu.wildfirestorage;

public class MetadataQueryParam {
    public enum PARAM_TYPE {LOCATION, VARIABLE, ATTRIBUTE, AND, OR};
    public enum OPERATOR {GREATER_THAN, LESS_THAN, EQUALS, NOT_EQUALS, GREATER_THAN_OR_EQUALS, LESS_THAN_OR_EQUALS, IN, AND, OR, NOT, INTERSECTS, WITHIN, NEAR};
    public PARAM_TYPE type;
    public Object lhs;
    public Object rhs;
    public OPERATOR operator;
}
