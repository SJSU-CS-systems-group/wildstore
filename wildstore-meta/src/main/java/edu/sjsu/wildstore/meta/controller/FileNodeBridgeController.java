package edu.sjsu.wildstore.meta.controller;

import java.util.List;

import java.util.Arrays;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
//import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import edu.sjsu.wildstore.wildstore_relationalDb.records.FileNodeRecord;

@RestController
public class FileNodeBridgeController {

    @Value("${custom.sqlServerUrl}")
    private String sqlServerUrl;

    private final RestClient restClient = RestClient.create();

    @PreAuthorize("hasRole('GUEST')")
    @GetMapping("/file_contents")
    public ResponseEntity<List<FileNodeRecord>> getFileNodes(@RequestParam(value = "file_id", defaultValue = "0") String fileId) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(sqlServerUrl)
                    .path("/file_contents")
                    .queryParam("file_id", fileId)
                    .toUriString();

            FileNodeRecord[] response = restClient.get().uri(url).retrieve().body(FileNodeRecord[].class);
            List<FileNodeRecord> responseList = Arrays.asList(response);

            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to load FileNodeController data", e);
        }
    }
}
