package pt.ulusofona.orderservice.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.ulusofona.orderservice.model.Order;
import pt.ulusofona.orderservice.model.OrderItem;
import pt.ulusofona.orderservice.model.OrderStatus;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventSqsPublisherTest {

    @Mock
    private SqsClient sqsClient;

    private OrderEventSqsPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OrderEventSqsPublisher(
                sqsClient,
                new ObjectMapper().findAndRegisterModules(),
                "https://sqs.eu-central-1.amazonaws.com/263050932702/order-created"
        );
    }

    @Test
    void publishOrderCreated_sendsJsonWithEventType() throws Exception {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(42L);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("199.99"));
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        OrderItem item = new OrderItem();
        item.setProductId(10L);
        item.setProductName("Widget");
        item.setQuantity(2);
        item.setPrice(new BigDecimal("99.99"));
        item.setOrder(order);
        order.addOrderItem(item);

        publisher.publishOrderCreated(order);

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());
        SendMessageRequest req = captor.getValue();
        assertEquals("https://sqs.eu-central-1.amazonaws.com/263050932702/order-created", req.queueUrl());
        assertTrue(req.messageBody().contains("\"eventType\":\"OrderCreated\""));
        assertTrue(req.messageBody().contains("\"orderId\":1"));
        assertTrue(req.messageAttributes().containsKey("eventType"));
    }
}
