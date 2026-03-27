package edu.sjsu.wildstore.wildstore_relationalDb;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

@Converter
public class UserEmailConverter implements AttributeConverter<String, String> {
 
    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final byte[] KEY = System.getenv("EMAIL_ENCRYPTION_KEY").getBytes();
 
    @Override
    public String convertToDatabaseColumn(String email) {
      // do some encryption
      Key key = new SecretKeySpec(KEY, "AES");
      try {
         Cipher c = Cipher.getInstance(ALGORITHM);
         c.init(Cipher.ENCRYPT_MODE, key);
         return Base64.getEncoder().encodeToString(c.doFinal(email.getBytes()));
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
    }

    @Override
    public String convertToEntityAttribute(String encrypted_email) {
      // do some decryption
      Key key = new SecretKeySpec(KEY, "AES");
      try {
        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.DECRYPT_MODE, key);
        return new String(c.doFinal(Base64.getDecoder().decode(encrypted_email)));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
}
