/**
 * Should test the function(s) in: 
 * 
 * firefly/util/validate.js
 * 
 * 'chai' asserts and expect to improve or cover more code
 * http://chaijs.com/api/assert/
 * See coverage here: coverage/['browsers']/lcov-report/util/Validate.js.html
 * See browsers defined in karma.conf.js: 
 * 	{
 * 		...
 * 		'browsers':'..','..',..
 * 	}
 */
var expect=require('chai').expect,
assert = require('chai').assert; // assertXXX equivalent in JUnit 

var validate = require('../Validate');

var retval= {
        valid : false,
        message : ''
};

describe("A test suite for util/Validate.js", function() {
	/* @BeforeClass JUnit or setUp */
	before(
		function() {
			retval = this.retval = {
			        valid : true,
			        message : ''
			};
		}
	);
	/* @AfterClass or tearDown */
	after(function() {
		
		
		}
	);
	
	/* @before equivalent in JUnit*/
	beforeEach(function() { 
			var retval= {
			        valid : true,
			        message : ''
			};
		}
	);
   /* @after equivalent in JUnit */
   afterEach(function() { 
	   
   		}
   );
   
   /* 
    * @Test equivalent in JUnit 
    * The test effectively to be run
    * 
    */
   it('should be valid', function() { 
	   expect(this.retval.valid).to.be.true; 
	   expect(this.retval.message).to.empty;
	   }
   );
   
   it('-1 < 0 should not be valid and message \'must be\' shoulb included in returned value', function() { 
	   var testedVal = validate.intRange(0,1,' test -1 ',-1);
	   expect(testedVal.valid).to.be.false;
	   assert.include(testedVal.message, 'must be'); // 'chai' asserts and expect to extends this test!
	   }
   );
});