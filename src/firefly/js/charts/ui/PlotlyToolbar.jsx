import React from 'react';
import PropTypes from 'prop-types';
import {Divider, IconButton, Stack, ToggleButtonGroup} from '@mui/joy';
import {get, isEmpty} from 'lodash';

import {dispatchChartUpdate, dispatchChartFilterSelection, dispatchChartSelect, getChartData, dispatchSetActiveTrace, dispatchChartExpanded, resetChart} from '../ChartsCntlr.js';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {getTblById, clearFilters, getColumnIdx, getColumnType} from '../../tables/TableUtil.js';
import {dispatchSetLayoutMode, LO_MODE, LO_VIEW} from '../../core/LayoutCntlr.js';
import {downloadChart} from './PlotlyWrapper.jsx';
import {getColValidator} from './ColumnOrExpression.jsx';
import {getColValStats} from '../TableStatsCntlr.js';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {showOptionsPopup} from '../../ui/PopupUtil.jsx';
import {showChartsDialog} from './ChartSelectPanel.jsx';
import {FilterEditorWrapper} from './FilterEditorWrapper.jsx';
import {isScatter2d} from '../ChartUtil.js';
import {findViewerWithItemId, getLayoutType, getMultiViewRoot, PLOT2D} from '../../visualize/MultiViewCntlr.js';
import {ListBoxInputFieldView} from 'firefly/ui/ListBoxInputField';
import {ClearFilterButton, ExpandButton, FilterButton, ResetButton, SaveButton, SettingsButton} from '../../visualize/ui/Buttons.jsx';

import ZoomIco from 'html/images/icons-2014/24x24_ZoomIn.png';
import PanIco from 'html/images/icons-2014/pan.png';
import SelectIco from 'html/images/icons-2014/select.png';
import TurntableIco from 'html/images/turntable-tmp.png';
import OrbitalIco from 'html/images/orbital-tmp.png';
import ClearSelectIco from 'html/images/icons-2014/24x24_CheckmarkOff_Circle.png';
import ApplySelectIco from 'html/images/icons-2014/24x24_Checkmark.png';
import ApplyFilterIco from 'html/images/icons-2014/24x24_FilterAdd.png';
import ResetZoomIco from 'html/images/icons-2014/Zoom1x-24x24-tmp.png';

export function PlotlyToolbar({chartId, expandable}) {
    const {activeTrace} = getChartData(chartId);
    const ToolbarUI = getToolbarUI(chartId, activeTrace);
    return <ToolbarUI {...{chartId, expandable}}/>;
}

PlotlyToolbar.propTypes = {
    chartId: PropTypes.string,
    expandable: PropTypes.bool,
    expandedMode: PropTypes.bool
};

function getToolbarUI(chartId, activeTrace=0) {

    const {data} =  getChartData(chartId);
    const type = get(data, [activeTrace, 'type'], 'scatter');
    if (isScatter2d(type)) {
        return ScatterToolbar;
    } else {
        return BasicToolbar;
    }
}

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


