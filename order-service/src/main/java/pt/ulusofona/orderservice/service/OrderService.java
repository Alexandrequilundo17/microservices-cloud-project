package pt.ulusofona.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.ulusofona.orderservice.client.ProductResponse;
import pt.ulusofona.orderservice.client.ProductServiceClient;
import pt.ulusofona.orderservice.client.UserResponse;
import pt.ulusofona.orderservice.client.UserServiceClient;
import pt.ulusofona.orderservice.dto.OrderItemRequest;
import pt.ulusofona.orderservice.dto.OrderItemResponse;
import pt.ulusofona.orderservice.dto.OrderRequest;
import pt.ulusofona.orderservice.dto.OrderResponse;
import pt.ulusofona.orderservice.model.Order;
import pt.ulusofona.orderservice.model.OrderItem;
import pt.ulusofona.orderservice.model.OrderStatus;
import pt.ulusofona.orderservice.repository.OrderRepository;
import pt.ulusofona.orderservice.sqs.OrderEventSqsPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;
    private final ObjectProvider<OrderEventSqsPublisher> orderEventSqsPublisher;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for user ID: {}", request.getUserId());

        try {
            UserResponse user = userServiceClient.getUserById(request.getUserId());
            log.debug("User validated: {}", user.getName());
        } catch (Exception e) {
            log.error("User validation failed for user ID: {}", request.getUserId(), e);
            throw new RuntimeException("User not found with ID: " + request.getUserId());
        }

        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setStatus(OrderStatus.PENDING);

        for (OrderItemRequest itemRequest : request.getItems()) {
            ProductResponse product;
            try {
                product = productServiceClient.getProductById(itemRequest.getProductId());
                log.debug("Product validated: {} - Price: {}", product.getName(), product.getPrice());
            } catch (Exception e) {
                log.error("Product validation failed for product ID: {}", itemRequest.getProductId(), e);
                throw new RuntimeException("Product not found with ID: " + itemRequest.getProductId());
            }

            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new RuntimeException(
                    String.format("Insufficient stock for product %s. Available: %d, Requested: %d",
                        product.getName(), product.getStockQuantity(), itemRequest.getQuantity()));
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(product.getPrice());
            order.addOrderItem(orderItem);
        }

        order.calculateTotal();

        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {}", savedOrder.getId());

        orderEventSqsPublisher.ifAvailable(publisher -> {
            try {
                publisher.publishOrderCreated(savedOrder);
            } catch (Exception ex) {
                log.warn("SQS order-created publish failed: {}", ex.getMessage());
            }
        });

        return mapToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + id));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus newStatus) {
        log.info("Updating order {} status to {}", id, newStatus);

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + id));

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        log.info("Order {} status updated from {} to {}", id, previousStatus, newStatus);

        return mapToResponse(updatedOrder);
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems() != null ?
                order.getOrderItems().stream()
                        .map(item -> new OrderItemResponse(
                                item.getId(),
                                item.getProductId(),
                                item.getProductName(),
                                item.getQuantity(),
                                item.getPrice()))
                        .collect(Collectors.toList()) :
                List.of();

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                items,
                order.getTotalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
