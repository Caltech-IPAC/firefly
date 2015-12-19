/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';

var FormPanel = function (props) {
    var {children, onSubmit, onCancel} = props;

    return (
        <div>
            <div style={{width: 500, height: 300, backgroundColor: 'white'}}>
                {children}
            </div>
            <div style={{display: 'inline-flex', margin: '5px 10px'}}>
                <button type='button' style={{height: '25px'}} onClick={onSubmit}><b>Search</b></button> &nbsp;&nbsp;
                <button type='button' style={{height: '25px'}} onClick={onCancel}>Cancel</button>
            </div>
        </div>
    );
};


FormPanel.propTypes = {
    onSubmit: React.PropTypes.func,
    onCancel: React.PropTypes.func
};


export default FormPanel;