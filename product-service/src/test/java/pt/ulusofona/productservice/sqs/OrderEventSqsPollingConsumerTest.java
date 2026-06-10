package pt.ulusofona.productservice.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.ulusofona.productservice.event.OrderCreatedEvent;
import pt.ulusofona.productservice.service.OrderEventConsumer;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventSqsPollingConsumerTest {

    @Mock
    private SqsClient sqsClient;

    @Mock
    private OrderEventConsumer orderEventConsumer;

    private OrderEventSqsPollingConsumer consumer;
    private OrderEventSqsProperties properties;

    @BeforeEach
    void setUp() {
        properties = new OrderEventSqsProperties();
        properties.setQueueUrl("https://sqs.eu-central-1.amazonaws.com/263050932702/order-created");
        properties.setMaxNumberOfMessages(10);
        properties.setWaitTimeSeconds(20);
        consumer = new OrderEventSqsPollingConsumer(
                sqsClient,
                new ObjectMapper().findAndRegisterModules(),
                properties,
                orderEventConsumer
        );
    }

    @Test
    void pollQueue_deletesAfterSuccessfulProcess() throws Exception {
        OrderCreatedSqsPayload payload = new OrderCreatedSqsPayload(
                "OrderCreated",
                1L,
                42L,
                List.of(new OrderItemSqsPayload(10L, "Widget", 2, new BigDecimal("99.99"))),
                new BigDecimal("199.98"),
                LocalDateTime.now()
        );
        String body = new ObjectMapper().findAndRegisterModules().writeValueAsString(payload);

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(
                ReceiveMessageResponse.builder()
                        .messages(
                                Message.builder()
                                        .messageId("m1")
                                        .receiptHandle("rh1")
                                        .body(body)
                                        .build()
                        )
                        .build()
        );

        consumer.pollQueue();

        verify(orderEventConsumer, times(1)).handleOrderCreated(any(OrderCreatedEvent.class));
        ArgumentCaptor<DeleteMessageRequest> deleteCaptor = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(sqsClient).deleteMessage(deleteCaptor.capture());
        assertEquals("https://sqs.eu-central-1.amazonaws.com/263050932702/order-created", deleteCaptor.getValue().queueUrl());
        assertEquals("rh1", deleteCaptor.getValue().receiptHandle());
    }

    @Test
    void pollQueue_skipsDeleteOnConsumerError() {
        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class))).thenReturn(
                ReceiveMessageResponse.builder()
                        .messages(
                                Message.builder()
                                        .messageId("m2")
                                        .receiptHandle("rh2")
                                        .body("invalid-json")
                                        .build()
                        )
                        .build()
        );

        assertDoesNotThrow(() -> consumer.pollQueue());

        verify(orderEventConsumer, never()).handleOrderCreated(any());
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }
}
