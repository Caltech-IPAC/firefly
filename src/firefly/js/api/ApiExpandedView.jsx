/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component} from 'react';
import sCompare from 'react-addons-shallow-compare';

import {flux} from '../Firefly.js';
import {TablesContainer} from '../tables/ui/TablesContainer.jsx';
import {ChartsContainer} from '../charts/ui/ChartsContainer.jsx';
import {ApiExpandedDisplay} from '../visualize/ui/ApiExpandedDisplay.jsx';
import {dispatchChangeExpandedMode, ExpandType} from '../visualize/ImagePlotCntlr.js';
import {dispatchSetLayoutMode, getExpandedMode, LO_MODE, LO_VIEW} from '../core/LayoutCntlr.js';

// import {deepDiff} from '../util/WebUtil.js';

export class ApiExpandedView extends Component {

    constructor(props) {
        super(props);
        const closeFunc = () => {
            dispatchChangeExpandedMode(ExpandType.COLLAPSE);
            dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none);
        };
        const images  = (<ApiExpandedDisplay closeFunc={closeFunc}/>);
        const xyPlots = (<ChartsContainer expandedMode={true} closeable={true}/>);
        const tables  = (<TablesContainer  mode='expanded' />);
        this.state = {images, xyPlots, tables};
    }

    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.removeListener && this.removeListener();
        this.isUnmounted = true;
    }

    shouldComponentUpdate(nProps, nState) {
        return sCompare(this, nProps, nState);
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
        var {expanded, images, xyPlots, tables} = this.state;
        expanded = LO_VIEW.get(expanded) || LO_VIEW.none;
        const view = expanded === LO_VIEW.tables ? tables
            : expanded === LO_VIEW.xyPlots ? xyPlots
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

