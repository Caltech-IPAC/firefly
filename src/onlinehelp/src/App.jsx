import React, {useState} from 'react';
import {get, pickBy} from 'lodash';

import Tree, { TreeNode } from 'rc-tree';
import 'rc-tree/assets/index.css';

import './App.css';

export function App({tableOfContent, showHidden=false}) {

    const [helpId, setHelpId] = useState(window.location.hash.substr(1));     // using a useState hook
    const [expandedKeys, setExpandAll] = useState();

    const onSelect = (p, e) => {
        const helpId = get(e, 'node.props.value.id');
        setHelpId(helpId);
        const url = window.location.href.replace(window.location.hash, '');
        window.history.pushState(undefined, window.title, `${url}#${helpId}`);
    };

    const expandAll = (flg) => {
        if (flg) {
            const allKeys =  Object.values(treeMap)
                                    .filter((n) => get(n, 'items.length')>0)
                                    .map((n) => n.key);
            setExpandAll(allKeys);
        } else {
            setExpandAll([]);
        }
    };

    const onExpand = (e) => {
        setExpandAll(e);
    };

    const treeRoot = convertToTreeNode('0', tableOfContent, showHidden);
    const treeMap = flattenTree({items: tableOfContent});
    const selNode = treeMap[helpId] || get(tableOfContent, '0');
    const selectedKeys = [selNode.key];
    const defaultExpandedKeys = selNode.parent ? [selNode.parent.key] : undefined;


    return (
        <div className = 'App'>
            <div className='App-header'>
                <div style={{fontSize: 'x-large'}}>
                    {document.title}
                </div>
                <div>
                    <a href='help.pdf' target='help_pdf'>View PDF</a>
                </div>
            </div>

            <div className='App-main'>
                <div className='TOC'>
                    <div className='TOC-toolbar'>
                        <div className='button' onClick={() => expandAll(true)}>Expand All</div>
                        <div className='button' onClick={() => expandAll(false)}>Collapse All</div>
                    </div>
                    <Tree showLine {...pickBy({onSelect, expandedKeys, onExpand, selectedKeys, defaultExpandedKeys, autoExpandParent: true})} >
                        {treeRoot}
                    </Tree>
                </div>
                <div className='TOC-view'>
                    <iframe title='HelpFrame' className='HelpFrame' src={selNode.href}/>
                </div>
            </div>
        </div>
    );
}

function convertToTreeNode(key, node, showHidden) {

    if (Array.isArray(node)) {
        return node.map((n, idx) =>convertToTreeNode(`${key}-${idx}`, n, showHidden));
    }

    const show = !node.hidden || showHidden;
    const items = showHidden ? node.items : node.items && node.items.filter((node) => !node.hidden);
    let {title, hidden, style={}} = node;
    style = hidden ? {color: 'darkgray', ...style} : style;
    node.key = key;
    if (show) {
        if (get(items, 'length') > 0) {
            return (
                <TreeNode {...{key, style, title, value:node}}>
                    {items.map((node, idx) => convertToTreeNode(`${key}-${idx}`, node, showHidden))}
                </TreeNode>
            );
        } else {
            return <TreeNode {...{isLeaf:true, key, style, title, value:node}}/>;
        }
    }
};

function flattenTree(node={}, map={}, showHidden) {

    if (node.id) map[node.id] = node;

    const items = showHidden ? node.items : node.items && node.items.filter((node) => !node.hidden);
    if (get(items, 'length') > 0) {
        items.forEach( (n) => {
            const mnode = {...n, parent: node};
            flattenTree(mnode, map, showHidden);
        });
    }
    return map;
}
