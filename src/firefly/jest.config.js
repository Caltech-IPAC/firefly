/* eslint-env node */

const baseConfig = require('../../__jest__/jest.base.config');

// These two suites read a firefly_test_data checkout at module load, not inside a test,
// so a missing checkout fails the whole file instead of one test.
// Set FIREFLY_SKIP_TESTDATA=true to skip them where the data is unavailable.
const needsTestData = ['/__tests__/Projection-test\\.js$', '/__tests__/Wavelength-test\\.js$'];

module.exports = {
    ...baseConfig,
    // add overrides here (if any)

    // jest replaces its default rather than merging, so '/node_modules/' has to be repeated.
    testPathIgnorePatterns: [
        '/node_modules/',
        ...(process.env.FIREFLY_SKIP_TESTDATA === 'true' ? needsTestData : []),
    ],

    // jest defaults to one worker per core. In a container that sees many cores
    // but has a small memory ceiling, that many jsdom workers get OOM-killed.
    // Set JEST_MAX_WORKERS to cap it; compose.yml does for the test-js service.
    ...(process.env.JEST_MAX_WORKERS ? {maxWorkers: Number(process.env.JEST_MAX_WORKERS)} : {}),
};
