package com.sjsu.wildfirestorage.spring.controller;

import com.sjsu.wildfirestorage.spring.util.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Controller
public class OauthController {
    @Autowired
    private MongoTemplate mongoTemplate;
    public final String USER_COLLECTION = "userData";

    @Value("${custom.expireAfterSeconds:2592000/}")
    private long expireAfterSeconds;

    @PreAuthorize("hasRole('GUEST')")
    @RequestMapping(value = { "/", "/token", "/forbidden", "/home" })

    public String index () { return "index.html"; }

    @PreAuthorize("hasRole('GUEST')")
    @GetMapping("/api/oauth/user")
    public ResponseEntity<String> user(OAuth2User user) {
        Map details = user.getAttributes();
        var name = details.get("login");
        return new ResponseEntity<>(name.toString(), HttpStatus.OK);
    }

    @GetMapping("/api/oauth/userInfo")
    public ResponseEntity<String> info(Principal user) {
        return new ResponseEntity<>(user.toString(), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('GUEST')")
    @GetMapping("/api/oauth/token")
    public ResponseEntity<String> token(OAuth2AuthenticationToken user) {
        String opaqueToken = getOpaqueToken(user);
        return new ResponseEntity<>(opaqueToken, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('GUEST')")
    @GetMapping("/api/oauth/token/regenerate")
    public ResponseEntity<String> tokenRegenerate(OAuth2AuthenticationToken user) {
        String email = user.getPrincipal().getAttribute("email");
        if(email == null) {
            email = user.getPrincipal().getAttribute("login") + "@github";
        }
        Query query = new Query(Criteria.where("email").is(email));
        String opaqueToken = generateToken();
        Update update = new Update().set("token", opaqueToken);
        mongoTemplate.updateFirst(query, update, "userData");

        return new ResponseEntity<>(opaqueToken, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/api/oauth/checkAccess")
    public ResponseEntity<Boolean> checkAccess() {
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

    public String getOpaqueToken(OAuth2AuthenticationToken user) {
        String name = user.getPrincipal().getAttribute("name");
        String email = UserInfo.getUserId(user);
        Query query = new Query(Criteria.where("email").is(email));
        var opaqueTokenMap = mongoTemplate.find(query, Map.class, USER_COLLECTION);
        String opaqueToken = null;
        if(!opaqueTokenMap.isEmpty()) {
            opaqueToken = (String) opaqueTokenMap.get(0).get("token");
        }
        else {
            opaqueToken = generateToken();
            Map<String, Object> map = new HashMap<>();
            map.put("name", name);
            map.put("email", email);
            map.put("token", opaqueToken);
            map.put("expiry", Instant.now().plusSeconds(expireAfterSeconds));
            map.put("role", "ROLE_GUEST");
            mongoTemplate.insert(map, USER_COLLECTION);
        }
        return opaqueToken;
    }

    private String generateToken() {
        Random random = ThreadLocalRandom.current();
        byte[] randomBytes = new byte[32];
        random.nextBytes(randomBytes);
        String opaqueToken = Base64.getUrlEncoder().encodeToString(randomBytes);
        return opaqueToken;
    }
}

