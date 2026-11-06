import {dispatchAddActionWatcher, dispatchCancelActionWatcher} from '../../core/MasterSaga.js';
import {dispatchTableHighlight} from '../../tables/TablesCntlr.js';
import {getActiveTableId, getTblById} from '../../tables/TableUtil.js';
import {showPinMessage} from '../../ui/PopupUtil.jsx';
import {
    dispatchChangeActivePlotView, dispatchDeletePlotView, dispatchPlotImage, dispatchZoom
} from '../../visualize/ImagePlotDispatch.js';
import {dispatchReplaceViewerItems, getLayoutType, getViewerItemIds} from '../../visualize/MultiViewCntlr.js';
import {PlotAttribute} from '../../visualize/PlotAttribute.js';
import {RangeValues} from '../../visualize/RangeValues.js';
import {getPlotGroupById} from '../../visualize/PlotGroup.js';
import {
    currentP, getPlotViewAry, isDefaultCoverageActive, isImageExpanded
} from '../../visualize/PlotViewUtil.js';
import {
    CHANGE_ACTIVE_PLOT_VIEW, DEFAULT_FITS_VIEWER_ID, GRID, IMAGE, PLOT_IMAGE, PLOT_IMAGE_FAIL, SINGLE, ZoomType
} from '../../visualize/VisConst.js';
import {visRoot} from '../../visualize/VisStoreRoots.js';
import {WebPlotRequest} from '../../visualize/WebPlotRequest.js';
import {DPtypes} from '../DataProductsType.js';
import {
    changeTableHighlightToMatchPlotView, createGridImagesActivate, createRelatedDataGridActivate,
    createRelatedGridImagesActivate, createSingleImageActivate, createSingleImageExtraction,
    makeServerRequestBuilder, resetImageFullGridActivePlot, setupWatchForPvChange, zoomPlotPerViewSize
} from '../ImageDataProductsUtil.js';

// Every image call goes through the image store; these stubs stand in for it, and each test sets what it needs.
jest.mock('../../core/MasterSaga.js', () => ({
    ...jest.requireActual('../../core/MasterSaga.js'),
    dispatchAddActionWatcher: jest.fn(),
    dispatchCancelActionWatcher: jest.fn(),
}));
jest.mock('../../ui/PopupUtil.jsx', () => ({
    showPinMessage: jest.fn(),
}));
jest.mock('../../tables/TablesCntlr.js', () => ({
    ...jest.requireActual('../../tables/TablesCntlr.js'),
    dispatchTableHighlight: jest.fn(),
}));
jest.mock('../../tables/TableUtil.js', () => ({
    ...jest.requireActual('../../tables/TableUtil.js'),
    getActiveTableId: jest.fn(),
    getTblById: jest.fn(),
}));
jest.mock('../../visualize/ImagePlotDispatch.js', () => ({
    dispatchChangeActivePlotView: jest.fn(),
    dispatchDeletePlotView: jest.fn(),
    dispatchPlotImage: jest.fn(),
    dispatchZoom: jest.fn(),
}));
jest.mock('../../visualize/MultiViewCntlr.js', () => ({
    ...jest.requireActual('../../visualize/MultiViewCntlr.js'),
    dispatchReplaceViewerItems: jest.fn(),
    getLayoutType: jest.fn(),
    getMultiViewRoot: jest.fn(),
    getViewerItemIds: jest.fn(),
}));
jest.mock('../../visualize/PlotGroup.js', () => ({
    ...jest.requireActual('../../visualize/PlotGroup.js'),
    getPlotGroupById: jest.fn(),
}));
jest.mock('../../visualize/PlotViewUtil.js', () => ({
    ...jest.requireActual('../../visualize/PlotViewUtil.js'),
    currentP: jest.fn(),
    getPlotViewAry: jest.fn(),
    isDefaultCoverageActive: jest.fn(),
    isImageExpanded: jest.fn(),
}));
jest.mock('../../visualize/VisStoreRoots.js', () => ({
    ...jest.requireActual('../../visualize/VisStoreRoots.js'),
    visRoot: jest.fn(),
}));

