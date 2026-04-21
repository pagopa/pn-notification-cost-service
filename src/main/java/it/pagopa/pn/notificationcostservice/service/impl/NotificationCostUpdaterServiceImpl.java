package it.pagopa.pn.notificationcostservice.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.commons.log.dto.metrics.GeneralMetric;
import it.pagopa.pn.notificationcostservice.middleware.dao.NotificationDeliveryCostDao;
import it.pagopa.pn.notificationcostservice.middleware.dao.dynamo.entity.notificationdeliverycost.NotificationDeliveryCostEntity;
import it.pagopa.pn.notificationcostservice.model.cost.CostUpdatePhaseInt;
import it.pagopa.pn.notificationcostservice.model.cost.NotificationCostUpdate;
import it.pagopa.pn.notificationcostservice.model.notificationdeliverycost.NotificationDeliveryCost;
import it.pagopa.pn.notificationcostservice.service.NotificationCostUpdaterService;
import it.pagopa.pn.notificationcostservice.service.mapper.NotificationCostUpdaterMapper;
import it.pagopa.pn.notificationcostservice.utils.MetricUtils;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.pagopa.pn.notificationcostservice.exception.PnNotificationCostServiceExceptionCodes.ERROR_CODE_NOTIFICATIONCOSTSERVICE_INTERNAL_SERVER_ERROR;

@lombok.CustomLog
@AllArgsConstructor
@Service
public class NotificationCostUpdaterServiceImpl implements NotificationCostUpdaterService {

    private final NotificationDeliveryCostDao notificationDeliveryCostDao;
    private final NotificationCostUpdaterMapper notificationCostUpdaterMapper;

    @Override
    public Mono<Void> updateBaseCost(NotificationDeliveryCost notificationDeliveryCost) {
        return Mono.justOrEmpty(notificationDeliveryCost)
                .switchIfEmpty(Mono.error(new PnInternalException("NotificationDeliveryCost cannot be null",
                        ERROR_CODE_NOTIFICATIONCOSTSERVICE_INTERNAL_SERVER_ERROR)))
                .map(notificationCostUpdaterMapper::toEntityForBaseCostUpdate)
                .flatMap(entityToUpdate -> {
                    PnAuditLogEvent auditEntity = buildLogAudit(CostUpdatePhaseInt.VALIDATION, entityToUpdate);
                    auditEntity.log();
                    return notificationDeliveryCostDao.updateBaseCostIfNotExistsOrMatch(entityToUpdate)
                            .doOnNext(entity -> auditEntity.generateSuccess("Updated entity successfully, entity={}", entity))
                            .doOnError(error -> auditEntity.generateFailure("Entity update failed, error={}", error));
                })
                .then();
    }

    @Override
    public Mono<Void> updateCostByPhase(NotificationCostUpdate notificationCostUpdate) {
        return Mono.justOrEmpty(notificationCostUpdate)
                .switchIfEmpty(Mono.error(new PnInternalException("Missing required data for updateCostByPhase",
                        ERROR_CODE_NOTIFICATIONCOSTSERVICE_INTERNAL_SERVER_ERROR)))
                .map(notificationCostUpdaterMapper::mapNotificationCostUpdater)
                .flatMap(entityToUpdate -> {
                    PnAuditLogEvent auditEntity = buildLogAudit(notificationCostUpdate.getCostUpdatePhase(), entityToUpdate);
                    auditEntity.log();
                    return executeUpdate(entityToUpdate, auditEntity, notificationCostUpdate);
                })
                .then();
    }

    private PnAuditLogEvent buildLogAudit(CostUpdatePhaseInt updateCostPhase, NotificationDeliveryCostEntity entity) {
        return new PnAuditLogBuilder()
                .before(PnAuditLogEventType.AUD_NT_UPDATE_COST, "Notification delivery cost to update: phase={}, iun={}, recIndex={}, entity={}",
                        updateCostPhase,
                        entity.getIun(),
                        entity.getRecIndex(),
                        entity)
                .build();
    }

    private Mono<Void> executeUpdate(NotificationDeliveryCostEntity entityToUpdate, PnAuditLogEvent audit, NotificationCostUpdate notificationCostUpdate) {
        return notificationDeliveryCostDao.updateNotificationDeliveryCostNotNull(entityToUpdate)
                .doOnNext(entity -> audit.generateSuccess("Updated entity successfully, entity={}", entity).log())
                .doOnNext(entity -> generateRecordUpdateLatencyMetric(entity, notificationCostUpdate))
                .doOnError(error -> audit.generateFailure("Entity update failed, error={}", error))
                .then();
    }

    private void generateRecordUpdateLatencyMetric(NotificationDeliveryCostEntity entity, NotificationCostUpdate notificationCostUpdate) {
        long ageInMillis = entity.getLastUpdate().toEpochMilli() - notificationCostUpdate.getElementTimestamp().toEpochMilli();

        GeneralMetric metric = MetricUtils.generateGeneralMetric(
                MetricUtils.MetricName.COST_UPDATE_PROPAGATION_DELAY,
                (int) ageInMillis,
                MetricUtils.MetricUnit.MILLISECONDS
        );

        String logMsg = String.format("Generated metric for cost update propagation delay: iun=%s, recIndex=%d, ageInMillis=%d",
                entity.getIun(), entity.getRecIndex(), ageInMillis);
        log.logMetric(List.of(metric), logMsg);
    }
}
