package com.example.project.customer.service;

import com.example.project.customer.config.SellerContextUtil;
import com.example.project.customer.entity.ApprovalStatus;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Role;
import com.example.project.customer.entity.Seller;
import com.example.project.customer.entity.Store;
import com.example.project.customer.entity.StoreStatus;
import com.example.project.customer.entity.VerificationStatus;
import com.example.project.customer.exception.ForbiddenException;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.ProductRepository;
import com.example.project.customer.repository.SellerDocumentRepository;
import com.example.project.customer.repository.SellerRepository;
import com.example.project.customer.repository.StoreRepository;
import com.example.project.customer.security.FirebaseUserPrincipal;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class SellerSoftDeleteTest {

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private SellerDocumentRepository documentRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private FirebaseAuthService firebaseAuthService;

    @Mock
    private S3ImageService s3ImageService;

    private SellerOnboardingServiceImpl onboardingService;
    private SellerProductServiceImpl sellerProductService;

    private Seller seller;
    private Store store;
    private Product product1;
    private Product product2;
    private Customer customer;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        onboardingService = new SellerOnboardingServiceImpl(
                sellerRepository,
                documentRepository,
                customerRepository,
                storeRepository,
                productRepository,
                s3ImageService,
                firebaseAuthService,
                validator
        );

        sellerProductService = new SellerProductServiceImpl(
                productRepository,
                null,
                sellerRepository,
                storeRepository
        );

        seller = Seller.builder()
                .sellerId(10)
                .name("Test Merchant")
                .email("merchant@hinchmart.com")
                .phone("+919876543210")
                .verificationStatus(VerificationStatus.VERIFIED)
                .isDeleted(false)
                .build();

        store = Store.builder()
                .storeId(100)
                .seller(seller)
                .name("Test Store")
                .slug("test-store")
                .status(StoreStatus.ACTIVE)
                .build();

        product1 = Product.builder()
                .productId(201)
                .seller(seller)
                .store(store)
                .title("Cement Bag 50kg")
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();

        product2 = Product.builder()
                .productId(202)
                .seller(seller)
                .store(store)
                .title("Steel Rebar 12mm")
                .active(true)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();

        customer = Customer.builder()
                .customerId(50)
                .email("merchant@hinchmart.com")
                .role(Role.SELLER.name())
                .firebaseUid("fb-uid-seller-10")
                .build();
    }

    @Test
    @DisplayName("softDeleteSeller - Marks seller isDeleted=true, closes store, deactivates products, demotes customer role")
    void softDeleteSeller_FullLifecycle_Success() {
        when(sellerRepository.findById(10)).thenReturn(Optional.of(seller));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(i -> i.getArgument(0));
        when(storeRepository.findBySeller_SellerId(10)).thenReturn(Optional.of(store));
        when(productRepository.findBySellerId(10)).thenReturn(List.of(product1, product2));
        when(customerRepository.findByEmailIgnoreCase("merchant@hinchmart.com")).thenReturn(Optional.of(customer));

        Seller result = onboardingService.softDeleteSeller(10, "Breach of marketplace terms");

        // 1. Seller soft delete assertion
        assertThat(result.isDeleted()).isTrue();
        assertThat(result.getIsDeleted()).isTrue();
        assertThat(result.getDeletedAt()).isNotNull();
        assertThat(result.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
        verify(sellerRepository).save(seller);

        // 2. Store closure assertion
        assertThat(store.getStatus()).isEqualTo(StoreStatus.CLOSED);
        verify(storeRepository).save(store);

        // 3. Products deactivation assertion
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Product>> productCaptor = ArgumentCaptor.forClass(List.class);
        verify(productRepository).saveAll(productCaptor.capture());
        List<Product> deactivated = productCaptor.getValue();
        assertThat(deactivated).hasSize(2);
        assertThat(deactivated).allMatch(p -> !p.getActive()
                && p.getApprovalStatus() == ApprovalStatus.REJECTED
                && "Breach of marketplace terms".equals(p.getRejectionReason()));

        // 4. Customer role demoted
        assertThat(customer.getRole()).isEqualTo(Role.CUSTOMER.name());
        verify(customerRepository).save(customer);
        verify(firebaseAuthService).setUserRoleClaim("fb-uid-seller-10", Role.CUSTOMER);
    }

    @Test
    @DisplayName("getAllSellersForAdmin - Excludes soft-deleted sellers by default; includes them when includeDeleted=true")
    void getAllSellersForAdmin_FiltersSoftDeletedProperly() {
        Seller activeSeller = Seller.builder()
                .sellerId(11)
                .name("Active Seller")
                .email("active@hinchmart.com")
                .isDeleted(false)
                .build();

        Seller deletedSeller = Seller.builder()
                .sellerId(12)
                .name("Deleted Seller")
                .email("deleted@hinchmart.com")
                .isDeleted(true)
                .build();

        when(sellerRepository.findAll()).thenReturn(List.of(activeSeller, deletedSeller));

        // Default (includeDeleted=false)
        List<Seller> activeOnly = onboardingService.getAllSellersForAdmin(null, null);
        assertThat(activeOnly).hasSize(1);
        assertThat(activeOnly.get(0).getSellerId()).isEqualTo(11);

        // Explicit includeDeleted=false
        List<Seller> activeOnlyExplicit = onboardingService.getAllSellersForAdmin(null, null, false);
        assertThat(activeOnlyExplicit).hasSize(1);
        assertThat(activeOnlyExplicit.get(0).getSellerId()).isEqualTo(11);

        // includeDeleted=true
        List<Seller> all = onboardingService.getAllSellersForAdmin(null, null, true);
        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("SellerContextUtil - Rejects soft-deleted seller from accessing seller portal")
    void sellerContextUtil_RejectsSoftDeletedSeller() {
        Seller deletedSeller = Seller.builder()
                .sellerId(10)
                .name("Deleted Seller")
                .email("deleted@hinchmart.com")
                .isDeleted(true)
                .build();

        when(sellerRepository.findFirstByEmailIgnoreCase("deleted@hinchmart.com"))
                .thenReturn(Optional.of(deletedSeller));

        FirebaseUserPrincipal principal = FirebaseUserPrincipal.builder()
                .firebaseUid("fb-deleted-uid")
                .email("deleted@hinchmart.com")
                .role(Role.SELLER)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        SellerContextUtil util = new SellerContextUtil(sellerRepository, storeRepository);

        try {
            assertThatThrownBy(util::getCurrentSellerId)
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("deactivated / deleted");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("deleteSellerProduct - Soft deletes (deactivates) product instead of hard delete")
    void deleteSellerProduct_SoftDeletesInsteadOfHardDelete() {
        when(productRepository.findById(201)).thenReturn(Optional.of(product1));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        sellerProductService.deleteSellerProduct(10, 201);

        // Product is deactivated, not removed from DB
        assertThat(product1.getActive()).isFalse();
        assertThat(product1.getApprovalStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(product1.getRejectionReason()).isEqualTo("Removed from inventory by seller");

        verify(productRepository).save(product1);
        verify(productRepository, never()).delete(any(Product.class));
    }
}
