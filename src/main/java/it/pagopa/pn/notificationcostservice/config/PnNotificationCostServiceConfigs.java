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

    private NotificationDeliveryCostDao notificationDeliveryCostDao;

    @Data
    public static class NotificationDeliveryCostDao {
        private String tableName;
    }
    @PostConstruct
    public void init() {
        log.info("PnNotificationCostServiceConfigs={}", this);
    }
}
