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
import {FormPanel} from '../../ui/FormPanel.jsx';
import {Tabs, Tab} from '../../ui/panel/TabPanel.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import Validate from '../../util/Validate.js';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {InputGroup} from '../../ui/InputGroup.jsx';
import {UploadPanel} from './LcViewer.jsx';
import {LcImageToolbar} from './LcImageToolbar.jsx';
import {LcPFOptionsPanel} from './LcPhaseFoldingPanel.jsx';
import {LcPeriodFindingPanel} from './PeriodogramOptionsPanel.jsx';


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
                    <div><DownloadButton/></div>
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





//*-------------------------------- TEST CODE FOR BACKGROUND MONITOR -------------------------------*/
import * as TblUtil from '../../tables/TableUtil.js';
import {dispatchPackage, doOnPackage} from '../../core/background/BackgroundCntlr.js';
import {SelectInfo} from '../../tables/SelectInfo.js';
import DialogRootContainer from '../../ui/DialogRootContainer.jsx';
import {dispatchShowDialog, dispatchHideDialog} from '../../core/ComponentCntlr.js';
import {PopupPanel} from '../../ui/PopupPanel.jsx';
import {dispatchAddSaga} from '../../core/MasterSaga.js';


const showDownloadDialog = (show=true, mask) => {
    const content= (
        <PopupPanel title={'Download Dialog'} >
            <DownloadDialog mask= {mask}/>
        </PopupPanel>
    );
    DialogRootContainer.defineDialog('Download Dialog', content);
    if (show) {
        dispatchShowDialog('Download Dialog');
    } else {
        dispatchHideDialog('Download Dialog');
    }
};

function DownloadDialog({mask=false}) {

    const onSearchSubmit = (request) => {
        const options = {};

        const searchParams = {
            no_args: 'this is just a test processor.  no parameter is needed'
        };
        const dlparams = {
            MaxBundleSize: 1024*1024*1024,
            FilePrefix: 'WISE_Files',
            BaseFileName: 'WISE_Files',
            DataSource: 'WISE images',
            Title: request.Title
        };
        const rowCount = request.rowCount || 2;
        const sreq = TblUtil.makeTblRequest(searchParams.id, 'search request', searchParams, options);
        const dreq = TblUtil.makeTblRequest('LightCurveFileGroupsProcessor', request.Title, dlparams);
        const selectionInfo = SelectInfo.newInstance({selectAll: true, rowCount}).toString();
        dispatchPackage(dreq, sreq, selectionInfo);
        showDownloadDialog(true, true);
        dispatchAddSaga(doOnPackage, {title: request.Title, callback:() => {
            showDownloadDialog(false);
        }});
    };

    const labelWidth = 150;
    return (
        <div style={{position: 'relative'}}>
            {mask && <div style={{width: '100%', height: '100%'}} className='loading-mask'/>}
            <FormPanel
                width='300px' height='100px'
                groupKey='LC_DOWNLOAD'
                onSubmit={(request) => onSearchSubmit(request)}
                onCancel={() => showDownloadDialog(false)}>
                <FieldGroup groupKey='LC_DOWNLOAD' keepState={true}>
                    <InputGroup labelWidth={150}>
                        <ValidationField
                            initialState= {{
                                       value: '2',
                                       validator: Validate.intRange.bind(null, 1, 248, 'Number of images'),
                                       groupKey: 'LC_DOWNLOAD',
                                       label : 'Number of images:'
                                           }}
                            fieldKey='rowCount'
                            labelWidth={labelWidth}/>
                        <ValidationField
                            initialState= {{
                                       value: 'A sample download',
                                       groupKey: 'LC_DOWNLOAD',
                                       label : 'Title for this download:'
                                           }}
                            fieldKey='Title'
                            labelWidth={labelWidth}/>
                    </InputGroup>
                </FieldGroup>
            </FormPanel>
        </div>
    );

}



const DownloadButton = ({}) => {
    return (
        <div>
            <button type='button'
                    className='button std hl'
                    onClick={showDownloadDialog}
                    title='Download lighcurve images'
            >Download</button>
        </div>
    );
};