const VIEWER_ID = 'dp-image-viewer';
const TBL_ID = 'obscore-tbl';
const GROUP_ID = `${VIEWER_ID}-${TBL_ID}-standard`; // the plot group of the data product images of a table
const IMAGE_URL = 'https://irsa.ipac.caltech.edu/data/example/';

/**
 * A request for an image file of the made up example data.
 * @param {string} fileName
 * @param {string} plotId
 * @return {WebPlotRequest}
 */
function makeRequest(fileName, plotId) {
    const request = WebPlotRequest.makeURIPlotRequest(IMAGE_URL + fileName);
    request.setPlotId(plotId);
    return request;
}

beforeEach(() => {
    jest.resetAllMocks();
    // an empty image store and viewer, unless a test sets more
    visRoot.mockReturnValue({});
    currentP.mockReturnValue({});
    getPlotViewAry.mockReturnValue([]);
    getViewerItemIds.mockReturnValue([]);
});

describe('createSingleImageActivate', () => {
    test('no request gives no activate function', () => {
        expect(createSingleImageActivate(undefined, VIEWER_ID, TBL_ID, 3)).toBeUndefined();
    });

    test('tags the request with the table row', () => {
        const request = makeRequest('w1.fits', 'plot-w1');
        createSingleImageActivate(request, VIEWER_ID, TBL_ID, 3);
        expect(request.getAttributes()).toMatchObject(
            {[PlotAttribute.RELATED_TABLE_ROW]: '3', [PlotAttribute.RELATED_TABLE_ID]: TBL_ID});
    });

    test('a request with no plot id gets one named for its table', () => {
        const request = WebPlotRequest.makeURIPlotRequest(IMAGE_URL + 'w1.fits');
        createSingleImageActivate(request, VIEWER_ID, TBL_ID, 3);
        expect(request.getPlotId()).toBe(`${TBL_ID}-singleview`);
    });

    test('a request with no plot id and no table gets the no-table plot id', () => {
        const request = WebPlotRequest.makeURIPlotRequest(IMAGE_URL + 'w1.fits');
        createSingleImageActivate(request, VIEWER_ID, undefined, 3);
        expect(request.getPlotId()).toBe('no-table-singleview');
    });

    test('the activate function shows the image in the viewer, in the table plot group', () => {
        const request = makeRequest('w1.fits', 'plot-w1');
        createSingleImageActivate(request, VIEWER_ID, TBL_ID, 3)();
        expect(dispatchReplaceViewerItems).toHaveBeenCalledWith(VIEWER_ID, ['plot-w1'], IMAGE);
        const {wpRequest, plotId, viewerId, attributes} = dispatchPlotImage.mock.calls[0][0];
        expect([plotId, viewerId, attributes]).toEqual(['plot-w1', VIEWER_ID, {tbl_id: TBL_ID}]);
        expect(wpRequest.getPlotGroupId()).toBe(GROUP_ID);
        expect(wpRequest.getURL()).toBe(IMAGE_URL + 'w1.fits');
    });

    test('the image is made active a second after it is plotted', () => {
        jest.useFakeTimers();
        const request = makeRequest('w1.fits', 'plot-w1');
        isDefaultCoverageActive.mockReturnValue(false);
        currentP.mockReturnValue({pv: {plotId: 'plot-w1', serverCall: 'success'}});
        createSingleImageActivate(request, VIEWER_ID, TBL_ID, 3)();
        expect(dispatchChangeActivePlotView).not.toHaveBeenCalled();
        jest.advanceTimersByTime(1000);
        expect(dispatchChangeActivePlotView).toHaveBeenCalledWith('plot-w1');
        jest.useRealTimers();
    });

    test('the image is not made active when the coverage image is', () => {
        jest.useFakeTimers();
        const request = makeRequest('w1.fits', 'plot-w1');
        isDefaultCoverageActive.mockReturnValue(true);
        currentP.mockReturnValue({pv: {plotId: 'plot-w1', serverCall: 'success'}});
        createSingleImageActivate(request, VIEWER_ID, TBL_ID, 3)();
        jest.advanceTimersByTime(1000);
        expect(dispatchChangeActivePlotView).not.toHaveBeenCalled();
        jest.useRealTimers();
    });

    test('an image still plotting is made active when its plot completes', () => {
        const request = makeRequest('w1.fits', 'plot-w1');
        isDefaultCoverageActive.mockReturnValue(false);
        currentP.mockReturnValue({pv: {plotId: 'plot-w1', serverCall: 'working'}});
        createSingleImageActivate(request, VIEWER_ID, TBL_ID, 3)();
        expect(dispatchAddActionWatcher).toHaveBeenCalledWith(expect.objectContaining(
            {params: expect.objectContaining({plotId: 'plot-w1'}), actions: [PLOT_IMAGE, PLOT_IMAGE_FAIL]}));
    });
});

