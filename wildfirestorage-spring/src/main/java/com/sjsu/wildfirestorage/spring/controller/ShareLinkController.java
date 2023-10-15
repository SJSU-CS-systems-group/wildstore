package com.sjsu.wildfirestorage.spring.controller;

import com.mongodb.DBObject;
import com.sjsu.wildfirestorage.Download;
import com.sjsu.wildfirestorage.ShareLink;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/share-link")
public class ShareLinkController {

    @Value("${custom.fileServer}")
    private String fileServerUrl;

    @Autowired
    private MongoTemplate mongoTemplate;
    @PostMapping("/create")
    public String create(@RequestBody String fileDigest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ShareLink shareLink = new ShareLink();
        shareLink.fileDigest = fileDigest;
        shareLink.createdBy = (String)((DefaultOidcUser)(auth.getPrincipal())).getAttribute("name");
//        shareLink.createdBy = (String)auth.getPrincipal();
        shareLink.shareId = UUID.randomUUID().toString().replace("-", "");
        mongoTemplate.insert(shareLink, "share-links");
        return fileServerUrl + "/api/share/" + shareLink.shareId;
    }

    @PostMapping("/verify")
    public DBObject verify(@RequestBody String shareId) {
        Query query = new Query(Criteria.where("shareId").is(shareId));
        List<ShareLink> res = mongoTemplate.find(query, ShareLink.class, "share-links");
        if(res.isEmpty()){
            System.out.println("verify failed");
            return null;
        }
        Query query2 = new Query(Criteria.where("digestString").is(res.get(0).fileDigest));
        List<DBObject> res2 = mongoTemplate.find(query2, DBObject.class, "metadata");
        System.out.println("Verfied Success");
        return (res.isEmpty() || res2 == null)? null : res2.get(0);
    }

    @PostMapping("/downloadhistory")
    public Integer addDownloadHistory(@RequestBody String shareId, HttpServletRequest request, HttpServletResponse response) {
        if(!request.getHeader(HttpHeaders.AUTHORIZATION).isEmpty()) {
            Query query = new Query(Criteria.where("shareId").is(shareId));
            Query authQuery = new Query(Criteria.where("token").is(request.getHeader(HttpHeaders.AUTHORIZATION).substring(7)));
            List<DBObject> authList = mongoTemplate.find(authQuery, DBObject.class, "auth-tokens");
            if (authList.isEmpty()) {
                return 1;
            }
            Download download = new Download();
            download.dateTime = LocalDateTime.now();
            download.downloadedBy = (String) authList.get(0).get("username");
            Update update = new Update().push("downloads", download);
            mongoTemplate.updateFirst(query, update, "share-links");
            return 0;
        } else {
            return 2;
        }
    }
}
