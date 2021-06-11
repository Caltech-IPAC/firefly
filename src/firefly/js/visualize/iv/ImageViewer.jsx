/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo, useEffect, useRef} from 'react';
import PropTypes from 'prop-types';
import {omit} from 'lodash';
import shallowequal from 'shallowequal';
import {getPlotViewById, getAllDrawLayersForPlot, pvEqualForRender} from '../PlotViewUtil.js';
import {ImageViewerView} from './ImageViewerDecorate.jsx';
import {visRoot, ExpandType} from '../ImagePlotCntlr.js';
import {extensionRoot} from '../../core/ExternalAccessCntlr.js';
import {MouseState, addImageMouseListener, lastMouseCtx} from '../VisMouseSync.js';
import {getPlotUIExtensionList} from '../../core/ExternalAccessUtils.js';
import {getTaskCount} from '../../core/AppDataCntlr.js';
import {useStoreConnector} from '../../ui/SimpleComponent';
import {dlRoot} from '../DrawLayerCntlr';


const visRootOmitList= ['plotViewAry','activePlotId', 'processedTiles', 'plotRequestDefaults'];

function hasVisRootChanged(plotId,newVisRoot,oldVisRoot) {
    if (newVisRoot===oldVisRoot) return false;

    if (newVisRoot.activePlotId!==oldVisRoot.activePlotId &&
        (newVisRoot.activePlotId===plotId || oldVisRoot.activePlotId===plotId)) {
        return true;
    }

    if (!pvEqualForRender(getPlotViewById(oldVisRoot,plotId),getPlotViewById(newVisRoot,plotId))) return true;
    return !shallowequal(omit(newVisRoot,visRootOmitList), omit(oldVisRoot,visRootOmitList));  // compare certain keys in visRoot
}

function hasStateChanged(plotId, newState,oldState) {
    if (!oldState) return true;
    if (newState===oldState) return false;
    if (newState?.plotView?.localZoomStart) return false;
    // if (!shallowequal(newState.drawLayersAry,oldState.drawLayersAry)) return true;
    if (newState.extRoot!==oldState.extRoot || newState.taskCount!==oldState.taskCount) return true;
    return hasVisRootChanged(plotId,newState.vr,oldState.vr);
}

/**
 * Get the current state from the redux store.
 * if old state is passed then do and nothing important has changed then return the oldState. Returning oldstate
 * will keep the component from updating unnecessarily
 * @param {string} plotId
 * @param {Object} oldState
 * @return {Object}
 */
function getStoreState(plotId, oldState) {
    const vr= visRoot();
    const newState= {
        vr,
        plotView: getPlotViewById(vr,plotId),
        drawLayersAry: getAllDrawLayersForPlot(dlRoot(),plotId),
        extRoot: extensionRoot(),
        taskCount: getTaskCount(plotId)
    };
    return hasStateChanged(plotId,newState,oldState) ? newState : oldState;
}


const TEN_SECONDS= 10000;

export const ImageViewer= memo( ({showWhenExpanded=false, plotId, handleInlineTools=true, inlineTitle, aboveTitle}) => {

    const {current:mouseRef} = useRef({mousePlotId:undefined});
    const [{plotView,vr,drawLayersAry,taskCount}] = useStoreConnector( (oldState) => getStoreState(plotId,oldState) );
    const {current:timeoutRef} = useRef({timeId:undefined});

    useEffect(() => {
        let alive= true;
        const removeListener= addImageMouseListener((mState) => {
            mouseRef.mousePlotId= mState.plotId;
            timeoutRef.timeId && clearTimeout(timeoutRef.timeId);
            timeoutRef.timeId= undefined;
            if (mState.mouseState!==MouseState.EXIT || mouseRef.mousePlotId!==plotId) return;
            timeoutRef.timeId= setTimeout(
                    () => {
                        timeoutRef.timeId= undefined;
                        if (alive && lastMouseCtx().plotId===plotId) mouseRef.mousePlotId= undefined;
                    }, TEN_SECONDS); // 10 seconds
        });
        return () => {
            alive= false;
            timeoutRef.timeId && clearTimeout(timeoutRef.timeId);
            removeListener();
        };
    }, [plotId]);


    if (!showWhenExpanded  && vr.expandedMode!==ExpandType.COLLAPSE) return false;
    if (!plotView) return false;

    return (
        <ImageViewerView plotView={plotView}
                         drawLayersAry={drawLayersAry}
                         visRoot={vr}
                         mousePlotId={mouseRef.mousePlotId}
                         handleInlineTools={handleInlineTools}
                         inlineTitle={inlineTitle}
                         aboveTitle={aboveTitle}
                         workingIcon= {taskCount>0}
                         extensionList={getPlotUIExtensionList(plotId)} />
    );
});

ImageViewer.displayName= 'ImageViewer';
ImageViewer.propTypes= {
    plotId : PropTypes.string.isRequired,
    showWhenExpanded : PropTypes.bool,
    handleInlineTools : PropTypes.bool,
    inlineTitle: PropTypes.bool,
    aboveTitle: PropTypes.bool,
};