describe('createGridImagesActivate', () => {
    test('tags each request with its row and plot id, and plots them all', () => {
        const requests = [makeRequest('r0.fits'), undefined, makeRequest('r2.fits')];
        const plotRows = [{row: 0, plotId: 'grid-0'}, {row: 1, plotId: 'grid-1'}, {row: 2, plotId: 'grid-2', highlight: true}];
        createGridImagesActivate(requests, VIEWER_ID, TBL_ID, plotRows)();
        expect(requests[2].getPlotId()).toBe('grid-2');
        expect(requests[2].getAttributes()).toMatchObject(
            {[PlotAttribute.RELATED_TABLE_ROW]: '2', [PlotAttribute.RELATED_TABLE_ID]: TBL_ID});
        expect(dispatchReplaceViewerItems).toHaveBeenCalledWith(VIEWER_ID, ['grid-0', 'grid-2'], IMAGE);
        expect(dispatchPlotImage.mock.calls.map(([{plotId}]) => plotId)).toEqual(['grid-0', 'grid-2']);
    });

    test('the highlighted row image is made active', () => {
        const requests = [makeRequest('r0.fits'), makeRequest('r1.fits')];
        const plotRows = [{row: 0, plotId: 'grid-0'}, {row: 1, plotId: 'grid-1', highlight: true}];
        isDefaultCoverageActive.mockReturnValue(false);
        currentP.mockReturnValue({pv: {serverCall: 'working'}});
        createGridImagesActivate(requests, VIEWER_ID, TBL_ID, plotRows)();
        expect(dispatchAddActionWatcher.mock.calls[0][0].params.plotId).toBe('grid-1');
    });
});

describe('createRelatedGridImagesActivate', () => {
    test('numbers the requests and makes the first one active by default', () => {
        const requestAry = [makeRequest('sci.fits', 'rel-sci'), makeRequest('wht.fits', 'rel-wht')];
        currentP.mockReturnValue({pv: {serverCall: 'working'}});
        createRelatedGridImagesActivate({requestAry, imageViewerId: VIEWER_ID, tbl_id: TBL_ID})();
        expect(requestAry.map((r) => r.getAttributes()[PlotAttribute.RELATED_GRID_IMAGE_COUNT])).toEqual(['0', '1']);
        expect(requestAry[1].getAttributes()[PlotAttribute.RELATED_TABLE_ID]).toBe(TBL_ID);
        expect(dispatchAddActionWatcher.mock.calls[0][0].params.plotId).toBe('rel-sci');
    });

    test('plots a three color image with the standard ones', () => {
        const requestAry = [makeRequest('sci.fits', 'rel-sci')];
        const threeColorReqAry = [makeRequest('r.fits', 'rel-3c'), makeRequest('g.fits', 'rel-3c'), undefined];
        createRelatedGridImagesActivate({requestAry, threeColorReqAry, imageViewerId: VIEWER_ID, tbl_id: TBL_ID})();
        expect(dispatchReplaceViewerItems).toHaveBeenCalledWith(VIEWER_ID, ['rel-sci', 'rel-3c'], IMAGE);
        const threeColorCall = dispatchPlotImage.mock.calls.find(([{threeColor}]) => threeColor)[0];
        expect(threeColorCall).toMatchObject({plotId: 'rel-3c', viewerId: VIEWER_ID, wpRequest: threeColorReqAry});
        expect(threeColorReqAry[0].getPlotGroupId()).toBe(GROUP_ID);
    });

    test('watches for the active image to change', () => {
        const requestAry = [makeRequest('sci.fits', 'rel-sci')];
        const onActivePvChanged = jest.fn();
        createRelatedGridImagesActivate({requestAry, imageViewerId: VIEWER_ID, tbl_id: TBL_ID, onActivePvChanged})();
        expect(dispatchAddActionWatcher).toHaveBeenCalledWith(expect.objectContaining(
            {actions: [CHANGE_ACTIVE_PLOT_VIEW], params: {onActivePvChanged}}));
    });
});

