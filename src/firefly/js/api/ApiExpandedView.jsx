/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import {flux} from '../Firefly.js';
import {TablesContainer} from '../tables/ui/TablesContainer.jsx';
import {ChartsContainer} from '../charts/ui/ChartsContainer.jsx';
import {ApiExpandedDisplay} from '../visualize/ui/ApiExpandedDisplay.jsx';
import {dispatchChangeExpandedMode, ExpandType} from '../visualize/ImagePlotCntlr.js';
import {dispatchSetLayoutMode, getExpandedMode, LO_MODE, LO_VIEW} from '../core/LayoutCntlr.js';
import {getExpandedChartProps} from '../charts/ChartsCntlr.js';


export class ApiExpandedView extends PureComponent {

    constructor(props) {
        super(props);
        const closeFunc = () => {
            dispatchChangeExpandedMode(ExpandType.COLLAPSE);
            dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none);
        };
        const images  = (<ApiExpandedDisplay closeFunc={closeFunc}/>);
        const tables  = (<TablesContainer  mode='expanded' />);
        this.state = {images, tables};
    }

    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.removeListener && this.removeListener();
        this.isUnmounted = true;
    }

    storeUpdate() {
        if (!this.isUnmounted) {
            const expanded = getExpandedMode();
            this.setState({expanded});
        }
    }

    // componentDidUpdate(prevProps, prevState) {
    //     deepDiff({props: prevProps, state: prevState},
    //         {props: this.props, state: this.state},
    //         this.constructor.name);
    // }

    render() {
        var {expanded, images, tables} = this.state;
        expanded = LO_VIEW.get(expanded) || LO_VIEW.none;
        const {chartId} = expanded === LO_VIEW.xyPlots ? getExpandedChartProps() : {};
        const view = expanded === LO_VIEW.tables ? tables
            : expanded === LO_VIEW.xyPlots ? (<ChartsContainer key='api' expandedMode={true} closeable={true} chartId={chartId}/>)
            : images;
        if (expanded === LO_VIEW.none) {
            document.body.style.overflow= this.saveOverflow;
            return false;
        }
        else {
            this.saveOverflow= document.body.style.overflow;
            document.body.style.overflow= 'hidden';
            return (
                <div className='api-expanded rootStyle'>
                    {view}
                </div>
            );

        }
    }
}

