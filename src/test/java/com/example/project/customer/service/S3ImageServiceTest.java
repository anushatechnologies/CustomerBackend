package com.example.project.customer.service;

import com.example.project.customer.dto.ImageFolder;
import com.example.project.customer.dto.ImageUploadResponse;
import com.example.project.customer.exception.ImageStorageException;
import com.example.project.customer.exception.InvalidImageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ImageServiceTest {

    @Mock
    private S3Client s3Client;

    private S3ImageServiceImpl s3ImageService;

    private final String bucketName = "hinchmart-storage-191481838776-ap-south-2-an";
    private final String region = "ap-south-2";

    @BeforeEach
    void setUp() {
        s3ImageService = new S3ImageServiceImpl(s3Client, bucketName, region);
    }

    @Test
    @DisplayName("uploadImage - Should successfully upload valid image and return response")
    void uploadImage_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        ImageUploadResponse response = s3ImageService.uploadImage(file, ImageFolder.PRODUCTS);

        assertNotNull(response);
        assertNotNull(response.getImageKey());
        assertTrue(response.getImageKey().startsWith("products/"));
        assertTrue(response.getImageKey().endsWith(".jpg"));
        assertTrue(response.getFileUrl().contains(bucketName));
        assertEquals("sample.jpg", response.getOriginalFileName());
        assertEquals("image/jpeg", response.getContentType());
        assertEquals(file.getSize(), response.getSizeBytes());

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertEquals(bucketName, captor.getValue().bucket());
        assertEquals("image/jpeg", captor.getValue().contentType());
    }

    @Test
    @DisplayName("uploadImage - Should throw InvalidImageException when file is empty")
    void uploadImage_EmptyFile_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        assertThrows(InvalidImageException.class, () -> s3ImageService.uploadImage(file, ImageFolder.CATEGORIES));
    }

    @Test
    @DisplayName("uploadImage - Should throw ImageStorageException when S3 rejects the upload")
    void uploadImage_S3Failure_ThrowsStorageException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().statusCode(403).message("Access denied").build());

        assertThrows(ImageStorageException.class, () -> s3ImageService.uploadImage(file, ImageFolder.PRODUCTS));
    }

    @Test
    @DisplayName("downloadImage - Should successfully return bytes")
    void downloadImage_Success() {
        byte[] expectedBytes = "image binary data".getBytes();
        GetObjectResponse getResponse = GetObjectResponse.builder().build();
        ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(getResponse, expectedBytes);

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        byte[] actualBytes = s3ImageService.downloadImage("products/123.jpg");

        assertArrayEquals(expectedBytes, actualBytes);
    }

    @Test
    @DisplayName("downloadImage - Should throw InvalidImageException when key does not exist")
    void downloadImage_NoSuchKey_ThrowsException() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("The specified key does not exist.").build());

        assertThrows(InvalidImageException.class, () -> s3ImageService.downloadImage("products/notfound.jpg"));
    }

    @Test
    @DisplayName("deleteImage - Should call S3 deleteObject")
    void deleteImage_Success() {
        s3ImageService.deleteImage("banners/abc-123.webp");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals(bucketName, captor.getValue().bucket());
        assertEquals("banners/abc-123.webp", captor.getValue().key());
    }

    @Test
    @DisplayName("imageExists - Should return true when object exists and false on NoSuchKeyException")
    void imageExists_Check() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        assertTrue(s3ImageService.imageExists("products/sample.png"));

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        assertFalse(s3ImageService.imageExists("products/nonexistent.png"));
    }

    @Test
    @DisplayName("extractKeyFromUrl - Should extract key correctly from URL or key")
    void extractKeyFromUrl_Test() {
        assertEquals("products/123.jpg", s3ImageService.extractKeyFromUrl("products/123.jpg"));
        assertEquals("banners/xyz.png", s3ImageService.extractKeyFromUrl("https://hinchmart-storage-191481838776-ap-south-2-an.s3.ap-south-2.amazonaws.com/banners/xyz.png"));
    }

    @Test
    @DisplayName("uploadImages - Should upload multiple images successfully")
    void uploadImages_BatchSuccess() {
        MockMultipartFile file1 = new MockMultipartFile("files", "img1.png", "image/png", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "img2.jpg", "image/jpeg", "content2".getBytes());

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        java.util.List<ImageUploadResponse> responses = s3ImageService.uploadImages(java.util.List.of(file1, file2), ImageFolder.PRODUCTS);

        assertEquals(2, responses.size());
        verify(s3Client, org.mockito.Mockito.times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadImages - Should rollback and delete uploaded files when a subsequent upload fails")
    void uploadImages_BatchFailureRollsBack() {
        MockMultipartFile file1 = new MockMultipartFile("files", "img1.png", "image/png", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "empty.jpg", "image/jpeg", new byte[0]); // will fail validation

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        assertThrows(InvalidImageException.class, () ->
                s3ImageService.uploadImages(java.util.List.of(file1, file2), ImageFolder.PRODUCTS));

        // file1 was uploaded, so it should have been deleted during rollback
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("uploadFile - Should upload document/pdf successfully")
    void uploadFile_Success() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "pdf content".getBytes());

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        ImageUploadResponse response = s3ImageService.uploadFile(file, ImageFolder.DOCUMENTS);

        assertNotNull(response);
        assertTrue(response.getImageKey().startsWith("documents/"));
        assertTrue(response.getImageKey().endsWith(".pdf"));
    }

    @Test
    @DisplayName("deleteImages - Should delete list of keys/urls")
    void deleteImages_BatchSuccess() {
        s3ImageService.deleteImages(java.util.List.of("products/p1.jpg", "products/p2.jpg"));

        verify(s3Client, org.mockito.Mockito.times(2)).deleteObject(any(DeleteObjectRequest.class));
    }
}
