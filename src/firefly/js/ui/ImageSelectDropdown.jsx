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
import {visRoot } from '../visualize/ImagePlotCntlr.js';
import FormPanel from './FormPanel.jsx';
import {dispatchHideDropDown} from '../core/LayoutCntlr.js';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils';
import {ImageSelection, getPlotInfo, panelKey, isCreatePlot, PLOT_NO,
        isOnThreeColorSetting, completeButtonKey} from '../visualize/ui/ImageSelectPanel.jsx';
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

        this.state = {
            fields: FieldGroupUtils.getGroupFields(panelKey),
            visroot: visRoot()
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
        var vr = visRoot();

        if (fields !== this.state.fields || vr.activePlotId !== this.state.visroot.activePlotId) {
            if (this.iAmMounted) {
                this.setState({
                    fields, visroot: vr
                });
            }
        }
    }

    render() {
        var {plotId, viewerId, plotMode} = getPlotInfo(this.state.visroot);
        var addPlot = isCreatePlot(plotMode, this.state.fields);
        var isThreeColor = isOnThreeColorSetting(plotMode, this.state.fields);
        var plotInfo = {addPlot, isThreeColor, plotId, viewerId};

        return (
            <div style={{padding: 10}}>
                <FormPanel
                    groupKey={completeButtonKey(isThreeColor)}
                    onSubmit={resultSuccess(plotInfo, true)}
                    onError={resultFail()}
                    onCancel={hideSearchPanel}>
                    <ImageSelection loadButton={false} />
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
    dispatchHideDropDown();
}
