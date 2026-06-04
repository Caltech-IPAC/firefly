import React, { useId, useEffect, useRef } from 'react';
import PropTypes from 'prop-types';
import { TablePanel } from 'firefly/tables/ui/TablePanel.jsx';
import { useStoreConnector } from 'firefly/ui/SimpleComponent.jsx';
import { getTblById } from 'firefly/tables/TableUtil.js';
import { dispatchTableSearch } from 'firefly/tables/TablesCntlr.js';

/**
 * Displays a Firefly table.
 *
 * The `source` prop accepts three forms:
 * - **string** — `tbl_id` of a table already loaded in the Firefly store. No request is dispatched.
 * - **tableModel object** — an in-memory table (detected by the presence of a `tableData` field).
 * - **request object** — a server-side request created with `makeFileRequest` or `makeTblRequest`.
 *
 * Features:
 * - **Paging**: navigates large result sets a page at a time.
 * - **Sorting**: click any column header to sort ascending or descending.
 * - **Filtering**: toolbar filter button opens an inline filter row for per-column constraints;
 *   advanced mode accepts SQL-style expressions for complex queries.
 * - **Add column**: define new computed columns using expressions over existing columns.
 * - **Text mode**: toggle a raw-text view of the underlying table data.
 * - **Row selection**: checkbox column for selecting one or more rows.
 * - **Column options**: show/hide individual columns and resize them.
 * - **Save/download**: export the current (filtered/sorted) table to a file.
 * - **Full-screen expand**: expand the table to fill the viewport.
 * - **Event callbacks**: subscribe to highlight, select, sort, filter, load, and remove events
 *   via the `events` prop.
 */
export function DataTable({ source, tbl_ui_id: tbl_ui_id_prop, options = {}, events = {}, ...props }) {
    const generatedId = useId();
    const { request, tableModel, tbl_id: sourceTblId } = parseSource(source);
    const tbl_id    = sourceTblId ?? `tbl-${generatedId}`;
    const tbl_ui_id = tbl_ui_id_prop ?? `${tbl_id}-ui`;

    useEffect(() => {
        const removable = props?.removable;
        if (request) dispatchTableSearch(request, {tbl_ui_id, removable});
    }, [tbl_id]); // eslint-disable-line react-hooks/exhaustive-deps

    useEventHandlers(tbl_id, events);

    return (
        <TablePanel
            tbl_id={tbl_id}
            tbl_ui_id={tbl_ui_id}
            tableModel={tableModel}
            {...options}
            {...props}
        />
    );
}

// Subscribes to store changes and fires event callbacks when table state changes.
// Skips the subscription entirely when no callbacks are provided.
function useEventHandlers(tbl_id, events) {
    const hasEvents = Object.keys(events).length > 0;
    const tbl       = useStoreConnector(() => hasEvents ? getTblById(tbl_id) : null, [tbl_id, hasEvents]);
    const prevRef   = useRef(null);

    useEffect(() => {
        const prev = prevRef.current;
        const { onHighlight, onSelect, onSort, onFilter, onLoaded, onRemove } = events;

        if (tbl === undefined && prev !== undefined) {
            onRemove?.(tbl_id);
        }

        if (tbl) {
            if (prev && tbl.highlightedRow !== prev.highlightedRow) {
                onHighlight?.(tbl.highlightedRow, tbl_id);
            }
            if (prev && tbl.selectInfo !== prev.selectInfo) {
                onSelect?.(tbl.selectInfo, tbl_id);
            }
            if (prev && tbl.request?.sortInfo !== prev.sortInfo) {
                onSort?.(tbl.request?.sortInfo, tbl_id);
            }
            if (prev && tbl.request?.filters !== prev.filters) {
                onFilter?.(tbl.request?.filters, tbl_id);
            }
            if (prev?.isFetching && !tbl.isFetching) {
                onLoaded?.(tbl);
            }
        }

        prevRef.current = tbl ? {
            highlightedRow: tbl.highlightedRow,
            selectInfo:     tbl.selectInfo,
            sortInfo:       tbl.request?.sortInfo,
            filters:        tbl.request?.filters,
            isFetching:     tbl.isFetching,
        } : undefined;
    }, [tbl]);
}

