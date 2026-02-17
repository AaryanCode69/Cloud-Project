package com.example.order_service.client;

import com.example.order_service.client.dto.ReduceInventoryRequestDTO;
import com.example.order_service.client.dto.ReduceInventoryResponseDTO;
import com.example.order_service.exception.ExternalServiceException;
import com.example.order_service.exception.ResourceNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class InventoryClient {
    private final RestTemplate restTemplate;

    @Value("${external.inventory-service-url}")
    private String inventoryServiceUrl;

    public ReduceInventoryResponseDTO reduceInventory(UUID productId, int quantity) {
        String url = inventoryServiceUrl + "/api/inventory/reduce";
        ReduceInventoryRequestDTO request = new ReduceInventoryRequestDTO(productId, quantity);

        try {
            ResponseEntity<ReduceInventoryResponseDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(request),
                    ReduceInventoryResponseDTO.class
            );
            return response.getBody();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("Inventory not found for product");
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

