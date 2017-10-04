import './ChartPanel.css';
import React from 'react';
import {get, isEmpty} from 'lodash';

import {dispatchChartUpdate, dispatchChartFilterSelection, dispatchChartSelect, getChartData, dispatchSetActiveTrace, dispatchChartExpanded} from '../ChartsCntlr.js';
import {SimpleComponent} from '../../ui/SimpleComponent.jsx';
import {getTblById, clearFilters, getColumnIdx, getColumnType} from '../../tables/TableUtil.js';
import {dispatchSetLayoutMode, LO_MODE, LO_VIEW} from '../../core/LayoutCntlr.js';
import {downloadChart} from './PlotlyWrapper.jsx';
import {getColValidator} from './ColumnOrExpression.jsx';
import {getColValStats} from '../TableStatsCntlr.js';

function getToolbarStates(chartId) {
    const {selection, hasSelected, activeTrace=0, tablesources, layout, data=[]} = getChartData(chartId);
    const {tbl_id} = get(tablesources, [activeTrace], {});
    const {columns} = get(getTblById(tbl_id), ['tableData']) || {};
    const hasFilter = tbl_id && !isEmpty(get(getTblById(tbl_id), 'request.filters'));
    const hasSelection = !isEmpty(selection);
    const traceNames = data.map((t) => t.name).toString();
    const activeTraceType = get(data, `${activeTrace}.type`);
    return {hasSelection, hasFilter, activeTrace, activeTraceType, tbl_id, hasSelected,
            dragmode: get(layout, 'dragmode'), traceNames, columns};
}

export class ScatterToolbar extends SimpleComponent {

    getNextState(np) {
        const {chartId} = np || this.props;
        return getToolbarStates(chartId);
    }

