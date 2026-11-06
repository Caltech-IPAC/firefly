import {FileAnalysisType, Format, TableDataType, UIEntry, UIRender} from '../../data/FileAnalysis.js';
import {RequestType} from '../../visualize/RequestType.js';
import {TitleOptions, WebPlotRequest} from '../../visualize/WebPlotRequest.js';
import {DPtypes, SHOW_CHART, SHOW_TABLE} from '../DataProductsType.js';
import {analyzePart} from '../PartAnalyzer.js';
import {
    createChartSingleRowArrayActivate, createChartTableActivate, createTableExtraction
} from '../TableDataProductUtils.js';

// Loading TableDataProductUtils pulls in UI components that Jest can't load (validator/es, canvas), so it is
// replaced with stubs; getExtractionText is copied from it.
jest.mock('../TableDataProductUtils.js', () => ({
    createChartSingleRowArrayActivate: jest.fn(),
    createChartTableActivate: jest.fn(),
    createTableExtraction: jest.fn(),
    getExtractionText: (tableDataType) => {
        if (tableDataType === 'Spectrum') return 'Pin Table/Spectrum';
        if (tableDataType === 'LightCurve') return 'Pin Table/LightCurve';
        return 'Pin Table';
    },
}));

const FILE_URL = 'https://irsa.ipac.caltech.edu/data/example/file.fits';
const SOURCE = '${upload-dir}/upload_123.fits'; // the server key of the uploaded file

/**
 * The details of a FITS part are its header cards, one row each: #, key, value, comment (FitsHDUUtil.getDetails).
 * @param {Array.<Array.<string>>} cards - [key, value] pairs
 * @return {Object}
 */
function makeFitsHeaderDetails(cards) {
    return {tableData: {
        columns: ['#', 'key', 'value', 'comment'].map((name) => ({name})),
        data: cards.map(([key, value], idx) => [String(idx), key, value, '']),
    }};
}

/**
 * The details of a table part are its columns, one row each: name, type, unit, desc, UCD (TableUtil.getDetails).
 * @param {Array.<Array.<string>>} columns - [name, type, unit, desc, UCD]
 * @return {Object}
 */
function makeTableColumnDetails(columns) {
    return {tableData: {
        columns: ['name', 'type', 'unit', 'desc', 'UCD'].map((name) => ({name})),
        data: columns,
    }};
}

// made up: a 2048 x 2048 image in the primary HDU
const IMAGE_PART = {
    type: FileAnalysisType.Image, index: 0, fileLocationIndex: 0, desc: 'W1 coadd', uiEntry: UIEntry.UseAll,
    uiRender: UIRender.NotSpecified,
    details: makeFitsHeaderDetails([['SIMPLE', 'true'], ['BITPIX', '-32'], ['NAXIS', '2'], ['NAXIS1', '2048'],
        ['NAXIS2', '2048']]),
};

// made up: a 1D spectrum stored as a 4000 pixel image in HDU 1, EXTNAME FLUX; the server adds the size to the desc
const ONE_D_IMAGE_PART = {
    type: FileAnalysisType.Image, index: 1, fileLocationIndex: 1, desc: 'FLUX (4000 x 1)', uiEntry: UIEntry.UseAll,
    uiRender: UIRender.NotSpecified,
    details: makeFitsHeaderDetails([['XTENSION', 'IMAGE'], ['BITPIX', '-32'], ['NAXIS', '1'], ['NAXIS1', '4000']]),
};

// made up: a 4000 x 3 image, three spectra of 4000 pixels, that can be shown as a table or an image
const THREE_ROW_IMAGE_PART = {
    type: FileAnalysisType.Image, index: 0, fileLocationIndex: 0, desc: 'spectra', uiEntry: UIEntry.UseAll,
    uiRender: UIRender.NotSpecified,
    details: makeFitsHeaderDetails([['SIMPLE', 'true'], ['BITPIX', '-32'], ['NAXIS', '2'], ['NAXIS1', '4000'],
        ['NAXIS2', '3']]),
};

