/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import CompleteButton from '../../ui/CompleteButton.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {getDlAry} from '../DrawLayerCntlr.js';
import {dispatchCreateDrawLayer,
        dispatchAttachLayerToPlot,
        dispatchDetachLayerFromPlot,
        dispatchModifyCustomField,
        dispatchDestroyDrawLayer} from '../DrawLayerCntlr.js';
import {getDrawLayerByType, isDrawLayerAttached } from '../PlotViewUtil.js';
import {Tabs, Tab} from '../../ui/panel/TabPanel.jsx';


import HelpIcon from '../../ui/HelpIcon.jsx';
import StatsPoint from '../../drawingLayers/StatsPoint.js';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {dispatchShowDialog} from '../../core/ComponentCntlr.js';


const popupId = 'ImageAreaStatsPopup';
const helpId = 'visualization.fitsViewer';
const typeId = StatsPoint.TYPE_ID;
const noBand = 'NO_BAND';

// style of the top divs
const rS = {
    padding: 10
};

const tableW = 450;

function destroyDrawLayer(plotId)
{
    const dl = getDrawLayerByType(getDlAry(), typeId);

    if (isDrawLayerAttached(dl, plotId)) {
        dispatchDetachLayerFromPlot(typeId, plotId, false);
    }
    dispatchDestroyDrawLayer(typeId);
}


/**
 * show image area stats popup window
 * @param {string} popTitle
 * @param {string} plotId
 * @param {object} statsResult image area stats content
 */

export function showImageAreaStatsPopup(popTitle, statsResult, plotId) {
    var popupStats = statsResult.hasOwnProperty(noBand) ?
                    <ImageStats statsResult={statsResult[noBand]} plotId={plotId}/> :
                    <ImageStatsTab statsResult={statsResult} plotId={plotId}/>;

    var popup = (<PopupPanel title={popTitle}
                             closeCallback={() => destroyDrawLayer(plotId)}>
                    {popupStats}
                 </PopupPanel>);

    DialogRootContainer.defineDialog(popupId, popup);
    dispatchShowDialog(popupId);
}


/**
 * component for image area stats popup
 * @param {object} statsResult
 * @param {string} plotId
 * @returns {XML}
 *
 */

function ImageStats ( {statsResult, plotId} ) {
    return (
        <div>
            <ImageAreaStatsSummary statsSummary={statsResult.statsSummary}/>
            <ImageAreaStatsTable statsTbl={statsResult.statsTable} plotId={plotId}/>
            <ImageAreaStatsClose plotId={plotId}/>
        </div>
    );
}

ImageStats.propTypes= {
    statsResult: PropTypes.shape({
        statsSummary: PropTypes.array.isRequired,
        statsTable: PropTypes.array.isRequired
    }).isRequired,
    plotId: PropTypes.string
};


/**
 * component for image area stats popup
 * @param {object} statsResult
 * @param {string} plotId
 * @returns {XML}
 *
 */

function ImageStatsTab({statsResult, plotId})
{
    var bandList = Object.keys(statsResult);

    var allTabs = bandList.map((bandName, index) => {
        var crtBand = statsResult[bandName];

        return (
              <Tab name={bandName.toUpperCase()} key={index}>
                <div>
                    <ImageAreaStatsSummary statsSummary={crtBand.statsSummary}/>
                    <ImageAreaStatsTable statsTbl={crtBand.statsTable} plotId={plotId}/>
                </div>
            </Tab>
        );
    });

    return (
        <div style={rS}>
            <Tabs useFlex={true}>
                {allTabs}
            </Tabs>
            <ImageAreaStatsClose plotId={plotId}/>
        </div>
    );
}

ImageStatsTab.propTypes= {
    statsResult: PropTypes.shape({
        Blue:  PropTypes.shape({
            statsSummary: PropTypes.array.isRequired,
            statsTable: PropTypes.array.isRequired
        }).isRequired,
        Red:   PropTypes.shape({
            statsSummary: PropTypes.array.isRequired,
            statsTable: PropTypes.array.isRequired
        }).isRequired,
        Green: PropTypes.shape({
            statsSummary: PropTypes.array.isRequired,
            statsTable: PropTypes.array.isRequired
        }).isRequired
    }).isRequired,
    plotId: PropTypes.string
};

/**
 * component of stats summary
 * @param {array} statsSummary
 * @returns {XML}
 *
 */

function ImageAreaStatsSummary({statsSummary})
{
    var summaryRows = statsSummary.map((summaryLine, index) => (
            <tr key={index}>
                <td> {summaryLine[0] + ':'} </td>
                <td> {summaryLine[1]}</td>
            </tr>
        ));


    return (
        <div style={rS}>
            <table>
                <tbody>
                {summaryRows}
                </tbody>
            </table>
        </div>
    );
}

