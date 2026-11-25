import {FileAnalysisType, Format} from '../../data/FileAnalysis.js';
import {MetaConst} from '../../data/MetaConst.js';
import {
    createObsCoreProductTitle, createObsCoreProductTitleAndSource, ensureDropDownText, getAnalysisAllImageDesc,
    getAnalysisPartTableTitling, getAnalysisSSATitle, getBasicTitling, getCutoutTitleFromTitle, getSerDescTitling,
    getSpectrumTitle, makeDatalinkTitles, makeDataTooLargeDLMessage, makeObsCoreFailDownloadTitling,
    makeObsTitleExtension
} from '../VoUITitles.js';

const OBSCORE_COLUMNS = ['access_url', 'access_format', 'dataproduct_type', 'obs_collection', 'obs_id',
    'instrument_name', 'obs_title'];

/**
 * A one row ObsCore table; a column is left out when its value is undefined.
 * @param {Object} rowValues - value by column name
 * @param {Object} tableMeta
 * @return {TableModel}
 */
function makeObsCoreTable(rowValues, tableMeta = {}) {
    const columnNames = OBSCORE_COLUMNS.filter((name) => rowValues[name] !== undefined);
    return {
        tbl_id: 'obscore-tbl', tableMeta, highlightedRow: 0,
        tableData: {columns: columnNames.map((name) => ({name})), data: [columnNames.map((name) => rowValues[name])]},
    };
}

// made up: a CADC OMM row with no obs_title, so its title is computed from the collection, instrument and id
const OMM_ROW = {access_url: 'https://ws.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/caom2ops/datalink?ID=x',
    access_format: 'application/x-votable+xml;content=datalink', dataproduct_type: 'image',
    obs_collection: 'OMM', obs_id: 'C140117_0290', instrument_name: 'SPIOMM'};
const TITLED_ROW = {...OMM_ROW, obs_title: 'Orion nebula'};

// made up: an SSA spectrum table, found by its coverage and title utypes
const SSA_TABLE = {
    tbl_id: 'ssa-tbl', highlightedRow: 1,
    tableData: {
        columns: [{name: 'pos', utype: 'ssa:Char.SpatialAxis.Coverage.Location.Value'},
            {name: 'title', utype: 'ssa:DataID.Title'}],
        data: [['10.1 41.2', 'M31 spectrum'], ['10.7 41.3', 'M32 spectrum']],
    },
};

describe('getAnalysisSSATitle', () => {
    test('returns the title of the row from the SSA title column', () => {
        expect(getAnalysisSSATitle(SSA_TABLE, 1)).toBe('M32 spectrum');
    });

    test('returns undefined when the table has no SSA title column', () => {
        expect(getAnalysisSSATitle(makeObsCoreTable(OMM_ROW), 0)).toBeUndefined();
    });
});

describe('getSpectrumTitle', () => {
    test('is the spectrum menu title', () => {
        expect(getSpectrumTitle()).toBe('Show: Spectrum');
    });
});

describe('getCutoutTitleFromTitle', () => {
    test('prefixes the title', () => {
        expect(getCutoutTitleFromTitle('OMM, SPIOMM, C140117_0290')).toBe('Cutout: OMM, SPIOMM, C140117_0290');
    });
});

describe('ensureDropDownText', () => {
    test('keeps a drop down text, or makes one from the title', () => {
        expect(ensureDropDownText('Cutout', 'Cutout of obs 42')).toBe('Cutout of obs 42');
        expect(ensureDropDownText('Cutout', undefined)).toBe('Show: Cutout');
    });
});

describe('makeObsCoreFailDownloadTitling', () => {
    test('uses the cause of the error, or the reason itself', () => {
        const table = makeObsCoreTable(OMM_ROW);
        const error = new Error('fetch failed', {cause: '404 Not Found'});
        expect(makeObsCoreFailDownloadTitling(table, 0, error)).toEqual({
            message: 'No data to display: Could not load datalink data, 404 Not Found',
            title: 'Download File: OMM, SPIOMM, C140117_0290'});
        expect(makeObsCoreFailDownloadTitling(table, 0, 'timeout').message)
            .toBe('No data to display: Could not load datalink data, timeout');
    });
});

describe('makeDataTooLargeDLMessage', () => {
    test('has the product title', () => {
        expect(makeDataTooLargeDLMessage(makeObsCoreTable(TITLED_ROW), 0))
            .toEqual({message: 'Data is too large to load', titleStr: 'Download File: Orion nebula'});
    });
});

