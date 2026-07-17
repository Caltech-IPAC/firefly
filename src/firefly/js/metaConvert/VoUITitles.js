import {FileAnalysisType, Format} from '../data/FileAnalysis';
import {getCellValue, getColumns} from '../tables/TableUtil';
import {getDataServiceOptionByTable} from '../ui/tap/DataServicesOptions';
import {tokenSub} from '../util/WebUtil';
import {getObsTitle, hasObsCoreLikeDataProducts, isSSATable} from '../voAnalyzer/TableAnalysis';
import {SSA_TITLE_UTYPE} from '../voAnalyzer/VoConst';
import {getTableModel} from '../voAnalyzer/VoCoreUtils';



export function getAnalysisSSATitle(tableOrId, row) {
    const table = getTableModel(tableOrId);
    if (!table) return false;
    const foundCol = table.tableData.columns
        .filter((c) => {
            if (c?.utype?.toLowerCase().includes(SSA_TITLE_UTYPE)) return true;
        });
    return foundCol.length > 0 ? getCellValue(table, row, foundCol[0].name) : undefined;
}

export const getSpectrumTitle= () => 'Show: Spectrum';

export const getCutoutTitleFromTitle= (baseTitle) => 'Cutout: ' + baseTitle;

/**
 * @param {ServiceDescriptorDef} serDef
 * @param {TableModel} sourceTable
 * @param {number} sourceRow
 * @return {string}
 */
export function getSerDescTitling(serDef,sourceTable,sourceRow) {
    const ret= makeDatalinkTitles({dlData:{serDef},sourceTable,sourceRow});
    return {...ret,dropDownText:'Show: '+ret.name};

}


/**
 *
 * @param {TableModel }table
 * @param {number} row
 * @param {Error} reason
 * @return {{message:string,title:string}}
 */
export function makeObsCoreFailDownloadTitling(table, row, reason) {
    return {
        message: `No data to display: Could not retrieve datalink data, ${reason}`,
        title: 'Download File: ' + createObsCoreProductTitle(table,row)
    };
}

export function makeDataTooLargeDLMessage(table, row) {
    const prodTitle= createObsCoreProductTitle(table,row);
    return {message:'Data is too large to load', titleStr: 'Download File: '+prodTitle};
}

export const ensureDropDownText = (title, dropDownText) => dropDownText ?? 'Show: '+title;

/**
 * get titles and descriptions for more simple data types
 * @param {string} title
 * @param {string} obsTitle
 * @param {string} fileType
 * @return {{title: string, dropDownText: string, message: string, loadInBrowserMsg: string|undefined}}
 */
export function getBasicTitling(title,obsTitle='',fileType='') {
    const obsTitleMsg= makeObsTitleExtension(obsTitle);

    const defResult= (fileLabel, verb= 'Show') => (
        {
            title: `${verb} ${fileLabel} file${obsTitleMsg}`,
            dropDownText:  title ? `${title}${obsTitleMsg} (${fileLabel} file)` : undefined,
            message: '',
            loadInBrowserMsg: undefined
        }
    );

    switch (fileType) {
        case Format.PNG : return {
            ...defResult('PNG image'),
            title: title ? `${title}${obsTitleMsg} (PNG image)` : undefined,
        };

        case Format.JSON : return {...defResult('JSON')};
        case Format.YAML : return {...defResult('YAML')};
        case Format.TEXT : return {...defResult('plain text')};
        case Format.GZIP : return {...defResult('GZip', 'Download'),
            message: 'This is a GZip file. It may only be downloaded'
        };
        case Format.TAR : return {...defResult('TAR', 'Download'),
            message: 'This is a TAR file. It may only be downloaded',
        };
        case Format.PDF : return {...defResult('PDF', 'Open'),
            message: 'This is a PDF file. It may be downloaded or opened in another tab',
            loadInBrowserMsg: 'Open PDF File'+obsTitleMsg
        };
        case Format.HTML : return {...defResult('HTML', 'Open'),
            message: 'This is a web page or web application. It can be open in another tab',
            loadInBrowserMsg: 'Open Page'+obsTitleMsg
        };
        default: return {
            ...defResult('Unknown', 'Download'),
            message: 'This file may only only be downloaded'
        };

    }
}

