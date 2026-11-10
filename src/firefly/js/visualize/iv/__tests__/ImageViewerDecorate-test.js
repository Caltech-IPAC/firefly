/*eslint-env node, jest */

// ImageViewerLayout pulls in canvas-based tile rendering that jsdom can't load; contextToolbar doesn't use it
jest.mock('firefly/visualize/ImageViewerLayout.jsx', () => ({ImageViewerLayout: () => null}));
// VisMiniToolbar draws color table images at load time, which jsdom can't do; contextToolbar doesn't use it
jest.mock('firefly/visualize/ui/VisMiniToolbar.jsx', () => ({expand: jest.fn(), VisMiniToolbar: () => null}));
// catalog selections are read from the table store; only the selection info matters here
jest.mock('firefly/tables/TableUtil', () => ({
    ...jest.requireActual('firefly/tables/TableUtil'),
    getTblById: jest.fn(),
}));

import {getTblById} from 'firefly/tables/TableUtil';
import {makeContextToolbar} from 'firefly/visualize/iv/ImageViewerDecorate.jsx';
import {VisCtxToolbarView} from 'firefly/visualize/ui/VisCtxToolbarView';
import {PlotAttribute} from 'firefly/visualize/PlotAttribute.js';
import {AREA_SELECT, LINE_SELECT, POINT} from 'firefly/core/ExternalAccessUtils.js';
import Catalog, {CatalogType} from 'firefly/drawingLayers/Catalog.js';

const PLOT_ID= 'test-plot';

const areaExt= {id: 'area', extType: AREA_SELECT};
const lineExt= {id: 'line', extType: LINE_SELECT};
const pointExt= {id: 'point', extType: POINT};

const selectedCatalogLayer= {drawLayerTypeId: Catalog.TYPE_ID, catalogType: CatalogType.POINT, canSelect: true,
    tbl_id: 'selected-tbl', visiblePlotIdAry: [PLOT_ID], plotIdAry: [PLOT_ID]};
const filteredCatalogLayer= {drawLayerTypeId: Catalog.TYPE_ID, catalogType: CatalogType.POINT, canSelect: false,
    tableRequest: {filters: 'ra > 10'}, tbl_id: 'filtered-tbl', visiblePlotIdAry: [PLOT_ID], plotIdAry: [PLOT_ID]};

/**
 * Build a plot view from the given inputs, call contextToolbar, and return the props of the toolbar it renders
 * @param {object} props
 * @param {string} props.plotType - 'image' or 'hips'
 * @param {number} props.plotCnt - number of plots in the plot view, more than 1 shows the multi-image controller for images
 * @param {object} props.attributes - plot attributes, e.g. the active distance or point tool
 * @param {boolean} props.hipsFits - can the plot view convert between HiPS and FITS
 * @param {boolean} props.embedMainToolbar - is the main toolbar embedded in the context toolbar
 * @param {Array.<object>} props.dlAry - draw layers
 * @param {Array.<object>} props.extensionList - toolbar extensions
 * @return {object|undefined} the VisCtxToolbarView props, or undefined when no toolbar is rendered
 */
function contextToolbarProps({plotType, plotCnt, attributes, hipsFits, embedMainToolbar, dlAry, extensionList}) {
    const plots= Array.from({length: plotCnt}, (_, idx) => ({plotId: PLOT_ID, plotImageId: `img-${idx}`, plotType, attributes, cubeDepth: 1}));
    const plotView= {plotId: PLOT_ID, primeIdx: 0, plots, viewDim: {width: 500, height: 500}, overlayPlotViews: [],
        plotViewCtx: {
            embedMainToolbar,
            hipsImageConversion: hipsFits ? {hipsRequestRoot: {}, imageRequestRoot: {}} : undefined,
        },
    };
    const toolbar= makeContextToolbar(plotView, dlAry, extensionList, 500, () => null);
    if (!toolbar) return undefined;
    expect(toolbar.type).toBe(VisCtxToolbarView);
    return toolbar.props;
}

