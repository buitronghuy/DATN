package com.example.federatedserver;

import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class Controller {

    @PostMapping("/uploadmodel")
    public String handleFileUpload(@RequestParam("file") MultipartFile file) throws IOException {
        // Save file to disk
        file.transferTo(new File(System.getProperty("user.dir") + "/src/main/resources/model/clientmodel/" + System.currentTimeMillis() + "_file.zip"));
        return "File uploaded successfully";
    }

    @GetMapping("/getmodel")
    public ResponseEntity<FileSystemResource> downloadZipFile() {
        // Specify the path to the zip file
        String filePath = "src/main/resources/model/trained_nn.zip";
        File file = new File(filePath);

        // Set the response headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", file.getName());

        // Create a FileSystemResource from the file
        FileSystemResource resource = new FileSystemResource(file);

        // Return the zip file as the response
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    @GetMapping("/getfolderpath")
    public String getFolderPath() {
        File directory = new File(System.getProperty("user.dir"));

        List<File> resultList = new ArrayList<File>();

        // get all the files from a directory
        File[] fList = directory.listFiles();
        resultList.addAll(Arrays.asList(fList));
        for (File file : fList) {
            if (file.isFile()) {
                System.out.println(file.getAbsolutePath());
            } else if (file.isDirectory()) {
//                resultList.addAll(listf(file.getAbsolutePath()));
                System.out.println(file.getAbsolutePath());
            }
        }
        System.out.println(fList);
        return "Access successfully";
    }

    @PostMapping("/getlistfile")
    public List<String> getListFile(@RequestBody String path) throws Exception {
        File directory = new File(System.getProperty("user.dir") + path);

        List<File> resultList = new ArrayList<File>();
        List<String> fileList = new ArrayList<String>();

        // get all the files from a directory
        File[] fList = directory.listFiles();
        resultList.addAll(Arrays.asList(fList));
        for (File file : fList) {
            fileList.add(file.getAbsolutePath());
            System.out.println(file.getAbsolutePath());
        }
        System.out.println(fList);
        return fileList;
    }

    @GetMapping("/resetmodel")
    public String resetModel() throws IOException {
        String originFilePath = "src/main/resources/model/origin_trained_nn.zip";
        File originFile = new File(originFilePath);
        String filePath = "src/main/resources/model/trained_nn.zip";
        File file = new File(filePath);

        Files.copy(originFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);

//        // Return the zip file as the response
        return "Copy file successfully!";
    }

    @GetMapping("/averageweights")
    public String averageWeights() throws Exception {
        ClassifierNNAverageWeights.run();
        return "Average weights successfully!";
    }

    @GetMapping("/testmodel")
    public String testModel() throws Exception {
        return ClassifierNNTest.run();
    }
}
