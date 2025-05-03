package com.example.piplchecker.controller;

import com.example.piplchecker.service.BaiduApiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Controller
public class FileUploadController {

    private final BaiduApiService baiduApiService;
    private final String tempFolder;

    public FileUploadController(BaiduApiService baiduApiService,
            @Value("${temp.folder}") String tempFolder) {
        this.baiduApiService = baiduApiService;
        this.tempFolder = tempFolder.trim();
    }

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    @PostMapping("/upload-zip")
    public ResponseEntity<InputStreamResource> handleFileUpload(
            @RequestParam("file") MultipartFile file) throws IOException {

        String accessToken = baiduApiService.getAccessToken();
        if (accessToken == null) {
            return ResponseEntity.badRequest().body(null);
        }

        String currentDir = System.getProperty("user.dir");
        Path tempDir = Paths.get(currentDir, tempFolder).normalize();
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }

        Path zipPath = tempDir.resolve(file.getOriginalFilename());
        if (Files.exists(zipPath)) {
            Files.delete(zipPath);
        }
        Files.copy(file.getInputStream(), zipPath);

        Path extractDir = tempDir.resolve("extracted");
        if (Files.exists(extractDir)) {
            Files.walk(extractDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
        Files.createDirectories(extractDir);

        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                Path filePath = extractDir.resolve(entry.getName());
                if (!entry.isDirectory()) {
                    Files.createDirectories(filePath.getParent());
                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                    }
                    Files.copy(zipIn, filePath);
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }

        String zipFolderName = file.getOriginalFilename().substring(0, file.getOriginalFilename().lastIndexOf('.'));
        Path zipFolder = extractDir.resolve(zipFolderName);

        if (!Files.exists(zipFolder)) {
            return ResponseEntity.badRequest().body(null);
        }

        List<String[]> results = new ArrayList<>();
        Files.walk(zipFolder)
                .filter(path -> path.toString().endsWith(".txt"))
                .forEach(path -> {
                    try {
                        String content = new String(Files.readAllBytes(path));
                        int complianceLabel = baiduApiService.checkComplianceWithPIPL(content, accessToken);
                        results.add(new String[] { path.getFileName().toString(), String.valueOf(complianceLabel) });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        if (results.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        StringBuilder csvContent = new StringBuilder("filename,label\n");
        for (String[] row : results) {
            csvContent.append(String.join(",", row)).append("\n");
        }

        Path csvPath = tempDir.resolve("PIPL_compliance.csv");
        if (Files.exists(csvPath)) {
            Files.delete(csvPath);
        }
        Files.write(csvPath, csvContent.toString().getBytes());

        InputStreamResource resource = new InputStreamResource(new FileInputStream(csvPath.toFile()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=PIPL_compliance.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }
}