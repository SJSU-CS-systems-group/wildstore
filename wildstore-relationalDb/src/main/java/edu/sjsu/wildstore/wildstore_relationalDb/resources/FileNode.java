package edu.sjsu.wildstore.wildstore_relationalDb;

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

@Entity
public class FileNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = FileNameConverter.class)
    private String fileName; 

    private long size; 

    @Enumerated(EnumType.STRING)
    private FileType fileType; 

    @ManyToOne
    @JoinColumn(name="file_node_id", referencedColumnName="id")
    private FileNode parent;

    @Column(unique = true, nullable = false)
    @Convert(converter = FileDigestConverter.class)
    private String digest;

    public FileNode(String fileName, long size, FileType fileType, FileNode parent, String digest) {
      this.fileName = fileName;
      this.size = size;
      this.fileType = fileType;
      this.parent = parent;
      this.digest = digest;
    }
}
