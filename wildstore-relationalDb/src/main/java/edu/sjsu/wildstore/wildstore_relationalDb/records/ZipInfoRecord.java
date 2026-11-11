package edu.sjsu.wildstore.wildstore_relationalDb.records;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ZipInfoRecord(
    @JsonProperty("zipName") String zipName, 
    @JsonProperty("fileNodeParentChain") List<ZipEntryRecord> zipEntryRecords){}