ImageAreaStatsSummary.propTypes={
    statsSummary: PropTypes.arrayOf(PropTypes.array.isRequired).isRequired
};

/**
 * component of stats table
 * @param {array} statsTbl
 * @param {string} plotId
 * @returns {XML}
 *
 */

function ImageAreaStatsTable ({statsTbl, plotId})
{
    // table style
    var tS = {
        width: tableW,
        border: '1px solid black'
    };

    var tableRows = statsTbl.map((statsRow, index) =>
            <ImageAreaStatsTableRow key={index} statsRow={statsRow} plotId={plotId} isTitle={!index}/>
        );

    return (
        <div style={rS}>
            <table style={tS}>
                <colgroup>
                    <col style={{width: '30%'}}/>
                    <col style={{width: '40%'}}/>
                    <col style={{width: '30%'}}/>
                </colgroup>
                <tbody>
                {tableRows}
                </tbody>
            </table>
        </div>
    );
}

ImageAreaStatsTable.propTypes = {
    statsTbl: PropTypes.arrayOf(PropTypes.object).isRequired,
    plotId: PropTypes.string
};

/**
 * component of stats table row
 * @param {array} statsRow containing title, position and value.
 * @param {string} plotId
 * @returns {XML}
 *
 */

class ImageAreaStatsTableRow extends PureComponent {

    constructor(props) {
        super(props);

        this.state = {  hover: false };

        this.rowStates = {
            hover: {
                backgroundColor: '#d3fad1',
                cursor: 'text'
            },

            nohover: {
                backgroundColor: 'transparent',
                cursor: 'default'
            },

            title: {
                backgroundColor: '#888888',
                textAlign: 'center'
            }
        };
    }

    onMouseHover() {
        if (this.props.isTitle) {
            return;
        }

        this.setState({hover: true});

        const dl = getDrawLayerByType(getDlAry(), typeId);
        const {statsRow:{worldPt}, plotId} = this.props;
        //const {worldPt} = this.props.statsRow;

        if (!dl) {
            dispatchCreateDrawLayer(typeId);
        }

        if (!isDrawLayerAttached(dl, plotId)) {
            dispatchAttachLayerToPlot(typeId, plotId);
        }

        dispatchModifyCustomField(typeId, {worldPt}, plotId);
    }

    onMouseOut() {
        if (this.props.isTitle) {
            return;
        }

        this.setState({hover: false});

        const {plotId} = this.props;
        const dl = getDrawLayerByType(getDlAry(), typeId);

        if (isDrawLayerAttached(dl, plotId)) {
            dispatchModifyCustomField(typeId, {}, plotId);
        }
    }

    render() {
        var trS;

        if (this.props.isTitle) {
            trS = this.rowStates.title;
        } else {
            trS = this.state.hover ? this.rowStates.hover : this.rowStates.nohover;
        }

        var tableCells = this.props.statsRow.cells.map( (cell, index) => {
            const newline = '\n';
            var dS = {  border: '1px solid black',
                        padding: 5  };

            // cell contains newline (ex. RA:..\n DEC:...)
            if (cell.includes(newline)) {
                var br = cell.split(newline).map((line, id) => <span key={id}>{line}<br/></span>);

                return (
                    <td key={index} style={dS}>
                        { br }
                    </td>
                );
            } else {
                return (
                    <td key={index} style={dS}>
                        {cell}
                    </td>
                );
            }
        });

        return (
            <tr style={trS}
                onMouseOver={this.onMouseHover.bind(this)}
                onMouseOut={this.onMouseOut.bind(this)}>
                {tableCells}
            </tr>
        );
    }
}

ImageAreaStatsTableRow.propTypes={
    statsRow: PropTypes.shape({
        cells: PropTypes.array.isRequired,
        worldPt: PropTypes.object
    }).isRequired,
    isTitle: PropTypes.bool,
    plotId: PropTypes.string.isRequired
};

/**
 * component under the stats table containing close button and help icon
 * @param {string} closeButton
 * @param {string} plotId
 * @returns {XML}
 * @constructor
 */

function ImageAreaStatsClose ({closeButton='Close', plotId} )
{
    var tbS = {textAlign: 'right', width: tableW};

    return (
        <div style={rS}>
            <table style={tbS}>
                <colgroup>
                    <col style={{width: '92%'}}/>
                    <col style={{width: '8%'}}/>
                </colgroup>
                <tbody>
                    <tr>
                        <td>
                            <CompleteButton
                            text={closeButton}
                            dialogId={popupId}
                            onSuccess={() => destroyDrawLayer(plotId)}
                            />
                        </td>
                        <td>
                            <HelpIcon helpId={helpId}/>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    );
}

ImageAreaStatsClose.PropTypes={
    closeButton: PropTypes.string,
    plotId: PropTypes.string.isRequired
};
