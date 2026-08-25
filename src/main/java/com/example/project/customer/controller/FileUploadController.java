package com.example.project.customer.controller;

import com.example.project.customer.common.ApiResponse;
import com.example.project.customer.dto.FileUploadResponse;
import com.example.project.customer.dto.ImageUploadResponse;
import com.example.project.customer.service.S3ImageService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private final S3ImageService s3ImageService;

    public FileUploadController(S3ImageService s3ImageService) {
        this.s3ImageService = s3ImageService;
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "rfq_docs") String folder) {
        ImageUploadResponse resp = s3ImageService.uploadImage(file, folder);
        FileUploadResponse data = new FileUploadResponse(
                resp.getImageUrl(),
                resp.getOriginalFileName(),
                resp.getContentType(),
                resp.getSizeBytes(),
                resp.getImageKey()
        );
        return ApiResponse.ok("File uploaded successfully", data);
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileUploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "products") String folder) {
        ImageUploadResponse resp = s3ImageService.uploadImage(file, folder);
        FileUploadResponse data = new FileUploadResponse(
                resp.getImageUrl(),
                resp.getOriginalFileName(),
                resp.getContentType(),
                resp.getSizeBytes(),
                resp.getImageKey()
        );
        return ApiResponse.ok("File uploaded successfully", data);
    }
}
