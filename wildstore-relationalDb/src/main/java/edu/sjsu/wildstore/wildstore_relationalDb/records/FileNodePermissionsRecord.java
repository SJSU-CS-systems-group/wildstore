package edu.sjsu.wildstore.wildstore_relationalDb.records;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record FileNodePermissionsRecord(
    @JsonProperty("filePermissions") List<FilePermissionRecord> filePermissions, 
    @JsonProperty("downstreamPermissions") List<FilePermissionRecord> downstreamPermissions){}
