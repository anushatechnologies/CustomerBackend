package com.example.project.customer.controller;

import com.example.project.customer.dto.SubcategoryRequest;
import com.example.project.customer.dto.SubcategoryResponse;
import com.example.project.customer.service.SubcategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/subcategories")
public class SubcategoryController {
    private final SubcategoryService service;
    public SubcategoryController(SubcategoryService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<SubcategoryResponse> create(@Valid @RequestBody SubcategoryRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request)); }
    @GetMapping("/{id}")
    public SubcategoryResponse getById(@PathVariable Integer id) { return service.getById(id); }
    @GetMapping
    public List<SubcategoryResponse> getAll() { return service.getAll(); }
    @PutMapping("/{id}")
    public SubcategoryResponse update(@PathVariable Integer id, @Valid @RequestBody SubcategoryRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) { service.delete(id); return ResponseEntity.noContent().build(); }
}