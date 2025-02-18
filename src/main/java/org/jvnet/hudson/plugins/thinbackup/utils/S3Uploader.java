package org.jvnet.hudson.plugins.thinbackup.utils;

import org.jvnet.hudson.plugins.thinbackup.ThinBackupPluginImpl;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class S3Uploader {

    private final S3Client s3Client;

    public S3Uploader(ThinBackupPluginImpl plugin) {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(plugin.getS3AccessKey(), plugin.getS3SecretKey());
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create("endpoint")) //TODO to change
                .region(Region.of(plugin.getS3Region()))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
    }

    public void uploadFile(String bucketName, String key, File file) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.putObject(request, file.toPath());
    }

    public static void zipDirectory(String sourceDirPath, String zipFilePath) throws IOException {
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new IllegalArgumentException("Source directory does not exist or is not a directory.");
        }

        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            addDirectoryToZip(sourceDir, sourceDir, zos);
        }
    }

    private static void addDirectoryToZip(File rootDir, File currentDir, ZipOutputStream zos) throws IOException {
        for (File file : currentDir.listFiles()) {
            if (file.isDirectory()) {
                addDirectoryToZip(rootDir, file, zos); // Recursive call for subdirectories
            } else {
                try (FileInputStream fis = new FileInputStream(file)) {
                    String relativePath = rootDir.toURI().relativize(file.toURI()).getPath();
                    ZipEntry zipEntry = new ZipEntry(relativePath);
                    zos.putNextEntry(zipEntry);

                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                    zos.closeEntry();
                }
            }
        }
    }
}
