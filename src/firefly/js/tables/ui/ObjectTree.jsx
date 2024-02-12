/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {set, get, isObject} from 'lodash';

import Tree from 'rc-tree';
import 'rc-tree/assets/index.css';


/**
 * This component renders a TableModel as a tree.  It will use the `groupBy` prop
 * to transform the table into a tree.
 * Based on rc-tree:  http://react-component.github.io/tree/
 *
 */
export const ObjectTree = React.memo(({data, defaultExpandAll=true, selectable=false, onSelect, nodeClassName, className, style, title}) => {
    const treeData = transformToTreeNodes(data, nodeClassName);
    return (
        <div className={className} style={style}> {title || ''}
            <Tree {...{defaultExpandAll,onSelect,selectable }} treeData={get(treeData, [0, 'children'])}/>
        </div>
    );
});

ObjectTree.propTypes = {
    data:               PropTypes.object.isRequired,
    defaultExpandAll:   PropTypes.bool,
    onSelect:           PropTypes.func,
    selectable:         PropTypes.bool,
    className:          PropTypes.string,
    nodeClassName:      PropTypes.string,
    style:              PropTypes.object,
    title:              PropTypes.node
};


export function transformToTreeNodes(data, nodeClassName, treeData=[], path=[0]) {

    // transform into a tree with children as objects keyed by its key
    if (path.length === 1) set(treeData, '0', {key:'root', title: 'ROOT'});
    Object.entries(data).forEach(([k,v], idx) => {
        const cpath = path.concat(['children', idx]);
        const cnode = {key:`${path.join('-')}-${idx}`, title: k, className: nodeClassName};
        if (isObject(v)) {
            set(treeData, cpath, cnode);
            transformToTreeNodes(v, nodeClassName, treeData, cpath);
        } else {
            set(treeData, cpath, {...cnode, title: `${k}: ${v}` });
        }
    });
    return treeData;
}

