/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import DockLayoutPanel from './panel/DockLayoutPanel.jsx';
import appDataCntlr from '../core/AppDataCntlr.js';


function generateLayout(results, layoutInfo) {
    var layout = {};
    const mode = layoutInfo.mode || 'auto';
    if (mode === 'auto') {
        if ( results.length === 3 ) {
            layout = {  east: {index: 0}, west: {index: 1}, south: {index: 2, defaultSize: 'calc(100% - 300px'} };
        } else if (results.length === 2 ) {
            layout = {east: {index: 0,  defaultSize: '50%'}, west: {index: 1} };
        } else if ( results.length === 1 ) {
            layout = {center: {index: 0,  defaultSize: '50%'}, resize: false };
        }
    } else if ( mode === 'tri') {
        layout = {  east: {index: 0}, west: {index: 1}, south: {index: 2, defaultSize: 'calc(100% - 300px'} };
    } else if ( mode === 'sbs') {
        layout = {east: {index: 0,  defaultSize: '50%'}, west: {index: 1} };
    } else if ( mode === 'tb') {
        layout = {north: {index: 0,  defaultSize: '50%'}, south: {index: 1} };
    } else if ( mode === 'expand') {
        layout = {center: {index: 0} };
    }

    return layout;
}
function resolveComponents(imagePlot, xyPlot, tables, layoutInfo) {
    const views = layoutInfo.views || [];
    var components = [imagePlot, tables, xyPlot];
    if (views.length > 0) {
        components = views.reduce( (res, v) => {
            if (v === 'tables' && layoutInfo.hasTables && tables) {
                res.push(tables);
            } else if (v === 'images' && layoutInfo.hasImages && imagePlot) {
                res.push(imagePlot);
            } else if (v === 'xyPlots' && layoutInfo.hasXyPlots && xyPlot) {
                res.push(xyPlot);
            }

        }, [] );
    }
    return components;
}

function renderResults(imagePlot, xyPlot, tables, layoutInfo) {
    const components = resolveComponents(imagePlot, xyPlot, tables, layoutInfo);
    const layout = generateLayout(components, layoutInfo);


    return (
        <DockLayoutPanel layout={ layout } >
            {components}
        </DockLayoutPanel>
    );
}

function renderSearchDesc(searchDesc) {
    const SearchDesc = searchDesc && <div id='results-searchDesc' style={ {display: 'inline-block'} }>{searchDesc}</div>;
    return (
        <div>
            {SearchDesc || <div style={{fontSize: 'medium', display: 'inline-block'}}> >>Search description not provided..</div>}
            <div style={ {display: 'inline-block', float: 'right'} }>
                <button type='button' className='button-std' onClick={() => appDataCntlr.dispatchUpdateLayout( {mode: 'tri' })}>tri-view</button>&nbsp;
                <button type='button' className='button-std' onClick={() => appDataCntlr.dispatchUpdateLayout( {mode: 'sbs' })}>side-by-side</button>&nbsp;
                <button type='button' className='button-std' onClick={() => appDataCntlr.dispatchUpdateLayout( {mode: 'tb' })}>top-bottom</button>&nbsp;
                <button type='button' className='button-std' onClick={() => appDataCntlr.dispatchUpdateLayout( {mode: 'expand' })}>expanded</button>
            </div>
        </div>
    );
}
const ResultsPanel = function (props) {
    var {visToolbar,searchDesc,title, imagePlot, xyPlot, tables, layoutInfo} = props;
    const Title = title && (<h2 style={{textAlign: 'center'}}>{title}</h2>);
    const SearchDesc = renderSearchDesc(searchDesc);

    return (
        <div style={{ flex: 'auto', display: 'flex', flexFlow: 'column'}}>
            {visToolbar}
            {SearchDesc}
            {Title}
            {renderResults(imagePlot, xyPlot, tables, layoutInfo)}
        </div>
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


export default ResultsPanel;


