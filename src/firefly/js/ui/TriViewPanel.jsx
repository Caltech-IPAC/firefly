/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component} from 'react';
import sCompare from 'react-addons-shallow-compare';

import {pick} from 'lodash';
import {flux} from '../Firefly.js';
import {LO_VIEW, LO_MODE, getLayouInfo, dispatchSetLayoutMode} from '../core/LayoutCntlr.js';
import {ResultsPanel} from '../ui/ResultsPanel.jsx';
import {TablesContainer} from '../tables/ui/TablesContainer.jsx';
import {ChartsContainer} from '../visualize/ChartsContainer.jsx';
import {VisToolbar} from '../visualize/ui/VisToolbar.jsx';
import {TriViewImageSection} from '../visualize/ui/TriViewImageSection.jsx';

export class TriViewPanel extends Component {

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
        const nextState = pick(getLayouInfo(), ['title', 'mode', 'showTables', 'showImages', 'showXyPlots', 'images']);
        this.setState(nextState);
    }

    render() {
        const {title, mode, showTables, showImages, showXyPlots, images={}} = this.state;
        const {expanded, standard, closeable} = mode || {};
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
        const searchDesc = (showImages && showXyPlots && showTables) ?
            (<div>
                <div style={ {display: 'inline-block', float: 'right'} }>
                    <button type='button' className='button-std'
                            onClick={() => dispatchSetLayoutMode(LO_MODE.standard, LO_VIEW.get('tables | images | xyPlots'))}>tri-view</button>
                    <button type='button' className='button-std'
                            onClick={() => dispatchSetLayoutMode(LO_MODE.standard, LO_VIEW.get('tables | images'))}>img-tbl</button>
                    <button type='button' className='button-std'
                            onClick={() => dispatchSetLayoutMode(LO_MODE.standard, LO_VIEW.get('images | xyPlots'))}>img-xy</button>
                    <button type='button' className='button-std'
                            onClick={() => dispatchSetLayoutMode(LO_MODE.standard, LO_VIEW.get('tables | xyPlots'))}>xy-tbl</button>
                </div>
            </div>)
            : <div/>;


        if (showImages || showXyPlots || showTables) {
            return (
                <ResultsPanel key='results'
                              title={title}
                              searchDesc ={searchDesc}
                              expanded={expanded}
                              standard={standard}
                              visToolbar={visToolbar}
                    { ...content}
                />
            );
        } else {
            return <div/>;
        }
    }
}
