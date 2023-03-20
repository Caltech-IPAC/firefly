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
import {ResultsPanel} from './ResultsPanel.jsx';
import {TablesContainer} from '../../tables/ui/TablesContainer.jsx';
import {ChartsContainer} from '../../charts/ui/ChartsContainer.jsx';
import {
    makeCoverageTab, makeFitsTab, makeMultiProductViewerTab, TriViewImageSection
} from '../../visualize/ui/TriViewImageSection.jsx';
import {AppInitLoadingMessage} from '../../ui/AppInitLoadingMessage.jsx';
import {getExpandedChartProps} from '../../charts/ChartsCntlr.js';
import {DEFAULT_PLOT2D_VIEWER_ID} from '../../visualize/MultiViewCntlr.js';

const stateKeys= ['title', 'mode', 'showTables', 'showImages', 'showXyPlots', 'images'];
const LEFT= 'LEFT';
const RIGHT= 'RIGHT';
const triViewKey= 'images | tables | xyplots';
const tblImgKey= 'tables | images';
const imgXyKey= 'images | xyplots';
const tblXyKey= 'tables | xyplots';

export const TriViewPanel= memo(( {showViewsSwitch=true, leftButtons, centerButtons, rightButtons,
                                      coverageSide=LEFT, initLoadingMessage, initLoadCompleted} ) => {
    const state= useStoreConnector(() => pick(getLayouInfo(), stateKeys));
    const {title, mode, showTables, showImages, showXyPlots, images={}} = state;
    const {expanded, standard, closeable} = mode ?? {};
    const content = {};
    const {showMeta, showFits, dataProductTableId, showCoverage} = images;
    const coverageRight= showCoverage && coverageSide===RIGHT;
    const coverageLeft= showCoverage && coverageSide===LEFT;
    const currLayoutMode= getLayouInfo()?.mode?.standard?.toString() ?? triViewKey;
    const imagesWithCharts= currLayoutMode===tblXyKey;

    if (initLoadingMessage && !initLoadCompleted) return (<AppInitLoadingMessage message={initLoadingMessage}/>);
    if (!showImages && !showXyPlots && !showTables) return <div/>;

    if (!imagesWithCharts && (showImages || coverageLeft)) {
        content.imagePlot = (<TriViewImageSection key='res-tri-img'
                                                  closeable={closeable}
                                                  coverageSide={coverageSide}
                                                  imageExpandedMode={expanded===LO_VIEW.images}
                                                  {...images}  />);
    }
    if (showXyPlots || coverageRight || imagesWithCharts) {
        content.rightSide= <RightSide {...{key:'rightSide', expanded,closeable,showXyPlots, showMeta, showFits,
            dataProductTableId, coverageRight, imagesWithCharts}}/>;
        if (expanded===LO_VIEW.xyPlots) content.xyPlot= content.rightSide;
    }
    if (showTables) {
        content.tables = (<TablesContainer key='res-tables'
                                           mode='both'
                                           closeable={closeable}
                                           expandedMode={expanded===LO_VIEW.tables}/>);
    }

    const isTriView = (showImages||coverageLeft) && (showXyPlots||coverageRight) && showTables;
    return (
        <ResultsPanel {...{key:'results', title,
                      searchDesc:searchDesc({showViewsSwitch, showImages, isTriView, leftButtons, centerButtons,
                          rightButtons, showCoverage:images.showCoverage, coverageSide, currLayoutMode}),
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


function RightSide({expanded, closeable, showXyPlots, showMeta, showFits, dataProductTableId, coverageRight, imagesWithCharts }) {
    const onTabSelect = (idx, id) => dispatchUpdateLayoutInfo({rightSide:{selectedTab:id}});
    const chartExpandedMode= expanded===LO_VIEW.xyPlots;
    const cov= imagesWithCharts || coverageRight;
    const meta= imagesWithCharts && showMeta;
    const fits= imagesWithCharts && showFits;
    const {expandedViewerId}= getExpandedChartProps();
    const xyPlot = (<ChartsContainer key='res-xyplots' closeable={closeable} expandedMode={chartExpandedMode}
                                     viewerId={chartExpandedMode ? expandedViewerId : DEFAULT_PLOT2D_VIEWER_ID}
                                     useOnlyChartsInViewer={chartExpandedMode && expandedViewerId!==DEFAULT_PLOT2D_VIEWER_ID}
                                     tbl_group='main' addDefaultChart={true}/>);
    if (chartExpandedMode  || (!coverageRight && !imagesWithCharts)) return xyPlot;


    const style= {height: '100%'};
    const defaultSelected= coverageRight ? 'coverage' : showXyPlots ? 'xyplot' : 'fits';
    const key= `${showXyPlots&&'xyplot'}-${cov&&'cov'}-${meta&&'meta'}-${fits&&'fits'}`;
    return(
        <Tabs {...{key, style, onTabSelect, defaultSelected, useFlex:true, resizable:true}}>
            {showXyPlots && (<Tab key='res-xyplots' name='Charts' removable={false} id='xyplot'>{xyPlot}</Tab>)}
            {cov && makeCoverageTab()}
            {meta && makeMultiProductViewerTab(dataProductTableId)}
            {fits && makeFitsTab()}
        </Tabs>
    );
}


function getCovSideOptions(currLayoutMode, showImages) {
    const make= (l,r) => [ {label:l, value:LEFT}, {label:r, value:RIGHT}];

    switch (currLayoutMode) {
        case triViewKey: return make('Left', showImages?'Right':'Coverage w/Charts');
        case tblImgKey: return make('Coverage showing', 'Coverage hidden');
        case imgXyKey: return make('Left', 'Right');
        case tblXyKey: return make('Coverage hidden', 'Coverage showing');
        default: return make('Coverage left', 'Coverage right');
    }
}


function searchDesc({showViewsSwitch, showImages, isTriView, showCoverage, leftButtons, centerButtons, rightButtons, coverageSide, currLayoutMode}) {

    const hasContent = showViewsSwitch || leftButtons || centerButtons || rightButtons;
    if (!hasContent) return  <div/>;

    const covSideOptions= getCovSideOptions(currLayoutMode, showImages);

    const options= [
        {label:'Tri-view', value:triViewKey},
        {label:'Bi-view Images', value:imgXyKey},
        {label:'Bi-view Tables', value:tblXyKey},
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
                        {showCoverage && currLayoutMode!==tblXyKey &&
                            <RadioGroupInputFieldView {...{
                                wrapperStyle:{display:'inline-flex', alignItems:'center'},
                                options:covSideOptions, value:coverageSide, labelWidth:60,
                                labelStyle:{fontSize:'larger'}, label:'Coverage: ',
                                buttonGroup:true, inline:true,
                                onChange:(ev) => dispatchUpdateLayoutInfo({coverageSide:ev.target.value}),
                            }} /> }
                        {isTriView &&
                            <RadioGroupInputFieldView
                                {...{
                                    wrapperStyle:{paddingLeft: 30, width:320, display:'inline-flex', alignItems:'center'},
                                    options, value:currLayoutMode, labelWidth:43,
                                    labelStyle:{fontSize:'larger'}, label:'Layout: ',
                                    buttonGroup:true, inline:true,
                                    onChange:(ev) => dispatchSetLayoutMode(LO_MODE.standard, ev.target.value),
                                }} />
                        }
                    </div>
                }
            </div>
        </div>
    );
}

