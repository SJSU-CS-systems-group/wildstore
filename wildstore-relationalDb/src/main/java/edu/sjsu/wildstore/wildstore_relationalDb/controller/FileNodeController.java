package edu.sjsu.wildstore.wildstore_relationalDb;

import edu.sjsu.wildstore.wildstore_relationalDb.records.FileNodeRecord;
import edu.sjsu.wildstore.wildstore_relationalDb.records.FileNodeContentsRecord;
import edu.sjsu.wildstore.wildstore_relationalDb.records.FilePermissionRecord;
import edu.sjsu.wildstore.wildstore_relationalDb.records.FileNodePermissionsRecord;

import java.util.List;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class FileNodeController {
  @Autowired
  private FileNodeService fileNodeService;
  @Autowired
  private FilePermissionService filePermissionService;

  @PostMapping("/file_contents")
  public ResponseEntity<Long> createFileNode(@RequestBody FileNodeParams fileNodeParams) {
    FileNode savedFileNode = fileNodeService.saveFileNode(fileNodeParams);
    return new ResponseEntity<>(savedFileNode.id, HttpStatus.CREATED);
  }

  @GetMapping("/file_contents")
  public ResponseEntity<FileNodeContentsRecord> fileNodes(@RequestParam(value="file_id", defaultValue= "0") String fileId, @RequestParam(value="user_email") String userEmail) {
    try {
      Long longFileId = Long.parseLong(fileId);
      if (longFileId == 0) {
        longFileId = null;
      }
      if (longFileId != null && !fileNodeService.userCanAccessFileNode(longFileId, userEmail)) {
        return ResponseEntity.notFound().build();
      }
      List<FileNode> fileNodeList = fileNodeService.fileNodeChildrenUserCanAccess(longFileId, userEmail);
      List<FileNodeRecord> fileNodeRecordList = fileNodeList.stream().map(fileNode -> fileNode.toRecord()).toList();

      List<FileNode> fileNodeParentChain = fileNodeService.fileNodeParentChain(longFileId);
      List<FileNodeRecord> fileNodeParentRecordChain = fileNodeParentChain.stream().map(fileNode -> fileNode.toRecord()).toList();

      FileNodeContentsRecord fileNodeContentsRecord = new FileNodeContentsRecord(fileNodeRecordList, fileNodeParentRecordChain);

      return ResponseEntity.ok(fileNodeContentsRecord);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to load FileNode data", e);
    }
  }

  @GetMapping("/file_contents/permissions")
  public ResponseEntity<FileNodePermissionsRecord> getFileNodePermissions(@RequestParam(value="file_id") String fileId) {
    try {
      Long longFileId = Long.parseLong(fileId);
      if (longFileId == 0) {
        return ResponseEntity.notFound().build();
      }
      List<FilePermission> allFilePermissions = filePermissionService.findByFileNodeId(longFileId);
      List<FilePermissionRecord> filePermissions = allFilePermissions.stream()
                                                    .filter(filePermission -> {
                                                      return filePermission.getDownstreamFileNodeId() == null;
                                                    })
                                                    .map(filePermission -> filePermission.toRecord())
                                                    .toList();
      List<FilePermissionRecord> downstreamPermissions = allFilePermissions.stream()
                                                    .filter(filePermission -> {
                                                      return filePermission.getDownstreamFileNodeId() != null;
                                                    })
                                                    .map(filePermission -> filePermission.toRecord())
                                                    .toList();
      FileNodePermissionsRecord fileNodePermissionsRecord = new FileNodePermissionsRecord(filePermissions, downstreamPermissions);

      return ResponseEntity.ok(fileNodePermissionsRecord);
    } catch (Exception e) {
      System.out.println(e.toString());
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to load FilePermissions data", e);
    }
  }
}
