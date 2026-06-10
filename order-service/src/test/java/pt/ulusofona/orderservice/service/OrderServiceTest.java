package pt.ulusofona.orderservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import pt.ulusofona.orderservice.client.ProductResponse;
import pt.ulusofona.orderservice.client.ProductServiceClient;
import pt.ulusofona.orderservice.client.UserResponse;
import pt.ulusofona.orderservice.client.UserServiceClient;
import pt.ulusofona.orderservice.dto.OrderItemRequest;
import pt.ulusofona.orderservice.dto.OrderRequest;
import pt.ulusofona.orderservice.dto.OrderResponse;
import pt.ulusofona.orderservice.model.Order;
import pt.ulusofona.orderservice.model.OrderItem;
import pt.ulusofona.orderservice.model.OrderStatus;
import pt.ulusofona.orderservice.repository.OrderRepository;
import pt.ulusofona.orderservice.sqs.OrderEventSqsPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    @SuppressWarnings("unchecked")
    private ObjectProvider<OrderEventSqsPublisher> orderEventSqsPublisher;

    @InjectMocks
    private OrderService orderService;

    private UserResponse testUser;
    private ProductResponse testProduct;
    private OrderRequest orderRequest;
    private Order savedOrder;

    @BeforeEach
    void setUp() {
        testUser = new UserResponse(
                1L,
                "John Doe",
                "john@example.com",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        testProduct = new ProductResponse(
                1L,
                "Laptop",
                "High-performance laptop",
                new BigDecimal("999.99"),
                10,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        OrderItemRequest itemRequest = new OrderItemRequest(1L, 2);
        orderRequest = new OrderRequest(1L, Arrays.asList(itemRequest));

        savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setUserId(1L);
        savedOrder.setStatus(OrderStatus.PENDING);
        savedOrder.setTotalAmount(new BigDecimal("1999.98"));
        savedOrder.setCreatedAt(LocalDateTime.now());
        savedOrder.setUpdatedAt(LocalDateTime.now());

        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setProductId(1L);
        orderItem.setProductName("Laptop");
        orderItem.setQuantity(2);
        orderItem.setPrice(new BigDecimal("999.99"));
        orderItem.setOrder(savedOrder);
        savedOrder.addOrderItem(orderItem);
    }

    @Test
    void testCreateOrder_Success() {
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(productServiceClient.getProductById(1L)).thenReturn(testProduct);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponse response = orderService.createOrder(orderRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(1, response.getItems().size());
        assertEquals(new BigDecimal("1999.98"), response.getTotalAmount());

        verify(userServiceClient, times(1)).getUserById(1L);
        verify(productServiceClient, times(1)).getProductById(1L);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderEventSqsPublisher, times(1)).ifAvailable(any());
    }

    @Test
    void testCreateOrder_UserNotFound() {
        when(userServiceClient.getUserById(1L))
                .thenThrow(new RuntimeException("User not found"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(orderRequest);
        });

        assertTrue(exception.getMessage().contains("User not found"));
        verify(userServiceClient, times(1)).getUserById(1L);
        verify(productServiceClient, never()).getProductById(anyLong());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCreateOrder_ProductNotFound() {
        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(productServiceClient.getProductById(1L))
                .thenThrow(new RuntimeException("Product not found"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(orderRequest);
        });

        assertTrue(exception.getMessage().contains("Product not found"));
        verify(userServiceClient, times(1)).getUserById(1L);
        verify(productServiceClient, times(1)).getProductById(1L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCreateOrder_InsufficientStock() {
        ProductResponse lowStockProduct = new ProductResponse(
                1L,
                "Laptop",
                "High-performance laptop",
                new BigDecimal("999.99"),
                1,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(productServiceClient.getProductById(1L)).thenReturn(lowStockProduct);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(orderRequest);
        });

        assertTrue(exception.getMessage().contains("Insufficient stock"));
        verify(userServiceClient, times(1)).getUserById(1L);
        verify(productServiceClient, times(1)).getProductById(1L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testGetAllOrders() {
        when(orderRepository.findAll()).thenReturn(Arrays.asList(savedOrder));

        List<OrderResponse> responses = orderService.getAllOrders();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getId());
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void testGetOrderById_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));

        OrderResponse response = orderService.getOrderById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void testGetOrderById_NotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.getOrderById(1L);
        });

        assertTrue(exception.getMessage().contains("Order not found"));
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void testGetOrdersByUserId() {
        when(orderRepository.findByUserId(1L)).thenReturn(Arrays.asList(savedOrder));

        List<OrderResponse> responses = orderService.getOrdersByUserId(1L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getUserId());
        verify(orderRepository, times(1)).findByUserId(1L);
    }

    @Test
    void testUpdateOrderStatus_Success() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        savedOrder.setStatus(OrderStatus.CONFIRMED);

        OrderResponse response = orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

        assertNotNull(response);
        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testUpdateOrderStatus_NotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);
        });

        assertTrue(exception.getMessage().contains("Order not found"));
        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void testCreateOrder_MultipleItems() {
        OrderItemRequest item1 = new OrderItemRequest(1L, 2);
        OrderItemRequest item2 = new OrderItemRequest(2L, 1);
        OrderRequest multiItemRequest = new OrderRequest(1L, Arrays.asList(item1, item2));

        ProductResponse product2 = new ProductResponse(
                2L,
                "Mouse",
                "Wireless mouse",
                new BigDecimal("29.99"),
                5,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        when(productServiceClient.getProductById(1L)).thenReturn(testProduct);
        when(productServiceClient.getProductById(2L)).thenReturn(product2);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponse response = orderService.createOrder(multiItemRequest);

        assertNotNull(response);
        verify(userServiceClient, times(1)).getUserById(1L);
        verify(productServiceClient, times(1)).getProductById(1L);
        verify(productServiceClient, times(1)).getProductById(2L);
        verify(orderRepository, times(1)).save(any(Order.class));
    }
}
