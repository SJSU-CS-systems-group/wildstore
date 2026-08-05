package edu.sjsu.wildstore.wildstore_relationalDb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileNodeService {
    private final FileNodeRepository fileNodeRepository;
    private final FilePermissionRepository filePermissionRepository;
    private final UserService userService;

    public class ParentDoesNotExistException extends Exception {
      public ParentDoesNotExistException(String message) {
        super(message);
      }
    }

    public class FileNodeDoesNotExistException extends Exception {
      public FileNodeDoesNotExistException(String message) {
        super(message);
      }
    }

    public class UserDoesNotExistException extends Exception {
      public UserDoesNotExistException(String message) {
        super(message);
      }
    }

    @Autowired
    public FileNodeService(FileNodeRepository fileNodeRepository, UserService userService, FilePermissionRepository filePermissionRepository) {
        this.fileNodeRepository = fileNodeRepository;
        this.userService = userService;
        this.filePermissionRepository = filePermissionRepository;
    }

    @Transactional
    public FileNode findOrCreate(String fileName, long size, FileType fileType, Long parentId, String digest) throws ParentDoesNotExistException {
        // Check if the entity already exists
        Optional<FileNode> existingFileNode = fileNodeRepository.findByDigest(digest);
        if (existingFileNode.isPresent()) {
          return existingFileNode.get();
        }
        // If not found, create a new instance
        Optional<FileNode> parent = fileNodeRepository.findByIdAndFileType(parentId, FileType.DIRECTORY);
        if (!parent.isPresent()) {
          String message = "Parent FileNode with id " + parentId + " either doesn't exist or is not a directory.";
          throw new ParentDoesNotExistException(message); 
        }
        // If parent directory found create the instance 
        FileNode fileNode = new FileNode(fileName, size, fileType, parent.get(), digest);
        return fileNodeRepository.save(fileNode); // Save the new entity
    }

    public FileNode saveFileNode(FileNodeParams fileNodeParams) {
        FileNode parent = null;
        if (fileNodeParams.parentId != null) {
            parent = fileNodeRepository.findById(fileNodeParams.parentId).get();
        }
        String fileName = fileNodeParams.fileName;
        FileType fileType = fileNodeParams.fileType;
        String digest = fileNodeParams.digest;
        FileNode fileNode = new FileNode(fileName, fileNodeParams.size, fileType, parent, digest);
        return fileNodeRepository.save(fileNode);
    }

    public Optional<FileNode> findById(Long fileNodeId) {
        return fileNodeRepository.findById(fileNodeId);
    }

    public List<FileNode> findByParentId(Long parentId) {
        return fileNodeRepository.findByParentId(parentId);
    }

    public List<FileNode> fileNodeChildrenUserCanAccess(Long parentId, String userEmail) throws Exception {
        User user = userService.findOrCreate(userEmail);
        if (userService.isAdmin(user.getId())) {
          return findByParentId(parentId);
        }
        return fileNodeChildrenUserCanAccess(parentId, user.getId());
    }

    public List<FileNode> fileNodeChildrenUserCanAccess(Long parentId, Long userId) throws Exception {
        if (userCanAccessParent(parentId, userId)) {
          return fileNodeRepository.findByParentId(parentId);
        }
        return fileNodeRepository.findByParentIdAndUserIdFilePermission(parentId, userId);
    }

    public boolean userCanAccessParent(Long parentId, Long userId) throws Exception {
        if (parentId == null) {
          return false;
        } else if (filePermissionRepository.existsByUserIdAndFileNodeIdAndDownstreamFileNodeId(userId, parentId, null)) {
          return true;
        }
        Optional<FileNode> parentNode = findById(parentId);
        if (!parentNode.isPresent()) {
          return false;
        }
        return userCanAccessParent(parentNode.get().getParentId(), userId);
    }

    public boolean userCanAccessFileNode(Long fileNodeId, String userEmail) throws Exception {
      User user = userService.findOrCreate(userEmail);
      if (userService.isAdmin(user.getId())) {
        return true;
      }
      return userCanAccessFileNode(fileNodeId, user.getId());
    }

    public boolean userCanAccessFileNode(Long fileNodeId, Long userId) throws Exception {
      if (fileNodeId == null) {
        return true;
      } else if (filePermissionRepository.existsByUserIdAndFileNodeId(userId, fileNodeId)) {
        return true;
      }
      Optional<FileNode> parentNode = findById(fileNodeId);
      if (!parentNode.isPresent()) {
        return false;
      }
      return userCanAccessFileNode(parentNode.get().getParentId(), userId);
    }

    public List<FileNode> fileNodeParentChain(Long fileNodeId) throws Exception {
      List<FileNode> parentChain = new ArrayList<FileNode>();
      while (fileNodeId != null) {
          Optional<FileNode> optionalFileNode = findById(fileNodeId);
          if (!optionalFileNode.isPresent()) {
            String message = "FileNode with id " + fileNodeId + " either doesn't exist or is not a directory.";
            throw new FileNodeDoesNotExistException(message); 
          } else {
            FileNode fileNode = optionalFileNode.get();
            parentChain.add(fileNode);
            fileNodeId = fileNode.getParentId();
          }
      }
      Collections.reverse(parentChain);
      return parentChain;
    }
}
