/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {isEmpty, get} from 'lodash';
import {fieldGroupConnector} from '../../ui/FieldGroupConnector.jsx';
//import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import * as TblUtil from '../../tables/TableUtil.js';

export class CatalogTableView extends Component {

    /**
     *
     * @param props
     */
    constructor(props) {
        super(props);
        this.state = {
            idx: 0
        };
        //this.state = {
        //    fields: FieldGroupUtils.getGroupFields(this.props.groupKey)
        //};
    }

    //
    //componentWillUnmount() {
    //    if (this.removeListener) this.removeListener();
    //    this.iAmMounted = false;
    //}
    //
    //componentDidMount() {
    //    this.iAmMounted = true;
    //    this.removeListener = FieldGroupUtils.bindToStore(this.props.groupKey, (fields) => {
    //        if (this.iAmMounted) this.setState({fields});
    //    });
    //}

    /**
     * The function should be used to fetch the DD from a catalog named passed in as argument catName
     * and used to populate a table with column definition, description and any DD fetched
     * @param {string} catName the catalog name to get DD fetched from
     */
    updateTable(catName = 'wise_allwise_p3as_psd') {
        this.getDD(catName);//'wise_allwise_p3as_psd'
    }

    /**
     * Getting dd info from catalog name catName
     * @param catName string name of the catalog for searching DD information
     */
    getDD(catName) {


        const request = {id: 'GatorDD', 'catalog': catName}; //Fetch DD master table
        TblUtil.doFetchTable(request).then((tableModel) => {

            var data = tableModel.tableData.data;
            const html = data.map((c) => {
                return c[0] + ':' + c[1] + ' [' + c[2] + ']';
            });
            const cols = tableModel.tableData.columns;
            // What to do with that?
            // this.setState({...state},{})

        }).catch((reason) => {
                console.error(reason);
            }
        );
    }

    componentWillReceiveProps(np) {
        const newFirstCatalogValue = get(np, 'data[0].value', '');
        const oldFirstValueCatalog = get(this.props, 'data[0].value', '');
        if (newFirstCatalogValue != oldFirstValueCatalog) {
            this.setState(...this.state, {idx: 0});
        } else {
            this.setState(...this.state, {idx: np.indexClicked});
        }

    }

    /**
     * Only update when new catalog selected or different list item has been clicked
     * @param np
     * @returns {boolean}
     */
    shouldComponentUpdate(np) {
        const newCatValue = get(np, 'value', '');
        const oldCatValue = get(this.props, 'value', '');
        //FIXME Doesn't work when user comes back from another tab
        return newCatValue != oldCatValue;
    }

    render() {
        var {data, cols, onClick, fieldKey} = this.props;//data {cat:...}
        const indexClicked = get(this.state, 'idx', 0);

        if (isEmpty(data)) {
            return (
                <div style={{position: 'relative'}}>
                    <div className='loading-mask'/>
                </div>
            );
        }

        var items = data.map((o, index) => {


            const clicked = indexClicked === index;

            return (

                <CatalogTableItem
                    key={index}
                    onClick={onClick}
                    isClicked={clicked}
                    cols={cols}
                    itemData={o}/>
            );
        });

        return (
            <div>
                <table name={fieldKey}>
                    <tbody>
                    {items}
                    </tbody>
                </table>

            </div>
        );
    }
}

export const CatalogTableListField = fieldGroupConnector(CatalogTableView, getProps, CatalogTableView.propTypes, null);

CatalogTableView.propTypes = {
    data: PropTypes.array.isRequired,
    cols: PropTypes.array.isRequired,
    groupKey: PropTypes.string,
    onClick: PropTypes.func,
    fieldKey: PropTypes.string
};

CatalogTableView.defaultProps = {
    data: [],
    cols: []
};


function getProps(params, fireValueChange) {

    return Object.assign({}, params,
        {
            onClick: (ev) => handleOnClick(ev, params, fireValueChange)
        });
}

function handleOnClick(ev, params, fireValueChange) {

    // the value of this input field is a string
    fireValueChange({
        value: ev.currentTarget.value || ev.currentTarget.attributes['value'].value
    });
}

// get element data and wrapit around html markup knowing the columns name
function htmlElementMarkup(element, cols) {
    let m = '';
    const url = '<a target=\'_new\' href=\'http://irsa.ipac.caltech.edu/cgi-bin/Gator/nph-dd?mode=xml&catalog=@\'>go to dd</a>';
    cols.forEach((e, idx) => {
            if (idx < 10) {
                //<span class="marked-text" style="font-size:80%;">Rows: </span><span class="normal-text" style="font-size:80%;">18,240</span>
                m += '<b>' + e.name + '</b>: ' + element.cat[idx];
                if (e.name.includes('cat')) {
                    m += ' [ ' + url.replace('@', element.cat[idx]) + ' ]';
                }
                m += '    ';
            }
        }
    );
    return m;
}

class CatalogTableItem extends Component {

    /**
     *
     * @param props
     */
    constructor(props) {
        super(props);
    }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    render() {
        const {itemData, onClick, isClicked, cols} = this.props;

        const html = '<b>' + itemData.cat[2] + '</b></br>' + htmlElementMarkup(itemData, cols);//<li key="' + `${itemData.value}` + '"> + '</li>';
        const v = {__html: html};
        const color = isClicked ? '#7df26a' : 'white';

        return (
            <tr>
                <td value={itemData.value} onClick={(ev) => onClick ? onClick(ev) : null}
                    style={{backgroundColor: `${color}`}}
                    dangerouslySetInnerHTML={v}>
                </td>
            </tr>
        );
    }
}

CatalogTableItem.propTypes = {
    itemData: PropTypes.object.isRequired,
    isClicked: PropTypes.bool.isRequired,
    cols: PropTypes.array,
    onClick: PropTypes.func
};

CatalogTableItem.defaultProps = {
    itemData: {},
    isClicked: false
};