describe('getBasicTitling', () => {
    test.each([
        [Format.JSON, 'Show JSON file (obs 42)', 'spec (obs 42) (JSON file)', ''],
        [Format.YAML, 'Show YAML file (obs 42)', 'spec (obs 42) (YAML file)', ''],
        [Format.TEXT, 'Show plain text file (obs 42)', 'spec (obs 42) (plain text file)', ''],
        [Format.GZIP, 'Download GZip file (obs 42)', 'spec (obs 42) (GZip file)',
            'This is a GZip file. It may only be downloaded'],
        [Format.TAR, 'Download TAR file (obs 42)', 'spec (obs 42) (TAR file)',
            'This is a TAR file. It may only be downloaded'],
        [Format.PDF, 'Open PDF file (obs 42)', 'spec (obs 42) (PDF file)',
            'This is a PDF file. It may be downloaded or opened in another tab'],
        [Format.HTML, 'Open HTML file (obs 42)', 'spec (obs 42) (HTML file)',
            'This is a web page or web application. It can be opened in another tab'],
        [Format.UNKNOWN, 'Download Unknown file (obs 42)', 'spec (obs 42) (Unknown file)',
            'This file may only be downloaded'],
    ])('titles a %s file', (fileType, title, dropDownText, message) => {
        expect(getBasicTitling('spec', 'obs 42', fileType)).toMatchObject({title, dropDownText, message});
    });

    test('a PDF or HTML file can be opened in the browser', () => {
        expect(getBasicTitling('spec', 'obs 42', Format.PDF).loadInBrowserMsg).toBe('Open PDF File (obs 42)');
        expect(getBasicTitling('spec', 'obs 42', Format.HTML).loadInBrowserMsg).toBe('Open Page (obs 42)');
        expect(getBasicTitling('spec', 'obs 42', Format.TAR).loadInBrowserMsg).toBeUndefined();
    });

    test('a PNG image is titled by its name', () => {
        expect(getBasicTitling('preview', 'obs 42', Format.PNG)).toMatchObject(
            {title: 'preview (obs 42) (PNG image)', dropDownText: 'preview (obs 42) (PNG image file)'});
        expect(getBasicTitling(undefined, 'obs 42', Format.PNG).title).toBeUndefined();
    });

    test('with no name or observation title there is no drop down text and no extension', () => {
        expect(getBasicTitling(undefined, undefined, Format.TAR))
            .toMatchObject({title: 'Download TAR file', dropDownText: undefined});
    });
});

describe('getAnalysisAllImageDesc', () => {
    test('names all the images, or the only one', () => {
        const parts = [{type: FileAnalysisType.HeaderOnly, desc: 'primary'},
            {type: FileAnalysisType.Image, desc: 'SCI'}, {type: FileAnalysisType.Image, desc: 'ERR'}];
        expect(getAnalysisAllImageDesc(parts, 2)).toBe('All Images in File');
        expect(getAnalysisAllImageDesc(parts, 1)).toBe('Image SCI');
        expect(getAnalysisAllImageDesc([{type: FileAnalysisType.Table}], 0)).toBe('Image ');
    });
});

describe('getAnalysisPartTableTitling', () => {
    test('interpreted data keeps its title', () => {
        expect(getAnalysisPartTableTitling({title: 'Spectrum', part: {index: 1, interpretedData: true},
            fileFormat: Format.FITS})).toEqual({title: 'Spectrum', dropDownText: 'Spectrum'});
    });

    test('a FITS table is titled by its HDU', () => {
        expect(getAnalysisPartTableTitling({title: 'EVENTS', part: {index: 1}, fileFormat: Format.FITS,
            originalTitle: 'obs 42'})).toEqual({title: 'HDU #1 (table or chart) EVENTS',
            dropDownText: 'obs 42 - HDU #1 (table or chart) EVENTS'});
    });

    test('a FITS image shown as a table says if it is 1D or 2D', () => {
        expect(getAnalysisPartTableTitling({title: 'FLUX', part: {index: 2}, fileFormat: Format.FITS,
            useImageAsTable: true, imageAsTableColCnt: 2}).title)
            .toBe('HDU #2 (1D image - show as table or chart) FLUX');
        expect(getAnalysisPartTableTitling({title: 'SPECTRA', part: {index: 0}, fileFormat: Format.FITS,
            useImageAsTable: true, imageAsTableColCnt: 4}).title)
            .toBe('HDU #0 (2D image - show as table or chart or image) SPECTRA');
    });

    test('an SSA table is titled by the highlighted row title', () => {
        expect(getAnalysisPartTableTitling({title: 'results', part: {index: 0}, fileFormat: Format.VO_TABLE,
            table: SSA_TABLE}).title).toBe('M32 spectrum');
    });

    test('other tables use the original title when alone, or a part number', () => {
        const table = makeObsCoreTable(OMM_ROW);
        expect(getAnalysisPartTableTitling({title: 'sources', part: {index: 0}, fileFormat: Format.VO_TABLE,
            table, totalParts: 1, originalTitle: 'obs 42'}).title).toBe('obs 42');
        expect(getAnalysisPartTableTitling({title: 'sources', part: {index: 1}, fileFormat: Format.VO_TABLE,
            table, totalParts: 3}).title).toBe('Part #1 sources');
    });
});

