package it.pagopa.pn.notificationcostservice.utils;

import it.pagopa.pn.commons.log.dto.metrics.GeneralMetric;
import it.pagopa.pn.commons.log.dto.metrics.Metric;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricUtilsTest {
    @Test
    void generateGeneralMetricSuccess() {
        // Given
        MetricUtils.MetricName metricName = MetricUtils.MetricName.COST_UPDATE_PROPAGATION_DELAY;
        int metricValue = 100;
        // When
        GeneralMetric result = MetricUtils.generateGeneralMetric(metricName, metricValue, MetricUtils.MetricUnit.MILLISECONDS);

        // Then
        assertNotNull(result, "The generated metric should not be null");
        assertEquals("pn-notification-cost-service", result.getNamespace());
        assertNull(result.getDimensions());
        assertEquals(MetricUtils.MetricUnit.MILLISECONDS.getValue(), result.getUnit());

        // Verifico la lista delle metriche
        assertNotNull(result.getMetrics());
        assertEquals(1, result.getMetrics().size());

        Metric innerMetric = result.getMetrics().getFirst();
        assertEquals(metricName.getValue(), innerMetric.getName());
        assertEquals(metricValue, innerMetric.getValue());
    }

    @Test
    void metricNameEnumValues() {
        assertEquals("cost_update_propagation_delay",
                MetricUtils.MetricName.COST_UPDATE_PROPAGATION_DELAY.getValue());
    }
}