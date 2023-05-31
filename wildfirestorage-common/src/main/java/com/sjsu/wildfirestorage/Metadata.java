package com.sjsu.wildfirestorage;

import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "metadata")
public class Metadata {
    public List<WildfireVariable> variables;
    public List<WildfireAttribute> globalAttributes;
    public String fileName;
    public String filePath;
    public Object corners;
    public String fileType;
    public int domain;
}