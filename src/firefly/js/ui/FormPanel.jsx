/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import CompleteButton from './CompleteButton.jsx';
import * as TablesCntlr from '../tables/TablesCntlr.js';
import {HelpIcon} from './HelpIcon.jsx';
import {dispatchHideDropDown} from '../core/LayoutCntlr.js';
import {makeTblRequest, onTableLoaded} from '../tables/TableUtil.js';
import {getDefaultXYPlotOptions, DT_XYCOLS} from '../charts/dataTypes/XYColsCDT.js';
import {SCATTER} from '../charts/ChartUtil.js';
import * as ChartsCntlr from '../charts/ChartsCntlr.js';

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

        if (onSubmit) {
            onSubmit(request);
        }
        dispatchHideDropDown();
    };
}

export const FormPanel = function (props) {
    var {children, onSubmit, onCancel, onError, groupKey, action, params, title,
        submitText='Search', help_id, changeMasking} = props;

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
                                    groupKey={groupKey}
                                    onSuccess={createSuccessHandler(action, params, title, onSubmit)}
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


FormPanel.propTypes = {
    submitText: PropTypes.string,
    width: PropTypes.string,
    height: PropTypes.string,
    onSubmit: PropTypes.func,
    onCancel: PropTypes.func,
    onError: PropTypes.func,
    groupKey: PropTypes.any,
    action: PropTypes.oneOfType([PropTypes.string, PropTypes.func]),
    params: PropTypes.object,
    help_id: PropTypes.string,
    changeMasking: PropTypes.func
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