function ScatterToolbar({chartId, expandable}) {
    const scatterToolbarState = useStoreConnector(() => getToolbarStates(chartId), [chartId]);

    const {hasSelection, hasFilter, activeTrace, tbl_id, hasSelected, dragmode} = scatterToolbarState;
    const hasSelectionMode = Boolean(tbl_id);
    const help_id = getChartData(chartId)?.help_id;

    return (
        <Stack direction='row' alignItems='center'>
            <ActiveTraceSelect {...{chartId, activeTrace}}/>
            <SelectionPart {...{chartId, hasFilter, activeTrace, hasSelection, hasSelected, tbl_id}}/>
            <DragModePart {...{chartId, tbl_id, dragmode, hasSelectionMode}}/>
            <ResetZoomBtn {...{chartId}} />
            <SaveBtn {...{chartId}} />
            <RestoreBtn {...{chartId}} />
            {tbl_id && <FiltersBtn {...{chartId}} />}
            <OptionsBtn {...{chartId}} />
            {expandable && <ExpandBtn {...{chartId}} />}
            { help_id && <Stack p={1/4}> <HelpIcon helpId={help_id} /> </Stack>}
        </Stack>
    );
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
    const noSelectionTraceIdx = tablesources?.findIndex((tablesource) =>  {
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

function BasicToolbar({chartId, expandable}) {
    const basicToolbarState = useStoreConnector(() => getToolbarStates(chartId), [chartId]);

    const {activeTrace, hasFilter, hasSelection, tbl_id, dragmode} = basicToolbarState;
    const type = getChartData(chartId)?.data?.[activeTrace]?.type ?? '';
    const showSelectionPart = isSelectable(tbl_id, chartId, type);
    const showDragPart = !type.includes('pie');
    const is3d = type.endsWith('3d') || type === 'surface'; // scatter3d, mesh3d, surface

    const help_id = getChartData(chartId)?.help_id;

    const selectionPart = (
        <Stack direction='row' spacing={1/2}>
            {hasFilter && <ClearFilter {...{tbl_id}} /> }
            {hasSelection && <FilterSelection {...{chartId}} />}
        </Stack>
    );

    return (
        <Stack direction='row' alignItems='center'>
            <ActiveTraceSelect {...{chartId, activeTrace}}/>
            {showDragPart &&
                <DragModePart {...{chartId, tbl_id, dragmode, hasSelectionMode: showSelectionPart, is3d}}/>}
            {showSelectionPart && selectionPart}
            {showDragPart && <ResetZoomBtn style={{marginLeft: 10}} {...{chartId}} />}
            <SaveBtn {...{chartId}} />
            <RestoreBtn {...{chartId}} />
            {tbl_id && <FiltersBtn {...{chartId}} />}
            <OptionsBtn {...{chartId}} />
            {expandable && <ExpandBtn {...{chartId}} />}
            { help_id && <Stack p={1/4}> <HelpIcon helpId={help_id} /> </Stack>}
        </Stack>
    );
}


function SelectionPart({chartId, hasFilter, hasSelection, hasSelected, tbl_id}) {
    if (! (hasFilter || hasSelection || hasSelected)) return null;   // don't show if nothing to show
    let showSelectSelection = hasSelection;
    if (hasSelection) {
        const {data, activeTrace} = getChartData(chartId);
        showSelectSelection = get(data, `${activeTrace}.hoverinfo`) !== 'skip'
            // for singleTraceUI only - handle heatmap
            && !get(getChartData(chartId), `data.${activeTrace}.type`, '').includes('heatmap');
    }
    return (
        <Stack direction='row' spacing={1/2}>
            {hasFilter    && <ClearFilter {...{tbl_id}} />}
            {hasSelected  && <ClearSelected {...{chartId}} />}
            {hasSelection && <FilterSelection {...{chartId}} />}
            {showSelectSelection && <SelectSelection style={{marginRight:10}} {...{chartId}} />}
        </Stack>
    );
}

function DragModePart({chartId, tbl_id, dragmode, hasSelectionMode, is3d}) {

    return (
        <Stack direction='row' spacing={1/2}>
            <Divider orientation='vertical'/>
            <ToggleButtonGroup value={dragmode} variant='plain' size='sm'
                               sx={{
                                   mx:1/2,
                                   '--ButtonGroup-connected': '0'
                               }}>
                <ZoomBtn {...{chartId}} />
                <PanBtn {...{chartId}} />
                {is3d && <OrbitBtn {...{chartId}} />}
                {is3d && <TurntableBtn {...{chartId}} />}
                {tbl_id && hasSelectionMode && <SelectBtn {...{chartId}} />}
            </ToggleButtonGroup>
            <Divider orientation='vertical'/>
        </Stack>
    );
}

function ZoomBtn({chartId}) {
    return (
        <IconButton value='zoom'
                    onClick={() => dispatchChartUpdate({chartId, changes:{'layout.dragmode': 'zoom', selection: undefined}})}
                    title='Zoom in the enclosed points'
        ><img src={ZoomIco}/></IconButton>
    );
}

function PanBtn({chartId}) {
    return (
        <IconButton value='pan'
                    onClick={() => dispatchChartUpdate({chartId, changes:{'layout.dragmode': 'pan', selection: undefined}})}
                    title='Pan'
        ><img src={PanIco}/></IconButton>
    );
}

function TurntableBtn({chartId}) {
    return (
        <IconButton value='turntable'
                    onClick={() => dispatchChartUpdate({chartId, changes:{'layout.dragmode': 'turntable', selection: undefined}})}
                    title='Turntable rotation'
        ><img src={TurntableIco}/></IconButton>
    );
}

function OrbitBtn({chartId}) {
    return (
        <IconButton value='orbit'
                    onClick={() => dispatchChartUpdate({chartId, changes:{'layout.dragmode': 'orbit', selection: undefined}})}
                    title='Orbital rotation'
        ><img src={OrbitalIco}/></IconButton>
    );
}

function SelectBtn({chartId}) {
    return (
        <IconButton value='select'
                    onClick={() => dispatchChartUpdate({chartId, changes:{'layout.dragmode': 'select', selection: undefined}})}
                    title='Select'
        ><img src={SelectIco}/></IconButton>
    );
}

function ResetZoomBtn({style={}, chartId}) {
    const doClick = () => {
        const {_original, layout} = getChartData(chartId) || {};

        // 2d axes
        const changes = ['xaxis','yaxis'].reduce((pv, axis) => {
            if (get(layout, `${axis}`)) {
                const range = get(layout, `${axis}.range`) || [];
                const autorange = get(layout, `${axis}.autorange`);
                const reversed = (autorange === 'reversed') || (range[1] < range[0]);

                pv[`layout.${axis}.autorange`] = reversed ? 'reversed' : true;
                pv[`layout.${axis}.range`] = undefined;
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
        <IconButton onClick={doClick}
             title='Zoom out to original range'
        ><img src={ResetZoomIco}/></IconButton>
    );
}

function RestoreBtn({chartId}) {
    return (
        <ResetButton onClick={() => { resetChart(chartId);}}
                     title='Reset chart to the original'/>
    );
}

function SaveBtn({chartId}) {
    return (
        <SaveButton onClick={() => { downloadChart(chartId);}}
             title='Download the chart as a PNG image'/>
    );
}

function FiltersBtn({chartId}) {
    return (
        <FilterButton onClick={() => showFilterDialog(chartId)}/>
    );
}

function OptionsBtn({chartId}) {
    return (
        <SettingsButton onClick={() => showChartsDialog(chartId)}
             title='Chart options and tools'/>
    );
}


function ExpandBtn({chartId} ){
    const expand = () => {
        const expandedViewerId= findViewerWithItemId(getMultiViewRoot(), chartId,PLOT2D);
        const layout = getLayoutType(getMultiViewRoot(), expandedViewerId);                // record the current layout before expanding
        dispatchChartExpanded({chartId, expandedViewerId, layout});
        // dispatchChangeViewerLayout(expandedViewerId,'single');                      // auto-switch to single view so that the active chart is fully expanded
        dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.xyPlots);
    };

    return (
        <ExpandButton onClick={expand}
             title='Expand this panel to take up a larger area'/>
    );
}

export function ActiveTraceSelect({sx={}, chartId, activeTrace}) {
    const {data} = getChartData(chartId) || [];
    if (!data || data.length < 2) return null;

    return (<ListBoxInputFieldView value={activeTrace}
                                   options={data.map((trace, idx) => {
                                       const option = get(trace, 'name', `trace ${idx}`);
                                       return {label: option, value: idx};
                                   })}
                                   onChange={(e, newValue) => dispatchSetActiveTrace({chartId, activeTrace: newValue})}
                                   sx={sx}
                                   slotProps={{input: {size: 'sm'}}}
    />);
}

function FilterSelection({style={}, chartId}) {
    return (
        <IconButton onClick={() => dispatchChartFilterSelection({chartId})}
             title='Filter on the selected points'
        ><img src={ApplyFilterIco}/></IconButton>
    );
}

function SelectSelection({chartId}) {
    const onClick = () => {
            const selIndexes = get(getChartData(chartId), 'selection.points', []);
            dispatchChartSelect({chartId, selIndexes, chartTrigger: true});
        };
    return (
        <IconButton onClick={onClick}
             title='Select the enclosed points'
        ><img src={ApplySelectIco}/></IconButton>
    );
}

function ClearSelected({chartId}) {
    return (
        <IconButton onClick={() => dispatchChartSelect({chartId, selIndexes:[], chartTrigger: true})}
             title='Unselect all selected points'
        ><img src={ClearSelectIco}/></IconButton>
    );
}

function ClearFilter({tbl_id}) {
    return <ClearFilterButton onClick={() => clearFilters(getTblById(tbl_id))}/>;
}

/**
 * Creates and shows the modal dialog with filter options.
 * @param {string} chartId
 */
function showFilterDialog(chartId) {
    const {data, fireflyData, activeTrace} = getChartData(chartId);
    const tbl_id = get(data, `${activeTrace}.tbl_id`) || get(fireflyData, `${activeTrace}.tbl_id`);
    const content= (
        <FilterEditorWrapper tbl_id={tbl_id}/>
    );

    showOptionsPopup({content, title: 'Filters', modal: true, show: true});
}