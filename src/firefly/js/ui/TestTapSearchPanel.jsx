/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {FormPanel} from './FormPanel.jsx';
import {FieldGroup} from '../ui/FieldGroup.jsx';
import {ValidationField} from '../ui/ValidationField.jsx';
import {ListBoxInputField} from '../ui/ListBoxInputField.jsx';

import {dispatchHideDropDown} from '../core/LayoutCntlr.js';

import {dispatchTableSearch} from '../tables/TablesCntlr.js';
import {makeTblRequest} from '../tables/TableRequestUtil.js';
import {updateSet} from '../util/WebUtil.js';
import {VALUE_CHANGE} from '../fieldGroup/FieldGroupCntlr.js';
import {get, pick} from 'lodash';


export const TestTapSearchPanel = (props) => {
    return (
        <div style={{padding: 10}}>
            <FormPanel
                width='740px' height='400px'
                groupKey='TBL_QUERY_PANEL'
                onSubmit={(request) => onSearchSubmit(request)}
                onCancel={hideSearchPanel}>
                <FieldGroup groupKey='TBL_QUERY_PANEL' validatorFunc={null} keepState={true} reducerFunc={reducer}>
                    <ListBoxInputField fieldKey='serviceUrl' options={tapServiceOptions} />

                    <ValidationField style={{width:600}}
                                     fieldKey='adqlQuery'
                    />
                </FieldGroup>
            </FormPanel>
        </div>
    );
};


function hideSearchPanel() {
    dispatchHideDropDown();
}

function onSearchSubmit(request) {
    if (request.adqlQuery) {
        const params = {serviceUrl: request.serviceUrl, QUERY: request.adqlQuery};
        const options = {};
        const found = request.serviceUrl.match(/.*\:\/\/(.*)\/.*/i);
        const treq = makeTblRequest('AsyncTapQuery', found && found[1], params, options);
        dispatchTableSearch(treq, {backgroundable: true});
    }
}

const tapServices = [
    {
        label: 'IRSA https://irsa.ipac.caltech.edu/TAP',
        value: 'https://irsa.ipac.caltech.edu/TAP',
        query: 'SELECT * FROM fp_psc WHERE CONTAINS(POINT(\'ICRS\',ra,dec),CIRCLE(\'ICRS\',210.80225,54.34894,1.0))=1'
    },
    {
        label: 'GAIA http://gea.esac.esa.int/tap-server/tap',
        value: 'http://gea.esac.esa.int/tap-server/tap',
        query: 'SELECT TOP 5000 * FROM gaiadr2.gaia_source'
    },
    {
        label: 'CASDA http://atoavo.atnf.csiro.au/tap',
        value: 'http://atoavo.atnf.csiro.au/tap',
        query: 'SELECT * FROM ivoa.obscore WHERE CONTAINS(POINT(\'ICRS\',s_ra,s_dec),CIRCLE(\'ICRS\',32.69,-51.01,1.0))=1'
    },
    {
        label: 'MAST http://vao.stsci.edu/CAOMTAP/TapService.aspx',
        value: 'http://atoavo.atnf.csiro.au/tap',
        query: 'SELECT * FROM ivoa.obscore WHERE CONTAINS(POINT(\'ICRS\',s_ra,s_dec),CIRCLE(\'ICRS\',32.69,-51.01,1.0))=1'
    },
    {
        label: 'LSST TEST http://tap.lsst.rocks/tap',
        value: 'http://tap.lsst.rocks/tap',
        query: 'SELECT TOP 5000 * FROM gaiadr2.gaia_source'
    }
];

const tapServiceOptions = tapServices.map((e)=>pick(e, ['label', 'value']));

function reducer(inFields, action) {
    if (!inFields) {
        const initialIdx = 0;
        return {
            serviceUrl: {
                fieldKey: 'serviceUrl',
                tooltip: 'tap service url',
                label: 'TAP service URL:',
                labelWidth: 120,
                options: tapServiceOptions,
                multiple: false,
                value: tapServices[initialIdx].value
            },
            adqlQuery: {
                fieldKey: 'adqlQuery',
                tooltip: 'ADQL to submit to the selected TAP service',
                label: 'ADQL Query:',
                labelWidth: 120,
                value: tapServices[initialIdx].query
            }
        };
    } else {
        if (get(action, 'type') === VALUE_CHANGE) {
            const {fieldKey, value} = action.payload;
            if (fieldKey === 'serviceUrl') {
                // update the sample ADQL query
                const idx = tapServices.findIndex((e) => e.value === value);
                if (idx >= 0) {
                    inFields = updateSet(inFields, 'adqlQuery.value', tapServices[idx].query);
                }
            }
        }
        return inFields;
    }
}

