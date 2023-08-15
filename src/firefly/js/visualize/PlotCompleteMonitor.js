import {isString} from 'lodash';
import ImagePlotCntlr, {visRoot} from './ImagePlotCntlr';
import {dispatchAddActionWatcher} from '../core/MasterSaga';
import {getPlotViewById, primePlot} from './PlotViewUtil';


/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/**
 * return promise to a loaded PlotView
 * @param plotId
 * @param failureAsReject - if true the call reject otherwise just resolve with an undefined
 * @return {Promise<PlotView>}
 */
export function onPlotComplete(plotId, failureAsReject = false) {

    const failActions = [ImagePlotCntlr.ABORT_HIPS, ImagePlotCntlr.PLOT_HIPS_FAIL, ImagePlotCntlr.PLOT_IMAGE_FAIL];
    const succActions = [ImagePlotCntlr.PLOT_HIPS, ImagePlotCntlr.PLOT_IMAGE];
    const pv = plotId && getPlotViewById(visRoot(), plotId);
    if (pv && pv.serverCall !== 'working' && primePlot(pv) && pv.viewDim.width && pv.viewDim.height) {
        if (pv.serverCall === 'success') {
            return Promise.resolve(pv);
        } else {
            return failureAsReject ? Promise.reject(pv) : Promise.resolve(pv);
        }
    }

    return new Promise((resolve, reject) => {
        dispatchAddActionWatcher({
                actions: [...succActions, ...failActions, ImagePlotCntlr.UPDATE_VIEW_SIZE],
                callback: watchViewDim,
                params: {plotId, resolve, reject, failureAsReject, failActions, succActions}
            }
        );
    });
}

function watchViewDim(action, cancelSelf, {plotId, resolve, reject, failureAsReject, failActions, succActions, foundSuccComplete}) {
    if (!resolve) cancelSelf();
    if (action.payload.plotId !== plotId) return;
    const {type} = action;
    const vr = visRoot();
    const pv = getPlotViewById(vr, plotId);
    const {width=0, height=0} = pv?.viewDim ?? {};
    if (failActions.includes(type)) {
        failureAsReject ? reject(Error(action)) : resolve();
        cancelSelf();
        return;
    }
    if (succActions.includes(type)) foundSuccComplete = true;
    if (action.type===ImagePlotCntlr.UPDATE_VIEW_SIZE) foundSuccComplete = true;
    if (foundSuccComplete && width > 30 && height > 30) {
        resolve(pv);
        cancelSelf();
    }
    return {plotId, resolve, reject, failureAsReject, failActions, succActions, foundSuccComplete};
}


/**
 * return when the view dim is defined, or PlotView is deleted, or after 10 seconds
 * @param plotIdAryOrPlotId
 * @return {Promise}
 */
export async function onViewDimDefined(plotIdAryOrPlotId) {
    const ary=  (isString(plotIdAryOrPlotId)) ? [plotIdAryOrPlotId] : plotIdAryOrPlotId;
    return await Promise.all( ary.map( (plotId) => resolveViewDimPromise(plotId)));
}

function onViewDimDefinedWatcher(action, cancelSelf, {plotId, resolve, startTime}) {
    const {type, payload} = action;
    const pv = getPlotViewById(visRoot(), plotId);
    let resolvedViewDim;
    if ((payload.plotId !== plotId && type===ImagePlotCntlr.DELETE_PLOT_VIEW) || !pv) resolvedViewDim= {width:0,height:0};
    if (pv?.viewDim?.width>0 && pv?.viewDim?.height>0) resolvedViewDim= pv.viewDim;
    if (Date.now() > (startTime +10_000)) resolvedViewDim= {width:0,height:0};
    if (resolvedViewDim) {
        resolve(resolvedViewDim);
        cancelSelf();
    }
    return {plotId, resolve, startTime};
}

function resolveViewDimPromise(plotId) {
    const pv= getPlotViewById(visRoot(),plotId);
    if (pv?.viewDim.width>0 && pv?.viewDim.height>0 ) return Promise.resolve(pv.viewDim);
    return new Promise((resolve) => {
        dispatchAddActionWatcher({
            actions: [ImagePlotCntlr.UPDATE_VIEW_SIZE, ImagePlotCntlr.DELETE_PLOT_VIEW],
            callback: onViewDimDefinedWatcher, params: {plotId, resolve, startTime: Date.now()}
        } );
    });
}
