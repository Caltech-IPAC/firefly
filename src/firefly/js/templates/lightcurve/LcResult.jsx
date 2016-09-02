/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {pick} from 'lodash';
import SplitPane from 'react-split-pane';

import {flux} from '../../Firefly.js';
import {LO_VIEW, getLayouInfo} from '../../core/LayoutCntlr.js';
import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';
import {ChartsContainer} from '../../charts/ui/ChartsContainer.jsx';
import {VisToolbar} from '../../visualize/ui/VisToolbar.jsx';
import {TriViewImageSection} from '../../visualize/ui/TriViewImageSection.jsx';
import {createContentWrapper} from '../../ui/panel/DockLayoutPanel.jsx';

export class LcResult extends Component {

    constructor(props) {
        super(props);
        this.state = {};
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    componentDidMount() {
        this.removeListener = flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.removeListener && this.removeListener();
    }

    storeUpdate() {
        const nextState = pick(getLayouInfo(), ['title', 'mode', 'showTables', 'showImages', 'showXyPlots', 'showForm', 'images']);
        this.setState(nextState);
    }

    render() {
        const {title, mode, showTables, showImages, showXyPlots, showForm=true, searchDesc, images} = this.state;
        var {expanded, standard, closeable} = mode || {};
        const content = {};
        var visToolbar;
        if (showImages) {
            visToolbar = <VisToolbar key='res-vis-tb'/>;
            content.imagePlot = (<TriViewImageSection key='res-tri-img'
                                        closeable={closeable}
                                        imageExpandedMode={expanded===LO_VIEW.images}
                                        {...images}  />);
        }
        if (showXyPlots) {
            content.xyPlot = (<ChartsContainer key='res-xyplots'
                                        closeable={closeable}
                                        expandedMode={expanded===LO_VIEW.xyPlots}/>);
        }
        if (showTables) {
            content.tables = (<TablesContainer key='res-tables'
                                        mode='both'
                                        closeable={closeable}
                                        expandedMode={expanded===LO_VIEW.tables}/>);
        }
        if (showForm) {
            content.form = (<div>Put your input fields here!</div>);
        }

        expanded = LO_VIEW.get(expanded);
        const expandedProps =  {expanded, ...content};
        const standardProps =  {visToolbar, title, searchDesc, standard, ...content};
        
        return (
            expanded === LO_VIEW.none
                ? <StandardView key='res-std-view' {...standardProps} />
                : <ExpandedView key='res-exp-view' {...expandedProps} />
        );
    }
}


// eslint-disable-next-line
const ExpandedView = ({expanded, imagePlot, xyPlot, tables}) => {
    const view = expanded === LO_VIEW.tables ? tables
        : expanded === LO_VIEW.xyPlots ? xyPlot
        : imagePlot;
    return (
        <div style={{width: '100%'}}>{view}</div>
    );
};


// eslint-disable-next-line
const StandardView = ({visToolbar, title, searchDesc, standard, imagePlot, xyPlot, tables, form}) => {

    return (
        <div style={{display: 'flex', flexDirection: 'column', flexGrow: 1, position: 'relative'}}>
            {visToolbar}
            {searchDesc}
            {title && <h2 style={{textAlign: 'center'}}>{title}</h2>}
            <div style={{flexGrow: 1, position: 'relative'}}>
            <div style={{position: 'absolute', top: 0, right: 0, bottom: 0, left: 0}}>
                <SplitPane split='vertical' minSize={20} defaultSize={435}>
                    <SplitPane split='horizontal' minSize={20} defaultSize={300}>
                        {createContentWrapper(form)}
                        {createContentWrapper(tables)}
                    </SplitPane>
                    <SplitPane split='horizontal' minSize={20}>
                        {createContentWrapper(xyPlot)}
                        {createContentWrapper(imagePlot)}
                    </SplitPane>
                </SplitPane>
            </div>
            </div>
        </div>
    );
};
