/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */



// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// THIS PANEL IS TEMPORARY, ONLY TO TEST CATALOGS UNTIL WE FINISH THE REAL PANEL
// This panel will do search on 3 of the most common IRSA catalogs
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

import React, { Component, PropTypes} from 'react';
import {flux} from '../Firefly.js';

import FormPanel from './FormPanel.jsx';
import {dispatchHideDropDownUi} from '../core/LayoutCntlr.js';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils';
import {ImageSelection, getPlotInfo, panelKey, isCreatePlot} from '../visualize/ui/ImageSelectPanel.jsx';
import {resultSuccess, resultFail} from '../visualize/ui/ImageSelectPanelResult.js';

const dropdownName = 'ImageSelectDropDownCmd';
/**
 *
 * @param props
 * @return {XML}
 * @constructor
 */
export class ImageSelectDropdown extends Component {
    constructor(props) {
        super(props);

        this.plotInfo = getPlotInfo();
        this.state = {
            addPlot: false
        };
    }


    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.removeListener) this.removeListener();
    }

    componentDidMount() {
        this.iAmMounted = true;
        this.removeListener = flux.addListener(() => this.stateUpdate());

    }

    stateUpdate() {
        var fields = FieldGroupUtils.getGroupFields(panelKey);

        if (fields && this.iAmMounted ) {
             this.setState({addPlot: isCreatePlot(this.plotInfo.plotMode, fields)});
        }
    }

    render() {
        var {addPlot} = this.state;
        var {viewerId} = this.plotInfo;

        return (
            <div style={{padding: 10}}>
                <FormPanel
                    groupKey={panelKey}
                    onSubmit={resultSuccess(addPlot, viewerId, true)}
                    onError={resultFail()}
                    onCancel={hideSearchPanel}>
                    <ImageSelection plotMode={this.plotInfo.plotMode}
                                    viewerId={this.plotInfo.viewerId}
                                    loadButton={false}
                    />
                </FormPanel>
            </div>);
    }
}

ImageSelectDropdown.propTypes = {
    name: PropTypes.oneOf([dropdownName])
};

ImageSelectDropdown.defaultProps = {
    name: dropdownName
};


function hideSearchPanel() {
    dispatchHideDropDownUi();
}
