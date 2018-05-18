import {getTblById} from '../tables/TableUtil.js';
import {dispatchTableFetch} from '../tables/TablesCntlr.js';
import {makeTblRequest} from '../tables/TableRequestUtil.js';
import {dispatchAddActionWatcher} from '../core/MasterSaga.js';
import {TABLE_LOADED} from '../tables/TablesCntlr';
import {doUpload} from '../ui/FileUpload.jsx';
import {getDrawLayersByType} from './PlotViewUtil.js';
import {getDlAry, dispatchCreateDrawLayer} from './DrawLayerCntlr.js';
import HiPSMOC from '../drawingLayers/HiPSMOC.js';

const MOC = '_moc';
let   mocCnt = 0;

export function makeMocTableId(ivoid) {
    const id = ivoStr(ivoid);

    return id.endsWith(MOC) ? id : id+MOC;
}

export function ivoStr(ivoid) {
    return ivoid ? ivoid.trim().replace('ivo://', '').replace(/\//g,'_') : 'moc_table_' + (++mocCnt);
}

/**
 * @summary get HiPS MOC table
 *  @param {string} mocUrl moc url
 *  @param {string} tblId table Id for moc table
 */
function onHiPSMoc(mocUrl, tblId) {
    if (!getTblById(tblId)) {
        doUpload(mocUrl, {isFromURL: true}).then(({status, cacheKey}) => {
            if (status === '200') {
                const tReq = makeTblRequest('userCatalogFromFile', 'Table Upload', {
                    filePath: cacheKey,
                    sourceFrom: 'isLocal'
                }, {tbl_id: tblId});
                dispatchTableFetch(tReq, 0);  // change to dispatchTableFetch later
            }
        });
    }
}

/**
 * @summary get HiPS MOC table
 * @param mocUrl
 * @param id
 * @returns {*}
 */
export function getHiPSMocTable(mocUrl, id) {
    const tblId = makeMocTableId(id);
    const mocTable = getTblById(tblId);
    if (mocTable && mocTable.tableData) return Promise.resolve(mocTable);


    return new Promise((resolve) => {
        const watcher= (action, cancelSelf) =>{
            const {tbl_id}= action.payload;
            if (tbl_id !== tblId) return;

            const mocTable = getTblById(tbl_id);
            if (mocTable) resolve(mocTable);
            cancelSelf();
        };
        dispatchAddActionWatcher({actions:[TABLE_LOADED], callback: watcher});
        onHiPSMoc(mocUrl, tblId);
    });
}


export function getMocNuniq(order, npix) {
    return 4 * (4 ** Math.floor(order)) + Math.floor(npix);
}

export function getMocOrderIndex(Nuniq) {
    const order = Math.floor(Math.log2(Nuniq/4)/2);
    const npix = Nuniq - 4 * (4**order);

    return {order, npix};
}

/**
 * add new layer on MOC table
 * @param {Array} moc_nuniq_nums moc number list
 * @param {string} tbl_id moc table id
 * @param {Object} fromPlot the active plot issuing the new layer
 * @returns {T|SelectInfo|*|{}}
 */
export function addNewMocLayer(moc_nuniq_nums, tbl_id, fromPlot) {
    const dls = getDrawLayersByType(getDlAry(), HiPSMOC.TYPE_ID);
    let   dl = dls.find((oneLayer) => oneLayer.drawLayerId === tbl_id);

    if (!dl) {
        dl = dispatchCreateDrawLayer(HiPSMOC.TYPE_ID, {moc_nuniq_nums, tbl_id, fromPlot});
    }
    return dl;
}

const sideCells = [[0], [0, 1, 3, 2]];   // side cells from order 0

/**
 * cells at each side as the cell from order 0 is sub-divided into 4 cells recursively up to order 'maxOrder'
 * @param {number} maxOrder  the max order to generate the side cells
 * @return {array} side cells from left bottom corner of order 0 to maxOrder, the return is like
 *                  [ [0]   // cell 0 at order 0
 *                    [0, 1, 3, 2]   // side cells at order 1, from the side cells of order 0
 *                    [0, .........] // side cells at order 2, from the side cells of order 1
 *                   ...]
 */
export function computeSideCellsToOrder(maxOrder) {
    const sideOffset = [[0, 1], [1, 3], [3, 2], [2, 0]];               // offset used to get sub-divided side cells of next order at each side
    const cornerOffset = [[2, 0, 1], [0, 1, 3], [1, 3, 2], [3, 2, 0]];  // offset used to get sub-divided corner cells of next order at each corner

    for (let i = sideCells.length; i <= maxOrder; i++) {
        const cornerInt = (2**(i-1)) - 1;

        // sub-divide side cells of previous order
        const cells = sideCells[i-1].reduce((prev, cellNum, idx) => {
                const atSide = Math.floor(idx/cornerInt);             // side #
                if (idx%cornerInt === 0) {                            // cell at the corner
                    const corners = cornerOffset[atSide].map((offset) => (cellNum*4+offset));   // sub-divided cells at the side

                    prev = [...prev,...corners];
                } else {                                              // side but corner cells
                    const sideCells = sideOffset[atSide].map((offset) => (cellNum*4 + offset));  // sub-divided cells at the side

                    prev = [...prev,...sideCells];
                }
                return prev;
        }, []);
        const newCells = [...cells.slice(1), cells[0]];

        sideCells.push(newCells);
    }

    return sideCells;
}

