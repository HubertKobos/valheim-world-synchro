package org.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

public class FilesFinder {
    private Path path;
    private String worldName;
    public FilesFinder(String path, String worldName){
        this.path = Paths.get(path);
        this.worldName = worldName;
    }


    public List<File> findFiles(){
        if(path.getFileName().toString().length() == 0) throw new IllegalArgumentException("Path can not be empty");
        List<File> files = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(this.path)) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString().split("\\.")[0];
                if(fileName.equals(this.worldName)){
                    files.add(file.toFile());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return files;

    }

    public static HashMap<String, String> getDateCreation(List<File> files){
        HashMap<String, String> dates = new HashMap();
        for(File file : files){
            try{
                BasicFileAttributes attr = Files.readAttributes(Path.of(file.getPath()), BasicFileAttributes.class);

                FileTime creationTime = attr.creationTime();

                Date creationDate = new Date(creationTime.toMillis());

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String formattedCreationDate = dateFormat.format(creationDate);
                dates.put(file.getName(), formattedCreationDate.replace(':', '-'));
            }catch(IOException e){
                e.printStackTrace();
            }
        }
        return dates;
    }

    public static void copyPasteFiles(Path fromPath, Path toPath){
        try {
            // Create the destination directory if it doesn't exist
            if (!Files.exists(toPath)) {
                Files.createDirectories(toPath);
            }

            Files.walk(fromPath)
                    .filter(Files::isRegularFile)
                    .forEach(sourceFile -> {
                        try {
                            Path destinationFile = toPath.resolve(fromPath.relativize(sourceFile));

                            Files.move(sourceFile, destinationFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}


