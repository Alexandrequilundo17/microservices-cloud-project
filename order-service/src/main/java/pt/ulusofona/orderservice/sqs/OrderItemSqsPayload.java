package pt.ulusofona.orderservice.sqs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderItemSqsPayload(
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal price
) {}
