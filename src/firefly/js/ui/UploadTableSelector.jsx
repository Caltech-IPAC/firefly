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
import {FilterInfo} from 'firefly/tables/FilterInfo';
import {dispatchTableFilter} from 'firefly/tables/TablesCntlr';
import {onTableLoaded} from 'firefly/tables/TableUtil';


//--------------------------------------------------------------------------------------------------------------------
// GENERIC components and helpers
//--------------------------------------------------------------------------------------------------------------------

/**
 * A generic component that allows users to select an upload table and map its columns to the required fields.
 *
 * @param props
 * @param props.uploadInfo {Object} - state passed from parent that contains upload table info: columns, filename, etc.
 * @param props.setUploadInfo {function} - uploadInfo state setter
 * @param props.columnFields {ColumnFieldsType[]} - array of objects that define the column fields: fieldKey, name are
 * required keys, guessValue and any key that can be passed to ColumnFld as props is optional
 * @param props.columnMappingPanelKey {string} - key for the collapsible panel that contains column mapping
 * @param props.allowUploadColumnsSelection {boolean} - if true, show a button to select columns to upload
 * (note: this is different from columns mapping; selected upload columns can be more than the mapped columns)
 * @param props.defaultUploadColumnsSelection {DefaultColsEnabled} - default selection of columns to upload
 * @param props.slotProps {Object} - slotProps for the component
 * @returns {Element}
 */
export function UploadTableSelector({uploadInfo, setUploadInfo, columnFields=[], columnMappingPanelKey,
                                        allowUploadColumnsSelection=true, defaultUploadColumnsSelection,
                                        slotProps}) {
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

        // if there is only one column in the upload table and only one column field, set that column as column field value
        if (columns?.length === 1 && columnFields?.length === 1) {
            setVal(columnFields[0].fieldKey, columns[0].name);
        }

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
                <TextButton text={(fileName&&haveTable) ? 'Replace Uploaded Table' : 'Upload Table'}
                            onClick={() => showUploadTableChooser(preSetUploadInfo, undefined,
                                defaultUploadColumnsSelection)} />
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

/**
 * @typedef {Object} ColumnFieldsType
 *
 * @prop {string} fieldKey
 * @prop {string} name
 * @prop {function(columns):string} guessValue
 * @prop {*} [additionalProps] - Any additional key that can be passed to ColumnFld as prop.
 */

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
    defaultUploadColumnsSelection: PropTypes.shape({
        colTypes: PropTypes.arrayOf(PropTypes.string),
        colCount: PropTypes.number
    }),
    slotProps: PropTypes.shape({
        fileInfo: PropTypes.object,
        columnMappingPanel: PropTypes.shape({
            component: PropTypes.elementType,
            ...ColumnMappingPanel.propTypes
        })
    })
};

/**
 * A generic component that shows a collapsible panel for mapping columns, with a header and column input fields.
 * Note: this a controlled component whose state is managed by the parent component, generally an UploadTableSelector.
 *
 * @param props
 * @param props.cols {Columns[]} - All available columns to select mappings from
 * @param props.columnFieldValues {string[]} - array of field values, one for each column field in FieldGroupState
 * @param props.columnFields {ColumnFieldsType[]} - array of objects that define the column fields: fieldKey, name are
 * required keys, guessValue and any key that can be passed to ColumnFld as props is optional
 * @param props.panelKey {string} - key for the collapsible panel that contains column mapping
 * @param props.headerTitle {Node} - (prefix) title of the header
 * @param props.getHeaderColumnMapping {function(string[]):string} - function to render the text in header for column mapping
 * @param props.headerPostTitle {string} - (suffix) title text to show after the column mapping in header
 * @param props.openPreMessage {string} - message to show before column input fields when the panel is open
 * @param props.sx {Object} - sx prop used by the FieldGroupCollapsible
 * @param props.slotProps {Object} - slotProps for the component
 * @param props.children {Node} - children to render inside the panel. Note: this will override the default rendering
 * of column input fields
 * @returns {Element}
 */
export function ColumnMappingPanel({cols, columnFieldValues, columnFields, panelKey,
                                       headerTitle='Mapped Columns:', getHeaderColumnMapping, headerPostTitle = '',
                                       openPreMessage='', sx, slotProps, children}) {
    const defaultColumnMapping = (colValues) => colValues?.every((val) => !val)
        ? 'unset'
        : colValues.length===2 && colValues[0]===colValues[1]
            ? colValues[0]
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
        <FieldGroupCollapsible fieldKey={panelKey} initialState={{value:'open'}}
                               header={panelHeader}
                               sx={{mt: 1/2, ...sx}}
                               slotProps={slotProps?.panelRoot}>
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
    );
}

