package it.pagopa.pn.notificationcostservice.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.PnNoticePaymentEvent;
import it.pagopa.pn.api.dto.events.PnNoticePaymentPaidEvent;
import it.pagopa.pn.notificationcostservice.middleware.dao.PaymentInfoDao;
import it.pagopa.pn.notificationcostservice.middleware.eventbus.EventBridgeProducer;
import it.pagopa.pn.notificationcostservice.model.CommunicationType;
import it.pagopa.pn.notificationcostservice.model.paymentinfo.PaymentInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class NoticePaymentEventHandlerTest {

    private static final String CREDITOR_TAX_ID = "creditor-tax-id";
    private static final String NOTICE_CODE = "notice-code";
    private static final String EXPECTED_PK = CREDITOR_TAX_ID + "##" + NOTICE_CODE;

    private PaymentInfoDao paymentInfoDao;
    private EventBridgeProducer<PnNoticePaymentPaidEvent> producer;
    private NoticePaymentEventHandler handler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        paymentInfoDao = mock(PaymentInfoDao.class);
        producer = mock(EventBridgeProducer.class);
        handler = new NoticePaymentEventHandler(paymentInfoDao, producer);
    }

    @Test
    void handleNoticePaymentEvent_paymentInfoFound_sendsEnrichedEvent() {
        PnNoticePaymentEvent.Payload input = buildInputPayload();
        PaymentInfo info = buildPaymentInfo();

        when(paymentInfoDao.getPaymentInfoByIuv(EXPECTED_PK)).thenReturn(Mono.just(info));
        when(producer.sendEvent(any(PnNoticePaymentPaidEvent.class))).thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(input))
                .verifyComplete();

        verify(paymentInfoDao).getPaymentInfoByIuv(EXPECTED_PK);

        ArgumentCaptor<PnNoticePaymentPaidEvent> captor =
                ArgumentCaptor.forClass(PnNoticePaymentPaidEvent.class);
        verify(producer).sendEvent(captor.capture());

        PnNoticePaymentPaidEvent.Payload sent = captor.getValue().getPayload();

        // Dati provenienti dall'evento in input
        assertThat(sent.getModelType()).isEqualTo(input.getModelType());
        assertThat(sent.getIuv()).isEqualTo(input.getIuv());
        assertThat(sent.getIur()).isEqualTo(input.getIur());
        assertThat(sent.getNoticeCode()).isEqualTo(input.getNoticeCode());
        assertThat(sent.getCreditorTaxId()).isEqualTo(input.getCreditorTaxId());
        assertThat(sent.getCreditorCompanyName()).isEqualTo(input.getCreditorCompanyName());
        assertThat(sent.getPaymentDateTime()).isEqualTo(input.getPaymentDateTime());
        assertThat(sent.getDueDate()).isEqualTo(input.getDueDate());
        assertThat(sent.getTotalNotice()).isEqualTo(input.getTotalNotice());
        assertThat(sent.getPaymentSourceChannel()).isEqualTo(input.getPaymentSourceChannel());
        assertThat(sent.getAmount()).isEqualTo(input.getAmount());
        assertThat(sent.getPaymentMethod()).isEqualTo(input.getPaymentMethod());
        assertThat(sent.getFee()).isEqualTo(input.getFee());
        assertThat(sent.getPaymentId()).isEqualTo(input.getPaymentId());
        assertThat(sent.getPagopaPersistedTimestamp()).isEqualTo(input.getPagopaPersistedTimestamp());
        assertThat(sent.getPagopaPublishedTimestamp()).isEqualTo(input.getPagopaPublishedTimestamp());
        assertThat(sent.getSendIngestionTimestamp()).isEqualTo(input.getSendIngestionTimestamp());
        assertThat(sent.getKafkaPartition()).isEqualTo(input.getKafkaPartition());

        // Dati recuperati da PaymentInfo
        assertThat(sent.getCommunicationType()).isEqualTo(info.getCommunicationType().name());
        assertThat(sent.getIun()).isEqualTo(info.getIun());
        assertThat(sent.getRecIndex()).isEqualTo(info.getRecIndex());
    }

    @Test
    void handleNoticePaymentEvent_paymentInfoNotFound_doesNotSendEvent() {
        PnNoticePaymentEvent.Payload input = buildInputPayload();

        when(paymentInfoDao.getPaymentInfoByIuv(EXPECTED_PK)).thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(input))
                .verifyComplete();

        verify(paymentInfoDao).getPaymentInfoByIuv(EXPECTED_PK);
        verify(producer, never()).sendEvent(any(PnNoticePaymentPaidEvent.class));
        verifyNoInteractions(producer);
    }

    @Test
    void handleNoticePaymentEvent_daoError_propagatesErrorAndDoesNotSendEvent() {
        PnNoticePaymentEvent.Payload input = buildInputPayload();
        RuntimeException error = new RuntimeException("dao error");

        when(paymentInfoDao.getPaymentInfoByIuv(EXPECTED_PK)).thenReturn(Mono.error(error));

        StepVerifier.create(handler.handle(input))
                .expectErrorMatches(ex -> ex == error)
                .verify();

        verifyNoInteractions(producer);
    }

    @Test
    void handleNoticePaymentEvent_producerError_propagatesError() {
        PnNoticePaymentEvent.Payload input = buildInputPayload();
        PaymentInfo info = buildPaymentInfo();
        RuntimeException error = new RuntimeException("producer error");

        when(paymentInfoDao.getPaymentInfoByIuv(EXPECTED_PK)).thenReturn(Mono.just(info));
        when(producer.sendEvent(any(PnNoticePaymentPaidEvent.class))).thenReturn(Mono.error(error));

        StepVerifier.create(handler.handle(input))
                .expectErrorMatches(ex -> ex == error)
                .verify();

        verify(producer).sendEvent(any(PnNoticePaymentPaidEvent.class));
    }

    // ---------- helper ----------

    private PnNoticePaymentEvent.Payload buildInputPayload() {
        return PnNoticePaymentEvent.Payload.builder()
                .modelType("PAYMENTS_TO_VERIFY")
                .iuv("iuv")
                .iur("iur")
                .noticeCode(NOTICE_CODE)
                .creditorTaxId(CREDITOR_TAX_ID)
                .creditorCompanyName("creditor-company")
                .paymentDateTime(Instant.parse("2026-01-01T10:00:00Z"))
                .dueDate(LocalDate.parse("2026-01-02"))
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
                .build();
    }

    private PaymentInfo buildPaymentInfo() {
        PaymentInfo info = new PaymentInfo();
        info.setIuv(CREDITOR_TAX_ID + "##" + NOTICE_CODE);
        info.setIun("previous-iun");
        info.setRecIndex(0);
        info.setCommunicationType(CommunicationType.LEGAL);
        return info;
    }
}