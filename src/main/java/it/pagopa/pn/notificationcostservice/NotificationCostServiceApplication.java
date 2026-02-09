package it.pagopa.pn.notificationcostservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class NotificationCostServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationCostServiceApplication.class, args);
    }
}