package com.sjsu.wildfirestorage.spring.controller;

import com.mongodb.MongoWriteException;
import com.sjsu.wildfirestorage.Dataset;
import com.sjsu.wildfirestorage.Metadata;
import com.sjsu.wildfirestorage.WildfireAttribute;
import com.sjsu.wildfirestorage.WildfireVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api")
public class DatasetController {

    @Autowired
    private MongoTemplate mongoTemplate;
    public final String METADATA_COLLECTION = "metadata";
    public final String DATASET_COLLECTION = "dataset";

    private final Path datasetCreationLog = Paths.get("DatasetCreation.log");

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/dataset")
    public int upsertDataset() throws MongoWriteException {
        AggregationOptions options = AggregationOptions.builder().allowDiskUse(true).build();
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.unwind("filePath"),
                Aggregation.sort(Sort.Direction.ASC, "filePath")
        ).withOptions(options);

        AtomicReference<String> currentFilePath = new AtomicReference<>("");
        final Dataset[] currentDataset = {new Dataset()};

        try {
            Files.createFile(datasetCreationLog);
        } catch (FileAlreadyExistsException e) {
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        mongoTemplate.aggregateStream(aggregation, METADATA_COLLECTION, Metadata.class).forEach(metadata -> {

            for(String path : metadata.filePath)
            {
                String tempPath = String.valueOf(currentFilePath);

                if(!path.equals(tempPath))
                {
                    //Search the collection and update the current dataset
                    if(currentDataset[0].digestString != null) {
                        checkUpdate(currentDataset[0], tempPath);
                    }
                    //Update new current path
                    currentFilePath.set(path);

                    //Create new Dataset
                    currentDataset[0] = new Dataset();

                    // Initialize set and add dataset path
                    currentDataset[0].datasetPath = new HashSet<>();
                    currentDataset[0].datasetPath.add(path);

                    // Initialize digest string and max domain
                    currentDataset[0].digestString = metadata.digestString;
                    currentDataset[0].maxDomain = metadata.domain;

                    // Initialize list and add digest
                    currentDataset[0].digestList = new ArrayList<>();
                    currentDataset[0].digestList.add(metadata.digestString);

                    //Initialize first and last time stamp in dataset
                    for(int i = 0; i < metadata.globalAttributes.size(); i++)
                    {
                        var attribute = metadata.globalAttributes.get(i);
                        if(attribute.attributeName.equals("StartDate") || attribute.attributeName.equals("Start_Date")) {
                            if (attribute.type.equals("int")) {
                                ArrayList<Integer> dateList = (ArrayList<Integer>) attribute.value;
                                currentDataset[0].firstTimeStamp = new Date(dateList.get(0));
                                currentDataset[0].lastTimeStamp = new Date(dateList.get(0));
                            }
                            else {
                                currentDataset[0].firstTimeStamp = (Date) attribute.value;
                                currentDataset[0].lastTimeStamp = (Date) attribute.value;
                            }
                        }
                    }
                }
                else
                {
                    BigInteger digest = base64decoding(currentDataset[0].digestString);
                    currentDataset[0].digestString = base64encoding(digest.add(base64decoding(metadata.digestString)));
                    //Update digestList
                    currentDataset[0].digestList.add(metadata.digestString);
                    //Update maxDomain
                    if(currentDataset[0].maxDomain < metadata.domain) {
                        currentDataset[0].maxDomain = metadata.domain;
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
                            //Update  time stamps if null
                            if (currentDataset[0].firstTimeStamp == null) {
                                currentDataset[0].firstTimeStamp = tempDate;
                            }
                            if (currentDataset[0].lastTimeStamp == null) {
                                currentDataset[0].lastTimeStamp = tempDate;
                            }
                            //Update time stamps if there is corresponding earlier or later one
                            if (currentDataset[0].firstTimeStamp.compareTo(tempDate) > 0) {
                                currentDataset[0].firstTimeStamp = tempDate;
                            }
                            else if (currentDataset[0].lastTimeStamp.compareTo(tempDate) < 0) {
                                currentDataset[0].lastTimeStamp = tempDate;
                            }
                            break;
                        }

                    }
                }
            }
        });

        if(currentDataset[0].digestString != null) {
            checkUpdate(currentDataset[0], String.valueOf(currentFilePath));
        }

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
     * Check the collection to see if the dataset needs to be updated. If the document already exist but
     * different digestString, remove path from the current document, and create or update a document.
     * @param dataset Dataset to be checked and updated
     * @param path Path of the current dataset
     */
    public void checkUpdate(Dataset dataset, String path)
    {
        Query query = new Query(Criteria.where("datasetPath").is(path));
        List<Dataset> existingDoc = mongoTemplate.find(query, Dataset.class, DATASET_COLLECTION);

        if (!existingDoc.isEmpty()) { //If doc exists, compare digestString
            //If digestString update, remove from old dataset add to new dataset
            //If the same do nothing
            if (!existingDoc.get(0).digestString.equals(dataset.digestString)) {
                Update update = new Update();

                // We remove the dataset path from the old dataset
                existingDoc.get(0).datasetPath.remove(path);

                // If the dataset no longer has any paths, delete it
                if (existingDoc.get(0).datasetPath.size() == 0) {
                    try {
                        Files.writeString(datasetCreationLog, "Removed file with Digest String, "  + existingDoc.get(0).digestString +
                                ", and replaced with file digest string, " +dataset.digestString + "\n", StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        System.out.println("Failed to write to DatasetCreation Log");
                        throw new RuntimeException(e);
                    }
                    mongoTemplate.remove(new Query(Criteria.where("digestString").is(existingDoc.get(0).digestString)), DATASET_COLLECTION);
                } else {
                    update.set("datasetPath", existingDoc.get(0).datasetPath);
                    mongoTemplate.updateFirst(query, update, Dataset.class, DATASET_COLLECTION);
                }

                upsertDatasetPath(dataset, String.valueOf(dataset));
            }
        } else {
            //If document does not exist add it to the collection, using the object but single path
            //Also need to check if it is a duplicate digestString before creating new object
            upsertDatasetPath(dataset, path);
        }
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
            // Update existing document with corresponding digest String with new path
            Update update = new Update();

            existingDoc.get(0).datasetPath.add(path);
            update.set("datasetPath", existingDoc.get(0).datasetPath);

            mongoTemplate.updateFirst(query, update, Dataset.class, DATASET_COLLECTION);
        }
        else
        {
            //Create new document for the updated document
            mongoTemplate.save(dataset, DATASET_COLLECTION);
        }
    }

}
