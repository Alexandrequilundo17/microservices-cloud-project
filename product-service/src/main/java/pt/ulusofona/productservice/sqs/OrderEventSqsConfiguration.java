package pt.ulusofona.productservice.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.ulusofona.productservice.service.OrderEventConsumer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

@Configuration
@ConditionalOnProperty(prefix = "cloud.sqs.order-created-consumer", name = "enabled", havingValue = "true")
public class OrderEventSqsConfiguration {

    @Bean(destroyMethod = "close")
    public SqsClient orderEventSqsClient(OrderEventSqsProperties properties) {
        if (properties.getQueueUrl() == null || properties.getQueueUrl().isBlank()) {
            throw new IllegalStateException(
                    "cloud.sqs.order-created-consumer.queue-url must be set when enabled=true"
            );
        }
        SqsClientBuilder builder = SqsClient.builder();
        if (properties.getRegion() != null && !properties.getRegion().isBlank()) {
            builder = builder.region(Region.of(properties.getRegion()));
        }
        return builder.build();
    }

    @Bean
    public OrderEventSqsPollingConsumer orderEventSqsPollingConsumer(
            SqsClient orderEventSqsClient,
            ObjectMapper objectMapper,
            OrderEventSqsProperties properties,
            OrderEventConsumer orderEventConsumer
    ) {
        return new OrderEventSqsPollingConsumer(orderEventSqsClient, objectMapper, properties, orderEventConsumer);
    }
}
