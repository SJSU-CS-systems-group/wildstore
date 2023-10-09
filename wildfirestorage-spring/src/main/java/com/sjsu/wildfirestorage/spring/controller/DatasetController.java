package com.sjsu.wildfirestorage.spring.controller;

import com.mongodb.MongoWriteException;
import com.sjsu.wildfirestorage.Dataset;
import com.sjsu.wildfirestorage.Metadata;
import com.sjsu.wildfirestorage.WildfireAttribute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.*;

@RestController
@RequestMapping("/api")
public class DatasetController {

    @Autowired
    private MongoTemplate mongoTemplate;
    public final String METADATA_COLLECTION = "metadata";
    public final String DATASET_COLLECTION = "dataset";

    public final String TEMP_DATASET_COLLECTION = "dataset-temp";

    @PostMapping("/dataset")
    public int upsertDataset() throws MongoWriteException {

        mongoTemplate.createCollection(TEMP_DATASET_COLLECTION);
        //Query all metadata in the collection
        mongoTemplate.stream(new Query(), Metadata.class, METADATA_COLLECTION).forEach( metadata -> {
            Set<String> filepath = metadata.filePath;

            //For each filepath in metadata, update corresponding dataset
            for (String path : filepath) {
                path = path.substring(0, path.lastIndexOf('/')+1);

                Query query = new Query(Criteria.where("datasetPath").is(path));
                var existingDoc = mongoTemplate.find(query, Dataset.class, TEMP_DATASET_COLLECTION);

                if(!existingDoc.isEmpty()) {
                    //If document exist, update it
                    Update update = new Update();
                    //Update dataDigestString
                    BigInteger digest = base64decoding(existingDoc.get(0).digestString);
                    digest = digest.add(base64decoding(metadata.digestString));
                    update.set("digestString", base64encoding(digest));
                    //Update digestList
                    existingDoc.get(0).digestList.add(metadata.digestString);
                    update.set("digestList", existingDoc.get(0).digestList);
                    //Update filePath
                    existingDoc.get(0).datasetPath.add(path);
                    update.set("datasetPath", existingDoc.get(0).datasetPath);
                    //Update maxDomain
                    if(existingDoc.get(0).maxDomain < metadata.domain) {
                        existingDoc.get(0).maxDomain = metadata.domain;
                        update.set("maxDomain", existingDoc.get(0).maxDomain);
                    }
                    //Update first time and last stamp if timestamp is earlier or later
                    for (WildfireAttribute attr : metadata.globalAttributes)
                    {
                        if(attr.attributeName.equals("StartDate") || attr.attributeName.equals("Start_Date")) {
                            //If existing first date is greater than current, update
                            Date tempDate;
                            if (attr.type.equals("int")) {
                                ArrayList<Integer> dateList = (ArrayList<Integer>) attr.value;
                                tempDate = new Date(dateList.get(0));
                            }
                            else {
                                tempDate = (Date) attr.value;
                            }
                            if (existingDoc.get(0).firstTimeStamp.compareTo(tempDate) > 0) {
                                existingDoc.get(0).firstTimeStamp = tempDate;
                                update.set("firstTimeStamp", existingDoc.get(0).firstTimeStamp);
                            }
                            else if (existingDoc.get(0).lastTimeStamp.compareTo(tempDate) < 0) {
                                existingDoc.get(0).lastTimeStamp = tempDate;
                                update.set("lastTimeStamp", existingDoc.get(0).lastTimeStamp);
                            }
                            break;
                        }

                    }
                    mongoTemplate.updateFirst(query, update, Dataset.class, TEMP_DATASET_COLLECTION);
                } else {
                    //Create new Dataset
                    Dataset dataset = new Dataset();

                    // Initialize set and add dataset path
                    dataset.datasetPath = new HashSet<>();
                    dataset.datasetPath.add(path);

                    // Initialize digest string and max domain
                    dataset.digestString = metadata.digestString;
                    dataset.maxDomain = metadata.domain;

                    // Initialize list and add digest
                    dataset.digestList = new ArrayList<>();
                    dataset.digestList.add(metadata.digestString);

                    //Initialize first and last time stamp in dataset
                    for(int i = 0; i < metadata.globalAttributes.size(); i++)
                    {
                        var attribute = metadata.globalAttributes.get(i);
                        if(attribute.attributeName.equals("StartDate") || attribute.attributeName.equals("Start_Date")) {
                            if (attribute.type.equals("int")) {
                                ArrayList<Integer> dateList = (ArrayList<Integer>) attribute.value;
                                dataset.firstTimeStamp = new Date(dateList.get(0));
                                dataset.lastTimeStamp = new Date(dateList.get(0));
                            }
                            else {
                                dataset.firstTimeStamp = (Date) attribute.value;
                                dataset.lastTimeStamp = (Date) attribute.value;
                            }
                        }
                    }

                    mongoTemplate.save(dataset, TEMP_DATASET_COLLECTION);
                }
            }

        });

        //For each path, if it exists in main database, update if different
        mongoTemplate.stream(new Query(), Dataset.class, TEMP_DATASET_COLLECTION).forEach( dataset -> {
            Set<String> dataPath = dataset.datasetPath;

            //For each dataset_path in metadata, update corresponding dataset
            for (String path : dataPath) {

                Query query = new Query(Criteria.where("datasetPath").is(path));
                var existingDoc = mongoTemplate.find(query, Dataset.class, DATASET_COLLECTION);

                if(!existingDoc.isEmpty()) { //If doc exists, compare digestString
                    //If digestString update, remove from old dataset add to new dataset
                    //If the same do nothing
                    if(!existingDoc.get(0).digestString.equals(dataset.digestString))
                    {
                        Update update = new Update();

                        // We remove the dataset path from the old dataset
                        existingDoc.get(0).datasetPath.remove(path);
                        update.set("datasetPath", existingDoc.get(0).datasetPath);

                        // If the dataset no longer has any paths, delete it
                        if (existingDoc.get(0).datasetPath.size() == 0) {
                            mongoTemplate.remove(new Query(Criteria.where("digestString").is(existingDoc.get(0).digestString)), DATASET_COLLECTION);
                        }
                        else {
                            mongoTemplate.updateFirst(query, update, Dataset.class, DATASET_COLLECTION);
                        }

                        upsertDatasetPath(dataset, path);
                    }

                } else {
                    //If document does not exist add it to the collection, using the object but single path
                    //Also need to check if it is a duplicate digestString before creating new object
                    upsertDatasetPath(dataset, path);
                }
            }
        });

        mongoTemplate.dropCollection(TEMP_DATASET_COLLECTION);

        return 0;
    }

