package com.example.project.customer;

import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.dto.BulkPriceAdjustmentRequest;
import com.example.project.customer.dto.CategoryResponse;
import com.example.project.customer.dto.InventoryAdjustmentRequest;
import com.example.project.customer.dto.ProductResponse;
import com.example.project.customer.dto.SellerDocumentItemResponse;
import com.example.project.customer.dto.SellerEnquiryItemResponse;
import com.example.project.customer.dto.SellerPricingUpdateRequest;
import com.example.project.customer.dto.SellerProductCreateRequest;
import com.example.project.customer.dto.SellerProductPageResponse;
import com.example.project.customer.dto.SellerQuotationCreateRequest;
import com.example.project.customer.dto.SellerQuotationItemDto;
import com.example.project.customer.dto.SellerQuotationRecordResponse;
import com.example.project.customer.dto.SubcategoryResponse;
import com.example.project.customer.dto.WarehouseRequest;
import com.example.project.customer.entity.Brand;
import com.example.project.customer.entity.Category;
import com.example.project.customer.entity.DocumentType;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.entity.Warehouse;
import com.example.project.customer.repository.BrandRepository;
import com.example.project.customer.repository.InventoryAdjustmentRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.QuotationRepository;
import com.example.project.customer.repository.RfqRepository;
import com.example.project.customer.repository.SellerDocumentRepository;
import com.example.project.customer.repository.SellerRepository;
import com.example.project.customer.repository.WarehouseRepository;
import com.example.project.customer.service.SellerDocumentVaultService;
import com.example.project.customer.service.SellerDocumentVaultServiceImpl;
import com.example.project.customer.service.SellerOnboardingService;
import com.example.project.customer.service.SellerPricingService;
import com.example.project.customer.service.SellerPricingServiceImpl;
import com.example.project.customer.service.SellerProductService;
import com.example.project.customer.service.SellerProductServiceImpl;
import com.example.project.customer.service.SellerQuotationManagementService;
import com.example.project.customer.service.SellerQuotationManagementServiceImpl;
import com.example.project.customer.service.SellerWarehouseService;
import com.example.project.customer.service.SellerWarehouseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"null", "unchecked"})
public class SellerPortalIntegrationFlowsTest {

    @Mock private ProductRepository productRepository;
    @Mock private BrandRepository brandRepository;
    @Mock private SellerRepository sellerRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private InventoryAdjustmentRepository inventoryAdjustmentRepository;
    @Mock private SellerDocumentRepository sellerDocumentRepository;
    @Mock private SellerOnboardingService sellerOnboardingService;
    @Mock private RfqRepository rfqRepository;
    @Mock private QuotationRepository quotationRepository;

    private SellerProductService sellerProductService;
    private SellerWarehouseService sellerWarehouseService;
    private SellerPricingService sellerPricingService;
    private SellerDocumentVaultService sellerDocumentVaultService;
    private SellerQuotationManagementService sellerQuotationService;

    private Category mockCategory;
    private Subcategory mockSubcategory;
    private Brand mockBrand;

    @BeforeEach
    void setUp() {
        sellerProductService = new SellerProductServiceImpl(productRepository, brandRepository, sellerRepository);
        sellerWarehouseService = new SellerWarehouseServiceImpl(warehouseRepository, productRepository, inventoryAdjustmentRepository);
        sellerPricingService = new SellerPricingServiceImpl(productRepository);
        sellerDocumentVaultService = new SellerDocumentVaultServiceImpl(sellerOnboardingService, sellerDocumentRepository);
        sellerQuotationService = new SellerQuotationManagementServiceImpl(rfqRepository, quotationRepository, sellerRepository);

        mockCategory = Category.builder().categoryId(1).name("Civil & Structural").slug("civil-structural").build();
        mockSubcategory = Subcategory.builder().subcategoryId(1).category(mockCategory).name("TMT Steel & Rebars").slug("tmt-steel").build();
        mockBrand = Brand.builder().brandId(1).subcategory(mockSubcategory).name("Tata Tiscon").slug("tata-tiscon").build();
    }

