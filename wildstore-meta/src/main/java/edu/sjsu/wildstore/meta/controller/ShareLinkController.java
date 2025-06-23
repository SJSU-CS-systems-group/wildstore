package edu.sjsu.wildstore.meta.controller;

import com.mongodb.DBObject;
import edu.sjsu.wildstore.Download;
import edu.sjsu.wildstore.Metadata;
import edu.sjsu.wildstore.ShareLink;
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
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/share-link")
public class ShareLinkController {

    Logger logger = LoggerFactory.getLogger(ShareLinkController.class);

    public final String USER_DATA_COLLECTION = "userData";
    public final String METADATA_COLLECTION = "metadata";
    public final String SHARE_LINKS_COLLECTION = "share-links";

    @Value("${custom.fileServer}")
    private String fileServerUrl;

    @Autowired
    private MongoTemplate mongoTemplate;

    @PreAuthorize("hasRole('USER')")
    @PostMapping("/create")
    public String create(@RequestBody Map<String, Object> request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Query query = new Query();
        var listOfCriteria = new ArrayList<Criteria>();
        var fileNamesList = (List<String>) request.get("fileNames");
        var fileNames = fileNamesList == null ? null : new HashSet<>(fileNamesList);
        var fileDigests = (List<String>) request.get("fileDigest");
        if (fileNames != null) {
            listOfCriteria.add(Criteria.where("fileName").in(fileNames));
        }
        if (fileDigests != null) {
            listOfCriteria.add(Criteria.where("digestString").in((fileDigests)));
        }
        if (listOfCriteria.isEmpty()) {
            return "NO_FILE_SPECIFIED";
        }
        query.addCriteria(new Criteria().orOperator(listOfCriteria));
        query.fields().exclude("variables", "globalAttributes");
        List<Metadata> res = mongoTemplate.find(query, Metadata.class, METADATA_COLLECTION);
        if (!res.isEmpty()) {
            Map<String, Metadata> existingDigests = res.stream().collect(Collectors.toMap(m -> m.digestString, m -> m));
            Query linkQuery = new Query(Criteria.where("fileDigest").in(existingDigests.keySet()));
            linkQuery.addCriteria(Criteria.where("createdBy").is(getCurrentUserName()));
            linkQuery.addCriteria(Criteria.where("emailAddresses").all(request.get("emailAddresses")));
            linkQuery.addCriteria(Criteria.where("expiry").gt(LocalDateTime.now()));
            List<ShareLink> existing = mongoTemplate.find(linkQuery, ShareLink.class, SHARE_LINKS_COLLECTION);

            List<String> finalShareLinks = new ArrayList<>();
            if (!existing.isEmpty()) {
                for (ShareLink sl : existing) {
                    var fileName = getFileName(res, fileNames, sl.fileDigest);
                    finalShareLinks.add(fileServerUrl + "/api/share/" + sl.shareId + fileName);
                    removeFileName(fileNamesList, fileName.substring("?filename=".length()));
                    existingDigests.remove(sl.fileDigest);
                }
            }

            List<ShareLink> linksToInsert = new ArrayList<>();
            if (!existingDigests.isEmpty()) {
                for (String digest : existingDigests.keySet()) {
                    ShareLink shareLink = new ShareLink();
                    shareLink.fileDigest = digest;
                    shareLink.filePath = existingDigests.get(digest).filePath;
                    shareLink.createdBy = getCurrentUserName();
                    shareLink.shareId = UUID.randomUUID().toString().replace("-", "");
                    shareLink.createdAt = LocalDateTime.now();
                    shareLink.emailAddresses = new HashSet<String>((ArrayList<String>) request.get("emailAddresses"));
                    switch ((String) request.get("validFor")) {
                        case "day":
                            shareLink.expiry = LocalDateTime.now().plusDays(1);
                            break;
                        case "week":
                            shareLink.expiry = LocalDateTime.now().plusWeeks(1);
                            break;
                        case "month":
                            shareLink.expiry = LocalDateTime.now().plusMonths(1);
                            break;
                        case "year":
                            shareLink.expiry = LocalDateTime.now().plusYears(1);
                            break;
                        default:
                            break;
                    }
                    linksToInsert.add(shareLink);
                    var fileName = getFileName(res, fileNames, digest);
                    finalShareLinks.add(fileServerUrl + "/api/share/" + shareLink.shareId + fileName);
                    removeFileName(fileNamesList, fileName.substring("?filename=".length()));
                }
                mongoTemplate.insert(linksToInsert, "share-links");
            }
            return String.join("\n", finalShareLinks) + (fileNamesList.isEmpty() ? "" : "\nFiles not found:\n" + String.join("\n", fileNamesList));
        } else {
            return "FILE_NOT_FOUND";
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
//            linkQuery.addCriteria(Criteria.where("createdBy").is(getCurrentUserName()));
//            List<ShareLink> existing = mongoTemplate.find(linkQuery, ShareLink.class, SHARE_LINKS_COLLECTION);
//            if(!existing.isEmpty()) {
//                return fileServerUrl + "/api/share/" + existing.get(0).shareId;
//            }
//            ShareLink shareLink = new ShareLink();
//            shareLink.fileDigest = res.get(0).digestString;
//            shareLink.createdBy = getCurrentUserName();
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
    public List<DBObject> getShareLinkList(OAuth2AuthenticationToken oAuth2AuthenticationToken,
                                           @RequestParam(defaultValue = "100") int limit,
                                           @RequestParam(defaultValue = "0") int offset) {
        String email = UserInfo.getUserId(oAuth2AuthenticationToken);
        Query query = new Query(Criteria.where("createdBy").is(getCurrentUserName()));
        query.limit(limit);
        query.skip(offset);
        List<DBObject> res = mongoTemplate.find(query, DBObject.class, "share-links");
        return res;
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/count")
    public long getShareLinkCount(OAuth2AuthenticationToken oAuth2AuthenticationToken) {
        String email = UserInfo.getUserId(oAuth2AuthenticationToken);
        Query query = new Query(Criteria.where("createdBy").is(getCurrentUserName()));
        return mongoTemplate.count(query, "share-links");
    }

    @PreAuthorize("hasRole('USER')")
    @DeleteMapping("/{shareId}")
    public boolean deleteShareLink(@PathVariable String shareId) {
        try {
            Query query = new Query(Criteria.where("shareId").is(shareId));
            mongoTemplate.remove(query, DBObject.class, "share-links");
            return true;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return false;
        }
    }

    @PreAuthorize("hasRole('GUEST')")
    @PostMapping("/verify")
    public DBObject verify(@RequestBody String shareId) {
        String currentUserEmail = getCurrentUserEmail();
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
            userName = getCurrentUserName();
            if (userName == null) {
                logger.warn("User not authenticated, cannot add download history");
                return 1;
            }
        }
        Download download = new Download();
        download.dateTime = LocalDateTime.now();
        download.downloadedBy = userName;
        Update update = new Update().push("downloads", download);
        mongoTemplate.updateFirst(query, update, "share-links");
        logger.info("Add download history successful");
        return 0;
    }

    private String getCurrentUserName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getPrincipal() instanceof DefaultOAuth2User) {
            return (String) ((DefaultOAuth2User) (auth.getPrincipal())).getAttribute("name");
        } else if (auth.getPrincipal() instanceof DefaultOAuth2AuthenticatedPrincipal) {
            return (String) ((DefaultOAuth2AuthenticatedPrincipal) (auth.getPrincipal())).getAttribute("name");
        } else {
            return (String) ((DefaultOidcUser) (auth.getPrincipal())).getAttribute("name");
        }
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth.getPrincipal() instanceof DefaultOAuth2AuthenticatedPrincipal) {
            return ((DefaultOAuth2AuthenticatedPrincipal) (auth.getPrincipal())).getAttribute("email");
        } else if (auth.getPrincipal() instanceof DefaultOAuth2User) {
            return (String) ((DefaultOAuth2User) (auth.getPrincipal())).getAttribute("email");
        } else {
            return (String) ((DefaultOidcUser) (auth.getPrincipal())).getAttribute("email");
        }
    }

    private String getFileName(List<Metadata> res, Set<String> fileNames, String fileDigest) {
        if (fileNames == null) return "";
        for (Metadata data : res) {
            if (fileDigest.equals(data.digestString)) {
                var intersection = new HashSet<>(data.fileName);
                intersection.retainAll(fileNames);
                if (!intersection.isEmpty()) {
                    String fileName = intersection.iterator().next();
                    fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                    return "?filename=" + fileName;
                }
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
