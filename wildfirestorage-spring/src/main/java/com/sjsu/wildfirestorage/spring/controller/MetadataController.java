package com.sjsu.wildfirestorage.spring.controller;

import com.mongodb.DBObject;
import com.mongodb.MongoWriteException;
import com.sjsu.wildfirestorage.Metadata;
import com.sjsu.wildfirestorage.MetadataRequest;
import com.sjsu.wildfirestorage.spring.CriteriaBuilder;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;


@RestController
@RequestMapping("/api")
public class MetadataController {

    @Autowired
    private MongoTemplate mongoTemplate;

    public final String METADATA_COLLECTION = "metadata";

    @Value("classpath:static/variableDescriptions.json")
    Resource resourceFile;

    /**
     * Searches metadata documents corresponding to the query
     * @param request A request object that contains the search query
     * @return a list of matching Metadata documents
     * @throws JSQLParserException
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/metadata/search")
    public List<Metadata> search(@RequestBody MetadataRequest request) throws JSQLParserException {
        Query query = new Query();
        Criteria criteria = CriteriaBuilder.buildFromSQL(request.searchQuery);
        if(criteria != null) {
            query.addCriteria(criteria);
        }
        query.limit(request.limit);
        query.skip(request.offset);
        if(request.includeFields != null) {
            query.fields().include(request.includeFields);
        }
        if(request.excludeFields != null) {
            query.fields().exclude(request.excludeFields);
        }
        var res = mongoTemplate.find(query, Metadata.class);
        return res;
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/metadata/search/count")
    public long searchCount(@RequestBody MetadataRequest request) throws JSQLParserException {
        SecurityContextHolder.getContext().getAuthentication().getAuthorities().forEach(ga -> System.out.println("^^^^^^^^^^^"+ga));
        System.out.println();
        Query query = new Query();
        Criteria criteria = CriteriaBuilder.buildFromSQL(request.searchQuery);
        if(criteria != null) {
            query.addCriteria(criteria);
        }
        if(request.excludeFields != null) {
            query.fields().exclude(request.excludeFields);
        }
        return mongoTemplate.count(query, Metadata.class);
    }

    @PreAuthorize("hasRole('GUEST')")
    @GetMapping("/metadata/{digestString}")
    public DBObject getMetadataByDigest(@PathVariable String digestString) {
        Query query = new Query(Criteria.where("digestString").is(digestString));
        List<DBObject> res = mongoTemplate.find(query, DBObject.class, METADATA_COLLECTION);
        return res.isEmpty()? null : res.get(0);
    }

    /**
     * Searches Metadata documents where filename matches
     * @param fileName the file whose metadata is to be retrieved
     * @return Metadata related to the filename
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/metadata")
    public List<DBObject> getFileMetadata(@RequestParam("filename") String fileName) {
        Query query = new Query(Criteria.where("fileName").regex(".*"+fileName+".*"));
        List<DBObject> res = mongoTemplate.find(query, DBObject.class, METADATA_COLLECTION);
        return res;
    }

    /**
     *
     * @param metadata The metadata document to be inserted
     * @return 0 on success
     * @throws MongoWriteException
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/metadata")
    public int upsertMetadata(@RequestBody Metadata metadata) throws MongoWriteException {
        // Convert string date to date type to allow querying on dates
        metadata.globalAttributes.forEach(attr -> {
            if(attr.type.equals("Date")) {
                try {
                    attr.value = new Date((long)attr.value);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        Query query = new Query(Criteria.where("digestString").is(metadata.digestString));
        query.fields().exclude("variables", "globalAttributes");
        var existingDoc = mongoTemplate.find(query, Metadata.class);
        if(!existingDoc.isEmpty()) {
            existingDoc.get(0).fileName.addAll(metadata.fileName);
            existingDoc.get(0).filePath.addAll(metadata.filePath);
            Update update = new Update().set("fileName", existingDoc.get(0).fileName)
                .set("filePath", existingDoc.get(0).filePath);
            if(existingDoc.get(0).fileType != null && metadata.fileType != null) {
                existingDoc.get(0).fileType.addAll(metadata.fileType);
                update.set("fileType", existingDoc.get(0).fileType);
            } else if ( metadata.fileType != null ){
                update.set("fileType", metadata.fileType);
            }
            mongoTemplate.updateFirst(query, update, Metadata.class);
        } else {
            mongoTemplate.save(metadata, METADATA_COLLECTION);
        }
        return 0;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/metadata/filepath")
    public List<String> getFilePaths(@RequestParam("limit") int limit, @RequestParam("offset") int offset) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.skip(offset),
                Aggregation.limit(limit),
                Aggregation.unwind("filePath"),
                Aggregation.group().addToSet("filePath").as("files"),
                Aggregation.project("files"));

        List<DBObject> res = mongoTemplate.aggregate(aggregation, "metadata", DBObject.class).getMappedResults();
        if(res.isEmpty()) {
            return List.of();
        }
        List<String> files = (List<String>)res.get(0).get("files");
        return files;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/metadata/filepath")
    public int deleteMetadata(@RequestBody List<String> filePaths) {
        Query query = new Query(Criteria.where("filePath").in(filePaths));
        mongoTemplate.remove(query, METADATA_COLLECTION);
        return 0;
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/metadata/description")
    public String getDescriptions() throws IOException {
        return Files.readString(resourceFile.getFile().toPath());
    }
}

