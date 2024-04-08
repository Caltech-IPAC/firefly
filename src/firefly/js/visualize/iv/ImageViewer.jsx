/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo, useState, useEffect, useRef, useDeferredValue} from 'react';
import PropTypes from 'prop-types';
import {omit} from 'lodash';
import shallowequal from 'shallowequal';
import {getPlotViewById,getAllDrawLayersForPlot} from '../PlotViewUtil.js';
import {ImageViewerView} from './ImageViewerDecorate.jsx';
import {visRoot, ExpandType} from '../ImagePlotCntlr.js';
import {extensionRoot} from '../../core/ExternalAccessCntlr.js';
import {MouseState, addImageMouseListener, lastMouseCtx} from '../VisMouseSync.js';
import {getPlotUIExtensionList} from '../../core/ExternalAccessUtils.js';
import {getTaskCount} from '../../core/AppDataCntlr.js';
import {useStoreConnector} from '../../ui/SimpleComponent';
import {dlRoot} from '../DrawLayerCntlr';


const omitList= ['plotViewAry','activePlotId'];

function hasVisRootChanged(plotId,newVisRoot,oldVisRoot) {
    if (newVisRoot===oldVisRoot) return false;

    if (newVisRoot.activePlotId!==oldVisRoot.activePlotId &&
        (newVisRoot.activePlotId===plotId || oldVisRoot.activePlotId===plotId)) {
        return true;
    }
    if (getPlotViewById(newVisRoot,plotId)!==getPlotViewById(oldVisRoot,plotId)) return true;
    return !shallowequal(omit(newVisRoot,omitList), omit(oldVisRoot,omitList));  // compare certain keys in visRoot
}

function hasStateChanged(plotId, newState,oldState) {
    if (!oldState) return true;
    if (newState===oldState) return false;
    if (!shallowequal(newState.drawLayersAry,oldState.drawLayersAry)) return true;
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

export const ImageViewer= memo( ({showWhenExpanded=false, plotId, makeToolbar}) => {

    const [mousePlotId, setMousePlotId] = useState(lastMouseCtx().plotId);
    const {plotView,vr,drawLayersAry,taskCount} = useStoreConnector( (oldState) => getStoreState(plotId,oldState) );
    const {current:timeoutRef} = useRef({timeId:undefined});

    const deferredDrawLayersAry= useDeferredValue(drawLayersAry);
    const deferredTaskCount= useDeferredValue(taskCount);
    const deferredMousePlotId= useDeferredValue(mousePlotId);
    // const deferredDrawLayersAry= drawLayersAry;
    // const deferredTaskCount= taskCount;
    // const deferredMousePlotId= mousePlotId;

    useEffect(() => {
        const removeListener= addImageMouseListener((mState) => {
            setMousePlotId(mState.plotId);
            timeoutRef.timeId && clearTimeout(timeoutRef.timeId);
            timeoutRef.timeId= undefined;
            if (mState.mouseState!==MouseState.EXIT || mousePlotId!==plotId) return;
            timeoutRef.timeId= setTimeout(
                    () => {
                        timeoutRef.timeId= undefined;
                        if (lastMouseCtx().plotId===plotId) setMousePlotId(undefined);
                    }, TEN_SECONDS); // 10 seconds
        });
        return () => {
            timeoutRef.timeId && clearTimeout(timeoutRef.timeId);
            removeListener();
        };
    }, [mousePlotId, plotId]);


    if (!showWhenExpanded  && vr.expandedMode!==ExpandType.COLLAPSE) return false;
    if (!plotView) return false;


    return (
        <ImageViewerView {...{plotView,
                         makeToolbar,
                         visRoot:vr,
                         drawLayersAry: deferredDrawLayersAry,
                         mousePlotId: deferredMousePlotId,
                         workingIcon: deferredTaskCount>0,
                         extensionList: getPlotUIExtensionList(plotId)}} />
    );
});

ImageViewer.displayName= 'ImageViewer';
ImageViewer.propTypes= {
    plotId : PropTypes.string.isRequired,
    showWhenExpanded : PropTypes.bool,
};