describe('createRelatedDataGridActivate', () => {
    test('no standard requests gives no activate function', () => {
        expect(createRelatedDataGridActivate({standard: []}, VIEWER_ID, TBL_ID, 'rel-sci')).toBeUndefined();
    });

    test('remembers the highlighted plot and plots the standard requests', () => {
        const reqRet = {standard: [makeRequest('sci.fits', 'rel-sci')]};
        createRelatedDataGridActivate(reqRet, VIEWER_ID, TBL_ID, 'rel-sci')();
        expect(reqRet.highlightPlotId).toBe('rel-sci');
        expect(dispatchPlotImage.mock.calls[0][0].plotId).toBe('rel-sci');
    });
});

describe('the activate functions', () => {
    test('an image already plotted in the group is not plotted again, and keeps its plot id', () => {
        const plotted = makeRequest('w1.fits', 'plot-old');
        plotted.setPlotGroupId(GROUP_ID);
        plotted.setAttributes({[PlotAttribute.RELATED_TABLE_ROW]: '0', [PlotAttribute.RELATED_TABLE_ID]: TBL_ID});
        const pv = {plotId: 'plot-old', plotGroupId: GROUP_ID, request: plotted};
        getPlotViewAry.mockReturnValue([pv]);
        currentP.mockReturnValue({pv});
        getViewerItemIds.mockReturnValue(['plot-old']);
        const request = makeRequest('w1.fits', 'plot-new');
        createSingleImageActivate(request, VIEWER_ID, TBL_ID, 0)();
        expect(request.getPlotId()).toBe('plot-old');
        expect(dispatchPlotImage).not.toHaveBeenCalled();
        expect(dispatchReplaceViewerItems).not.toHaveBeenCalled();
    });

    test('images in the viewer that are not plotted now are deleted', () => {
        getViewerItemIds.mockReturnValue(['plot-a', 'plot-b']);
        createSingleImageActivate(makeRequest('w1.fits', 'plot-a'), VIEWER_ID, TBL_ID, 0)();
        expect(dispatchReplaceViewerItems).toHaveBeenCalledWith(VIEWER_ID, ['plot-a'], IMAGE);
        expect(dispatchDeletePlotView).toHaveBeenCalledWith({plotId: 'plot-b'});
    });

    test('the group range values are used for each image', () => {
        const rangeValues = RangeValues.make2To10SigmaLinear();
        getPlotGroupById.mockReturnValue({defaultRangeValues: rangeValues});
        createSingleImageActivate(makeRequest('w1.fits', 'plot-w1'), VIEWER_ID, TBL_ID, 0)();
        expect(RangeValues.serializeRV(dispatchPlotImage.mock.calls[0][0].wpRequest.getInitialRangeValues()))
            .toBe(RangeValues.serializeRV(rangeValues));
    });

    describe('the cleanup function returned', () => {
        const PLOT_ROWS = [{row: 1, plotId: 'plot-1', highlight: true}];

        /**
         * Plot views in the table plot group, one per row.
         * @param {Array.<number>} rows
         * @return {Array.<Object>}
         */
        function makeGroupPlotViews(rows) {
            return rows.map((row) => ({plotId: `plot-${row}`, plotGroupId: GROUP_ID,
                plot: {attributes: {[PlotAttribute.RELATED_TABLE_ROW]: row + '', [PlotAttribute.RELATED_TABLE_ID]: TBL_ID}}}));
        }

        test('deletes the group images that are not for the highlighted row', () => {
            const cleanup = createGridImagesActivate([makeRequest('w1.fits')], VIEWER_ID, TBL_ID, PLOT_ROWS)();
            const plotViews = makeGroupPlotViews([0, 1, 2]);
            getPlotViewAry.mockReturnValue(plotViews);
            currentP.mockImplementation((plotId) => ({plot: plotViews.find((pv) => pv.plotId === plotId)?.plot}));
            getActiveTableId.mockReturnValue(TBL_ID);
            getTblById.mockReturnValue({tbl_id: TBL_ID, highlightedRow: 1});
            dispatchDeletePlotView.mockClear();
            cleanup({nextDisplayType: DPtypes.TABLE});
            expect(dispatchDeletePlotView.mock.calls.map(([{plotId}]) => plotId)).toEqual(['plot-0', 'plot-2']);
        });

        test('a single image activate returns one too, which deletes the images not for the highlighted row', () => {
            const cleanup = createSingleImageActivate(makeRequest('w1.fits', 'plot-1'), VIEWER_ID, TBL_ID, 1)();
            const plotViews = makeGroupPlotViews([0, 1]);
            getPlotViewAry.mockReturnValue(plotViews);
            currentP.mockImplementation((plotId) => ({plot: plotViews.find((pv) => pv.plotId === plotId)?.plot}));
            getActiveTableId.mockReturnValue(TBL_ID);
            getTblById.mockReturnValue({tbl_id: TBL_ID, highlightedRow: 1});
            dispatchDeletePlotView.mockClear();
            cleanup({nextDisplayType: DPtypes.TABLE});
            expect(dispatchDeletePlotView.mock.calls.map(([{plotId}]) => plotId)).toEqual(['plot-0']);
        });

        test('keeps the images when the next product is an image grid of the same table', () => {
            const cleanup = createGridImagesActivate([makeRequest('w1.fits')], VIEWER_ID, TBL_ID, PLOT_ROWS)();
            getPlotViewAry.mockReturnValue(makeGroupPlotViews([0, 1, 2]));
            getLayoutType.mockReturnValue(GRID);
            dispatchDeletePlotView.mockClear();
            cleanup({nextDisplayType: DPtypes.IMAGE, nextMetaDataTableId: TBL_ID});
            expect(dispatchDeletePlotView).not.toHaveBeenCalled();
        });

        test('stops watching for the active image to change', () => {
            const requestAry = [makeRequest('sci.fits', 'rel-sci')];
            const cleanup = createRelatedGridImagesActivate({requestAry, imageViewerId: VIEWER_ID, tbl_id: TBL_ID,
                onActivePvChanged: jest.fn()})();
            const watcherId = dispatchAddActionWatcher.mock.calls
                .find(([{actions}]) => actions.includes(CHANGE_ACTIVE_PLOT_VIEW))[0].id;
            cleanup({nextDisplayType: DPtypes.TABLE});
            expect(dispatchCancelActionWatcher).toHaveBeenLastCalledWith(watcherId);
        });

        test('keeps the images when an image is expanded', () => {
            const cleanup = createGridImagesActivate([makeRequest('w1.fits')], VIEWER_ID, TBL_ID, PLOT_ROWS)();
            getPlotViewAry.mockReturnValue(makeGroupPlotViews([0, 1, 2]));
            isImageExpanded.mockReturnValue(true);
            getLayoutType.mockReturnValue(SINGLE);
            dispatchDeletePlotView.mockClear();
            cleanup({nextDisplayType: DPtypes.TABLE});
            expect(dispatchDeletePlotView).not.toHaveBeenCalled();
        });
    });
});

