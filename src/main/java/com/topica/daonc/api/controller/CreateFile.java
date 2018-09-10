package com.topica.daonc.api.controller;

import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import com.topica.daonc.api.ApiApplication;
import com.topica.daonc.api.model.DriveFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@RestController
public class CreateFile {
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, ApiApplication.getCredentials(HTTP_TRANSPORT))
            .setApplicationName(APPLICATION_NAME)
            .build();

    public CreateFile() throws IOException, GeneralSecurityException {
    }

    @GetMapping("/create_file")
    public DriveFile createFile(@RequestParam String title) throws IOException {
        String idFile = "";
        String titleFile = "";
        DriveFile driveFile = null;

        if (title.isEmpty()) {
            titleFile = "VCRXUNI:template";
        } else {
            titleFile = "VCRXUNI:" + title;
        }

        FileList result = service.files().list()
                .setPageSize(100)
                .setFields("nextPageToken, files(id, name)")
                .setQ("name='" + titleFile + "'")
                .execute();
        List<File> files = result.getFiles();

        if (files.size() > 0) {
            for (File f : files) {
                driveFile = new DriveFile(f.getId(), f.getName());
            }
        } else {
            File fileMetadata = new File();
            fileMetadata.setMimeType("application/vnd.google-apps.document");
            fileMetadata.setName(titleFile);
            File file = service.files()
                    .create(fileMetadata)
                    .setFields("id")
                    .execute();
            idFile = file.getId();
            titleFile = fileMetadata.getName();
            driveFile = new DriveFile(idFile, titleFile);
        }

        JsonBatchCallback<Permission> callback = new JsonBatchCallback<Permission>() {
            @Override
            public void onFailure(GoogleJsonError e,
                                  HttpHeaders responseHeaders)
                    throws IOException {
                // Handle error
                System.err.println(e.getMessage());
            }

            @Override
            public void onSuccess(Permission permission,
                                  HttpHeaders responseHeaders)
                    throws IOException {
                System.out.println("Permission ID: " + permission.getId());
            }
        };
        BatchRequest batch = service.batch();
        Permission userPermission = new Permission()
                .setType("anyone")
                .setRole("reader");
        if (userPermission.getRole() != "reader") {
            service.permissions().create(idFile, userPermission)
                    .setFileId(idFile)
                    .setFields("id")
                    .queue(batch, callback);
            System.out.println("batch:" + batch);
            batch.execute();
        }

        return driveFile;
    }
}
