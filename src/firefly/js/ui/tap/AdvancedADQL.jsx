/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useState, useRef, useEffect, Fragment, useContext} from 'react';
import ReactDOM from 'react-dom';
import PropTypes from 'prop-types';
import SplitPane from 'react-split-pane';
import Tree, { TreeNode } from 'rc-tree';
import 'rc-tree/assets/index.css';
import {cloneDeep, defer, isArray, isObject} from 'lodash';
import {getSizeAsString} from '../../util/WebUtil.js';
import {FieldGroupCtx} from '../FieldGroup.jsx';
import {ExtraButton} from '../FormPanel.jsx';

import {InputAreaFieldConnected} from '../InputAreaField.jsx';
import {SplitContent} from '../panel/DockLayoutPanel';
import {useFieldGroupValue} from '../SimpleComponent.jsx';
import {showUploadTableChooser} from '../UploadTableChooser.js';
import {
    loadTapSchemas, loadTapTables, loadTapColumns, getTapServices, maybeQuote, TAP_UPLOAD_SCHEMA,
    ADQL_UPLOAD_TABLE_NAME, defaultADQLExamples, makeUploadSchema
} from './TapUtil';
import {getColumnIdx} from '../../tables/TableUtil.js';
import {dispatchValueChange} from '../../fieldGroup/FieldGroupCntlr.js';

import Prism from 'prismjs';
// bliss is needed for prism-live
import 'blissfuljs';
import '../../externalSource/prismLive/prism-sql.js';
import '../../externalSource/prismLive/prism-live.js';

import '../../externalSource/prismLive/prism.css';
import '../../externalSource/prismLive/prism-live.css';

const code = {className: 'language-sql'};
let cFetchKey = Date.now();
const SB_TIP= 'Clicking on a table or column name below will insert it into the ADQL Query field to the right';


function getExamples(serviceUrl) {
    const configEx= getTapServices().find( ({value}) => value===serviceUrl)?.examples;
    const ex= isArray(configEx) ?
        defaultADQLExamples.map( (e,idx) => (isObject(configEx?.[idx])) ? configEx[idx] : e) : defaultADQLExamples;

    return ex.map( ({description, statement},idx) =>
        (<Fragment key={description}>
            <div style={{paddingTop:idx===0?0:7, paddingBottom:3}}>{description}</div>
            <code className='language-sql' style={{  display: 'block', whiteSpace: 'pre-wrap', marginLeft:5, paddingLeft:3 }}>
                {statement}
            </code>
        </Fragment>)
    );
}

