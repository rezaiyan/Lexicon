// Increase Karma timeouts to prevent flaky CI disconnects
config.set({
    browserDisconnectTimeout: 10000,
    browserDisconnectTolerance: 3,
    browserNoActivityTimeout: 60000,
    captureTimeout: 60000,
    pingTimeout: 10000,
});
