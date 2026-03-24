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
                        "folder", "ecommerce/products"  // organises uploads in Cloudinary dashboard
                )
        );
        return (String) uploadResult.get("secure_url");  // e.g. https://res.cloudinary.com/...
    }

    // Delete from Cloudinary using the public_id embedded in the URL
    private void deleteImageIfExists(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        try {
            // Extract public_id from URL: "ecommerce/products/filename" (no extension)
            String publicId = extractPublicId(imageUrl);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException ignored) {}
    }

    // Cloudinary URLs look like: https://res.cloudinary.com/cloud/image/upload/v123/ecommerce/products/filename.jpg
    // public_id is everything after "/upload/vXXX/" and before the file extension
    private String extractPublicId(String url) {
        String[] parts = url.split("/upload/");
        String afterUpload = parts[1];                          // "v123/ecommerce/products/filename.jpg"
        String withoutVersion = afterUpload.replaceFirst("v\\d+/", "");  // "ecommerce/products/filename.jpg"
        int dotIndex = withoutVersion.lastIndexOf('.');
        return dotIndex != -1
                ? withoutVersion.substring(0, dotIndex)            // "ecommerce/products/filename"
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
            deleteImageIfExists(existing.getImagePath()); // delete old image from Cloudinary
            existing.setImagePath(saveImage(imageFile));  // upload new, store URL
        }

        return productRepository.save(existing);
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        deleteImageIfExists(product.getImagePath());
        productRepository.deleteById(id);
    }

    public List<Product> searchProducts(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }
}