package edu.sjsu.wildstore.wildstore_relationalDb;

import edu.sjsu.wildstore.Metadata;
import edu.sjsu.wildstore.FilterMetadataRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MetadataController {
  @Autowired
  private FileNodeService fileNodeService;

  @Autowired
  private UserService userService;

  public List<Metadata> filterResults(String userEmail, List<Metadata> metadataResults) throws Exception {
      Long userId = userService.findOrCreate(userEmail).getId();
      if (userService.isAdmin(userId)) {
        return metadataResults;
      }
      Set<String> resultDigests = new HashSet<String>();
      for (Metadata result : metadataResults) {
        resultDigests.add(result.digestString);
      }
      List<FileNode> resultsFileNodes = fileNodeService.findByDigestIn(resultDigests);
      
      Set<String> digestsUserHasAccess = resultsFileNodes.stream()
          .filter(fileNode -> {
            try{
              return fileNodeService.userCanAccessFileNode(fileNode.getId(), userId);
            } catch (Exception e) {
              return false;
            }
          })
          .map(fileNode -> fileNode.getDigest())
          .collect(Collectors.toSet());

      List<Metadata> filteredMetadataResults = metadataResults.stream()
          .filter(metadata -> digestsUserHasAccess.contains(metadata.digestString))
          .collect(Collectors.toList());
      return filteredMetadataResults;
  }

  @PostMapping("/metadata")
  public ResponseEntity<List<Metadata>> filterMetadataByPermissions(@RequestBody FilterMetadataRequest filterMetadataRequest) {
    List<Metadata> metadataResults = filterMetadataRequest.metadataResults;
    String userEmail = filterMetadataRequest.userEmail;
    try {
      List<Metadata> filteredMetadataResults = filterResults(userEmail, metadataResults);
      return ResponseEntity.ok(filteredMetadataResults);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to filter Metadata", e);
    }
  }

  @PostMapping("/metadata/count")
  public ResponseEntity<Long> countMetadataByPermissions(@RequestBody FilterMetadataRequest filterMetadataRequest) {
    List<Metadata> metadataResults = filterMetadataRequest.metadataResults;
    String userEmail = filterMetadataRequest.userEmail;
    try {
      Long filteredMetadataResultsCount = (long) filterResults(userEmail, metadataResults).size();
      return ResponseEntity.ok(filteredMetadataResultsCount);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to count Metadata", e);
    }
  }
}
