package edu.sjsu.wildstore.wildstore_relationalDb;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class FileNameConverter implements AttributeConverter<String, String> {
 
    private static final String KEY = System.getenv("FILE_NAME_ENCRYPTION_KEY");
 
    @Override
    public String convertToDatabaseColumn(String fileName) {
      try {
        return EncryptionUtils.encryptString(fileName, KEY);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public String convertToEntityAttribute(String encryptedFileName) {
      try {
        return EncryptionUtils.decryptString(encryptedFileName, KEY);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
}
