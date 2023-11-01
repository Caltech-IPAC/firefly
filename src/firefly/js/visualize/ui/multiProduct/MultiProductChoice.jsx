import React, {useEffect, useState} from 'react';
import {MultiChartViewer} from '../../../charts/ui/MultiChartViewer.jsx';
import {SHOW_CHART, SHOW_IMAGE, SHOW_TABLE} from '../../../metaConvert/DataProductsType.js';
import {getMetaEntry, getTableGroup, getTblById, onTableLoaded} from '../../../tables/TableUtil.js';
import {TablesContainer} from '../../../tables/ui/TablesContainer.jsx';
import {RadioGroupInputFieldView} from '../../../ui/RadioGroupInputFieldView.jsx';
import {NewPlotMode} from '../../MultiViewCntlr.js';
import {ImageMetaDataToolbar} from '../ImageMetaDataToolbar.jsx';
import {MultiImageViewer} from '../MultiImageViewer.jsx';


const imageOp= {label: 'Image', value: SHOW_IMAGE};

const choiceTBStyle= {display:'flex', flexDirection: 'column', background: '#c8c8c8', width:'100%', height:'100%'};
const chartChoiceTBStyle= { width:'100%', height:'calc(100% - 30px)', background: '#c8c8c8'};
const tbInternalStyle= {display:'flex', flexDirection: 'row', alignItems:'center', height:30};



export function MultiProductChoice({
                                       makeDropDown, chartViewerId, imageViewerId, metaDataTableId,
                                       tableGroupViewerId, whatToShow, onChange, mayToggle = false, factoryKey
                                   }) {
    const chartTableOptions = [{label: 'Table', value: SHOW_TABLE}, {label: 'Chart', value: SHOW_CHART}];
    const options = !imageViewerId ? chartTableOptions : [...chartTableOptions, imageOp];
    const [chartName, setChartName] = useState('Chart');
    options[1].label = chartName;
    const tbl_id = getTableGroup(tableGroupViewerId)?.active;
    const table = tbl_id ? getTblById(tbl_id) : undefined;
    useEffect(() => {
        if (!table) return;
        onTableLoaded(tbl_id).then(() => {
            const name = (getMetaEntry(tbl_id, 'utype') === 'spec:Spectrum') ? 'Spectrum' : 'Chart';
            setChartName(name);
        });
    }, [table]);
    const toolbar = (
        <div style={tbInternalStyle}>
            {makeDropDown && <div style={{height: 30}}> {makeDropDown()} </div>}
            {mayToggle && <RadioGroupInputFieldView wrapperStyle={{paddingLeft: 20}}
                                                    {...{options, value: whatToShow, buttonGroup: true, onChange}} />}
        </div>);

    switch (whatToShow) {
        case SHOW_CHART:
            return (
                <div style={chartChoiceTBStyle}>
                    {toolbar}
                    <MultiChartViewer viewerId={chartViewerId} closeable={false}
                                      autoRowOriented={false}
                                      canReceiveNewItems={NewPlotMode.none.key}/>
                </div>
            );
        case SHOW_TABLE:
            return (
                <div style={choiceTBStyle}>
                    {toolbar}
                    <TablesContainer tbl_group={tableGroupViewerId} closeable={false} expandedMode={false}/>
                </div>
            );
        case SHOW_IMAGE:
            return (
                <div style={choiceTBStyle}>
                    {mayToggle && toolbar}
                    <MultiImageViewer {...{
                        viewerId:imageViewerId, insideFlex:true,
                        canReceiveNewPlots: NewPlotMode.none.key, tableId:metaDataTableId, controlViewerMounting:false,
                        makeDropDown: !mayToggle ? makeDropDown : undefined,
                        Toolbar:ImageMetaDataToolbar, factoryKey}} />
                </div>
            );
    }
    return false;
}