// made up: a VOTable spectrum with wavelength and flux columns
const SPECTRUM_TABLE_PART = {
    type: FileAnalysisType.Table, index: 0, fileLocationIndex: 0, desc: 'spectrum', uiEntry: UIEntry.UseAll,
    uiRender: UIRender.NotSpecified, tableDataType: TableDataType.Spectrum, totalTableRows: 2048,
    details: makeTableColumnDetails([
        ['wavelength', 'double', 'um', 'wavelength', 'em.wl'],
        ['flux', 'double', 'mJy', 'flux density', 'phot.flux.density'],
        ['flux_err', 'double', 'mJy', 'flux uncertainty', 'stat.error'],
    ]),
};

// made up: an IPAC table light curve with mjd and mag columns
const LIGHT_CURVE_TABLE_PART = {
    type: FileAnalysisType.Table, index: 0, fileLocationIndex: 0, desc: 'light curve', uiEntry: UIEntry.UseAll,
    uiRender: UIRender.NotSpecified, tableDataType: TableDataType.NotSpecified, totalTableRows: 312,
    details: makeTableColumnDetails([
        ['mjd', 'double', 'd', '', ''],
        ['mag', 'double', 'mag', '', ''],
        ['magerr', 'double', 'mag', '', ''],
        ['filtercode', 'char', '', '', ''],
    ]),
};

// made up: a VOTable source list with no columns to chart
const SOURCE_LIST_TABLE_PART = {
    type: FileAnalysisType.Table, index: 0, fileLocationIndex: 0, desc: 'sources', uiEntry: UIEntry.UseAll,
    uiRender: UIRender.NotSpecified, tableDataType: TableDataType.NotSpecified, totalTableRows: 50,
    details: makeTableColumnDetails([
        ['source_id', 'char', '', '', 'meta.id'],
        ['name', 'char', '', '', ''],
    ]),
};

// made up: a FITS binary table with one row whose wavelength and flux columns are arrays
const SINGLE_ROW_FITS_TABLE_PART = {
    type: FileAnalysisType.Table, index: 1, fileLocationIndex: 1, desc: 'SPECTRUM', uiEntry: UIEntry.UseAll,
    uiRender: UIRender.NotSpecified, tableDataType: TableDataType.NotSpecified,
    details: makeFitsHeaderDetails([['XTENSION', 'BINTABLE'], ['NAXIS', '2'], ['NAXIS1', '32000'], ['NAXIS2', '1'],
        ['TTYPE1', 'WAVELENGTH'], ['TFORM1', '4000D'], ['TTYPE2', 'FLUX'], ['TFORM2', '4000D']]),
};

