package edu.sjsu.wildstore.wildstore_relationalDb;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FileNodeRepository extends JpaRepository<FileNode, Long> {
  Optional<FileNode> findByDigest(String digest);

  Optional<FileNode> findById(Long fileNodeId);

  Optional<FileNode> findByIdAndFileType(Long fileNodeId, FileType fileType);

  boolean existsById(Long fileNodeId);

  List<FileNode> findByParentId(Long parentId);

  @Query("SELECT DISTINCT f FROM FileNode f JOIN f.filePermissions p WHERE f.parent = :parent AND p.user = :user")
  List<FileNode> findByParentIdAndUserFilePermission(@Param("parent") FileNode parent, @Param("user") User user);
}
