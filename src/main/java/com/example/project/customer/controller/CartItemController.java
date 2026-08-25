package com.example.project.customer.controller;

import com.example.project.customer.dto.CartItemRequest;
import com.example.project.customer.dto.CartItemResponse;
import com.example.project.customer.service.CartItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cart-items")
public class CartItemController {

    private final CartItemService cartItemService;

    public CartItemController(CartItemService cartItemService) {
        this.cartItemService = cartItemService;
    }

    @PostMapping
    public ResponseEntity<CartItemResponse> create(@Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartItemService.create(request));
    }

    @GetMapping("/{id}")
    public CartItemResponse getById(@PathVariable Integer id) {
        return cartItemService.getById(id);
    }

    @GetMapping
    public List<CartItemResponse> getAll(@RequestParam(required = false) Integer customerId) {
        return cartItemService.getAll(customerId);
    }

    @GetMapping("/customer/{customerId}")
    public List<CartItemResponse> getByCustomerId(@PathVariable Integer customerId) {
        return cartItemService.getByCustomerId(customerId);
    }

    @PutMapping("/{id}")
    public CartItemResponse update(@PathVariable Integer id, @Valid @RequestBody CartItemRequest request) {
        return cartItemService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        cartItemService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/customer/{customerId}")
    public ResponseEntity<Void> deleteByCustomerId(@PathVariable Integer customerId) {
        cartItemService.deleteByCustomerId(customerId);
        return ResponseEntity.noContent().build();
    }
}