// Interprets the polymorphic `source` prop into { request, tableModel, tbl_id }.
// - string          → tbl_id (table already in the store)
// - object w/ tableData → tableModel (in-memory data)
// - any other object    → request (server-side fetch)
function parseSource(source) {
    if (typeof source === 'string')    return { tbl_id: source };
    if (source?.tableData)     return { tableModel: source, tbl_id: source.tbl_id };
    if (source)                return { request: source,    tbl_id: source.tbl_id };
    return {};
}

DataTable.propTypes = {
    /**
     * The data source for the table. Accepts three forms:
     * - **string** — `tbl_id` of a table already in the Firefly store (no request dispatched).
     * - **tableModel object** — in-memory data (detected by the presence of a `tableData` field).
     * - **request object** — server-side request from `makeFileRequest` or `makeTblRequest`.
     *   Must not be recreated on every render — wrap it in `useMemo`.
     */
    source: PropTypes.oneOfType([PropTypes.string, PropTypes.object]).isRequired,

    /**
     * Stable key for this component's UI state in the Firefly store.
     * Defaults to `"${tbl_id}-ui"`. Only set this explicitly when two `DataTable`
     * instances share the same `tbl_id` but need independent scroll/selection state.
     * @default "${tbl_id}-ui"
     */
    tbl_ui_id: PropTypes.string,

    /** Theme-aware styles applied to the container. */
    sx: PropTypes.object,

    /**
     * Display options for the table. All keys are optional and default to the
     * Firefly table defaults (most are `true`).
     *
     * Key options:
     * - `showToolbar`:hide/show the entire toolbar
     * - `showTitle`:hide/show the table title in the toolbar
     * - `showPaging`:hide/show paging controls
     * - `showFilterButton`:hide/show the filter toggle button in the toolbar
     * - `showFilters`:show the inline filter row below column headers
     * - `showSave`:hide/show the download button
     * - `selectable`:enable row-selection checkboxes
     * - `expandable`:enable full-screen expand button
     * - `border`:draw a border around the table
     */
    options: PropTypes.shape({
        /** Show the toolbar. @default true */
        showToolbar:      PropTypes.bool,
        /** Show the table title inside the toolbar. @default true */
        showTitle:        PropTypes.bool,
        /** Show paging controls. @default true */
        showPaging:       PropTypes.bool,
        /** Show the filter toggle button in the toolbar. @default true */
        showFilterButton: PropTypes.bool,
        /** Show the inline filter row below column headers. @default false */
        showFilters:      PropTypes.bool,
        /** Show the save/download button. @default true */
        showSave:         PropTypes.bool,
        /** Show the column-options button. @default true */
        showOptionButton: PropTypes.bool,
        /** Show the info button. @default true */
        showInfoButton:   PropTypes.bool,
        /** Show the add-column button. @default true */
        showAddColumn:    PropTypes.bool,
        /** Show column types in the header row. @default false */
        showTypes:        PropTypes.bool,
        /** Enable row-selection checkboxes. @default true */
        selectable:       PropTypes.bool,
        /** Enable the full-screen expand button. @default true */
        expandable:       PropTypes.bool,
        /** Draw a border around the table. @default true */
        border:           PropTypes.bool,
    }),

    /**
     * Callbacks fired on table interactions. All keys are optional.
     * Each callback also receives `tbl_id` as its last argument.
     *
     * - `onHighlight(rowIdx, tbl_id)`:active row changed
     * - `onSelect(selectInfo, tbl_id)`:row selection changed
     * - `onSort(sortInfo, tbl_id)`:sort order changed
     * - `onFilter(filters, tbl_id)`:filters changed
     * - `onLoaded(tableModel)`:data finished loading
     * - `onRemove(tbl_id)`:table removed from the store
     */
    events: PropTypes.shape({
        /** Called when the highlighted (active) row changes. `(rowIdx, tbl_id) => void` */
        onHighlight: PropTypes.func,
        /** Called when row selection changes. `(selectInfo, tbl_id) => void` */
        onSelect:    PropTypes.func,
        /** Called when the sort order changes. `(sortInfo, tbl_id) => void` */
        onSort:      PropTypes.func,
        /** Called when filters change. `(filters, tbl_id) => void` */
        onFilter:    PropTypes.func,
        /** Called when the table finishes loading. `(tableModel) => void` */
        onLoaded:    PropTypes.func,
        /** Called when the table is removed from the store. `(tbl_id) => void` */
        onRemove:    PropTypes.func,
    }),
};