    @Test
    @DisplayName("Flow 1: Cascading Hierarchy DTO serialization includes 'id' and parent-child links")
    void testCascadingHierarchyIds() {
        CategoryResponse catResp = CategoryResponse.builder().categoryId(1).name("Civil & Structural").slug("civil-structural").build();
        assertEquals(1, catResp.getId());

        SubcategoryResponse subResp = SubcategoryResponse.builder().subcategoryId(1).categoryId(1).name("TMT Steel & Rebars").build();
        assertEquals(1, subResp.getId());
        assertEquals(1, subResp.getCategoryId());
    }

    @Test
    @DisplayName("Flow 2: Seller Product Creation auto-assigns sellerId and PENDING status")
    void testSellerProductCreation() {
        when(brandRepository.findById(1)).thenReturn(Optional.of(mockBrand));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setProductId(101);
            return p;
        });

        SellerProductCreateRequest req = SellerProductCreateRequest.builder()
                .brandId(1)
                .title("Tata Tiscon 550D TMT Steel Rebars 16mm")
                .sku("SKU-TATA-550D-16MM")
                .price(BigDecimal.valueOf(64500))
                .sellingPrice(BigDecimal.valueOf(64500))
                .mrp(BigDecimal.valueOf(69000))
                .unit("Ton")
                .moq(5)
                .stockQty(85)
                .build();

        ProductResponse resp = sellerProductService.createSellerProduct(1001, req);

        assertNotNull(resp);
        assertEquals(101, resp.getProductId());
        assertEquals("PENDING", resp.getStatus());
        assertEquals("seller_1001", resp.getSellerId());
        assertEquals("Tata Tiscon", resp.getBrandName());
    }

    @Test
    @DisplayName("Flow 2b: Seller Product Stock & Pricing patch updates")
    void testSellerStockAndPricingUpdates() {
        Product existing = Product.builder()
                .productId(101)
                .sellerId(1001)
                .brand(mockBrand)
                .title("Tata Tiscon 16mm")
                .price(BigDecimal.valueOf(64500))
                .sellingPrice(BigDecimal.valueOf(64500))
                .mrp(BigDecimal.valueOf(69000))
                .stockQty(85)
                .build();

        when(productRepository.findById(101)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // Fast stock patch
        ProductResponse stockUpdated = sellerProductService.updateSellerStock(1001, 101, 150);
        assertEquals(150, stockUpdated.getStockQty());

        // Fast price patch
        SellerPricingUpdateRequest priceReq = SellerPricingUpdateRequest.builder()
                .sellingPrice(BigDecimal.valueOf(62500))
                .mrp(BigDecimal.valueOf(68000))
                .build();
        ProductResponse priceUpdated = sellerProductService.updateSellerPricing(1001, 101, priceReq);
        assertEquals(BigDecimal.valueOf(62500), priceUpdated.getSellingPrice());
        assertEquals(BigDecimal.valueOf(68000), priceUpdated.getMrp());
    }

    @Test
    @DisplayName("Flow 3: Warehouse management and stock inward adjustments")
    void testWarehouseAndInventoryAdjustment() {
        Warehouse wh = Warehouse.builder()
                .warehouseId(1)
                .sellerId(1001)
                .name("Bhiwandi Central Logistics Yard")
                .city("Bhiwandi")
                .isDefault(true)
                .build();

        when(warehouseRepository.findBySellerId(1001)).thenReturn(List.of(wh));
        List<Warehouse> list = sellerWarehouseService.getWarehouses(1001);
        assertEquals(1, list.size());
        assertEquals("wh_1", list.get(0).getId());

        Product product = Product.builder()
                .productId(101)
                .sellerId(1001)
                .stockQty(85)
                .build();

        when(productRepository.findById(101)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryAdjustmentRequest adjReq = InventoryAdjustmentRequest.builder()
                .productId("sp_101")
                .warehouseId("wh_1")
                .adjustmentType("add")
                .quantity(25)
                .reason("New mill batch received")
                .build();

        Map<String, Object> adjResult = sellerWarehouseService.adjustInventory(1001, adjReq);
        assertEquals("sp_101", adjResult.get("productId"));
        assertEquals(110, adjResult.get("newStock"));
    }

    @Test
    @DisplayName("Flow 4: Bulk Price Adjustment percentage calculation across products")
    void testBulkPriceAdjustment() {
        Product p1 = Product.builder().productId(101).sellerId(1001).brand(mockBrand).price(BigDecimal.valueOf(1000.00)).sellingPrice(BigDecimal.valueOf(1000.00)).mrp(BigDecimal.valueOf(1100.00)).build();
        Product p2 = Product.builder().productId(102).sellerId(1001).brand(mockBrand).price(BigDecimal.valueOf(2000.00)).sellingPrice(BigDecimal.valueOf(2000.00)).mrp(BigDecimal.valueOf(2200.00)).build();

        when(productRepository.findBySellerId(1001)).thenReturn(List.of(p1, p2));

        BulkPriceAdjustmentRequest req = BulkPriceAdjustmentRequest.builder()
                .brand("Tata Tiscon")
                .adjustmentType("percentage_increase")
                .value(BigDecimal.valueOf(10.0)) // +10%
                .applyTo("both")
                .build();

        Map<String, Object> result = sellerPricingService.bulkAdjustPricing(1001, req);
        assertEquals(true, result.get("success"));
        assertEquals(2, result.get("modifiedCount"));

        assertEquals(BigDecimal.valueOf(1100.00).setScale(2), p1.getSellingPrice());
        assertEquals(BigDecimal.valueOf(1210.00).setScale(2), p1.getMrp());
        assertEquals(BigDecimal.valueOf(2200.00).setScale(2), p2.getSellingPrice());
        assertEquals(BigDecimal.valueOf(2420.00).setScale(2), p2.getMrp());
    }

    @Test
    @DisplayName("Flow 5: Document Vault type parsing for GSTIN and INCORPORATION")
    void testDocumentTypeParsing() {
        assertEquals(DocumentType.GSTIN, DocumentType.fromString("GSTIN"));
        assertEquals(DocumentType.INCORPORATION, DocumentType.fromString("INCORPORATION"));
        assertEquals(DocumentType.PAN, DocumentType.fromString("pan"));
        assertEquals(DocumentType.MSME, DocumentType.fromString("MSME"));
    }

    @Test
    @DisplayName("Flow 6: Custom Quotation Generation and Total Calculation with GST & Freight")
    void testQuotationDispatchCalculation() {
        when(quotationRepository.save(any())).thenAnswer(inv -> {
            com.example.project.customer.entity.Quotation q = inv.getArgument(0);
            q.setQuoteId(884);
            return q;
        });

        SellerQuotationCreateRequest req = SellerQuotationCreateRequest.builder()
                .enquiryId("enq_2026_01")
                .buyerName("L&T Construction Infra Project")
                .buyerEmail("procurement@intec.lnt.com")
                .validUntil("2026-09-15")
                .items(List.of(
                        SellerQuotationItemDto.builder()
                                .productId("sp_101")
                                .name("Tata Tiscon 550D TMT Steel Rebars 16mm")
                                .quantity(100)
                                .unit("Ton")
                                .quotedRate(BigDecimal.valueOf(61500))
                                .gstRate(BigDecimal.valueOf(18))
                                .build()
                ))
                .freightCharges(BigDecimal.valueOf(25000))
                .paymentTerms("50% Advance, 50% on Delivery")
                .deliveryTimeline("3 Business Days")
                .build();

        Map<String, Object> resp = sellerQuotationService.createQuotation(1001, req);
        assertEquals(true, resp.get("success"));

        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        assertNotNull(data);
        assertEquals("quot_2026_884", data.get("id"));
        assertEquals("SENT", data.get("status"));

        // 100 * 61500 = 6,150,000 + 18% GST (1,107,000) = 7,257,000 + 25000 Freight = 7,282,000
        assertEquals(BigDecimal.valueOf(7282000).setScale(2), ((BigDecimal) data.get("totalAmount")).setScale(2));
    }
}
