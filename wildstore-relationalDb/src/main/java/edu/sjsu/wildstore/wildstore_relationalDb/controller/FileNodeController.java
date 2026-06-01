package edu.sjsu.wildstore.wildstore_relationalDb;

import edu.sjsu.wildstore.wildstore_relationalDb.records.FileNodeRecord;

import java.util.List;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
public class FileNodeController {
  @Autowired
  private FileNodeService fileNodeService;

  @PostMapping("/file_contents")
  public ResponseEntity<Long> createFileNode(@RequestBody FileNodeParams fileNodeParams) {
    FileNode savedFileNode = fileNodeService.saveFileNode(fileNodeParams);
    return new ResponseEntity<>(savedFileNode.id, HttpStatus.CREATED);
  }

  @GetMapping("/file_contents")
  public ResponseEntity<List<FileNodeRecord>> fileNodes(@RequestParam(value="file_id", defaultValue= "0") String fileId) {
    Long longFileId = Long.parseLong(fileId);
    List<FileNode> fileNodeList = fileNodeService.findByParentId(longFileId);
    List<FileNodeRecord> fileNodeRecordList = fileNodeList.stream().map(fileNode -> fileNode.toRecord()).toList();
    
    return ResponseEntity.ok(fileNodeRecordList);
  }
}
