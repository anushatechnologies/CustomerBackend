package com.example.project.customer.service;

import com.example.project.customer.dto.CartItemRequest;
import com.example.project.customer.dto.CartItemResponse;
import com.example.project.customer.entity.CartItem;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Product;
import com.example.project.customer.entity.Subcategory;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.CartItemRepository;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartItemServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartItemServiceImpl cartItemService;

    private Customer customer;
    private Product product;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setCustomerId(1);
        customer.setName("John Doe");
        customer.setEmail("john@example.com");
        customer.setPhone("1234567890");

        product = new Product();
        product.setProductId(10);
        product.setTitle("Organic Apple");
        product.setPrice(new BigDecimal("2.50"));
        product.setStockQty(50);
        product.setUnit("kg");
        product.setImageUrl("https://example.com/apple.png");
        product.setActive(true);

        cartItem = new CartItem(customer, product, 3);
        cartItem.setCartItemId(100);
        cartItem.setAddedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Create cart item - success for new item")
    void create_Success_NewItem() {
        CartItemRequest request = new CartItemRequest(1, 10, 3);

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCustomerCustomerIdAndProductProductId(1, 10)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem saved = invocation.getArgument(0);
            saved.setCartItemId(100);
            saved.setAddedAt(LocalDateTime.now());
            return saved;
        });

        CartItemResponse response = cartItemService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.cartItemId()).isEqualTo(100);
        assertThat(response.customerId()).isEqualTo(1);
        assertThat(response.productId()).isEqualTo(10);
        assertThat(response.quantity()).isEqualTo(3);
        assertThat(response.price()).isEqualByComparingTo("2.50");
        assertThat(response.itemTotal()).isEqualByComparingTo("7.50");
    }

    @Test
    @DisplayName("Create cart item - success when item already exists, merges quantity")
    void create_Success_ExistingItem_MergesQuantity() {
        CartItemRequest request = new CartItemRequest(1, 10, 2);

        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCustomerCustomerIdAndProductProductId(1, 10)).thenReturn(Optional.of(cartItem));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);

        CartItemResponse response = cartItemService.create(request);

        assertThat(response).isNotNull();
        assertThat(cartItem.getQuantity()).isEqualTo(5); // 3 + 2
    }

    @Test
    @DisplayName("Create cart item - throws when customer not found")
    void create_CustomerNotFound() {
        CartItemRequest request = new CartItemRequest(999, 10, 1);
        when(customerRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartItemService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found with id: 999");
    }

    @Test
    @DisplayName("Create cart item - throws when product not found")
    void create_ProductNotFound() {
        CartItemRequest request = new CartItemRequest(1, 999, 1);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartItemService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id: 999");
    }

    @Test
    @DisplayName("Create cart item - throws when product is inactive")
    void create_ProductInactive() {
        product.setActive(false);
        CartItemRequest request = new CartItemRequest(1, 10, 1);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartItemService.create(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Product is currently inactive");
    }

    @Test
    @DisplayName("Create cart item - throws when quantity exceeds stock")
    void create_ExceedsStock() {
        CartItemRequest request = new CartItemRequest(1, 10, 100);
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCustomerCustomerIdAndProductProductId(1, 10)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartItemService.create(request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("exceeds available stock");
    }

    @Test
    @DisplayName("Get cart item by ID - success")
    void getById_Success() {
        when(cartItemRepository.findById(100)).thenReturn(Optional.of(cartItem));

        CartItemResponse response = cartItemService.getById(100);

        assertThat(response).isNotNull();
        assertThat(response.cartItemId()).isEqualTo(100);
        assertThat(response.productTitle()).isEqualTo("Organic Apple");
    }

    @Test
    @DisplayName("Get cart item by ID - not found")
    void getById_NotFound() {
        when(cartItemRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartItemService.getById(999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cart item not found with id: 999");
    }

    @Test
    @DisplayName("Get all cart items without filter")
    void getAll_NoFilter() {
        when(cartItemRepository.findAll()).thenReturn(List.of(cartItem));

        List<CartItemResponse> responses = cartItemService.getAll(null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).cartItemId()).isEqualTo(100);
    }

    @Test
    @DisplayName("Get all cart items with customerId filter")
    void getAll_WithCustomerId() {
        when(customerRepository.existsById(1)).thenReturn(true);
        when(cartItemRepository.findByCustomerCustomerId(1)).thenReturn(List.of(cartItem));

        List<CartItemResponse> responses = cartItemService.getAll(1);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).customerId()).isEqualTo(1);
    }

    @Test
    @DisplayName("Update cart item - success")
    void update_Success() {
        CartItemRequest request = new CartItemRequest(1, 10, 5);

        when(cartItemRepository.findById(100)).thenReturn(Optional.of(cartItem));
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10)).thenReturn(Optional.of(product));
        when(cartItemRepository.existsByCustomerCustomerIdAndProductProductIdAndCartItemIdNot(1, 10, 100)).thenReturn(false);
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);

        CartItemResponse response = cartItemService.update(100, request);

        assertThat(response).isNotNull();
        assertThat(cartItem.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("Update cart item - duplicate conflict")
    void update_DuplicateConflict() {
        CartItemRequest request = new CartItemRequest(1, 10, 5);

        when(cartItemRepository.findById(100)).thenReturn(Optional.of(cartItem));
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10)).thenReturn(Optional.of(product));
        when(cartItemRepository.existsByCustomerCustomerIdAndProductProductIdAndCartItemIdNot(1, 10, 100)).thenReturn(true);

        assertThatThrownBy(() -> cartItemService.update(100, request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("Another cart item already exists");
    }

    @Test
    @DisplayName("Delete cart item by ID - success")
    void delete_Success() {
        when(cartItemRepository.findById(100)).thenReturn(Optional.of(cartItem));

        cartItemService.delete(100);

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    @DisplayName("Delete cart items by customer ID - success")
    void deleteByCustomerId_Success() {
        when(customerRepository.existsById(1)).thenReturn(true);

        cartItemService.deleteByCustomerId(1);

        verify(cartItemRepository).deleteByCustomerCustomerId(1);
    }

    @Test
    @DisplayName("Delete cart items by customer ID - customer not found")
    void deleteByCustomerId_CustomerNotFound() {
        when(customerRepository.existsById(999)).thenReturn(false);

        assertThatThrownBy(() -> cartItemService.deleteByCustomerId(999))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found with id: 999");
    }
}
