/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
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


export class AdvancedADQL extends PureComponent {

    constructor(props) {
        super(props);
        this.state = {
            treeData: []
        };

        this.onLoadData = this.onLoadData.bind(this);
        this.onSelect = this.onSelect.bind(this);
        this.reloadSchemas = this.reloadSchemas.bind(this);
        this.onReset = this.onReset.bind(this);
        this.onClear = this.onClear.bind(this);
    }

    componentWillReceiveProps(np) {
        if (this.props.serviceUrl !== np.serviceUrl) {
            this.reloadSchemas(np.serviceUrl);
        }
    }

    componentDidMount() {
        const {serviceUrl} = this.props;
        this.reloadSchemas(serviceUrl);
    }

    reloadSchemas(serviceUrl) {
        loadTapSchemas(serviceUrl).then((tm) => {

            const tableData = get(tm, 'tableData.data', []);
            const cidx = getColumnIdx(tm, 'schema_name');
            const treeData = tableData.map( (row) => ({key: `schema--${row[cidx]}`, title: row[cidx]}) );
            this.setState({treeData});
        });
    }

    onSelect(p) {
        const {fieldKey, groupKey} = this.props;

        const node = ReactDOM.findDOMNode(this.refs.adql);
        const textArea = node && node.firstChild;

        const [key=''] = p;
        const [type, value] = key.split('--');
        if (type === 'table') {
            insertNewLine(textArea, `SELECT TOP 1000 * FROM ${value}`, fieldKey, groupKey);
        } else if (type === 'column') {
            insertAtCursor(textArea, `${value}`, fieldKey, groupKey);
        }
    }

    onLoadData(treeNode) {
        const {serviceUrl} = this.props;
        const {treeData} = this.state;
        return new Promise((resolve) => {
            const {eventKey, schema, title} = treeNode.props;

            if (schema) {
                // it has schema info.. must be a table node.
                loadTapColumns(serviceUrl, schema, title).then( (tm) => {
                    const tableData = get(tm, 'tableData.data', []);
                    const nidx = getColumnIdx(tm, 'column_name');
                    const didx = getColumnIdx(tm, 'description');
                    const cols = tableData.map( (row) => {
                        const key = `column--${row[nidx]}`;
                        const title = row[nidx] + (row[didx] ? ` (${row[didx]})` : '');
                        return {key, title, isLeaf: true};
                    });
                    addChildNodes(treeData, eventKey, cols);
                    this.setState({ treeData: cloneDeep(treeData) });
                    resolve();
                });
            } else {
                loadTapTables(serviceUrl, title).then( (tm) => {
                    const tableData = get(tm, 'tableData.data', []);
                    const cidx = getColumnIdx(tm, 'table_name');
                    const tables = tableData.map( (row) => ({key: `table--${row[cidx]}`, title: row[cidx], schema: title}) );
                    addChildNodes(treeData, eventKey, tables);
                    this.setState({ treeData: cloneDeep(treeData) });
                    resolve();
                });
            }
        });
    }

    onClear() {
        const {fieldKey, groupKey} = this.props;
        dispatchValueChange({fieldKey, groupKey, value: '', valid: true});
    }

    onReset() {
        const {fieldKey, groupKey, origFieldKey} = this.props;
        const value = getFieldVal(groupKey, origFieldKey, '');
        dispatchValueChange({fieldKey, groupKey, value, valid: true});
    }

