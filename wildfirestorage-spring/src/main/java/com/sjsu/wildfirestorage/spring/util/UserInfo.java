package com.sjsu.wildfirestorage.spring.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Map;

@Controller
public class UserInfo {

    @Autowired
    private static MongoTemplate mongoTemplate;

    @Autowired
    public void setMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    public static final String USER_COLLECTION = "userData";

    public static boolean tokenExist(String input) {
//        MongoCollection<Document> USER_COLLECTION = mongoTemplate.getCollection("userData");
        Query query = new Query(Criteria.where("token").is(input));
        var opaqueTokenMap = mongoTemplate.find(query, Map.class, USER_COLLECTION);
        if (!opaqueTokenMap.isEmpty()) {
            return true;
        }
        return false;
    }
}
