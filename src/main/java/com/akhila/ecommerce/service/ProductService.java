package com.akhila.ecommerce.service;

import com.akhila.ecommerce.model.Product;
import com.akhila.ecommerce.repository.ProductRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final Cloudinary cloudinary;

    // Upload to Cloudinary, returns the secure HTTPS image URL
    public String saveImage(MultipartFile file) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "ecommerce/products"
                )
        );
        return (String) uploadResult.get("secure_url");
    }

    // Delete from Cloudinary using the public_id embedded in the URL
    private void deleteImageIfExists(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        try {
            String publicId = extractPublicId(imageUrl);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException ignored) {}
    }

    private String extractPublicId(String url) {
        String[] parts = url.split("/upload/");
        String afterUpload = parts[1];
        String withoutVersion = afterUpload.replaceFirst("v\\d+/", "");
        int dotIndex = withoutVersion.lastIndexOf('.');
        return dotIndex != -1
                ? withoutVersion.substring(0, dotIndex)
                : withoutVersion;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product updatedProduct, MultipartFile imageFile) throws IOException {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        existing.setName(updatedProduct.getName());
        existing.setDescription(updatedProduct.getDescription());
        existing.setPrice(updatedProduct.getPrice());

        if (imageFile != null && !imageFile.isEmpty()) {
            deleteImageIfExists(existing.getImageUrl());  // ← fixed
            existing.setImageUrl(saveImage(imageFile));   // ← fixed
        }

        return productRepository.save(existing);
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        deleteImageIfExists(product.getImageUrl());  // ← fixed
        productRepository.deleteById(id);
    }

    public List<Product> searchProducts(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }
}