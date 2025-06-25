package com.example.demo;

import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
public class FileUploadController {

    private static final String UPLOAD_DIR = "C:/Users/apurv/OneDrive/Desktop/Java project/uploads";

    // Upload file to subject folder
    @PostMapping("/upload")
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file,
                                                   @RequestParam("subject") String subject) {
        try {
            Path subjectPath = Paths.get(UPLOAD_DIR, subject);
            Files.createDirectories(subjectPath);

            Path filePath = subjectPath.resolve(file.getOriginalFilename());
            Files.write(filePath, file.getBytes());

            return ResponseEntity.ok("File uploaded successfully!");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed!");
        }
    }

    // List files by subject
    @GetMapping("/files")
    public ResponseEntity<List<String>> listFiles(@RequestParam("subject") String subject) {
        try {
            Path subjectPath = Paths.get(UPLOAD_DIR, subject);
            if (!Files.exists(subjectPath)) return ResponseEntity.ok(Collections.emptyList());

            List<String> files = Files.list(subjectPath)
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(files);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Serve file for viewing or downloading
    @GetMapping("/uploads/{subject}/{filename:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String subject,
                                            @PathVariable String filename,
                                            @RequestParam(defaultValue = "false") boolean download) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR, subject).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) return ResponseEntity.notFound().build();

            String mimeType = Files.probeContentType(filePath);
            if (mimeType == null) mimeType = "application/octet-stream";

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                download
                ? "attachment; filename=\"" + resource.getFilename() + "\""
                : "inline; filename=\"" + resource.getFilename() + "\"");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .headers(headers)
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
