package edu.sjsu.wildstore.meta.service;

import com.mongodb.DBObject;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static edu.sjsu.wildstore.meta.controller.OauthController.generateToken;

@Service
public class UserService {

    MongoTemplate mongoTemplate;
    public UserService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<Map> getUserList() {
        Query query = new Query();
        query.fields().exclude("token");
        return mongoTemplate.find(query, DBObject.class, "userData").stream().map(DBObject::toMap).toList();
    }

    public boolean deleteUser(String email) {
        Query query = new Query(Criteria.where("email").is(email));
        var result = mongoTemplate.remove(query, "userData");
        return result.getDeletedCount() > 0;
    }

    public List<Map> getUser(String email) {
        Query query = new Query(Criteria.where("email").is(email));
        var result = mongoTemplate.find(query, DBObject.class, "userData");
        return result.stream().map(DBObject::toMap).toList();
    }

    public boolean updateUserRole(String email, String role) {
        Query query = new Query(Criteria.where("email").is(email));
        Update update = new Update().set("role", role);
        var result = mongoTemplate.upsert(query, update, "userData");
        var upsertId = result.getUpsertedId();

        if (upsertId != null) {
            // Inserted: fetch by the new ID
            Query idQuery = new Query(Criteria.where("_id").is(upsertId.asObjectId().getValue()));
            // we don't have a name, so just use the first part of the email
            var idUpdate = new Update().set("token", generateToken()).set("name", email.split("@")[0]);
            mongoTemplate.updateFirst(idQuery, idUpdate, "userData");
            return true;
        } else {
            return result.getModifiedCount() > 0;
        }
    }
}
