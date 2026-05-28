package edu.sjsu.wildstore.wildstore_relationalDb;

import edu.sjsu.wildstore.wildstore_relationalDb.records.FileNodeRecord;

import java.util.ArrayList;
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
  public ResponseEntity<FileNode> createFileNode(@RequestBody FileNode fileNode) {
    FileNode savedFileNode = fileNodeService.saveFileNode(fileNode);
    return new ResponseEntity<>(savedFileNode, HttpStatus.CREATED);
  }

  @GetMapping("/file_contents")
  public ResponseEntity<List<FileNode>> fileNodes(@RequestParam(value="file_id", defaultValue= "0") String fileId) {
    Long longFileId = Long.parseLong(fileId);
    List<FileNode> fileNodeList = fileNodeService.findByParentId(longFileId);
    
    return ResponseEntity.ok(fileNodeList);
  }

  @GetMapping("/file_contents_test")
  public List<FileNodeRecord> files(@RequestParam(value="file_id", defaultValue= "0") String fileId) {
    List<FileNodeRecord> fileNodeList = new ArrayList<FileNodeRecord>();
    if (fileId.equals("0")) {
      FileNodeRecord dir_1 = new FileNodeRecord(1, "dir1", 0, 0, "###");
      FileNodeRecord file_1 = new FileNodeRecord(2, "file1.txt", 100, 1, "##1");
      fileNodeList.add(dir_1);
      fileNodeList.add(file_1);
    } else if (fileId.equals("1")) {
      FileNodeRecord file_2 = new FileNodeRecord(3, "file2.txt", 200, 1, "##2");
      FileNodeRecord file_3 = new FileNodeRecord(4, "file3.txt", 300, 1, "##3");
      fileNodeList.add(file_2);
      fileNodeList.add(file_3);
    }
    return fileNodeList;
  }
}
