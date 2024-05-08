package org.example.amazon;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.example.FilesFinder;
import org.example.enums.WorldFileFormat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class AmazonS3Bucket {

    private AmazonS3 s3Client;
    private String bucketName;
    private static AmazonS3Bucket amazonS3BucketInstance;
    private AmazonS3Bucket(){}
    public static AmazonS3Bucket getInstance(String bucketName, String accessKey, String secretKey){
        if(amazonS3BucketInstance == null){
            amazonS3BucketInstance = new AmazonS3Bucket(bucketName, accessKey, secretKey);
        }
        return amazonS3BucketInstance;
    }
    private AmazonS3Bucket(String bucketName, String accessKey, String secretKey){
        this.bucketName = bucketName;
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(Regions.EU_NORTH_1)
                .build();
    }
    public Bucket getBucket(String bucket_name) {
        Bucket named_bucket = null;
        List<Bucket> buckets = this.s3Client.listBuckets();
        for (Bucket b : buckets) {
            if (b.getName().equals(bucket_name)) {
                named_bucket = b;
            }
        }
        return named_bucket;
    }

    public Bucket createBucket(String bucket_name) {

        Bucket b = null;
        if (this.s3Client.doesBucketExistV2(bucket_name)) {
            System.out.format("Bucket %s already exists.\n", bucket_name);
            b = getBucket(bucket_name);
        } else {
            try {
                b = this.s3Client.createBucket(bucket_name);
            } catch (AmazonS3Exception e) {
                System.err.println(e.getErrorMessage());
            }
        }
        return b;
    }

    public void listBuckets(){
        List<Bucket> buckets = s3Client.listBuckets();
        for (Bucket b: buckets){
            System.out.println("* " + b.getName());
        }
    }

    public List<S3ObjectSummary> listObjects(String bucketName){
        ListObjectsV2Result results = s3Client.listObjectsV2(bucketName);
        List<S3ObjectSummary> objects = results.getObjectSummaries();
        return objects;
    }

    public void downloadObjects(String bucketName, HashMap<String, S3ObjectSummary> objectsSummaries, String destinationDirectory){

        try {
            for(S3ObjectSummary obj: objectsSummaries.values()){
                System.out.println(obj.getKey());
                S3Object o = s3Client.getObject(bucketName, obj.getKey());
                S3ObjectInputStream s3is = o.getObjectContent();
                FileOutputStream fos = new FileOutputStream(new File(destinationDirectory, obj.getKey()));
                byte[] read_buf = new byte[1024];
                int read_len = 0;
                while ((read_len = s3is.read(read_buf)) > 0) {
                    fos.write(read_buf, 0, read_len);
                }
                s3is.close();
                fos.close();
            }
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }


    public HashMap<String, S3ObjectSummary> findLastModifiedObject(String bucketName, String worldName){
        HashMap<String, S3ObjectSummary> foundObjects = new HashMap<>();
        long lastModifiedDBTime = 0, lastModifiedDBOLDTime = 0, lastModifiedFWLTime = 0, lastModifiedFWLOLDTime = 0;

        List<S3ObjectSummary> s3ObjectSummaries = listObjects(bucketName);

        for (S3ObjectSummary obj : s3ObjectSummaries){
            if(obj.getKey().contains(worldName) && obj.getKey().contains(WorldFileFormat.DB.getFormatName()) && !obj.getKey().contains(".old")){
                if (obj.getLastModified().getTime() > lastModifiedDBTime){
                    lastModifiedDBTime = obj.getLastModified().getTime();
                    foundObjects.put(WorldFileFormat.DB.getFormatName(), obj);
                }
            }

            if(obj.getKey().contains(worldName) && obj.getKey().contains(WorldFileFormat.FWL.getFormatName()) && !obj.getKey().contains(".old")){
                if (obj.getLastModified().getTime() > lastModifiedFWLTime){
                    lastModifiedFWLTime = obj.getLastModified().getTime();
                    foundObjects.put(WorldFileFormat.FWL.getFormatName(), obj);
                }
            }


            if(obj.getKey().contains(worldName) && obj.getKey().contains(WorldFileFormat.FWL_OLD.getFormatName()) ){
                if (obj.getLastModified().getTime() > lastModifiedFWLOLDTime){
                    lastModifiedFWLOLDTime = obj.getLastModified().getTime();
                    foundObjects.put(WorldFileFormat.FWL_OLD.getFormatName(), obj);
                }
            }

            if(obj.getKey().contains(worldName) && obj.getKey().contains(WorldFileFormat.DB_OLD.getFormatName())){
                if (obj.getLastModified().getTime() > lastModifiedDBOLDTime){
                    lastModifiedDBOLDTime = obj.getLastModified().getTime();
                    foundObjects.put(WorldFileFormat.DB_OLD.getFormatName(), obj);
                }
            }
        }

        return foundObjects;


    }
    public void uploadObjects(List<File> files, String owner){
        HashMap<String, String> dateCreation = FilesFinder.getDateCreation(files);
        for(File file : files){
            try{
                s3Client.putObject(bucketName, String.format("%s_%s_%s_%s", file.getName(), dateCreation.get(file.getName()), owner, generateRandomId()), file);
            }catch (AmazonS3Exception e){
                System.err.println(e.getErrorMessage());
                System.exit(1);
            }
        }
    }

    private String generateRandomId(){
        String characters = "abcdefghijklmnopqrstuvwxyz0123456789";

        Random random = new Random();

        StringBuilder idBuilder = new StringBuilder();

        while (idBuilder.length() < 10) {
            int index = random.nextInt(characters.length());
            idBuilder.append(characters.charAt(index));
        }


        return idBuilder.toString();
    }

}