export function getAnalysisAllImageDesc(parts,imagePartsLength) {
    if (imagePartsLength > 1) return 'All Images in File';
    const imagePart0Desc= parts.filter( (pa) => pa.type===FileAnalysisType.Image)?.[0]?.desc ?? '';
    return 'Image ' + imagePart0Desc;
}

/**
 * Get the description of a table part of a file.
 * @param {Object} obj
 * @param {String} obj.title
 * @param {FileAnalysisPart} obj.part
 * @param obj.fileFormat
 * @param {boolean} obj.useImageAsTable
 * @param {number} obj.imageAsTableColCnt
 * @param {TableModel} obj.table
 * @param {Number} obj.totalParts
 * @param {String} obj.originalTitle
 * @return {{title:String,dropDownText:String}}
 */
export function getAnalysisPartTableTitling({title='',part,fileFormat,table,useImageAsTable,
                                                 imageAsTableColCnt, totalParts, originalTitle}) {
    let retTitle='';
    if (part.interpretedData) {
        retTitle= title;
    }
    else if (fileFormat===Format.FITS) {
        const tOrCStr= 'table or chart';
        if (useImageAsTable) {
            const twoD= imageAsTableColCnt>2;
            const imageAsStr=  twoD ? '2D image - show as ': '1D image - show as ';
            retTitle= `HDU #${part.index} (${imageAsStr}${tOrCStr}${twoD? ' or image':''}) ${title}`;
        }
        else {
            retTitle= `HDU #${part.index} (${tOrCStr}) ${title}`;
        }
    }
    else if (isSSATable(table)) {
        retTitle= getAnalysisSSATitle(table,table.highlightedRow);
    }
    else {
       retTitle= totalParts===1 ? originalTitle :`Part #${part.index} ${title}`;
    }

    const dropDownText= originalTitle ? `${originalTitle} - ${retTitle}` : retTitle;
    return {title:retTitle, dropDownText};
}


/**
 * Make a title from arow of an obscore table.
 * @param table
 * @param row
 * @return {*|string}
 */
export function createObsCoreProductTitle(table, row) {
    if (!hasObsCoreLikeDataProducts(table)) return '';
    // 1. try a template
    const template = getDataServiceOptionByTable('productTitleTemplate', table);
    if (template?.trim() === '') return ''; // setting template to empty string disables all title guessing
    if (template) {
        const templateColNames = template && getColNameFromTemplate(template);
        const columns = getColumns(table);
        if (templateColNames?.length && columns?.length) {
            const cNames = columns.map(({name}) => name);
            const colObj = templateColNames.reduce((obj, v) => {
                if (cNames.includes(v)) {
                    obj[v] = getCellValue(table, row, v);
                }
                return obj;
            }, {});
            if (Object.keys(colObj).length === templateColNames.length) {
                const titleStr = tokenSub(colObj, template);
                if (titleStr) return titleStr;
            }
        }
    }
    // 2. try obs_title
    if (getObsTitle(table, row)) return getObsTitle(table, row);

    // 3. compute a name
    let obsCollect = getCellValue(table, row, 'obs_collection') || '';
    const obsId = getCellValue(table, row, 'obs_id') || '';
    const iName = getCellValue(table, row, 'instrument_name') || '';
    if (obsCollect === iName) obsCollect = '';
    return `${obsCollect ? obsCollect + ', ' : ''}${iName ? iName + ', ' : ''}${obsId}`;
}

function getColNameFromTemplate(template) {
    return template.match(/\${[\w -.]+}/g)?.map( (s) => s.substring(2,s.length-1));
}

export function makeObsTitleExtension(obsTitle) {
    if (!obsTitle) return '';
    const otBase = obsTitle.length > 25 ? obsTitle.substring(0, 29) : obsTitle;
    return ` (${otBase})`;
}


/**
 * Make the starting title for a datalink table row
 * @param {Object} p
 * @param {DatalinkData|object} p.dlData
 * @param {Object} [p.totals]
 * @param {Object} [p.indices]
 * @param {TableModel} p.sourceTable
 * @param {number} p.sourceRow
 * @return {{name:String,prodTitle:String,imageRelatedTitle:String,imageGridTitle:String}}
 */
