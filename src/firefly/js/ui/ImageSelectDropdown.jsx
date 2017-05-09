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

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../Firefly.js';
import {visRoot } from '../visualize/ImagePlotCntlr.js';
import {FormPanel} from './FormPanel.jsx';
import {dispatchHideDropDown} from '../core/LayoutCntlr.js';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils';
import {ImageSelection, getPlotInfo, panelKey, isCreatePlot, PLOT_NO,
        isOnThreeColorSetting, completeButtonKey} from '../visualize/ui/ImageSelectPanel.jsx';
import {resultSuccess, resultFail} from '../visualize/ui/ImageSelectPanelResult.js';

const dropdownName = 'ImageSelectDropDownCmd';


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
export class ImageSelectDropdown extends PureComponent {
    constructor(props) {
        super(props);

        this.state = {
            fields: FieldGroupUtils.getGroupFields(panelKey),
            visroot: visRoot(),
            doMask : false
        };
        this.changeMasking= (doMask) => this.setState(() => ({doMask}));
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
                    onCancel={hideSearchPanel}
                    changeMasking={this.changeMasking}>
                    <ImageSelection loadButton={false} />
                </FormPanel>
                {this.state.doMask && <div style={maskWrapper}> <div className='loading-mask'/> </div> }
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
