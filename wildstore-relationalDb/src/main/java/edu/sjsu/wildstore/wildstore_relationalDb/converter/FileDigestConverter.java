package edu.sjsu.wildstore.wildstore_relationalDb;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class FileDigestConverter implements AttributeConverter<String, String> {
 
    private static final byte[] KEY = System.getenv("FILE_DIGEST_ENCRYPTION_KEY").getBytes();
 
    @Override
    public String convertToDatabaseColumn(String digest) {
      try {
        return EncryptionUtils.encryptString(digest, KEY);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public String convertToEntityAttribute(String encryptedDigest) {
      try {
        return EncryptionUtils.decryptString(encryptedDigest, KEY);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
}
