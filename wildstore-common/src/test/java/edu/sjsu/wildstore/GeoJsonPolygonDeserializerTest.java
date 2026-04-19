package edu.sjsu.wildstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;

import java.util.List;

public class GeoJsonPolygonDeserializerTest {

    private ObjectMapper objectMapper() {
        var mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule()
                .addDeserializer(GeoJsonPolygon.class, new GeoJsonPolygonDeserializer()));
        return mapper;
    }

    @Test
    void testDeserialize() throws Exception {
        String json = "{\"type\":\"Polygon\",\"coordinates\":[[[10.5,20.5],[30.5,20.5],[30.5,40.5],[10.5,40.5],[10.5,20.5]]]}";

        var polygon = objectMapper().readValue(json, GeoJsonPolygon.class);

        Assertions.assertNotNull(polygon);
        Assertions.assertEquals("Polygon", polygon.getType());

        var allPoints = polygon.getCoordinates().stream()
                .flatMap(ls -> ls.getCoordinates().stream())
                .toList();
        Assertions.assertEquals(5, allPoints.size());

        var firstPoint = allPoints.get(0);
        Assertions.assertEquals(10.5, firstPoint.getX(), 0.001);
        Assertions.assertEquals(20.5, firstPoint.getY(), 0.001);
    }

    @Test
    void testRoundTrip() throws Exception {
        var serializerMapper = new ObjectMapper();
        serializerMapper.registerModule(new SimpleModule()
                .addSerializer(GeoJsonPolygon.class, new GeoJsonPolygonSerializer()));

        var original = new GeoJsonPolygon(List.of(
                new Point(-120, 35),
                new Point(-110, 35),
                new Point(-110, 40),
                new Point(-120, 40),
                new Point(-120, 35)
        ));

        var json = serializerMapper.writeValueAsString(original);
        var restored = objectMapper().readValue(json, GeoJsonPolygon.class);

        var originalPoints = original.getCoordinates().get(0).getCoordinates();
        var restoredPoints = restored.getCoordinates().get(0).getCoordinates();
        Assertions.assertEquals(originalPoints.size(), restoredPoints.size());
        for (int i = 0; i < originalPoints.size(); i++) {
            Assertions.assertEquals(originalPoints.get(i).getX(), restoredPoints.get(i).getX(), 0.001);
            Assertions.assertEquals(originalPoints.get(i).getY(), restoredPoints.get(i).getY(), 0.001);
        }
    }
}