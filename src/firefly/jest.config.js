/* eslint-env node */

const baseConfig = require('../../__jest__/jest.base.config');

// Projection-test and Wavelength-test scan a sibling firefly_test_data checkout while the
// module is still being declared, so a missing checkout fails the whole suite instead of
// skipping it.  Set FIREFLY_SKIP_TESTDATA where that data is unavailable, e.g. CI.
const needsTestData = ['/__tests__/Projection-test\\.js$', '/__tests__/Wavelength-test\\.js$'];

module.exports = {
    ...baseConfig,
    //add overrides here (if any)

    // jest replaces its default rather than merging, so '/node_modules/' has to be repeated.
    testPathIgnorePatterns: [
        '/node_modules/',
        ...(process.env.FIREFLY_SKIP_TESTDATA ? needsTestData : []),
    ],
};
