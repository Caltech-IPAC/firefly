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

export const FormPanel = function (props) {
    var {children, onSubmit, onCancel, onError, groupKey, action, params, width='100%', height='100%', submitText='Search'} = props;

    const style = { width, height,
        backgroundColor: 'white',
        border: '1px solid rgba(0,0,0,0.3)',
        padding: 5,
        marginBottom: 5,
        boxSizing: 'border-box'
    };
    return (
        <div>
            <div style={style}>
                {children}
            </div>
            <CompleteButton style={{display: 'inline-block', marginRight: 10}}
                            groupKey={groupKey}
                            onSuccess={createSuccessHandler(action, params, onSubmit)}
                            onFail={onError || handleFailfure}
                            text = {submitText}
            />
            <button style={{display: 'inline-block'}} type='button' className='button std' onClick={onCancel}>Cancel</button>
        </div>
    );
};


FormPanel.propTypes = {
    submitText: React.PropTypes.string,
    width: React.PropTypes.string,
    height: React.PropTypes.string,
    onSubmit: React.PropTypes.func,
    onCancel: React.PropTypes.func,
    onError: React.PropTypes.func,
    groupKey: React.PropTypes.any,
    action: React.PropTypes.string,
    params: React.PropTypes.object
};


