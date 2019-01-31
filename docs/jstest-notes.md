JavaScript Unit Testing
----------------------- 

###### Overview of JavaScript testing requirements and its tools:

- Testing structure (Mocha, Jasmine, Jest, Cucumber)
- Assertions functions (Chai, Jasmine, Jest, Unexpected)
    - https://jestjs.io/docs/en/using-matchers
- Provide mocks, spies, and stubs (Sinon, Jasmine, enzyme, Jest, testdouble)
- Generate, display, and watch test results (Mocha, Jasmine, Jest, Karma)
- Generate code coverage reports (Istanbul, Jest, Blanket)
- Snapshots of component and data (Jest, Ava)
- Provide a browser or browser-like environment(Protractor, Nightwatch, Phantom, Casper)
    - Jest can do this too, if you add jest-puppeteer


###### What is Jest?

- https://jestjs.io/
- Developed, used and supported by Facebook
- Zero configuration, claimed
- Run tests in parallel to speed up the process
- Support all of the above requirements


###### How to run tests:
    gradle :firefly:jsTest       # run all test suites


###### Running test with additional options:  
    cd into src/firefly  
    yarn run test-unit --verbose        # run all, displaying more details  
    yarn run test-unit -t pattern	    # run all tests matching the given pattern  


###### How to write tests using Jest:

Sample tests: /firefly/src/firefly/js/tables/\__tests__/TableUtil-test.js

- Test files under \__tests__ directories are automatically picked up 
- The structure.  describe -> test
    - grouping
    - scope
    - can be nested
- A simple unit test
- A test requiring ‘mocking’ one function from a module 


###### How to debug tests using Chrome DevTool:

    gradle :firefly:jsTestDebug  # run all test in debug mode
    yarn test-debug -t pattern   # run all tests matching the given pattern in debug mode

- Using Chrome, goto url `chrome://inspect/`    

###### Using IntelliJ..  may require the latest version.

- right-click on a test then select `run` or `debug` to run that one test
- right-click on a group (describe block) then select `run` or `debug` to run that group of tests
- right-click on open space then select `run` or `debug` to run the whole test suite or module

**AWESOME!!!**




Firefly Integration Tests
-------------------------

http://localhost:8080/firefly/test/
- landing page
- listing of test pages
- HowTos


A simple homegrown testing framework intended to run on a fully functional deployment.

###### Objectives:

- easy to write
- tests need to be sand-boxed to increase reliability
- results that’s easy to understand
- framework can expands as needed


###### Purposes:

- code base still works with external services (IBE, gator, TAP, etc)
    - may not be due changes on our end, but changes to the services.
- supported API (especially less common ones) still works as expected 
- anything else that requires a full functioning deployment to test
