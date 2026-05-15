package edu.sjsu.wildstore.meta.controller;

import com.mongodb.DBObject;
import com.mongodb.MongoWriteException;
import edu.sjsu.wildstore.Metadata;
import edu.sjsu.wildstore.MetadataRequest;
import edu.sjsu.wildstore.meta.CriteriaBuilder;
import edu.sjsu.wildstore.meta.MongoCollections;
import net.sf.jsqlparser.JSQLParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class MetadataController {

    @Autowired
    private MongoTemplate mongoTemplate;

    public final String METADATA_COLLECTION = MongoCollections.METADATA;

    @Value("classpath:static/variableDescriptions.json")
    Resource variableResourceFile;

    @Value("classpath:static/attributeDescriptions.json")
    Resource attributeResourceFile;

    /**
     * Searches metadata documents corresponding to the query
     *
     * @param request A request object that contains the search query
     * @return a list of matching Metadata documents
     * @throws JSQLParserException
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/metadata/search")
    public List<Metadata> search(@RequestBody MetadataRequest request) throws JSQLParserException {
        Query query = new Query();
        Criteria criteria = CriteriaBuilder.buildFromSQL(request.searchQuery);
        if (criteria != null) {
            query.addCriteria(criteria);
        }
        query.limit(request.limit);
        query.skip(request.offset);
        if (request.includeFields != null) {
            query.fields().include(request.includeFields);
        }
        if (request.excludeFields != null) {
            query.fields().exclude(request.excludeFields);
        }
        var res = mongoTemplate.find(query, Metadata.class);
        return res;
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/metadata/search/count")
    public long searchCount(@RequestBody MetadataRequest request) throws JSQLParserException {
        Query query = new Query();
        Criteria criteria = CriteriaBuilder.buildFromSQL(request.searchQuery);
        if (criteria != null) {
            query.addCriteria(criteria);
        }
        if (request.excludeFields != null) {
            query.fields().exclude(request.excludeFields);
        }
        return mongoTemplate.count(query, Metadata.class);
    }

    @PreAuthorize("hasRole('GUEST')")
    @GetMapping("/metadata/{digestString}")
    public DBObject getMetadataByDigest(@PathVariable String digestString) {
        Query query = new Query(Criteria.where("digestString").is(digestString));
        List<DBObject> res = mongoTemplate.find(query, DBObject.class, METADATA_COLLECTION);
        return res.isEmpty() ? null : res.get(0);
    }

    /**
     * Searches Metadata documents where filename matches
     *
     * @param fileName the file whose metadata is to be retrieved
     * @return Metadata related to the filename
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/metadata")
    public List<DBObject> getFileMetadata(@RequestParam("filename") String fileName) {
        Query query = new Query(Criteria.where("fileName").regex(".*" + Pattern.quote(fileName) + ".*"));
        List<DBObject> res = mongoTemplate.find(query, DBObject.class, METADATA_COLLECTION);
        return res;
    }

    /**
     * @param metadata The metadata document to be inserted
     * @return 0 on success
     * @throws MongoWriteException
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/metadata")
    public boolean upsertMetadata(@RequestBody Metadata metadata) throws MongoWriteException {
        // if metadata with same fileName and digestString already exist, we don't need to update anything
        if (metadata.fileName != null && !metadata.fileName.isEmpty() && removeFilenames(metadata.fileName.iterator().next(), metadata.digestString)) {
                return false;
        }
        // Convert string date to date type to allow querying on dates
        metadata.globalAttributes.forEach(attr -> {
            if (attr.type.equals("Date")) {
                try {
                    attr.value = new Date((long) attr.value);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        Query query = new Query(Criteria.where("digestString").is(metadata.digestString));
        query.fields().exclude("variables", "globalAttributes");
        var existingDoc = mongoTemplate.find(query, Metadata.class);
        if (!existingDoc.isEmpty()) {
            existingDoc.get(0).fileName.addAll(metadata.fileName);
            existingDoc.get(0).filePath.addAll(metadata.filePath);
            Update update = new Update().set("fileName", existingDoc.get(0).fileName)
                    .set("filePath", existingDoc.get(0).filePath);
            if (existingDoc.get(0).fileType != null && metadata.fileType != null) {
                existingDoc.get(0).fileType.addAll(metadata.fileType);
                update.set("fileType", existingDoc.get(0).fileType);
            } else if (metadata.fileType != null) {
                update.set("fileType", metadata.fileType);
            }
            update.max("lastModified", metadata.lastModified);
            mongoTemplate.updateFirst(query, update, Metadata.class);
            return false;
        } else {
            mongoTemplate.save(metadata, METADATA_COLLECTION);
            return true;
        }
    }

    private boolean removeFilenames(String fileName, String digestString) {
        var dataExist = false;
        Query query = new Query(Criteria.where("fileName").regex(".*" + Pattern.quote(fileName) + ".*"));
        List<Metadata> res = (List<Metadata>) mongoTemplate.find(query, Metadata.class, METADATA_COLLECTION);
        for (Metadata data : res) {
            if (data.digestString.equals(digestString)) {
                dataExist = true;
                continue;
            }
            Query remove = new Query(Criteria.where("digestString").is(data.digestString));
            if (data.fileName.size() == 1) {
                mongoTemplate.remove(remove, METADATA_COLLECTION);
            } else {
                Update update = new Update().pull("fileName", fileName)
                        .pull("filePath", fileName.substring(0, fileName.lastIndexOf('/') + 1));
                mongoTemplate.updateFirst(query, update, METADATA_COLLECTION);
            }
        }
        return dataExist;
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/metadata/filenames")
    public List<Map<String,? extends Serializable>> getFileNames(@RequestParam("limit") int limit, @RequestParam("offset") int offset) {
        Aggregation aggregation = Aggregation.newAggregation(Aggregation.skip(offset),
                                                             Aggregation.limit(limit),
                                                             Aggregation.unwind("fileName"),
                                                             Aggregation.project("fileName", "lastModified"));
        List<DBObject> res = mongoTemplate.aggregate(aggregation, MongoCollections.METADATA, DBObject.class).getMappedResults();
        if (res.isEmpty()) {
            return List.of();
        }
        return res.stream()
                .filter(dbObject -> dbObject.get("fileName") instanceof String)
                .map(dbObject -> {
            String fileName = (String) dbObject.get("fileName");
            var lastModifiedObject = dbObject.get("lastModified");
            return Map.of("fileName", fileName,
                          "lastModified", lastModifiedObject instanceof Long ? (Long)lastModifiedObject : Long.MIN_VALUE);
        }).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/metadata/filenames")
    public int deleteMetadata(@RequestBody List<String> fileNames) {
        Query query = new Query(Criteria.where("fileName").in(fileNames));
        return (int) mongoTemplate.remove(query, METADATA_COLLECTION).getDeletedCount();
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/metadata/description")
    public Map getDescriptions() throws IOException {
        return Map.of("variables",
                      variableResourceFile.getContentAsString(Charset.defaultCharset()),
                      "attributes",
                      attributeResourceFile.getContentAsString(Charset.defaultCharset()));
    }
}

