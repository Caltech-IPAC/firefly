/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';

import CompleteButton from '../../ui/CompleteButton.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {getDlAry} from '../DrawLayerCntlr.js';
import DrawLayerCntlr from '../DrawLayerCntlr.js';
import {getDrawLayerByType, isDrawLayerAttached } from '../PlotViewUtil.js';
import {Tabs, Tab} from '../../ui/panel/TabPanel.jsx';


import HelpIcon from '../../ui/HelpIcon.jsx';
import StatsPoint from '../../drawingLayers/StatsPoint.js';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import AppDataCntlr from '../../core/AppDataCntlr.js';


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

    if (dl) {
        if (!isDrawLayerAttached(dl, plotId)) {
            DrawLayerCntlr.dispatchDetachLayerFromPlot(typeId, plotId, false, false);
        }
        DrawLayerCntlr.dispatchDestroyDrawLayer(typeId);
    }
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
    AppDataCntlr.showDialog(popupId);
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

class ImageStatsTab extends React.Component {

    constructor (props) {
        super(props);

        this.bandList = Object.keys(props.statsResult);
        this.state = {
            selected: 0
        };
    }

    onClickTab (index) {
        this.setState({selected: index});
    }

    render() {
        var {statsResult, plotId} = this.props;
        var crtBand = this.bandList[this.state.selected];

        var allTabs = this.bandList.map(function (bandName, index) {
             return (
                <Tab name={bandName.toUpperCase()} key={index}>
                    <ImageAreaStatsTable statsTbl={statsResult[bandName].statsTable} plotId={plotId}/>
                </Tab>
            );
        });

        return (
            <div>
                <ImageAreaStatsSummary statsSummary={statsResult[crtBand].statsSummary}/>
                <Tabs onTabSelect={this.onClickTab.bind(this)}>
                    {allTabs}
                </Tabs>
                <ImageAreaStatsClose plotId={plotId}/>
            </div>
        );
    }
}

ImageStatsTab.propTypes= {
    statsResult: PropTypes.object.isRequired,
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
    var summaryRows = statsSummary.map(function(summaryLine, index) {
            return (
                <tr key={index}>
                   <td> {summaryLine[0] + ':'} </td>
                   <td> {summaryLine[1]}</td>
                </tr>
            );
        });


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

ImageAreaStatsSummary.PropTypes={
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

    var tableRows = statsTbl.map(function (statsRow, index) {
        return (
            <ImageAreaStatsTableRow key={index} statsRow={statsRow} plotId={plotId}/>
        );
    });

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

class ImageAreaStatsTableRow extends React.Component {

    constructor(props) {
        super(props);
        this.state = {hover: false};
        this.rowStates = {
            hover: {
                backgroundColor: 'yellow',
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
        this.setState({hover: true});

        const dl = getDrawLayerByType(getDlAry(), typeId);
        const {statsRow:{worldPt}, plotId} = this.props;
        //const {worldPt} = this.props.statsRow;

        if (!dl) {
            DrawLayerCntlr.dispatchCreateDrawLayer(typeId);
        }

        if (!isDrawLayerAttached(dl, plotId)) {
            DrawLayerCntlr.dispatchAttachLayerToPlot(typeId, plotId);
        }

        DrawLayerCntlr.dispatchModifyCustomField(typeId, {worldPt}, plotId, false);
    }

    onMouseOut() {
        this.setState({hover: false});

        const {plotId} = this.props;
        const dl = getDrawLayerByType(getDlAry(), typeId);

        if (dl && isDrawLayerAttached(dl, plotId)) {
            DrawLayerCntlr.dispatchModifyCustomField(typeId, {}, plotId, false);
        }
    }

    render() {
        var trS;
        var cells = this.props.statsRow.cells;

        if (!cells[0]) {
            trS = this.rowStates.title;
        } else {
            trS = this.state.hover ? this.rowStates.hover : this.rowStates.nohover;
        }

        var tableCells = cells.map(function (cell, index) {
            const newline = '\n';
            var dS = {  border: '1px solid black',
                        padding: 5  };

            // cell contains newline (ex. RA:..\n DEC:...)

            if (cell.includes(newline)) {
                var br = cell.split(newline).map(function (line, id) {
                    return (<span key={id}>{line}<br/></span>);
                });

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
    statsRow: PropTypes.object.isRequired,
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