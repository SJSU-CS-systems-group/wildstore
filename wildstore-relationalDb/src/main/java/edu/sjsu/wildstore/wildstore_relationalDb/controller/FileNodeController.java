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
  public List<FileNodeRecord> files(@RequestParam(value="file_id", defaultValue= "0") String file_id) {
    List<FileNodeRecord> fileNodeList = new ArrayList<FileNodeRecord>();
    if (file_id.equals("0")) {
      FileNodeRecord dir_1 = new FileNodeRecord(1, "dir1", 0, 0, "###");
      FileNodeRecord file_1 = new FileNodeRecord(2, "file1.txt", 100, 1, "##1");
      fileNodeList.add(dir_1);
      fileNodeList.add(file_1);
    } else if (file_id.equals("1")) {
      FileNodeRecord file_2 = new FileNodeRecord(3, "file2.txt", 200, 1, "##2");
      FileNodeRecord file_3 = new FileNodeRecord(4, "file3.txt", 300, 1, "##3");
      fileNodeList.add(file_2);
      fileNodeList.add(file_3);
    }
    return fileNodeList;
  }
}
