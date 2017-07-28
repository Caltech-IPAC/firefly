/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {FormPanel} from './FormPanel.jsx';
import {dispatchHideDropDown} from '../core/LayoutCntlr.js';
import {FileUploadViewPanel, panelKey, resultSuccess, resultFail, validateModelSelection} from '../visualize/ui/FileUploadViewPanel.jsx';

const dropdownName = 'FileUploadDropDownCmd';

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
export class FileUploadDropdown extends PureComponent {
    constructor(props) {
        super(props);

        this.state = {doMask: false};
        this.changeMasking= (doMask) => this.setState(() => ({doMask}));
    }

    render() {
        return (
            <div style={{padding: 10, width: 1000}}>
                <FormPanel
                    groupKey={panelKey}
                    onSubmit={resultSuccess()}
                    onError={resultFail()}
                    onCancel={hideSearchPanel}
                    params={{hideOnInvalid: false}}
                    changeMasking={this.changeMasking}>
                    <FileUploadViewPanel />
                </FormPanel>
                {this.state.doMask && <div style={maskWrapper}> <div className='loading-mask'/> </div> }
            </div>
        );
    }
}

FileUploadDropdown.propTypes = {
    name: PropTypes.oneOf([dropdownName])
};

FileUploadDropdown.defaultProps = {
    name: dropdownName
};


function hideSearchPanel() {
    dispatchHideDropDown();
}
