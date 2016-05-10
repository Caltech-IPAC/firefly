/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {pick, filter} from 'lodash';

import DockLayoutPanel from './panel/DockLayoutPanel.jsx';
import {LO_EXPANDED, LO_STANDARD} from '../core/LayoutCntlr.js';
// import {deepDiff} from '../util/WebUtil.js';

const wrapperStyle = { flex: 'auto', display: 'flex', flexFlow: 'column', overflow: 'hidden'};
const eastWest = {east: {index: 0,  defaultSize: '50%'}, west: {index: 1} };
const northSouth = {north: {index: 0,  defaultSize: '50%'}, south: {index: 1} };
const triView = {east: {index: 0}, west: {index: 1}, south: {index: 2, defaultSize: 'calc(100% - 300px)'}};
const singleView = {center: {index: 0,  defaultSize: '100%',  resize: false}};


export class ResultsPanel extends Component {

    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    // componentDidUpdate(prevProps, prevState) {
    //     deepDiff({props: prevProps, state: prevState},
    //         {props: this.props, state: this.state},
    //         this.constructor.name);
    // }

    render() {
        const {expanded} = this.props;
        const expandedProps = pick(this.props, ['expanded','imagePlot','xyPlot','tables'] );
        return (
            expanded
                ? <ExpandedView key='res-exp-view' {...expandedProps} />
                : <StandardView key='res-std-view' {...this.props} />
        );
    }
}

ResultsPanel.propTypes = {
    visToolbar: PropTypes.element,
    searchDesc: PropTypes.element,
    title: PropTypes.string,
    expanded: PropTypes.string,
    standard: PropTypes.string,
    imagePlot: PropTypes.element,
    xyPlot: PropTypes.element,
    tables: PropTypes.element
};


const ExpandedView = ({expanded, imagePlot, xyPlot, tables}) => {
    const view = expanded === LO_EXPANDED.tables.view ? tables
                : expanded === LO_EXPANDED.xyPlots.view ? xyPlot
                : imagePlot;
    return (
        <div style={wrapperStyle}>
            {view}
        </div>
    );
};


const StandardView = ({visToolbar, title, searchDesc, standard, imagePlot, xyPlot, tables}) => {
    const components = resolveComponents(standard, imagePlot, xyPlot, tables);
    const config = generateLayout(standard, components);

    return (
        <div style={wrapperStyle}>
            {visToolbar}
            {searchDesc}
            {title && <h2 style={{textAlign: 'center'}}>{title}</h2>}
            <DockLayoutPanel key={standard} config={ config } >
                {components}
            </DockLayoutPanel>
        </div>
    );
};

function generateLayout(standard, results) {
    if ( results.length === 3 ) {
        if (standard === LO_STANDARD.image_table.view) {
            return eastWest;
        } else if (standard === LO_STANDARD.image_xyplot.view) {
            return northSouth;
        } else if (standard === LO_STANDARD.xyplot_table.view) {
            return northSouth;
        } else {
            return triView;
        }
    } else if (results.length === 2 ) {
        if (standard === LO_STANDARD.image_table.view) {
            return eastWest;
        } else {
            return northSouth;
        }
    } else {
        return singleView;
    }
}
function resolveComponents(standard, imagePlot, xyPlot, tables) {
    var components;
    if (standard === LO_STANDARD.image_table.view) {
        components = [imagePlot, tables];
    } else if (standard === LO_STANDARD.image_xyplot.view) {
        components = [imagePlot, xyPlot];
    } else if (standard === LO_STANDARD.xyplot_table.view) {
        components = [tables, xyPlot];
    } else {
        components = ([imagePlot, tables, xyPlot]);
    }
    return filter(components);  // only takes declared items
}

