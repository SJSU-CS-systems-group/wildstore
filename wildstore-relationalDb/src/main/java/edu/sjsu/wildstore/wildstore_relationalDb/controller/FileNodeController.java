package edu.sjsu.wildstore.wildstore_relationalDb;

import edu.sjsu.wildstore.wildstore_relationalDb.records.FileNodeRecord;
import edu.sjsu.wildstore.wildstore_relationalDb.records.FileNodeContentsRecord;
import edu.sjsu.wildstore.wildstore_relationalDb.records.FilePermissionRecord;
import edu.sjsu.wildstore.wildstore_relationalDb.records.FileNodePermissionsRecord;
import edu.sjsu.wildstore.wildstore_relationalDb.records.ZipInfoRecord;
import edu.sjsu.wildstore.wildstore_relationalDb.records.ZipEntryRecord;

import java.io.IOException;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
//import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.Optional;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

  @GetMapping("/file/download/stream")
  public ResponseEntity<ZipInfoRecord> fileDownloadStream(@RequestParam(value="file_id", defaultValue= "0") String fileId, @RequestParam(value="user_email") String userEmail) {
    List<ZipEntryRecord> zipEntries = new ArrayList<>();
    try {
      Long longFileId = Long.parseLong(fileId);
      if (longFileId == 0 || !fileNodeService.userCanAccessFileNode(longFileId, userEmail)) {
        return ResponseEntity.notFound().build();
      }
      Optional<FileNode> optionalFileNode = fileNodeService.findById(longFileId);
      if (!optionalFileNode.isPresent()) {
        return ResponseEntity.notFound().build();
      }
      FileNode fileNode = optionalFileNode.get();
      if (fileNode.getFileType() != FileType.DIRECTORY) {
        return ResponseEntity.notFound().build();
      }
      Path sourceFolder = fileNode.getFilePath();

      zipStreamHelper(zipEntries, sourceFolder, longFileId, userEmail);

      ZipInfoRecord zipInfo = new ZipInfoRecord(sourceFolder.getFileName().toString(), zipEntries);
      return ResponseEntity.ok(zipInfo);
    } catch (Exception e) {
      System.out.println(e.toString());
      return ResponseEntity.notFound().build();
    }
  }

  void zipStreamHelper(List<ZipEntryRecord> zipEntries, Path sourceFolder, Long longFileId, String userEmail) {
    try {
      List<FileNode> fileNodeList = fileNodeService.fileNodeChildrenUserCanAccess(longFileId, userEmail);
      for (FileNode fileNode : fileNodeList) {
        Path filePath = fileNode.getFilePath();
        Path relativePath = sourceFolder.relativize(filePath);
        String zipEntryName = relativePath.toString().replace("\\", "/");
        ZipEntryRecord zipEntryRecord = new ZipEntryRecord(zipEntryName, filePath.toString(), fileNode.getFileType().name());
        zipEntries.add(zipEntryRecord);
        if (fileNode.getFileType() == FileType.DIRECTORY) {
          zipStreamHelper(zipEntries, sourceFolder, fileNode.getId(), userEmail);
        }
      }
    } catch (Exception e) {
      System.out.println(e.toString());
    }
  }

  @GetMapping("/file/download")
  public void fileDownload(@RequestParam(value="file_id", defaultValue= "0") String fileId, @RequestParam(value="user_email") String userEmail, HttpServletRequest request,
                             HttpServletResponse response) {
    try {
      Long longFileId = Long.parseLong(fileId);
      if (longFileId == 0 || !fileNodeService.userCanAccessFileNode(longFileId, userEmail)) {
        return;
      }
      Optional<FileNode> optionalFileNode = fileNodeService.findById(longFileId);
      if (!optionalFileNode.isPresent()) {
        return;
      }
      FileNode fileNode = optionalFileNode.get();
      if (fileNode.getFileType() != FileType.DIRECTORY) {
        return;
      }
      Path sourceFolder = fileNode.getFilePath();
      Path zipFile = Paths.get(sourceFolder.toString() + ".zip");

      response.setContentType("application/zip");
      response.setHeader("Content-Disposition", "attachment; filename=\"archive.zip\"");
      response.setStatus(HttpServletResponse.SC_OK);

      //ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()));
      try (OutputStream os = response.getOutputStream();
          BufferedOutputStream bos = new BufferedOutputStream(os);
          ZipOutputStream zos = new ZipOutputStream(bos)) {
      //ZipOutputStream zos = new ZipOutputStream(response.getOutputStream());
        zipHelper(zos, sourceFolder, longFileId, userEmail);
        zos.finish();
        zos.close();
      } catch (Exception e) {
        System.out.println(e.toString());
      }

      //downloadHelper(zipFile.toString(), request, response);
    } catch (Exception e) {
      System.out.println(e.toString());
    }
  }

  void zipHelper(ZipOutputStream zos, Path sourceFolder, Long longFileId, String userEmail) {
    try {
      List<FileNode> fileNodeList = fileNodeService.fileNodeChildrenUserCanAccess(longFileId, userEmail);
      for (FileNode fileNode : fileNodeList) {
        Path filePath = fileNode.getFilePath();
        Path relativePath = sourceFolder.relativize(filePath);
        String zipEntryName = relativePath.toString().replace("\\", "/");
        try {
          if (fileNode.getFileType() == FileType.DIRECTORY) {
            System.out.println(relativePath.toString());
            zos.putNextEntry(new ZipEntry(zipEntryName + "/"));
            zos.closeEntry();
            zos.flush();
            zipHelper(zos, sourceFolder, fileNode.getId(), userEmail);
          } else if (fileNode.getFileType() == FileType.FILE) {
            if (Files.exists(filePath)) {
              ZipEntry entry = new ZipEntry(zipEntryName);
              zos.putNextEntry(entry);
              
              System.out.println(filePath.toString());
              FileInputStream fis = new FileInputStream(filePath.toString());
              byte[] buffer = new byte[4096];
              int bytesRead;
              while ((bytesRead = fis.read(buffer)) != -1) {
                  zos.write(buffer, 0, bytesRead);
              }

              zos.closeEntry();
              zos.flush();
            }
          }
        } catch (IOException e) {
          System.out.println(e.toString());
        }
      }
    } catch (Exception e) {
      System.out.println(e.toString());
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
