package edu.sjsu.wildstore.meta.controller;

import java.util.List;
import edu.sjsu.wildstore.FilePermissionsParams;

import java.util.Arrays;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
//import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.sjsu.wildstore.wildstore_relationalDb.records.FileNodeRecord;

@RestController
public class FileNodeBridgeController {

    @Value("${custom.sqlServerUrl}")
    private String sqlServerUrl;

    private final RestClient restClient = RestClient.create();

    private String getUserEmail(OAuth2AuthenticationToken auth) throws IllegalArgumentException {
        OAuth2User principal = auth.getPrincipal();
        String provider = auth.getAuthorizedClientRegistrationId();
        String userEmail = "";
        switch (provider.toLowerCase()) {
            case "google":
                userEmail = principal.getAttribute("email");
                break;
            case "github":
                userEmail = principal.getAttribute("email");
                if (userEmail == null) {
                    userEmail = principal.getAttribute("login") + "@github";
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported provider: " + provider);
        }
        return userEmail;
    }

    @PreAuthorize("hasRole('GUEST')")
    @GetMapping("/file_contents")
    public ResponseEntity<List<FileNodeRecord>> getFileNodes(@RequestParam(value = "file_id", defaultValue = "0") String fileId, OAuth2AuthenticationToken auth) {
        try {
            String userEmail = getUserEmail(auth);
            String url = UriComponentsBuilder.fromHttpUrl(sqlServerUrl)
                    .path("/file_contents")
                    .queryParam("file_id", fileId)
                    .queryParam("user_email", userEmail)
                    .toUriString();

            FileNodeRecord[] response = restClient.get().uri(url).retrieve().body(FileNodeRecord[].class);
            List<FileNodeRecord> responseList = Arrays.asList(response);

            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to load FileNodeController data", e);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/file/share")
    public ResponseEntity<Void> shareFileNodes(@RequestBody FilePermissionsParams filePermissionsParams, OAuth2AuthenticationToken auth) {
        try {
            String adminEmail = getUserEmail(auth);

            String url = UriComponentsBuilder.fromHttpUrl(sqlServerUrl)
                    .path("/file/share")
                    .toUriString();

            filePermissionsParams.adminEmail = adminEmail;

            return restClient.post().uri(url).contentType(MediaType.APPLICATION_JSON).body(filePermissionsParams).retrieve().toBodilessEntity();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to Post FilePermission data", e);
        }
    }
}
