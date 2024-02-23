import {Box, Chip, Stack, Typography} from '@mui/joy';
import PropTypes from 'prop-types';
import {FieldGroupCtx} from 'firefly/ui/FieldGroup';
import {useFieldGroupValue} from 'firefly/ui/SimpleComponent';
import {findTableCenterColumns} from 'firefly/voAnalyzer/TableAnalysis';
import {ColumnFld, getColValidator} from 'firefly/charts/ui/ColumnOrExpression';
import {ExtraButton} from 'firefly/ui/FormPanel';
import {showUploadTableChooser} from 'firefly/ui/UploadTableChooser';
import {showColSelectPopup} from 'firefly/charts/ui/ColSelectView';
import {getSizeAsString} from 'firefly/util/WebUtil';
import React, {useContext, useEffect} from 'react';
import {FieldGroupCollapsible} from 'firefly/ui/panel/CollapsiblePanel';

export function UploadTableSelector({uploadInfo, setUploadInfo, uploadTable=false}) {

    const UploadCenterLonColumns = 'uploadCenterLonColumns';
    const UploadCenterLatColumns = 'uploadCenterLatColumns';

    const {groupKey,setVal, register, unregister}= useContext(FieldGroupCtx);
    const [getLon,setLon]= useFieldGroupValue(UploadCenterLonColumns);
    const [getLat,setLat]= useFieldGroupValue(UploadCenterLatColumns);

    const {fileName,columns,totalRows,fileSize}= uploadInfo ?? {};
    const columnsUsed= columns?.filter( ({use}) => use)?.length ?? 0;
    //TODO: possibly remove this check if we want pos columns *ALWAYS* open for uploaded tables
    const openKey= uploadTable ? 'open' : 'upload-pos-columns';

    useEffect(() => {
        //if user changes position column(s), make the new columns entries selectable in the columns/search
        const columns = uploadInfo?.columns;
        if (getLon()) {
            const cObj= columns.find((col) => col.name === getLon());
            if (cObj) cObj.use = true;
        }
        if (getLat()) {
            const cObj= columns.find((col) => col.name === getLat());
            if (cObj) cObj.use = true;
        }
        uploadInfo = {...uploadInfo, columns};
        setUploadInfo(uploadInfo);
        //when setting uploadInfo, also register, so it shows up in the request object
        const additionalReqObjs = {uploadInfo};
        register('additionalParams', () => additionalReqObjs);
        return () => unregister('additionalParams');
    }, [getLon, getLat]);

    useEffect(() => {
        if (!columns) return;
        const centerCols = findTableCenterColumns({tableData:{columns}}) ?? {};
        const {lonCol='', latCol=''}= centerCols;
        if (!getLon()) setLon(lonCol);
        if (!getLat()) setLat(latCol);
        //TODO: this line can also be removed if we decide to always keep pos columns open for uplaoded tables
        if (!uploadTable) setVal(openKey, (!lonCol || !latCol) ? 'open' : 'closed');
    },[columns]);

    const preSetUploadInfo= (ui) => {
        setLon('', {validator: getColValidator(ui.columns, true, false), valid: true});
        setLat('', {validator: getColValidator(ui.columns, true, false), valid: true});
        setUploadInfo(ui);
    };

    const haveTable= Boolean(fileName && columns);

    const onColsSelected = (selectedColNames) => {
        //get rid of extra quotes within each selectedColNames - because non-alphanumeric entries may have
        //been quoted by calling quoteNonAlphanumeric
        // , e.g.: ['"Object Name"', 'RA', 'Notes']
        selectedColNames = selectedColNames.map((col) => col.replace(/^"(.*)"$/, '$1'));
        const columns = uploadInfo.columns.map((col) => (
            {...col, use:selectedColNames.includes((col.name))}));
        uploadInfo = {...uploadInfo, columns};
        setUploadInfo(uploadInfo);
    };

    return (
        <div style={{margin: '10px 0 0 0'}}>
            <Stack {...{direction:'row', alignItems:'center'}}>
                <ExtraButton text={(fileName&&haveTable) ? 'Change Upload Table...' : 'Add Upload Table...'}
                             onClick={() => showUploadTableChooser(preSetUploadInfo)} />
                <Stack {...{direction:'row', alignItems:'center'}}>
                    {haveTable &&
                        <Typography level='title-lg' sx={{width:200, overflow:'hidden', whiteSpace:'nowrap',
                            textOverflow:'ellipsis', pl:1.5}}>
                            {`${fileName}`}
                        </Typography>
                    }
                </Stack>
            </Stack>
            {haveTable &&
                <Stack {...{direction:'row', ml: 22, justifyContent:'flex-start'}}>
                    <Typography wx={{whiteSpace:'nowrap'}}>
                        {`Rows: ${totalRows}`}
                    </Typography>
                    {fileSize && <Typography style={{paddingLeft: 8, whiteSpace:'nowrap'}}>
                        {`Size: ${getSizeAsString(fileSize)}`}
                    </Typography>}
                    <Chip onClick={() => showColSelectPopup(columns, onColsSelected, 'Choose Columns', 'OK',
                        null,true)}>
                        {`${columns.length} columns (using ${columnsUsed})`}
                    </Chip>
                </Stack>
            }
            {haveTable &&
                <CenterColumns {...{lonCol: getLon(), latCol: getLat(), cols:columns,
                    sx:{ml:22},
                    headerTitle:'Position Columns:', openKey,
                    headerPostTitle:'(from the uploaded table)',
                    lonKey:UploadCenterLonColumns, latKey:UploadCenterLatColumns}} />
            }
        </div>
    );
}

UploadTableSelector.propTypes = {
    uploadInfo: PropTypes.object,
    setUploadInfo: PropTypes.func,
    uploadTable: PropTypes.bool
};

export function CenterColumns({lonCol,latCol, sx, cols, lonKey, latKey, openKey,
                           headerTitle, headerPostTitle = '', openPreMessage=''}) {


    const posHeader= (
         <Box sx={{ml:-1}}>
             <Typography component='div'>
                 <Stack {...{direction:'row', alignItems:'baseline', spacing:1}}>
                     <span>{headerTitle}</span>
                     <Typography level={'body-md'} sx={{fontWeight:'normal'}}>{(lonCol || latCol) ? `${lonCol || 'unset'}, ${latCol || 'unset'}` : 'unset'}</Typography>
                     <Typography level='body-sm' sx={{fontWeight:'normal'}}>{`${headerPostTitle}`}</Typography>
                 </Stack>
             </Typography>
         </Box>
     );

    return (
        <Stack sx={{mt: 1/2, ...sx}}>
            <FieldGroupCollapsible header={posHeader}
                                   initialState={{value:'closed'}} fieldKey={openKey}>
                {openPreMessage && <Typography sx={{pb:1}}> {openPreMessage} </Typography>}
                <Stack {...{direction:'row', spacing:1, sx:{'& .ff-Input': {width:'11rem'}} }}>
                    <ColumnFld fieldKey={lonKey} cols={cols}
                               name='longitude column'  // label that appears in column chooser
                               tooltip='Center longitude column for spatial search'
                               label='Lon Column'
                               validator={getColValidator(cols, true, false)} />
                    <ColumnFld fieldKey={latKey} cols={cols}
                               name='latitude column' // label that appears in column chooser
                               tooltip='Center latitude column for spatial search'
                               label='Lat Column'
                               validator={getColValidator(cols, true, false)} />
                </Stack>
            </FieldGroupCollapsible>

        </Stack>
    );
}