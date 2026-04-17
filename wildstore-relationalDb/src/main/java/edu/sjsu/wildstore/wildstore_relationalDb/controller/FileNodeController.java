package edu.sjsu.wildstore.wildstore_relationalDb.controller;

import java.util.ArrayList;
import java.util.List;

import edu.sjsu.wildstore.wildstore_relationalDb.resources.FileNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FileNodeController {
  @GetMapping("/file_contents")
  public List<FileNode> files(@RequestParam(value = "file_id", defaultValue = "0") String fileId) {
    List<FileNode> fileNodeList = new ArrayList<>();
    if ("0".equals(fileId)) {
      fileNodeList.add(new FileNode(1, "dir1", 0, 0, "###"));
      fileNodeList.add(new FileNode(2, "file1.txt", 100, 1, "##1"));
    } else if ("1".equals(fileId)) {
      fileNodeList.add(new FileNode(3, "file2.txt", 200, 1, "##2"));
      fileNodeList.add(new FileNode(4, "file3.txt", 300, 1, "##3"));
    }
    return fileNodeList;
  }
}
