/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */



// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// THIS PANEL IS TEMPORARY, ONLY TO TEST CATALOGS UNTIL WE FINISH THE REAL PANEL
// This panel will do search on 3 of the most common IRSA catalogs
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

import React, {PropTypes} from 'react';

import FormPanel from './FormPanel.jsx';
import {FieldGroup} from '../ui/FieldGroup.jsx';
import {ValidationField} from '../ui/ValidationField.jsx';
import {TargetPanel} from '../ui/TargetPanel.jsx';
import {InputGroup} from '../ui/InputGroup.jsx';
import {ListBoxInputField} from '../ui/ListBoxInputField.jsx';
import {ServerParams} from '../data/ServerParams.js';

import Validate from '../util/Validate.js';
import {dispatchHideDropDownUi} from '../core/LayoutCntlr.js';

import {TableRequest} from '../tables/TableRequest.js';
import {dispatchSetupTblTracking} from '../visualize/TableStatsCntlr.js';
import {dispatchTableSearch} from '../tables/TablesCntlr.js';
import * as TblUtil from '../tables/TableUtil.js';

const options= [
    {label: 'AllWISE Source Catalog', value:'wise_allwise_p3as_psd', proj:'WISE'},
    {label: '2MASS All-Sky Point Source Catalog (PSC)', value:'fp_psc', proj:'2MASS'},
    {label: 'IRAS Point Source Catalog v2.1 (PSC)', value:'iraspsc', proj: 'IRAS'}
];

/**
 *
 * @param props
 * @return {XML}
 * @constructor
 */
export const TestCatalog = (props) => {
    const {resultId} = props;
    return (
        <div style={{padding: 10}}>
            <FormPanel
                width='640px' height='300px'
                groupKey='TEST_CAT_PANEL'
                onSubmit={(request) => onSearchSubmit(request, resultId)}
                onCancel={hideSearchPanel}>
                <FieldGroup groupKey='TEST_CAT_PANEL' validatorFunc={null} keepState={true}>

                    <InputGroup labelWidth={110}>
                        <TargetPanel/>
                        <ListBoxInputField  initialState= {{
                                          tooltip: 'Select Catalog',
                                          label : 'Select Catalog:'
                                      }}
                                            options={options }
                                            multiple={false}
                                            fieldKey='catalog'
                        />


                        <ValidationField fieldKey='radius'
                                         initialState= {{
                                          fieldKey: 'radius',
                                          value: '300',
                                          validator: Validate.floatRange.bind(null, 0, 2000, 3,'field 3'),
                                          tooltip: 'radius',
                                          label : 'radius:',
                                          labelWidth : 100
                                      }} />

                    </InputGroup>

                </FieldGroup>
            </FormPanel>
        </div>
    );
};

TestCatalog.propTypes = {
    name: PropTypes.oneOf(['TestACatalog']),
    resultId: PropTypes.string
};

TestCatalog.defaultProps = {
    name: 'TestACatalog',
    resultId: TblUtil.uniqueTblUiGid()
};


function hideSearchPanel() {
    dispatchHideDropDownUi();
}




function onSearchSubmit(request, resultId) {
    const activeTblId = TblUtil.uniqueTblId();
    var tReq = TableRequest.newInstance({
        [ServerParams.USER_TARGET_WORLD_PT] : request[ServerParams.USER_TARGET_WORLD_PT],
        id:'GatorQuery',
        title : request.catalog,
        SearchMethod: 'Cone',
        catalog : request.catalog,
        RequestedDataSet :request.catalog,
        radius : request.radius,
        use : 'catalog_overlay',
        catalogProject : options.find( (op) => request.catalog===op.value).proj
    });
    const tbl_ui_id = TblUtil.uniqueTblUiId();
    dispatchSetupTblTracking(activeTblId);
    dispatchTableSearch(tReq, resultId, tbl_ui_id);
}

