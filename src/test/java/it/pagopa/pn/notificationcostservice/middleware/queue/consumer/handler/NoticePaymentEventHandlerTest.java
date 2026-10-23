package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.CommunicationType;
import it.pagopa.pn.api.dto.events.PnNoticePaymentEvent;
import it.pagopa.pn.api.dto.events.PnNoticePaymentPaidEvent;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NoticePaymentEventHandlerTest {

    private final NoticePaymentEventHandler handler = new NoticePaymentEventHandler(null, null);

    @Test
    void updatePaymentInfoDataShouldEnrichCopyAndPreserveOriginalFields() {
        PnNoticePaymentPaidEvent.Payload payload = PnNoticePaymentPaidEvent.Payload.builder()
                .modelType("PAYMENTS_TO_VERIFY")
                .iuv("iuv")
                .iur("iur")
                .noticeCode("notice-code")
                .creditorTaxId("creditor-tax-id")
                .creditorCompanyName("creditor-company")
                .paymentDateTime(Instant.parse("2026-01-01T10:00:00Z"))
                .dueDate(Instant.parse("2026-01-02T10:00:00Z"))
                .totalNotice(1)
                .paymentSourceChannel("channel")
                .amount(100)
                .paymentMethod("method")
                .fee("10")
                .paymentId("payment-id")
                .pagopaPersistedTimestamp(Instant.parse("2026-01-01T10:01:00Z"))
                .pagopaPublishedTimestamp(Instant.parse("2026-01-01T10:02:00Z"))
                .sendIngestionTimestamp(Instant.parse("2026-01-01T10:03:00Z"))
                .kafkaPartition("0")
                .communicationType(CommunicationType.LEGAL)
                .iun("previous-iun")
                .recIndex(0)
                .build();
        PaymentInfo info = PaymentInfo.builder().iun("enriched-iun").recIndex(2).build();

        Mono<PnNoticePaymentPaidEvent.Payload> result =
                ReflectionTestUtils.invokeMethod(handler, "updatePaymentInfoData", payload, info);

        StepVerifier.create(result)
                .assertNext(enriched -> {
                    assertThat(enriched).isNotSameAs(payload);
                    assertThat(enriched).usingRecursiveComparison()
                            .ignoringFields("communicationType", "iun", "recIndex")
                            .isEqualTo(payload);
                    assertThat(enriched.getCommunicationType()).isEqualTo(CommunicationType.LEGAL);
                    assertThat(enriched.getIun()).isEqualTo(info.getIun());
                    assertThat(enriched.getRecIndex()).isEqualTo(info.getRecIndex());
                    assertThat(payload.getCommunicationType()).isEqualTo(CommunicationType.LEGAL);
                    assertThat(payload.getIun()).isEqualTo("previous-iun");
                    assertThat(payload.getRecIndex()).isZero();
                })
                .verifyComplete();
    }

    @Test
    void updatePaymentInfoDataShouldPreserveNullOptionalFields() {
        PnNoticePaymentEvent.Payload payload = PnNoticePaymentEvent.Payload.builder().build();
        PaymentInfo info = PaymentInfo.builder().iun("iun").recIndex(0).build();

        Mono<PnNoticePaymentPaidEvent.Payload> result =
                ReflectionTestUtils.invokeMethod(handler, "updatePaymentInfoData", payload, info);

        StepVerifier.create(result)
                .assertNext(enriched -> {
                    assertThat(enriched).usingRecursiveComparison()
                            .ignoringFields("communicationType", "iun", "recIndex")
                            .isEqualTo(payload);
                    assertThat(enriched.getCommunicationType()).isEqualTo(CommunicationType.LEGAL);
                    assertThat(enriched.getIun()).isEqualTo("iun");
                    assertThat(enriched.getRecIndex()).isZero();
                })
                .verifyComplete();
    }
}
