/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useState, useRef, useEffect} from 'react';
import ReactDOM from 'react-dom';
import PropTypes from 'prop-types';
import SplitPane from 'react-split-pane';
import Tree, { TreeNode } from 'rc-tree';
import 'rc-tree/assets/index.css';
import {get, cloneDeep, defer} from 'lodash';

import {InputAreaFieldConnected} from '../InputAreaField.jsx';
import {SplitContent} from '../panel/DockLayoutPanel';
import {loadTapSchemas, loadTapTables, loadTapColumns} from './TapUtil';
import {getColumnIdx} from '../../tables/TableUtil.js';
import {dispatchValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {getFieldVal} from '../../fieldGroup/FieldGroupUtils.js';

import Prism from 'prismjs';
// bliss is needed for prism-live
import 'blissfuljs';
import '../../externalSource/prismLive/prism-sql.js';
import '../../externalSource/prismLive/prism-live.js';

import '../../externalSource/prismLive/prism.css';
import '../../externalSource/prismLive/prism-live.css';

const code = {className: 'language-sql'};
var cFetchKey = Date.now();

export function AdvancedADQL({adqlKey, defAdqlKey, groupKey, serviceUrl, style={}}) {

    const [treeData, setTreeData] = useState([]);                               // using a useState hook
    const adqlEl = useRef(null);                                                // using a useRef hook
    const ffcn = useRef(null);                                                  // using a useRef hook
    const prismLiveRef = useRef(null);

    useEffect(() => {
        cFetchKey = Date.now();
        const key = cFetchKey;
        // reload TAP schema when serviceUrl changes
        loadTapSchemas(serviceUrl).then((tm) => {
            if (key === cFetchKey) {
                const tableData = get(tm, 'tableData.data', []);
                const cidx = getColumnIdx(tm, 'schema_name');
                const treeData = tableData.map( (row) => ({key: `schema--${key}--${row[cidx]}`, title: row[cidx]}) );
                setTreeData(treeData);
            }
        });
        window.setTimeout( () => prismLiveRef.current.syncStyles?.(), 10);
    }, [serviceUrl]);

    useEffect(() => {
        // highlight help text/code snippets
        Prism.highlightAll();
        // We need to get prism-live to adopt to the textarea
        const textArea = ReactDOM.findDOMNode(adqlEl.current)?.firstChild;
        // adopt textArea
        prismLiveRef.current = new Prism.Live(textArea);
    }, []);

    const onSelect = (p) => {
        const textArea = document.getElementById('adqlEditor');

        const [key=''] = p;
        const [type, , tname, cname] = key.split('--');
        const taVal = getUnselectedValue(textArea);
        if (type === 'table') {
            if (taVal) {
                insertAtCursor(textArea, tname, adqlKey, groupKey, prismLiveRef.current);
            } else {
                dispatchValueChange({fieldKey: adqlKey, groupKey, value: `SELECT TOP 1000 * FROM ${tname}`, valid: true});
                window.setTimeout( () => prismLiveRef.current.syncStyles?.(), 10);
            }
        } else if (type === 'column') {
            const val = ffcn.current.checked ? `${tname}.${cname}` : cname;
            insertAtCursor(textArea, val, adqlKey, groupKey, prismLiveRef.current);
        }
    };

    const onLoadData = (treeNode) => {
        return new Promise((resolve) => {
            const {eventKey, schema, title} = treeNode.props;
            if (schema) {
                // it has schema info.. must be a table node.
                expandColumns(serviceUrl, title, schema, treeData, eventKey, setTreeData)
                    .then(() => resolve());
            } else {
                expandTables(serviceUrl, title, treeData, eventKey, setTreeData)
                    .then(() => resolve());
            }
        });
    };

    const onClear = () => {
        dispatchValueChange({fieldKey:adqlKey, groupKey, value: '', valid: true});
        // trigger prismLive style sync
        window.setTimeout( () => prismLiveRef.current.syncStyles?.(), 10);
    };

    const onReset = () => {
        const value = getFieldVal(groupKey, defAdqlKey, '');
        dispatchValueChange({fieldKey:adqlKey, groupKey, value, valid: true});
        // trigger prismLive style sync
        window.setTimeout( () => prismLiveRef.current.syncStyles?.(), 10);
    };

    const treeNodes = convertToTreeNode(treeData);


    return (
            <SplitPane split='vertical' defaultSize={200} style={{position: 'relative', ...style}}>
                <SplitContent style={{display: 'flex', flexDirection: 'column'}}>
                    <div style={{fontWeight: 'bold', paddingBottom:5, textAlign:'center'}}>Schema Browser</div>
                    <div style={{textAlign:'center', paddingBottom:2, }}>Schema -> Table -> Column</div>
                    <div  style={{overflow: 'auto', flexGrow: 1}}>
                        <Tree defaultExpandAll showLine selectedKeys={[]} loadData={onLoadData} onSelect={onSelect}>
                            {treeNodes}
                        </Tree>
                    </div>
                </SplitContent>
                <SplitContent style={{overflow: 'auto'}}>
                    <div className='flex-full'>
                        <div style={{display: 'inline-flex', marginRight: 25, justifyContent: 'space-between', alignItems: 'center'}}>
                            <h3>ADQL Query:</h3>
                            <div style={{display: 'inline-flux'}}>
                                <button className='button std' title='Reset to the initial query' style={{height: 24, marginRight: 5}} onClick={onReset}>Reset</button>
                                <button className='button std' title='Clear the query' style={{height: 24}} onClick={onClear}>Clear</button>
                            </div>
                        </div>
                        <InputAreaFieldConnected
                            ref={adqlEl}
                            style={{backgroundColor: 'transparent'}}
                            fieldKey={adqlKey}
                            tooltip='ADQL to submit to the selected TAP service'
                            additionalClasses='prism-live language-sql'
                            idName='adqlEditor'
                        />
                        <div style={{color: '#4c4c4c'}}>
                            <h3>Schema Browser Usage</h3>
                            <div style={{marginLeft: 5}}>
                                <div>Click on a Table node to insert a default SELECT statement of that table into the Query input box.</div>
                                <div>Click on a Column node to insert the column's name at the Query input box's cursor.</div>
                                <label style={{display: 'flex', alignItems: 'center', marginLeft: 20}}> <input type='checkbox' ref={ffcn} defaultChecked={true} />Insert fully-qualified column names</label>
                            </div>
                            <h3>Popular Functions</h3>
                            <div style={{marginLeft: 5}}>
                                <pre><code {...code}>{(`\
                                    TOP n  -- Limit the results to n number of records
                                    ORDER BY [ASC/DESC] -- Used for sorting
                                    POINT('<coordinate system>', RIGHT_ASCENSION, DECLINATION)
                                    CIRCLE('<coordinate system>', RIGHT_ASCENSION_CENTER, DECLINATION_CENTER, RADIUS)
                                    BOX('<coordinate system>', RIGHT_ASCENSION_CENTER, DECLINATION_CENTER, WIDTH, HEIGHT)
                                    POLYGON('<coordinate system>', POINT1, POINT2, POINT3...)
                                    DISTANCE(POINT1, POINT2)
                                    CONTAINS(REGION1, REGION2)
                                    INTERSECTS(REGION1, REGION2)`).replace(/    +/g, '')
                                    }</code></pre>
                            </div>

                            <h3>Sample Queries</h3>
                            <div style={{marginLeft: 5}}>
                                <div          >{'A 1 degree cone search around M101 would be:'}</div>
                                <code {...code}>{"SELECT * FROM fp_psc WHERE CONTAINS(POINT('J2000', ra, dec), CIRCLE('J2000', 210.80225, 54.34894, 1.0))=1"}</code>
                                <br/>
                                <div          >{'A 1 degree by 1 degree box around M101 would be:'}</div>
                                <code {...code}>{"SELECT * FROM fp_psc WHERE CONTAINS(POINT('J2000', ra, dec), BOX('J2000', 210.80225, 54.34894, 1.0, 1.0))=1"}</code>
                                <br/>
                                <div          >{'A triangle search around M101 would be:'}</div>
                                <code {...code}>{`SELECT * FROM fp_psc WHERE CONTAINS(POINT('J2000', ra, dec),
                  POLYGON('J2000', 209.80225, 53.34894, 209.80225, 55.34894, 211.80225, 54.34894))=1`}</code>
                            </div>
                        </div>
                    </div>
                </SplitContent>
            </SplitPane>
    );
}

AdvancedADQL.propTypes= {
    groupKey:       PropTypes.string,
    adqlKey:        PropTypes.string,
    tblNameKey:     PropTypes.string,
    defAdqlKey:     PropTypes.string,      // used for reset
    serviceUrl:     PropTypes.string,
    style:          PropTypes.object
};


function expandTables(serviceUrl, title, treeData, eventKey, setTreeData) {
    const key = cFetchKey;
    return loadTapTables(serviceUrl, title).then( (tm) => {
        if (cFetchKey === key) {
            const tableData = get(tm, 'tableData.data', []);
            const cidx = getColumnIdx(tm, 'table_name');
            const tables = tableData.map( (row) => ({key: `table--${key}--${row[cidx]}`, title: row[cidx], schema: title}) );
            addChildNodes(treeData, eventKey, tables);
            setTreeData(cloneDeep(treeData));
        }
    });
}

function expandColumns(serviceUrl, title, schema, treeData, eventKey, setTreeData) {
    const tname = title;
    const key = cFetchKey;
    return loadTapColumns(serviceUrl, schema, title).then( (tm) => {
        if (cFetchKey === key) {
            const tableData = get(tm, 'tableData.data', []);
            const nidx = getColumnIdx(tm, 'column_name');
            const didx = getColumnIdx(tm, 'description');
            const cols = tableData.map( (row) => {
                const colkey = `column--${key}--${tname}--${row[nidx]}`;
                const title = row[nidx] + (row[didx] ? ` (${row[didx]})` : '');
                return {key: colkey, title, isLeaf: true};
            });
            addChildNodes(treeData, eventKey, cols);
            setTreeData(cloneDeep(treeData));
        }
    });
}

function addChildNodes(data, key, children) {
    for (let idx = 0; idx < data.length; idx++) {
        const item = data[idx];
        if (item.key === key) {
            item.children = children;
            return;
        }
        if (item.children) {
            addChildNodes(item.children, key, children);
        }
    }
}

function convertToTreeNode(data) {
    return data.map((item) => {
        if (item.children) {
            return <TreeNode {...item}>{convertToTreeNode(item.children)}</TreeNode>;
        }
        return <TreeNode {...item} isLeaf={item.isLeaf}/>;
    });
}

export function getUnselectedValue (input) {
    let val = input.value || '';
    const start = input.selectionStart;
    const end = input.selectionEnd;
    val = val.slice(0, start) + val.slice(end);
    return val.trim();
}

export function insertAtCursor (input, textToInsert, fieldKey, groupKey, prismLive) {
    const ovalue = input.value;

    // save selection start and end position
    const start = input.selectionStart;
    const end = input.selectionEnd;

    // update the value with our text inserted
    const value = ovalue.slice(0, start) + textToInsert + ovalue.slice(end);
    dispatchValueChange({fieldKey, groupKey, value, valid: true});

    // update cursor to be at the end of insertion.. need to defer because react lifecycle.
    defer( () => {
        input.selectionStart = input.selectionEnd = start + textToInsert.length;
        input.focus();
    });

    // trigger prismLive style sync
    window.setTimeout( () => prismLive.syncStyles(), 10);
}
