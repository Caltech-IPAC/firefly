import {dispatchHideDialog} from '../../../core/ComponentCntlr';
import {dispatchCancelActionWatcher} from '../../../core/MasterSaga';
import ExtractHiPSTileTool from '../../../drawingLayers/ExtractHiPSTileTool';
import ExtractLineTool from '../../../drawingLayers/ExtractLineTool';
import ExtractPointsTool from '../../../drawingLayers/ExtractPointsTool';
import {dispatchDestroyDrawLayer, dispatchDetachLayerFromPlot} from '../../DrawLayerDispatch';
import {dispatchAttributeChange, dispatchChangePointSelection} from '../../ImagePlotDispatch';
import {PlotAttribute} from '../../PlotAttribute';
import {currentP, getPlotViewAry, primePlot} from '../../PlotViewUtil';
import {visRoot} from '../../VisStoreRoots';
import {isHiPS} from '../../WebPlot';


export const EXTRACT_DIALOG_ID = 'extractionDialog';
export const HIPS_TILE_EXTRACT_DIALOG_ID = 'hipsTileExtractionDialog';
export const ZAXIS_POINT_SELECTION_ID = 'z-axisExtraction';
export const EXTRACT_END_ID = 'extractEndId';

export const Z_AXIS = 'Z_AXIS';
export const LINE = 'LINE';
export const POINTS = 'POINTS';
export const HIPS_TILE= 'HIPS_TILE';

export function endExtraction() {
    cancelPointExtraction();
    cancelZaxisExtraction();
    cancelLineExtraction();
    cancelHiPSTileExtraction();
    dispatchCancelActionWatcher(EXTRACT_END_ID);
}

export function cancelHiPSTileExtraction() {
    const pvAry= getPlotViewAry(visRoot()).filter( (pv) => isHiPS(primePlot(pv)));
    dispatchHideDialog(HIPS_TILE_EXTRACT_DIALOG_ID);
    pvAry.forEach( (pv) => {
        const plotId= pv.plotId;
        dispatchDetachLayerFromPlot(ExtractHiPSTileTool.TYPE_ID, plotId);
        dispatchAttributeChange({
            plotId,
            changes: {
                [PlotAttribute.ACTIVE_HIPS_CELL]: undefined,
                [PlotAttribute.ACTIVE_HIPS_NORDER]: undefined
            }
        });
    });
    dispatchDestroyDrawLayer(ExtractHiPSTileTool.TYPE_ID);
    dispatchHideDialog(EXTRACT_DIALOG_ID);
}

function cancelZaxisExtraction() {
    dispatchChangePointSelection(ZAXIS_POINT_SELECTION_ID, false);
    dispatchHideDialog(EXTRACT_DIALOG_ID);
}

export function cancelLineExtraction() {
    const {pv} = currentP();
    if (pv) {
        dispatchDetachLayerFromPlot(ExtractLineTool.TYPE_ID, pv.plotId, true);
        dispatchAttributeChange({
            plotId: pv.plotId,
            changes: {[PlotAttribute.SELECT_ACTIVE_CHART_PT]: undefined}
        });
        dispatchDestroyDrawLayer(ExtractLineTool.TYPE_ID);
    }
    dispatchHideDialog(EXTRACT_DIALOG_ID);
}

function cancelPointExtraction() {
    const {pv} = currentP();
    if (pv) {
        dispatchDetachLayerFromPlot(ExtractPointsTool.TYPE_ID, pv.plotId, true);
        dispatchDestroyDrawLayer(ExtractPointsTool.TYPE_ID);
    }
    dispatchHideDialog(EXTRACT_DIALOG_ID);
}
