/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';

import CompleteButton from '../../ui/CompleteButton.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';

import HelpIcon from '../../ui/HelpIcon.jsx';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import AppDataCntlr from '../../core/AppDataCntlr.js';

const popupId = 'ImageAreaStatsPopup';
const helpId = 'visualization.fitsViewer';

// style of the top divs
const rS = {
    padding: 10
};

const tableW = 450;

/**
 * show image area stats popup window
 * @param {string} popTitle
 * @param {object} statsResult image area stats content
 */

export function showImageAreaStatsPopup(popTitle, statsResult) {
    const popup=
        (<PopupPanel title={popTitle} >
            <ImageStats statsResult={statsResult}/>
        </PopupPanel>);

    DialogRootContainer.defineDialog(popupId, popup);
    AppDataCntlr.showDialog(popupId);
}

/**
 * component for image area stats popup
 * @param {object} statsResult
 * @returns {XML}
 *
 */

function ImageStats ( {statsResult} ) {
    return (
        <div>
            <ImageAreaStatsSummary statsSummary={statsResult.statsSummary}/>
            <ImageAreaStatsTable statsTbl={statsResult.statsTable}/>
            <ImageAreaStatsClose />
        </div>
    );
}

ImageStats.propTypes= {
    statsResult: PropTypes.shape({
        statsSummary: PropTypes.array.isRequired,
        statsTable: PropTypes.array.isRequired
    }).isRequired
};

/**
 * component of stats summary
 * @param {array} statsSummary
 * @returns {XML}
 *
 */

function ImageAreaStatsSummary({statsSummary})
{
    var summaryRows = statsSummary.map(function(summaryLine) {
            return (
                <tr key={summaryLine[0]}>
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
 * @returns {XML}
 *
 */

function ImageAreaStatsTable ({statsTbl})
{
    // table style
    var tS = {
        width: tableW,
        border: '1px solid black'
    };

    var tableRows = statsTbl.map(function (statsRow) {
        return (
            <ImageAreaStatsTableRow key={statsRow[0] || statsRow[1]} statsRow={statsRow} />
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

ImageAreaStatsTable.propTypes= {
    statsTbl: PropTypes.arrayOf(PropTypes.array.isRequired).isRequired
};

/**
 * component of stats table row
 * @param {array} statsRow containing title, position and value.
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
    }

    onMouseOut() {
        this.setState({hover: false});
    }

    render() {
        var trS;

        if (!this.props.statsRow[0]) {
            trS = this.rowStates.title;
        } else {
            trS = this.state.hover ? this.rowStates.hover : this.rowStates.nohover;
        }

        var tableCells = this.props.statsRow.map(function (cell) {
            const newline = '\n';
            var dS = {  border: '1px solid black',
                        padding: 5  };

            // cell contains newline (ex. RA:..\n DEC:...)

            if (cell.includes(newline)) {
                var br = cell.split(newline).map(function (line) {
                    return (<span key={line}>{line}<br/></span>);
                });

                return (
                    <td key={cell} style={dS}>
                        { br }
                    </td>
                );
            } else {
                return (
                    <td key={cell} style={dS}>{cell}</td>
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
    statsRow: PropTypes.array.isRequired
};

/**
 * component under the stats table containing close button and help icon
 * @param {string} closeButton
 * @returns {XML}
 * @constructor
 */

function ImageAreaStatsClose ({closeButton='Close'} )
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
                            dialogId={popupId} />
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
    closeButton: PropTypes.string
};