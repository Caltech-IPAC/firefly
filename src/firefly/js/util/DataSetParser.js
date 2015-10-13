/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * @author tatianag
 */

import {HAS_ACCESS_CNAME, TableMeta} from '../data/table/TableMeta.js';
import {BaseTableColumn, Align} from '../data/table/BaseTableColumn.js';
import {BaseTableData} from '../data/table/BaseTableData.js';
import {DataSet} from '../data/table/DataSet.js';
import {CoordUtil} from '../visualize/CoordUtil.js';

const LABEL_TAG = 'col.@.Label';
const VISI_TAG = 'col.@.Visibility';
const WIDTH_TAG = 'col.@.Width';
const PREF_WIDTH_TAG = 'col.@.PrefWidth';
const DESC_TAG = 'col.@.ShortDescription';
const UNIT_TAG = 'col.@.Unit';
const ITEMS_TAG = 'col.@.Items';
const SORT_BY_TAG = 'col.@.SortByCols';

const RELATED_COLS_TAG = 'col.related';
const GROUPBY_COLS_TAG = 'col.groupby';

const VISI_SHOW = 'show';
const VISI_HIDE = 'hide';
const VISI_HIDDEN = 'hidden';      // for application use only.


export const makeAttribKey = function(tag, colName) {
    return tag.replace('@', colName);
};

/*
 * @param raw {RawDataSet}
 * @return {DataSet}
 */
export const parseRawDataSet = function(raw) {
//        GwtUtil.showScrollingDebugMsg('start parsing raw .. ');
    const dataset = new DataSet();
    dataset.setMeta(raw.meta);
    dataset.setStartingIdx(raw.startingIndex);
    dataset.setTotalRows(raw.totalRows);

    dataset.setColumns(parseColumns(raw.dataSetString));
    dataset.setModel(parseTableModel(dataset, raw.dataSetString));

    dataset.getMeta().setAttributes(dataset.getModel().getAttributes());


    const relCols = dataset.getMeta().getAttribute(RELATED_COLS_TAG);
    if (relCols) {
        dataset.getMeta().relatedCols(relCols.split(','));
    }

    const grpByCols = dataset.getMeta().getAttribute(GROUPBY_COLS_TAG);
    if (grpByCols) {
        dataset.getMeta().groupByCols(grpByCols.split(','));
    }

    var columns = dataset.getColumns(); // array of BaseTableColumn objects
    var attribs = dataset.getMeta().attributes;
    for (var i=0; i<columns.length; i++) {
        const c = columns[i];
        if (c.getUnits() === 'RA' || c.getUnits() === 'DEC') {
            c.setWidth(13);
        }

        // modify column's attributes based on table's attributes
        const label = attribs.get( makeAttribKey(LABEL_TAG, c.getName()) );
        if (label) {
            c.setTitle(label);
        }

        const desc = attribs.get( makeAttribKey(DESC_TAG, c.getName()) );
        if (desc) {
            c.setShortDesc(desc);
        }

        const vis = attribs.get( makeAttribKey(VISI_TAG, c.getName()) );
        if (vis) {
            if (vis === VISI_HIDDEN) {
                c.setHidden(true);
                c.setVisible(false);
            } else if (vis===VISI_HIDE) {
                c.setHidden(false);
                c.setVisible(false);
            }
        }

        const width = attribs.get( makeAttribKey(WIDTH_TAG, c.getName()) );
        if (width) {
            let w = parseInt(width, 10);
            if (Number(width) === w) {
                c.setWidth(parseInt(width, 10));
            }
        }

        const prefWidth = attribs.get( makeAttribKey(PREF_WIDTH_TAG, c.getName()) );
        if (prefWidth) {
            const w = parseInt(prefWidth, 10);
            if (Number(prefWidth) === w) {
                c.setPrefWidth(parseInt(prefWidth, 10));
            }
        }

        const unit = attribs.get( makeAttribKey(UNIT_TAG, c.getName()) );
        if (unit) {
            c.setUnits(unit);
        }

        const enumVals = attribs.get( makeAttribKey(ITEMS_TAG, c.getName()) );
        if (enumVals) {
            c.setEnums(enumVals.split(','));
        }

        const sortBy = attribs.get( makeAttribKey(SORT_BY_TAG, c.getName()) );
        if (sortBy) {
            c.setSortByCols(sortBy.split(','));
        }
    }

    return dataset;
};

