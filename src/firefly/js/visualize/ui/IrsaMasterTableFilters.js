/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {getColumnIdx} from '../../tables/TableUtil.js';
import {clone} from '../../util/WebUtil.js';


export const masterTableFilter = {
    defaultFilter : (tableModel) => tableModel,
    lsstFilter : lsstFilter
};


const filterOut= ['TAP_SCHEMA'];
const lsstTblOrder= ['Gaia', 'WISE', '2MASS', 'SPITZER'];

function getColPriority(c) {
    const idx= lsstTblOrder.findIndex( (v) => v===c);
    return idx===-1 ? lsstTblOrder.length : idx;
}


function lsstFilter(tableModel) {
    const pIdx= getColumnIdx(tableModel, 'projectshort');
    if (pIdx<0) return tableModel;

    var retData= tableModel.tableData.data
        .filter((r) => !filterOut.find( (out) => out===r[pIdx]))
        .sort( (r1,r2) => {
            const c1P= getColPriority(r1[pIdx]);
            const c2P= getColPriority(r2[pIdx]);
            return c1P<c2P ? -1 : c1P===c2P ? 0 : 1;
        });
    const newTm= clone(tableModel);
    newTm.tableData= clone(tableModel.tableData, {data:retData});
    newTm.totalRows= retData.length;
    return newTm;

}
