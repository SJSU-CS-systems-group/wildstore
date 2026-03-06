package edu.sjsu.wildstore.meta;

import net.sf.jsqlparser.JSQLParserException;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Criteria;

import static org.junit.jupiter.api.Assertions.*;

class CriteriaBuilderTest {

    @Test
    void emptyQueryReturnsNull() throws JSQLParserException {
        assertNull(CriteriaBuilder.buildFromSQL(""));
    }

    @Test
    void equalsString() throws JSQLParserException {
        Criteria c = CriteriaBuilder.buildFromSQL("fileName = 'test.nc'");
        assertNotNull(c);
        String json = c.getCriteriaObject().toJson();
        assertTrue(json.contains("fileName"));
        assertTrue(json.contains("test.nc"));
    }

    @Test
    void equalsLong() throws JSQLParserException {
        Criteria c = CriteriaBuilder.buildFromSQL("size = 100");
        assertNotNull(c);
        String json = c.getCriteriaObject().toJson();
        assertTrue(json.contains("size"));
        assertTrue(json.contains("100"));
    }

    @Test
    void notEquals() throws JSQLParserException {
        Criteria c = CriteriaBuilder.buildFromSQL("fileName != 'test.nc'");
        assertNotNull(c);
        String json = c.getCriteriaObject().toJson();
        assertTrue(json.contains("$ne"));
    }

    @Test
    void greaterThan() throws JSQLParserException {
        Criteria c = CriteriaBuilder.buildFromSQL("size > 50");
        assertNotNull(c);
        String json = c.getCriteriaObject().toJson();
        assertTrue(json.contains("$gt"));
    }

    @Test
    void greaterThanEquals() throws JSQLParserException {
        Criteria c = CriteriaBuilder.buildFromSQL("size >= 50");
        assertNotNull(c);
        String json = c.getCriteriaObject().toJson();
        assertTrue(json.contains("$gte"));
    }

    @Test
    void lessThan() throws JSQLParserException {
        Criteria c = CriteriaBuilder.buildFromSQL("size < 50");
        assertNotNull(c);
        String json = c.getCriteriaObject().toJson();
        assertTrue(json.contains("$lt"));
    }

    @Test
    void lessThanEquals() throws JSQLParserException {
        Criteria c = CriteriaBuilder.buildFromSQL("size <= 50");
        assertNotNull(c);
        String json = c.getCriteriaObject().toJson();
        assertTrue(json.contains("$lte"));
    }

    @Test
    void likeExpression() throws JSQLParserException {
        Criteria c = CriteriaBuilder.buildFromSQL("fileName LIKE '%test%'");
        assertNotNull(c);
        String json = c.getCriteriaObject().toJson();
        assertTrue(json.contains("$regularExpression"));
        assertTrue(json.contains(".*test.*"));
    }

    @Test
    void andExpression() throws JSQLParserException {
        Criteria c = CriteriaBuilder.buildFromSQL("fileName = 'test.nc' AND size > 50");
        assertNotNull(c);
        String json = c.getCriteriaObject().toJson();
        assertTrue(json.contains("$and"));
    }

    @Test
    void orExpression() throws JSQLParserException {
        Criteria c = CriteriaBuilder.buildFromSQL("fileName = 'a.nc' OR fileName = 'b.nc'");
        assertNotNull(c);
        String json = c.getCriteriaObject().toJson();
        assertTrue(json.contains("$or"));
    }

    @Test
    void varTypeElemMatch() throws JSQLParserException {
        Criteria c = CriteriaBuilder.buildFromSQL("VAR.temperature.units = 'K'");
        assertNotNull(c);
        String json = c.getCriteriaObject().toJson();
        assertTrue(json.contains("variables"));
        assertTrue(json.contains("$elemMatch"));
        assertTrue(json.contains("variableName"));
        assertTrue(json.contains("temperature"));
    }

    @Test
    void attrTypeElemMatch() throws JSQLParserException {
        Criteria c = CriteriaBuilder.buildFromSQL("ATTR.conventions.value = 'CF-1.6'");
        assertNotNull(c);
        String json = c.getCriteriaObject().toJson();
        assertTrue(json.contains("globalAttributes"));
        assertTrue(json.contains("$elemMatch"));
        assertTrue(json.contains("attributeName"));
        assertTrue(json.contains("conventions"));
    }

    @Test
    void invalidQueryThrows() {
        assertThrows(JSQLParserException.class, () ->
            CriteriaBuilder.buildFromSQL("NOT VALID SQL !!!")
        );
    }
}
