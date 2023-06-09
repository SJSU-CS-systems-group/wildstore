package com.sjsu.wildfirestorage;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GeoJsonPolygonDeserializer extends JsonDeserializer<GeoJsonPolygon> {
    @Override
    public GeoJsonPolygon deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        List<Point> points = new ArrayList<>();
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        Iterator it = node.get("coordinates").iterator();
        while (it.hasNext()) {
            var ringNode = (ArrayNode) it.next();
            ringNode.forEach(coordinatePair -> {
                    points.add(new Point(coordinatePair.get(0).asDouble(), coordinatePair.get(1).asDouble()));
                }
            );
        }
        return new GeoJsonPolygon(points);
    }
}
