/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import {ExpandedModeDisplay} from '../iv/ExpandedModeDisplay.jsx';
import {isEmpty} from 'lodash';
import {Tab, Tabs, FieldGroupTabs} from '../../ui/panel/TabPanel.jsx';
import sCompare from 'react-addons-shallow-compare';
import {flux} from '../../Firefly.js';
import {dispatchAddViewer, getMultiViewRoot, getViewer, getLayoutType} from '../MultiViewCntlr.js';
import {MultiViewStandardToolbar} from './MultiViewStandardToolbar.jsx';
import {ImageMetaDataToolbar} from './ImageMetaDataToolbar.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {MultiImageViewer} from './MultiImageViewer.jsx';
import {visRoot} from '../ImagePlotCntlr.js';
import {watchImageMetaData} from '../saga/ImageMetaDataWatcher.js';
import {dispatchAddSaga} from '../../core/MasterSaga.js';
import {LO_MODE, LO_VIEW, dispatchSetLayoutMode} from '../../core/LayoutCntlr.js';



/**
 * This component works with ImageMetaDataWatch sega which should be launch during initialization
 * @param showCoverage
 * @param showFits
 * @param showImageMetaData
 * @param imageExpandedMode if true, then imageExpandedMode overrides everything else
 * @param closeable expanded mode should have a close button
 * @return {XML}
 * @constructor
 */
export function TriViewImageSection({showCoverage=true, showFits=true, 
                                     showImageMetaData=true, imageExpandedMode=false, closeable=true}) {
    
    if (imageExpandedMode) {
        return <ExpandedModeDisplay   {...{key:'results-plots-expanded',
                                            forceExpandedMode:true,
                                            closeFunc:closeable?closeExpanded:null}}/>;
    }

    if (showCoverage && showFits && showImageMetaData) {
        return (
                <FieldGroup groupKey='TRI_VIEW_SELECTION' keepState={true}
                            style={{display:'flex', position:'absolute',
                                   left:0, right:0, top:0, bottom:0}} >
                    <FieldGroupTabs defaultSelected={0} initialState= {{ value:'image' }}>
                        <Tab name='Fits Data' removable={false} id='image'>
                            <MultiImageViewer viewerId='triViewImages'
                                              insideFlex={true}
                                              canReceiveNewPlots={true}
                                              canDelete={true}
                                              Toolbar={MultiViewStandardToolbar}/>
                        </Tab>
                        <Tab name='Image Meta Data' removable={false} id='meta'>
                            <MultiImageViewer viewerId='triViewImageMetaData'
                                              insideFlex={true}
                                              canReceiveNewPlots={false}
                                              canDelete={false}
                                              Toolbar={ImageMetaDataToolbar}/>
                        </Tab>
                        <Tab name='Coverage' removable={false} id='cov'>
                            <div style={{padding:10}}>TODO: Coverage Here</div>
                        </Tab>
                    </FieldGroupTabs>
                </FieldGroup>
        );

    }
    else {
        return <div>todo</div>;
    }
}


function closeExpanded() {
    dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none);
}

export function launchImageMetaDataSega() {
    dispatchAddSaga(watchImageMetaData,{viewerId:'triViewImageMetaData'});
}

TriViewImageSection.propTypes= {
    showCoverage : PropTypes.bool,
    showFits : PropTypes.bool,
    showImageMetaData : PropTypes.bool,
    imageExpandedMode : PropTypes.bool,
    closeable: PropTypes.bool
};