describe('createSingleImageExtraction', () => {
    test('no request gives no extraction function', () => {
        expect(createSingleImageExtraction(undefined)).toBeUndefined();
    });

    test('pins a copy of the image to the image area, with its ObsCore and DataLink data', () => {
        const request = makeRequest('w1.fits', 'plot-w1');
        const sourceObsCoreData = {obs_id: 'obs-42'};
        const dlData = {semantics: '#this'};
        createSingleImageExtraction(request, sourceObsCoreData, dlData)();
        const {viewerId, wpRequest, attributes} = dispatchPlotImage.mock.calls[0][0];
        expect(viewerId).toBe(DEFAULT_FITS_VIEWER_ID);
        expect(wpRequest).not.toBe(request);
        expect(wpRequest.getURL()).toBe(IMAGE_URL + 'w1.fits');
        expect(wpRequest.getPlotId()).toMatch(/^extract-plotId-/);
        expect(wpRequest.getPlotGroupId()).toBe('extract-group');
        expect(attributes).toEqual({[PlotAttribute.USER_PINNED_IMAGE]: true, sourceObsCoreData, dlData});
        expect(showPinMessage).toHaveBeenCalledWith('Pinning to Image Area');
    });

    test('each pinned image gets the next extract plot id', () => {
        createSingleImageExtraction(makeRequest('w1.fits', 'plot-w1'))();
        createSingleImageExtraction(makeRequest('w2.fits', 'plot-w2'))();
        const [firstPinNumber, secondPinNumber] = dispatchPlotImage.mock.calls
            .map(([{wpRequest}]) => Number(wpRequest.getPlotId().replace('extract-plotId-', '')));
        expect(secondPinNumber).toBe(firstPinNumber + 1);
    });

    test('of several images, pins the active one', () => {
        const requests = [makeRequest('sci.fits', 'rel-sci'), makeRequest('wht.fits', 'rel-wht')];
        currentP.mockReturnValue({plotId: 'rel-wht'});
        createSingleImageExtraction(requests)();
        expect(dispatchPlotImage).toHaveBeenCalledTimes(1);
        expect(dispatchPlotImage.mock.calls[0][0].wpRequest.getURL()).toBe(IMAGE_URL + 'wht.fits');
    });

    test('of several images, pins nothing when none is active', () => {
        currentP.mockReturnValue({plotId: 'other-plot'});
        createSingleImageExtraction([makeRequest('sci.fits', 'rel-sci')])();
        expect(dispatchPlotImage).not.toHaveBeenCalled();
    });
});

