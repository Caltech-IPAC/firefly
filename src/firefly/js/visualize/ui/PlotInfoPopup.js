import {isHiPS, isImage} from 'firefly/visualize/WebPlot.js';
import {currentP, primePlot} from 'firefly/visualize/PlotViewUtil.js';
import {FITS_HEADER_POPUP_ID, fitsHeaderView} from 'firefly/visualize/ui/FitsHeaderView.jsx';
import {HIPS_PROPERTY_POPUP_ID, showHiPSPropertyView} from 'firefly/visualize/ui/HiPSPropertyView.jsx';
import ComponentCntlr, {dispatchHideDialog, isDialogVisible} from 'firefly/core/ComponentCntlr.js';
import {CHANGE_ACTIVE_PLOT_VIEW, CHANGE_PRIME_PLOT, DELETE_PLOT_VIEW, PLOT_HIPS, PLOT_IMAGE} from '../VisConst';
import {dispatchAddActionWatcher} from 'firefly/core/MasterSaga.js';


let initLeft= NaN;
let initTop= NaN;

function onMove({left,top}) {
    initLeft= left;
    initTop= top;
}

export function showPlotInfoPopup(pv, element) {

    showInfo(pv,element);
    dispatchAddActionWatcher(
        {
            id: 'hips-image-info-id',
            actions: [CHANGE_ACTIVE_PLOT_VIEW, CHANGE_PRIME_PLOT, PLOT_IMAGE, PLOT_HIPS,
                ComponentCntlr.HIDE_DIALOG, DELETE_PLOT_VIEW],
            callback:  watchActivePlotChange,
            params: {element}
        });
}

function showInfo(pv,element, left, top) {
    if (isImage(primePlot(pv))) fitsHeaderView(pv, element, left, top, onMove);
    else showHiPSPropertyView(pv, element, left, top, onMove);
}

const watchActivePlotChange = (action, cancelSelf, params) => {
    const {displayedPlotId, element, lastType} = params;
    let {hideExternal=true}= params;
    if (hideExternal && action.type===ComponentCntlr.HIDE_DIALOG && !isDialogVisible(FITS_HEADER_POPUP_ID) && !isDialogVisible(HIPS_PROPERTY_POPUP_ID)) {
        initLeft= NaN;
        initTop= NaN;
        cancelSelf();
        return;
    }
    else {
        hideExternal=true;
    }
    const {pv,plot:crtPlot}= currentP();

    if (action.type===PLOT_IMAGE || action.type===PLOT_HIPS || displayedPlotId!==crtPlot?.plotId ) {
        if (isHiPS(crtPlot) || lastType!==crtPlot?.plotType) {
            if (isDialogVisible(FITS_HEADER_POPUP_ID)) {
                dispatchHideDialog(FITS_HEADER_POPUP_ID);
                hideExternal= false;
            }
            if (isDialogVisible(HIPS_PROPERTY_POPUP_ID)) {
                dispatchHideDialog(HIPS_PROPERTY_POPUP_ID);
                hideExternal= false;
            }
            showInfo(pv,element, initLeft, initTop);
        }
    }
    return {displayedPlotId:pv?.plotId, lastType:crtPlot?.plotType, element, hideExternal};
};
