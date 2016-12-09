/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {get, has} from 'lodash';
import {makeWisePlotRequest} from './WiseRequestList.js';
import {make2MassPlotRequest} from './TwoMassRequestList.js';
import {makeLsstSdssPlotRequest} from './LsstSdssRequestList.js';
import {WebPlotRequest, TitleOptions} from '../visualize/WebPlotRequest.js';
import {ZoomType} from '../visualize/ZoomType.js';
import {Band} from '../visualize/Band';
import {getCellValue} from '../tables/TableUtil.js';
import {makeWorldPt} from '../visualize/Point.js';
import {MetaConst} from '../data/MetaConst.js';
import {CoordinateSys} from '../visualize/CoordSys.js';


const URL= 'URL';
const FILE= 'FILE';
const dataSourceUpper= 'DATASOURCE';


const defGuesses= [ 'FILE', 'FITS', 'DATA', 'SOURCE' ];


export const converters = {
    'wise' : {
        threeColor : true,
        hasRelatedBands : true,
        canGrid : true,
        maxPlots : 12,
        makeRequest : makeWisePlotRequest,
        threeColorBands : {
            b1 : {color : Band.RED, title: 'Band 1'},
            b2 : {color : Band.GREEN, title: 'Band 2'},
            b3 : {color : null, title: 'Band 3'},
            b4 : {color : Band.BLUE, title: 'Band 4'}
        }
    },
    'twomass' : {
        threeColor : true,
        hasRelatedBands : true,
        canGrid : true,
        maxPlots : 12,
        makeRequest : make2MassPlotRequest,
        threeColorBands : {
            J : {color : Band.RED, title: 'J'},
            H : {color : Band.GREEN, title: 'H'},
            K : {color : Band.BLUE, title: 'K'}
        }
    },
    'lsst_sdss' : {
        threeColor : true,
        hasRelatedBands : true,
        canGrid : true,
        maxPlots : 12,
        makeRequest : makeLsstSdssPlotRequest,
        threeColorBands : {
            u : {color : null, title: 'u'},
            g : {color : Band.RED, title: 'g'},
            r : {color : Band.GREEN, title: 'r'},
            i : {color : null,  title: 'i'},
            z : {color : Band.BLUE, title: 'z'}
        }
    },
    'UNKNOWN' : {
        threeColor : false,
        hasRelatedBands : false,
        canGrid : true,
        maxPlots : 12,
        makeRequest : makeRequestForUnknown
    },
    'SimpleMoving' : {
        threeColor : false,
        hasRelatedBands : false,
        canGrid : true,
        maxPlots : 12,
        makeRequest : makeRequestSimpleMoving
    }
};




export function converterFactory(table) {
    var dataId= get(table, ['tableMeta', MetaConst.DATASET_CONVERTER]);
    if (!dataId && has(table, 'tableMeta') && has(table, 'tableData')) {
        dataId = findAColumn(table.tableMeta, get(table, 'tableData.columns', [])) && 'UNKNOWN';
    }
    const converter= dataId && converters[dataId];
    return converter && {converter,dataId};
}


/**
 *  Support data the we don't know about
 * @param table
 * @param row
 * @param includeSingle
 * @param includeStandard
 * @return {{}}
 */
function makeRequestForUnknown(table, row, includeSingle, includeStandard) {

    const {tableMeta:meta}= table;
    const dataSource= meta['DataSource'] || URL;

    const col= findAColumn(meta,table.tableData.columns);
    if (!col) return {};


    const retval= {};
    if (includeSingle) {
        retval.single= makeRequest(table,col.name,dataSource,row);
    }
    
    if (includeStandard) {
        retval.standard= [makeRequest(table,col.name,dataSource,row)];
        retval.highlightPlotId= retval.standard[0].getPlotId();
    }
    
    return retval;

}

function makeRequestSimpleMoving(table, row, includeSingle, includeStandard) {

    const {tableMeta:meta}= table;
    const dataSource= meta['DataSource'] || URL;



    if (!meta[MetaConst.MOVING_COORD_COLS]) return [];


    if (!meta['urlColumn']) return [];

    const sAry= meta[MetaConst.MOVING_COORD_COLS].split(';');
    if (!sAry || sAry.length!== 3) return [];

    const lonCol= sAry[0];
    const latCol= sAry[1];
    const csys= CoordinateSys.parse(sAry[2]);
    const urlCol= meta['urlColumn'];

    const retval= {};
    if (includeSingle) {
        retval.single= makeMovingRequest(table,row,lonCol,latCol,urlCol,csys,'simple-moving-single-'+(row %24));
    }

    if (includeStandard) {
        retval.standard= [makeMovingRequest(table,row,lonCol,latCol,urlCol,csys,'simple-moving-single')];
        retval.highlightPlotId= retval.standard[0].getPlotId();
    }

    return retval;

}


function findAColumn(meta,columns) {
    const dsCol= Object.keys(meta).find( (key) => key.toUpperCase()===dataSourceUpper);
    const guesses= dsCol ? [dsCol,...defGuesses] : defGuesses;
    return columns.find( (c) => guesses.includes(c.name.toUpperCase()));
}


function makeMovingRequest(table, row, lonCol, latCol, urlCol, csys, plotId) {
    const lon= Number(getCellValue(table,row,lonCol));
    const lat= Number(getCellValue(table,row,latCol));
    const url= getCellValue(table,row,urlCol);
    const r = WebPlotRequest.makeURLPlotRequest(url, 'Fits Image');
    const wp= makeWorldPt(lon,lat,csys);
    r.setTitleOptions(TitleOptions.FILE_NAME);
    r.setZoomType(ZoomType.TO_WIDTH_HEIGHT);
    r.setOverlayPosition(wp);
    r.setPlotId(plotId);
    return r;

}


function makeRequest(table, dataColumn, dataSource, row) {
    if (!dataColumn || !table || !dataSource) return null;

    var r;
    var source= getCellValue(table, row, dataColumn);
    if (dataSource.toLocaleUpperCase() === URL) {
        r = WebPlotRequest.makeURLPlotRequest(source, 'Fits Image');
    }
    else if (dataSource.toLocaleUpperCase() === FILE) {
        r = WebPlotRequest.makeFilePlotRequest(source, 'Fits Image');
    }
    r.setTitleOptions(TitleOptions.FILE_NAME);
    r.setZoomType(ZoomType.FULL_SCREEN);
    r.setPlotId(source);
    
    return r;
}
