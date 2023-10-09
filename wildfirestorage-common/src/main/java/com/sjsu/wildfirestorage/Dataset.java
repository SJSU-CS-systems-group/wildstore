package com.sjsu.wildfirestorage;

import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Document(collection = "dataset")
public class Dataset {
    public String digestString;
    public Set<String> datasetPath;
    public Date firstTimeStamp;
    public Date lastTimeStamp;
    public int maxDomain;
    public List<String> digestList;
}