describe('setupWatchForPvChange', () => {
    test('calls back with the new active plot id', () => {
        const onActivePvChanged = jest.fn();
        setupWatchForPvChange('watch-1', onActivePvChanged);
        const {id, callback, params} = dispatchAddActionWatcher.mock.calls[0][0];
        expect(id).toBe('watch-1');
        callback({type: CHANGE_ACTIVE_PLOT_VIEW, payload: {plotId: 'plot-w2'}}, jest.fn(), params);
        expect(onActivePvChanged).toHaveBeenCalledWith('plot-w2');
    });
});

describe('zoomPlotPerViewSize', () => {
    test('zooms a plotted image now', () => {
        currentP.mockReturnValue({pv: {serverCall: 'success'}});
        zoomPlotPerViewSize('plot-w1', ZoomType.TO_WIDTH);
        expect(dispatchZoom).toHaveBeenCalledWith({plotId: 'plot-w1', userZoomType: ZoomType.TO_WIDTH});
    });

    test('does nothing for a plot id with no image', () => {
        currentP.mockReturnValue({});
        zoomPlotPerViewSize('removed-plot', ZoomType.TO_WIDTH);
        expect(dispatchZoom).not.toHaveBeenCalled();
        expect(dispatchAddActionWatcher).not.toHaveBeenCalled();
    });

    test('does not zoom when the images are WCS matched', () => {
        visRoot.mockReturnValue({wcsMatchType: 'Standard'});
        currentP.mockReturnValue({pv: {serverCall: 'success'}});
        zoomPlotPerViewSize('plot-w1', ZoomType.TO_WIDTH);
        expect(dispatchZoom).not.toHaveBeenCalled();
    });

    test('zooms an image still plotting once it is plotted', () => {
        currentP.mockReturnValue({pv: {serverCall: 'working'}});
        zoomPlotPerViewSize('plot-w1', ZoomType.TO_WIDTH);
        expect(dispatchZoom).not.toHaveBeenCalled();
        const {callback, params} = dispatchAddActionWatcher.mock.calls[0][0];
        const cancelSelf = jest.fn();
        callback({type: PLOT_IMAGE, payload: {pvNewPlotInfoAry: [{plotId: 'other-plot'}]}}, cancelSelf, params);
        expect(dispatchZoom).not.toHaveBeenCalled();
        callback({type: PLOT_IMAGE, payload: {pvNewPlotInfoAry: [{plotId: 'plot-w1'}]}}, cancelSelf, params);
        expect(dispatchZoom).toHaveBeenCalledWith({plotId: 'plot-w1', userZoomType: ZoomType.TO_WIDTH});
        expect(cancelSelf).toHaveBeenCalled();
    });

    test('stops waiting when the plot fails', () => {
        currentP.mockReturnValue({pv: {serverCall: 'working'}});
        zoomPlotPerViewSize('plot-w1', ZoomType.TO_WIDTH);
        const {callback, params} = dispatchAddActionWatcher.mock.calls[0][0];
        const cancelSelf = jest.fn();
        callback({type: PLOT_IMAGE_FAIL, payload: {plotId: 'plot-w1'}}, cancelSelf, params);
        expect(cancelSelf).toHaveBeenCalled();
        expect(dispatchZoom).not.toHaveBeenCalled();
    });
});

