package com.sjsu.wildfirestorage.spring.controller;

import com.mongodb.DBObject;
import com.mongodb.MongoWriteException;
import com.sjsu.wildfirestorage.Metadata;
import com.sjsu.wildfirestorage.spring.MetadataRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api")
public class MetadataController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostMapping("/metadata/search")
    public List<Metadata> getMetadataWithin(@RequestBody MetadataRequest request) {
        Query query = new Query();
        query.addCriteria(Criteria.where("corner").intersects(request.polygon));
        query.addCriteria(Criteria.where("fileName").in(Arrays.asList("file1")));
        query.addCriteria(Criteria.where("globalAttributes").elemMatch(Criteria.where("attributeName").is("simulationDate").and("value").gt(new Date())));
        return mongoTemplate.find(query, Metadata.class);
    }

    @GetMapping("/metadata")
    public List<DBObject> getFileMetadata(@RequestParam("filename") String fileName) {
        Query query = new Query(Criteria.where("fileName").regex(".*"+fileName+".*"));
        List<DBObject> res = mongoTemplate.find(query, DBObject.class, "metadata");
        return res;
    }

    @PostMapping("/metadata")
    public int upsertMetadata(@RequestBody Metadata metadata) {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

        metadata.globalAttributes.forEach(attr -> {
            if(attr.type.equals("Date")) {
                try {
                    attr.value = isoFormat.parse((String)attr.value);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        try {
            mongoTemplate.save(metadata, "metadata");
            return 0;
        } catch (MongoWriteException e) {
            return 1;
        }
    }
}

