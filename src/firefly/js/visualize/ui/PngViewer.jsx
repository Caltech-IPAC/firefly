import {Stack, Typography} from '@mui/joy';
import React,{memo,useState} from 'react';
import {useStoreConnector} from '../../ui/SimpleComponent';
import {getActiveTableId, getCellValue, getMetaEntry, getTblById} from '../../tables/TableUtil';

export const PngViewer = memo(()=> {
    const tbl_id         = useStoreConnector(() => getActiveTableId());
    const highlightedRow = useStoreConnector(() => getTblById(getActiveTableId())?.highlightedRow ?? -1);

    const tableModel = getTblById(tbl_id);
    const [badUrl, setBadUrl] = useState(undefined);

    if (!tableModel || highlightedRow <0 || tableModel.isFetching){
        return noPreview();
    }

    const PngSource= getMetaEntry(tbl_id, 'ImagePreview', '');
    const png_url = getCellValue(tableModel, highlightedRow, PngSource);

    if (png_url && png_url !== badUrl) {
        return (
            <Stack {...{ width: 1, height: 1, }}>
                <Stack {...{overflow: 'auto', direction: 'row', justifyContent: 'center', alignItems: 'center'}}>
                    <img src={png_url} alt={`Preview not found: ${png_url}`}
                         onError={()=>setBadUrl(png_url)}
                         style={{width:'100%', objectFit:'contain', flexGrow: 0, flexShrink: 0}}
                    />
                </Stack>
            </Stack>
        );
    }
    else {
        return noPreview();
    }
});


const noPreview= () => (
        <Stack {...{ width: 1, height: 1, alignItems:'center'}}>
            <Typography level='title-lg' sx={{pt:5, textAlign:'center'}}>No Preview Available</Typography>
        </Stack>
    );