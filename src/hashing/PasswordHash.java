package hashing;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

public class PasswordHash {

    private final int iterationCount;
    private final int keyLength;

    public PasswordHash() {
        this.iterationCount = 65536;
        this.keyLength = 128;
    }

    public String hashPassword(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] hashedPassword = hashUsingPBKDF2(password);

        return toHex(hashedPassword);
    }

    public boolean checkPassword(String passwordToCheck, String realPassword) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] hash = fromHex(realPassword);

        byte[] hashedPasswordToCheck = hashUsingPBKDF2(passwordToCheck);

        int diff = hashedPasswordToCheck.length ^ hash.length;
        for(int i = 0; i < hashedPasswordToCheck.length && i < hash.length; i++)
            diff |= hashedPasswordToCheck[i] ^ hash[i];

        return diff == 0;
    }

    private byte[] fromHex(String hex)
    {
        byte[] binary = new byte[hex.length() / 2];
        for(int i = 0; i < binary.length; i++)
        {
            binary[i] = (byte)Integer.parseInt(hex.substring(2*i, 2*i+2), 16);
        }
        return binary;
    }

    private String toHex(byte[] array)
    {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if(paddingLength > 0)
            return String.format("%0" + paddingLength + "d", 0) + hex;
        else
            return hex;
    }

    private byte[] hashUsingPBKDF2(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        //Now were are using this algorithm which is recommended!
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterationCount, keyLength);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

        return factory.generateSecret(spec).getEncoded();
    }
}