describe('PartAnalyzer:', () => {

    // clearMocks (jest config) keeps queued mock*Once values, so an unused one would leak into the next test
    beforeEach(() => jest.resetAllMocks());

    test('analyzePart: a header only part gives nothing', () => {
        const headerOnlyPart = {...IMAGE_PART, type: FileAnalysisType.HeaderOnly};

        expect(analyzePart({part: headerOnlyPart, request: WebPlotRequest.makeURIPlotRequest(FILE_URL), table: undefined,
            row: 0, fileFormat: Format.FITS, dlData: undefined, originalTitle: 'file.fits', source: undefined,
            activateParams: {imageViewerId: 'viewer-1'}, totalParts: 1, options: {}}))
            .toEqual({imageResult: false, tableResult: false});
    });

    test('analyzePart: a 2D image is an image of its HDU', () => {
        const request = WebPlotRequest.makeURIPlotRequest(FILE_URL);

        const {imageResult, tableResult} = analyzePart({part: IMAGE_PART, request, table: undefined, row: 0,
            fileFormat: Format.FITS, dlData: undefined, originalTitle: 'file.fits', source: undefined,
            activateParams: {imageViewerId: 'viewer-1'}, totalParts: 1, options: {}});

        expect(tableResult).toBe(false);
        expect(imageResult).toMatchObject({displayType: DPtypes.IMAGE, name: 'HDU #0 (image) W1 coadd',
            dropDownText: 'HDU #0 (image) W1 coadd', override: false, interpretedData: false});
        expect(imageResult.request).not.toBe(request); // a copy
        expect(imageResult.request.getURL()).toBe(FILE_URL);
        expect(imageResult.request.getParam('MultiImageExts')).toBe('0');
    });

    test('analyzePart: with a source the image is named by its description, and a DataLink row adds its semantics', () => {
        const dlData = {semantics: '#this', sourceObsCoreData: {obs_id: 'obs-1'}};

        const {imageResult} = analyzePart({part: IMAGE_PART, request: WebPlotRequest.makeURIPlotRequest(FILE_URL),
            table: undefined, row: 0, fileFormat: Format.FITS, dlData, originalTitle: 'file.fits', source: SOURCE,
            activateParams: {imageViewerId: 'viewer-1'}, totalParts: 1, options: {}});

        expect(imageResult).toMatchObject({name: 'W1 coadd (image)', dropDownText: 'W1 coadd (image) (#this)', dlData});
    });

    test('analyzePart: a converted file replaces the request file, and additional image params are added', () => {
        const convertedPart = {...IMAGE_PART, convertedFileName: '${upload-dir}/converted.fits',
            additionalImageParams: {PlotDescAppend: 'W1'}};

        const {imageResult} = analyzePart({part: convertedPart, request: WebPlotRequest.makeURIPlotRequest(FILE_URL),
            table: undefined, row: 0, fileFormat: Format.FITS, dlData: undefined, originalTitle: 'file.fits',
            source: undefined, activateParams: {imageViewerId: 'viewer-1'}, totalParts: 1, options: {}});

        expect(imageResult.override).toBe(true);
        const {request} = imageResult;
        expect(request.getRequestType()).toBe(RequestType.FILE);
        expect(request.getFileName()).toBe('${upload-dir}/converted.fits');
        expect(request.getTitle()).toBe('W1 coadd');
        expect(request.getTitleOptions()).toBe(TitleOptions.NONE);
        expect(request.getParam('PlotDescAppend')).toBe('W1');
    });

    test('analyzePart: an image part that says it is to be shown as a table gives no image', () => {
        const tableRenderPart = {...IMAGE_PART, uiEntry: UIEntry.UseSpecified, uiRender: UIRender.Table};

        const {imageResult} = analyzePart({part: tableRenderPart, request: WebPlotRequest.makeURIPlotRequest(FILE_URL),
            table: undefined, row: 0, fileFormat: Format.FITS, dlData: undefined, originalTitle: 'file.fits',
            source: undefined, activateParams: {imageViewerId: 'viewer-1'}, totalParts: 1, options: {}});

        expect(imageResult).toBeUndefined();
    });

    test('analyzePart: a 1D image is a chart of pixel value against index, not an image', () => {
        createChartTableActivate.mockReturnValueOnce('activate-chart-table');
        createTableExtraction.mockReturnValueOnce('extract-table');
        const activateParams = {imageViewerId: 'viewer-1'};

        const {imageResult, tableResult} = analyzePart({part: ONE_D_IMAGE_PART,
            request: WebPlotRequest.makeURIPlotRequest(FILE_URL), table: undefined, row: 0, fileFormat: Format.FITS,
            dlData: undefined, originalTitle: 'file.fits', source: SOURCE, activateParams, totalParts: 2, options: {}});

        expect(imageResult).toBe(false);
        const title = 'HDU #1 (1D image - show as table or chart) FLUX (4000 x 1)';
        expect(tableResult).toMatchObject({displayType: DPtypes.CHOICE_CTI, name: title,
            activate: 'activate-chart-table', extraction: 'extract-table', extractionText: 'Pin Table', paIdx: 1,
            chartTableDefOption: SHOW_CHART, dropDownText: `file.fits - ${title}`});
        // the pixel value column is named after the HDU
        const imageAsTableInfo = expect.objectContaining({colNames: ['naxis1_idx', 'FLUX'], colUnits: undefined,
            cubePlanes: 0});
        expect(createChartTableActivate).toHaveBeenCalledWith(expect.objectContaining({chartAndTable: true,
            source: SOURCE, tbl_index: 1, imageAsTableInfo,
            chartInfo: expect.objectContaining({xAxis: 'naxis1_idx', yAxis: 'FLUX', connectPoints: false,
                tableDataType: TableDataType.NotSpecified})}));
        expect(createTableExtraction).toHaveBeenCalledWith(SOURCE, title, 1, imageAsTableInfo, 0,
            TableDataType.NotSpecified);
    });

    test('analyzePart: the pixel value column of a 1D image with no HDU name is named pixel value', () => {
        const unnamedPart = {...ONE_D_IMAGE_PART, desc: ' (4000 x 1)'};

        analyzePart({part: unnamedPart, request: WebPlotRequest.makeURIPlotRequest(FILE_URL), table: undefined, row: 0,
            fileFormat: Format.FITS, dlData: undefined, originalTitle: 'file.fits', source: SOURCE,
            activateParams: {imageViewerId: 'viewer-1'}, totalParts: 2, options: {}});

        expect(createChartTableActivate.mock.calls[0][0].imageAsTableInfo.colNames).toEqual(['naxis1_idx', 'pixel value']);
    });

    test('analyzePart: an image with a few rows can be an image or a table', () => {
        const {imageResult, tableResult} = analyzePart({part: THREE_ROW_IMAGE_PART,
            request: WebPlotRequest.makeURIPlotRequest(FILE_URL), table: undefined, row: 0, fileFormat: Format.FITS,
            dlData: undefined, originalTitle: 'file.fits', source: SOURCE, activateParams: {imageViewerId: 'viewer-1'},
            totalParts: 1, options: {}});

        expect(imageResult).toMatchObject({displayType: DPtypes.IMAGE, name: 'spectra (image)'});
        expect(tableResult).toMatchObject({displayType: DPtypes.CHOICE_CTI,
            name: 'HDU #0 (2D image - show as table or chart or image) spectra', chartTableDefOption: SHOW_TABLE});
        expect(createChartTableActivate.mock.calls[0][0].imageAsTableInfo.colNames)
            .toEqual(['naxis1_idx', 'naxis1_data_0', 'naxis1_data_1', 'naxis1_data_2']);
    });

    test('analyzePart: a spectrum table is a chart and table, shown as a chart', () => {
        createChartTableActivate.mockReturnValueOnce('activate-chart-table');
        createTableExtraction.mockReturnValueOnce('extract-table');

        const {imageResult, tableResult} = analyzePart({part: SPECTRUM_TABLE_PART, request: undefined,
            table: undefined, row: 0, fileFormat: Format.VO_TABLE, dlData: undefined, originalTitle: 'spectrum.xml',
            source: SOURCE, activateParams: {}, totalParts: 1, options: {}});

        expect(imageResult).toBe(false);
        expect(tableResult).toMatchObject({displayType: DPtypes.CHOICE_CTI, name: 'spectrum.xml',
            dropDownText: 'spectrum.xml - spectrum.xml', extractionText: 'Pin Table/Spectrum',
            chartTableDefOption: SHOW_CHART, paIdx: 0});
        expect(createChartTableActivate.mock.calls[0][0].chartInfo).toMatchObject({hasChart: true,
            tableDataType: TableDataType.Spectrum, xAxis: 'wavelength', yAxis: 'flux', connectPoints: true});
    });

    test('analyzePart: a light curve table is charted with unconnected points', () => {
        const {tableResult} = analyzePart({part: LIGHT_CURVE_TABLE_PART, request: undefined, table: undefined, row: 0,
            fileFormat: Format.IPACTABLE, dlData: undefined, originalTitle: 'lc.tbl', source: SOURCE,
            activateParams: {}, totalParts: 1, options: {}});

        expect(tableResult).toMatchObject({displayType: DPtypes.CHOICE_CTI, extractionText: 'Pin Table/LightCurve',
            chartTableDefOption: SHOW_CHART});
        expect(createChartTableActivate.mock.calls[0][0].chartInfo).toMatchObject({hasChart: true,
            tableDataType: TableDataType.LightCurve, xAxis: 'mjd', yAxis: 'mag', connectPoints: false});
    });

    test('analyzePart: a table with nothing to chart is a table only', () => {
        createChartTableActivate.mockReturnValueOnce('activate-table');
        createTableExtraction.mockReturnValueOnce('extract-table');

        const {tableResult} = analyzePart({part: SOURCE_LIST_TABLE_PART, request: undefined, table: undefined, row: 0,
            fileFormat: Format.VO_TABLE, dlData: undefined, originalTitle: 'sources.xml', source: SOURCE,
            activateParams: {}, totalParts: 3, options: {}});

        expect(tableResult).toMatchObject({displayType: DPtypes.TABLE, name: 'Part #0 sources',
            dropDownText: 'sources.xml - Part #0 sources', activate: 'activate-table', extraction: 'extract-table',
            extractionText: 'Pin Table', paIdx: 0});
        expect(createChartTableActivate.mock.calls[0][0]).not.toHaveProperty('chartAndTable');
    });

    test('analyzePart: the table and chart ids can come from the options', () => {
        analyzePart({part: SPECTRUM_TABLE_PART, request: undefined, table: undefined, row: 0,
            fileFormat: Format.VO_TABLE, dlData: undefined, originalTitle: 'spectrum.xml', source: SOURCE,
            activateParams: {}, totalParts: 1,
            options: {tableIdList: [{description: 'spectrum', tbl_id: 'spectrum-tbl'}], chartIdBase: 'spectrum-chart',
                statefulTabComponentKey: 'tabs-key'}});

        expect(createChartTableActivate.mock.calls[0][0]).toMatchObject({tbl_id: 'spectrum-tbl',
            chartId: 'spectrum-chart', statefulTabComponentKey: 'tabs-key'});
    });

    test('analyzePart: a FITS table with one row of array columns is a single row chart', () => {
        createChartSingleRowArrayActivate.mockReturnValueOnce('activate-single-row');

        const {tableResult} = analyzePart({part: SINGLE_ROW_FITS_TABLE_PART, request: undefined, table: undefined,
            row: 0, fileFormat: Format.FITS, dlData: undefined, originalTitle: 'spec.fits', source: SOURCE,
            activateParams: {}, totalParts: 2, options: {}});

        expect(tableResult).toMatchObject({displayType: DPtypes.CHOICE_CTI, name: 'HDU #1 (table or chart) SPECTRUM',
            activate: 'activate-single-row', paIdx: 1, chartTableDefOption: SHOW_TABLE});
        expect(createChartSingleRowArrayActivate).toHaveBeenCalledWith(SOURCE, 'Single Row Chart', {},
            expect.objectContaining({xAxis: 'WAVELENGTH', yAxis: 'FLUX'}), 0, 1);
        expect(createChartTableActivate).not.toHaveBeenCalled();
    });

    test('analyzePart: a table part that is to be shown as a chart is a chart and table', () => {
        const chartPart = {...SPECTRUM_TABLE_PART, uiEntry: UIEntry.UseSpecified, uiRender: UIRender.Chart};

        expect(analyzePart({part: chartPart, request: undefined, table: undefined, row: 0, fileFormat: Format.VO_TABLE,
            dlData: undefined, originalTitle: 'spectrum.xml', source: SOURCE, activateParams: {}, totalParts: 1,
            options: {}}).tableResult.displayType).toBe(DPtypes.CHOICE_CTI);
    });

    test('analyzePart: a table part that is to be shown as a table is a table only, even if it could be charted', () => {
        const tablePart = {...SPECTRUM_TABLE_PART, uiEntry: UIEntry.UseSpecified, uiRender: UIRender.Table};

        const {tableResult} = analyzePart({part: tablePart, request: undefined, table: undefined, row: 0,
            fileFormat: Format.VO_TABLE, dlData: undefined, originalTitle: 'spectrum.xml', source: SOURCE,
            activateParams: {}, totalParts: 1, options: {}});

        expect(tableResult).toMatchObject({displayType: DPtypes.TABLE, name: 'spectrum.xml', extractionText: 'Pin Table'});
        expect(createChartTableActivate.mock.calls[0][0]).not.toHaveProperty('chartAndTable');
    });

    test('analyzePart: a table part that is to be shown as an image gives no table', () => {
        const imageRenderPart = {...SPECTRUM_TABLE_PART, uiEntry: UIEntry.UseSpecified, uiRender: UIRender.Image};

        expect(analyzePart({part: imageRenderPart, request: undefined, table: undefined, row: 0,
            fileFormat: Format.VO_TABLE, dlData: undefined, originalTitle: 'spectrum.xml', source: SOURCE,
            activateParams: {}, totalParts: 1, options: {}}).tableResult).toBeUndefined();
    });
});
