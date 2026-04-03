class PartialBatchProcessingError extends Error {
  constructor(message, options = {}) {
    super(message);
    this.name = "PartialBatchProcessingError";
    this.processedItems = options.processedItems ?? [];
    this.failedEvents = options.failedEvents ?? [];
    this.shouldStopProcessing = options.shouldStopProcessing ?? true;

    if (options.cause) {
      this.cause = options.cause;
    }
  }
}

module.exports = PartialBatchProcessingError;

