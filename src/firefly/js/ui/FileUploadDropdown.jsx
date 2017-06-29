/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {FormPanel} from './FormPanel.jsx';
import {dispatchHideDropDown} from '../core/LayoutCntlr.js';
import {FileUploadViewPanel, panelKey, resultSuccess, resultFail} from '../visualize/ui/FileUploadViewPanel.jsx';

const dropdownName = 'FileUploadDropDownCmd';


/**
 *
 * @param props
 * @return {XML}
 * @constructor
 */
export class FileUploadDropdown extends PureComponent {
    constructor(props) {
        super(props);
/*
        this.state = {
            fields: FieldGroupUtils.getGroupFields(panelKey)
        };
*/
    }

    render() {
        return (
            <div style={{padding: 10}}>
                <FormPanel
                    groupKey={panelKey}
                    onSubmit={resultSuccess()}
                    onError={resultFail()}
                    onCancel={hideSearchPanel}>
                    <FileUploadViewPanel />
                </FormPanel>
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
