package org.example.encryption;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

public class AESEncryption {
    private final String SECRET_KEY_SPEC = "AES";
    private final int KEY_SIZE = 128;
    private String fileName;
    private String encryptKeyFileName;
    public AESEncryption(String fileName, String encryptKeyFileName){
        this.fileName = fileName;
        this.encryptKeyFileName = encryptKeyFileName;
    };


    public void encryptKeys(String awsAccessKey, String awsSecretKey){
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, generateSecretKey());

            // Encrypt the AWS keys
            byte[] encryptedAccessKey = cipher.doFinal(awsAccessKey.getBytes(StandardCharsets.UTF_8));
            byte[] encryptedSecretKey = cipher.doFinal(awsSecretKey.getBytes(StandardCharsets.UTF_8));
            byte[] encryptString = cipher.doFinal("encrypted".getBytes(StandardCharsets.UTF_8));

            // Encode encrypted keys to Base64
            String encodedAccessKey = Base64.getEncoder().encodeToString(encryptedAccessKey);
            String encodedSecretKey = Base64.getEncoder().encodeToString(encryptedSecretKey);
            String encodedEncryptString = Base64.getEncoder().encodeToString(encryptString);

            Files.write(Paths.get(fileName), (encodedAccessKey + "\n" + encodedSecretKey + "\n" + encodedEncryptString).getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public HashMap<String, String> decryptKeys() throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, loadSecretKeyFromFile());

        // Read encrypted keys from the file
        String encryptedKeys = null;
        try {
            encryptedKeys = new String(Files.readAllBytes(Paths.get(fileName)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String[] parts = encryptedKeys.split("\n");
        String encodedAccessKey = parts[0];
        String encodedSecretKey = parts[1];

        // Decode Base64 and decrypt the keys
        byte[] decodedAccessKey = Base64.getDecoder().decode(encodedAccessKey);
        byte[] decodedSecretKey = Base64.getDecoder().decode(encodedSecretKey);

        String decryptedAccessKey = new String(cipher.doFinal(decodedAccessKey), StandardCharsets.UTF_8);
        String decryptedSecretKey = new String(cipher.doFinal(decodedSecretKey), StandardCharsets.UTF_8);

        HashMap<String, String> keys = new HashMap<>();
        keys.put("decryptedAccessKey", decryptedAccessKey);
        keys.put("decryptedSecretKey", decryptedSecretKey);

        System.out.println("Decrypted Access Key: " + decryptedAccessKey);
        System.out.println("Decrypted Secret Key: " + decryptedSecretKey);

        return keys;
    }

    private SecretKeySpec loadSecretKeyFromFile(){
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(encryptKeyFileName))) {
            // Read the SecretKeySpec object from the file
            SecretKeySpec secretKeySpec = (SecretKeySpec) inputStream.readObject();

            return secretKeySpec;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String decryptString(String encryptedString){
        try{
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedString);

            Cipher cipher = Cipher.getInstance(SECRET_KEY_SPEC);
            cipher.init(Cipher.DECRYPT_MODE, loadSecretKeyFromFile());

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        }catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }



    public boolean areKeysEncrypted(){
        try{
            List<String> lines = Files.readAllLines(Path.of(fileName));
            if(decryptString(lines.get(2)).equals("encrypted")){
                return true;
            }else{
                return false;
            }
        }catch (IOException ex){
            return false;
        }

    }

    private SecretKeySpec generateSecretKey(){
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(SECRET_KEY_SPEC);
            keyGenerator.init(KEY_SIZE);
            SecretKey secretKey = keyGenerator.generateKey();
            SecretKeySpec key = new SecretKeySpec(secretKey.getEncoded(), SECRET_KEY_SPEC);

            saveSecretKeyToFile(key);

            return key;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveSecretKeyToFile(SecretKeySpec secretKey) {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(encryptKeyFileName))) {
            outputStream.writeObject(secretKey);
            System.out.println("Secret key saved to file: " + secretKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
