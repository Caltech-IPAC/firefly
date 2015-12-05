/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import ReactDOM from 'react-dom';
import TblUtil from '../TableUtil.js';
import TblCntlr from '../TablesCntlr';
import FixedDataTable from 'fixed-data-table';
import './TablePanel.css';
import Resizable from 'react-component-resizable';


const {Table, Column, Cell} = FixedDataTable;

const TextCell = ({rowIndex, data, col, ...props}) => {
    const val = (data[rowIndex] && data[rowIndex][col]) ? data[rowIndex][col] : 'undef';
    return (
        <Cell {...props}>
            {val}
        </Cell>
    );
};

function makeColWidth (tableModel) {
    var columns = TblUtil.find(tableModel, 'tableData', 'columns');
    return !columns ? {} : columns.reduce( (widths, col, cidx) => {
        const label = col.title || col.name;
        widths[col.name] = col.prefWidth || (label.length * 12);
        return widths;
    }, {});
}

class TablePanel extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            widthPx: 200,
            heightPx: 100,
            columnWidths: {}
        };
        this.onColumnResizeEndCallback = this.onColumnResizeEndCallback.bind(this);
        this.makeColumns = this.makeColumns.bind(this);
        this.onResize = this.onResize.bind(this);
        this.rowClassName = this.rowClassName.bind(this);
        this.onRowSelect = this.onRowSelect.bind(this);
    }

    onColumnResizeEndCallback(newColumnWidth, columnKey) {
        this.setState(({columnWidths}) => ({
            columnWidths: {
                ...columnWidths,
                [columnKey]: newColumnWidth,
            }
        }));
    }

    makeColumns(tableModel, selectable) {
        var columns = TblUtil.find(tableModel, 'tableData', 'columns');
        if (!columns) return false;
        var colsEl = columns.map((col, idx) => {
            return (
                <Column
                    columnKey={col.name}
                    header={<Cell>{col.title || col.name}</Cell>}
                    cell={<TextCell data={tableModel.tableData.data} col={idx} />}
                    fixed={false}
                    width={this.state.columnWidths[col.name]}
                    isResizable={true}
                    allowCellsRecycling={true}
                />
            )
        });
        if (selectable) {
            var cbox = <Column
                columnKey='selectable-checkbox'
                header={<div className="tablePanel_checkbox_wrapper"><input type='checkbox' /></div>}
                cell={<div className="tablePanel_checkbox_wrapper"><input type='checkbox' value='rowIn'/></div>}
                fixed={true}
                width={20}
                allowCellsRecycling={true}
            />
            colsEl.splice(0, 0, cbox);
        }
        return colsEl;
    }

    onResize() {
        const c = ReactDOM.findDOMNode(this);
        if (c && c.offsetWidth && c.offsetHeight) {
            this.setState({ widthPx: c.offsetWidth, heightPx: c.offsetHeight });
        }
    }

    onRowSelect(e, index) {
        var {tableModel} = this.props;
        if (tableModel) {
            TblCntlr.dispatchHighlightRow(tableModel.tbl_id, index);
        }
    }

    rowClassName(index) {
        var {tableModel} = this.props;
        const hlrow = tableModel.highlightedRow || 0;
        return (hlrow === index) ? 'tablePanel_Row_highlighted' : '';
    }

    componentWillUpdate(nProps, nContext) {
        if (Object.keys(this.state.columnWidths).length == 0) {
            this.state.columnWidths = makeColWidth(nProps.tableModel);
        }
    }

    componentDidMount() {
        const c = ReactDOM.findDOMNode(this);
        const w = c ? c.offsetWidth() : 200;
        const h = c ? c.offsetHeight() : 100;
        if (w > 0 && h > 0) {
            this.state.widthPx = w;
            this.state.heightPx = h;
        }
    }

    render() {
        var {tableModel, showFilters, selectable, width, height} = this.props;
        var {widthPx, heightPx} = this.state;

        if (!tableModel || !tableModel.tableData) return false;
        height = height || '100%';
        return (
        <Resizable style={{width:width, height:height}} onResize={this.onResize}>
            <Table
                rowHeight={30}
                headerHeight={35}
                rowsCount={tableModel.totalRows}
                onColumnResizeEndCallback={this.onColumnResizeEndCallback}
                onRowClick={this.onRowSelect}
                rowClassNameGetter={this.rowClassName}
                width={widthPx}
                height={heightPx}
                {...this.props}>
                {this.makeColumns(tableModel, selectable)}
            </Table>
        </Resizable>
        );
    }
}

TablePanel.propTypes = {
    tableModel : React.PropTypes.object,
    showFilters : React.PropTypes.bool,
    selectable : React.PropTypes.bool,
    width : React.PropTypes.string,
    height : React.PropTypes.string

};


export default TablePanel;
