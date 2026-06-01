package edu.sjsu.wildstore.wildstore_relationalDb;

import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Convert;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
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

    @ManyToOne
    @JoinColumn(name="parentId", referencedColumnName="id")
    private FileNode parent;

    @OneToMany(mappedBy = "fileNode", cascade = CascadeType.REMOVE)
    private List<FilePermission> filePermissions;

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
}

