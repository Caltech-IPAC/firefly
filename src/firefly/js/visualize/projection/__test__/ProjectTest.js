
import {describe, expect, it, assert} from 'chai';
import {Projection, makeProjection} from '../Projection.js';

import {Projection} from '../Projection.js';

describe('A test suite for projection.js', function () {
    /* run once before testing */
    var jsonObj;
    before(() => {
       //jsonObj = loadJsonObj();

        }
    );
 //TODO add test below
    it('should have a header and a coordinate parameters', function() {
        // Test implementation goes here
        var gwtProject = 'load gwt project here';

        var jsProject = makeProjection(jsonObj);
        assert.equal(jsProject, gwtProject);
    });

});