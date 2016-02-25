/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import CompleteButton from '../../ui/CompleteButton.jsx';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import PopupPanel from '../../ui/PopupPanel.jsx';
import AppDataCntlr from '../../core/AppDataCntlr.js';
import HelpIcon from '../../ui/HelpIcon.jsx';

const popupId = 'ImageAreaStatsPopup';
const helpId = 'visualization.fitsViewer';
const rS = {
    padding: '10'
};
const tS = {
    width: '450px',
    border: '1px solid black'
};

export function showImageAreaStatsPopup(popTitle, statsResult, helpImage) {
    const popup=
        (<PopupPanel title={popTitle} >
            <ImageStats statsResult={statsResult} helpImage={helpImage}/>
        </PopupPanel>);

    DialogRootContainer.defineDialog(popupId, popup);
    AppDataCntlr.showDialog(popupId);
}

/**
 * component for image area stats popup
 * @param {object} statsResult
 * @param {string} helpImage image for help icon
 * @returns {XML}
 *
 */

function ImageStats ( {statsResult, helpImage} ) {
    return (
        <div>
            <ImageAreaStatsSummary statsSummary={statsResult.statsSummary}/>
            <ImageAreaStatsTable statsTbl={statsResult.statsTable}/>
            <ImageAreaStatsClose imgFile={helpImage} />
        </div>
    );
}

ImageStats.propTypes= {
    statsResult: PropTypes.shape({
        statsSummary: PropTypes.array.isRequired,
        statsTable: PropTypes.array.isRequired
    }).isRequired,
    helpImage: PropTypes.string
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
    statsRow: PropTypes.array.isRequired
};

/**
 * component under the stats table containing close button and help icon
 * @param {string} closeButton
 * @param {string} imgFile
 * @returns {XML}
 * @constructor
 */

function ImageAreaStatsClose ({closeButton='Close', imgFile=''} )
{
    var rcS = Object.assign({}, rS, {float: 'right'});
    var sideW = 23;

    return (
        <div>
            <div style={rcS} >
                <HelpIcon width={sideW} height={sideW} id={helpId} src={imgFile}/>
            </div>
            <div style={rcS} >
                <CompleteButton
                style={{padding : '5px'}}
                text={closeButton}
                dialogId={popupId} />
            </div>
        </div>
    );
}

ImageAreaStatsClose.PropTypes={
    closeButton: PropTypes.string,
    imgFile: PropTypes.string
};