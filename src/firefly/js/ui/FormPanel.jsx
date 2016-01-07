/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import CompleteButton from './CompleteButton.jsx';
import TablesCntlr from '../tables/TablesCntlr.js';


function handleFailfure() {

}

function createSuccessHandler(action, params, onSubmit) {

    return (request={}) => {
        if (action === TablesCntlr.FETCH_TABLE) {
            if (params) {
                request = Object.assign(request, params);
            }
            TablesCntlr.dispatchFetchTable(request);
        }
        if (onSubmit) {
            onSubmit(request);
        }
    };
}

var FormPanel = function (props) {
    var {children, onSubmit, onCancel, onError, groupKey, action, params} = props;

    return (
        <div>
            <div style={{width: 500, height: 300, backgroundColor: 'white'}}>
                {children}
            </div>
            <div style={{display: 'inline-flex', margin: '5px 10px'}}>
                <CompleteButton groupKey={groupKey}
                                onSuccess={createSuccessHandler(action, params, onSubmit)}
                                onFail={onError || handleFailfure}
                                text = 'Search'
                /> &nbsp;&nbsp;&nbsp;
                <button type='button' className='button__std' onClick={onCancel}>Cancel</button>
            </div>
        </div>
    );
};


FormPanel.propTypes = {
    onSubmit: React.PropTypes.func,
    onCancel: React.PropTypes.func,
    groupKey: React.PropTypes.string,
    action: React.PropTypes.string,
    params: React.PropTypes.object
};


export default FormPanel;