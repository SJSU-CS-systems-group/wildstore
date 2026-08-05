package edu.sjsu.wildstore.wildstore_relationalDb;

import edu.sjsu.wildstore.wildstore_relationalDb.records.FileNodeRecord;

import java.nio.file.Path;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Convert;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class FileNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Convert(converter = FileNameConverter.class)
    public String fileName; 

    private Long size; 

    @Enumerated(EnumType.STRING)
    private FileType fileType; 

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="parentId", referencedColumnName="id")
    private FileNode parent;

    @Column(name="parentId", insertable=false, updatable=false)
    private Long parentId;

    @OneToMany(mappedBy = "fileNode", cascade = CascadeType.ALL)
    private List<FilePermission> filePermissions;

    @OneToMany(mappedBy = "downstreamFileNode", cascade = CascadeType.ALL)
    private List<FilePermission> upstreamFilePermissions;

    @Column(unique = true, nullable = true)
    @Convert(converter = FileDigestConverter.class)
    private String digest;

    public FileNode() {}

    public FileNode(String fileName, Long size, FileType fileType, FileNode parent, String digest) {
      this.fileName = fileName;
      this.size = size;
      this.fileType = fileType;
      this.parent = parent;
      this.digest = digest;
    }

    public FileNodeRecord toRecord() {
      Path path = Path.of(fileName);
      String endFileName = path.getFileName().toString();
      return new FileNodeRecord(id, endFileName, size, digest, fileType.name());
    }

    public Long getId() {
      return this.id;
    }

    public Long getParentId() {
      return parentId;
    }

    public FileNode getParent() {
      return parent;
    }

    public FileType getFileType() {
      return fileType;
    }

    public String getFileName() {
      Path path = Path.of(fileName);
      return path.getFileName().toString();
    }
}