describe('createObsCoreProductTitleAndSource', () => {
    test('a table that is not ObsCore like has no title', () => {
        const table = makeObsCoreTable({obs_id: 'C140117_0290', obs_title: 'Orion nebula'});
        expect(createObsCoreProductTitleAndSource(table, 0)).toEqual({source: 'none', title: ''});
    });

    test('a product title template from the data service options is used first', () => {
        const options = JSON.stringify({productTitleTemplate: '${obs_collection} ${obs_id}'});
        const table = makeObsCoreTable(TITLED_ROW, {[MetaConst.DATA_SERVICE_OPTIONS]: options});
        expect(createObsCoreProductTitleAndSource(table, 0)).toEqual({source: 'template', title: 'OMM C140117_0290'});
    });

    test('an empty template turns off titles', () => {
        const options = JSON.stringify({productTitleTemplate: ''});
        const table = makeObsCoreTable(TITLED_ROW, {[MetaConst.DATA_SERVICE_OPTIONS]: options});
        expect(createObsCoreProductTitleAndSource(table, 0)).toEqual({source: 'none', title: ''});
    });

    test('a template with a column the table does not have is skipped', () => {
        const options = JSON.stringify({productTitleTemplate: '${target_name}'});
        const table = makeObsCoreTable(TITLED_ROW, {[MetaConst.DATA_SERVICE_OPTIONS]: options});
        expect(createObsCoreProductTitleAndSource(table, 0)).toEqual({source: 'obsTitle', title: 'Orion nebula'});
    });

    test('obs_title is used next', () => {
        expect(createObsCoreProductTitleAndSource(makeObsCoreTable(TITLED_ROW), 0))
            .toEqual({source: 'obsTitle', title: 'Orion nebula'});
    });

    test('otherwise the title is made from the collection, instrument and id', () => {
        expect(createObsCoreProductTitleAndSource(makeObsCoreTable(OMM_ROW), 0))
            .toEqual({source: 'computed', title: 'OMM, SPIOMM, C140117_0290'});
    });

    test('the collection is left out when it is the instrument name', () => {
        const table = makeObsCoreTable({...OMM_ROW, obs_collection: 'SPIOMM'});
        expect(createObsCoreProductTitleAndSource(table, 0).title).toBe('SPIOMM, C140117_0290');
    });
});

describe('createObsCoreProductTitle', () => {
    test('returns just the title', () => {
        expect(createObsCoreProductTitle(makeObsCoreTable(OMM_ROW), 0)).toBe('OMM, SPIOMM, C140117_0290');
    });
});

describe('makeObsTitleExtension', () => {
    test('puts a short observation title in parentheses', () => {
        expect(makeObsTitleExtension('obs 42')).toBe(' (obs 42)');
        expect(makeObsTitleExtension(undefined)).toBe('');
    });

    test('shortens an observation title longer than 25 characters to 25 with an ellipsis', () => {
        expect(makeObsTitleExtension('Orion nebula, north field')).toBe(' (Orion nebula, north field)'); // 25
        expect(makeObsTitleExtension('Orion nebula, north field 2')).toBe(' (Orion nebula, north fiel…)'); // 27
    });

    test('does not leave a space before the ellipsis', () => {
        expect(makeObsTitleExtension('Orion nebula, north fie 2')).toBe(' (Orion nebula, north fie 2)'); // 25
        expect(makeObsTitleExtension('Orion nebula, north fie 2nd')).toBe(' (Orion nebula, north fie…)'); // cut after 'fie '
    });
});