export function AdvancedADQL({adqlKey, defAdqlKey, serviceUrl, capabilities, style={}}) {

    const [treeData, setTreeData] = useState([]);                               // using a useState hook
    const {canUpload=false}= capabilities ?? {};
    const adqlEl = useRef(null);                                                // using a useRef hook
    const ffcn = useRef(null);                                                  // using a useRef hook
    const prismLiveRef = useRef(null);
    const {groupKey, setVal, getVal} = useContext(FieldGroupCtx);
    const [getUploadSchema, setUploadSchema]= useFieldGroupValue(TAP_UPLOAD_SCHEMA);
    const [getUploadFile, setUploadFile]= useFieldGroupValue('uploadFile');
    const uploadSchema= getUploadSchema();

    const setUploadInfo = (uploadInfo) => {
        if (uploadInfo) {
            const {serverFile, fileName, columns, totalRows, fileSize, }= uploadInfo;
            setUploadSchema(makeUploadSchema(fileName,serverFile,columns,totalRows,fileSize,ADQL_UPLOAD_TABLE_NAME,''));
            setUploadFile(fileName);
        }
        else {
            setUploadSchema(false);
            setUploadFile('');
        }
    };

    useEffect(() => {
        if (!canUpload) setUploadInfo(undefined);
    }, [canUpload]);

    useEffect(() => {
        cFetchKey = Date.now();
        const key = cFetchKey;
        // reload TAP schema when serviceUrl changes
        loadTapSchemas(serviceUrl).then((tm) => {
            if (key === cFetchKey) {
                const tableData = tm?.tableData?.data ?? [];
                const cidx = getColumnIdx(tm, 'schema_name');
                const baseTD = tableData.map( (row) => ({key: `schema--${key}--${row[cidx]}`, title: row[cidx]}) );
                const treeData= (canUpload && uploadSchema) ?
                    [{key: `schema--${key}--${TAP_UPLOAD_SCHEMA}`, title: TAP_UPLOAD_SCHEMA}, ...baseTD] : baseTD;
                setTreeData(treeData);
            }
        });
        window.setTimeout( () => prismLiveRef.current.syncStyles?.(), 10);
    }, [serviceUrl, uploadSchema]);

    useEffect(() => {
        // highlight help text/code snippets
        Prism.highlightAll();
    },  [serviceUrl]);

    useEffect(() => {
        // We need to get prism-live to adopt to the textarea
        const textArea = ReactDOM.findDOMNode(adqlEl.current)?.firstChild;
        // adopt textArea
        prismLiveRef.current = new Prism.Live(textArea);
    }, []);

    const onSelect = (p) => {
        const textArea = document.getElementById('adqlEditor');

        const [key=''] = p;
        const [type, , tname, cname,upload] = key.split('--');
        const taVal = getUnselectedValue(textArea);
        if (type === 'table') {
            if (taVal) {
                insertAtCursor(textArea, tname, adqlKey, groupKey, prismLiveRef.current);
            } else {
                let insertTname= tname;
                if (upload) {
                    insertTname= TAP_UPLOAD_SCHEMA+'.'+tname;
                    const asTable= Object.values(uploadSchema).find( (o) => o.table===tname)?.asTable;
                    if (asTable) insertTname+= ' AS '+asTable;
                }
                setVal(adqlKey, `SELECT TOP 1000 * FROM ${maybeQuote(insertTname,true)}`);
                window.setTimeout( () => prismLiveRef.current.syncStyles?.(), 10);
            }
        } else if (type === 'column') {
            const val = ffcn.current.checked ? `${maybeQuote(tname,true)}.${maybeQuote(cname)}` : maybeQuote(cname);
            insertAtCursor(textArea, val, adqlKey, groupKey, prismLiveRef.current);
        }
    };

    const onLoadData = (treeNode) => {
        return new Promise((resolve) => {
            const {eventKey, schema, title} = treeNode.props;
            if (schema) {
                // it has schema info.. must be a table node.
                expandColumns(serviceUrl, title, schema, uploadSchema, treeData, eventKey, setTreeData)
                    .then(() => resolve());
            } else {
                expandTables(serviceUrl, title, uploadSchema, treeData, eventKey, setTreeData)
                    .then(() => resolve());
            }
        });
    };

    const onClear = () => {
        setVal(adqlKey, '');
        // trigger prismLive style sync
        window.setTimeout( () => prismLiveRef.current.syncStyles?.(), 10);
    };

    const onReset = () => {
        const value = getVal(defAdqlKey) ?? '';
        setVal(adqlKey, value);
        // trigger prismLive style sync
        window.setTimeout( () => prismLiveRef.current.syncStyles?.(), 10);
    };

    const fileName= getUploadFile();
    const treeNodes = convertToTreeNode(treeData);
    const haveTable= Boolean(fileName && uploadSchema);
    const {serverFile,totalRows,fileSize}= uploadSchema?.[fileName] ?? {};


    return (
            <SplitPane split='vertical' defaultSize={200} style={{position: 'relative', ...style}}>
                <SplitContent style={{display: 'flex', flexDirection: 'column'}}>
                    <div style={{fontWeight: 'bold', paddingBottom:5, textAlign:'center'}} title={SB_TIP}>Schema Browser</div>
                    <div style={{textAlign:'center', paddingBottom:2, }} title={SB_TIP}>Schema -> Table -> Column</div>
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
                            <div style={{marginLeft: 5, lineHeight:'2em'}}>
                                <div>Type ADQL text; you can use the Schema Browser on the left to insert table and column names.</div>
                                <label style={{display: 'flex', alignItems: 'center', marginLeft: 20}}> <input type='checkbox' ref={ffcn} defaultChecked={true} />Insert fully-qualified column names (recommended for table joins)</label>
                            </div>
                            {canUpload && <div style={{margin: '15px 0 0 5px'}}>
                                <div style={{display:'flex', alignItems:'center'}}>
                                    <div style={{whiteSpace:'nowrap'}}>Upload Table:</div>
                                    <div style={{display:'flex', alignItems:'center'}}>
                                        <ExtraButton text={serverFile ? 'Change...' : 'Add...'}
                                                     onClick={() => showUploadTableChooser(setUploadInfo)} style={{marginLeft: 10}} />
                                        {serverFile && <ExtraButton text='Clear' onClick={() => setUploadInfo(undefined)} />}
                                        {haveTable &&
                                            <div style={{width:200, overflow:'hidden', whiteSpace:'nowrap',fontSize:'larger',
                                                textOverflow:'ellipsis', lineHeight:'2em', paddingLeft:20}}>
                                                {`${fileName}`}
                                            </div>
                                        }
                                    </div>
                                </div>
                            </div>}
                            {haveTable &&
                                <div style={{display:'flex', flexDirection:'row', margin: '0 0 15px 217px', justifyContent:'flex-start'}}>
                                    <div style={{whiteSpace:'nowrap'}}>
                                        <span>Rows: </span>
                                        <span>{totalRows},</span>
                                    </div>
                                    <div style={{paddingLeft: 8, whiteSpace:'nowrap'}}>
                                        <span>Size: </span>
                                        <span>{getSizeAsString(fileSize)}</span>
                                    </div>
                                </div>
                            }
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
                            <div style={{marginLeft: 5, lineHeight: '1.4em'}}>
                                { getExamples(serviceUrl) }
                            </div>
                            <div style={{margin:'7px 0 0 2px'}}>
                                These examples may not be directly usable in any TAP service you have selected.
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
    capabilities:   PropTypes.string,
    style:          PropTypes.object
};


function expandTables(serviceUrl, title, uploadSchema, treeData, eventKey, setTreeData) {
    const key = cFetchKey;

    const makeTabKey= (key,tname) => `table--${key}--${tname}`;

    if (eventKey.endsWith(TAP_UPLOAD_SCHEMA)) {

        const tList= Object.entries(uploadSchema)
            .map(([file,info]) => ({key: makeTabKey(key,info.table)+'----upload', title:`${info.table} (${file})`, schema:TAP_UPLOAD_SCHEMA}));
        addChildNodes(treeData, eventKey, tList);
        setTreeData(cloneDeep(treeData));
        return Promise.resolve();
    }
    return loadTapTables(serviceUrl, title).then( (tm) => {
        if (cFetchKey === key) {
            const tableData = tm?.tableData?.data ?? [];
            const cidx = getColumnIdx(tm, 'table_name');
            const tables = tableData.map( (row) => ({key: makeTabKey(key,row[cidx])+'--upload', title: row[cidx], schema: title}) );
            addChildNodes(treeData, eventKey, tables);
            setTreeData(cloneDeep(treeData));
        }
    });
}

function expandColumns(serviceUrl, title, schema, uploadSchema, treeData, eventKey, setTreeData) {
    const tname = title;
    const key = cFetchKey;

    const makeColKey= (key,tname,cname) => `column--${key}--${tname}--${cname}`;
    

    if (uploadSchema && schema===TAP_UPLOAD_SCHEMA) {
        const startT= title?.match(/\(.*\)/)?.[0];
        const tableKey= startT?.substring(1,startT.length-1);
        const schemaEntry=uploadSchema[tableKey];
        if (!schemaEntry) return Promise.resolve();
        const cols= schemaEntry.columns
            .map( ({name}) => ( {key:makeColKey(key,schemaEntry.asTable?? schemaEntry.table,name),c: name, title:name, isLeaf:true}));
        addChildNodes(treeData, eventKey, cols);
        setTreeData(cloneDeep(treeData));
        return Promise.resolve();
    }
    return loadTapColumns(serviceUrl, schema, title).then( (tm) => {
        if (cFetchKey === key) {
            const tableData = tm?.tableData?.data ?? [];
            const nidx = getColumnIdx(tm, 'column_name');
            const didx = getColumnIdx(tm, 'description');
            const cols = tableData.map( (row) => {
                const colkey = makeColKey(key,tname,row[nidx]);
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
    prismLive && window.setTimeout( () => prismLive.syncStyles(), 10);
}
