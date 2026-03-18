package it.pagopa.pn.notificationcostservice.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConfigurationProperties( prefix = "pn.notification-cost-service")
@Data
@Import({SharedAutoConfiguration.class})
@Slf4j
public class PnNotificationCostServiceConfigs {

    private NotificationDeliveryCostTable notificationDeliveryCostTable;
    private PaymentInfoTable paymentInfoTable;
    private Topics topics;

    @Data
    public static class NotificationDeliveryCostTable {
        private String tableName;
    }
    @Data
    public static class PaymentInfoTable {
        private String tableName;
    }

    @Data
    public static class Topics {
        private String pnNotificationCostToUpdate;
    }

    @PostConstruct
    public void init() {
        log.info("PnNotificationCostServiceConfigs={}", this);
    }
}
