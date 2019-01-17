/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {set, get, groupBy, isPlainObject} from 'lodash';

import Tree from 'rc-tree';
import 'rc-tree/assets/index.css';


/**
 * This component renders a TableModel as a tree.  It will use the `groupBy` prop
 * to transform the table into a tree.
 * Based on rc-tree:  http://react-component.github.io/tree/
 *
 */
export class TableTree extends PureComponent {

    constructor(props) {
        super(props);
    }

    render() {
        const {tableModel, groupBy, onSelect, valueRenderer, iconRenderer, className, nodeClassName} = this.props;
        const treeData = transformToTreeNodes(tableModel, groupBy, nodeClassName, valueRenderer);
        return (
            <Tree defaultExpandAll showLine treeData={treeData}/>
        );
    }
}
TableTree.propTypes = {
    tableModel:     PropTypes.object.isRequired,
    groupBy:        PropTypes.arrayOf(PropTypes.string).isRequired,
    onSelect:       PropTypes.func,
    valueRenderer:   PropTypes.func,            // TreeNode props will be given as parameter.  rowData is available for leaf node
    iconRenderer:   PropTypes.func,             // TreeNode props will be given as parameter.  rowData is available for leaf node
    className:      PropTypes.string,
    nodeClassName:  PropTypes.string,
};


function Icon({ selected, isLeaf, renderer }) {
    
}

/*
 * Takes the tableModel and transform it into an object tree
 * by grouping the rows based on the given groupByCols
 */
export function transformToTreeNodes(tableModel, groupByCols, nodeClassName, valueRenderer) {
    const columns = get(tableModel, 'tableData.columns', {});
    const tableData = get(tableModel, 'tableData.data', []);
    const groupByIdx = groupByCols.map((cname) => columns.findIndex((c) => c.name === cname));

    const groupByFunc = (row) => groupByIdx.map((idx) => row[idx]).join('--');

    const treeData = {};

    // transform into a tree with children as objects keyed by its key
    Object.entries(groupBy(tableData, (row) => groupByFunc(row)))
        .forEach(([k, rows]) => {
            const keys = k.split('--');
            // creating parent nodes studs if not yet created
            keys.forEach((k, idx) => {
                const ckey = keys.slice(0,idx+1);
                const path = ckey.reduce( (r, e) => {r.push('children', e);return r;}, []);
                const key = ckey.join('--');
                const title = valueRenderer ? valueRenderer({key, isLeaf: false}) : k;
                set(treeData, path.concat(['key']), key);
                set(treeData, path.concat(['title']), title);
                set(treeData, path.concat(['className']), nodeClassName);
            });

            // inserting leaf nodes
            const path = keys.reduce( (r, e) => {r.push('children', e);return r;}, []);
            rows.forEach((r, idx) => {
                const key = `${k}--${idx}`;
                const title = valueRenderer ? valueRenderer({key, isLeaf: true, rowData: rows[idx]}) :
                                    rows[idx].filter((e, idx) => !groupByIdx.includes(idx)).join('--');  // use the remain columns as value, separated by '--'
                set(treeData, path.concat(['children', idx]), {key: `${k}--${idx}`, title});
            });
        });

    // transform the children into array to match rc-tree expectation
    const transChildren = (obj) => {
        Object.values(obj).forEach( (v) => {
            if (isPlainObject(v)) transChildren(v);
        });
        if (isPlainObject(obj.children)) {
            obj.children = Object.values(obj.children);
        }
    };
    transChildren(treeData);
    return treeData.children;
}



















