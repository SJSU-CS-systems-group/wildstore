package edu.sjsu.wildstore.wildstore_relationalDb;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FilePermissionService {
    private final FilePermissionRepository filePermissionRepository;
    private final UserService userService;
    private final FileNodeService fileNodeService;

    public class FileNodeDoesNotExistException extends Exception {
      public FileNodeDoesNotExistException(String message) {
        super(message);
      }
    }

    @Autowired
    public FilePermissionService(FilePermissionRepository filePermissionRepository, UserService userService, FileNodeService fileNodeService) {
        this.filePermissionRepository = filePermissionRepository;
        this.userService = userService;
        this.fileNodeService = fileNodeService;
    }

    @Transactional
    public FilePermission findOrCreate(Long fileNodeId, String email, String adminEmail) throws FileNodeDoesNotExistException {
        // if not admin
        Optional<User> admin = userService.getAdminByEmail(adminEmail);
        if (!admin.isPresent()) {
          throw new SecurityException("User is not authorized: Requires Admin Role.");
        }
        // Get the User
        User user = userService.findOrCreate(email);
        Long userId = user.getId();

        // Check if the entity already exists
        Optional<FilePermission> existingFilePermission = filePermissionRepository.findByUserIdAndFileNodeId(userId, fileNodeId);
        if (existingFilePermission.isPresent()) {
          return existingFilePermission.get();
        }
        // If not found, create a new instance
        Optional<FileNode> fileNode = fileNodeService.findById(fileNodeId);
        if (!fileNode.isPresent()) {
          String message = "FileNode with id " + fileNode + " does not exist.";
          throw new FileNodeDoesNotExistException(message); 
        }
        
        FilePermission filePermission = new FilePermission(admin.get(), user, fileNode.get());
        return filePermissionRepository.save(filePermission); // Save the new entity
    }

    @Transactional
    public void deleteByFileNodeId(Long fileNodeId) {
      filePermissionRepository.deleteByFileNodeId(fileNodeId);
    }
}
