import {useFieldGroupValue} from 'firefly/ui/SimpleComponent';
import {Box, Chip, Stack, Typography} from '@mui/joy';
import {ColumnFld, getColValidator} from 'firefly/charts/ui/ColumnOrExpression';
import {TextButton} from 'firefly/visualize/ui/Buttons';
import {showUploadTableChooser} from 'firefly/ui/UploadTableChooser';
import {showColSelectPopup} from 'firefly/charts/ui/ColSelectView';
import {getSizeAsString} from 'firefly/util/WebUtil';
import React, {useEffect, useState} from 'react';
import {onTableLoaded} from 'firefly/tables/TableUtil';
import {FieldGroupCollapsible} from 'firefly/ui/panel/CollapsiblePanel';
import {FilterInfo} from 'firefly/tables/FilterInfo';
import {dispatchTableFilter} from 'firefly/tables/TablesCntlr';

const UploadSingleColumn = 'uploadSingleColumn';
const defaultTblId = 'singleColTable';

/**
 * This component may be used for any multi-object uploads, where we want user to only select one column
 * from the uploaded file.
 *
 * As of 7/12/2024, ObjectIDSearch on TAP and Search by ID in Euclid make use of this.
 *
 * If you just want a regular UploadTableSelector that'll have 2 Position Columns (ra and dec) from the uploadd file,
 * please use the UploadTableSelectorPosCol component
 * @param props
 * @param props.uploadInfo
 * @param props.setUploadInfo
 * @param props.headerTitle
 * @param props.colName
 * @param props.getUseSelectIn used for ObjectID on TAP
 * @returns {JSX.Element}
 */
export function UploadTableSelectorSingleCol({uploadInfo, setUploadInfo,
                                                 headerTitle='Uploaded Column:',
                                                 colName = '',
                                                 getUseSelectIn = () => {} }) {
    const [getSingleCol,setSingleCol]= useFieldGroupValue(UploadSingleColumn);
    const {fileName,columns,totalRows,fileSize}= uploadInfo ?? {};
    const columnsUsed= columns?.filter( ({use}) => use)?.length ?? 0;
    const openKey= 'upload-single-column';

    const preSetUploadInfo= (ui) => {
        setSingleCol('', {validator: getColValidator(ui.columns, true, false), valid: true});
        setUploadInfo(ui);
    };

    const haveFile= Boolean(fileName && columns);

    const onColsSelected = (selectedColNames) => {
        //get rid of extra quotes within each selectedColNames - because non-alphanumeric entries may have
        //been quoted by calling quoteNonAlphanumeric
        // , e.g.: ['"Object Name"', 'RA', 'Notes']
        selectedColNames = selectedColNames.map((col) => col.replace(/^"(.*)"$/, '$1'));
        const columns = uploadInfo?.columns.map((col) => (
            {...col, use:selectedColNames.includes((col.name))}));
        uploadInfo = {...uploadInfo, columns};
        setUploadInfo(uploadInfo);
    };

    return (
        <Stack mt={2} pl={1}>
            <Stack direction='row' alignItems='center'>
                <TextButton text={fileName ? 'Change Upload Table...' : 'Add Upload Table...'}
                            onClick={() => showUploadTableChooser(preSetUploadInfo,'singleColSelect',{colTypes: ['int','long','string'],colCount: 3})} style={{marginLeft: 42}} />
                {haveFile && (
                    <Typography pl={2} sx={{fontSize:'larger'}}>
                        {`${fileName}`}
                    </Typography>
                )}
            </Stack>
            {haveFile && (
                <Stack direction='row' ml={24} justifyContent='flex-start'>
                    <Typography>
                        Rows: {totalRows},
                    </Typography>
                    <Box pl={1} sx={{whiteSpace: 'nowrap'}}>
                        {getUseSelectIn() !== 'use' && (
                            <>
                                <Chip
                                    onClick={() =>
                                        showColSelectPopup(columns, onColsSelected, 'Choose Columns', 'OK', null, true)
                                    }
                                >
                                    <Typography level={'body-xs'}>
                                        Columns: {columns.length} (using {columnsUsed})
                                    </Typography>
                                </Chip>
                                {fileSize && <span>,</span>}
                            </>
                        )}
                    </Box>
                    {fileSize && (
                        <Typography pl={1}>
                            Size: {getSizeAsString(fileSize)}
                        </Typography>
                    )}
                </Stack>
            )}
            {haveFile && (
                <SingleCol
                    singleCol={getSingleCol()}
                    cols={columns}
                    headerTitle={headerTitle}
                    openKey={openKey}
                    headerPostTitle='(from the uploaded table)'
                    headerStyle={{ paddingLeft: 1 }}
                    sx={{ mb: 1, ml: 24 }}
                    colKey={UploadSingleColumn}
                    colName={colName}
                />
            )}
        </Stack>
    );
}

export function SingleCol({singleCol, sx={},cols, colKey, openKey, colName='',
                         headerTitle, headerPostTitle = '', openPreMessage='', headerStyle,colTblId=null}) {
    const posHeader= (
        <Box ml={-1}>
            <Typography display='inline' color={!singleCol?'warning':undefined} level='title-md' style={{...headerStyle}}>
                {(singleCol) ? `${singleCol || 'unset'}` : 'unset'}
            </Typography>
            <Typography display='inline' level='body-sm' pl={2}  whiteSpace='nowrap'>
                {headerPostTitle}
            </Typography>
        </Box>
    );

    const [clickingSelectCols, setClickingSelectCols] = useState(false);

    //clickingSelectCols is toggled each time user clicks on the 'magnifying glass' to select cols, rendering this useEffect
    useEffect(() => {
        onTableLoaded(defaultTblId).then((tbl) => {
            filterTable(cols,tbl);
        });
    }, [clickingSelectCols]);

    return (
        <Box mt={1} sx={{...sx }}>
            <Stack {...{direction:'row', spacing:1}}>
                <Typography pt={1} sx={{width:'14rem', whiteSpace:'nowrap'}} component='div'>
                    {headerTitle}
                </Typography>
                <FieldGroupCollapsible header={posHeader}
                                       initialState={{value:'open'}} fieldKey={openKey}>
                    {openPreMessage && <Typography pb={1}>
                        {openPreMessage}
                    </Typography>}
                    <ColumnFld fieldKey={colKey} cols={cols}
                               name={`${colName} Column`} // label that appears in column chooser
                               tooltip={`${colName} Column`}
                               label={colName}
                               placeholder={`choose ${colName} column`}
                               validator={getColValidator(cols, true, false)}
                               colTblId={colTblId}
                               onSearchClicked= {() => {
                                   if (clickingSelectCols) setClickingSelectCols(false);
                                   else setClickingSelectCols?.(true);
                                   return true;
                               }}
                    />
                </FieldGroupCollapsible>

            </Stack>
        </Box>
    );
}

function filterTable(cols, tbl) {
    const colsWithMetaID = cols.filter((col) => { if (col.ucd) {return col.ucd.includes('meta.id');}  });
    if (colsWithMetaID.length > 1) {
        if (!tbl) return;
        const filterInfo = tbl?.request?.filters;
        const filterInfoCls = FilterInfo.parse(filterInfo);
        const meta = 'meta.id';
        const filter = `like '%${meta}%'`;
        filterInfoCls.setFilter('UCD', filter);
        const newRequest = {tbl_id: tbl.tbl_id, filters: filterInfoCls.serialize()};
        dispatchTableFilter(newRequest);
    }
}