describe('resetImageFullGridActivePlot', () => {
    /**
     * A plot for a table row; a related grid image after the first has a count above 0.
     * @param {number} row
     * @param {string} [gridImageCount] - attributes are strings
     * @return {Object}
     */
    const makeRowPlot = (row, gridImageCount) => ({attributes: {[PlotAttribute.RELATED_TABLE_ROW]: row + '',
        [PlotAttribute.RELATED_GRID_IMAGE_COUNT]: gridImageCount}});

    test('makes the image of the highlighted row active', () => {
        const plotsById = {'plot-0': makeRowPlot(0), 'plot-1': makeRowPlot(1)};
        getTblById.mockReturnValue({highlightedRow: 1});
        currentP.mockImplementation((plotId) => ({plot: plotsById[plotId]}));
        resetImageFullGridActivePlot(TBL_ID, ['plot-0', 'plot-1']);
        expect(dispatchChangeActivePlotView).toHaveBeenCalledWith('plot-1');
    });

    test('skips the extra images of a related grid', () => {
        const plotsById = {'plot-1b': makeRowPlot(1, '1'), 'plot-1a': makeRowPlot(1, '0')};
        getTblById.mockReturnValue({highlightedRow: 1});
        currentP.mockImplementation((plotId) => ({plot: plotsById[plotId]}));
        resetImageFullGridActivePlot(TBL_ID, ['plot-1b', 'plot-1a']);
        expect(dispatchChangeActivePlotView).toHaveBeenCalledWith('plot-1a');
    });

    test('does nothing with no table or no plots', () => {
        resetImageFullGridActivePlot(undefined, ['plot-0']);
        resetImageFullGridActivePlot(TBL_ID, []);
        expect(dispatchChangeActivePlotView).not.toHaveBeenCalled();
    });
});

