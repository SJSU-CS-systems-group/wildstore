package edu.sjsu.wildstore.meta.controller;

import com.mongodb.DBObject;
import edu.sjsu.wildstore.Download;
import edu.sjsu.wildstore.Metadata;
import edu.sjsu.wildstore.ShareLink;
import edu.sjsu.wildstore.meta.MongoCollections;
import edu.sjsu.wildstore.meta.util.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/share-link")
public class ShareLinkController {

    Logger logger = LoggerFactory.getLogger(ShareLinkController.class);

    public final String USER_DATA_COLLECTION = MongoCollections.USER_DATA;
    public final String METADATA_COLLECTION = MongoCollections.METADATA;
    public final String SHARE_LINKS_COLLECTION = MongoCollections.SHARE_LINKS;

    @Value("${custom.fileServer}")
    private String fileServerUrl;

    @Autowired
    private MongoTemplate mongoTemplate;

    public static class CreatedLinks {
        public List<String> created;
        public List<String> missing;

        public CreatedLinks(List<String> created, List<String> missing) {
            this.created = created;
            this.missing = missing;
        }
    }

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/create")
    public CreatedLinks create(@RequestBody Map<String, Object> request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Query query = new Query();
        var listOfCriteria = new ArrayList<Criteria>();
        var fileNames = (List<String>) request.get("fileNames");
        var fileDigests = (List<String>) request.get("fileDigest");
        if (fileNames != null) {
            listOfCriteria.add(Criteria.where("fileName").in(fileNames));
        } else {
            fileNames = new ArrayList<>();
        }
        if (fileDigests != null) {
            listOfCriteria.add(Criteria.where("digestString").in((fileDigests)));
        }
        query.addCriteria(new Criteria().orOperator(listOfCriteria));
        query.fields().exclude("variables", "globalAttributes");
        List<Metadata> res = mongoTemplate.find(query, Metadata.class, METADATA_COLLECTION);
        if (!res.isEmpty()) {
            Map<String, Metadata> existingDigests = res.stream().collect(Collectors.toMap(m -> m.digestString, m -> m));
            Query linkQuery = new Query(Criteria.where("fileDigest").in(existingDigests.keySet()));
            linkQuery.addCriteria(Criteria.where("createdBy").is(UserInfo.getUserEmail(SecurityContextHolder.getContext().getAuthentication())));
            linkQuery.addCriteria(Criteria.where("emailAddresses").all(request.get("emailAddresses")));
            linkQuery.addCriteria(Criteria.where("expiry").gt(LocalDateTime.now()));
            List<ShareLink> existing = mongoTemplate.find(linkQuery, ShareLink.class, SHARE_LINKS_COLLECTION);

            List<String> finalShareLinks = new ArrayList<>();
            if (!existing.isEmpty()) {
                for (ShareLink sl : existing) {
                    var newExpiry = computeExpiry((String) request.get("validFor"));
                    if (newExpiry.isAfter(sl.expiry)) {
                        sl.expiry = newExpiry;
                        mongoTemplate.save(sl, SHARE_LINKS_COLLECTION);
                    }
                    var queryName = getQueryName(res, sl.fileDigest);
                    finalShareLinks.add(fileServerUrl + "/api/share/" + sl.shareId + queryName);
                    removeFileName(fileNames, queryName.substring("?filename=".length()));
                    existingDigests.remove(sl.fileDigest);
                }
            }

            List<ShareLink> linksToInsert = new ArrayList<>();
            if (!existingDigests.isEmpty()) {
                for (String digest : existingDigests.keySet()) {
                    ShareLink shareLink = new ShareLink();
                    shareLink.fileDigest = digest;
                    shareLink.filePath = existingDigests.get(digest).filePath;
                    shareLink.createdBy = UserInfo.getUserEmail(SecurityContextHolder.getContext().getAuthentication());
                    shareLink.shareId = UUID.randomUUID().toString().replace("-", "");
                    shareLink.createdAt = LocalDateTime.now();
                    shareLink.emailAddresses = new HashSet<String>((ArrayList<String>) request.get("emailAddresses"));
                    shareLink.expiry = computeExpiry((String) request.get("validFor"));
                    linksToInsert.add(shareLink);
                    var queryName = getQueryName(res, digest);
                    finalShareLinks.add(fileServerUrl + "/api/share/" + shareLink.shareId + queryName);
                    // Remove the fileName from the list of fileNames to share so we can return fileName as a list of files not found
                    removeFileName(fileNames, queryName.substring("?filename=".length()));
                }
                mongoTemplate.insert(linksToInsert, SHARE_LINKS_COLLECTION);
            }
            return new CreatedLinks(finalShareLinks, fileNames);
        } else {
            return new CreatedLinks(Collections.emptyList(), fileNames);
        }
    }

//    @PreAuthorize("hasRole('USER')")
//    @PostMapping("/create")
//    public String createFromWildcard(@RequestBody String filePath) {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        Query linkQuery = new Query(Criteria.where("filePath").is(filePath));
//        List<ShareLink> existing = mongoTemplate.find(linkQuery, ShareLink.class, SHARE_LINKS_COLLECTION);
//        if(!existing.isEmpty()) {
//            return fileServerUrl + "/api/share/" + existing.get(0).shareId;
//        }
//
//        String regex = WildcardToRegex.wildcardToRegex(filePath);
//        List<Metadata> res = mongoTemplate.find(query, Metadata.class, METADATA_COLLECTION);
//        if(!res.isEmpty()) {
//            Query linkQuery = new Query(Criteria.where("fileDigest").is(res.get(0).digestString));
//            linkQuery.addCriteria(Criteria.where("createdBy").is(UserInfo.getUserName(SecurityContextHolder.getContext().getAuthentication())));
//            List<ShareLink> existing = mongoTemplate.find(linkQuery, ShareLink.class, SHARE_LINKS_COLLECTION);
//            if(!existing.isEmpty()) {
//                return fileServerUrl + "/api/share/" + existing.get(0).shareId;
//            }
//            ShareLink shareLink = new ShareLink();
//            shareLink.fileDigest = res.get(0).digestString;
//            shareLink.createdBy = UserInfo.getUserName(SecurityContextHolder.getContext().getAuthentication());
//            shareLink.shareId = UUID.randomUUID().toString().replace("-", "");
//            shareLink.createdAt = LocalDateTime.now();
//            mongoTemplate.insert(shareLink, "share-links");
//            return fileServerUrl + "/api/share/" + shareLink.shareId;
//        } else {
//            return "FILE_NOT_FOUND";
//        }
//    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/")
    public List<ShareLink> getShareLinkList(Authentication authentication,
                                            @RequestParam(defaultValue = "100") int limit,
                                            @RequestParam(defaultValue = "0") int offset) {
        // old sharelinks are created with username in field createdBy
        Query query = new Query(new Criteria().orOperator(Criteria.where("createdBy").is(UserInfo.getUserEmail(SecurityContextHolder.getContext().getAuthentication())),
                                                          Criteria.where("createdBy").is(UserInfo.getUserName(SecurityContextHolder.getContext().getAuthentication()))));
        query.limit(limit);
        query.skip(offset);
        List<ShareLink> res = mongoTemplate.find(query, ShareLink.class, SHARE_LINKS_COLLECTION);
        return res;
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/count")
    public long getShareLinkCount() {
        // old sharelinks are created with username in field createdBy
        Query query = new Query(new Criteria().orOperator(Criteria.where("createdBy").is(UserInfo.getUserEmail(SecurityContextHolder.getContext().getAuthentication())),
                                                          Criteria.where("createdBy").is(UserInfo.getUserName(SecurityContextHolder.getContext().getAuthentication()))));
        return mongoTemplate.count(query, SHARE_LINKS_COLLECTION);
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{shareId}")
    public boolean deleteShareLink(@PathVariable String shareId, Authentication authentication) {
        // old sharelinks are created with shareId and username in field createdBy
        // new sharelinks are created with _id and email in field createdBy
        Query query = new Query(new Criteria().andOperator(new Criteria().orOperator(Criteria.where("_id").is(shareId),
                                                                                     Criteria.where("shareId")
                                                                                             .is(shareId)),
                                                           new Criteria().orOperator(Criteria.where("createdBy")
                                                                                             .is(UserInfo.getUserEmail(SecurityContextHolder.getContext().getAuthentication())),
                                                                                     Criteria.where("createdBy")
                                                                                             .is(UserInfo.getUserName(SecurityContextHolder.getContext().getAuthentication())))
        ));
        return mongoTemplate.remove(query, SHARE_LINKS_COLLECTION).getDeletedCount() > 0;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/delete")
    public int deleteShareLinks(@RequestBody List<String> shareId) {
        // old sharelinks are created with shareId
        Query query = new Query(new Criteria().orOperator(Criteria.where("_id").in(shareId),
                                                          Criteria.where("shareId").in(shareId)));
        return (int) mongoTemplate.remove(query, SHARE_LINKS_COLLECTION).getDeletedCount();
    }

    @PreAuthorize("hasRole('GUEST')")
    @PostMapping("/verify")
    public DBObject verify(@RequestBody String shareId) {
        String currentUserEmail = UserInfo.getUserEmail(SecurityContextHolder.getContext().getAuthentication());
        System.out.println(currentUserEmail);
        Query query = new Query(Criteria.where("shareId").is(shareId));
        //query.addCriteria(Criteria.where("emailAddresses").in(currentUserEmail));
        //query.addCriteria(Criteria.where("expiry").gt(LocalDateTime.now()));
        List<ShareLink> res = mongoTemplate.find(query, ShareLink.class, SHARE_LINKS_COLLECTION);
        if (res.isEmpty()) {
            logger.info("Verification failed. Share ID not found");
            throw new ResponseStatusException(NOT_FOUND,
                                              String.format("Share ID %s not found for %s", shareId, currentUserEmail));
        }
        logger.info("Verification success");
        Query query2 = new Query(Criteria.where("digestString").is(res.get(0).fileDigest));
        query2.fields().exclude("variables", "globalAttributes");
        List<DBObject> res2 = mongoTemplate.find(query2, DBObject.class, METADATA_COLLECTION);
        logger.info((res.isEmpty() || res2 == null) ? "Digest string not found" : "Success, returning metadata");
        if (res.isEmpty() || res2 == null) {
            throw new ResponseStatusException(NOT_FOUND, "Problem finding file for " + shareId);
        }
        return (res.isEmpty() || res2 == null) ? null : res2.get(0);
    }

    @PreAuthorize("hasRole('GUEST')")
    @PostMapping("/downloadhistory")
    public Integer addDownloadHistory(@RequestBody String shareId,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        Query query = new Query(Criteria.where("shareId").is(shareId));
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String userName;
        if (authorizationHeader != null) {
            Query authQuery = new Query(Criteria.where("token").is(authorizationHeader.substring(7)));
            List<DBObject> authList = mongoTemplate.find(authQuery, DBObject.class, USER_DATA_COLLECTION);
            if (authList.isEmpty()) {
                logger.warn("Token not found in userData collection");
                return 1;
            }
            userName = (String) authList.get(0).get("name");
        } else {
            // if a token wasn't used, see if there is other authentication info
            userName = UserInfo.getUserName(SecurityContextHolder.getContext().getAuthentication());
            if (userName == null) {
                logger.warn("User not authenticated, cannot add download history");
                return 1;
            }
        }
        Download download = new Download();
        download.dateTime = LocalDateTime.now();
        download.downloadedBy = userName;
        Update update = new Update().push("downloads", download);
        mongoTemplate.updateFirst(query, update, SHARE_LINKS_COLLECTION);
        logger.info("Add download history successful");
        return 0;
    }

    private LocalDateTime computeExpiry(String validFor) {
        return switch (validFor) {
            case "day" -> LocalDateTime.now().plusDays(1);
            case "week" -> LocalDateTime.now().plusWeeks(1);
            case "month" -> LocalDateTime.now().plusMonths(1);
            case "year" -> LocalDateTime.now().plusYears(1);
            default -> LocalDateTime.now();
        };
    }

    private String getQueryName(List<Metadata> res, String fileDigest) {
        for (Metadata data : res) {
            if (fileDigest.equals(data.digestString)) {
                var fileName = data.fileName.toString();
                var fileNameOnly = fileName.substring(fileName.lastIndexOf("/") + 1, fileName.length() - 1);
                return "?filename=" + fileNameOnly;
            }
        }
        return "";
    }

    private void removeFileName(List<String> fileNames, String fileName) {
        if (fileNames.isEmpty() || fileName.isEmpty()) return;
        for (String name : fileNames) {
            var nameOnly = name.substring(name.lastIndexOf("/") + 1);
            if (nameOnly.equals(fileName)) {
                fileNames.remove(name);
                return;
            }
        }
    }
}
