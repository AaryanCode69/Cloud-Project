package com.example.order_service.client;

import com.example.order_service.client.dto.ReduceInventoryRequestDTO;
import com.example.order_service.client.dto.ReduceInventoryResponseDTO;
import com.example.order_service.exception.DownstreamAuthorizationException;
import com.example.order_service.exception.ExternalServiceException;
import com.example.order_service.exception.ResourceNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class InventoryClient implements InventoryGateway {
    private final RestTemplate restTemplate;

    @Value("${external.inventory-service-url}")
    private String inventoryServiceUrl;

    @Override
    public void reduceInventory(UUID productId, int quantity) {
        String url = inventoryServiceUrl + "/api/inventory/reduce";
        ReduceInventoryRequestDTO request = new ReduceInventoryRequestDTO(productId, quantity);

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(request),
                    ReduceInventoryResponseDTO.class
            );
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("Inventory not found for product");
            }
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new DownstreamAuthorizationException(HttpStatus.UNAUTHORIZED,
                        "Authentication with inventory service failed");
            }
            if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new DownstreamAuthorizationException(HttpStatus.FORBIDDEN,
                        "The current token is not allowed to access inventory service");
            }
            if (ex.getStatusCode() == HttpStatus.CONFLICT || ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new IllegalStateException("Insufficient inventory");
            }
            throw new ExternalServiceException("Inventory service unavailable");
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Inventory service unavailable");
        }
    }
}
