/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import DockLayoutPanel from './panel/DockLayoutPanel.jsx';

function exists (...components) {

    return components.reduce( (ans, cval) => {
        return ans && cval;
    }, true );
}

const ResultsPanel = function (props) {
    var {visToolbar,title, imagePlot, xyPlot, tables} = props;
    var children = [imagePlot, xyPlot, tables].filter( (el) => { return (el); } );

    var layout;
    if ( exists(imagePlot, xyPlot, tables) ) {
        layout = { north: {index: children.indexOf(imagePlot), defaultSize: '60%'},
                   east: {index: children.indexOf(tables)},
                   west: {index: children.indexOf(xyPlot)}
                 };
    } else if ( exists(imagePlot, tables) ) {
        layout = {east: {index: children.indexOf(imagePlot)},
                  west: {index: children.indexOf(tables)}
                 };
    } else if ( exists(xyPlot, tables) ) {
        layout = {east: {index: children.indexOf(xyPlot)},
                  west: {index: children.indexOf(tables)}
                 };
    } else if ( exists(imagePlot, xyPlot) ) {
        layout = {east: {index: children.indexOf(imagePlot)},
                  west: {index: children.indexOf(xyPlot)}
                 };
    }

    return (
        <div style={{ flex: 'auto', display: 'flex', flexFlow: 'column'}}>
            {visToolbar}
            <h2 style={{textAlign: 'center'}}>{title}</h2>
            <DockLayoutPanel layout={ layout } >
                {children}
            </DockLayoutPanel>
        </div>
    );
};

ResultsPanel.propTypes = {
    visToolbar: React.PropTypes.element,
    title: React.PropTypes.string,
    imagePlot: React.PropTypes.element,
    xyPlot: React.PropTypes.element,
    tables: React.PropTypes.element
};







export default ResultsPanel;


