/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';

import FormPanel from './FormPanel.jsx';
import {FieldGroup} from '../ui/FieldGroup.jsx';
import {ValidationField} from '../ui/ValidationField.jsx';
import Validate from '../util/Validate.js';
import {download} from '../util/WebUtil.js';
import {getRootURL} from '../util/BrowserUtil.js';

import {dispatchHideDropDownUi} from '../core/LayoutCntlr.js';

import {TableRequest} from '../tables/TableRequest.js';
import * as TableStatsCntlr from '../visualize/TableStatsCntlr.js';
import {dispatchTableSearch} from '../tables/TablesCntlr.js';
import * as TblUtil from '../tables/TableUtil.js';


export const SearchPanel = (props) => {
    const {resultId} = props;
    return (
        <div style={{padding: 10}}>
            <FormPanel
                width='640px' height='300px'
                groupKey='TBL_BY_URL_PANEL'
                onSubmit={(request) => onSearchSubmit(request, resultId)}
                onCancel={hideSearchPanel}>
                <p>
                    <input type='button' name='dowload' value='Download Sample File' onClick={doFileDownload} />
                </p>
                <FieldGroup groupKey='TBL_BY_URL_PANEL' validatorFunc={null} keepState={true}>
                    <ValidationField style={{width:500}}
                                     fieldKey='srcTable'
                                     groupKey='TBL_BY_URL_PANEL'
                                     initialState= {{ 
                                                                value: 'http://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl',
                                                                validator: Validate.validateUrl.bind(null, 'Source Table'),
                                                                tooltip: 'The URL to the source table',
                                                                label : 'Source Table:',
                                                                labelWidth : 120 
                                                             }}
                    />
                    <ValidationField style={{width:500}}
                                     fieldKey='filters'
                                     groupKey='TBL_BY_URL_PANEL'
                                     initialState= {{ 
                                                                value: '',
                                                                label : 'Filters:',
                                                                labelWidth : 120 
                                                             }}
                    />

                </FieldGroup>
            </FormPanel>
        </div>
    );
};

SearchPanel.propTypes = {
    name: PropTypes.oneOf(['AnyDataSetSearch']),
    resultId: PropTypes.string
};

SearchPanel.defaultProps = {
    name: 'AnyDataSetSearch',
    resultId: TblUtil.uniqueTblUiGid()
};


function doFileDownload() {
    download(getRootURL() + 'samplehistdata.csv');
}

function hideSearchPanel() {
    dispatchHideDropDownUi();
}

function onSearchSubmit(request, resultId) {
    const activeTblId = TblUtil.uniqueTblId();
    if (request.srcTable) {
        var treq = TableRequest.newInstance({
            id:'IpacTableFromSource',
            source: request.srcTable,
            tbl_id:  activeTblId,
            filters: request.filters
        });
        const tbl_ui_id = TblUtil.uniqueTblUiId();
        TableStatsCntlr.dispatchSetupTblTracking(activeTblId);
        dispatchTableSearch(treq, resultId, tbl_ui_id);
    }
}

