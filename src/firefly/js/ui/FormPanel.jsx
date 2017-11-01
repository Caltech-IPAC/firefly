/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import CompleteButton from './CompleteButton.jsx';
import * as TablesCntlr from '../tables/TablesCntlr.js';
import {HelpIcon} from './HelpIcon.jsx';
import {dispatchHideDropDown} from '../core/LayoutCntlr.js';
import {onTableLoaded} from '../tables/TableUtil.js';
import {makeTblRequest} from '../tables/TableRequestUtil.js';
import {getDefaultXYPlotOptions, DT_XYCOLS} from '../charts/dataTypes/XYColsCDT.js';
import {SCATTER} from '../charts/ChartUtil.js';
import * as ChartsCntlr from '../charts/ChartsCntlr.js';
import {isNil, get} from 'lodash';

function handleFailfure() {

}

function createSuccessHandler(action, params={}, title, onSubmit) {
    return (request={}) => {
        request = Object.assign({}, params, request);
        const reqTitle = title && (typeof title === 'function') ? title(request) : title;
        request = makeTblRequest(request.id, reqTitle || request.title, request, params);

        if (action) {
            if (typeof action === 'function') {
                action(request);
            } else {
                switch (action) {
                    case TablesCntlr.TABLE_SEARCH  :
                        TablesCntlr.dispatchTableSearch(request);
                        break;
                    case ChartsCntlr.CHART_ADD  :
                        handleChartAdd(request);
                        break;
                }
            }
        }

        let submitResult;
        if (onSubmit) {
            submitResult = onSubmit(request);
        }

        // submitResult is not defined, or it is true, or it is false and hideOnInValid is true
        if (isNil(submitResult) || submitResult || (!submitResult&&get(params, 'hideOnInvalid', true))) {
            dispatchHideDropDown();
        }
    };
}

export const FormPanel = function (props) {
    var {children, onSuccess, onSubmit, onCancel, onError, groupKey, action, params, title,
        submitText='Search', help_id, changeMasking, includeUnmounted=false} = props;

    const style = {
        backgroundColor: 'white',
        border: '1px solid rgba(0,0,0,0.2)',
        padding: 5,
        marginBottom: 5,
        boxSizing: 'border-box',
        flexGrow: 1
    };
    onCancel = onCancel || dispatchHideDropDown;

    return (
        <div style={{height: '100%', display:'flex', flexDirection: 'column', boxSizing: 'border-box'}}>
            <div style={style}>
                {children}
            </div>
            <div style={{flexGrow: 0, display: 'inline-flex', justifyContent: 'space-between', width: '100%', alignItems: 'flex-end', padding:'2px 0px 3px'}}>
                <div>
                    <CompleteButton style={{display: 'inline-block', marginRight: 10}}
                                    includeUnmounted={includeUnmounted}
                                    groupKey={groupKey}
                                    onSuccess={onSuccess||createSuccessHandler(action, params, title, onSubmit)}
                                    onFail={onError || handleFailfure}
                                    text = {submitText} changeMasking={changeMasking}
                    />
                    <button style={{display: 'inline-block'}} type='button' className='button std' onClick={onCancel}>Cancel</button>

                </div>
                <div>
                    {help_id && <HelpIcon helpId={help_id} />}
                </div>
            </div>
        </div>
    );
};


// Use onSubmit, action, param, and title props when the callback expects a table request
// Use onSuccess for a generic callback, expecting object with key-values for group fields
// If onSuccess is provided, onSubmit, action, param, and title properties are ignored
FormPanel.propTypes = {
    submitText: PropTypes.string,
    width: PropTypes.string,
    height: PropTypes.string,
    onSubmit: PropTypes.func, // onSubmit(request) - callback that accepts table request, use with action, params, and title props
    onSuccess: PropTypes.func, // onSuccess(fields) - callback that takes fields object, its keys are the field keys for fields in the given group
    onCancel: PropTypes.func,
    onError: PropTypes.func,
    groupKey: PropTypes.any,
    action: PropTypes.oneOfType([PropTypes.string, PropTypes.func]),
    params: PropTypes.object,
    help_id: PropTypes.string,
    changeMasking: PropTypes.func,
    includeUnmounted: PropTypes.bool
};

function handleChartAdd(request) {
    const {tbl_id} = request;
    onTableLoaded(tbl_id).then( () => {
        const defaultOptions = getDefaultXYPlotOptions(tbl_id);
        if (defaultOptions) {
            ChartsCntlr.dispatchChartAdd({
                chartId: 'xyplot-' + tbl_id,
                chartType: SCATTER,
                groupId: tbl_id,
                chartDataElements: [{tblId: tbl_id, type: DT_XYCOLS, options: defaultOptions}]
            });
        }
    });
}