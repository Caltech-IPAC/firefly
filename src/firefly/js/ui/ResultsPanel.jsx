/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {get, pick} from 'lodash';

import DockLayoutPanel from './panel/DockLayoutPanel.jsx';
import {dispatchUpdateLayout} from '../core/AppDataCntlr.js';
import {LO_STD_MODE} from '../core/AppDataCntlr.js';
import {LO_XPD_MODE} from '../core/AppDataCntlr.js';

const wrapperStyle = { flex: 'auto', display: 'flex', flexFlow: 'column'};

export const ResultsPanel = function (props) {
    const expandedView = get(props, 'layoutInfo.mode.expanded');
    return (
            expandedView
                ? <ExpandedView expandedView={expandedView} { ...pick(props, ['imagePlot','xyPlot','tables'] ) } />
                : <StandardView {...props} />
            );
};

ResultsPanel.propTypes = {
    visToolbar: React.PropTypes.element,
    searchDesc: React.PropTypes.element,
    title: React.PropTypes.string,
    imagePlot: React.PropTypes.element,
    xyPlot: React.PropTypes.element,
    tables: React.PropTypes.element,
    layoutInfo: React.PropTypes.object
};


const ExpandedView = ({expandedView, imagePlot, xyPlot, tables}) => {
    const view = expandedView === LO_XPD_MODE.tables.mode.expanded ? tables
                : expandedView === LO_XPD_MODE.xy_plots.mode.expanded ? xyPlot
                : imagePlot;
    return (
        <div style={wrapperStyle}>
            {view}
        </div>
    );
};


const StandardView = ({visToolbar, title, searchDesc, imagePlot, xyPlot, tables, layoutInfo}) => {
    const components = resolveComponents(imagePlot, xyPlot, tables, layoutInfo);
    const layout = generateLayout(components, layoutInfo);

    return (
        <div style={wrapperStyle}>
            {visToolbar}
            <SearchDesc searchDesc={searchDesc}/>
            title && <h2 style={{textAlign: 'center'}}>{title}</h2>
            <DockLayoutPanel layout={ layout } >
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
                        onClick={() => dispatchUpdateLayout(LO_STD_MODE.triview)}>tri-view</button>&nbsp;
                <button type='button' className='button-std'
                        onClick={() => dispatchUpdateLayout(LO_STD_MODE.image_table)}>img-tbl</button>&nbsp;
                <button type='button' className='button-std'
                        onClick={() => dispatchUpdateLayout(LO_STD_MODE.image_xyplot)}>img-xy</button>&nbsp;
                <button type='button' className='button-std'
                        onClick={() => dispatchUpdateLayout(LO_STD_MODE.xyplot_table)}>xy-tbl</button>
            </div>
        </div>
    );
};

function generateLayout(results, layoutInfo) {
    const mode = get(layoutInfo, 'mode.standard', LO_STD_MODE.triview.mode.standard);
    if (mode === LO_STD_MODE.image_table.mode.standard) {
        return {east: {index: 0,  defaultSize: '50%'}, west: {index: 1} };
    } else if (mode === LO_STD_MODE.image_xyplot.mode.standard) {
        return {north: {index: 0,  defaultSize: '50%'}, south: {index: 1} };
    } else if (mode === LO_STD_MODE.xyplot_table.mode.standard) {
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
function resolveComponents(imagePlot, xyPlot, tables, layoutInfo) {
    const mode = get(layoutInfo, 'mode.standard', LO_STD_MODE.triview.mode.standard);
    if (mode === LO_STD_MODE.image_table.mode.standard) {
        return [imagePlot, tables];
    } else if (mode === LO_STD_MODE.image_xyplot.mode.standard) {
        return [imagePlot, xyPlot];
    } else if (mode === LO_STD_MODE.xyplot_table.mode.standard) {
        return [tables, xyPlot];
    } else {
        return [imagePlot, tables, xyPlot];
    }
}

