/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useState} from 'react';
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
const helpId = 'visualization.selectregion';
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
    const summaryRows = statsSummary.map((summaryLine, index) => (
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




const rowStates = {
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


/**
 * component of stats table row
 */
const ImageAreaStatsTableRow= ({isTitle,statsRow, plotId}) => {
    const [hover,setHover]= useState(() => false);

    const onMouseHover= () => {
        if (isTitle) return;
        setHover(true);

        const dl = getDrawLayerByType(getDlAry(), typeId);
        if (!dl) dispatchCreateDrawLayer(typeId);


        if (!isDrawLayerAttached(dl, plotId)) {
            dispatchAttachLayerToPlot(typeId, plotId);
        }
        const {worldPt}= statsRow;
        dispatchModifyCustomField(typeId, {worldPt}, plotId);
    };

    const onMouseOut= () => {
        if (isTitle) return;
        setHover(false);
        const dl = getDrawLayerByType(getDlAry(), typeId);
        if (isDrawLayerAttached(dl, plotId)) {
            dispatchModifyCustomField(typeId, {}, plotId);
        }
    };

    let trS;
    if (isTitle) trS = rowStates.title;
    else trS = hover ? rowStates.hover : rowStates.nohover;


    const tableCells = statsRow.cells.map( (cell, index) => {
        const newline = '\n';
        const dS = {  border: '1px solid black', padding: 5  };

        // cell contains newline (ex. RA:..\n DEC:...)
        if (cell.includes(newline)) {
            const br = cell.split(newline).map((line, id) => <span key={id}>{line}<br/></span>);
            return ( <td key={index} style={dS}> { br } </td> );
        } else {
            return ( <td key={index} style={dS}> {cell} </td> );
        }
    });

    return (
        <tr style={trS} onMouseOver={onMouseHover} onMouseOut={onMouseOut}> {tableCells} </tr>
    );
};

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

const ImageAreaStatsClose= ({closeButton='Close', plotId} ) => (
        <div style={rS}>
            <table style={{textAlign: 'right', width: tableW}}>
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

ImageAreaStatsClose.propTypes={
    closeButton: PropTypes.string,
    plotId: PropTypes.string.isRequired
};
