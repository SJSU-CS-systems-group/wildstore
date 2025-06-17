package com.sjsu.wildfirestorage;

import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Document(collection = "share-links")
public class ShareLink {
    public String shareId;
    public String createdBy;
    public String fileDigest;

    public LocalDateTime createdAt;
    public List<Download> downloads;

    public Set<String> filePath;

    public Set<String> emailAddresses;

    public LocalDateTime expiry;
}