describe('makeContextToolbar', () => {

    beforeEach(() => {
        getTblById.mockImplementation((tbl_id) =>
            tbl_id==='selected-tbl' ? {selectInfo: {exceptions: new Set([3]), selectAll: false}} : undefined);
    });

    test('no plot renders no toolbar', () => {
        expect(contextToolbarProps({plotType: 'image', plotCnt: 0, attributes: {}, hipsFits: false,
            embedMainToolbar: false, dlAry: [], extensionList: []})).toBeUndefined();
    });

    test('single image with nothing active renders no toolbar', () => {
        expect(contextToolbarProps({plotType: 'image', plotCnt: 1, attributes: {}, hipsFits: false,
            embedMainToolbar: false, dlAry: [], extensionList: [lineExt, pointExt]})).toBeUndefined();
    });

    test('area selection shows the selection tools and only the area extensions', () => {
        const props= contextToolbarProps({plotType: 'image', plotCnt: 1, attributes: {[PlotAttribute.SELECTION]: {}},
            hipsFits: false, embedMainToolbar: false, dlAry: [selectedCatalogLayer], extensionList: [areaExt, lineExt]});
        expect(props.showSelectionTools).toBe(true);
        expect(props.extensionAry).toEqual([areaExt]);
        expect(props.showCatUnSelect).toBe(true);
    });

    test('active distance tool shows only the line extensions', () => {
        const props= contextToolbarProps({plotType: 'image', plotCnt: 1, attributes: {[PlotAttribute.ACTIVE_DISTANCE]: {}},
            hipsFits: false, embedMainToolbar: false, dlAry: [], extensionList: [lineExt, pointExt]});
        expect(props.extensionAry).toEqual([lineExt]);
        expect(props.showSelectionTools).toBeUndefined();
    });

    test('active point tool shows only the point extensions', () => {
        const props= contextToolbarProps({plotType: 'image', plotCnt: 1, attributes: {[PlotAttribute.ACTIVE_POINT]: {}},
            hipsFits: false, embedMainToolbar: false, dlAry: [], extensionList: [lineExt, pointExt]});
        expect(props.extensionAry).toEqual([pointExt]);
    });

    test('active distance tool keeps its extensions on a multi-image plot', () => {
        const props= contextToolbarProps({plotType: 'image', plotCnt: 2, attributes: {[PlotAttribute.ACTIVE_DISTANCE]: {}},
            hipsFits: false, embedMainToolbar: false, dlAry: [], extensionList: [lineExt]});
        expect(props.extensionAry).toEqual([lineExt]);
        expect(props.showMultiImageController).toBe(true);
    });

    test('active distance tool with no extensions falls through to the catalog unselect', () => {
        const props= contextToolbarProps({plotType: 'image', plotCnt: 1, attributes: {[PlotAttribute.ACTIVE_DISTANCE]: {}},
            hipsFits: false, embedMainToolbar: false, dlAry: [selectedCatalogLayer], extensionList: [pointExt]});
        expect(props.extensionAry).toEqual([]);
        expect(props.showCatUnSelect).toBe(true);
    });

    test('catalog selection shows unselect', () => {
        const props= contextToolbarProps({plotType: 'image', plotCnt: 1, attributes: {}, hipsFits: false,
            embedMainToolbar: false, dlAry: [selectedCatalogLayer], extensionList: []});
        expect(props.showCatUnSelect).toBe(true);
        expect(props.showClearFilter).toBe(false);
    });

    test('catalog filter shows clear filter', () => {
        const props= contextToolbarProps({plotType: 'image', plotCnt: 1, attributes: {}, hipsFits: false,
            embedMainToolbar: false, dlAry: [filteredCatalogLayer], extensionList: []});
        expect(props.showCatUnSelect).toBe(false);
        expect(props.showClearFilter).toBe(true);
    });

    test('multi-image plot shows the default toolbar with the multi-image controller', () => {
        const props= contextToolbarProps({plotType: 'image', plotCnt: 2, attributes: {}, hipsFits: false,
            embedMainToolbar: false, dlAry: [], extensionList: []});
        expect(props.showMultiImageController).toBe(true);
        expect(props.extensionAry).toEqual([]);
    });

    test('HiPS plot shows the default toolbar', () => {
        const props= contextToolbarProps({plotType: 'hips', plotCnt: 1, attributes: {}, hipsFits: false,
            embedMainToolbar: false, dlAry: [], extensionList: []});
        expect(props.showMultiImageController).toBe(false);
        expect(props.extensionAry).toEqual([]);
    });

    test('HiPS/FITS conversion shows the default toolbar', () => {
        const props= contextToolbarProps({plotType: 'image', plotCnt: 1, attributes: {}, hipsFits: true,
            embedMainToolbar: false, dlAry: [], extensionList: []});
        expect(props.extensionAry).toEqual([]);
    });

    test('embedded main toolbar always renders the toolbar', () => {
        const props= contextToolbarProps({plotType: 'image', plotCnt: 1, attributes: {}, hipsFits: false,
            embedMainToolbar: true, dlAry: [], extensionList: []});
        expect(props.makeToolbar).toBeInstanceOf(Function);
    });
});
