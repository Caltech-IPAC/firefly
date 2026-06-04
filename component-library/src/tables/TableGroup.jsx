import React, { useEffect, useRef } from 'react';
import PropTypes from 'prop-types';
import { TablesContainer } from 'firefly/tables/ui/TablesContainer.jsx';
import { useStoreConnector } from 'firefly/ui/SimpleComponent.jsx';
import { getTableGroup } from 'firefly/tables/TableUtil.js';

/**
 * Displays a group of Firefly tables as tabs.
 *
 * Tables are added to the group via `dispatchTableAddLocal` or `dispatchTableSearch`
 * with a matching `tbl_group` key. This component renders whatever tables are
 * currently registered under that group.
 */
export function TableGroup({ tbl_group, closeable = true, forceSingleTableAsTab = false, options, events = {}, ...props }) {
    useGroupEventHandlers(tbl_group, events);

    return (
        <TablesContainer
            tbl_group={tbl_group}
            closeable={closeable}
            forceSingleTableAsTab={forceSingleTableAsTab}
            tableOptions={options}
            mode='standard'
            {...props}
        />
    );
}

// Watches the group in the store and fires event callbacks when membership or
// active table changes. Skips the subscription when no callbacks are provided.
function useGroupEventHandlers(tbl_group, events) {
    const hasEvents = Object.keys(events).length > 0;
    const group     = useStoreConnector(() => hasEvents ? getTableGroup(tbl_group) : null, [tbl_group, hasEvents]);
    const prevRef   = useRef(null);

    useEffect(() => {
        const prev = prevRef.current;
        const { onActiveChange, onTableAdded, onTableRemoved } = events;

        if (group) {
            const { active, tables = {} } = group;
            const prevTables = prev?.tables ?? {};

            if (prev && active !== prev.active) {
                onActiveChange?.(active, tbl_group);
            }

            if (prev) {
                Object.keys(tables)
                    .filter((id) => !prevTables[id])
                    .forEach((id) => onTableAdded?.(id, tbl_group));

                Object.keys(prevTables)
                    .filter((id) => !tables[id])
                    .forEach((id) => onTableRemoved?.(id, tbl_group));
            }
        }

        prevRef.current = group ? { active: group.active, tables: group.tables ?? {} } : undefined;
    }, [group]);
}

TableGroup.propTypes = {
    /** The group key that identifies which tables to display. */
    tbl_group: PropTypes.string,
    /** Show a close button on each table tab. */
    closeable: PropTypes.bool,
    /** Render a tab bar even when there is only one table. */
    forceSingleTableAsTab: PropTypes.bool,
    /** Options passed through to each TablePanel inside the group. */
    options: PropTypes.object,
    /** Theme-aware styles applied to the container. */
    sx: PropTypes.object,

    /** Event callbacks. */
    events: PropTypes.shape({
        /** Fired when the active (focused) tab changes. Receives `(tbl_id, tbl_group)`. */
        onActiveChange: PropTypes.func,
        /** Fired when a table is added to the group. Receives `(tbl_id, tbl_group)`. */
        onTableAdded:   PropTypes.func,
        /** Fired when a table is removed from the group. Receives `(tbl_id, tbl_group)`. */
        onTableRemoved: PropTypes.func,
    }),
};
