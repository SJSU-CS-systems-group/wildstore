package com.sjsu.wildfirestorage;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "metadata")
public class Metadata {
    public List<WildfireVariable> variables;
    public List<WildfireAttribute> globalAttributes;
    public String fileName;
    public String filePath;
    public String fileType;
    public int domain;
    @JsonDeserialize(using = GeoJsonPolygonDeserializer.class)
    @JsonSerialize(using = GeoJsonPolygonSerializer.class)
    public GeoJsonPolygon location;
}