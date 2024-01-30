/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {FormPanel} from './FormPanel.jsx';
import {dispatchHideDropDown} from '../core/LayoutCntlr.js';
import {WorkspaceViewPanel, resultSuccess, resultFail} from '../visualize/ui/WorkspaceViewPanel.jsx';

const dropdownName = 'WorkspaceDropDownCmd';

const maskWrapper= {
    position:'absolute',
    left:0,
    top:0,
    width:'100%',
    height:'100%'
};


/**
 *
 * @param props
 * @return {XML}
 * @constructor
 */
export class WorkspaceDropdown extends PureComponent {
    constructor(props) {
        super(props);

        this.state = {doMask: false};
        this.changeMasking= (doMask) => this.setState(() => ({doMask}));
    }

    render() {
        return (
            <div style={{padding: 10}}>
                <FormPanel
                    cancelText=''
                    submitText={'close'}
                    changeMasking={this.changeMasking}>
                    <WorkspaceViewPanel />
                </FormPanel>
                {this.state.doMask && <div style={maskWrapper}> <div className='loading-mask'/> </div> }
            </div>
        );
    }
}

WorkspaceDropdown.propTypes = {
    name: PropTypes.oneOf([dropdownName])
};

WorkspaceDropdown.defaultProps = {
    name: dropdownName
};


function hideSearchPanel() {
    dispatchHideDropDown();
}
