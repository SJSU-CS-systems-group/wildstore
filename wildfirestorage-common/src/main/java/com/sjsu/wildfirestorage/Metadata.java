package com.sjsu.wildfirestorage;

import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

@Document(collection = "metadata")
public class Metadata {
    public List<Variable> variables;
    public List<GlobalAttribute> globalAttributes;
    public String fileName;
}