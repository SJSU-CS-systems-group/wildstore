package edu.sjsu.wildstore.wildstore_relationalDb;

import edu.sjsu.wildstore.wildstore_relationalDb.records.FilePermissionRecord;
import edu.sjsu.wildstore.wildstore_relationalDb.records.FileNodeRecord;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class FilePermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="adminId", referencedColumnName="id")
    private User admin; 

    @ManyToOne
    @JoinColumn(name="userId", referencedColumnName="id")
    private User user;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="fileNodeId", referencedColumnName="id")
    private FileNode fileNode;

    @Column(name="fileNodeId", insertable=false, updatable=false)
    private Long fileNodeId;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="downstreamFileNodeId", referencedColumnName="id")
    private FileNode downstreamFileNode;

    @Column(name="downstreamFileNodeId", insertable=false, updatable=false)
    private Long downstreamFileNodeId;

    public FilePermission(User admin, User user, FileNode fileNode, FileNode downstreamFileNode) {
      this.admin = admin;
      this.user = user;
      this.fileNode = fileNode;
      this.downstreamFileNode = downstreamFileNode;
    }

    public FilePermission() {}

    public Long getDownstreamFileNodeId() {
      return downstreamFileNodeId;
    }

    public FilePermissionRecord toRecord() {
      String userEmail = user.getEmail();
      if (downstreamFileNode == null) {
        return new FilePermissionRecord(id, userEmail, fileNodeId, null);
      }
      FileNodeRecord downstreamFileNodeRecord = downstreamFileNode.toRecord();
      return new FilePermissionRecord(id, userEmail, fileNodeId, downstreamFileNodeRecord);
    }
}
