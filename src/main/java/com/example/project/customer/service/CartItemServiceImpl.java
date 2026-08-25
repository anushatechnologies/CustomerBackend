package com.example.project.customer.service;

import com.example.project.customer.dto.CartItemRequest;
import com.example.project.customer.dto.CartItemResponse;
import com.example.project.customer.entity.CartItem;
import com.example.project.customer.entity.Customer;
import com.example.project.customer.entity.Product;
import com.example.project.customer.exception.ResourceConflictException;
import com.example.project.customer.exception.ResourceNotFoundException;
import com.example.project.customer.repository.CartItemRepository;
import com.example.project.customer.repository.CustomerRepository;
import com.example.project.customer.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CartItemServiceImpl implements CartItemService {

    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public CartItemServiceImpl(CartItemRepository cartItemRepository,
                               CustomerRepository customerRepository,
                               ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    @Override
    public CartItemResponse create(CartItemRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.customerId()));

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        if (!product.isActive()) {
            throw new ResourceConflictException("Product is currently inactive: " + product.getTitle());
        }

        Optional<CartItem> existingItemOpt = cartItemRepository
                .findByCustomerCustomerIdAndProductProductId(request.customerId(), request.productId());

        CartItem cartItem;
        if (existingItemOpt.isPresent()) {
            cartItem = existingItemOpt.get();
            int newQuantity = cartItem.getQuantity() + request.quantity();
            if (newQuantity > product.getStockQty()) {
                throw new ResourceConflictException("Total requested quantity (" + newQuantity
                        + ") exceeds available stock (" + product.getStockQty() + ") for product: " + product.getTitle());
            }
            cartItem.setQuantity(newQuantity);
        } else {
            if (request.quantity() > product.getStockQty()) {
                throw new ResourceConflictException("Requested quantity (" + request.quantity()
                        + ") exceeds available stock (" + product.getStockQty() + ") for product: " + product.getTitle());
            }
            cartItem = new CartItem(customer, product, request.quantity());
        }

        return toResponse(cartItemRepository.save(cartItem));
    }

    @Override
    @Transactional(readOnly = true)
    public CartItemResponse getById(Integer id) {
        return toResponse(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItemResponse> getAll(Integer customerId) {
        if (customerId != null) {
            return getByCustomerId(customerId);
        }
        return cartItemRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItemResponse> getByCustomerId(Integer customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }
        return cartItemRepository.findByCustomerCustomerId(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CartItemResponse update(Integer id, CartItemRequest request) {
        CartItem cartItem = find(id);

        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + request.customerId()));

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        if (!product.isActive()) {
            throw new ResourceConflictException("Product is currently inactive: " + product.getTitle());
        }

        if (cartItemRepository.existsByCustomerCustomerIdAndProductProductIdAndCartItemIdNot(
                request.customerId(), request.productId(), id)) {
            throw new ResourceConflictException("Another cart item already exists for customer id "
                    + request.customerId() + " and product id " + request.productId());
        }

        if (request.quantity() > product.getStockQty()) {
            throw new ResourceConflictException("Requested quantity (" + request.quantity()
                    + ") exceeds available stock (" + product.getStockQty() + ") for product: " + product.getTitle());
        }

        cartItem.setCustomer(customer);
        cartItem.setProduct(product);
        cartItem.setQuantity(request.quantity());

        return toResponse(cartItemRepository.save(cartItem));
    }

    @Override
    public void delete(Integer id) {
        cartItemRepository.delete(find(id));
    }

    @Override
    public void deleteByCustomerId(Integer customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found with id: " + customerId);
        }
        cartItemRepository.deleteByCustomerCustomerId(customerId);
    }

    private CartItem find(Integer id) {
        return cartItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + id));
    }

    private CartItemResponse toResponse(CartItem cartItem) {
        Product product = cartItem.getProduct();
        Customer customer = cartItem.getCustomer();
        BigDecimal price = product.getPrice();
        BigDecimal itemTotal = price != null ? price.multiply(BigDecimal.valueOf(cartItem.getQuantity())) : BigDecimal.ZERO;

        return new CartItemResponse(
                cartItem.getCartItemId(),
                customer.getCustomerId(),
                customer.getName(),
                product.getProductId(),
                product.getTitle(),
                price,
                product.getUnit(),
                product.getImageUrl(),
                cartItem.getQuantity(),
                itemTotal,
                cartItem.getAddedAt()
        );
    }
}
