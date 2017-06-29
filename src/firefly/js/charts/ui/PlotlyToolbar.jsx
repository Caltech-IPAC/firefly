import './ChartPanel.css';
import React from 'react';
import {get, isEmpty} from 'lodash';

import {dispatchChartUpdate, dispatchChartFilterSelection, dispatchChartSelect, getChartData, dispatchSetActiveTrace, dispatchChartExpanded} from '../ChartsCntlr.js';
import {SimpleComponent} from '../../ui/SimpleComponent.jsx';
import {getTblById, clearFilters} from '../../tables/TableUtil.js';
import {dispatchSetLayoutMode, LO_MODE, LO_VIEW} from '../../core/LayoutCntlr.js';
import {downloadChart} from './PlotlyWrapper.jsx';

function getToolbarStates(chartId) {
    const {selection, selected, activeTrace=0, tablesources, layout, data={}} = getChartData(chartId);
    const {tbl_id} = get(tablesources, [activeTrace], {});
    const hasFilter = tbl_id && !isEmpty(get(getTblById(tbl_id), 'request.filters'));
    const hasSelection = !isEmpty(selection);
    const traceNames = data.map((t) => t.name).toString();
    return {hasSelection, hasFilter, activeTrace, tbl_id, hasSelected: !!selected, dragmode: get(layout, 'dragmode'), traceNames};
}

export class ScatterToolbar extends SimpleComponent {

    getNextState(np) {
        const {chartId} = np || this.props;
        return getToolbarStates(chartId);
    }

    render() {
        const {chartId, expandable, toggleOptions} = this.props;
        const {hasSelection, hasFilter, activeTrace, tbl_id, hasSelected, dragmode} = this.state;
        return (
            <div className='ChartToolbar'>
                <ActiveTraceSelect style={{marginRight: 20}} {...{chartId, activeTrace}}/>
                <SelectionPart {...{chartId, hasFilter, activeTrace, hasSelection, hasSelected, tbl_id}}/>
                <DragModePart {...{chartId, tbl_id, dragmode}}/>
                <div className='ChartToolbar__buttons'>
                    <ResetZoomBtn style={{marginLeft: 10}} {...{chartId}} />
                    <SaveBtn {...{chartId}} />
                    {tbl_id && <FiltersBtn {...{chartId, toggleOptions}} />}
                    <OptionsBtn {...{chartId, toggleOptions}} />
                    {expandable && <ExpandBtn {...{chartId}} />}
                </div>
            </div>
        );
    }
}

export class BasicToolbar extends SimpleComponent {

    getNextState(np) {
        const {chartId} = np || this.props;
        return getToolbarStates(chartId);
    }

    render() {
        const {chartId, expandable, toggleOptions} = this.props;
        //const {hasSelection, hasFilter, activeTrace, tbl_id, hasSelected, dragmode} = this.state;
        const {activeTrace, tbl_id, dragmode} = this.state;
        return (
            <div className='ChartToolbar'>
                <ActiveTraceSelect style={{marginRight: 20}} {...{chartId, activeTrace}}/>
                <DragModePart {...{chartId, tbl_id, dragmode}}/>
                <div className='ChartToolbar__buttons'>
                    <ResetZoomBtn style={{marginLeft: 10}} {...{chartId}} />
                    <SaveBtn {...{chartId}} />
                    {tbl_id && <FiltersBtn {...{chartId, toggleOptions}} />}
                    <OptionsBtn {...{chartId, toggleOptions}} />
                    {expandable && <ExpandBtn {...{chartId}} />}
                </div>
            </div>
        );
    }
}


function SelectionPart({chartId, hasFilter, hasSelection, hasSelected, tbl_id}) {
    if (! (hasFilter || hasSelection || hasSelected)) return null;   // don't show if nothing to show
    return (
        <div className='ChartToolbar__buttons' style={{margin: '0 5px'}}>
            {hasFilter    && <ClearFilter {...{tbl_id}} />}
            {hasSelected  && <ClearSelected {...{chartId}} />}
            {hasSelection && <FilterSelection {...{chartId}} />}
            {hasSelection && <SelectSelection style={{marginRight:10}} {...{chartId}} />}
        </div>
    );
}

function DragModePart({chartId, tbl_id, dragmode}) {
    return (
        <div className='ChartToolbar__buttons' style={{margin: '0 5px'}}>
            <ZoomBtn {...{chartId, dragmode}} />
            <PanBtn {...{chartId, dragmode}} />
            {tbl_id && <SelectBtn {...{chartId, dragmode}} />}
        </div>
    );
}

