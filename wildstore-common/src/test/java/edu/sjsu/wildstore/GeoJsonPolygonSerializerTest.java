package edu.sjsu.wildstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;

import java.util.List;

public class GeoJsonPolygonSerializerTest {

    private ObjectMapper objectMapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule()
                .addSerializer(GeoJsonPolygon.class, new GeoJsonPolygonSerializer()));
        return mapper;
    }

    @Test
    void testSerialize() throws Exception {
        var polygon = new GeoJsonPolygon(List.of(
                new Point(10.5, 20.5),
                new Point(30.5, 20.5),
                new Point(30.5, 40.5),
                new Point(10.5, 40.5),
                new Point(10.5, 20.5)
        ));

        var node = objectMapper().readTree(objectMapper().writeValueAsString(polygon));

        Assertions.assertEquals("Polygon", node.get("type").asText());
        Assertions.assertTrue(node.get("coordinates").isArray());
        Assertions.assertTrue(node.get("coordinates").get(0).isArray());
        Assertions.assertTrue(node.get("coordinates").get(0).get(0).isArray());
        Assertions.assertEquals(2, node.get("coordinates").get(0).get(0).size());

        var json = node.toString();
        Assertions.assertTrue(json.contains("10.5"));
        Assertions.assertTrue(json.contains("20.5"));
        Assertions.assertTrue(json.contains("30.5"));
        Assertions.assertTrue(json.contains("40.5"));
    }
}