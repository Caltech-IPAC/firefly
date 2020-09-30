import React,{memo,useState} from 'react';
import {useStoreConnector} from '../../ui/SimpleComponent';
import {getActiveTableId, getCellValue, getMetaEntry, getTblById} from '../../tables/TableUtil';

export const PngViewer = memo(()=> {
    const [tbl_id, highlightedRow] = useStoreConnector(
        () => getActiveTableId(),
        () => getTblById(getActiveTableId()) ?. highlightedRow ?? -1
    );
    const tableModel = getTblById(tbl_id);
    const [badUrl, setBadUrl] = useState(undefined);

    if (!tableModel || highlightedRow <0 || tableModel.isFetching){
        return noPreview();
    }

    const PngSource= getMetaEntry(tbl_id, 'ImagePreview', '');
    const png_url = getCellValue(tableModel, highlightedRow, PngSource);

    if (png_url && png_url !== badUrl) {
        return (
            <div style={{
                display: 'flex',
                flexDirection: 'column',
                background: '#c8c8c8',
                width: '100%',
                height: '100%'
            }}>
                <div style={{overflow: 'auto', display: 'flex', justifyContent: 'center', alignItem: 'center'}}>
                    <img src={png_url} alt={`Preview not found: ${png_url}`}
                         onError={()=>setBadUrl(png_url)}
                         style={{width:'100%', objectFit:'contain', flexGrow: 0, flexShrink: 0}}
                    />
                </div>
            </div>
        );
    }else{
        return noPreview();
    }
});


function noPreview() {
    return (
        <div className='TablePanel_NoData'>
            <p>No Preview Available</p>
        </div>
    );
}