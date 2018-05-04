/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../Firefly.js';
import {dispatchShowDialog, dispatchHideDialog} from '../core/ComponentCntlr.js';
import DialogRootContainer from './DialogRootContainer.jsx';
import {PopupPanel} from './PopupPanel.jsx';
import {getActivePlotView, primePlot, getFoV} from '../visualize/PlotViewUtil.js';
import {visRoot, dispatchZoom} from '../visualize/ImagePlotCntlr.js';
import {levels, UserZoomTypes, convertZoomToString, makeFoVString} from '../visualize/ZoomUtil';
import {ToolbarButton} from './ToolbarButton.jsx';

const _levels = levels;

function getDialogBuilder() {
    var popup = null;
    return () => {
        if (!popup) {
            const popup = (
                <PopupPanel title={'Choose Field of View'}>
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
 * @returns {{plotState, colors: Array, hasThreeColorBand: boolean}}
 */
function getInitialPlotState() {
    var pv= getActivePlotView(visRoot());
    var plot= primePlot(pv);

    //var plot = primePlot(visRoot());

    var initcurrLevel = plot && plot.zoomFactor;

    return {
        pv,
        plot,
        initcurrLevel,
        colors: [],
        hasThreeColorBand: false,
    };

}

class ZoomOptionsPopup extends PureComponent {

    constructor(props)  {
        super(props);
        this.state = {plot:primePlot(visRoot()),
            initcurrLevel:primePlot(visRoot()).zoomFactor,
            plotView:getActivePlotView(visRoot())};
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();

    }

    componentDidMount() {

        this.removeListener= flux.addListener(() => {
            const { plot, initcurrLevel} = getInitialPlotState();
            if (plot===this.state.plot) return;
            if (plot) {
                this.setState({plot, initcurrLevel});
            }
            else {
                if (this.removeListener) this.removeListener();
                dispatchHideDialog('zoomOptionsDialog');
            }
        });
    }


    render() {
        const { plot, initcurrLevel, plotView} = this.state;
        return <ZoomOptionsPopupForm  plotView={plotView} plot={plot} initcurrLevel={initcurrLevel}/>;
    }

}

function makeZoomLabel(pv, zfactor) {
    return makeFoVString(getFoV(pv,zfactor));
}

function ZoomOptionsPopupForm() {

   const {plot, pv, initcurrLevel} = getInitialPlotState();

    var verticalColumn = {display: 'inline-block', padding: '10px 10px 20px 25px'};
    var zoom_levels = _levels;

    var initcurrZoomLevelStr = convertZoomToString(initcurrLevel);


    var optionArray = [];
    for (var i = 0; i < zoom_levels.length; i++) {
        // optionArray[i] = {label: convertZoomToString(zoom_levels[i]), value: zoom_levels[i]};
        optionArray[i] = {label: makeZoomLabel(pv,zoom_levels[i]), value: zoom_levels[i]};
    }

    return (
            <div style={{ minWidth:150, minHeight: 300} }>

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
    plot: PropTypes.object.isRequired,
    opAry : PropTypes.array.isRequired,
    currLevel : PropTypes.number.isRequired
};

function resultsSuccess(plotId,zoomLevel) {
    var zoom= Number(zoomLevel);
    dispatchZoom({plotId, userZoomType:UserZoomTypes.LEVEL, level:zoom});
}
