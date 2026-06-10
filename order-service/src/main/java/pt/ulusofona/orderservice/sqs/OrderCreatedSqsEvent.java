package pt.ulusofona.orderservice.sqs;

import pt.ulusofona.orderservice.model.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record OrderCreatedSqsEvent(
        String eventType,
        Long orderId,
        Long userId,
        List<OrderItemSqsPayload> items,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {
    public static OrderCreatedSqsEvent fromOrder(Order order) {
        return new OrderCreatedSqsEvent(
                "OrderCreated",
                order.getId(),
                order.getUserId(),
                order.getOrderItems().stream()
                        .map(i -> new OrderItemSqsPayload(
                                i.getProductId(),
                                i.getProductName(),
                                i.getQuantity(),
                                i.getPrice()))
                        .collect(Collectors.toList()),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
    }
}
