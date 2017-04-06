import './ChartPanel.css';
import React from 'react';
import {get, isEmpty} from 'lodash';

import {dispatchChartUpdate, dispatchChartFilterSelection, dispatchChartSelect, getChartData, dispatchSetActiveTrace, dispatchChartExpanded} from '../ChartsCntlr.js';
import {SimpleComponent} from '../../ui/SimpleComponent.jsx';
import {getTblById, clearFilters} from '../../tables/TableUtil.js';
import {dispatchSetLayoutMode, LO_MODE, LO_VIEW} from '../../core/LayoutCntlr.js';

function getToolbarStates(chartId) {
    const {showOptions, selection, selected, activeTrace=0, tablesources, layout} = getChartData(chartId);
    const {tbl_id} = get(tablesources, [activeTrace], {});
    const hasFilter = tbl_id && !isEmpty(get(getTblById(tbl_id), 'request.filters'));
    const hasSelection = !isEmpty(selection);
    return {showOptions, hasSelection, hasFilter, activeTrace, tbl_id, hasSelected: !!selected, dragmode: get(layout, 'dragmode')};
}

export class ScatterToolbar extends SimpleComponent {

    getNextState(np) {
        const {chartId} = np || this.props;
        return getToolbarStates(chartId);
    }

    render() {
        const {chartId, expandable} = this.props;
        const {showOptions, hasSelection, hasFilter, activeTrace, tbl_id, hasSelected, dragmode} = this.state;
        return (
            <div className='ChartToolbar'>
                <ActiveTraceSelect style={{marginRight: 20}} {...{chartId, activeTrace}}/>
                <SelectionPart {...{chartId, hasFilter, activeTrace, hasSelection, hasSelected, tbl_id}}/>
                <DragModePart {...{chartId, tbl_id, dragmode}}/>
                <div className='ChartToolbar__buttons'>
                    <AutoScaleBtn style={{marginLeft: 10}} {...{chartId}} />
                    <OptionsBtn {...{chartId, showOptions}} />
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
        const {chartId, expandable} = this.props;
        const {showOptions, hasSelection, hasFilter, activeTrace, tbl_id, hasSelected, dragmode} = this.state;
        return (
            <div className='ChartToolbar'>
                <ActiveTraceSelect style={{marginRight: 20}} {...{chartId, activeTrace}}/>
                <DragModePart {...{chartId, tbl_id, dragmode}}/>
                <div className='ChartToolbar__buttons'>
                    <AutoScaleBtn style={{marginLeft: 10}} {...{chartId}} />
                    <OptionsBtn {...{chartId, showOptions}} />
                    {expandable && <ExpandBtn {...{chartId}} />}
                </div>
            </div>
        );
    }
}


function SelectionPart({chartId, hasFilter, activeTrace, hasSelection, hasSelected, tbl_id}) {
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
             title='Zoom'
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

function AutoScaleBtn({style={}, chartId}) {
    return (
        <div style={style} onClick={() => dispatchChartUpdate({chartId, changes:{'layout.xaxis.autorange': true, 'layout.yaxis.autorange': true}})}
             title='Autoscale'
             className='ChartToolbar__auto-scale'/>
    );
}

function OptionsBtn({style={}, chartId, showOptions}) {
    return (
        <div style={style} onClick={() => dispatchChartUpdate({chartId, changes:{'showOptions': !showOptions}})}
             title='options'
             className='ChartToolbar__options'/>
    );
}

function ExpandBtn({style={}, chartId}) {
    return (
        <div style={style} onClick={() => {   dispatchChartExpanded(chartId);
                                              dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.xyPlots);
                                          }}
             title='expand'
             className='ChartToolbar__expand'/>
    );
}

function ActiveTraceSelect({style={}, chartId, activeTrace}) {
    const {data} = getChartData(chartId) || [];
    const selected = get(data, [activeTrace, 'name']) || `trace ${activeTrace}`;
    if (data.length < 2) return null;

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
             title='filter on the selected region'
             className='ChartToolbar__filter'/>
    );
}

function SelectSelection({style={}, chartId}) {
    const onClick = () => {
            const selIndexes = get(getChartData(chartId), 'selection.points', []);
            dispatchChartSelect({chartId, selIndexes});
        };
    return (
        <div style={style} onClick={onClick}
             title='mark the selected region selected'
             className='ChartToolbar__selected'/>
    );
}

function ClearSelected({style={}, chartId}) {
    return (
        <div style={style} onClick={() => dispatchChartSelect({chartId, selIndexes:[]})}
             title='mark the selected region selected'
             className='ChartToolbar__clear-selected'/>
    );
}

function ClearFilter({style={}, tbl_id}) {
    return (
        <div style={style} onClick={() => clearFilters(getTblById(tbl_id))}
             title='clear filters'
             className='ChartToolbar__clear-filters'/>
    );
}
