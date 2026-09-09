package com.example.project.customer.exception;

import com.example.project.customer.entity.Store;
import lombok.Getter;

@Getter
public class StoreMismatchException extends RuntimeException {

    private final Integer currentStoreId;
    private final String currentStoreName;
    private final String currentStoreSlug;
    private final Integer newStoreId;
    private final String newStoreName;
    private final String newStoreSlug;

    public StoreMismatchException(Store currentStore, Store newStore) {
        super(String.format("Your cart contains items from '%s'. Switch store to add items from '%s'?",
                currentStore != null ? currentStore.getName() : "Unknown Store",
                newStore != null ? newStore.getName() : "New Store"));

        this.currentStoreId = currentStore != null ? currentStore.getStoreId() : null;
        this.currentStoreName = currentStore != null ? currentStore.getName() : null;
        this.currentStoreSlug = currentStore != null ? currentStore.getSlug() : null;

        this.newStoreId = newStore != null ? newStore.getStoreId() : null;
        this.newStoreName = newStore != null ? newStore.getName() : null;
        this.newStoreSlug = newStore != null ? newStore.getSlug() : null;
    }
}
