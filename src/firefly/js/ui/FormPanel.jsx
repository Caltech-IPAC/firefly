/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';

import CompleteButton from './CompleteButton.jsx';
import * as TablesCntlr from '../tables/TablesCntlr.js';
import {HelpIcon} from './HelpIcon.jsx';


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
    var {children, onSubmit, onCancel, onError, groupKey, action, params,
        width='100%', height='100%', submitText='Search', help_id, changeMasking} = props;

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
            <div style={{display: 'inline-flex', justifyContent: 'space-between', width: '100%', alignItems: 'flex-end'}}>
                <div>
                    <CompleteButton style={{display: 'inline-block', marginRight: 10}}
                                    groupKey={groupKey}
                                    onSuccess={createSuccessHandler(action, params, onSubmit)}
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
    action: PropTypes.string,
    params: PropTypes.object,
    help_id: PropTypes.string,
    changeMasking: PropTypes.func,
};


