/*eslint-env node, jest */
import {reducer, UPDATE_TBL_STATS} from 'firefly/charts/TableStatsCntlr.js';
import {TABLE_REMOVE} from 'firefly/tables/TablesCntlr.js';

const DOTTED_TBL_ID = '2mass.catalog';

describe('TableStatsCntlr reducer with a table id containing a period', () => {
    const colStats = [{name: 'ra', min: 202.3, max: 202.7}];
    const updatedState = reducer({},
        {type: UPDATE_TBL_STATS, payload: {tblId: DOTTED_TBL_ID, isColStatsReady: true, colStats}});

    test('UPDATE_TBL_STATS stores stats under the whole id, not a nested path', () => {
        expect(updatedState[DOTTED_TBL_ID]).toEqual({isColStatsReady: true, colStats});
        expect(updatedState['2mass']).toBeUndefined();
    });

    test('TABLE_REMOVE removes the stats of that table', () => {
        const removedState = reducer(updatedState, {type: TABLE_REMOVE, payload: {tbl_id: DOTTED_TBL_ID}});
        expect(removedState).toEqual({});
    });
});
