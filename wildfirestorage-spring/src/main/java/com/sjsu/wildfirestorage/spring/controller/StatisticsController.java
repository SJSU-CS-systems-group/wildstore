package com.sjsu.wildfirestorage.spring.controller;

import com.sjsu.wildfirestorage.Metadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api")
public class StatisticsController {

    @Autowired
    private MongoTemplate mongoTemplate;

    public final String METADATA_COLLECTION = "metadata";
    public final String DATASET_COLLECTION = "dataset";

    @GetMapping("/stats/metadataBasic")
    public HashMap<String, Object> metadataBasic (@RequestParam("collectionName") String collectionName) {
        HashMap <String, Object> res = new HashMap<>();

        long collectionSize = getCollectionSize(collectionName);
        res.put("collectionSize", String.valueOf(collectionSize));

        long duplicateSize = getDuplicateSize(collectionName);
        res.put("duplicateSize", String.valueOf(duplicateSize));

        if(collectionName.equalsIgnoreCase("metadata")) {
            long numberOfVariables = getNumberOfVariables();
            res.put("numberOfVariables", String.valueOf(numberOfVariables));

            long numberOfAttributes = getNumberOfAttributes();
            res.put("numberOfAttributes", String.valueOf(numberOfAttributes));

            List<String> uniqueVarNames = getUniqueVariableNames();
            res.put("UniqueVariableNames", uniqueVarNames);

            List<String> uniqueAttrNames = getUniqueAttributeNames();
            res.put("UniqueAttributeNames", uniqueAttrNames);
        }

        return res;
    }

    /**
     * Returns number of documents in specified Collection. If return
     * 0, then no collection was specified or error.
     * @param collectionName Collection to query on
     * @return Returns number of documents in specified Collection
     */
    private long getCollectionSize(String collectionName) {
        Query query = new Query();
        if(collectionName.equalsIgnoreCase("metadata")) {
            return mongoTemplate.count(query, METADATA_COLLECTION);
        }
        else if (collectionName.equalsIgnoreCase("dataset")) {
            return mongoTemplate.count(query, DATASET_COLLECTION);
        }

        return 0;
    }

    /**
     * Returns number of duplicated documents in specified Collection. If return
     * 0, then no collection was specified or error.
     * Note: There "should" be no documents with filePath set size of 0
     * @param collectionName Collection to query on
     * @return Returns number of documents in specified Collection
     */
    private long getDuplicateSize(String collectionName) {
        if(collectionName.equalsIgnoreCase("metadata")) {
            Query query = new Query(Criteria.where("filePath").not().size(1));
            return mongoTemplate.count(query, Metadata.class);
        }
        else if (collectionName.equalsIgnoreCase("dataset")) {
            //Prob not working yet (Need to change datasetFilePath to set)
            Query query = new Query(Criteria.where("datasetPath").not().size(1));
            return mongoTemplate.count(query, DATASET_COLLECTION);
        }
        return 0;
    }

    /**
     * Returns the number of unique variables in the metadata collection.
     * @return Number of unique variables
     */
    private long getNumberOfVariables() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.project("variables"),
                Aggregation.unwind("variables"),
                Aggregation.group("variables.variableName").count().as("NumVariables")
        );
        AggregationResults<Object> aggregationResults = mongoTemplate.aggregate(aggregation, METADATA_COLLECTION, Object.class);
        return aggregationResults.getMappedResults().size();
    }

    /**
     * Return the number of unique attributes in the metadata collection.
     * @return Number of unique attributes
     */
    private long getNumberOfAttributes() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.project("globalAttributes"),
                Aggregation.unwind("globalAttributes"),
                Aggregation.group("globalAttributes.attributeName").count().as("NumAttributes")
        );
        AggregationResults<Object> aggregationResults = mongoTemplate.aggregate(aggregation, METADATA_COLLECTION, Object.class);
        return aggregationResults.getMappedResults().size();
    }

    /**
     * WIP
     * Should return the number of each variable mentioned in the metadata collection.
     *
     * @return List of each variable and number of times mentioned
     */
    private List<String> getUniqueVariableNames() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.project("variables"),
                Aggregation.unwind("variables"),
                Aggregation.group("variables.variableName"),
                Aggregation.group().addToSet("_id").as("UniqueVars"),
                Aggregation.project("UniqueVars").andExclude("_id"),
                Aggregation.unwind("UniqueVars"),
                Aggregation.sort(Sort.Direction.ASC, "UniqueVars")
        );
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(aggregation, METADATA_COLLECTION, HashMap.class);

        List<String> uniqueVars = new ArrayList<>();

        aggregationResults.getMappedResults().forEach( attrName -> {
            //Since result
            uniqueVars.add((String) attrName.get("UniqueVars"));
        });

        return uniqueVars;
    }

    /**
     * WIP
     * Should return the number of each variable mentioned in the metadata collection.
     *
     * @return List of each variable and number of times mentioned
     */
    private List<String> getUniqueAttributeNames() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.project("globalAttributes"),
                Aggregation.unwind("globalAttributes"),
                Aggregation.group("globalAttributes.attributeName"),
                Aggregation.group().addToSet("_id").as("UniqueAttrs"),
                Aggregation.project("UniqueAttrs").andExclude("_id"),
                Aggregation.unwind("UniqueAttrs"),
                Aggregation.sort(Sort.Direction.ASC, "UniqueAttrs")
        );
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(aggregation, METADATA_COLLECTION, HashMap.class);

        List<String> uniqueAttr = new ArrayList<>();

        aggregationResults.getMappedResults().forEach( attrName -> {
            //Since result
            uniqueAttr.add((String) attrName.get("UniqueAttrs"));
        });

        return uniqueAttr;
    }




}

