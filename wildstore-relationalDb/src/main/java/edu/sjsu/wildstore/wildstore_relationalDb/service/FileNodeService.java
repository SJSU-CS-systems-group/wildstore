package edu.sjsu.wildstore.wildstore_relationalDb;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileNodeService {
    private final FileNodeRepository fileNodeRepository;
    private final UserService userService;

    public class ParentDoesNotExistException extends Exception {
      public ParentDoesNotExistException(String message) {
        super(message);
      }
    }

    public class UserDoesNotExistException extends Exception {
      public UserDoesNotExistException(String message) {
        super(message);
      }
    }

    @Autowired
    public FileNodeService(FileNodeRepository fileNodeRepository, UserService userService) {
        this.fileNodeRepository = fileNodeRepository;
        this.userService = userService;
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
        return fileNodeRepository.findByParentIdAndUserIdFilePermission(parentId, userId);
    }
}
