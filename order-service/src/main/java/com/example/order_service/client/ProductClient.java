package com.example.order_service.client;

import com.example.order_service.client.dto.ProductClientResponseDTO;
import com.example.order_service.exception.DownstreamAuthorizationException;
import com.example.order_service.exception.ExternalServiceException;
import com.example.order_service.exception.ResourceNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class ProductClient {
    private final RestTemplate restTemplate;

    @Value("${external.product-service-url}")
    private String productServiceUrl;

    public ProductClientResponseDTO getProductById(UUID productId) {
        String url = productServiceUrl + "/api/products/" + productId;
        try {
            return restTemplate.getForObject(url, ProductClientResponseDTO.class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("Product not found");
            }
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new DownstreamAuthorizationException(HttpStatus.UNAUTHORIZED,
                        "Authentication with product service failed");
            }
            if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new DownstreamAuthorizationException(HttpStatus.FORBIDDEN,
                        "The current token is not allowed to access product service");
            }
            throw new ExternalServiceException("Product service unavailable");
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Product service unavailable");
        }
    }
}
