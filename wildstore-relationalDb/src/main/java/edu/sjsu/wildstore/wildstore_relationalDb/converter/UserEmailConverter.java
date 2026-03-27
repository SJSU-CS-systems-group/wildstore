package edu.sjsu.wildstore.wildstore_relationalDb;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class UserEmailConverter implements AttributeConverter<String, String> {
 
    private static final byte[] KEY = System.getenv("EMAIL_ENCRYPTION_KEY").getBytes();
 
    @Override
    public String convertToDatabaseColumn(String email) {
      try {
        return EncryptionUtils.encryptString(email, KEY);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public String convertToEntityAttribute(String encrypted_email) {
      try {
        return EncryptionUtils.decryptString(encrypted_email, KEY);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
}
