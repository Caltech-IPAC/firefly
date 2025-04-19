import React, {useContext, useEffect} from 'react';
import {Box, Chip, Stack, Tooltip, Typography} from '@mui/joy';
import PropTypes from 'prop-types';
import {defaultsDeep, omit} from 'lodash';

import {FieldGroupCtx} from 'firefly/ui/FieldGroup';
import {Slot, useStoreConnector} from 'firefly/ui/SimpleComponent';
import {findTableCenterColumns} from 'firefly/voAnalyzer/TableAnalysis';
import {ColumnFld, getColValidator} from 'firefly/charts/ui/ColumnOrExpression';
import {TextButton} from 'firefly/visualize/ui/Buttons.jsx';
import {showUploadTableChooser} from 'firefly/ui/UploadTableChooser';
import {showColSelectPopup} from 'firefly/charts/ui/ColSelectView';
import {getSizeAsString} from 'firefly/util/WebUtil';
import {FieldGroupCollapsible} from 'firefly/ui/panel/CollapsiblePanel';


//--------------------------------------------------------------------------------------------------------------------
// GENERIC components
//--------------------------------------------------------------------------------------------------------------------

export function UploadTableSelector({uploadInfo, setUploadInfo, columnFields=[], columnMappingPanelKey,
                                        allowUploadColumnsSelection=true, slotProps}) {
    const {getVal, setVal, register, unregister}= useContext(FieldGroupCtx);
    const columnFieldValues = useStoreConnector(() => columnFields.map(({fieldKey}) => getVal(fieldKey)));

    const {fileName,columns,totalRows,fileSize} = uploadInfo ?? {};
    const columnsUsed = columns?.filter(({use}) => use);
    columnMappingPanelKey ??= slotProps?.columnMappingPanel?.panelKey ?? 'uploadColumnsMappingPanel';

    useEffect(() => {
        //if user changes column field value(s), make the new columns entries selectable in the columns/search
        const columns = uploadInfo?.columns;

        for (const columnFieldValue of columnFieldValues) {
            const cObj = columns?.find((col) => col.name === columnFieldValue);
            // `.use` flag keeps track of columns in the upload table that are selected to be sent to the service
            if (cObj) cObj.use = true;
        }

        uploadInfo = {...uploadInfo, columns}; // Fixme: do we have to mutate passed prop, define it as a state?
        setUploadInfo(uploadInfo);

        // when setting uploadInfo, also register, so it shows up in the request object
        const additionalReqObjs = {uploadInfo};
        register('additionalParams', () => additionalReqObjs);
        return () => unregister('additionalParams');
    }, [...columnFieldValues]);

    useEffect(() => {
        if (!columns) return;

        // if columns (aka uploaded table) changed, guess column field values and set them in field group state
        const guessedColValues = [];
        for (const columnField of columnFields) {
            if (!getVal(columnField.fieldKey)) {
                const colVal = columnField.guessValue?.(columns) ?? '';
                guessedColValues.push(colVal);
                setVal(columnField.fieldKey, colVal);
            }
        }

        // keep the collapsible open if we couldn't guess value of at least one of the columnFields
        columnMappingPanelKey && setVal(columnMappingPanelKey, guessedColValues.some((val) => !val) ? 'open' : 'closed');
    },[columns]);

    const preSetUploadInfo= (uploadInfo) => {
        // initialize field group state for each column field fieldKey
        for (const columnField of columnFields) {
            setVal(columnField.fieldKey, '', {validator: getColValidator(uploadInfo.columns, true, false), valid: true});
        }
        // set upload info based on what was loaded from upload table dialog
        setUploadInfo(uploadInfo);
    };

    const onColsSelected = (selectedColNames) => {
        //get rid of extra quotes within each selectedColNames - because non-alphanumeric entries may have
        //been quoted by calling quoteNonAlphanumeric
        // , e.g.: ['"Object Name"', 'RA', 'Notes']
        selectedColNames = selectedColNames.map((col) => col.replace(/^"(.*)"$/, '$1'));

        // set `use` key of each column in the uploadInfo as per the selected columns in ColSelectPopup UI
        const columns = uploadInfo.columns.map((col) => (
            {...col, use: selectedColNames.includes(col.name)}
        ));
        uploadInfo = {...uploadInfo, columns}; // Fixme: do we have to mutate passed prop, define it as a state?
        setUploadInfo(uploadInfo);
    };

    const ColumnsSelectionButton = () => {
        const columnsUsedText = columnsUsed?.map(({name})=>name)?.join(', ') || 'None';
        const columnsUsedCount = columnsUsed?.length ?? 0;
        return (
            <Tooltip title={`Only the selected columns (${columnsUsedText}) will be uploaded`}>
                <Chip onClick={() => showColSelectPopup(columns, onColsSelected, 'Choose Columns',
                    'OK', null, true)}>
                    {`${columns.length} columns (${columnsUsedCount} selected)`}
                </Chip>
            </Tooltip>
        );
    };

    const haveTable= Boolean(fileName && columns);

    return (
        <Stack spacing={.5}>
            <Stack {...{direction:'row', spacing: 1.5, alignItems:'center'}}>
                <TextButton text={(fileName&&haveTable) ? 'Change Upload Table...' : 'Add Upload Table...'}
                             onClick={() => showUploadTableChooser(preSetUploadInfo)} />
                {haveTable &&
                    <Typography level='title-lg' sx={{maxWidth: '15rem', overflow:'hidden', whiteSpace:'nowrap',
                        textOverflow:'ellipsis'}}>
                        {fileName}
                    </Typography>
                }
            </Stack>
            {haveTable &&
                <Stack spacing={.5} pl={2}>
                    <Stack {...{direction:'row', spacing: 1, alignItems:'baseline', ...slotProps?.fileInfo}}>
                        <Typography sx={{whiteSpace:'nowrap'}}>
                            {`Rows: ${totalRows}`}
                        </Typography>
                        {fileSize && <Typography sx={{whiteSpace:'nowrap'}}>
                            {`Size: ${getSizeAsString(fileSize)}`}
                        </Typography>}
                        {allowUploadColumnsSelection
                            ? <ColumnsSelectionButton/>
                            : <Typography sx={{whiteSpace:'nowrap'}}> {`Columns: ${columns.length}`}</Typography>
                        }
                    </Stack>
                    {/*render a generic ColumnMappingPanel else a custom component that receives its props*/}
                    <Slot component={ColumnMappingPanel}
                          {...{cols: columns, columnFields, columnFieldValues, panelKey: columnMappingPanelKey,
                              slotProps: slotProps?.columnMappingPanel}}/>
                </Stack>
            }
        </Stack>
    );
}

