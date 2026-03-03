const { expect } = require('chai');
const sinon = require('sinon');
const proxyquire = require('proxyquire');

describe('Migration', () => {
  let runMigration;
  let notificationRepoStub;
  let costMapperStub;
  let consoleErrorStub;

  beforeEach(() => {
    // Stub the dependencies
    notificationRepoStub = {
      getNotificationsToProcess: sinon.stub(),
      getTimelineByIun: sinon.stub(),
      saveDeliveryCosts: sinon.stub(),
    };

    costMapperStub = {
      mapToDeliveryCost: sinon.stub(),
      extractRecIndex: sinon.stub().callsFake(id => {
        if (typeof id !== 'string') return null;
        const match = id.match(/RECINDEX_(\d+)/);
        return match ? Number(match[1]) : null;
      }),
    };

    consoleErrorStub = sinon.stub(console, 'error');

    // Use proxyquire to inject the stubs
    runMigration = proxyquire('../app/Migration', {
      './NotificationRepository': notificationRepoStub,
      './CostMapper': costMapperStub,
    }).runMigration;
  });

  afterEach(() => {
    sinon.restore();
  });

  it('should process notifications and save costs successfully', async () => {
    const notifications = [{ iun: 'test-iun-1', recipients: [{}] }];
    const timeline = [{ timelineElementId: 'RECINDEX_0#SEND_DIGITAL' }];
    const costItem = { iun: 'test-iun-1', recIndex: 0 };

    notificationRepoStub.getNotificationsToProcess.resolves(notifications);
    notificationRepoStub.getTimelineByIun.resolves(timeline);
    costMapperStub.mapToDeliveryCost.returns(costItem);

    await runMigration();

    expect(notificationRepoStub.getNotificationsToProcess.calledOnce).to.be.true;
    expect(notificationRepoStub.getTimelineByIun.calledOnceWith('test-iun-1')).to.be.true;
    expect(costMapperStub.mapToDeliveryCost.calledOnce).to.be.true;
    expect(notificationRepoStub.saveDeliveryCosts.calledOnceWith([costItem])).to.be.true;
    expect(consoleErrorStub.notCalled).to.be.true;
  });

  it('should handle the case where no notifications are found', async () => {
    notificationRepoStub.getNotificationsToProcess.resolves([]);

    await runMigration();

    expect(notificationRepoStub.getNotificationsToProcess.calledOnce).to.be.true;
    expect(notificationRepoStub.getTimelineByIun.notCalled).to.be.true;
    expect(costMapperStub.mapToDeliveryCost.notCalled).to.be.true;
    expect(notificationRepoStub.saveDeliveryCosts.notCalled).to.be.true;
    expect(consoleErrorStub.notCalled).to.be.true;
  });

  it('should log an error if getNotificationsToProcess fails', async () => {
    const error = new Error('DB Error');
    notificationRepoStub.getNotificationsToProcess.rejects(error);

    await runMigration();

    expect(notificationRepoStub.getNotificationsToProcess.calledOnce).to.be.true;
    expect(consoleErrorStub.calledOnceWith('Errore critico durante la migrazione:', error)).to.be.true;
    expect(notificationRepoStub.getTimelineByIun.notCalled).to.be.true;
  });
});

