/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';

import {FormPanel} from './FormPanel.jsx';
import {FieldGroup} from '../ui/FieldGroup.jsx';
import {ValidationField} from '../ui/ValidationField.jsx';
import {ListBoxInputField} from '../ui/ListBoxInputField.jsx';
import Validate from '../util/Validate.js';
import {download} from '../util/WebUtil.js';
import {getRootURL} from '../util/BrowserUtil.js';
import {FileUpload} from '../ui/FileUpload.jsx';

import {dispatchHideDropDown} from '../core/LayoutCntlr.js';

import {dispatchTableSearch} from '../tables/TablesCntlr.js';
import {makeFileRequest} from '../tables/TableRequestUtil.js';


export const TestSearchPanel = (props) => {
    return (
        <div style={{padding: 10}}>
            <FormPanel
                width='640px' height='300px'
                groupKey='TBL_BY_URL_PANEL'
                onSubmit={(request) => onSearchSubmit(request)}
                onCancel={hideSearchPanel}>
                <p>
                    <input type='button' name='dowload' value='Download Sample File' onClick={doFileDownload} />
                </p>
                <FieldGroup groupKey='TBL_BY_URL_PANEL' validatorFunc={null} keepState={true}>
                    <ValidationField style={{width:500}}
                                     fieldKey='srcTable'
                                     initialState= {{
                                                                value: '/Users/loi/data/300k.tbl',
                                                                tooltip: 'path to a table file',
                                                                label : 'Source Table:',
                                                                labelWidth : 120
                                                             }}
                    />
                    <ListBoxInputField initialState={{
                                          tooltip: 'db engine to use',
                                          label : 'dbType:',
                                          labelWidth : 120
                                      }}
                                       options={[{value: 'hsql'},{value: 'h2'},{value: 'sqlite'}]}
                                       multiple={false}
                                       fieldKey='dbType'
                    />

                    <ValidationField style={{width:500}}
                                     fieldKey='filters'
                                     initialState= {{
                                                                placeholder: 'Apply this filter on the table above',
                                                                value: '',
                                                                label : 'Filters:',
                                                                labelWidth : 120
                                                             }}
                    />

                    <FileUpload
                        wrapperStyle = {{margin: '5px 0'}}
                        fieldKey = 'fileUpload'
                        initialState= {{
                                tooltip: 'Select a file to upload',
                                label: 'Upload File:'
                            }}
                    />
                    <ValidationField fieldKey='tbl_index'
                                     initialState= {{
                                            value: 0,
                                            size: 4,
                                            validator: Validate.intRange.bind(null, 0, 100000),
                                            label : 'Table Index:',
                                            labelWidth : 60
                                         }}
                    />
                </FieldGroup>
            </FormPanel>
        </div>
    );
};

function doFileDownload() {
    download(getRootURL() + 'samplehistdata.csv');
}

function hideSearchPanel() {
    dispatchHideDropDown();
}

function onSearchSubmit(request) {
    if (request.fileUpload) {
        const treq = makeFileRequest(null, request.fileUpload, null, {...request});
        dispatchTableSearch(treq);
    } else if (request.srcTable) {
        const treq = makeFileRequest(request.dbType + ':' + request.srcTable, request.srcTable, null, {filters: request.filters, META_INFO: {tblFileType: request.dbType}});
        dispatchTableSearch(treq);
    }
}

