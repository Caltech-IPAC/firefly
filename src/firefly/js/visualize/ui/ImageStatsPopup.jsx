/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import CompleteButton from '../../ui/CompleteButton.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import PopupPanel from '../../ui/PopupPanel.jsx';
import AppDataCntlr from '../../core/AppDataCntlr.js';

const popupId = 'ImageAreaStatsPopup';
const rS = {
    padding: '10px 10px 10px 10px'
};
const tS = {
    width: '450px',
    border: '1px solid black'
};

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
            var dS = {border: '1px solid black', padding: '5'};

            // cell contains newline (location)
            if (cell.includes(newline)) {
                var lines = cell.split(newline);

                var br = lines.map(function (line) {
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
                onMouseOver={this.onMouseHover.bind(this)} onMouseOut={this.onMouseOut.bind(this)}>
                {tableCells}
            </tr>
        );
    }
}

ImageAreaStatsTableRow.propTypes={
    statsRow: React.PropTypes.array.isRequired
};

/**
 * component under the stats table containing close button and help icon
 * @param {string} closeButton
 * @param {string} imgFile
 * @returns {XML}
 * @constructor
 */

// TODO: add help icon
function ImageAreaStatsClose ({closeButton='Close', imgFile=''} )
{
    var rcS = Object.assign({}, rS, {float: 'right'});

    return (
        <div style={rcS} >
            <CompleteButton
                style={{padding : '5px'}}
                text={closeButton}
                dialogId={popupId} />
        </div>
    );
}

ImageAreaStatsClose.PropTypes={
    closeButton: PropTypes.string,
    imgFile: PropTypes.string
};