function ZoomBtn({style={}, chartId, dragmode='zoom'}) {
    const selected = dragmode === 'zoom' ? 'selected' : '';
    return (
        <div style={style} onClick={() => dispatchChartUpdate({chartId, changes:{'layout.dragmode': 'zoom', 'selection': undefined}})}
             title='Zoom in the enclosed points'
             className={`ChartToolbar__zoom ${selected}`}/>
    );
}

function PanBtn({style={}, chartId, dragmode}) {
    const selected = dragmode === 'pan' ? 'selected' : '';
    return (
        <div style={style} onClick={() => dispatchChartUpdate({chartId, changes:{'layout.dragmode': 'pan', 'selection': undefined}})}
             title='Pan'
             className={`ChartToolbar__pan ${selected}`}/>
    );
}

function SelectBtn({style={}, chartId, dragmode}) {
    const selected = dragmode === 'select' ? 'selected' : '';
    return (
        <div style={style} onClick={() => dispatchChartUpdate({chartId, changes:{'layout.dragmode': 'select', 'selection': undefined}})}
             title='Select'
             className={`ChartToolbar__select ${selected}`}/>
    );
}

function ResetZoomBtn({style={}, chartId}) {
    const {_original} = getChartData(chartId) || {};
    const doClick = () => {
        // TODO:  this only handles chart with 2 axes
        const changes = ['xaxis','yaxis'].reduce((pv, axis) => {
            pv[`layout.${axis}.autorange`]  = get(_original, `layout.${axis}.autorange`, true);
            pv[`layout.${axis}.range`]      = get(_original, `layout.${axis}.range`);
            return pv;
        }, {});
        dispatchChartUpdate({chartId, changes});
    };
    return (
        <div style={style} onClick={doClick}
             title='Zoom out to original range'
             className='ChartToolbar__reset-zoom'/>
    );
}

function SaveBtn({style={}, chartId}) {
    return (
        <div style={style} onClick={() => { downloadChart(chartId);}}
             title='Download the chart as a PNG image'
             className='ChartToolbar__save'/>
    );
}

function FiltersBtn({style={}, chartId, toggleOptions}) {
    return (
        <div style={style} onClick={() => toggleOptions('filters')}
             title='Show/edit filters'
             className='ChartToolbar__tblfilters'/>
    );
}

function OptionsBtn({style={}, chartId, toggleOptions}) {
    return (
        <div style={style} onClick={() => toggleOptions('options')}
             title='Chart options and tools'
             className='ChartToolbar__options'/>
    );
}


function ExpandBtn({style={}, chartId}) {
    return (
        <div style={style} onClick={() => {   dispatchChartExpanded(chartId);
                                              dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.xyPlots);
                                          }}
             title='Expand this panel to take up a larger area'
             className='ChartToolbar__expand'/>
    );
}

function ActiveTraceSelect({style={}, chartId, activeTrace}) {
    const {data} = getChartData(chartId) || [];
    const selected = get(data, [activeTrace, 'name']) || `trace ${activeTrace}`;
    if (!data || data.length < 2) return null;

    return (
        <div style={{width:100, height:20, ...style}} className='styled-select semi-square'>
            <select value={selected} onChange={(e) => dispatchSetActiveTrace({chartId, activeTrace: get(e, 'target.selectedIndex',0)})}>
                {data.map( (trace, idx) => <option key={`trace-${idx}`}>{get(trace, 'name', `trace ${idx}`)}</option>)}
            </select>
        </div>
    );
}

function FilterSelection({style={}, chartId}) {
    return (
        <div style={style} onClick={() => dispatchChartFilterSelection({chartId})}
             title='Filter in the selected points'
             className='ChartToolbar__filter'/>
    );
}

function SelectSelection({style={}, chartId}) {
    const onClick = () => {
            const selIndexes = get(getChartData(chartId), 'selection.points', []);
            dispatchChartSelect({chartId, selIndexes, chartTrigger: true});
        };
    return (
        <div style={style} onClick={onClick}
             title='Select the enclosed points'
             className='ChartToolbar__selected'/>
    );
}

function ClearSelected({style={}, chartId}) {
    return (
        <div style={style} onClick={() => dispatchChartSelect({chartId, selIndexes:[], chartTrigger: true})}
             title='Unselect all selected points'
             className='ChartToolbar__clear-selected'/>
    );
}

function ClearFilter({style={}, tbl_id}) {
    return (
        <div style={style} onClick={() => clearFilters(getTblById(tbl_id))}
             title='Remove all filters'
             className='ChartToolbar__clear-filters'/>
    );
}
