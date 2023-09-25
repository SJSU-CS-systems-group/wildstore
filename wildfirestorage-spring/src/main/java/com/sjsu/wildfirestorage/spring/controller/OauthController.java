package com.sjsu.wildfirestorage.spring.controller;

import com.sjsu.wildfirestorage.spring.util.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import java.security.Principal;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Controller
public class OauthController {

    @Autowired
    private MongoTemplate mongoTemplate;
    public final String USER_COLLECTION = "userData";
    @GetMapping("/")
    public String index () { return "index.html"; }

    @GetMapping("/user")
    public ResponseEntity<String> user(Principal user) {
        return new ResponseEntity<>(user.getName(), HttpStatus.OK);
    }

    @GetMapping("/token")
    public ResponseEntity<String> token(@RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client, Principal user) {
        String name = user.getName();
        Query query = new Query(Criteria.where("name").is(name));
        var opaqueTokenMap = mongoTemplate.find(query, Map.class, USER_COLLECTION);
        String opaqueToken = null;
        if(!opaqueTokenMap.isEmpty()) {
//            var opaqueTokenMap = mongoTemplate.find(query, Map.class, USER_COLLECTION);
            opaqueToken = (String) opaqueTokenMap.get(0).get("token");
        }
        else {
            Random random = ThreadLocalRandom.current();
            byte[] randomBytes = new byte[32];
            random.nextBytes(randomBytes);
            opaqueToken = Base64.getUrlEncoder().encodeToString(randomBytes);
            mongoTemplate.insert(Map.of("name", name, "token", opaqueToken), USER_COLLECTION);
        }
        return new ResponseEntity<>("token=" + opaqueToken, HttpStatus.OK);
    }
}