ColumnMappingPanel.propTypes = {
    cols: PropTypes.arrayOf(PropTypes.object).isRequired,
    columnFieldValues: PropTypes.arrayOf(PropTypes.string).isRequired,
    columnFields: columnFieldsType.isRequired,
    panelKey: PropTypes.string.isRequired,
    headerTitle: PropTypes.node,
    getHeaderColumnMapping: PropTypes.func, //(columnFieldValues) => node
    headerPostTitle: PropTypes.string,
    openPreMessage: PropTypes.string,
    sx: PropTypes.object,
    slotProps: PropTypes.shape({
        panelRoot: PropTypes.object,
        openPreMessage: PropTypes.object,
        columnFieldsRoot: PropTypes.object,
    }),
    children: PropTypes.node,
};

/** A convenience wrapper around ColumnFld to use in ColumnMappingPanel.
 *
 * @param props - same as ColumnFld props
 * @param props.cols {Columns} - all column options for the ColumnFld
 * @param props.fieldKey {string} - key for the column field
 * @param props.name {string} - name of the column field
 */
export const MappedColumnFld = ({cols, fieldKey, name, ...props}) => (
    <ColumnFld fieldKey={fieldKey} cols={cols} name={name} //the 3 required props
               // use following defaults if not present in props
               label={name}
               orientation={'vertical'}
               validator={getColValidator(cols, true, false)}
               {...props}/>
);


/**
 * Filter the column table associated with a ColumnFld, by UCD. This helps in reducing column choices to most relevant ones.
 *
 * @param cols {Columns} - all column options for the ColumnFld
 * @param colTblId {string} - id of the column selection table that appears in a popup when the search button is clicked on a ColumnFld
 * @param ucd {string} - UCD to filter by
 */
export function filterMappedColFldTbl(cols, colTblId, ucd) {
    onTableLoaded(colTblId).then((tbl) => {
        const colsWithUcd = cols.filter((col) => { if (col.ucd) {return col.ucd.includes(ucd);} });
        if (colsWithUcd.length > 1) {
            if (!tbl) return;
            const filterInfo = tbl?.request?.filters;
            const filterInfoCls = FilterInfo.parse(filterInfo);
            const ucdFilter = `like '%${ucd}%'`;
            filterInfoCls.setFilter('UCD', ucdFilter);
            const newRequest = {tbl_id: tbl.tbl_id, filters: filterInfoCls.serialize()};
            dispatchTableFilter(newRequest);
        }
    });
}


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


//--------------------------------------------------------------------------------------------------------------------
// ID (Single Column) specific components and helpers
//--------------------------------------------------------------------------------------------------------------------

export const UploadSingleColumn = 'uploadSingleColumn';
export const singleColumnFields = (colName='ID', colField={}) => [
    {
        fieldKey: UploadSingleColumn,
        name: `${colName} Column`, label: colName, placeholder: `choose ${colName} column`,
        ...colField
    }
];

const selectorSingleColSlotProps = {
    fileInfo: {},
    columnMappingPanel: {
        panelKey: 'upload-single-column',
        headerTitle: 'Uploaded ID Column:',
        headerPostTitle: '(from the uploaded table)',
        slotProps: {}
    }
};

export function UploadTableSelectorSingleCol(props) {
    return (
        <UploadTableSelector {...{
            columnFields: singleColumnFields('Object ID'),
            defaultUploadColumnsSelection: { colTypes: ['int', 'long', 'string'], colCount: 3 },
            ...props,
            slotProps: defaultsDeep({...props?.slotProps}, selectorSingleColSlotProps)}}/>
    );
}

export function SingleCol({singleCol, cols, headerTitle='ID Column:', headerPostTitle='', openPreMessage='',
                              openKey, colKey, colTblId, colName, slotProps}) {
    const columnFields = singleColumnFields(colName, {
        fieldKey: colKey,
        colTblId,
        onSearchBtnClicked: () => filterMappedColFldTbl(cols, colTblId, 'meta.id')
    });

    const customSlotProps = defaultsDeep({...slotProps}, selectorSingleColSlotProps.columnMappingPanel.slotProps);

    return (
      <ColumnMappingPanel cols={cols} columnFieldValues={[singleCol]} columnFields={columnFields} panelKey={openKey}
                          headerTitle={headerTitle} headerPostTitle={headerPostTitle} openPreMessage={openPreMessage}
                          slotProps={customSlotProps} />
    );
}

SingleCol.propTypes = {
    singleCol: PropTypes.string,
    cols: PropTypes.arrayOf(PropTypes.object),
    headerTitle: PropTypes.node,
    openKey: PropTypes.string,
    headerPostTitle: PropTypes.string,
    openPreMessage: PropTypes.string,
    colKey: PropTypes.string,
    colTblId: PropTypes.string,
    colName: PropTypes.string,
    slotProps: ColumnMappingPanel.propTypes.slotProps
};
