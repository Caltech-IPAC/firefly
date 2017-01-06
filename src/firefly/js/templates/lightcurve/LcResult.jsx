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
import {MultiImageViewerContainer} from '../../visualize/ui/MultiImageViewerContainer.jsx';
import {createContentWrapper} from '../../ui/panel/DockLayoutPanel.jsx';
import {LC} from './LcManager.js';
import {Tabs, Tab} from '../../ui/panel/TabPanel.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {UploadPanel} from './LcViewer.jsx';
import {LcImageToolbar} from './LcImageToolbar.jsx';
import {LcPFOptionsPanel} from './LcPhaseFoldingPanel.jsx';
import {LcPeriodFindingPanel} from './PeriodogramOptionsPanel.jsx';
import {DownloadButton, DownloadOptionPanel} from '../../ui/DownloadDialog.jsx';


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
                                        viewerId={LC.IMG_VIEWER_ID}
                                        closeable={true}
                                        forceRowSize={1}
                                        imageExpandedMode={expanded===LO_VIEW.images}
                                        Toolbar={LcImageToolbar}
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
                    <div style={{height:'100%'}}>
                        <Tabs componentKey='OuterTabs' defaultSelected={0} useFlex={true}>

                            <Tab name='Phase Folding'>
                                <div>
                                    {LcPFOptionsPanel(fields)}
                                </div>
                            </Tab>
                            <Tab name='Periodogram'>
                                <div>
                                    <LcPeriodFindingPanel />
                                </div>
                            </Tab>
                            <Tab name='Upload/Phase Folding'>
                                <div>
                                    <UploadPanel />
                                </div>
                            </Tab>
                        </Tabs>
                    </div>);
 /*
            content.form = (
                <div>
                    <div>
                        <Tabs componentKey='OuterTabs' defaultSelected={0} useFlex={true}>
                            <Tab name='Periodogram'>
                                <div>
                                    <LcPeriodFindingPanel />
                                </div>
                            </Tab>
                            <Tab name='Upload/Phase Folding'>
                                <div>
                                    <UploadPanel phaseButton={true}/>
                                </div>
                            </Tab>
                        </Tabs>
                    </div>
                </div> );
*/
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
        <div style={{ flex: 'auto', display: 'flex', flexFlow: 'column', overflow: 'hidden'}}>{view}</div>
    );
};


// eslint-disable-next-line
const StandardView = ({visToolbar, title, searchDesc, standard, imagePlot, xyPlot, tables, form}) => {

    return (
        <div style={{display: 'flex', flexDirection: 'column', flexGrow: 1, position: 'relative'}}>
            { visToolbar &&
                <div style={{display: 'inline-flex', justifyContent: 'space-between', alignItems: 'center'}}>
                    <div>{visToolbar}</div>
                    <div>
                        <DownloadButton>
                            <DownloadOptionPanel
                                cutoutSize = '200'
                                dlParams = {{
                                    MaxBundleSize: 200*1024*1024,    // set it to 200mb to make it easier to test multi-parts download.  each wise image is ~64mb
                                    FilePrefix: 'WISE_Files',
                                    BaseFileName: 'WISE_Files',
                                    DataSource: 'WISE images',
                                    FileGroupProcessor: 'LightCurveFileGroupsProcessor'
                                }}>
                                <ValidationField
                                        initialState= {{
                                               value: 'A sample download',
                                               label : 'Title for this download:'
                                                   }}
                                        fieldKey='Title'
                                        labelWidth={110}/>
                            </DownloadOptionPanel>
                        </DownloadButton>
                    </div>
                </div>
            }
            {searchDesc}
            {title && <h2 style={{textAlign: 'center'}}>{title}</h2>}
            <div style={{flexGrow: 1, position: 'relative'}}>
            <div style={{position: 'absolute', top: 0, right: 0, bottom: 0, left: 0}}>
                <SplitPane split='vertical' minSize={20}  defaultSize={435}>
                    <SplitPane split='horizontal' minSize={20} defaultSize={435}>
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

