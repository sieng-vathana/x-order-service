package com.x.order.service;

import com.sharedlib.response.ApiResponse;
import com.x.order.dto.ProductVariantSaleResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ProductClient {
    private final RestClient client;

    public ProductClient(RestClient.Builder builder,
                         @Value("${services.product.base-url:http://127.0.0.1:8082}") String baseUrl) {
        this.client = OrderServiceRestClient.create(builder, baseUrl, "/api/v1/products");
    }

    public ProductVariantSaleResponse getVariant(Long variantId) {
        ApiResponse<ProductVariantSaleResponse> response = client.get().uri("/variants/{id}", variantId)
                .retrieve().body(new ParameterizedTypeReference<>() {});
        if (response == null || response.getData() == null) {
            throw new IllegalStateException("Product service returned no variant");
        }
        return response.getData();
    }
}
