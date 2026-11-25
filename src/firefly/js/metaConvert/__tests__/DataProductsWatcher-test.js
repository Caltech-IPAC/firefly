/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*eslint-env node, jest */

import ComponentCntlr from '../../core/ComponentCntlr.js';
import {REINIT_APP} from '../../core/CoreConst.js';
import {dispatchAddTableTypeWatcherDef} from '../../core/MasterSaga.js';
import {TABLE_HIGHLIGHT, TABLE_REMOVE} from '../../tables/TablesCntlr.js';
import {getActiveTableId, getTblById, getTblInfo, isTblDataAvail} from '../../tables/TableUtil.js';
import {Band} from '../../visualize/Band.js';
import {dispatchDeletePlotView} from '../../visualize/ImagePlotDispatch';
import {
    dispatchChangeViewerLayout, getLayoutDetails, getLayoutType, getViewer, getViewerItemIds
} from '../../visualize/MultiViewCntlr.js';
import {isDefaultCoverageActive} from '../../visualize/PlotViewUtil.js';
import {
    ADD_VIEWER, ANY_REPLOT, CHANGE_ACTIVE_PLOT_VIEW, CHANGE_VIEWER_LAYOUT, GRID, GRID_FULL, GRID_RELATED,
    MOUSE_CLICK_REASON, SINGLE, UPDATE_VIEW_SIZE, UserZoomTypes, VIEWER_MOUNTED, VIEWER_UNMOUNTED
} from '../../visualize/VisConst.js';
import {isDataProductsTable} from '../../voAnalyzer/TableAnalysis.js';
import {dispatchUpdateDataProducts, getActivateParams} from '../DataProductsCntlr.js';
import {getFactoryTemplateOptions, makeDataProductsConverter} from '../DataProductsFactory.js';
import {dpdtMessage, DPtypes} from '../DataProductsType.js';
import {startDataProductsWatcher} from '../DataProductsWatcher.js';
import {
    changeTableHighlightToMatchPlotView, resetImageFullGridActivePlot, zoomPlotPerViewSize
} from '../ImageDataProductsUtil.js';
import {findGridTableRows} from '../TableDataProductUtils.js';

// the stores, the converters and the product makers; each test sets what it needs
jest.mock('../../core/MasterSaga.js', () => ({dispatchAddTableTypeWatcherDef: jest.fn()}));
jest.mock('../../tables/TableUtil.js', () => ({
    getActiveTableId: jest.fn(), getTblById: jest.fn(), getTblInfo: jest.fn(), isTblDataAvail: jest.fn(),
}));
jest.mock('../../visualize/ImagePlotDispatch', () => ({dispatchDeletePlotView: jest.fn()}));
jest.mock('../../visualize/MultiViewCntlr.js', () => ({
    dispatchChangeViewerLayout: jest.fn(), getLayoutDetails: jest.fn(), getLayoutType: jest.fn(),
    getMultiViewRoot: jest.fn(), getViewer: jest.fn(), getViewerItemIds: jest.fn(),
}));
jest.mock('../../visualize/PlotViewUtil.js', () => ({isDefaultCoverageActive: jest.fn()}));
jest.mock('../../voAnalyzer/TableAnalysis.js', () => ({isDataProductsTable: jest.fn()}));
jest.mock('../DataProductsCntlr.js', () => ({
    dataProductRoot: jest.fn(), dispatchUpdateDataProducts: jest.fn(), getActivateParams: jest.fn(),
}));
jest.mock('../DataProductsFactory.js', () => ({getFactoryTemplateOptions: jest.fn(), makeDataProductsConverter: jest.fn()}));
jest.mock('../ImageDataProductsUtil.js', () => ({
    changeTableHighlightToMatchPlotView: jest.fn(), resetImageFullGridActivePlot: jest.fn(), zoomPlotPerViewSize: jest.fn(),
}));
jest.mock('../TableDataProductUtils.js', () => ({findGridTableRows: jest.fn()}));

