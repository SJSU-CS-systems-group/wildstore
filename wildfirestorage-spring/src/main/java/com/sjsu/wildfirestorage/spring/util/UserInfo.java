package com.sjsu.wildfirestorage.spring.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class UserInfo {

    private static MongoTemplate mongoTemplate;

    @Autowired
    public void setMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    public static final String USER_COLLECTION = "userData";

    public static boolean tokenExist(String input) {
        Query query = new Query(Criteria.where("token").is(input));
        var opaqueTokenMap = mongoTemplate.find(query, Map.class, USER_COLLECTION);
        if (!opaqueTokenMap.isEmpty()) {
            return true;
        }
        return false;
    }

    public static Map getUser (String token) {
        Query query = new Query(Criteria.where("token").is(token));
        var opaqueTokenMap = mongoTemplate.find(query, Map.class, USER_COLLECTION);
        if (!opaqueTokenMap.isEmpty()) {
            return opaqueTokenMap.get(0);
        }
        return null;
    }
}
