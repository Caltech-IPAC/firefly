/*eslint-env node, mocha */

import {expect} from 'chai';
import {assert} from 'chai';

import {TableRequest} from '../TableRequest.js';
import {doFetchTable} from '../TableUtil.js';

describe('A test suite for tables/TablesCntlr.js', function () {
    var request;

    /* run once before testing */
    before(() => {
            request = TableRequest.newInstance({id:'IpacTableFromSource'});
        }
    );
    /* run once testing is done */
    after(() => {
        }
    );

    /* run before every test-case*/
    beforeEach(() => {
        }
    );
    /* run after every test-case*/
    afterEach(function () {

        }
    );

    /*
     * Test server-call sticky/CmdSrv.
     */
    it('should return TableModel', function () {
        //doFetchTable(request).then( (tableModel) => {
        //    expect( tableModel ).to.not.be.null;
        //    console.log(JSON.stringify(tableModel, null, 2));
        //
        //});
    });

});