    /**
     * Converts a file's (metadata) digestString to a bigInteger through decoding
     * and converting to bigInteger from byte array
     * @param digest Metadata digest string
     * @return BigInteger Construct
     */
    public static BigInteger base64decoding(String digest) {
        return digest.equals("") ? new BigInteger("0") : new BigInteger(Base64.getUrlDecoder().decode(digest));
    }
    /**
     * Converts a BigInteger construct of byte array into a digestString for datasets
     * @param digestBytes New Digest String of Dataset
     * @return Dataset new digest string
     */
    public static String base64encoding(BigInteger digestBytes) {
        return Base64.getUrlEncoder().encodeToString(digestBytes.toByteArray()).substring(0, 16);
    }

    /**
     * Checks if a dataset's digest string already exist, and if it does, adds it to the current object,
     * otherwise creates a new one
     * @param dataset Dataset object being searched
     * @param path Current path being checked
     * @throws MongoWriteException
     */
    public void upsertDatasetPath(Dataset dataset, String path) throws MongoWriteException {

        Query query = new Query(Criteria.where("digestString").is(dataset.digestString));
        var existingDoc = mongoTemplate.find(query, Dataset.class, DATASET_COLLECTION);

        if(!existingDoc.isEmpty()) {
            // Update new document with corresponding digest String with new path
            Update update = new Update();

            existingDoc.get(0).datasetPath.add(path);
            update.set("datasetPath", existingDoc.get(0).datasetPath);

            mongoTemplate.updateFirst(query, update, Dataset.class, DATASET_COLLECTION);
        }
        else
        {
            //Create new document for the updated document
            //Copy over all the data Todo can be copy constructor?
            Dataset new_dataset = new Dataset();
            new_dataset.digestString = dataset.digestString;
            new_dataset.digestList = dataset.digestList;
            new_dataset.maxDomain = dataset.maxDomain;
            new_dataset.firstTimeStamp = dataset.firstTimeStamp;
            new_dataset.lastTimeStamp = dataset.lastTimeStamp;

            //Save new set of path to be only the current one
            new_dataset.datasetPath = new HashSet<>();
            new_dataset.datasetPath.add(path);
            mongoTemplate.save(new_dataset, DATASET_COLLECTION);
        }
    }

}
