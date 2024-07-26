// Jest config that is shared by all the apps

/* eslint-env node */

module.exports = {
    'verbose': true,
    'clearMocks': true,
    'collectCoverage': true,
    'coverageDirectory': '../../build/dist/reports/firefly',
    'coverageReporters': ['lcov'],
    'moduleFileExtensions': [
    'js',
    'jsx'
],
    'moduleDirectories': [
    'node_modules',
    'js'
],
    'setupFiles': [
    '<rootDir>/../../__jest__/InitTest.js'
],
    'moduleNameMapper': {
    '^.+\\?raw$': '<rootDir>/../../__jest__/fileMock.js',
        '\\.(jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$': '<rootDir>/../../__jest__/fileMock.js',
        '\\.(css|less)$': '<rootDir>/../../__jest__/styleMock.js',
        '^firefly/(.*)$': '<rootDir>/js/$1'
},
    'transform': {
    '^.+\\.jsx?$': '<rootDir>/../../__jest__/jest.transform.js'
},
    'globals': {
    '__PROPS__': {}
}
};