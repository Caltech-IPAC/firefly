/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {pick,get} from 'lodash';
import SplitPane from 'react-split-pane';

import ColValuesStatistics from '../../charts/ColValuesStatistics.js';

import {flux} from '../../Firefly.js';
import {LO_VIEW, getLayouInfo} from '../../core/LayoutCntlr.js';
import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';
import {ChartsContainer} from '../../charts/ui/ChartsContainer.jsx';
import {VisToolbar} from '../../visualize/ui/VisToolbar.jsx';
import {MultiImageViewerContainer} from '../../visualize/ui/MultiImageViewerContainer.jsx';
import {createContentWrapper} from '../../ui/panel/DockLayoutPanel.jsx';
import {IMG_VIEWER_ID} from './LcManager.js';
import {FormPanel} from '../../ui/FormPanel.jsx';
import {Tabs, Tab,FieldGroupTabs} from '../../ui/panel/TabPanel.jsx';
import {CollapsiblePanel} from '../../ui/panel/CollapsiblePanel.jsx';
import {CheckboxGroupInputField} from '../../ui/CheckboxGroupInputField.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import Validate from '../../util/Validate.js';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {InputGroup} from '../../ui/InputGroup.jsx';
import {UploadPanel} from './LcViewer.jsx';
import {showLcParamForm, LcPFOptionsPanel} from './LcPhaseFoldingPanel.jsx';
import {LCPFOPanel} from './PeriodogramOptionsPanel.jsx';
import {LcPlotOptionsPanel} from './LcPlotOptions.jsx';

const PanelResizableStyle = {
    width: 400,
    minWidth: 450,
    height: 300,
    minHeight: 760,
    resize: 'both',
    position: 'relative',
    backgroundColor: '#e6ffff',
    overflow: 'auto'
};


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
        var {expanded, standard} = mode || {};
        const content = {};
        var visToolbar;
        if (showImages) {
            visToolbar = <VisToolbar key='res-vis-tb'/>;
            content.imagePlot = (<MultiImageViewerContainer key='res-images'
                                        viewerId={IMG_VIEWER_ID}
                                        closeable={true}
                                        imageExpandedMode={expanded===LO_VIEW.images}
                                        {...images}  />);
        }
        if (showXyPlots) {
            content.xyPlot = (<ChartsContainer key='res-charts'
                                        closeable={true}
                                        expandedMode={expanded===LO_VIEW.xyPlots}/>);
        }
        if (showTables) {
            content.tables = (<TablesContainer key='res-tables'
                                        mode='both'
                                        closeable={true}
                                        expandedMode={expanded===LO_VIEW.tables}/>);
        }
        if (showForm) {
            const fields= this.state;
            content.form = (
                <div>
                    <div>
                        <Tabs componentKey='OuterTabs' defaultSelected={0} useFlex={true}>
                            <Tab name="Phase Folding">
                                <div>
                                    {LcPFOptionsPanel(fields)}
                                </div>
                            </Tab>
                            <Tab name="Periodogram">
                                <div>
                                   <LCPFOPanel />
                                </div>
                            </Tab>
                            <Tab name='Upload'>
                                <div>
                                    <UploadPanel />
                                </div>
                            </Tab>
                        </Tabs>
                    </div>
                </div> );


        }

        expanded = LO_VIEW.get(expanded) || LO_VIEW.none;
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
                <SplitPane split='vertical' minSize={20}  defaultSize={435}>
                    <SplitPane split='horizontal' minSize={20} defaultSize={300}>
                        {createContentWrapper(form)}
                        {createContentWrapper(tables)}
                    </SplitPane>
                    <SplitPane split='horizontal' minSize={20} defaultSize={435}>
                        {createContentWrapper(xyPlot)}
                        {createContentWrapper(imagePlot)}
                    </SplitPane>
                </SplitPane>
            </div>
            </div>
        </div>
    );
};
