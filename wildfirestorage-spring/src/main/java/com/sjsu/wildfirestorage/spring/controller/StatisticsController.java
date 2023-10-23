package com.sjsu.wildfirestorage.spring.controller;

import com.mongodb.client.model.Accumulators;
import com.sjsu.wildfirestorage.Metadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
@RequestMapping("/api")
public class StatisticsController {

    @Autowired
    private MongoTemplate mongoTemplate;

    public final String METADATA_COLLECTION = "metadata";
    public final String DATASET_COLLECTION = "dataset";

    @GetMapping("/stats/metadataBasic")
    public HashMap<String, String> metadataBasic (@RequestParam("collectionName") String collectionName) {
        HashMap <String, String> res = new HashMap<>();

        long collectionSize = getCollectionSize(collectionName);
        res.put("collectionSize", String.valueOf(collectionSize));

        long duplicateSize = getDuplicateSize(collectionName);
        res.put("duplicateSize", String.valueOf(duplicateSize));

        if(collectionName.equalsIgnoreCase("metadata")) {
            long numberOfVariables = getNumberOfVariables();
            res.put("numberOfVariables", String.valueOf(numberOfVariables));

            long numberOfAttributes = getNumberOfAttributes();
            res.put("numberOfAttributes", String.valueOf(numberOfAttributes));
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
     * Returns the nnumber of unique attributes in the metadata collection.
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
     * @return List of each variable and number of times mentioned
     */
    private long getCountOfVariables() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.project("globalAttributes"),
                Aggregation.unwind("globalAttributes"),
                Aggregation.group("globalAttributes.attributeName").count().as("NumAttributes")
        );
        AggregationResults<Object> aggregationResults = mongoTemplate.aggregate(aggregation, METADATA_COLLECTION, Object.class);
//        return aggregationResults.getMappedResults();
        return 0;
    }


}