    render() {
        const {style={}, fieldKey, groupKey, adqlString=''} = this.props;

        const treeNodes = convertToTreeNode(this.state.treeData);
        const code = {style: {color: 'green', whiteSpace: 'pre', fontFamily: 'monospace'}};

        return (
                <SplitPane split='vertical' defaultSize={300} style={{position: 'relative', height: 600, ...style}}>
                    <SplitContent style={{overflow: 'auto'}}>
                        <b>Schema -> Table -> Column</b>
                        <Tree defaultExpandAll showLine loadData={this.onLoadData} onSelect={this.onSelect}>
                            {treeNodes}
                        </Tree>

                    </SplitContent>
                    <SplitContent style={{overflow: 'auto'}}>
                        <div className='flex-full'>
                            <div style={{display: 'inline-flex', marginRight: 25, justifyContent: 'space-between', alignItems: 'center'}}>
                                <h3>ADQL Query:</h3>
                                <div style={{display: 'inline-flux'}}>
                                    <button className='button std' title='Reset to the initial query' style={{height: 24, marginRight: 5}} onClick={this.onReset}>Reset</button>
                                    <button className='button std' title='Clear the query' style={{height: 24}} onClick={this.onClear}>Clear</button>
                                </div>
                            </div>
                            <InputAreaFieldConnected
                                ref='adql'
                                style={{width: 'calc(100% - 30px)', resize: 'none'}} rows={10}
                                fieldKey={fieldKey}
                                tooltip='ADQL to submit to the selected TAP service'
                            />
                            <div style={{color: '#4c4c4c'}}>
                                <h3>Schema Browser Hints</h3>
                                <div style={{marginLeft: 5}}>
                                    <div>Click on a Table node to insert a default SELECT statement of that table into the Query input box.</div>
                                    <div>Click on a Column node to insert the column's name at the Query input box's cursor.</div>
                                </div>
                                <h3>Popular Functions</h3>
                                <div style={{marginLeft: 5}}>
                                    <div><span {...code}>{'TOP n                   '}</span>{': Limit the results to n number of records'}</div>
                                    <div><span {...code}>{'ORDER BY [ASC/DESC]     '}</span>{': Used for sorting'}</div>
                                    <br/>
                                    <div {...code}>{"POINT('coordinate system', right ascension, declination)"}</div>
                                    <div {...code}>{"CIRCLE('coordinate system',right ascension center, declination center, radius)"}</div>
                                    <div {...code}>{"BOX('coordinate system', right ascension center, declination center, width, height)"}</div>
                                    <div {...code}>{"POLYGON('coordinate system', coordinate point 1, coordinate point 2, coordinate point 3...)"}</div>
                                    <div {...code}>{'DISTANCE(point1, point2)'}</div>
                                    <div {...code}>{'CONTAINS(region1, region2)'}</div>
                                    <div {...code}>{'INTERSECTS(region1, region2)'}</div>
                                </div>

                                <h3>Sample Queries</h3>
                                <div style={{marginLeft: 5}}>
                                    <div          >{'A 1 degree cone search around M101 would be:'}</div>
                                    <div {...code}>{"SELECT * FROM fp_psc WHERE CONTAINS(POINT('J2000',ra,dec),CIRCLE('J2000',210.80225,54.34894,1.0))=1"}</div>
                                    <br/>
                                    <div          >{'A 1 degree by 1 degree box around M101 would be:'}</div>
                                    <div {...code}>{"SELECT * FROM fp_psc WHERE CONTAINS(POINT('J2000',ra,dec),BOX('J2000',210.80225,54.34894,1.0,1.0))=1"}</div>
                                    <br/>
                                    <div          >{'A triangle search around M101 would be:'}</div>
                                    <div {...code}>{`SELECT * FROM fp_psc WHERE CONTAINS(POINT('J2000',ra,dec),
                      POLYGON('J2000',209.80225,53.34894,209.80225,55.34894,211.80225,54.34894))=1`}</div>
                                </div>
                            </div>
                        </div>
                    </SplitContent>
                </SplitPane>
        );
    }
}

AdvancedADQL.propTypes= {
    fieldKey:       PropTypes.string,
    origFieldKey:   PropTypes.string,               // used for reset
    groupKey:       PropTypes.string,
    serviceUrl:     PropTypes.string,
    style:          PropTypes.object,
};

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
};


function insertAtCursor (input, textToInsert, fieldKey, groupKey) {
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

}

function insertNewLine (input, textToInsert, fieldKey, groupKey) {
    const value = (input.value ? input.value + '\n' : '') + textToInsert;
    dispatchValueChange({fieldKey, groupKey, value, valid: true});
}