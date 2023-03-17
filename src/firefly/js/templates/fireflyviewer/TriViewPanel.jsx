/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {pick} from 'lodash';
import {
    LO_VIEW, LO_MODE, getLayouInfo, dispatchSetLayoutMode, dispatchUpdateLayoutInfo
} from '../../core/LayoutCntlr.js';
import {Tab, Tabs} from '../../ui/panel/TabPanel.jsx';
import {RadioGroupInputFieldView} from '../../ui/RadioGroupInputFieldView.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {CoverageViewer} from '../../visualize/ui/CoveraeViewer.jsx';
import {ResultsPanel} from './ResultsPanel.jsx';
import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';
import {ChartsContainer} from '../../charts/ui/ChartsContainer.jsx';
import {TriViewImageSection} from '../../visualize/ui/TriViewImageSection.jsx';
import {AppInitLoadingMessage} from '../../ui/AppInitLoadingMessage.jsx';
import {getExpandedChartProps} from '../../charts/ChartsCntlr.js';
import {DEFAULT_PLOT2D_VIEWER_ID} from '../../visualize/MultiViewCntlr.js';

const stateKeys= ['title', 'mode', 'showTables', 'showImages', 'showXyPlots', 'images'];
const LEFT= 'LEFT';
const RIGHT= 'RIGHT';

export const TriViewPanel= memo(( {showViewsSwitch=true, leftButtons, centerButtons, rightButtons,
                                      coverageSide=LEFT, initLoadingMessage, initLoadCompleted} ) => {
    const state= useStoreConnector(() => pick(getLayouInfo(), stateKeys));
    const {title, mode, showTables, showImages, showXyPlots, images={}} = state;
    const {expanded, standard, closeable} = mode ?? {};
    const content = {};
    const coverageRight= images.showCoverage && coverageSide===RIGHT;
    const coverageLeft= images.showCoverage && coverageSide===LEFT;

    const onTabSelect = (idx, id) => dispatchUpdateLayoutInfo({rightSide:{selectedTab:id}});

    if (initLoadingMessage && !initLoadCompleted) return (<AppInitLoadingMessage message={initLoadingMessage}/>);

    if (showImages || coverageLeft) {
        content.imagePlot = (<TriViewImageSection key='res-tri-img'
                                                  closeable={closeable}
                                                  coverageSide={coverageSide}
                                                  imageExpandedMode={expanded===LO_VIEW.images}
                                                  {...images}  />);
    }
    if (showXyPlots || coverageRight) {
        const chartExpandedMode= expanded===LO_VIEW.xyPlots;
        const {expandedViewerId}= getExpandedChartProps();
        const xyPlot = (<ChartsContainer key='res-xyplots' closeable={closeable} expandedMode={chartExpandedMode}
                                         viewerId={chartExpandedMode ? expandedViewerId : DEFAULT_PLOT2D_VIEWER_ID}
                                         useOnlyChartsInViewer={chartExpandedMode && expandedViewerId!==DEFAULT_PLOT2D_VIEWER_ID}
                                         tbl_group='main' addDefaultChart={true}/>);
        if (showXyPlots) content.xyPlot= xyPlot;
        if (coverageRight) {
            content.rightSide= (
                <Tabs key='res-right' style={{height: '100%'}}
                      onTabSelect={onTabSelect} defaultSelected={'coverage'}
                      useFlex={true} resizable={true}>
                    {showXyPlots && <Tab name='Charts' removable={false} id='xyplot'>
                        {xyPlot}
                    </Tab>}
                    <Tab name='Coverage' removable={false} id='coverage'>
                        <CoverageViewer/>
                    </Tab>
                </Tabs>
            );
        }
        else if (showXyPlots) {
            content.rightSide= xyPlot;
        }
    }
    if (showTables) {
        content.tables = (<TablesContainer key='res-tables'
                                           mode='both'
                                           closeable={closeable}
                                           expandedMode={expanded===LO_VIEW.tables}/>);
    }
    if (!showImages && !showXyPlots && !showTables) return <div/>;

    const isTriView = (showImages||coverageLeft) && (showXyPlots||coverageRight) && showTables;
    return (
        <ResultsPanel {...{key:'results', title,
                      searchDesc:searchDesc({showViewsSwitch, showImages, isTriView, leftButtons, centerButtons,
                          rightButtons, showCoverage:images.showCoverage, coverageSide}),
                      expanded, standard}}
                      { ...content} />
    );
});

