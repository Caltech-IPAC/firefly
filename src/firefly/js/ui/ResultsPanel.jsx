/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {get, pick} from 'lodash';

import DockLayoutPanel from './panel/DockLayoutPanel.jsx';
import {dispatchSetLayoutMode, getExpandedMode, getStandardMode, LO_EXPANDED, LO_STANDARD} from '../core/LayoutCntlr.js';

const wrapperStyle = { flex: 'auto', display: 'flex', flexFlow: 'column', overflow: 'hidden'};

export const ResultsPanel = function (props) {
    const expandedView = getExpandedMode();
    return (
            expandedView
                ? <ExpandedView key='res-exp-view' expandedView={expandedView} { ...pick(props, ['imagePlot','xyPlot','tables'] ) } />
                : <StandardView key='res-std-view' {...props} />
            );
};

ResultsPanel.propTypes = {
    visToolbar: React.PropTypes.element,
    searchDesc: React.PropTypes.element,
    title: React.PropTypes.string,
    imagePlot: React.PropTypes.element,
    xyPlot: React.PropTypes.element,
    tables: React.PropTypes.element,
    layout: React.PropTypes.object
};


const ExpandedView = ({expandedView, imagePlot, xyPlot, tables}) => {
    const view = expandedView === LO_EXPANDED.tables.view ? tables
                : expandedView === LO_EXPANDED.xyPlots.view ? xyPlot
                : imagePlot;
    return (
        <div style={wrapperStyle}>
            {view}
        </div>
    );
};


const StandardView = ({visToolbar, title, searchDesc, imagePlot, xyPlot, tables, layout}) => {
    const components = resolveComponents(imagePlot, xyPlot, tables, layout);
    const config = generateLayout(components, layout);

    return (
        <div style={wrapperStyle}>
            {visToolbar}
            <SearchDesc searchDesc={searchDesc}/>
            {title && <h2 style={{textAlign: 'center'}}>{title}</h2>}
            <DockLayoutPanel config={ config } >
                {components}
            </DockLayoutPanel>
        </div>
    );
};

const SearchDesc = ({searchDesc}) => {
    return (
        <div>
            {searchDesc ? <div id='results-searchDesc' style={ {display: 'inline-block'} }>{searchDesc}</div>
                : <div style={{fontSize: 'medium', display: 'inline-block'}}> >>Search description not provided..</div>}
            <div style={ {display: 'inline-block', float: 'right'} }>
                <button type='button' className='button-std'
                        onClick={() => dispatchSetLayoutMode(LO_STANDARD.tri_view)}>tri-view</button>&nbsp;
                <button type='button' className='button-std'
                        onClick={() => dispatchSetLayoutMode(LO_STANDARD.image_table)}>img-tbl</button>&nbsp;
                <button type='button' className='button-std'
                        onClick={() => dispatchSetLayoutMode(LO_STANDARD.image_xyplot)}>img-xy</button>&nbsp;
                <button type='button' className='button-std'
                        onClick={() => dispatchSetLayoutMode(LO_STANDARD.xyplot_table)}>xy-tbl</button>
            </div>
        </div>
    );
};

function generateLayout(results) {
    const mode = getStandardMode() || LO_STANDARD.tri_view.view;
    if (mode === LO_STANDARD.image_table.view) {
        return {east: {index: 0,  defaultSize: '50%'}, west: {index: 1} };
    } else if (mode === LO_STANDARD.image_xyplot.view) {
        return {north: {index: 0,  defaultSize: '50%'}, south: {index: 1} };
    } else if (mode === LO_STANDARD.xyplot_table.view) {
        return {north: {index: 0,  defaultSize: '50%'}, south: {index: 1} };
    } else {
        if ( results.length === 3 ) {
            return {east: {index: 0}, west: {index: 1}, south: {index: 2, defaultSize: 'calc(100% - 300px)'}};
        } else if (results.length === 2 ) {
            return {east: {index: 0,  defaultSize: '50%'}, west: {index: 1} };
        } else {
            return {center: {index: 0,  defaultSize: '50%'}, resize: false };
        }
    }
}
function resolveComponents(imagePlot, xyPlot, tables) {
    const mode = getStandardMode() || LO_STANDARD.tri_view.view;
    if (mode === LO_STANDARD.image_table.view) {
        return [imagePlot, tables];
    } else if (mode === LO_STANDARD.image_xyplot.view) {
        return [imagePlot, xyPlot];
    } else if (mode === LO_STANDARD.xyplot_table.view) {
        return [tables, xyPlot];
    } else {
        return [imagePlot, tables, xyPlot];
    }
}

