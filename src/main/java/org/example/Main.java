package org.example;


import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.example.amazon.AmazonS3Bucket;
import org.example.encryption.AESEncryption;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) throws IOException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String pathToValheimWorlds = ""; // AppData\\LocalLow\\IronGate\\Valheim\\worlds
        String pathToDownloads = ""; // path_to_project\downloads
        String pathToTest = ""; // path_to_project\testDir
        String bucketName = ""; // bucket name
        String worldName = ""; // world name
        String owner = "Hubert";

        AmazonS3Bucket amazonS3Bucket = null;

        FilesFinder ff = new FilesFinder(pathToValheimWorlds, worldName);
        AESEncryption aesEncryption = new AESEncryption("encrypted_keys.txt", "key_to_encrypt.txt");
        aesEncryption.encryptKeys("", "");

        if(aesEncryption.areKeysEncrypted()){
            HashMap<String, String> keys = aesEncryption.decryptKeys();
            amazonS3Bucket = AmazonS3Bucket.getInstance(bucketName, keys.get("decryptedAccessKey"), keys.get("decryptedSecretKey"));
        }
//        amazonS3Bucket.uploadObjects(ff.findFiles(), owner);


        HashMap<String, S3ObjectSummary> lastModifiedObjects = amazonS3Bucket.findLastModifiedObject(bucketName, worldName);
        System.out.println(lastModifiedObjects);
//        amazonS3Bucket.downloadObjects(bucketName, lastModifiedObjects, pathToDownloads);
        FilesFinder.copyPasteFiles(Path.of(pathToDownloads), Path.of(pathToTest));
    }
}