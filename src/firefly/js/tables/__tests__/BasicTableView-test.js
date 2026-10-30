/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*eslint-env node, jest */

import * as TableUtil from '../TableUtil.js';
import {defHighlightedRowHandler} from '../ui/BasicTableView.jsx';

// Returns the CSS class BasicTableView gives each of the first rowsShown rows of the page that starts at startIdx.
// loadedObsIds holds the obs_id of each row the browser has fetched, keyed by whole-table row,
// the way a server table keeps every page it has loaded.
function defHighlightedRowHandlerClasses({loadedObsIds, highlightedRow, startIdx, rowsShown}) {
    const data = [];
    Object.entries(loadedObsIds).forEach(([row, obsId]) => data[row] = [obsId]);
    const tableModel = {
        tbl_id: 'spherex-images',
        highlightedRow,
        tableMeta: {'tbl.relatedCols': 'obs_id'},
        tableData: {columns: [{name: 'obs_id'}], data},
    };
    jest.spyOn(TableUtil, 'getTblById').mockReturnValue(tableModel);
    const rowClass = defHighlightedRowHandler('spherex-images', highlightedRow - startIdx, startIdx);
    return Array.from({length: rowsShown}, (_, rowIndex) => rowClass(rowIndex));
}

// obs_id values from SPHEREx search results for M51 on irsatest (FIREFLY-2149), 100 rows per page
const page1Rows = {0: '2025W19_1B_0146_1', 1: '2025W19_1B_0146_1', 2: '2025W19_1B_0146_2', 3: '2025W19_1B_0146_2'};
const page4Rows = {300: '2025W51_2A_0177_1', 301: '2026W20_2A_0183_1', 302: '2026W20_2A_0183_1', 303: '2026W20_2A_0183_2'};

describe('defHighlightedRowHandler marks the rows that share the highlighted row\'s obs_id', () => {
    afterEach(() => jest.restoreAllMocks());

    test('page 1', () => {
        const classes = defHighlightedRowHandlerClasses(
            {loadedObsIds: page1Rows, highlightedRow: 0, startIdx: 0, rowsShown: 4});
        expect(classes).toEqual(['highlighted', 'related', undefined, undefined]);
    });

    test('page 4, row 300 has no partner on the page', () => {
        const classes = defHighlightedRowHandlerClasses(
            {loadedObsIds: {...page1Rows, ...page4Rows}, highlightedRow: 300, startIdx: 300, rowsShown: 4});
        expect(classes).toEqual(['highlighted', undefined, undefined, undefined]);
    });

    test('page 4, row 302 is paired with row 301', () => {
        const classes = defHighlightedRowHandlerClasses(
            {loadedObsIds: {...page1Rows, ...page4Rows}, highlightedRow: 302, startIdx: 300, rowsShown: 4});
        expect(classes).toEqual([undefined, 'related', 'highlighted', undefined]);
    });

    test('page 4 while page 1 is not loaded, as after clearing a filter', () => {
        const classes = defHighlightedRowHandlerClasses(
            {loadedObsIds: page4Rows, highlightedRow: 302, startIdx: 300, rowsShown: 4});
        expect(classes).toEqual([undefined, 'related', 'highlighted', undefined]);
    });
});