const TBL_ID = 'tbl-products';
const DP_ID = 'dp-viewer';
const FACTORY_KEY = 'dp-factory';
const ACTIVATE_PARAMS = {imageViewerId: 'image-viewer', dpId: DP_ID};
const TABLE = {tbl_id: TBL_ID, highlightedRow: 3};
const IMAGE_VIEWER = {viewerId: 'image-viewer'};
const PRODUCT = {displayType: DPtypes.IMAGE, menuKey: 'image-0'};
const NO_PRODUCTS = dpdtMessage('No Data Products', undefined, {noProductsAvailable: true});

const cancelSelf = jest.fn();

/** let the product promises resolve */
const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0));

/**
 * Calls the watcher that startDataProductsWatcher adds, for TBL_ID, the way MasterSaga does.
 * @param {Object} action - undefined for the first call, when the table is loaded
 * @param {Object} params - the state the watcher returned last time
 * @return {Object} the state for the next call
 */
function watchDataProductsTable(action, params) {
    startDataProductsWatcher({dataTypeViewerId: DP_ID, factoryKey: FACTORY_KEY});
    const {watcher, options} = dispatchAddTableTypeWatcherDef.mock.calls.at(-1)[0];
    return watcher(TBL_ID, action, cancelSelf, {...params, options});
}

/**
 * The viewers in the store.
 * @param {Object} viewersById
 */
function setViewers(viewersById) {
    getViewer.mockImplementation((root, viewerId) => viewersById[viewerId]);
}

/**
 * A converter whose product functions all return PRODUCT.
 * @param {Object} props - the converter's settings
 * @return {Object}
 */
function makeConverter(props) {
    return {
        converterId: 'conv',
        getSingleDataProduct: jest.fn(() => Promise.resolve(PRODUCT)),
        getGridDataProduct: jest.fn(() => Promise.resolve(PRODUCT)),
        getRelatedDataProduct: jest.fn(() => Promise.resolve(PRODUCT)),
        ...props,
    };
}

beforeEach(() => {
    jest.resetAllMocks();
    // the store: TBL_ID is the active table, its row 3 is highlighted and loaded, and it has a single layout
    getActivateParams.mockReturnValue(ACTIVATE_PARAMS);
    getActiveTableId.mockReturnValue(TBL_ID);
    getTblById.mockReturnValue(TABLE);
    getTblInfo.mockReturnValue({highlightedRow: 3});
    isTblDataAvail.mockReturnValue(true);
    getLayoutType.mockReturnValue(SINGLE);
});

describe('startDataProductsWatcher', () => {
    test('adds a watcher for data products tables, with the viewer and factory as options', () => {
        startDataProductsWatcher({dataTypeViewerId: DP_ID, factoryKey: FACTORY_KEY});
        const def = dispatchAddTableTypeWatcherDef.mock.calls[0][0];
        expect(def).toMatchObject({id: DP_ID, allowMultiples: false});
        expect(def.options).toEqual({dataTypeViewerId: DP_ID, factoryKey: FACTORY_KEY});
        expect(def.actions).toEqual(expect.arrayContaining([TABLE_HIGHLIGHT, TABLE_REMOVE, VIEWER_MOUNTED, UPDATE_VIEW_SIZE]));
        isDataProductsTable.mockReturnValue(true);
        expect(def.testTable({tbl_id: TBL_ID})).toBe(true);
        expect(isDataProductsTable).toHaveBeenCalledWith(TBL_ID);
    });
});

