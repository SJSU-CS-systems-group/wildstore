package edu.sjsu.wildstore.wildstore_relationalDb.records;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record FileNodeContentsRecord(
    @JsonProperty("fileNodeList") List<FileNodeRecord> fileNodeList, 
    @JsonProperty("fileNodeParentChain") List<FileNodeRecord> fileNodeParentChain){}
