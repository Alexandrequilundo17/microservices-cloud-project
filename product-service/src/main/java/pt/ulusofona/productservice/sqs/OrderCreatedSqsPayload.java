package pt.ulusofona.productservice.sqs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCreatedSqsPayload(
        String eventType,
        Long orderId,
        Long userId,
        List<OrderItemSqsPayload> items,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {}
