package pydio.sdk.java.utils;

import org.apache.commons.codec.binary.Base64;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by pydio on 01/04/2015.
 */
public abstract class PydioSecureStore {

    public static PydioSecureStore instance;

    public static String ENCRYPTED_PASSWORD_PREFIX = "enc:";
    private final int KEY_SIZE = 128;
    private final int PBKDF2_ITERATION_NUMBER = 20000;


    public abstract void addCertificate(String alias, X509Certificate cert)
    throws Exception;

    public abstract boolean checkCertificate(String alias, X509Certificate cert)
    throws Exception;

    public String decrypt(String masterPassword, String salt, String pass){
        byte[] encryptedPass = Base64.decodeBase64(pass.substring(pass.indexOf(ENCRYPTED_PASSWORD_PREFIX)));
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(masterPassword.toCharArray(), salt.getBytes(), PBKDF2_ITERATION_NUMBER, KEY_SIZE);
            SecretKey key = factory.generateSecret(spec);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(key.getEncoded());
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            return new String(cipher.doFinal(encryptedPass));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String encrypt(String masterPassword, String salt, String password){
        if(masterPassword == null || masterPassword.length() == 0 ){
            return "";
        }else {
            byte[] bytes = password.getBytes();
            try {
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                KeySpec spec = new PBEKeySpec(masterPassword.toCharArray(), salt.getBytes(), PBKDF2_ITERATION_NUMBER, KEY_SIZE);
                SecretKey key = factory.generateSecret(spec);
                SecretKeySpec secretKeySpec = new SecretKeySpec(key.getEncoded(), "AES");

                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                IvParameterSpec ivParameterSpec = new IvParameterSpec(key.getEncoded());
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
                String enc_password = new String(Base64.encodeBase64(cipher.doFinal(bytes)));
                return  ENCRYPTED_PASSWORD_PREFIX+enc_password;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String createMasterPasswordChecksum(String masterPassword){
        String rand = new BigInteger(130, new Random()).toString(32);
        int count = 0;
        for (int i = 0, len = rand.length(); i < len; i++) {
            if (Character.isDigit(rand.charAt(i))) {
                count++;
            }
        }
        String checksum = count + "pydio" + rand;
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(masterPassword.toCharArray(), "Pydio".getBytes(), PBKDF2_ITERATION_NUMBER, KEY_SIZE);
            SecretKey key = factory.generateSecret(spec);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(key.getEncoded());
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
            String enc_checksum = new String(Base64.encodeBase64(cipher.doFinal(checksum.getBytes())));
            return enc_checksum;
        } catch (Exception e) {}
        return null;
    }

    public boolean checkMasterPassword(String masterPassword, String b64Checksum){
        byte[] enc_checksum = Base64.decodeBase64(b64Checksum);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(masterPassword.toCharArray(), "Pydio".getBytes(), PBKDF2_ITERATION_NUMBER, KEY_SIZE);
            SecretKey key = factory.generateSecret(spec);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(key.getEncoded());
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            String clearChecksum = new String(cipher.doFinal(enc_checksum));
            if(!clearChecksum.contains("pydio")){
                return false;
            }
            String first = clearChecksum.substring(0, clearChecksum.indexOf("pydio"));
            String last =  clearChecksum.substring(clearChecksum.indexOf("pydio") + 5);
            int count = 0;
            for (int i = 0, len = last.length(); i < len; i++) {
                if (Character.isDigit(last.charAt(i))) {
                    count++;
                }
            }
            return first.equals(""+count);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static PydioSecureStore securityManager(){
        return instance;
    }
}