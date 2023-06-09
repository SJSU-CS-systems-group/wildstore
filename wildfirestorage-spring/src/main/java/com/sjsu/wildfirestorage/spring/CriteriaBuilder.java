package com.sjsu.wildfirestorage.spring;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;
import org.springframework.data.mongodb.core.query.Criteria;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class CriteriaBuilder {

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String VAR_TYPE = "VAR";
    public static final String ATTRIBUTE_TYPE = "ATTR";
    public static final String LOCATION_TYPE = "LOCATION";
    public static final String SQL_PREFIX = "SELECT * FROM metadata WHERE ";
    public static final String VARIABLE_NAME_FIELD = "variableName";
    public static final String VARIABLES_ARRAY_FIELD = "variables";
    public static final String ATTRIBUTE_NAME_FIELD = "attributeName";
    public static final String ATTRIBUTES_ARRAY_FIELD = "globalAttributes";
    public static final String LOCATION_FIELD = "location";

    /**
     *
     * @param query WHERE clause of an SQL query
     * @return MongoDB Criteria
     * @throws JSQLParserException
     */
    public static Criteria buildFromSQL(String query) throws JSQLParserException {
        // Set SELECT clause for parsing
        query = SQL_PREFIX + query;
        PlainSelect select = (PlainSelect) ((Select) CCJSqlParserUtil.parse(query)).getSelectBody();
        return build(select.getWhere());
    }

    /**
     * Builds Criteria recursively
     * @param ex a JSQLParser expression
     * @return a MongoDB Criteria representing the SQL query
     */
    private static Criteria build(Expression ex) {
        switch(ex.getClass().getSimpleName()){
            case "AndExpression" : {
                Criteria criteriaLeft = build(((AndExpression) ex).getLeftExpression());
                Criteria criteriaRight = build(((AndExpression) ex).getRightExpression());
                Criteria criteria = new Criteria();
                criteria.andOperator(criteriaLeft, criteriaRight);
                return criteria;
            }
            case "OrExpression" : {
                Criteria criteriaLeft = build(((OrExpression) ex).getLeftExpression());
                Criteria criteriaRight = build(((OrExpression) ex).getRightExpression());
                Criteria criteria = new Criteria();
                criteria.orOperator(criteriaLeft, criteriaRight);
                return criteria;
            }
            case "EqualsTo" : {
                EqualsTo eq = (EqualsTo) ex;
                Column column = (Column) eq.getLeftExpression();
                Criteria arrayCriteria = getArrayMatchCriteria(column);
                if(arrayCriteria != null) {
                    arrayCriteria.and(column.getColumnName()).is(getPrimitiveValue(eq.getRightExpression()));
                    return getElemMatchCriteria(column, arrayCriteria);
                } else {
                    return Criteria.where(column.toString()).is(getPrimitiveValue(eq.getRightExpression()));
                }
            }
            case "NotEqualsTo" : {
                NotEqualsTo neq = (NotEqualsTo) ex;
                Column column = (Column) neq.getLeftExpression();
                Criteria arrayCriteria = getArrayMatchCriteria(column);
                if(arrayCriteria != null) {
                    arrayCriteria.and(column.getColumnName()).ne(getPrimitiveValue(neq.getRightExpression()));
                    return getElemMatchCriteria(column, arrayCriteria);
                } else {
                    return Criteria.where(column.toString()).ne(getPrimitiveValue(neq.getRightExpression()));
                }
            }
            case "GreaterThan" : {
                GreaterThan gt = (GreaterThan) ex;
                Column column = (Column) gt.getLeftExpression();
                Criteria arrayCriteria = getArrayMatchCriteria(column);
                if(arrayCriteria != null) {
                    arrayCriteria.and(column.getColumnName()).gt(getPrimitiveValue(gt.getRightExpression()));
                    return getElemMatchCriteria(column, arrayCriteria);
                } else {
                    return Criteria.where(column.toString()).gt(getPrimitiveValue(gt.getRightExpression()));
                }
            }
            case "GreaterThanEquals" : {
                GreaterThanEquals gte = (GreaterThanEquals) ex;
                Column column = (Column) gte.getLeftExpression();
                Criteria arrayCriteria = getArrayMatchCriteria(column);
                if(arrayCriteria != null) {
                    arrayCriteria.and(column.getColumnName()).gte(getPrimitiveValue(gte.getRightExpression()));
                    return getElemMatchCriteria(column, arrayCriteria);
                } else {
                    return Criteria.where(column.toString()).gte(getPrimitiveValue(gte.getRightExpression()));
                }
            }
            case "MinorThan" : {
                MinorThan mt = (MinorThan) ex;
                Column column = (Column) mt.getLeftExpression();
                Criteria arrayCriteria = getArrayMatchCriteria(column);
                if(arrayCriteria != null) {
                    arrayCriteria.and(column.getColumnName()).lt(getPrimitiveValue(mt.getRightExpression()));
                    return getElemMatchCriteria(column, arrayCriteria);
                } else {
                    return Criteria.where(column.toString()).lt(getPrimitiveValue(mt.getRightExpression()));
                }
            }
            case "MinorThanEquals" : {
                MinorThanEquals mte = (MinorThanEquals) ex;
                Column column = (Column) mte.getLeftExpression();
                Criteria arrayCriteria = getArrayMatchCriteria(column);
                if(arrayCriteria != null) {
                    arrayCriteria.and(column.getColumnName()).lte(getPrimitiveValue(mte.getRightExpression()));
                    return getElemMatchCriteria(column, arrayCriteria);
                } else {
                    return Criteria.where(column.toString()).lte(getPrimitiveValue(mte.getRightExpression()));
                }
            }
            case "InExpression" : {
                InExpression in = (InExpression) ex;
                Column column = (Column) in.getLeftExpression();
                if(column.getColumnName().equals(LOCATION_TYPE)) {
                    List<Point> polygonPoints = new ArrayList<>();
                    for(var item: ((ExpressionList)in.getRightItemsList()).getExpressions()){
                        ArrayList<Double> pt = new ArrayList<>();
                        for(var coordinate : ((RowConstructor)item).getExprList().getExpressions()) {
                            pt.add(((DoubleValue)coordinate).getValue());
                        }
                        polygonPoints.add(new Point(pt.get(0), pt.get(1)));
                    }
                    polygonPoints.add(polygonPoints.get(0));
                    GeoJsonPolygon polygon = new GeoJsonPolygon(polygonPoints);
                    return (Criteria.where(LOCATION_FIELD).intersects(polygon));
                } else {
                    Criteria arrayCriteria = getArrayMatchCriteria(column);
                    List<Object> inListItems = new ArrayList<>();
                    for(var item: ((ExpressionList)in.getRightItemsList()).getExpressions()){
                        inListItems.add(getPrimitiveValue(item));
                    }
                    if(arrayCriteria != null) {
                        arrayCriteria.and(column.getColumnName()).in(inListItems);
                        return getElemMatchCriteria(column, arrayCriteria);
                    } else {
                        return Criteria.where(column.toString()).in(inListItems);
                    }
                }
            }
            case "Parenthesis": {
                return build(((Parenthesis) ex).getExpression());
            }
            default: {
                System.out.println("DEFAULT" + ex.getClass());
            }
        }
        return null;
    }

    /**
     * Returns a Criteria for an Array (variables, globalAttributes)
     * Schema name is used to identify variables or globalAttributes array.
     * Table name is used to identify a particular variable or global attribute name.
     * @param column JSQLParser column
     * @return A Criteria that simply matches the variableName or attributeName fields with the requested value
     */
    private static Criteria getArrayMatchCriteria(Column column) {
        if(column.getTable() != null && column.getTable().getSchemaName() != null) {
            switch (column.getTable().getSchemaName()) {
                case VAR_TYPE: {
                    return Criteria.where(VARIABLE_NAME_FIELD).is(column.getTable().getName());
                }
                case ATTRIBUTE_TYPE: {
                    return Criteria.where(ATTRIBUTE_NAME_FIELD).is(column.getTable().getName());
                }
                default: {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     *
     * @param column JSQLParser Column
     * @param elemCriteria the criteria that needs to be wrapped up in an elemMatch operator
     * @return Criteria with elemMatch operator
     */
    private static Criteria getElemMatchCriteria(Column column, Criteria elemCriteria) {
        if(column.getTable() != null && column.getTable().getSchemaName() != null) {
            switch (column.getTable().getSchemaName()) {
                case VAR_TYPE: {
                    return Criteria.where(VARIABLES_ARRAY_FIELD).elemMatch(elemCriteria);
                }
                case ATTRIBUTE_TYPE: {
                    return Criteria.where(ATTRIBUTES_ARRAY_FIELD).elemMatch(elemCriteria);
                }
                default: {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Converts JSQLParser values to Java values
     * @param ex JSQLParser expression
     * @return A java representation of the queried value
     */
    private static Object getPrimitiveValue(Expression ex) {
        switch (ex.getClass().getSimpleName()) {
            case "LongValue": {
                return (long) ((LongValue)ex).getValue();
            }
            case "DoubleValue": {
                return (double) ((DoubleValue)ex).getValue();
            }
            case "DateTimeLiteralExpression": {
                SimpleDateFormat isoFormat = new SimpleDateFormat(DATE_FORMAT);
                try {
                    String datetime = ((DateTimeLiteralExpression) ex).getValue();
                    return isoFormat.parse(datetime.substring(1, datetime.length()-1));
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
            case "StringValue": {
                return ((StringValue)ex).getValue();
            }
            default: {
                System.out.println("Unknown JSQLParser Value type");
                break;
            }
        }
        return 0.0;
    }
}
