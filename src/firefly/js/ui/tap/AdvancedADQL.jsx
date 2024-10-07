/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, Chip, Divider, FormHelperText, Stack, Switch, Tooltip, Typography} from '@mui/joy';
import React, {useState, useRef, useEffect, useContext, useCallback} from 'react';
import PropTypes from 'prop-types';
import SplitPane from 'react-split-pane';
import Tree from 'rc-tree';
import 'rc-tree/assets/index.css';
import {cloneDeep, defer, isArray, isObject, groupBy, uniqBy} from 'lodash';
import West from '@mui/icons-material/West';
import JoinInnerOutlinedIcon from '@mui/icons-material/JoinInnerOutlined';

import {getSizeAsString, updateSet} from '../../util/WebUtil.js';
import {FieldGroupCtx} from '../FieldGroup.jsx';
import {TextButton} from 'firefly/visualize/ui/Buttons.jsx';
import {InputAreaFieldConnected} from '../InputAreaField.jsx';
import {InputFieldView} from '../InputFieldView.jsx';
import {SplitContent} from '../panel/DockLayoutPanel';
import {useFieldGroupValue} from '../SimpleComponent.jsx';
import {showUploadTableChooser} from '../UploadTableChooser.js';
import {
    loadTapSchemas, loadTapTables, loadTapColumns, getTapServices, maybeQuote, TAP_UPLOAD_SCHEMA,
    ADQL_UPLOAD_TABLE_NAME, defaultADQLExamples, makeUploadSchema, loadTapKeys, searchNodeBy
} from './TapUtil';
import {getColumnIdx} from '../../tables/TableUtil.js';
import {dispatchValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {useColorMode} from 'firefly/ui/FireflyRoot';
import CssStrThemeWrapper from 'firefly/ui/CssStrThemeWrapper';

//--- PrismJs imports ---
import Prism from 'prismjs';
import 'prismjs/components/prism-sql.js';
import 'blissfuljs'; // bliss is needed for prism-live
import '../../externalSource/prismLive/prism-live.js';

// 'primjs/themes/*.css' uses same classnames, so to avoid css overriding, we import light and dark theme css files
// into 2 separate strings using `?raw` resourceQuery (see the corresponding rule defined in webpack.config.js)
import prismLightCss from 'prismjs/themes/prism.css?raw';
import prismDarkCss from 'prismjs/themes/prism-okaidia.css?raw';

// 'prism-live.css' needs to be imported in same way as above otherwise specificity of CSS selectors will change
import prismLiveCss from '../../externalSource/prismLive/prism-live.css?raw';
//--- PrismJs imports end ---


const joinIcon = () =>
    <JoinInnerOutlinedIcon sx={{pr:'2px', transform: 'translate(-4px, 5px) scale(1,.8) rotate(145deg)'}}/>;

const FULLY_QUALIFIED= 'fullyQualified';
const FILTER_STRING= 'filterString';
let cFetchKey = Date.now();
const SB_TIP= 'Clicking on a table or column name below will insert it into the ADQL Query field to the right';


function getExamples(serviceUrl) {
    const configEx= getTapServices().find( ({value}) => value===serviceUrl)?.examples;
    const ex= isArray(configEx) ?
        defaultADQLExamples.map( (e,idx) => (isObject(configEx?.[idx])) ? configEx[idx] : e) : defaultADQLExamples;

    return ex.map( ({description, statement},idx) =>
        (<Stack {...{key:description}}>
            <Typography level='body-sm'>{description}</Typography>
            <pre style={{margin: 0}}>
                <code className='language-sql'>
                    {statement}
                </code>
            </pre>
        </Stack>)
    );
}

export function AdvancedADQL({adqlKey, defAdqlKey, serviceUrl, capabilities, style={}, setError}) {

    const [treeData, setTreeData] = useState([]);                               // using a useState hook
    const [displayedTreeData, setDisplayedTreeData] = useState((treeData));
    const {canUpload=false}= capabilities ?? {};
    const adqlEl = useRef(null);                                                // using a useRef hook
    const prismLiveRef = useRef(null);
    const {groupKey, setVal, getVal} = useContext(FieldGroupCtx);
    const [getUploadSchema, setUploadSchema]= useFieldGroupValue(TAP_UPLOAD_SCHEMA);
    const [getFullQualified, setFullQualified]= useFieldGroupValue(FULLY_QUALIFIED);
    const [getFilterStr, setFilterStr]= useFieldGroupValue(FILTER_STRING);
    const [getUploadFile, setUploadFile]= useFieldGroupValue('uploadFile');
    const uploadSchema= getUploadSchema();
    const fullyQualified= getFullQualified() ?? true;
    const filterStr= getFilterStr() ?? '';

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
        onFilter({target: {value: getFilterStr()??''}});   // when treeData changes, apply filter to the new tree as well
    }, [treeData]);

    useEffect(() => {
        if (!canUpload) setUploadInfo(undefined);
    }, [canUpload]);

    useEffect(() => {
        cFetchKey = Date.now();
        const key = cFetchKey;
        // reload TAP schema when serviceUrl changes
        loadTapSchemas(serviceUrl).then(async (tm) => {
            if (tm.error) {
                setError(`Fail to retrieve schema for: ${serviceUrl}`);
                return;
            }
            if (key === cFetchKey) {
                const tableData = tm?.tableData?.data ?? [];
                const cidx = getColumnIdx(tm, 'schema_name');
                const treeData = await Promise.all( tableData.map(async (row) => {
                    const children = await expandTables(serviceUrl, row[cidx]);
                    return {key: `schema--${key}--${row[cidx]}`, title: row[cidx], children};
                }));
                if (canUpload && uploadSchema) {
                    const upload = {key: `schema--${key}--${TAP_UPLOAD_SCHEMA}`, title: TAP_UPLOAD_SCHEMA};
                    upload.children = await expandTables(serviceUrl, TAP_UPLOAD_SCHEMA, uploadSchema);
                    treeData.unshift(upload);
                }
                setTreeData(treeData);
            }
        });
        window.setTimeout( () => prismLiveRef.current.syncStyles?.(), 10);
    }, [serviceUrl, uploadSchema]);

    useEffect(() => {
        // Insert ADQL function tokens to Prism SQL language
        Prism.languages.insertBefore('sql', 'function', {
            'adql-function': {
                pattern: /\b(POINT|POLYGON|CIRCLE|BOX|DISTANCE|INTERSECTS|CONTAINS)\b/i,
                alias: 'function'
            }
        });

        // highlight help text/code snippets
        Prism.highlightAll();
    },  [serviceUrl]);

    useEffect(() => {
        // We need to get prism-live to adopt to the textarea
        const textArea = adqlEl.current?.querySelector('textarea');
        // adopt textArea
        prismLiveRef.current = new Prism.Live(textArea);
    }, []);

    const onSelect = async (selectedKeys, evt) => {
        const textArea = document.getElementById('adqlEditor');

        const [key=''] = selectedKeys;
        const [type, , tname, cname,upload] = key.split('--');
        const taVal = getUnselectedValue(textArea);
        if (type === 'table') {
            if (taVal) {
                insertAtCursor(textArea, ' ' + tname, adqlKey, groupKey, prismLiveRef.current);
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
            const val = fullyQualified ? `${maybeQuote(tname,true)}.${maybeQuote(cname)}` : maybeQuote(cname);
            insertAtCursor(textArea, ' ' + val, adqlKey, groupKey, prismLiveRef.current);
        } else if (type === 'keys') {
            // these values were set when key nodes were created during #expandColumns
            // {key, title, keyId, fromTable, targetTable, isLeaf: true};
            let {keyId, targetTable} = evt?.node || {};
            const keysInfo = await loadTapKeys(serviceUrl);
            const keys = keysInfo?.tableData?.data?.filter((row) => row[0] === keyId);

            targetTable  = maybeQuote(targetTable, true);
            let val = ` INNER JOIN ${targetTable} ON `;
            keys.forEach((row, idx) => {
                const fromColumn   = maybeQuote(fixCname(row[4], row[1]));
                const targetColumn = maybeQuote(fixCname(row[5], row[2]));
                val += `${idx>0 ? ' AND' : ''} ${fromColumn} = ${targetColumn}`;
            });
            insertAtCursor(textArea, val, adqlKey, groupKey, prismLiveRef.current);
        }
    };

    const onLoadData = (treeNode) => {
        return new Promise((resolve) => {
            const {key:eventKey, schema, title, isLoaded} = treeNode;
            if (schema && !isLoaded) {
                // it has schema info.. must be a table node.
                expandColumns(serviceUrl, title, schema, uploadSchema, treeData, eventKey, setTreeData)
                    .then(() => resolve());
            } else {
                resolve();
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

    const onFilter = useCallback((e) => {
        setFilterStr(e.target?.value);
        if (!e.target?.value) setTreeData(treeData);

        const filterNodes = (result, node) => {
            if (node?.title?.toLowerCase?.().includes(e.target?.value?.toLowerCase())) {
                result.push(node);
                return result;
            }
            if (node.children) {
                const children = node.children.reduce(filterNodes, []);
                if (children.length) result.push({ ...node, children });
            }
            return result;
        };

        const ntreedata = treeData.reduce(filterNodes, []);
        setDisplayedTreeData(ntreedata);
    }, [treeData, setTreeData]);

    const fileName= getUploadFile();
    const haveTable= Boolean(fileName && uploadSchema);
    const {serverFile,totalRows,fileSize}= uploadSchema?.[fileName] ?? {};

    // based on active mode, set BG color needed for the parent container of prism-live textarea
    // (hardcoding because prism css files don't have bg color defined as reusable variables yet)
    const {isDarkMode} = useColorMode();
    const prismCssBgColor = isDarkMode
        ? '#272822' //bgColor on L41 in prismDarkCss
        : '#f5f2f0'; //bgColor on L59 in prismLightCss

    return (
            <SplitPane split='vertical' defaultSize={275} style={{position: 'relative', ...style}}>
                <SplitContent style={{display:'flex', flexDirection:'column'}}>
                    <Tooltip title={SB_TIP}>
                        <Stack>
                            <Stack>
                                <div>
                                    <Typography level='title-lg' sx={{whiteSpace:'nowrap'}}>Schema Browser</Typography>
                                </div>
                                <InputFieldView
                                    value={filterStr}
                                    sx={{width:.9}}
                                    placeholder='Enter Filter'
                                    onChange={onFilter} size={15}
                                    title='Enter partial string to filter the visible nodes on the Schema Browser.  Leave blank to display all.'
                                />
                            </Stack>
                            <Typography level='body-xs'>Schema->Table->Column</Typography>
                        </Stack>
                    </Tooltip>
                    <Divider orientation='horizontal'/>
                    <div  style={{overflow: 'auto', flexGrow: 1}}>
                        <Tree treeData={displayedTreeData} defaultExpandAll={true} showLine={true} selectedKeys={[]} loadData={onLoadData} onSelect={onSelect} />
                    </div>
                </SplitContent>
                <SplitContent style={{overflow: 'auto'}}>
                    <CssStrThemeWrapper cssStr={{light: prismLightCss, dark: prismDarkCss}}>
                        <Stack flexGrow={1} ml={1} spacing={1}>
                            <Stack {...{spacing:4}}>
                                <Stack {...{spacing:1}}>
                                    <Stack {...{direction: 'row', spacing:10, mr: 3, justifyContent: 'flex-start', alignItems: 'center'}}>
                                        <Typography level='title-lg'>ADQL Query</Typography>
                                        <Stack {...{direction: 'row'}}>
                                            <Chip size='md' title='Reset to the initial query' style={{height: 24, marginRight: 5}} onClick={onReset}>Reset</Chip>
                                            <Chip size='md' title='Clear the query' style={{height: 24}} onClick={onClear}>Clear</Chip>
                                        </Stack>
                                    </Stack>
                                    <Tooltip placement='bottom'
                                             title={
                                                 <Stack>
                                                     <Typography>ADQL to submit to the selected TAP service </Typography>
                                                     <Typography>Type ADQL text</Typography>
                                                     <Typography sx={{pl:2}}>or</Typography>
                                                     <Stack direction='row' spacing={1} alignItems='center'>
                                                         <West/>
                                                         <Typography color='warning'>
                                                             Use the Schema Browser to insert table and column names.
                                                         </Typography>
                                                     </Stack>
                                                 </Stack>
                                             }>
                                        <Stack>
                                            <CssStrThemeWrapper cssStr={
                                                // nest the styles in InputAreaFieldView class to make them more specific than prismCss for overriding
                                                `.InputAreaFieldView {${prismLiveCss}}`
                                            }>
                                                <InputAreaFieldConnected
                                                    ref={adqlEl}
                                                    fieldKey={adqlKey}
                                                    slotProps={{
                                                        input: {sx: {bgcolor: prismCssBgColor}},
                                                        textArea: {className: 'prism-live language-sql', id: 'adqlEditor'}
                                                    }}
                                                />
                                            </CssStrThemeWrapper>
                                            <Typography level='body-sm' sx={{mt:-2}}>Type ADQL text; you can use the Schema Browser on the left to insert table and column names.</Typography>
                                        </Stack>
                                    </Tooltip>
                                    <Stack spacing={1}>
                                        <Switch {...{ size:'md', sx:{alignSelf:'flex-start'},
                                            endDecorator: (
                                                <Stack>
                                                    <Typography>
                                                        Insert fully-qualified column names (recommended for table joins)
                                                    </Typography>
                                                    <FormHelperText>
                                                        When selecting a column from the Schema browser use the full qualified name
                                                    </FormHelperText>
                                                </Stack>
                                            ),
                                            checked:fullyQualified,
                                            onChange: () => {
                                                setFullQualified(!getFullQualified());
                                            },
                                        }} />
                                        {canUpload &&
                                            <Stack sx={{mt:2}}>
                                                <Stack {...{direction:'row', alignItems:'center', spacing:1}}>
                                                    <Stack {...{direction:'row', alignItems:'center', spacing:1}}>
                                                        <TextButton text={serverFile ? 'Change Upload Table...' : 'Add Upload Table...'}
                                                                     onClick={() => showUploadTableChooser(setUploadInfo)} style={{marginLeft: 10}} />
                                                        {haveTable &&
                                                            <Typography level='title-lg'
                                                                        sx={{w:200, overflow:'hidden', whiteSpace:'nowrap',
                                                                            textOverflow:'ellipsis'}}>
                                                                {fileName}
                                                            </Typography>
                                                        }
                                                        {serverFile && <Chip onClick={() => setUploadInfo(undefined)}>Clear</Chip>}
                                                    </Stack>
                                                </Stack>
                                                {haveTable &&
                                                    <Stack {...{direction:'row'}}>
                                                        <Typography sx={{whiteSpace:'nowrap'}}>
                                                            {`Rows: ${totalRows},`}
                                                        </Typography>
                                                        <Typography sx={{pl: 1, whiteSpace:'nowrap'}}>
                                                            {`Size: ${getSizeAsString(fileSize)}`}
                                                        </Typography>
                                                    </Stack>}
                                            </Stack>}

                                </Stack>
                            </Stack>
                                <Stack>
                                    <Typography level='body-lg'>Popular Functions</Typography>
                                    <Box sx={{ml: 2}}>
                                        <pre>
                                            <code className='language-sql'>
                                                {(`\
                                            TOP n  -- Limit the results to n number of records
                                            ORDER BY column [ASC/DESC] -- Used for sorting
                                            POINT('<coordinate system>', RIGHT_ASCENSION, DECLINATION)
                                            CIRCLE('<coordinate system>', RIGHT_ASCENSION_CENTER, DECLINATION_CENTER, RADIUS)
                                            BOX('<coordinate system>', RIGHT_ASCENSION_CENTER, DECLINATION_CENTER, WIDTH, HEIGHT)
                                            POLYGON('<coordinate system>', POINT1, POINT2, POINT3...)
                                            DISTANCE(POINT1, POINT2)
                                            CONTAINS(REGION1, REGION2)
                                            INTERSECTS(REGION1, REGION2)`).replace(/    +/g, '')
                                                }
                                            </code>
                                        </pre>
                                    </Box>
                                </Stack>

                                <Stack>
                                    <Typography level='body-lg'>Sample Queries</Typography>
                                    <Stack {...{ml: 2, spacing:2}}>
                                        { getExamples(serviceUrl) }
                                        <Typography level='body-sm'>
                                            These examples may not be directly usable in any TAP service you have selected.
                                        </Typography>
                                    </Stack>
                                </Stack>
                            </Stack>
                        </Stack>
                    </CssStrThemeWrapper>
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
    capabilities:   PropTypes.object,
    style:          PropTypes.object
};

function fixCname(cname, tblname) {
    if (!cname || !tblname) return cname;
    return cname?.startsWith(tblname) ? cname : tblname + '.' + cname;
}

function expandTables(serviceUrl, title, uploadSchema) {
    const key = cFetchKey;
    const makeTabKey= (key,tname) => `table--${key}--${tname}`;

    if (uploadSchema) {
        const tList= Object.entries(uploadSchema)
            .map(([file,info]) => ({key: makeTabKey(key,info.table)+'----upload', title:`${info.table} (${file})`, schema:TAP_UPLOAD_SCHEMA}));
        return Promise.resolve(tList);
    }
    return loadTapTables(serviceUrl, title).then( (tm) => {
        if (cFetchKey === key) {
            const tableData = tm?.tableData?.data ?? [];
            const cidx = getColumnIdx(tm, 'table_name');
            const tables = tableData.map( (row) => ({key: makeTabKey(key,row[cidx])+'--upload', title: row[cidx], schema: title}) );
            return tables;
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

    return loadTapColumns(serviceUrl, schema, title).then( async (tm) => {
        if (cFetchKey === key) {
            const tableData = tm?.tableData?.data ?? [];
            const nidx = getColumnIdx(tm, 'column_name');
            const didx = getColumnIdx(tm, 'description');
            const cols = tableData.map( (row) => {
                const colkey = makeColKey(key,tname,row[nidx]);
                const title = row[nidx] + (row[didx] ? ` (${row[didx]})` : '');
                return {key: colkey, title, isLeaf: true};
            });

            await addAvailKeyNodes({serviceUrl, title, cols, treeData, setTreeData});

            addChildNodes(treeData, eventKey, cols);
            setTreeData(cloneDeep(treeData));
        }
    });
}

// add Available Keys node if they exists
async function addAvailKeyNodes({serviceUrl, title, cols, treeData, setTreeData}) {
    const keysInfo = await loadTapKeys(serviceUrl);
    const availKeys = keysInfo?.tableData?.data?.filter((row) => row[1] === title || row[2] === title);
    if (!availKeys?.length) return;

    const joins = {key: `JoinKeys--${cFetchKey}--${title}`, title: 'JOINs available', children: [], isLoaded: true};
    cols.unshift(joins);

    const byTable = groupBy(availKeys, (row) => title === row[1] ? row[2] : row[1]);        // group by table
    Object.entries(byTable).forEach(([, keysByTbl]) => {
        keysByTbl = uniqBy(keysByTbl, (row) => row[0]);         // collapsed by keyId so each join entry show up only once
        const keys = keysByTbl.map((row) => {
            const reversed = title !== row[1];      // true if mapping is reversed
            const fromTable = reversed ? row[2] : row[1];
            const targetTable = reversed ? row[1] : row[2];
            const keyId = row[0];
            const desc = row[3] ? ` (${row[3]}) ` : '';
            const key = `keys--${cFetchKey}--${title}--${row[0]}`;
            return {key, title:<JoinNode {...{targetTable, keyId, desc, treeData, setTreeData}}/>, keyId, fromTable, targetTable, icon: joinIcon, isLeaf: true};
        });
        // keys.unshift({key: `JumpToTable--${cFetchKey}--${title}--${relTbl}`, title: '(jump to table)', isLeaf: true}); // not implemented yet.
        if (keys?.length > 0) joins.children.push(...keys);
    });
}

function JoinNode({targetTable, keyId, desc, treeData, setTreeData}) {

    const jumpTo = useCallback(() => {
        const nodeKey = searchNodeBy(treeData, (n) => n.title === targetTable);
        if (nodeKey) {
            const nTree = updateSet(treeData, 'expandedKeys', treeData.expandedKeys.push(nodeKey));
            setTreeData(nTree);
        }
    });
    // <img src={INFO_ICO} onClick={jumpTo} style={{height:16, verticalAlign:'middle'}}/>  // can't get it to work.
    return <div>{targetTable} [{keyId}]{desc}</div>;
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
