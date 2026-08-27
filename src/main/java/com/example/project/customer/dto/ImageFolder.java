package com.example.project.customer.dto;

public enum ImageFolder {
    PRODUCTS("products"),
    CATEGORIES("categories"),
    BANNERS("banners"),
    DOCUMENTS("documents"),
    ATTACHMENTS("attachments"),
    OTHER("other");

    private final String folderName;

    ImageFolder(String folderName) {
        this.folderName = folderName;
    }

    public String getFolderName() {
        return folderName;
    }

    public static ImageFolder fromString(String text) {
        if (text == null || text.isBlank()) {
            return OTHER;
        }
        for (ImageFolder folder : ImageFolder.values()) {
            if (folder.folderName.equalsIgnoreCase(text.trim()) || folder.name().equalsIgnoreCase(text.trim())) {
                return folder;
            }
        }
        return OTHER;
    }
}