/*
 * @param dataset {DataSet}
 * @param lines {String}
 * @return {BaseTableModel}
 */
const parseTableModel = function(dataset, lines) {
    var data = [];
    var attribs = [];

    // TODO: might want to use indexOf for efficiency
    lines.split('\n').forEach(function(s){
        if (s) {
            if (s.startsWith('\\')) {
                const kv = s.substring(1).split('=', 2);
                const ktp = kv[0].trim().split('\\s+', 2);
                const val = kv.length === 1 ? '' : kv[1].trim();
                const key = ktp.length === 1 ? ktp[0] : ktp[1];
                attribs.push([key, val]);
            } else if (!s.startsWith('|')) {
                const row = getData(dataset.getColumns(), s, true);
                data.push(row);
            }
        }
    });

    var columns = [];
    dataset.getColumns().forEach(function(col) {
        columns.push(col.getName());

    });

    var model = new BaseTableData(columns);
    model.setHasAccessCName(dataset.getMeta().getAttribute(HAS_ACCESS_CNAME));
    attribs.forEach(function(a) {
        model.setAttribute(a[0], a[1]);
    });

    data.forEach(function(d) {
        model.addRow(d);
    });

    return model;
};

const parseColumns = function(lines) {
    var columns = [];
    var headerLineIdx = 0;

    // TODO: might want to use indexOf for efficiency
    lines.split('\n').some(function(line) {
        if (line) {
            if (line.startsWith('|')) {
                if (headerLineIdx === 0) {       // name
                    const cols = line.split('|');
                    cols.forEach(function(col){
                        if (col) {
                            const c = new BaseTableColumn(col.trim());
                            c.setWidth(col.length);
                            columns.push(c);

                            //TODO: remove this code when DB is updated with proper 'format' info
                            if (col.startsWith('raj2000')) {
                                c.setUnits('RA');
                            } else if (col.startsWith('decj2000')) {
                                c.setUnits('DEC');
                            }
                        }
                    });
                } else if (headerLineIdx === 1) { // type
                    let c = -1;
                    const cols = line.split('|');
                    cols.forEach(function(t) {
                        if (t) {
                            columns[c].setType(t.trim());
                        }
                        c++;
                    });
                } else if (headerLineIdx === 2) { // units
                    let c = -1;
                    const cols = line.split('|');
                    cols.forEach(function(u){
                        if (u) {
                            columns[c].setUnits(u.trim());
                        }
                        c++;
                    });
                }
                headerLineIdx++;
            } else if (!line.startsWith('\\')) {
                const data = getData(columns, line, false);
                for(var i = 0; i < columns.length; i++) {
                    const c = columns[i];
                    const s = data[i];
                    if(s.startsWith(' ') && s.endsWith(' ')) {
                        c.setAlign(Align.CENTER);
                    } else if (s.startsWith(' ')) {
                        c.setAlign(Align.RIGHT);
                    } else {
                        c.setAlign(Align.LEFT);
                    }
                }
                return true; // to break the
            }
        }
        return false;
    });


    return columns;
};

/*
 * @param columns {Array} of BaseTableColumn objects
 * @param line {String}
 * @param doTrim {Boolean}
 * @return {Array}
 */
const getData = function(columns, line, doTrim) {
    var data = new Array(columns.length);
    var beg, end = 0;
    for(var i = 0; i < columns.length; i++) {
        let c = columns[i];
        beg = end + 1;
        end = beg + c.getWidth();
        if (end > line.length) {
            data[i] = line.substring(beg);
        } else {
            data[i] = line.substring(beg, end);
        }
        if (data[i] && c.getUnits()) {
            try {
                if (c.getUnits() === 'RA') {
                    let d = Number(data[i].trim());
                    if (!isNaN(d)) {
                        data[i] = CoordUtil.convertLonToString(d, true);
                    }
                } else if (c.getUnits()==='DEC') {
                    let d = Number(data[i].trim());
                    if (!isNaN(d)) {
                        data[i] = CoordUtil.convertLatToString(d, true);
                    }
                }
            } catch (e) {
                throw 'error in parsing RA/DEC values:' + data[i] + ' - ' + e;
            }
        }

        if (doTrim) {
            data[i] = data[i].trim();
        }

    }
    return data;
};

