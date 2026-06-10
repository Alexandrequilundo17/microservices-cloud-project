package pt.ulusofona.orderservice.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pt.ulusofona.orderservice.model.Order;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class OrderEventSqsPublisher {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String queueUrl;

    public void publishOrderCreated(Order order) throws Exception {
        OrderCreatedSqsEvent event = OrderCreatedSqsEvent.fromOrder(order);
        String body = objectMapper.writeValueAsString(event);

        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(body)
                .messageAttributes(Map.of(
                        "eventType",
                        MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(event.eventType())
                                .build()
                ))
                .build();

        sqsClient.sendMessage(request);
        log.debug("Published OrderCreatedSqsEvent for orderId={}", event.orderId());
    }
}