describe('makeDatalinkTitles', () => {
    const OMM_ID = 'ivo://cadc.nrc.ca/OMM?C140117_0290/C140117_0290_SCI';

    test('the primary product of a row with an obs_title is named for it', () => {
        const dlData = {id: OMM_ID, semantics: '#this', dlAnalysis: {isThis: true}};
        expect(makeDatalinkTitles({dlData, sourceTable: makeObsCoreTable(TITLED_ROW), sourceRow: 0})).toEqual({
            name: 'Orion nebula (#this)', prodTitle: 'Orion nebula (#this)',
            imageGridTitle: 'Orion nebula (#this)', imageRelatedTitle: 'Orion nebula (#this)'});
    });

    test('a second primary product is numbered', () => {
        const dlData = {id: OMM_ID, semantics: '#this', dlAnalysis: {isThis: true}};
        expect(makeDatalinkTitles({dlData, indices: {'#this': 2}, sourceTable: makeObsCoreTable(TITLED_ROW),
            sourceRow: 0}).name).toBe('Orion nebula (#this 2)');
    });

    test('other products are named for their semantics, numbered when there are more than one', () => {
        const dlData = {id: OMM_ID, semantics: '#preview', dlAnalysis: {}};
        const sourceTable = makeObsCoreTable(TITLED_ROW);
        expect(makeDatalinkTitles({dlData, sourceTable, sourceRow: 0}).name).toBe('Preview: Orion nebula');
        expect(makeDatalinkTitles({dlData, totals: {'#preview': 3}, indices: {'#preview': 2}, sourceTable,
            sourceRow: 0}).name).toBe('Preview 2: Orion nebula');
    });

    test('a short label is added to the product title', () => {
        const dlData = {id: OMM_ID, semantics: '#this', labelDLExt: 'r band', dlAnalysis: {isThis: true}};
        expect(makeDatalinkTitles({dlData, sourceTable: makeObsCoreTable(TITLED_ROW), sourceRow: 0})).toEqual({
            name: 'Orion nebula (#this)', prodTitle: 'r band - Orion nebula',
            imageGridTitle: 'r band', imageRelatedTitle: 'r band'});
    });

    test('a usable description is used before a computed title', () => {
        const dlData = {id: OMM_ID, semantics: '#calibration', description: 'Flat field for the night',
            dlAnalysis: {}};
        expect(makeDatalinkTitles({dlData, sourceTable: makeObsCoreTable(OMM_ROW), sourceRow: 0}).name)
            .toBe('Calibration: Flat field for the night');
    });

    test.each([
        ['starts with a verb', 'Download the calibrated image'],
        ['has a URL', 'see https://irsa.ipac.caltech.edu/x'],
        ['has a file name', 'calibrated image C140117_0290.fits'],
        ['is a file path', 'data/omm/2014/C140117_0290'],
        ['is too short', 'cal'],
    ])('a description that %s is not used', (label, description) => {
        const dlData = {id: OMM_ID, semantics: '#calibration', description, dlAnalysis: {}};
        expect(makeDatalinkTitles({dlData, sourceTable: makeObsCoreTable(OMM_ROW), sourceRow: 0}).name)
            .toBe('Calibration: OMM, SPIOMM, C140117_0290');
    });

    test('with no source table the title comes from the ivo id', () => {
        const dlData = {id: OMM_ID, semantics: '#this', dlAnalysis: {isThis: true}};
        expect(makeDatalinkTitles({dlData}).name).toBe('C140117_0290/C140117_0290_SCI (#this)');
        const pathIdData = {id: 'ivo://irsa.ipac/spherex/level2/obs_7', semantics: '#this', dlAnalysis: {isThis: true}};
        expect(makeDatalinkTitles({dlData: pathIdData}).name).toBe('spherex/level2/obs_7 (#this)');
    });

    test('the service descriptor title is the last title tried', () => {
        const dlData = {id: 'obs-42', semantics: '#cutout', serDef: {title: 'SODA cutout service'}, dlAnalysis: {}};
        expect(makeDatalinkTitles({dlData}).name).toBe('Cutout: SODA cutout service');
    });

    test('with no title at all the primary products are named primary', () => {
        const thisData = {id: 'obs-42', semantics: '#this', dlAnalysis: {isThis: true}};
        expect(makeDatalinkTitles({dlData: thisData})).toEqual({name: 'Primary product (#this)',
            prodTitle: 'Primary', imageRelatedTitle: 'Primary', imageGridTitle: 'Primary'});
        expect(makeDatalinkTitles({dlData: thisData, indices: {'#this': 1}}).name).toBe('Primary product (#this 1)');
    });
});

describe('makeDatalinkTitles related data fallback', () => {
    test('with no title at all other products are named related data', () => {
        const auxData = {id: 'obs-42', semantics: '#auxiliary', dlAnalysis: {}};
        expect(makeDatalinkTitles({dlData: auxData})).toEqual({name: 'Auxiliary: Related Data',
            prodTitle: 'Auxiliary: Related Data', imageGridTitle: 'Auxiliary: Related Data',
            imageRelatedTitle: 'Auxiliary: Related Data'});
    });

    test('a related data product keeps its label', () => {
        const auxData = {id: 'obs-42', semantics: '#auxiliary', labelDLExt: 'r band', dlAnalysis: {}};
        expect(makeDatalinkTitles({dlData: auxData})).toEqual({name: 'Auxiliary: Related Data',
            prodTitle: 'r band - Related Data', imageGridTitle: 'r band', imageRelatedTitle: 'r band'});
    });
});

describe('getSerDescTitling', () => {
    test('names the service from the source row', () => {
        const serDef = {title: 'SODA cutout service'};
        expect(getSerDescTitling(serDef, makeObsCoreTable(TITLED_ROW), 0).name).toBe('Orion nebula');
    });
});