const columnFieldsType = PropTypes.arrayOf(PropTypes.shape({
    ...omit({...ColumnFld.propTypes}, ['cols']), //because cols come from uploadInfo
    guessValue: PropTypes.func //(columns) => string
}));

UploadTableSelector.propTypes = {
    uploadInfo: PropTypes.object,
    setUploadInfo: PropTypes.func,
    columnFields: columnFieldsType,
    columnMappingPanelKey: PropTypes.string,
    allowUploadColumnsSelection: PropTypes.bool,
    slotProps: PropTypes.shape({
        fileInfo: PropTypes.object,
        columnMappingPanel: PropTypes.shape({
            component: PropTypes.elementType,
            ...ColumnMappingPanel.propTypes
        })
    })
};


export function ColumnMappingPanel({cols, columnFieldValues, columnFields, panelKey,
                                       headerTitle='Mapped Columns:', getHeaderColumnMapping, headerPostTitle = '',
                                       openPreMessage='', sx, slotProps, children}) {
    const defaultColumnMapping = (colValues) => colValues?.every((val) => !val)
        ? 'unset'
        : colValues?.map((val) => val || 'unset').join(', ');

    const panelHeader= (
        <Stack {...{direction:'row', alignItems:'baseline', spacing:1}}>
            <span>{headerTitle}</span>
            <Typography level={'body-md'} sx={{fontWeight:'normal'}}>
                {getHeaderColumnMapping ? getHeaderColumnMapping(columnFieldValues) : defaultColumnMapping(columnFieldValues)}
            </Typography>
            <Typography level='body-sm' sx={{fontWeight:'normal'}}>
                {headerPostTitle}
            </Typography>
        </Stack>
    );

    return (
        <Stack sx={{mt: 1/2, ...sx}}>
            <FieldGroupCollapsible header={panelHeader}
                                   initialState={{value:'closed'}} fieldKey={panelKey}>
                <Stack spacing={2}>
                    {openPreMessage &&
                        <Typography {...slotProps?.openPreMessage} sx={{...slotProps?.openPreMessage?.sx}}>
                            {openPreMessage}
                        </Typography>
                    }
                    {!children && (
                        <Stack {...{spacing: 1, ...slotProps?.columnFieldsRoot}}>
                            {columnFields.map((columnField) => (
                                <Box key={columnField.fieldKey} display='inline-flex'>
                                    <MappedColumnFld cols={cols} {...columnField}/>
                                </Box>
                            ))}
                        </Stack>
                    )}
                    {children}
                </Stack>
            </FieldGroupCollapsible>
        </Stack>
    );
}

