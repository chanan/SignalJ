package signalJ.infrastructure;

import org.apache.commons.codec.binary.Base64;
import play.Logger;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Optional;

public class DefaultProtectedData implements ProtectedData {
    private final String key;
    public static final String CIPHER_ALGORITHM = "AES";
    public static final String KEY_ALGORITHM = "AES";
    public static final String PASSWORD_HASH_ALGORITHM = "PBKDF2WithHmacSHA1";

    public DefaultProtectedData(String key) {
        this.key = key;
    }

    @Override
    public Optional<String> protect(String data, String purpose) {
        try {
            Cipher cipher = buildCipher(key, purpose, Cipher.ENCRYPT_MODE);
            byte[] encrypted = cipher.doFinal(data.getBytes());
            return Optional.of(Base64.encodeBase64String(encrypted));
        } catch (Exception ex) {
            Logger.error("Error in protect", ex);
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> unprotect(String protectedValue, String purpose) {
        try {
            Cipher cipher = buildCipher(key, purpose, Cipher.DECRYPT_MODE);
            byte[] original = cipher.doFinal(Base64.decodeBase64(protectedValue));
            return Optional.of(new String(original));
        } catch (Exception ex) {
            Logger.error("Error in unprotect", ex);
        }
        return Optional.empty();
    }

    private Key buildKey(String password, String purpose) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PASSWORD_HASH_ALGORITHM);
        KeySpec ks = new PBEKeySpec(password.toCharArray(), purpose.getBytes(), 1024, 128);
        SecretKey s = factory.generateSecret(ks);
        return new SecretKeySpec(s.getEncoded(), KEY_ALGORITHM);
    }

    private Cipher buildCipher(String password, String purpose, int mode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, UnsupportedEncodingException, InvalidKeySpecException {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        Key key = buildKey(password, purpose);
        cipher.init(mode, key);
        return cipher;
    }
}