export function makeDatalinkTitles({dlData, totals={}, indices={}, sourceTable, sourceRow}) {
    const {id,labelDLExt, description, serDef, dlAnalysis:{isThis=false}={}}= dlData ?? {};
    const imageTitles= {};
    if (labelDLExt) {
        imageTitles.prodTitle= labelDLExt;
        imageTitles.imageRelatedTitle= labelDLExt;
        imageTitles.imageGridTitle= labelDLExt;
    }

    let baseTitle= undefined;
    if (isUsableDescription(description)) baseTitle = description;  // most descriptions are not usable, but this will probably change
    if (!baseTitle) baseTitle= createObsCoreProductTitle(sourceTable,sourceRow); //  this is the most common case
    if (!baseTitle) baseTitle= getBaseTitleFromId(id); // as very specialized case
    if (!baseTitle && serDef && isUsableDescription(serDef.title)) baseTitle= serDef.title; // a fallback, rarly used


    // if we were able to create a base title
    if (baseTitle) return makeTitlesWithBaseTitle(dlData,totals,indices,baseTitle,imageTitles);

    // fallback
    const {'#this':primeCnt= 0}= indices;
    imageTitles.prodTitle??= 'Primary';
    imageTitles.imageRelatedTitle??= 'Primary';
    imageTitles.imageGridTitle??= 'Primary';
    if (isThis && primeCnt===0) return {name:'Primary product (#this)', ...imageTitles};
    if (isThis && primeCnt>0) return {name: `Primary product (#this ${primeCnt})`, ...imageTitles};
    return makeNameWithBaseTitle(dlData,totals,indices,'Related Data', imageTitles);
}

function getBaseTitleFromId(id) {
    if (!id?.toLowerCase().startsWith('ivo:')) return;
    try {
        const url= new URL(id);
        if (!url) return;
        const sp= url.searchParams;
        if (sp.size) {
            const keyNames= [...sp.keys()];
            if (keyNames.length===1) return keyNames[0];
            return;
        }
        if (url.pathname.length>1) {
            return url.pathname.substring(1);
        }
    }
    catch {
        // do nothing
    }
}

const extRE= /\.([a-zA-Z]+)$/;

function wordIsFile(word='') {
    if (word.startsWith('/')) return true;
    if (!word.includes('.')) return false;
    return Boolean(word.match(extRE));
}

/**
 * determine with the description is usable
 * if is usable if it exists, does not contain filenames or urls, and does not start with a known verb
 * @param description
 * @return {boolean}
 */
function isUsableDescription(description) {
    if (!description || description.length<4) return false; // no string or short string
    if (description.includes('://'))  return false; // looks like a /url
    const words = description.split(/\s+/);
    if (words.length===1 && words[0].split('/').length>3) return false; // looks like a file path
    if (words.length>12) return false; // too many words
    const verbs= ['download', 'retrieve', 'show', 'update', 'call', 'get'];
    if (verbs.some( (v) => words[0]?.toLowerCase()?.startsWith(v) )) return false; // it the description start with a verb
    if (words.some( (w) => wordIsFile(w))) return false; //some of the words are file paths
    return true;
}


function makeTitlesWithBaseTitle(dlData, totals, indices, baseTitle, imageTitles) {
    const name= makeNameWithBaseTitle(dlData, totals, indices, baseTitle);
    const ret= {name, ...imageTitles};
    if (!ret.prodTitle) ret.prodTitle = name;
    else if (ret.prodTitle.length<12) ret.prodTitle= `${ret.prodTitle} - ${baseTitle||name}`;
    ret.imageGridTitle??= ret.prodTitle;
    ret.imageRelatedTitle??= name;
    return ret;
}



function makeNameWithBaseTitle(dlData={}, totals={}, indices={}, baseTitle) {
    const {'#this':primeCnt=0}= indices;
    const {semantics=undefined,dlAnalysis:{isThis=false}={}}= dlData;
    if (!semantics) return baseTitle;
    if (isThis) {
        return primeCnt<1 ? `${baseTitle} (#this)` : `${baseTitle} (#this ${primeCnt})`;
    }

    const tot= totals[dlData.semantics] ?? 0;
    const idx= indices[dlData.semantics] ?? 0;
    return `${getSemanticsTitlePart(semantics)}${tot>0?' '+idx:''}: ${baseTitle}`;
}


function getSemanticsTitlePart(semantics) {
    if (!semantics || !semantics.includes('#') || semantics.endsWith('#')) return '';
    const str= semantics.substring(semantics.indexOf('#')+1);
    return str.charAt(0).toUpperCase() + str.substring(1);
}