    render() {
        const {chartId, expandable, toggleOptions} = this.props;
        const {hasSelection, hasFilter, activeTrace, tbl_id, hasSelected, dragmode} = this.state;
        const hasSelectionMode = Boolean(tbl_id);

        return (
            <div className='ChartToolbar'>
                <ActiveTraceSelect style={{marginRight: 20}} {...{chartId, activeTrace}}/>
                <SelectionPart {...{chartId, hasFilter, activeTrace, hasSelection, hasSelected, tbl_id}}/>
                <DragModePart {...{chartId, tbl_id, dragmode, hasSelectionMode}}/>
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

function isSelectable(tbl_id, chartId, type) {
    const typeWithX = ['heatmap', 'histogram2dcontour', 'histogram2d', 'scatter'];
    const typeWithY = ['heatmap', 'histogram2dcontour', 'histogram2d', 'scatter'];

    if (!tbl_id) return false;

    const checkX = typeWithX.includes(type);
    const checkY = typeWithY.includes(type);
    if (!checkX&&!checkY) return false;     // chart type has no selection box in tool bar

    const {tablesources} = getChartData(chartId);
    const strCol = ['str', 's', 'char', 'c'];
    const tableModel = getTblById(tbl_id);
    const noSelectionTraceIdx = tablesources.findIndex((tablesource) =>  {
          const {x, y} = get(tablesource, 'mappings') || {};
          const dataExp = [x, y];

          const noSelectionIdx = [checkX, checkY].findIndex((checkItem, idx) => {
              if (!checkItem) return false;      // ignore

              if (dataExp[idx]) {
                  if (getColumnIdx(tableModel, dataExp[idx]) >= 0) {
                      return strCol.includes(getColumnType(tableModel, dataExp[idx]));
                  } else {
                      const colValStats = getColValStats(tbl_id);
                      if (colValStats) {
                          const colValidator = getColValidator(colValStats);
                          const {valid} = colValidator(dataExp[idx]);
                          return !valid;
                      } else {
                          return false;
                      }
                  }
              } else {
                  return true;   // not qualified to have selection box
              }
          });
          return noSelectionIdx >= 0;
    });
    return (noSelectionTraceIdx < 0);
}

export class BasicToolbar extends SimpleComponent {

    getNextState(np) {
        const {chartId} = np || this.props;
        return getToolbarStates(chartId);
    }

    render() {
        const {chartId, expandable, toggleOptions} = this.props;
        //const {hasSelection, hasFilter, activeTrace, tbl_id, hasSelected, dragmode} = this.state;
        const {activeTrace, hasFilter, hasSelection, tbl_id, dragmode} = this.state;

        const type = get(getChartData(chartId), `data.${activeTrace}.type`, '');
        const showSelectionPart = isSelectable(tbl_id, chartId, type);
        const showDragPart = !type.includes('pie');
        const is3d = type.endsWith('3d') || type === 'surface'; // scatter3d, mesh3d, surface

        return (
            <div className='ChartToolbar'>
                <ActiveTraceSelect style={{marginRight: 20}} {...{chartId, activeTrace}}/>
                {showDragPart &&
                    <DragModePart {...{chartId, tbl_id, dragmode, hasSelectionMode: showSelectionPart, is3d}}/>}
                {showSelectionPart && <div className='ChartToolbar__buttons' style={{margin: '0 5px'}}>
                    {hasFilter && <ClearFilter {...{tbl_id}} />}
                    {hasSelection && <FilterSelection {...{chartId}} />}
                    </div>}
                <div className='ChartToolbar__buttons'>
                    {showDragPart && <ResetZoomBtn style={{marginLeft: 10}} {...{chartId}} />}
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
    let showSelectSelection = hasSelection;
    if (hasSelection) {
        const {data, activeTrace} = getChartData(chartId);
        showSelectSelection = get(data, `${activeTrace}.hoverinfo`) !== 'skip';
    }
    return (
        <div className='ChartToolbar__buttons' style={{margin: '0 5px'}}>
            {hasFilter    && <ClearFilter {...{tbl_id}} />}
            {hasSelected  && <ClearSelected {...{chartId}} />}
            {hasSelection && <FilterSelection {...{chartId}} />}
            {showSelectSelection && <SelectSelection style={{marginRight:10}} {...{chartId}} />}
        </div>
    );
}

function DragModePart({chartId, tbl_id, dragmode, hasSelectionMode, is3d}) {
    return (
        <div className='ChartToolbar__buttons' style={{margin: '0 5px'}}>
            <ZoomBtn {...{chartId, dragmode}} />
            <PanBtn {...{chartId, dragmode}} />
            {is3d && <OrbitBtn {...{chartId, dragmode}} />}
            {is3d && <TurntableBtn {...{chartId, dragmode}} />}
            {tbl_id && hasSelectionMode && <SelectBtn {...{chartId, dragmode}} />}
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

function TurntableBtn({style={}, chartId, dragmode}) {
    const selected = dragmode === 'turntable' ? 'selected' : '';
    return (
        <div style={style} onClick={() => dispatchChartUpdate({chartId, changes:{'layout.dragmode': 'turntable', 'selection': undefined}})}
             title='Turntable rotation'
             className={`ChartToolbar__turntable ${selected}`}/>
    );
}

function OrbitBtn({style={}, chartId, dragmode}) {
    const selected = dragmode === 'orbit' ? 'selected' : '';
    return (
        <div style={style} onClick={() => dispatchChartUpdate({chartId, changes:{'layout.dragmode': 'orbit', 'selection': undefined}})}
             title='Orbital rotation'
             className={`ChartToolbar__orbital ${selected}`}/>
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
    const {_original, layout} = getChartData(chartId) || {};
    const doClick = () => {
        // 2d axes
        const changes = ['xaxis','yaxis'].reduce((pv, axis) => {
            if (get(layout, `${axis}`)) {
                pv[`layout.${axis}.autorange`] = get(_original, `layout.${axis}.autorange`, true);
                pv[`layout.${axis}.range`] = get(_original, `layout.${axis}.range`);
            }
            return pv;
        }, {});
        // 3d axes
        changes['layout.scene.camera'] = get(_original, 'layout.scene.camera', {});
        if (!isEmpty(changes)) {
            dispatchChartUpdate({chartId, changes});
        }
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
