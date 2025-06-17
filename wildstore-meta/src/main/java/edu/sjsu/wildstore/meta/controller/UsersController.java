package edu.sjsu.wildstore.meta.controller;

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

import static edu.sjsu.wildstore.meta.controller.OauthController.generateToken;

@RestController
@RequestMapping("/api/userlist")
public class UsersController {

    @Autowired
    private MongoTemplate mongoTemplate;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/")
    public List<Map> getUserList() {
        Query query = new Query();
        query.fields().exclude("token");
        return mongoTemplate.find(query, DBObject.class, "userData")
                .stream()
                .map(DBObject::toMap)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{email}")
    public boolean deleteUser(@PathVariable String email) {
        Query query = new Query(Criteria.where("email").is(email));
        var result = mongoTemplate.remove(query, "userData");
        return result.getDeletedCount() > 0;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{email}")
    public List<Map> getUser(@PathVariable String email) {
        Query query = new Query(Criteria.where("email").is(email));
        var result = mongoTemplate.find(query, DBObject.class, "userData");
        return result.stream().map(DBObject::toMap).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{email}")
    public boolean updateUserRole(@RequestBody Map<String, String> request, @PathVariable String email) {
        var role = request.get("role");

        if (role == null) {
            throw new IllegalArgumentException("Missing role in request");
        }
        if (!SecurityConfiguration.ROLES.contains(role)) {
            throw new IllegalArgumentException(role + " is not one of " + String.join(",", SecurityConfiguration.ROLES));
        }
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

