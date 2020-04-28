import React from 'react';
import {useStoreConnector} from '../../ui/SimpleComponent';
import {getActiveTableId, getTblInfoById, getCellValue} from '../../tables/TableUtil';

export function PngViewer() {
    const [tbl_id] = useStoreConnector(() => () => getActiveTableId());
    const {tableMeta, tableModel} = getTblInfoById(tbl_id);
    const highlightedRow = tableModel? tableModel.highlightedRow: 0;
    const PngSource = tableMeta.PngSource ? tableMeta.PngSource : '';
    const png_url = getCellValue(tableModel, highlightedRow, PngSource);

    if (png_url && png_url.endsWith('.png')) {
        return (
            <div style={{
                display: 'flex',
                flexDirection: 'column',
                background: '#c8c8c8',
                width: '100%',
                height: '100%'
            }}>
                <div style={{overflow: 'auto', display: 'flex', justifyContent: 'center', alignItem: 'center'}}>
                    <img src={png_url} alt={png_url} style={{maxWidth: '100%', flexGrow: 0, flexShrink: 0}}/>
                </div>
            </div>
        );
    }else{
        return (
            <div className='TablePanel_NoData'>
                <p>No Preview Available</p>
            </div>
        );
    }
};