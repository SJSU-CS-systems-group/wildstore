package edu.sjsu.wildstore.meta.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
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

    public static Map getUser(String token) {
        Query query = new Query(Criteria.where("token").is(token));
        var opaqueTokenMap = mongoTemplate.find(query, Map.class, USER_COLLECTION);
        if (!opaqueTokenMap.isEmpty()) {
            return opaqueTokenMap.get(0);
        }
        return null;
    }

    public static Map getUserBy(String key, String value) {
        Query query = new Query(Criteria.where(key).is(value));
        var opaqueTokenMap = mongoTemplate.find(query, Map.class, USER_COLLECTION);
        if (!opaqueTokenMap.isEmpty()) {
            return opaqueTokenMap.get(0);
        }
        return null;
    }

    public static String getUserEmail(Authentication user) {
        System.out.println(user.getPrincipal().getClass());
        OAuth2AuthenticatedPrincipal principal = (OAuth2AuthenticatedPrincipal) user.getPrincipal();
        String email = principal.getAttribute("email");
        if (email == null) {
            var githubLogin = principal.getAttribute("login");
            if (githubLogin != null) {
                email = principal.getAttribute("login") + "@github";
            }
        }
        return email;
    }

    public static String getUserName(Authentication user) {
        System.out.println(user.getPrincipal().getClass());
        OAuth2AuthenticatedPrincipal principal = (OAuth2AuthenticatedPrincipal) user.getPrincipal();
        return principal.getAttribute("name");
    }
}
