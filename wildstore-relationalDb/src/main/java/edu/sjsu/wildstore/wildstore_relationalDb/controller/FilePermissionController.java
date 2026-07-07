package edu.sjsu.wildstore.wildstore_relationalDb;

//import edu.sjsu.wildstore.wildstore_relationalDb.records.FilePermissionRecord;
import edu.sjsu.wildstore.FilePermissionsParams;

import java.util.List;

import org.springframework.http.*;
//import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
public class FilePermissionController {
  @Autowired
  private FilePermissionService filePermissionService;

  @PostMapping("/file/share")
  public ResponseEntity<Void> createFilePermissions(@RequestBody FilePermissionsParams filePermissionsParams) {
    try {
      Long longFileId = filePermissionsParams.fileNodeId;
      String adminEmail = filePermissionsParams.adminEmail;
      List<String> emailPermissions = filePermissionsParams.emails;
      for (String emailPermission : emailPermissions) {
        FilePermission savedFilePermission = filePermissionService.findOrCreate(longFileId, emailPermission, adminEmail);
      }
      return new ResponseEntity<>(HttpStatus.CREATED);
    } catch (Exception e) {
      return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
    }
  }

  //@GetMapping("/file/share")
  //public ResponseEntity<List<FilePermissionRecord>> filePermissions(@RequestParam(value="file_id", defaultValue= "0") String fileId) {
  //  Long longFileId = Long.parseLong(fileId);
  //  if (longFileId == 0) {
  //    longFileId = null;
  //  }
  //  List<FileNode> fileNodeList = fileNodeService.findByParentId(longFileId);
  //  List<FileNodeRecord> fileNodeRecordList = fileNodeList.stream().map(fileNode -> fileNode.toRecord()).toList();
  //
  //  return ResponseEntity.ok(fileNodeRecordList);
  //}
}
