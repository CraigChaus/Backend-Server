package hashing;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
// As an example the following source was used: https://gist.github.com/jtan189/3804290

public class PasswordHash {

    /**
     * Method to hash password using PBKDF2 hashing algorithm
     * @param password to be hashed
     * @return string format of hashed password
     * @throws NoSuchAlgorithmException when algorithm doesn't exist
     * @throws InvalidKeySpecException when the key spec is wrong
     */
    public String hashPassword(String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        byte[] hash = hashUsingPBKDF2(password.toCharArray(), salt, 128);
        return toHex(salt) + ":" +  toHex(hash);
    }

    /**
     * Method to check if the input password is correct or not
     * @param password to compare to stored password
     * @param goodHash that hashed password that was stored for that client
     * @return whether password is correct or not
     * @throws NoSuchAlgorithmException when the algorithm does not exist
     * @throws InvalidKeySpecException when the key spec does not exist
     */
    public boolean checkPassword(String password, String goodHash)
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        String[] params = goodHash.split(":");

        byte[] salt = fromHex(params[0]);
        byte[] hash = fromHex(params[1]);

        byte[] hashedPasswordToCheck = hashUsingPBKDF2(password.toCharArray(), salt, hash.length);

        int diff = hash.length ^ hashedPasswordToCheck.length;
        for(int i = 0; i < hash.length && i < hashedPasswordToCheck.length; i++)
            diff |= hash[i] ^ hashedPasswordToCheck[i];

        return diff == 0;
    }

    private byte[] hashUsingPBKDF2(char[] password, byte[] salt, int bytes)
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        PBEKeySpec spec = new PBEKeySpec(password, salt, 65536, bytes * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        return skf.generateSecret(spec).getEncoded();
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

}
