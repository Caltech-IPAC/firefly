

export const FileAnalysisType = {
    Image: 'Image',
    Table: 'Table',
    Spectrum: 'Spectrum',
    HeaderOnly: 'HeaderOnly',
    PDF: 'PDF',
    TAR: 'TAR',
    ErrorResponse: 'ErrorResponse',
    LoadInBrowser: 'LoadInBrowser',
    Unknown: 'UNKNOWN',
    HTML: 'HTML',
    REGION: 'REGION',
    PNG: 'PNG',
    UWS: 'UWS'
};

export const DataProductTypes = {
    spectrum : 'spectrum',
    image : 'image',
    cube : 'cube',
    sed : 'sed',
    timeseries : 'timeseries',
    visibility : 'visibility',
    event : 'event',
    mesurements : 'mesurements',
};

export const UIRender = {
    Table: 'Table',
    Chart: 'Chart',
    Image: 'Image',
    NotSpecified: 'NotSpecified'
};

export const UIEntry=  {
    UseSpecified: 'UseSpecified',
    UseGuess: 'UseGuess',
    UseAll : 'UseAll'
};


export const Format= {
    TSV: 'TSV',
    CSV: 'CSV',
    IPACTABLE: 'IPACTABLE',
    UNKNOWN: 'UNKNOWN',
    FIXEDTARGETS: 'FIXEDTARGETS',
    FITS: 'FITS',
    JSON: 'JSON',
    PDF: 'PDF',
    TAR: 'TAR',
    PNG: 'PNG',
    REGION: 'REGION',
    VO_TABLE: 'VO_TABLE',
    VO_TABLE_TABLEDATA: 'VO_TABLE_TABLEDATA',
    VO_TABLE_BINARY: 'VO_TABLE_BINARY',
    VO_TABLE_BINARY2: 'VO_TABLE_BINARY2',
    VO_TABLE_FITS: 'VO_TABLE_FITS'
};

export const ChartType = {
    XYChart : 'XYChart',
    Histogram : 'Histogram',
};


export const makeFileAnalysisReport= (type) => (
    {
        type,
        filePath: undefined,
        fileName: undefined,
        fileSize: undefined,
        fileFormat: undefined,
        dataType: undefined,
        parts: [
            makeFileAnalysisPart(0),
        ]
    });


export const makeFileAnalysisPart= (index,fileLocationIndex=0) => (
    {
        index,
        fileLocationIndex,
        uiEntry: UIEntry.UseAll,
        uiRender: UIRender.NotSpecified,
        convertedFileName: undefined,
        chartParams: undefined,
        tableColumnNames: undefined,
        tableColumnUnits: undefined,
        defaultPart: false,
    } );




/**
 * @global
 * @public
 * @typedef {Object} ChartInfo
 *
 * @prop {string} xAxis
 * @prop {string} yAxis
 * @prop {Array.<FileAnalysisChartParams>} chartParamsAry
 * @prop {boolean} useChartChooser
 * @props {Array.<string>} [cUnits]
 * @props {Array.<string>} [cNames]
 *
 */


/**
 * @global
 * @public
 * @typedef {Object} FileAnalysisReport
 *
 * @prop {string} type
 * @prop {number} fileSize - size in bytes of the file
 * @prop {string} filePath
 * @prop {string} fileFormat - format of the file
 * @prop {Array.<FileAnalysisPart>} parts
 * @prop {string} dataType
 * @prop {boolean} [disableAllImagesOption] - if true the the UI should not show an "All Images" options
 * @prop {string} [dataProductsAnalyzerId] - the id of the data products analyzer
 * @prop {boolean} analyzerFound - an data products analyzer was found and further processed this report
 *
 */

/**
 * @global
 * @public
 * @typedef {Object} FileAnalysisPart
 *
 *  @prop {string} type - see FileAnalysisType
 *  @prop {string} uiRender - see UIRender object
 *  @prop {string} uiEntry - see UIEntry object
 *  @prop {number} index - index of the part (may be different from fileLocationIndex)
 *  @prop {string} desc
 *  @prop {number} fileLocationIndex - either the FITS HDU number or some other location scheme
 *  @prop {string} [convertedFileName] - only set if this entry is has a alternate file than the one analyzed
 *  @prop {string} [convertedFileFormat] - format string
 *  @prop {FileAnalysisChartParams} [chartParams]
 *  @prop {Array.<string>} [tableColumnNames] only use for a fits image that is read as a table
 *  @prop {Array.<string>} [tableColumnUnits] only use for a fits image that is read as a table
 *  @prop {boolean} [defaultPart]
 *  @prop {boolean} [interpretedData] - should be true if this is data that has been added to the original
 *  @prop {string} chartTableDefOption - only used if there is a chart on of 'auto', 'showChart', 'showTable', 'showImage'
 *  @prop {TableModel} [details]
 *  @prop {Number} [totalTableRows] if the part is a table the number of rows in the table, otherwise undefined
 *
 *
 */

/**
 * @global
 * @public
 * @typedef {Object} FileAnalysisChartParams
 *
 * @prop {boolean} layoutAdds
 * @prop {boolean} simpleData
 * @prop {string} [simpleChartType] - see ChartType
 * @prop {Array.<string>} [layout] - a layout to be add or to replace the default
 * @prop {string} [xAxisColName]
 * @prop {string} [yAxisColName]
 * @prop {string} [mode]
 * @prop {Array.<Object>} [traces] - if defined it replaces the default
 *
 */
