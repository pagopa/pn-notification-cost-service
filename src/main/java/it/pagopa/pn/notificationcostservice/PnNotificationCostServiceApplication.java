package it.pagopa.pn.notificationcostservice;

import it.pagopa.pn.commons.configs.listeners.TaskIdApplicationListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PnNotificationCostServiceApplication {
    public static void main(String[] args) {
        buildSpringApplicationWithListener().run(args);
    }

    static SpringApplication buildSpringApplicationWithListener() {
        SpringApplication app = new SpringApplication(PnNotificationCostServiceApplication.class);
        app.addListeners(new TaskIdApplicationListener());
        return app;
    }}