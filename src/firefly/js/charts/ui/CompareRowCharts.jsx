import React, {useEffect, useState} from 'react';
import PropTypes from 'prop-types';
import {set} from 'lodash';

import {getActiveTableId, getTblById, getTblInfoById} from 'firefly/tables/TableUtil';
import {SelectInfo} from 'firefly/tables/SelectInfo';
import {CombineChart} from 'firefly/charts/ui/CombineChart';
import {useStoreConnector} from 'firefly/ui/SimpleComponent';
import {showInfoPopup} from 'firefly/ui/PopupUtil';
import {CombineChartButton} from 'firefly/visualize/ui/Buttons';

const ROW_LIMIT = 8;

/**
 * Retrieves charts from the rows selected in a table and creates a combined chart in the pinned-charts container.
 *
 * @param p
 * @param {function(TableModel,number): Promise<string>} p.fetchRowChart an async function that takes tableModel and row index and returns a promise of chart ID.
 * @param {function():void} p.activatePinnedCharts a function to activate pinned-charts container after combination is complete
 * @param {number} p.rowLimit maximum number of rows that can be combined.
 * @param {function(string):string} p.deriveTraceTitle function to create trace title from the title of chart. Default is to use first 10 characters.
 * @param {Object} p.slotProps
 * @returns {JSX.Element}
 * @constructor
 */
export function CompareRowCharts({fetchRowChart, activatePinnedCharts, rowLimit=ROW_LIMIT, deriveTraceTitle, slotProps}) {
    const {tblId, selectedRowIdxs} = useStoreConnector(() => {
        const tblId = getActiveTableId();
        const {selectInfo} = getTblInfoById(tblId);
        const selectedRowIdxs = Array.from(SelectInfo.newInstance(selectInfo).getSelected());
        return {tblId, selectedRowIdxs};
    });

    const [selectedRowChartIds, setSelectedRowChartIds] = useState([]);
    useEffect(()=>{
        if (selectedRowIdxs.length <= rowLimit) {
            setSelectedRowChartIds(selectedRowIdxs.map((row) => getRowChart(tblId, row, fetchRowChart))); //promised chartIds
        }
    }, [tblId, selectedRowIdxs]);

    const buttonTip = 'Compare selected rows';
    if (selectedRowIdxs.length > rowLimit) {
        const content = `Number of rows selected for chart comparison exceeds the limit! Please select ${rowLimit} rows or lesser.`;
        return (<CombineChartButton onClick={()=>showInfoPopup(content)} tip={buttonTip} {...slotProps?.button}/>);
    }

    return (
        <CombineChart
            chartIds={selectedRowChartIds}
            showChartSelectionTable={false} //because user already selected rows before opening dialog
            onCombineComplete={activatePinnedCharts}
            deriveTraceTitle={deriveTraceTitle}
            slotProps={{
                button: {tip: buttonTip},
                dialog: {title: 'Combine charts in the selected rows'},
                ...slotProps
            }}/>
    );
}

CompareRowCharts.propTypes = {
    fetchRowChart: PropTypes.func,
    activatePinnedCharts: PropTypes.func,
    rowLimit: PropTypes.number,
    deriveTraceTitle: PropTypes.func,
    slotProps: CombineChart.propTypes.slotProps,
};

// Local cache of promised chartIds
const allPromisedChartIds = {};

// Retrieve row chart id from cache if possible because fetching from server is expensive and
// causes side effects in the UI (like pinned chart viewer may re-render if fetchRowChart adds a chart in the store)
const getRowChart = (tblId, row, fetchRowChart) => {
    let promisedChartId = allPromisedChartIds?.[tblId]?.[`rowNum-${row}`];
    if (!promisedChartId) {
        const tableModel = getTblById(tblId);
        promisedChartId = fetchRowChart(tableModel, row);
        set(allPromisedChartIds, [tblId, `rowNum-${row}`], promisedChartId);
    }
    return promisedChartId;
};
