import {getActiveTableId, getTblById, tableDetailsView} from '../TableUtil.js';
import {TablePanel} from 'firefly/tables/ui/TablePanel';
import React, {useEffect} from 'react';
import {useStoreConnector} from 'firefly/ui/SimpleComponent';
import {dispatchTableAddLocal, dispatchTableUiUpdate} from 'firefly/tables/TablesCntlr';
import {getAppOptions} from 'firefly/core/AppDataCntlr';
import {NoDataTableView} from 'firefly/tables/ui/BasicTableView';
import {TableMask} from 'firefly/ui/panel/MaskPanel';

/**
 * A wrapper/watcher component for property sheet i.e., vertical display of all the data from a single table row, with additional metadata.
 * When tbl_id is not given, it will use the active table id in the given table group.
 * If data is present, it passes the table id and highlighted row in it to its children
 * for them to process and display the table row data as needed. If data is not ready, it also handles different states like loading, no data, etc.
 *
 * Note: Only pass tbl_id prop if you want to show property sheet for only the passed table ID, bypassing the active table watching.
 *
 * @param props
 * @param props.tbl_group Group of the table, optional.
 * @param props.tbl_id ID of the table, optional. If not provided (preferred), it will be set as ID of the active table in the group.
 * @param props.children React element(s) to enclose within this component. Active table's ID and highlighted row's index will be implicitly passed to it as props.
 * @param props.fetcher Function to fetch additional data
 * @returns {JSX.Element}
 */
export function PropertySheet({tbl_group='main', tbl_id, children, fetcher}){
    const {watchTblId, highlightedRow , isFetching, totalRows} = useStoreConnector(() => {
        const tblId = tbl_id || getActiveTableId(tbl_group);
        const tableModel = getTblById(tblId);
        if (!tableModel) return {watchTblId: tblId, totalRows: -1};
        const {highlightedRow=0,isFetching,totalRows} = tableModel;

        return {watchTblId: tblId, highlightedRow,isFetching, totalRows};
    }, [tbl_id, tbl_group]);

    if(totalRows === -1) return false;

    // TODO: use fetcher if defined

    if(isFetching) {
        return <TableMask/>;
    }
    else if (totalRows === 0) {
        return <NoDataTableView>No rows in table</NoDataTableView>;
    }
    else if (!highlightedRow && highlightedRow!==0) {
        return <NoDataTableView>No currently selected row</NoDataTableView>;
    }
    else {
        return React.cloneElement(children, {...{tbl_id: watchTblId, highlightedRow}});
    }
}

/**
 * A renderer component for property sheet that displays the highlighted row of the active table as a table.
 * The table produced is a client-side table which is capable of processing requests in the previous renders, including filters.
 *
 * @param props
 * @param {TblOptions} props.tblOptions
 * @param props.tbl_id [passed implicitly if child of PropertySheet] ID of the active table in property sheet
 * @param props.highlightedRow [passed implicitly if child of PropertySheet] index of highlighted row in the active table
 * @returns {JSX.Element}
 */
export function RowDetailsTable({tblOptions={}, tbl_id, highlightedRow, ...tblProps}) {

    const detailsTblId = `${tbl_id}-RowDetailsTable`;
    const detailsAnchor = 'tables.rowdetails';

    useEffect(()=>{
        const detailsTable = tableDetailsView(tbl_id, highlightedRow, detailsTblId);

        dispatchTableAddLocal(detailsTable, {
            tbl_group: detailsTblId,
            tbl_ui_id: detailsTblId,
            selectable: getAppOptions()?.table?.propertySheet?.selectableRows ?? true,
            showToolbar: true,
            showPropertySheetButton: false, // property sheet table shouldn't show property sheet button
            showInfoButton: false,
            removable: false,
            showFilters: true,
            showTypes: true,
            showUnits: false,
            expandable: true,
            help_id: detailsAnchor,
            ...tblOptions
        });
    }, [tbl_id, highlightedRow]);

    useEffect(()=>{
        dispatchTableUiUpdate({tbl_ui_id: detailsTblId, allowUnits: false, allowTypes: true});
    }, [detailsTblId]);

    return (<TablePanel tbl_id={detailsTblId} tbl_ui_id={detailsTblId} showTitle={false} {...tblProps}/>);
}


/**
 * Compound component that renders property sheet as table.
 * It simplifies this usage: <PropertySheet><RowDetailsTable></PropertySheet>.
 *
 * @param props refer to the documentation of above 2 components.
 * @param props.tbl_group
 * @param props.tbl_id
 * @param props.tblOptions
 */
export function PropertySheetAsTable({tbl_group, tbl_id, tblOptions, ...tblProps}) {
    return (
        <PropertySheet {...{tbl_group, tbl_id}}>
            <RowDetailsTable {...{tblOptions, ...tblProps}}/>
        </PropertySheet>
    );
}
