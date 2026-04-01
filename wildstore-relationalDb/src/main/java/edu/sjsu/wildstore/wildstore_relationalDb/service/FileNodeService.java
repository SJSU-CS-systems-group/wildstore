package edu.sjsu.wildstore.wildstore_relationalDb;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileNodeService {
    private final FileNodeRepository fileNodeRepository;

    public class ParentDoesNotExistException extends Exception {
      public ParentDoesNotExistException(String message) {
        super(message);
      }
    }

    @Autowired
    public FileNodeService(FileNodeRepository fileNodeRepository) {
        this.fileNodeRepository = fileNodeRepository;
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

    public Optional<FileNode> findById(Long fileNodeId) {
        return fileNodeRepository.findById(fileNodeId);
    }

    //public List<FileNode> fileNodeChildrenUserCanAccess(Long parentId, Long userId) {
    //    Optional<FileNode> parent = fileNodeRepository.findByIdAndFileType(parentId, FileType.DIRECTORY);
    //    if (!parent.isPresent()) {
    //      String message = "Parent FileNode with id " + parentId + " either doesn't exist or is not a directory.";
    //      throw new ParentDoesNotExistException(message); 
    //    }
    //}
}
