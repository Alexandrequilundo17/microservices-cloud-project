package pt.ulusofona.productservice.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import pt.ulusofona.productservice.event.OrderCreatedEvent;
import pt.ulusofona.productservice.event.OrderItemEvent;
import pt.ulusofona.productservice.service.OrderEventConsumer;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class OrderEventSqsPollingConsumer {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final OrderEventSqsProperties properties;
    private final OrderEventConsumer orderEventConsumer;

    @Scheduled(fixedDelayString = "${cloud.sqs.order-created-consumer.poll-interval-ms:5000}")
    public void pollQueue() {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(properties.getQueueUrl())
                .maxNumberOfMessages(properties.getMaxNumberOfMessages())
                .waitTimeSeconds(properties.getWaitTimeSeconds())
                .build();

        List<Message> messages = sqsClient.receiveMessage(request).messages();
        for (Message message : messages) {
            try {
                OrderCreatedSqsPayload payload = objectMapper.readValue(
                        message.body(),
                        OrderCreatedSqsPayload.class
                );
                List<OrderItemEvent> items = payload.items().stream()
                        .map(i -> new OrderItemEvent(i.productId(), i.productName(), i.quantity(), i.price()))
                        .collect(Collectors.toList());
                OrderCreatedEvent event = new OrderCreatedEvent(
                        payload.orderId(),
                        payload.userId(),
                        items,
                        payload.totalAmount(),
                        payload.createdAt()
                );
                orderEventConsumer.handleOrderCreated(event);
                sqsClient.deleteMessage(
                        DeleteMessageRequest.builder()
                                .queueUrl(properties.getQueueUrl())
                                .receiptHandle(message.receiptHandle())
                                .build()
                );
            } catch (Exception ex) {
                log.error("Failed to process SQS order-created message id={}", message.messageId(), ex);
            }
        }
    }
}
