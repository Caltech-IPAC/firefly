/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Sheet} from '@mui/joy';
import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {pick} from 'lodash';
import DockLayoutPanel from '../../ui/panel/DockLayoutPanel.jsx';
import {LO_VIEW} from '../../core/LayoutCntlr.js';

const wrapperStyle = { flex: 'auto', display: 'flex', flexFlow: 'column', overflow: 'hidden'};
const eastWest = {east: {index: 0,  defaultSize: '50%'}, west: {index: 1} };
const northSouth = {north: {index: 0,  defaultSize: '50%'}, south: {index: 1} };
const triView = {east: {index: 0, defaultSize: '50%'}, west: {index: 1}, south: {index: 2, defaultSize: 'calc(100% - 300px)'}};
const singleView = {center: {index: 0,  defaultSize: '100%',  resize: false}};


export const ResultsPanel= memo((props) =>{
    const {expanded:expandedIn=LO_VIEW.none} = props;
    const expanded = LO_VIEW.get(expandedIn);
    const expandedProps = pick(props, ['expanded','imagePlot','xyPlot','tables'] );
    return (
        expanded === LO_VIEW.none
            ? <StandardView key='res-std-view' {...props} />
            : <ExpandedView key='res-exp-view' {...expandedProps} />
    );
});

ResultsPanel.propTypes = {
    visToolbar: PropTypes.element,
    searchDesc: PropTypes.element,
    title: PropTypes.string,
    expanded: PropTypes.oneOfType([
                PropTypes.string,
                PropTypes.object]),
    standard: PropTypes.oneOfType([
                PropTypes.string,
                PropTypes.object]),
    imagePlot: PropTypes.element,
    xyPlot: PropTypes.element,
    tables: PropTypes.element
};


const ExpandedView = ({expanded, imagePlot, xyPlot, tables}) => {
    const view = expanded === LO_VIEW.tables ? tables
        : expanded === LO_VIEW.xyPlots ? xyPlot : imagePlot;
    return (
        <Sheet style={wrapperStyle} className='ff-ResultsPanel-ExpandedView'>
            {view}
        </Sheet>
    );
};


const StandardView = ({visToolbar, title, searchDesc, standard, imagePlot, tables, rightSide, flip=false}) => {
    standard = LO_VIEW.get(standard) || LO_VIEW.none;
    const components = resolveComponents(standard, imagePlot, tables, rightSide, flip);
    const config = generateLayout(standard, components.length);

    return (
        <Sheet style={wrapperStyle} className='ff-ResultsPanel-StandardView'>
            {visToolbar}
            {searchDesc}
            {title && <h2 style={{textAlign: 'center'}}>{title}</h2>}
            <DockLayoutPanel key={standard} config={ config } >
                {components}
            </DockLayoutPanel>
        </Sheet>
    );
};

const getShow= (standard) => ( {
    showTables: LO_VIEW.tables.has(standard),
    showRightSide: LO_VIEW.xyPlots.has(standard),
    showLeftSide: LO_VIEW.images.has(standard),
});

function generateLayout(standard, componentCnt) {
    const {showTables,showRightSide, showLeftSide}= getShow(standard);

    if (componentCnt===3 && showLeftSide && showRightSide && showTables) return triView;

    if ( componentCnt === 3 ) {
        if ( showLeftSide && showTables) return eastWest;
        else if (showLeftSide && showRightSide) return northSouth;
        else if (showRightSide && showTables) return northSouth;
    } else if (componentCnt === 2 ) {
        return eastWest;
        // return (showLeftSide && showTables) ? eastWest : northSouth;
    } else {
        return singleView;
    }
}

function resolveComponents(standard, imagePlot, tables, rightSide,flip) {
    const {showTables,showRightSide, showLeftSide}= getShow(standard);
    if (showRightSide&&rightSide && showTables&&tables && !showLeftSide) {
        return flip ? [rightSide,tables] : [tables,rightSide];
    }
    return [showLeftSide&&imagePlot, showRightSide&&rightSide, showTables&&tables].filter( (v) => v);
}