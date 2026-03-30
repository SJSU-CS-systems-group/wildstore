package edu.sjsu.wildstore.wildstore_relationalDb;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileNodeRepository extends JpaRepository<FileNode, Long> {
  Optional<FileNode> findByDigest(String digest);

  Optional<FileNode> findById(Long fileNodeId);

  Optional<FileNode> findByIdAndFileType(Long fileNodeId, FileType fileType);
}
