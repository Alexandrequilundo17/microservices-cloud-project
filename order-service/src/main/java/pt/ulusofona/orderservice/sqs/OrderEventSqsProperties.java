package pt.ulusofona.orderservice.sqs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloud.sqs.order-created-publisher")
public class OrderEventSqsProperties {

    private boolean enabled = false;
    private String queueUrl = "";
    private String region = "";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getQueueUrl() { return queueUrl; }
    public void setQueueUrl(String queueUrl) { this.queueUrl = queueUrl; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
}
