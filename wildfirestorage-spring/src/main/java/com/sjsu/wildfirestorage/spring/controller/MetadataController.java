package com.sjsu.wildfirestorage.spring.controller;

import com.mongodb.DBObject;
import com.mongodb.MongoWriteException;
import com.sjsu.wildfirestorage.Metadata;
import com.sjsu.wildfirestorage.MetadataRequest;
import com.sjsu.wildfirestorage.spring.CriteriaBuilder;
import net.sf.jsqlparser.JSQLParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;


@RestController
@RequestMapping("/api")
public class MetadataController {

    @Autowired
    private MongoTemplate mongoTemplate;

    public final String METADATA_COLLECTION = "metadata";

    /**
     * Searches metadata documents corresponding to the query
     * @param request A request object that contains the search query
     * @return a list of matching Metadata documents
     * @throws JSQLParserException
     */
    @PostMapping("/metadata/search")
    public List<Metadata> search(@RequestBody MetadataRequest request) throws JSQLParserException {
        Query query = new Query();
        query.addCriteria(CriteriaBuilder.buildFromSQL(request.searchQuery));
        var res = mongoTemplate.find(query, Metadata.class);
        return res;
    }

    /**
     * Searches Metadata documents where filename matches
     * @param fileName the file whose metadata is to be retrieved
     * @return Metadata related to the filename
     */
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
        mongoTemplate.save(metadata, METADATA_COLLECTION);
        return 0;
    }
}

