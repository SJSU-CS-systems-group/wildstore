package com.sjsu.wildfirestorage.spring.controller;

import com.mongodb.DBObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/userlist")
public class UsersController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/")
    public List<DBObject> getUserList() {
        Query query = new Query(Criteria.where("role").ne("ROLE_ADMIN"));
        query.fields().exclude("token");
        List<DBObject> res = mongoTemplate.find(query, DBObject.class, "userData");
        return res;
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/")
    public boolean updateUserRole(@RequestBody Map<String, String> request) {
        if(request.get("newRole") != "ROLE_ADMIN") {
            try {
                Query query = new Query(Criteria.where("email").is(request.get("userEmail")));
                Update update = new Update().set("role", request.get("newRole"));
                mongoTemplate.updateFirst(query, update, "userData");
                return true;
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                return false;
            }
        }
        return false;
    }
}

