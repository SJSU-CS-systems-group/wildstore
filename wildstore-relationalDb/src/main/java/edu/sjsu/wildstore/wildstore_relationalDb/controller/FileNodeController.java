package edu.sjsu.wildstore.wildstore_relationalDb;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FileNodeController {
  @GetMapping("/file_contents")
  public List<FileNode> files(@RequestParam(value="file_id", defaultValue= "0") String file_id) {
    List<FileNode> fileNodeList = new ArrayList<FileNode>();
    if (file_id.equals("0")) {
      FileNode dir_1 = new FileNode(1, "dir1", 0, 0, "###");
      FileNode file_1 = new FileNode(2, "file1.txt", 100, 1, "##1");
      fileNodeList.add(dir_1);
      fileNodeList.add(file_1);
    } else if (file_id.equals("1")) {
      FileNode file_2 = new FileNode(3, "file2.txt", 200, 1, "##2");
      FileNode file_3 = new FileNode(4, "file3.txt", 300, 1, "##3");
      fileNodeList.add(file_2);
      fileNodeList.add(file_3);
    }
    return fileNodeList;
  }
}