describe('changeTableHighlightToMatchPlotView', () => {
    test('highlights the table row of the image', () => {
        const tableRequest = {tbl_id: TBL_ID};
        currentP.mockReturnValue({plot: {attributes:
            {[PlotAttribute.RELATED_TABLE_ROW]: '4', [PlotAttribute.RELATED_TABLE_ID]: TBL_ID}}});
        getTblById.mockReturnValue({tbl_id: TBL_ID, highlightedRow: 1, request: tableRequest});
        changeTableHighlightToMatchPlotView('plot-4', TBL_ID);
        expect(dispatchTableHighlight).toHaveBeenCalledWith(TBL_ID, 4, tableRequest);
    });

    test('does nothing when the row is already highlighted or the image has no row', () => {
        getTblById.mockReturnValue({tbl_id: TBL_ID, highlightedRow: 4});
        currentP.mockReturnValue({plot: {attributes:
            {[PlotAttribute.RELATED_TABLE_ROW]: '4', [PlotAttribute.RELATED_TABLE_ID]: TBL_ID}}});
        changeTableHighlightToMatchPlotView('plot-4', TBL_ID);
        currentP.mockReturnValue({plot: {attributes: {[PlotAttribute.RELATED_TABLE_ID]: TBL_ID}}});
        changeTableHighlightToMatchPlotView('plot-x', TBL_ID);
        expect(dispatchTableHighlight).not.toHaveBeenCalled();
    });

    test('does nothing for an image of another table\'s row', () => {
        getTblById.mockReturnValue({tbl_id: TBL_ID, highlightedRow: 1});
        currentP.mockReturnValue({plot: {attributes:
            {[PlotAttribute.RELATED_TABLE_ROW]: '4', [PlotAttribute.RELATED_TABLE_ID]: 'tbl-other'}}});
        changeTableHighlightToMatchPlotView('plot-other-4', TBL_ID);
        expect(dispatchTableHighlight).not.toHaveBeenCalled();
    });
});

describe('makeServerRequestBuilder', () => {
    // made up: a WISE image search result row
    const WISE_TABLE = {
        tableMeta: {mission: 'wise', schema: 'allsky', source: 'irsa'},
        tableData: {columns: [{name: 'scan_id'}, {name: 'frame_num'}, {name: 'band'}],
            data: [['01234a', '117', '1'], ['05678b', '42', '2']]},
    };

    test('makes a processor request from the chosen columns and meta of a row', () => {
        const rangeValues = RangeValues.make2To10SigmaLinear();
        const makeReq = makeServerRequestBuilder(WISE_TABLE, ['scan_id', 'band'], ['mission'], rangeValues, 4);
        const request = makeReq('plot-wise', 'WiseImage', 'WISE W2', 1, {name: 'subsize', value: '0.1'});
        expect(request.getParams()).toMatchObject({id: 'WiseImage', scan_id: '05678b', band: '2', mission: 'wise',
            subsize: '0.1'});
        expect(request.getParams().frame_num).toBeUndefined();
        expect(request.getParams().schema).toBeUndefined();
        expect(request.getPlotId()).toBe('plot-wise');
        expect(request.getTitle()).toBe('WISE W2');
        expect(request.getZoomType()).toBe(ZoomType.TO_WIDTH);
        expect(request.getInitialColorTable()).toBe(4);
        expect(RangeValues.serializeRV(request.getInitialRangeValues())).toBe(RangeValues.serializeRV(rangeValues));
    });

    test('ALL, or no columns, uses every column and ALL uses all the meta', () => {
        const request = makeServerRequestBuilder(WISE_TABLE, 'ALL', 'ALL')('plot-wise', 'WiseImage', 'WISE', 0);
        expect(request.getParams()).toMatchObject({scan_id: '01234a', frame_num: '117', band: '1',
            mission: 'wise', schema: 'allsky', source: 'irsa'});
        const noColumns = makeServerRequestBuilder(WISE_TABLE, [], [])('plot-wise', 'WiseImage', 'WISE', 0);
        expect(noColumns.getParams()).toMatchObject({scan_id: '01234a', frame_num: '117', band: '1'});
        expect(noColumns.getParams().mission).toBeUndefined();
    });
});
