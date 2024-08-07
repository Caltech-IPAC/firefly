import React from 'react';
import PropTypes from 'prop-types';

import {getActiveTableId, getTblInfoById} from 'firefly/tables/TableUtil';
import {SelectInfo} from 'firefly/tables/SelectInfo';
import {CombineChart} from 'firefly/charts/ui/CombineChart';
import {useStoreConnector} from 'firefly/ui/SimpleComponent';

/**
 * Retrieves charts from the rows selected in a table and creates a combined chart in the pinned-charts container.
 *
 * @param p
 * @param {function} p.fetchRowChart an async function that takes tableModel and row index and returns a Promise<string|undefined> of chart ID.
 * @param {function} p.activatePinnedCharts a function to activate pinned-charts container after combination is complete
 * @param {Object} p.slotProps
 * @returns {JSX.Element}
 * @constructor
 */
export function CompareRowCharts({fetchRowChart, activatePinnedCharts, slotProps}) {
    const {tableModel, selectInfo} = useStoreConnector(() => getTblInfoById(getActiveTableId()));
    const selectedRowIdxs = Array.from(SelectInfo.newInstance(selectInfo).getSelected());
    const selectedRowChartIds = selectedRowIdxs.map((row) => fetchRowChart(tableModel, row));

    return (
        <CombineChart
            chartIds={selectedRowChartIds}
            allowChartSelection={false} //because user already selected rows before opening dialog
            onCombineComplete={activatePinnedCharts}
            slotProps={{
                button: {tip: 'Compare selected rows'},
                dialog: {title: 'Combine charts in the selected rows'},
                ...slotProps
            }}/>
    );
}

CompareRowCharts.propTypes = {
    fetchRowChart: PropTypes.func,
    activatePinnedCharts: PropTypes.func,
    slotProps: CombineChart.propTypes.slotProps,
};