/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {Component, PropTypes} from 'react';
import {flux} from '../Firefly.js';
import {dispatchShowDialog} from '../core/DialogCntlr.js';
import {Operation} from '../visualize/PlotState.js';
import {SingleColumnMenu} from './DropDownMenu.jsx';
import {FieldGroup} from './FieldGroup.jsx';
import InputGroup from './InputGroup.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {getActivePlotView, primePlot} from '../visualize/PlotViewUtil.js';
import {visRoot, dispatchZoom, ActionScope} from '../visualize/ImagePlotCntlr.js';
import {levels, UserZoomTypes, convertZoomToString, getZoomMax} from '../visualize/ZoomUtil';
import {ToolbarButton} from './ToolbarButton.jsx';
import {LinkButton} from './LinkButton.jsx';
import CompleteButton from './CompleteButton.jsx';

import HelpIcon from './HelpIcon.jsx';

const _levels = levels;
const _userZoomTypes = UserZoomTypes;
const zoomMax = getZoomMax();

function getDialogBuilder() {
    var popup = null;
    return () => {
        if (!popup) {
            const popup = (
                <PopupPanel title={'Zoom Level'}>
                    <ZoomOptionsPopup groupKey={'ZOOM_OPTIONS_FORM'}/>
                </PopupPanel>
            );
            DialogRootContainer.defineDialog('zoomOptionsDialog', popup);
        }
        return popup;
    };
}

const dialogBuilder = getDialogBuilder();

export function showZoomOptionsPopup() {
    dialogBuilder();
    dispatchShowDialog('zoomOptionsDialog');
}


/**
 * This method is called when the dialog is rendered. Only when an image is loaded, the PlotView is available.
 * Then, the color band, plotState etc can be determined.
 * @returns {{plotState, colors: Array, hasThreeColorBand: boolean, hasOperation: boolean}}
 */
function getInitialPlotState() {
    var pv= getActivePlotView(visRoot());
    var plot= primePlot(pv);

    //var plot = primePlot(visRoot());

    var initcurrLevel = plot.zoomFactor;

    var plotState = plot.plotState;


    var isCrop = plotState.hasOperation(Operation.CROP);
    var isRotation = plotState.hasOperation(Operation.ROTATE);
    var cropNotRotate = isCrop && !isRotation ? true : false;

    return {
        pv,
        plot,
        initcurrLevel,
        colors: [],
        hasThreeColorBand: false,
        hasOperation: cropNotRotate
    };

}

class ZoomOptionsPopup extends React.Component {

    constructor(props)  {
        super(props);
        this.state = {plot:primePlot(visRoot()),initcurrLevel:primePlot(visRoot()).zoomFactor};
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();

    }

    componentDidMount() {

        this.removeListener= flux.addListener(() => {
            const { plot, initcurrLevel} = getInitialPlotState();
            this.setState({plot, initcurrLevel});
        });
    }


    render() {
        const { plot, initcurrLevel} = this.state;
        return <ZoomOptionsPopupForm  plot={plot} initcurrLevel={initcurrLevel}/>;
    }

}

function ZoomOptionsPopupForm() {

   const {plot, initcurrLevel} = getInitialPlotState();

    var verticalColumn = {display: 'inline-block', paddingLeft: 10, paddingBottom: 20, paddingRight: 10};
    var zoom_levels = _levels;

    var initcurrZoomLevelStr = convertZoomToString(initcurrLevel);


    var optionArray = [];
    for (var i = 0; i < zoom_levels.length; i++) {
        optionArray[i] = {label: convertZoomToString(zoom_levels[i]), value: zoom_levels[i]};
    }

    return (
            <div style={{ minWidth:100, minHeight: 300} }>

                <div style={verticalColumn}>
                    {makezoomItems(plot, optionArray)}
                </div>

            </div>

    );

}


function makezoomItems(plot,opAry) {
    return opAry.map( (levelStr,opId) => {
        if (levelStr.value===plot.zoomFactor) {
            return(
                <u key={opId}>{levelStr.label} :  Current </u>

            );

        }
        else {
            return (
                <ToolbarButton text={levelStr.label} tip={levelStr.label}
                               enabled={true} horizontal={false} key={opId}
                               onClick={() =>resultsSuccess(plot.plotId,levelStr.value)}
                />
            );
        }
    });
}

makezoomItems.propTypes= {
    plot: React.PropTypes.object.isRequired,
    opAry : React.PropTypes.array.isRequired,
    currLevel : React.PropTypes.number.isRequired
};

function resultsSuccess(plotId,zoomLevel) {
        var zoom= Number(zoomLevel);
        dispatchZoom(plotId, _userZoomTypes.LEVEL, true, false, false, zoom, ActionScope.GROUP);
}

function resultsOK(request) {
    console.log(request + 'You clicked OK ');
}

function resultsFail(request) {
    console.log(request + ': Error');
}
