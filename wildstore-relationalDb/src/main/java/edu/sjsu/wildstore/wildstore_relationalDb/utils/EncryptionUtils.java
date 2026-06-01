package edu.sjsu.wildstore.wildstore_relationalDb;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

public class EncryptionUtils {
    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";

    public static String encryptString(String string, String aes_key) {
      if (string == null) {
        return null;
      }
      // do some encryption
      byte[] decodedKeyBytes = Base64.getUrlDecoder().decode(aes_key); 
      Key key = new SecretKeySpec(decodedKeyBytes, "AES");
      try {
         Cipher c = Cipher.getInstance(ALGORITHM);
         c.init(Cipher.ENCRYPT_MODE, key);
         return Base64.getEncoder().encodeToString(c.doFinal(string.getBytes()));
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
    }

    public static String decryptString(String encrypted_string, String aes_key) {
      if (encrypted_string == null) {
        return null;
      }
      // do some decryption
      byte[] decodedKeyBytes = Base64.getUrlDecoder().decode(aes_key); 
      Key key = new SecretKeySpec(decodedKeyBytes, "AES");
      try {
        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.DECRYPT_MODE, key);
        return new String(c.doFinal(Base64.getDecoder().decode(encrypted_string)));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
}
