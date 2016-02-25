/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import CompleteButton from './CompleteButton.jsx';
import * as TablesCntlr from '../tables/TablesCntlr.js';


function handleFailfure() {

}

function createSuccessHandler(action, params, onSubmit) {

    return (request={}) => {
        if (action === TablesCntlr.TABLE_FETCH) {
            if (params) {
                request = Object.assign(request, params);
            }
            TablesCntlr.dispatchTableFetch(request);
        }
        if (onSubmit) {
            onSubmit(request);
        }
    };
}

var FormPanel = function (props) {
    var {children, onSubmit, onCancel, onError, groupKey, action, params, width, height} = props;

    return (
        <div>
            <div style={{width, height, backgroundColor: 'white'}}>
                {children}
            </div>
            <div style={{display: 'inline-flex', margin: '5px 10px'}}>
                <CompleteButton groupKey={groupKey}
                                onSuccess={createSuccessHandler(action, params, onSubmit)}
                                onFail={onError || handleFailfure}
                                text = 'Search'
                /> &nbsp;&nbsp;&nbsp;
                <button type='button' className='button-std' onClick={onCancel}>Cancel</button>
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