ColumnMappingPanel.propTypes = {
    cols: PropTypes.arrayOf(PropTypes.object).isRequired,
    columnFieldValues: PropTypes.arrayOf(PropTypes.string).isRequired,
    columnFields: columnFieldsType.isRequired,
    panelKey: PropTypes.string.isRequired,
    headerTitle: PropTypes.string,
    getHeaderColumnMapping: PropTypes.func, //(columnFieldValues) => node
    headerPostTitle: PropTypes.string,
    openPreMessage: PropTypes.string,
    sx: PropTypes.object,
    slotProps: PropTypes.shape({
        openPreMessage: PropTypes.object,
        columnFieldsRoot: PropTypes.object,
    }),
    children: PropTypes.node,
};

export const MappedColumnFld = ({cols, fieldKey, name, ...props}) => (
    <ColumnFld fieldKey={fieldKey} cols={cols} name={name} //the 3 required props
               // use following defaults if not present in props
               label={name}
               orientation={'vertical'}
               validator={getColValidator(cols, true, false)}
               {...props}/>
);


//--------------------------------------------------------------------------------------------------------------------
// POSITION specific components and helpers
//--------------------------------------------------------------------------------------------------------------------

export const UploadCenterLonColumns = 'uploadCenterLonColumns';
export const UploadCenterLatColumns = 'uploadCenterLatColumns';

export const positionColumnFields = (lonColFields={}, latColFields={}) => [
    {
        fieldKey: UploadCenterLonColumns, name: 'longitude column', label: 'Lon Column',
        tooltip: 'Center longitude column for spatial search',
        guessValue: (columns) => findTableCenterColumns({tableData:{columns}})?.lonCol,
        ...lonColFields
    },
    {
        fieldKey: UploadCenterLatColumns, name: 'latitude column', label: 'Lat Column',
        tooltip: 'Center latitude column for spatial search',
        guessValue: (columns) => findTableCenterColumns({tableData:{columns}})?.latCol,
        ...latColFields
    },
];

const selectorPosColSlotProps = {
    fileInfo: {},
    columnMappingPanel: {
        panelKey: 'upload-pos-columns',
        headerTitle: 'Position Columns:',
        headerPostTitle: '(from the uploaded table)',
        slotProps: {
            columnFieldsRoot: {
                spacing: 2, direction: 'row',
                sx: {'& .ff-Input': {width: '11rem'}}
            }
        }
    }
};


export function UploadTableSelectorPosCol(props) {
    return (
        <UploadTableSelector {...{columnFields: positionColumnFields(), ...props,
            slotProps: defaultsDeep({...props?.slotProps}, selectorPosColSlotProps)}}/>
    );
}

export function CenterColumns({lonCol,latCol, sx, cols, lonKey, latKey, openKey,
                                  doQuoteNonAlphanumeric, headerTitle='Position Columns:',
                                  headerPostTitle = '', openPreMessage='', slotProps}) {
    const columnFields = positionColumnFields(
        {fieldKey: lonKey, doQuoteNonAlphanumeric},
        {fieldKey: latKey, doQuoteNonAlphanumeric}
    );
    const customSlotProps = defaultsDeep({...slotProps}, selectorPosColSlotProps.columnMappingPanel.slotProps);

    return (
        <ColumnMappingPanel cols={cols} columnFieldValues={[lonCol, latCol]} columnFields={columnFields}
                            panelKey={openKey} headerTitle={headerTitle} headerPostTitle={headerPostTitle}
                            openPreMessage={openPreMessage} sx={sx} slotProps={customSlotProps}/>
    );
}

CenterColumns.propTypes = {
    lonCol: PropTypes.string,
    latCol: PropTypes.string,
    sx: PropTypes.object,
    cols: PropTypes.arrayOf(PropTypes.object),
    lonKey: PropTypes.string,
    latKey: PropTypes.string,
    openKey: PropTypes.string,
    doQuoteNonAlphanumeric: PropTypes.bool,
    headerTitle: PropTypes.node,
    headerPostTitle: PropTypes.string,
    openPreMessage: PropTypes.string,
    slotProps: ColumnMappingPanel.propTypes.slotProps,
    children: PropTypes.node
};