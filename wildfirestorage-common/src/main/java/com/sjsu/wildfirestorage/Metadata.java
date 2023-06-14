package com.sjsu.wildfirestorage;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
import java.util.Set;

@Document(collection = "metadata")
public class Metadata {
    public List<WildfireVariable> variables;
    public List<WildfireAttribute> globalAttributes;
    public Set<String> fileName;
    public Set<String> filePath;
    public Set<String> fileType;
    public int domain;
    public String digestString;
  
    @JsonDeserialize(using = GeoJsonPolygonDeserializer.class)
    @JsonSerialize(using = GeoJsonPolygonSerializer.class)
    public GeoJsonPolygon location;
}