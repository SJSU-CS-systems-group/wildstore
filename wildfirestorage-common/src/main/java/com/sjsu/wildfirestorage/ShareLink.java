package com.sjsu.wildfirestorage;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "share-links")
public class ShareLink {
    public String shareId;
    public String createdBy;
    public String fileDigest;
    public List<Download> downloads;
}
