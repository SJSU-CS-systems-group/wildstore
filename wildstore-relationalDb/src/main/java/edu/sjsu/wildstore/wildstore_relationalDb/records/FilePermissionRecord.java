package edu.sjsu.wildstore.wildstore_relationalDb.records;
import com.fasterxml.jackson.annotation.JsonProperty;

public record FilePermissionRecord(
    @JsonProperty("filePermissionId") Long id, 
    @JsonProperty("userEmail") String userEmail, 
    @JsonProperty("fileNodeId") Long fileNodeId, 
    @JsonProperty("downstreamFileNode") FileNodeRecord downstreamFileNode){}
