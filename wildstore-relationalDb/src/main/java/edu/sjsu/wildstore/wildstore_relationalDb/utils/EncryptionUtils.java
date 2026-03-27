package edu.sjsu.wildstore.wildstore_relationalDb;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

public class EncryptionUtils {
    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";

    public static String encryptString(String string, byte[] aes_key) {
      // do some encryption
      Key key = new SecretKeySpec(aes_key, "AES");
      try {
         Cipher c = Cipher.getInstance(ALGORITHM);
         c.init(Cipher.ENCRYPT_MODE, key);
         return Base64.getEncoder().encodeToString(c.doFinal(string.getBytes()));
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
    }

    public static String decryptString(String encrypted_string, byte[] aes_key) {
      // do some decryption
      Key key = new SecretKeySpec(aes_key, "AES");
      try {
        Cipher c = Cipher.getInstance(ALGORITHM);
        c.init(Cipher.DECRYPT_MODE, key);
        return new String(c.doFinal(Base64.getDecoder().decode(encrypted_string)));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
}
