/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {set, get, isObject, isString} from 'lodash';

import Tree from 'rc-tree';
import 'rc-tree/assets/index.css';
import {Sheet, Stack, Typography} from '@mui/joy';


/**
 * This component renders a TableModel as a tree.  It will use the `groupBy` prop
 * to transform the table into a tree.
 * Based on rc-tree:  http://react-component.github.io/tree/
 *
 */
export const ObjectTree = React.memo(({data, defaultExpandAll=true, selectable=false, onSelect, nodeClassName, sx, title}) => {
    const treeData = transformToTreeNodes(data, nodeClassName);
    return (
        <Sheet component={Stack} spacing={.5} variant='outlined' sx={{fontSize: 'sm', p: 1, ...sx}}>
            {isString(title) ? <Typography level='title-md'>{title}</Typography> : (title || false)}
            <Tree {...{defaultExpandAll,onSelect,selectable }} treeData={get(treeData, [0, 'children'])}/>
        </Sheet>
    );
});

ObjectTree.propTypes = {
    data:               PropTypes.object.isRequired,
    defaultExpandAll:   PropTypes.bool,
    onSelect:           PropTypes.func,
    selectable:         PropTypes.bool,
    nodeClassName:      PropTypes.string,
    sx:                 PropTypes.object,
    title:              PropTypes.oneOfType([PropTypes.element, PropTypes.string])
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

