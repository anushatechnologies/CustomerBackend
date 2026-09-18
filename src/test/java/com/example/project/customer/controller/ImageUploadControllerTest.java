package com.example.project.customer.controller;

import com.example.project.customer.config.SecurityConfig;
import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.dto.ImageFolder;
import com.example.project.customer.dto.ImageUploadResponse;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.Store;
import com.example.project.customer.exception.GlobalExceptionHandler;
import com.example.project.customer.exception.InvalidImageException;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.service.S3ImageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ImageUploadController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@SuppressWarnings("null")
class ImageUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private S3ImageService s3ImageService;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private SellerContextUtil sellerContextUtil;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/images/upload - Should upload image to specified folder and return 201 wrapped in ApiResponse")
    void uploadImage_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "shoe.jpg",
                "image/jpeg",
                "image bytes".getBytes()
        );

        ImageUploadResponse response = ImageUploadResponse.builder()
                .imageKey("products/shoe-123.jpg")
                .fileUrl("https://bucket.s3.ap-south-2.amazonaws.com/products/shoe-123.jpg")
                .fileName("shoe.jpg")
                .mimeType("image/jpeg")
                .fileSize(11L)
                .build();

        when(s3ImageService.uploadImage(any(), eq("products"))).thenReturn(response);

        mockMvc.perform(multipart("/api/images/upload")
                        .file(file)
                        .param("folder", "products"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.imageKey").value("products/shoe-123.jpg"))
                .andExpect(jsonPath("$.data.imageUrl").value("https://bucket.s3.ap-south-2.amazonaws.com/products/shoe-123.jpg"))
                .andExpect(jsonPath("$.data.originalFileName").value("shoe.jpg"))
                .andExpect(jsonPath("$.data.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.data.sizeBytes").value(11));
    }

    @Test
    @WithMockUser(roles = "SELLER")
    @DisplayName("POST /api/images/products - Should upload image to products folder for SELLER")
    void uploadProductImage_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "product.png",
                "image/png",
                "png bytes".getBytes()
        );

        ImageUploadResponse response = ImageUploadResponse.builder()
                .imageKey("products/uuid.png")
                .fileUrl("https://bucket.s3.ap-south-2.amazonaws.com/products/uuid.png")
                .fileName("product.png")
                .mimeType("image/png")
                .fileSize(9L)
                .build();

        when(s3ImageService.uploadImage(any(), eq(ImageFolder.PRODUCTS))).thenReturn(response);

        mockMvc.perform(multipart("/api/images/products").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.imageKey").value("products/uuid.png"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/images/categories - Should upload image to categories folder for ADMIN")
    void uploadCategoryImage_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cat.webp",
                "image/webp",
                "webp bytes".getBytes()
        );

        ImageUploadResponse response = ImageUploadResponse.builder()
                .imageKey("categories/uuid.webp")
                .fileUrl("https://bucket.s3.ap-south-2.amazonaws.com/categories/uuid.webp")
                .fileName("cat.webp")
                .mimeType("image/webp")
                .fileSize(10L)
                .build();

        when(s3ImageService.uploadImage(any(), eq(ImageFolder.CATEGORIES))).thenReturn(response);

        mockMvc.perform(multipart("/api/images/categories").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.imageKey").value("categories/uuid.webp"));
    }

    @Test
    @WithMockUser(roles = "SELLER")
    @DisplayName("POST /api/images/categories - Should reject SELLER from uploading category image (Admin only)")
    void uploadCategoryImage_ForbiddenForSeller() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cat.webp", "image/webp", "webp bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/images/categories").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/images/banners - Should upload image to banners folder")
    void uploadBannerImage_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "banner.jpg",
                "image/jpeg",
                "banner bytes".getBytes()
        );

        ImageUploadResponse response = ImageUploadResponse.builder()
                .imageKey("banners/uuid.jpg")
                .fileUrl("https://bucket.s3.ap-south-2.amazonaws.com/banners/uuid.jpg")
                .fileName("banner.jpg")
                .mimeType("image/jpeg")
                .fileSize(12L)
                .build();

        when(s3ImageService.uploadImage(any(), eq(ImageFolder.BANNERS))).thenReturn(response);

        mockMvc.perform(multipart("/api/images/banners").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.imageKey").value("banners/uuid.jpg"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/images/upload - Should return 400 when image is invalid")
    void uploadImage_InvalidImage_ReturnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "doc.txt",
                "text/plain",
                "text bytes".getBytes()
        );

        when(s3ImageService.uploadImage(any(), eq("products")))
                .thenThrow(new InvalidImageException("Invalid image content type: text/plain"));

        mockMvc.perform(multipart("/api/images/upload")
                        .file(file)
                        .param("folder", "products"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid image content type: text/plain"));
    }

    @Test
    @DisplayName("GET /api/images/download - Should return image bytes with proper content type")
    void downloadImage_Success() throws Exception {
        byte[] imageBytes = "sample image data".getBytes();
        when(s3ImageService.downloadImage("products/test.png")).thenReturn(imageBytes);
        when(s3ImageService.extractKeyFromUrl("products/test.png")).thenReturn("products/test.png");

        mockMvc.perform(get("/api/images/download").param("key", "products/test.png"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE))
                .andExpect(content().bytes(imageBytes));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/images - Should delete image for ADMIN and return 200 OK")
    void deleteImage_AdminSuccess() throws Exception {
        when(s3ImageService.extractKeyFromUrl("products/test.png")).thenReturn("products/test.png");
        doNothing().when(s3ImageService).deleteImage("products/test.png");

        mockMvc.perform(delete("/api/images").param("key", "products/test.png"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Image deleted successfully"));

        verify(s3ImageService).deleteImage("products/test.png");
    }

    @Test
    @DisplayName("DELETE /api/images - Should reject unauthenticated deletion with 401 or 403")
    void deleteImage_Unauthenticated_Rejected() throws Exception {
        mockMvc.perform(delete("/api/images").param("key", "products/test.png"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(status == 401 || status == 403);
                });
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("DELETE /api/images - Should reject customer role with 403 Forbidden")
    void deleteImage_UnauthorizedRoleCustomer_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/images").param("key", "products/test.png"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SELLER")
    @DisplayName("DELETE /api/images - Should allow seller deleting their own product image")
    void deleteImage_SellerOwnsProduct_Success() throws Exception {
        when(s3ImageService.extractKeyFromUrl("products/seller1-prod.jpg")).thenReturn("products/seller1-prod.jpg");
        when(sellerContextUtil.getCurrentSellerId()).thenReturn(10);

        Seller seller = new Seller();
        seller.setSellerId(10);
        Store store = new Store();
        store.setSeller(seller);
        Product product = new Product();
        product.setStore(store);
        product.setImageUrl("https://s3.aws.com/products/seller1-prod.jpg");

        when(productRepository.findByImageUrlContainingKey("products/seller1-prod.jpg")).thenReturn(List.of(product));
        doNothing().when(s3ImageService).deleteImage("products/seller1-prod.jpg");

        mockMvc.perform(delete("/api/images").param("key", "products/seller1-prod.jpg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "SELLER")
    @DisplayName("DELETE /api/images - Should reject seller attempting to delete system banners with 403")
    void deleteImage_SellerAttemptsToDeleteSystemBanner_Forbidden() throws Exception {
        when(s3ImageService.extractKeyFromUrl("banners/summer-sale.jpg")).thenReturn("banners/summer-sale.jpg");

        mockMvc.perform(delete("/api/images").param("key", "banners/summer-sale.jpg"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied: Sellers are not permitted to delete platform or catalog assets."));
    }

    @Test
    @WithMockUser(roles = "SELLER")
    @DisplayName("DELETE /api/images - Should reject seller attempting to delete other seller's product image with 403")
    void deleteImage_SellerAttemptsToDeleteOtherSellerProduct_Forbidden() throws Exception {
        when(s3ImageService.extractKeyFromUrl("products/other-seller-item.jpg")).thenReturn("products/other-seller-item.jpg");
        when(sellerContextUtil.getCurrentSellerId()).thenReturn(10);

        Seller otherSeller = new Seller();
        otherSeller.setSellerId(99); // different seller!
        Store otherStore = new Store();
        otherStore.setSeller(otherSeller);
        Product otherProduct = new Product();
        otherProduct.setStore(otherStore);
        otherProduct.setImageUrl("https://s3.aws.com/products/other-seller-item.jpg");

        when(productRepository.findByImageUrlContainingKey("products/other-seller-item.jpg")).thenReturn(List.of(otherProduct));

        mockMvc.perform(delete("/api/images").param("key", "products/other-seller-item.jpg"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied: You do not own the product associated with this image."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/images/subcategories - Should upload image to subcategories folder for ADMIN")
    void uploadSubcategoryImage_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "subcat.webp", "image/webp", "bytes".getBytes()
        );

        ImageUploadResponse response = ImageUploadResponse.builder()
                .imageKey("subcategories/uuid.webp")
                .fileUrl("https://bucket.s3.ap-south-2.amazonaws.com/subcategories/uuid.webp")
                .fileName("subcat.webp")
                .mimeType("image/webp")
                .fileSize(10L)
                .build();

        when(s3ImageService.uploadImage(any(), eq(ImageFolder.SUBCATEGORIES))).thenReturn(response);

        mockMvc.perform(multipart("/api/images/subcategories").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.imageKey").value("subcategories/uuid.webp"));
    }

    @Test
    @WithMockUser(roles = "SELLER")
    @DisplayName("POST /api/images/documents - Should upload document for SELLER")
    void uploadDocument_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "pdf bytes".getBytes()
        );

        ImageUploadResponse response = ImageUploadResponse.builder()
                .imageKey("documents/uuid.pdf")
                .fileUrl("https://bucket.s3.ap-south-2.amazonaws.com/documents/uuid.pdf")
                .fileName("doc.pdf")
                .mimeType("application/pdf")
                .fileSize(20L)
                .build();

        when(s3ImageService.uploadFile(any(), eq(ImageFolder.DOCUMENTS))).thenReturn(response);

        mockMvc.perform(multipart("/api/images/documents").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.imageKey").value("documents/uuid.pdf"));
    }

    @Test
    @WithMockUser(roles = "SELLER")
    @DisplayName("POST /api/images/multiple - Should upload multiple images for SELLER")
    void uploadMultipleImages_Success() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "files", "img1.jpg", "image/jpeg", "bytes1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "files", "img2.jpg", "image/jpeg", "bytes2".getBytes()
        );

        ImageUploadResponse r1 = ImageUploadResponse.builder().imageKey("products/img1.jpg").build();
        ImageUploadResponse r2 = ImageUploadResponse.builder().imageKey("products/img2.jpg").build();

        when(s3ImageService.uploadImages(any(), eq("products"))).thenReturn(java.util.List.of(r1, r2));

        mockMvc.perform(multipart("/api/images/multiple").file(file1).file(file2).param("folder", "products"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
