package it.pagopa.pn.notificationcostservice.utils;

import it.pagopa.pn.commons.log.dto.metrics.GeneralMetric;
import it.pagopa.pn.commons.log.dto.metrics.Metric;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

public class MetricUtils {
    private final static String METRIC_NAMESPACE = "pn-notification-cost-service";

    @Getter
    public enum MetricName {
        COST_UPDATE_PROPAGATION_DELAY("cost_update_propagation_delay");

        private final String value;

        MetricName(String value) {
            this.value = value;
        }
    }

    @Getter
    public enum MetricUnit {
        MILLISECONDS("Milliseconds");

        private final String value;

        MetricUnit(String value) {
            this.value = value;
        }
    }


    private MetricUtils() {
    }

    public static GeneralMetric generateGeneralMetric(MetricName metricName, int metricValue, MetricUnit metricUnit) {
        GeneralMetric generalMetric = new GeneralMetric();
        generalMetric.setNamespace(METRIC_NAMESPACE);
        generalMetric.setMetrics(List.of(new Metric(metricName.getValue(), metricValue)));
        generalMetric.setTimestamp(Instant.now().toEpochMilli());
        generalMetric.setUnit(metricUnit.value);
        return generalMetric;
    }

}