TriViewPanel.propTypes = {
    showViewsSwitch: PropTypes.bool,
    leftButtons: PropTypes.arrayOf( PropTypes.func ),
    centerButtons: PropTypes.arrayOf( PropTypes.func ),
    rightButtons: PropTypes.arrayOf( PropTypes.func ),
    initLoadingMessage:  PropTypes.string,
    coverageSide:  PropTypes.string,
    initLoadCompleted:  PropTypes.bool,
};


const triViewKey= 'images | tables | xyplots';
const tblImgKey= 'tables | images';
const imgXyKey= 'images | xyplots';
const tblXyKey= 'tables | xyplots';


function getCovSideOptions(currLayoutMode, showImages) {
    const make= (l,r) => [ {label:l, value:LEFT}, {label:r, value:RIGHT}]

    switch (currLayoutMode) {
        case triViewKey: return make('Coverage left', showImages?'Coverage right':'Coverage w/Charts');
        case tblImgKey: return make('Coverage showing', 'Coverage hidden');
        case imgXyKey: return make('Coverage top', showImages? 'Coverage bottom':'Coverage w/Charts');
        case tblXyKey: return make('Coverage hidden', 'Coverage showing');
        default: return make('Coverage left', 'Coverage right');
    }
}


function searchDesc({showViewsSwitch, showImages, isTriView, showCoverage, leftButtons, centerButtons, rightButtons, coverageSide}) {

    const hasContent = showViewsSwitch || leftButtons || centerButtons || rightButtons;
    if (!hasContent) return  <div/>;
    const rightStr= coverageSide===RIGHT ? 'xy/cov' : 'xy';

    const currLayoutMode= getLayouInfo()?.mode?.standard?.toString() ?? triViewKey;
    const covSideOptions= getCovSideOptions(currLayoutMode, showImages);

    const options= [
        {label:'tri-view', value:triViewKey},
        {label:'img-tbl', value:tblImgKey},
        {label:`img-${rightStr}`, value:imgXyKey},
        {label:`${rightStr}-tbl`, value:tblXyKey},
    ];

    return (
        <div style={{display: 'inline-flex', justifyContent: 'space-between', marginBottom:coverageSide===RIGHT ? -1 : -1}}>
            <div>
                {leftButtons?.map( (el) => el()) }
            </div>
            <div>
                {centerButtons?.map( (el) => el()) }
            </div>
            <div style={{display: 'inline-flex'}}>
                {rightButtons?.map( (el) => el()) }
                <div style={{width: 20}}/>
                {showViewsSwitch &&
                    <div style={ {display: 'inline-block', float: 'right'} }>
                        {showCoverage && <RadioGroupInputFieldView
                            {...{
                                options:covSideOptions, value:coverageSide, labelWidth:0, buttonGroup:true, inline:true,
                                onChange:(ev) => dispatchUpdateLayoutInfo({coverageSide:ev.target.value}),
                            }} /> }
                        {isTriView &&
                            <RadioGroupInputFieldView
                                {...{
                                    wrapperStyle:{paddingLeft: 30, width:270},
                                    options, value:currLayoutMode, labelWidth:0, buttonGroup:true, inline:true,
                                    onChange:(ev) => dispatchSetLayoutMode(LO_MODE.standard, ev.target.value),
                                }} />
                        }
                    </div>
                }
            </div>
        </div>
    );
}