describe('watchDataProductsTable', () => {
    describe('first call, when the table loads', () => {
        test('stays paused while neither viewer exists', () => {
            setViewers({});
            expect(watchDataProductsTable(undefined, {})).toEqual({paused: true, firstTime: true});
            expect(makeDataProductsConverter).not.toHaveBeenCalled();
        });

        test('with the image viewer there, shows the product for the highlighted row of the active table', async () => {
            const converter = makeConverter({});
            makeDataProductsConverter.mockReturnValue(converter);
            setViewers({'image-viewer': IMAGE_VIEWER});
            expect(watchDataProductsTable(undefined, {})).toEqual({paused: false, firstTime: false});
            expect(makeDataProductsConverter).toHaveBeenCalledWith(TABLE, FACTORY_KEY);
            expect(dispatchChangeViewerLayout).toHaveBeenCalledWith('image-viewer', SINGLE, undefined, TBL_ID);
            expect(converter.getSingleDataProduct).toHaveBeenCalledWith(TABLE, 3, ACTIVATE_PARAMS, {});
            await flushPromises();
            expect(dispatchUpdateDataProducts).toHaveBeenCalledWith(DP_ID, PRODUCT);
        });

        test('a table that is not active is not shown yet', () => {
            setViewers({'image-viewer': IMAGE_VIEWER});
            getActiveTableId.mockReturnValue('tbl-other');
            expect(watchDataProductsTable(undefined, {})).toEqual({paused: false, firstTime: true});
            expect(makeDataProductsConverter).not.toHaveBeenCalled();
        });
    });

    describe('which actions count', () => {
        test('an action for another viewer or another table changes nothing', () => {
            const state = {paused: false, firstTime: false};
            expect(watchDataProductsTable({type: VIEWER_MOUNTED, payload: {viewerId: 'other-viewer'}}, state))
                .toMatchObject(state);
            expect(watchDataProductsTable({type: TABLE_HIGHLIGHT, payload: {tbl_id: 'tbl-other'}}, state))
                .toMatchObject(state);
            expect(makeDataProductsConverter).not.toHaveBeenCalled();
        });

        test('an action with no table counts only while this table is active', () => {
            getActiveTableId.mockReturnValue('tbl-other');
            watchDataProductsTable({type: UPDATE_VIEW_SIZE, payload: {plotId: 'plot-1'}},
                {paused: false, zoomOnNextViewSizeChange: true});
            expect(zoomPlotPerViewSize).not.toHaveBeenCalled();
        });
    });

    describe('table actions', () => {
        test('removing the table deletes its images, shows "No Data Products" and stops the watcher', () => {
            getViewerItemIds.mockReturnValue(['plot-1', 'plot-2']);
            expect(watchDataProductsTable({type: TABLE_REMOVE, payload: {tbl_id: TBL_ID}}, {paused: false}))
                .toBeUndefined();
            expect(dispatchDeletePlotView.mock.calls).toEqual([[{plotId: 'plot-1'}], [{plotId: 'plot-2'}]]);
            expect(dispatchUpdateDataProducts).toHaveBeenCalledWith(DP_ID, NO_PRODUCTS);
            expect(cancelSelf).toHaveBeenCalled();
        });

        test('removing the table leaves the products alone when another data products table is active', () => {
            getActiveTableId.mockReturnValue('tbl-other');
            isDataProductsTable.mockReturnValue(true);
            watchDataProductsTable({type: TABLE_REMOVE, payload: {tbl_id: TBL_ID}}, {paused: false});
            expect(dispatchDeletePlotView).not.toHaveBeenCalled();
            expect(dispatchUpdateDataProducts).not.toHaveBeenCalled();
            expect(cancelSelf).toHaveBeenCalled();
        });

        test('a new highlighted row is shown, and the product still loading for the last row is dropped', async () => {
            makeDataProductsConverter.mockReturnValue(makeConverter({}));
            setViewers({'image-viewer': IMAGE_VIEWER});
            const abortLastPromise = jest.fn();
            const state = watchDataProductsTable({type: TABLE_HIGHLIGHT, payload: {tbl_id: TBL_ID}},
                {paused: false, firstTime: false, abortPromise: abortLastPromise});
            expect(abortLastPromise).toHaveBeenCalled();
            expect(state).toEqual({paused: false, firstTime: false, zoomOnNextViewSizeChange: false,
                abortPromise: expect.any(Function)});
            await flushPromises();
            expect(dispatchUpdateDataProducts).toHaveBeenCalledWith(DP_ID, PRODUCT);
        });

        test('a product that finishes after the next row is highlighted is not shown', async () => {
            let resolveFirstRow;
            const converter = makeConverter({});
            converter.getSingleDataProduct.mockReturnValueOnce(new Promise((resolve) => resolveFirstRow = resolve));
            makeDataProductsConverter.mockReturnValue(converter);
            setViewers({'image-viewer': IMAGE_VIEWER});
            const highlight = {type: TABLE_HIGHLIGHT, payload: {tbl_id: TBL_ID}};
            const firstRowState = watchDataProductsTable(highlight, {paused: false, firstTime: false});
            watchDataProductsTable(highlight, firstRowState);
            resolveFirstRow({displayType: DPtypes.IMAGE, menuKey: 'first-row'});
            await flushPromises();
            expect(dispatchUpdateDataProducts.mock.calls).toEqual([[DP_ID, PRODUCT]]);
        });

        test('while paused a new highlighted row is not shown', () => {
            setViewers({});
            const state = watchDataProductsTable({type: TABLE_HIGHLIGHT, payload: {tbl_id: TBL_ID}},
                {paused: true, firstTime: false});
            expect(state).toMatchObject({paused: true});
            expect(makeDataProductsConverter).not.toHaveBeenCalled();
        });

        test('a paused watcher starts again on a table action once the image viewer is mounted', () => {
            const converter = makeConverter({});
            makeDataProductsConverter.mockReturnValue(converter);
            setViewers({'image-viewer': {...IMAGE_VIEWER, mounted: true}});
            const state = watchDataProductsTable({type: TABLE_HIGHLIGHT, payload: {tbl_id: TBL_ID}},
                {paused: true, firstTime: false});
            expect(state).toMatchObject({paused: false});
            expect(converter.getSingleDataProduct).toHaveBeenCalled();
        });
    });

    describe('viewer actions', () => {
        test('a layout change shows the product again, keeps the one loading, and fits the next view size change', () => {
            const converter = makeConverter({});
            makeDataProductsConverter.mockReturnValue(converter);
            setViewers({'image-viewer': IMAGE_VIEWER});
            const abortLastPromise = jest.fn();
            const state = watchDataProductsTable({type: CHANGE_VIEWER_LAYOUT, payload: {viewerId: 'image-viewer'}},
                {paused: false, firstTime: false, abortPromise: abortLastPromise});
            expect(converter.getSingleDataProduct).toHaveBeenCalled();
            expect(abortLastPromise).not.toHaveBeenCalled();
            expect(state).toMatchObject({zoomOnNextViewSizeChange: true});
            watchDataProductsTable({type: UPDATE_VIEW_SIZE, payload: {plotId: 'plot-1'}}, state);
            expect(zoomPlotPerViewSize).toHaveBeenCalledWith('plot-1', UserZoomTypes.FIT);
        });

        test('a product left loading by a layout change is still dropped when the next row is highlighted', async () => {
            let resolveFirstRow;
            const converter = makeConverter({});
            converter.getSingleDataProduct.mockReturnValueOnce(new Promise((resolve) => resolveFirstRow = resolve));
            makeDataProductsConverter.mockReturnValue(converter);
            setViewers({'image-viewer': IMAGE_VIEWER});
            const firstRowState = watchDataProductsTable({type: TABLE_HIGHLIGHT, payload: {tbl_id: TBL_ID}},
                {paused: false, firstTime: false});
            const layoutState = watchDataProductsTable(
                {type: CHANGE_VIEWER_LAYOUT, payload: {viewerId: 'image-viewer'}}, firstRowState);
            watchDataProductsTable({type: TABLE_HIGHLIGHT, payload: {tbl_id: TBL_ID}}, layoutState);
            await flushPromises();
            resolveFirstRow({displayType: DPtypes.IMAGE, menuKey: 'first-row'});
            await flushPromises();
            expect(dispatchUpdateDataProducts.mock.calls).toEqual([[DP_ID, PRODUCT]]);
        });

        test('a product left loading by a layout change that finds no converter is still dropped later', async () => {
            let resolveFirstRow;
            const converter = makeConverter({});
            converter.getSingleDataProduct.mockReturnValueOnce(new Promise((resolve) => resolveFirstRow = resolve));
            makeDataProductsConverter.mockReturnValueOnce(converter).mockReturnValueOnce(undefined).mockReturnValue(converter);
            setViewers({'image-viewer': IMAGE_VIEWER});
            const firstRowState = watchDataProductsTable({type: TABLE_HIGHLIGHT, payload: {tbl_id: TBL_ID}},
                {paused: false, firstTime: false});
            const layoutState = watchDataProductsTable(
                {type: CHANGE_VIEWER_LAYOUT, payload: {viewerId: 'image-viewer'}}, firstRowState);
            watchDataProductsTable({type: TABLE_HIGHLIGHT, payload: {tbl_id: TBL_ID}}, layoutState);
            await flushPromises();
            resolveFirstRow({displayType: DPtypes.IMAGE, menuKey: 'first-row'});
            await flushPromises();
            expect(dispatchUpdateDataProducts.mock.calls).toEqual([[DP_ID, PRODUCT]]);
        });

        test('a view size change that does not follow a layout change does not zoom', () => {
            watchDataProductsTable({type: UPDATE_VIEW_SIZE, payload: {plotId: 'plot-1'}},
                {paused: false, zoomOnNextViewSizeChange: false});
            expect(zoomPlotPerViewSize).not.toHaveBeenCalled();
        });

        test('adding the image viewer mounted starts the watcher and shows the product; unmounted, nothing changes', () => {
            const converter = makeConverter({});
            makeDataProductsConverter.mockReturnValue(converter);
            setViewers({'image-viewer': IMAGE_VIEWER});
            const notMountedState = watchDataProductsTable(
                {type: ADD_VIEWER, payload: {viewerId: 'image-viewer', mounted: false}}, {paused: true, firstTime: false});
            expect(notMountedState).toMatchObject({paused: true});
            expect(converter.getSingleDataProduct).not.toHaveBeenCalled();
            const mountedState = watchDataProductsTable(
                {type: ADD_VIEWER, payload: {viewerId: 'image-viewer', mounted: true}}, {paused: true, firstTime: false});
            expect(mountedState).toMatchObject({paused: false});
            expect(converter.getSingleDataProduct).toHaveBeenCalled();
        });

        test('mounting the image viewer starts the watcher and shows the product; unmounting pauses it', () => {
            const converter = makeConverter({});
            makeDataProductsConverter.mockReturnValue(converter);
            setViewers({'image-viewer': IMAGE_VIEWER});
            const mountedState = watchDataProductsTable({type: VIEWER_MOUNTED, payload: {viewerId: 'image-viewer'}},
                {paused: true, firstTime: false});
            expect(mountedState).toMatchObject({paused: false});
            expect(converter.getSingleDataProduct).toHaveBeenCalled();
            expect(watchDataProductsTable({type: VIEWER_UNMOUNTED, payload: {viewerId: 'image-viewer'}}, mountedState))
                .toMatchObject({paused: true});
        });

        test('a change to the data products component state shows the product again; other components are ignored', () => {
            const converter = makeConverter({});
            makeDataProductsConverter.mockReturnValue(converter);
            getFactoryTemplateOptions.mockReturnValue({dataProductsComponentKey: 'dp-component'});
            setViewers({'image-viewer': IMAGE_VIEWER});
            const state = {paused: false, firstTime: false};
            watchDataProductsTable({type: ComponentCntlr.COMPONENT_STATE_CHANGE, payload: {componentId: 'other'}}, state);
            expect(converter.getSingleDataProduct).not.toHaveBeenCalled();
            watchDataProductsTable({type: ComponentCntlr.COMPONENT_STATE_CHANGE, payload: {componentId: 'dp-component'}}, state);
            expect(converter.getSingleDataProduct).toHaveBeenCalled();
        });

        test('a change to the data products component state while paused waits for the viewer to come back', () => {
            const converter = makeConverter({});
            makeDataProductsConverter.mockReturnValue(converter);
            getFactoryTemplateOptions.mockReturnValue({dataProductsComponentKey: 'dp-component'});
            setViewers({'image-viewer': IMAGE_VIEWER});
            watchDataProductsTable({type: ComponentCntlr.COMPONENT_STATE_CHANGE, payload: {componentId: 'dp-component'}},
                {paused: true, firstTime: false});
            expect(converter.getSingleDataProduct).not.toHaveBeenCalled();
        });

        test('a product shown for a component state change is the first one, so the next update keeps the layout', () => {
            makeDataProductsConverter.mockReturnValue(makeConverter({}));
            getFactoryTemplateOptions.mockReturnValue({dataProductsComponentKey: 'dp-component'});
            setViewers({'image-viewer': IMAGE_VIEWER});
            const state = watchDataProductsTable(
                {type: ComponentCntlr.COMPONENT_STATE_CHANGE, payload: {componentId: 'dp-component'}},
                {paused: false, firstTime: true});
            expect(state).toMatchObject({firstTime: false});
            expect(dispatchChangeViewerLayout).toHaveBeenCalledTimes(1);
            watchDataProductsTable({type: TABLE_HIGHLIGHT, payload: {tbl_id: TBL_ID}}, state);
            expect(dispatchChangeViewerLayout).toHaveBeenCalledTimes(1);
        });

        test('clicking an image highlights its row; other ways of changing the active image do not', () => {
            const state = {paused: false, firstTime: false};
            watchDataProductsTable({type: CHANGE_ACTIVE_PLOT_VIEW, payload: {plotId: 'plot-1', reason: 'other'}}, state);
            expect(changeTableHighlightToMatchPlotView).not.toHaveBeenCalled();
            watchDataProductsTable({type: CHANGE_ACTIVE_PLOT_VIEW, payload: {plotId: 'plot-2', reason: MOUSE_CLICK_REASON}},
                state);
            expect(changeTableHighlightToMatchPlotView).toHaveBeenCalledWith('plot-2', TBL_ID);
        });

        test('clicking an image while paused does not highlight a row', () => {
            watchDataProductsTable({type: CHANGE_ACTIVE_PLOT_VIEW, payload: {plotId: 'plot-2', reason: MOUSE_CLICK_REASON}},
                {paused: true, firstTime: false});
            expect(changeTableHighlightToMatchPlotView).not.toHaveBeenCalled();
        });

        test('a replot resets the active grid image, unless the default coverage is active', () => {
            const state = {paused: false, firstTime: false};
            isDefaultCoverageActive.mockReturnValue(true);
            watchDataProductsTable({type: ANY_REPLOT, payload: {plotIdAry: ['plot-1']}}, state);
            expect(resetImageFullGridActivePlot).not.toHaveBeenCalled();
            isDefaultCoverageActive.mockReturnValue(false);
            watchDataProductsTable({type: ANY_REPLOT, payload: {plotIdAry: ['plot-1']}}, state);
            expect(resetImageFullGridActivePlot).toHaveBeenCalledWith(TBL_ID, ['plot-1']);
        });

        test('restarting the app stops the watcher', () => {
            watchDataProductsTable({type: REINIT_APP, payload: {}}, {paused: false});
            expect(cancelSelf).toHaveBeenCalled();
        });
    });

    describe('choosing the product', () => {
        const highlight = {type: TABLE_HIGHLIGHT, payload: {tbl_id: TBL_ID}};

        test('with no data loaded for the highlighted row, the products are removed', () => {
            setViewers({'image-viewer': IMAGE_VIEWER});
            isTblDataAvail.mockReturnValue(false);
            getViewerItemIds.mockReturnValue([]);
            watchDataProductsTable(highlight, {paused: false, firstTime: false});
            expect(makeDataProductsConverter).not.toHaveBeenCalled();
            expect(dispatchUpdateDataProducts).toHaveBeenCalledWith(DP_ID, NO_PRODUCTS);
        });

        test('the first time, a converter that starts in a full grid sets the viewer to it, with at most 50 images', () => {
            const converter = makeConverter({canGrid: true, initialLayout: GRID_FULL, maxPlots: 80});
            makeDataProductsConverter.mockReturnValue(converter);
            setViewers({'image-viewer': IMAGE_VIEWER});
            getLayoutType.mockReturnValue(GRID);
            getLayoutDetails.mockReturnValue(GRID_FULL);
            const plotRows = [{plotId: 'conv-gridfull-row-0', row: 0, highlight: true}];
            findGridTableRows.mockReturnValue(plotRows);
            watchDataProductsTable(highlight, {paused: false, firstTime: true});
            expect(dispatchChangeViewerLayout).toHaveBeenCalledWith('image-viewer', GRID, GRID_FULL, TBL_ID);
            expect(findGridTableRows).toHaveBeenCalledWith(TABLE, 50, 'conv-gridfull');
            expect(converter.getGridDataProduct).toHaveBeenCalledWith(TABLE, plotRows, ACTIVATE_PARAMS, {});
        });

        test('the related grid gets the three color bands when the three color view is on', () => {
            const converter = makeConverter({canGrid: true, hasRelatedBands: true, threeColor: true});
            makeDataProductsConverter.mockReturnValue(converter);
            setViewers({'image-viewer': {...IMAGE_VIEWER, highlightPlotId: 'plot-1', customData: {
                conv: {threeColorVisible: true, w1: {color: Band.RED}, w2: {color: [Band.GREEN]}, w3: {color: Band.BLUE}},
            }}});
            getLayoutType.mockReturnValue(GRID);
            getLayoutDetails.mockReturnValue(GRID_RELATED);
            watchDataProductsTable(highlight, {paused: false, firstTime: false});
            expect(dispatchChangeViewerLayout).not.toHaveBeenCalled();
            expect(converter.getRelatedDataProduct)
                .toHaveBeenCalledWith(TABLE, 3, ['w1', 'w2', 'w3'], 'plot-1', ACTIVATE_PARAMS, {});
        });

        test('a related grid left by another table is set back to single when this converter has no related bands', () => {
            const converter = makeConverter({canGrid: true, hasRelatedBands: false});
            makeDataProductsConverter.mockReturnValue(converter);
            setViewers({'image-viewer': IMAGE_VIEWER});
            getLayoutType.mockReturnValueOnce(GRID).mockReturnValue(SINGLE);
            getLayoutDetails.mockReturnValue(GRID_RELATED);
            watchDataProductsTable(highlight, {paused: false, firstTime: false});
            expect(dispatchChangeViewerLayout).toHaveBeenCalledWith('image-viewer', SINGLE, undefined, TBL_ID);
            expect(converter.getSingleDataProduct).toHaveBeenCalled();
        });

        test('a grid is set back to single for a converter that cannot grid', () => {
            const converter = makeConverter({canGrid: false});
            makeDataProductsConverter.mockReturnValue(converter);
            setViewers({'image-viewer': IMAGE_VIEWER});
            getLayoutType.mockReturnValueOnce(GRID).mockReturnValueOnce(GRID).mockReturnValue(SINGLE);
            getLayoutDetails.mockReturnValue(GRID_FULL);
            watchDataProductsTable(highlight, {paused: false, firstTime: false});
            expect(dispatchChangeViewerLayout).toHaveBeenCalledWith('image-viewer', SINGLE, undefined, TBL_ID);
            expect(converter.getSingleDataProduct).toHaveBeenCalled();
            expect(converter.getGridDataProduct).not.toHaveBeenCalled();
        });

        test('a product that cannot be shown in a grid sets the viewer back to single', async () => {
            const converter = makeConverter({canGrid: true});
            converter.getGridDataProduct.mockResolvedValue({...PRODUCT, gridNotSupported: true});
            makeDataProductsConverter.mockReturnValue(converter);
            setViewers({'image-viewer': IMAGE_VIEWER});
            getLayoutType.mockReturnValue(GRID);
            getLayoutDetails.mockReturnValue(GRID_FULL);
            watchDataProductsTable(highlight, {paused: false, firstTime: false});
            await flushPromises();
            expect(dispatchUpdateDataProducts).toHaveBeenCalledWith(DP_ID, {...PRODUCT, gridNotSupported: true});
            expect(dispatchChangeViewerLayout).toHaveBeenCalledWith('image-viewer', SINGLE, undefined, TBL_ID);
        });

        test('a promise result is shown, then the product it resolves to', async () => {
            const promiseResult = {displayType: DPtypes.PROMISE, promise: Promise.resolve(PRODUCT)};
            makeDataProductsConverter.mockReturnValue(makeConverter({
                getSingleDataProduct: jest.fn(() => Promise.resolve(promiseResult)),
            }));
            setViewers({'image-viewer': IMAGE_VIEWER});
            watchDataProductsTable(highlight, {paused: false, firstTime: false});
            await flushPromises();
            expect(dispatchUpdateDataProducts.mock.calls).toEqual([[DP_ID, promiseResult], [DP_ID, PRODUCT]]);
        });

        test('an error result shows the HTTP status and the URL', async () => {
            makeDataProductsConverter.mockReturnValue(makeConverter({
                getSingleDataProduct: jest.fn(() => Promise.resolve({displayType: DPtypes.ERROR,
                    error: new Error('Request failed, status 404: Not Found'), url: 'https://example.com/products'})),
            }));
            setViewers({'image-viewer': IMAGE_VIEWER});
            watchDataProductsTable(highlight, {paused: false, firstTime: false});
            await flushPromises();
            expect(dispatchUpdateDataProducts.mock.calls).toEqual([[DP_ID, {displayType: DPtypes.MESSAGE, menuKey: 'message-0',
                message: 'Error- Search for Data product failed, status 404 (Not Found), url: https://example.com/products'}]]);
        });

        test('an error with no HTTP status, or no result at all, shows only the plain error message', async () => {
            const plainError = {displayType: DPtypes.MESSAGE, menuKey: 'message-0', message: 'Error- Search for Data product failed'};
            makeDataProductsConverter.mockReturnValue(makeConverter({
                getSingleDataProduct: jest.fn()
                    .mockResolvedValueOnce({displayType: DPtypes.ERROR, error: new Error('Request timed out')})
                    .mockResolvedValueOnce(undefined),
            }));
            setViewers({'image-viewer': IMAGE_VIEWER});
            watchDataProductsTable(highlight, {paused: false, firstTime: false});
            await flushPromises();
            expect(dispatchUpdateDataProducts.mock.calls).toEqual([[DP_ID, plainError]]);
            watchDataProductsTable(highlight, {paused: false, firstTime: false});
            await flushPromises();
            expect(dispatchUpdateDataProducts.mock.calls).toEqual([[DP_ID, plainError], [DP_ID, plainError]]);
        });
    });
});
