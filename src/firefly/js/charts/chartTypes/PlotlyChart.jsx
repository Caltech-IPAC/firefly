/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';

import {get} from 'lodash';
import {getTblById} from '../../tables/TableUtil.js';
import {getToolbarUI, getOptionsUI} from '../ChartUtil.js';
import {PlotlyChartArea} from '../ui/PlotlyChartArea.jsx';
import {getChartData} from '../ChartsCntlr.js';

import {FilterEditorWrapper} from './TblView.jsx';


export const PLOTLY_CHART = {
    id : 'plot.ly',
    Chart: PlotlyChartArea,
    Options,
    Toolbar,
    getChartProperties
};



function Options({chartId, optionsKey}) {
    if (!optionsKey || optionsKey === 'options') {
        const OptionsUI = getOptionsUI(chartId);
        return (
            <OptionsUI {...{chartId}}/>
        );
    } else if (optionsKey === 'filters') {
        //todo
        const {activeTrace=0, tablesources} = getChartData(chartId);
        const tbl_id = get(tablesources, `${activeTrace}.tbl_id`);
        const tableModel = getTblById(tbl_id);
        return (
            <FilterEditorWrapper tableModel={tableModel}/>
        );
    }
}

Options.propTypes = {
    chartId: PropTypes.string,
    optionsKey: PropTypes.string
};


function Toolbar({chartId, expandable, expandedMode, toggleOptions}) {
    const {activeTrace} = getChartData(chartId) || {};
    const ToolbarUI = getToolbarUI(chartId, activeTrace);
    return (
        <div className={`PanelToolbar ChartPanel__toolbar ${expandedMode?'ChartPanel__toolbar--offsetLeft':''}`}>
            <div className='PanelToolbar__group'/>
            <div className='PanelToolbar__group'>
                <ToolbarUI {...{chartId, expandable, toggleOptions}}/>
                </div>
        </div>
    );
}

Toolbar.propTypes = {
    chartId: PropTypes.string,
    expandable: PropTypes.bool,
    expandedMode: PropTypes.bool,
    toggleOptions: PropTypes.func // callback: toggleOptions(optionsKey)
};

/**
 * Get properties defining chart from the store.
 * @param chartId
 * @returns {{}}
 */
function getChartProperties(chartId) {
    return {};
}

