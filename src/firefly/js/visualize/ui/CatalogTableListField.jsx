/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent, Component} from 'react';
import PropTypes from 'prop-types';
import {isEmpty, get} from 'lodash';
import {fieldGroupConnector} from '../../ui/FieldGroupConnector.jsx';
import './CatalogTableListField.css';

export class CatalogTableView extends Component {

    /**
     * @param props
     */
    constructor(props) {
        super(props);
        this.state = {
            idx: 0
        };
    }

    /**
     * Only update when new catalog selected or different list item has been clicked
     * @param np
     * @returns {boolean}
     */
    shouldComponentUpdate(np) {
        const newCatValue = get(np, 'value', '');
        const oldCatValue = get(this.props, 'value', '');
        return newCatValue != oldCatValue;
    }

    render() {
        var {data, cols, onClick, indexClicked} = this.props;//data {cat:...}

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
                    indexItem={index}
                    onClick={onClick}
                    isClicked={clicked}
                    cols={cols}
                    itemData={o}/>
            );
        });

        return (
            <div>
                <div className='catalogtable'>
                    <table style={{width:'100%'}}>
                        <tbody>
                        {items}
                        </tbody>
                    </table>
                </div>
            </div>
        );
    }
}

export const CatalogTableListField = fieldGroupConnector(CatalogTableView, getProps, CatalogTableView.propTypes, null);

CatalogTableView.propTypes = {
    data: PropTypes.array.isRequired,
    cols: PropTypes.array.isRequired,
    groupKey: PropTypes.string,
    indexClicked: PropTypes.number,
    onClick: PropTypes.func
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
    const value = ev.currentTarget.value || ev.currentTarget.attributes['value'].value;
    const indexClicked = parseInt(ev.currentTarget.attributes['id'].value);
    // the value of this input field is a string
    fireValueChange({
        value, indexClicked
    });
}

// get element data and wrapit around html markup knowing the columns name
function itemMarkupTransform(element, cols) {

    const itemIdx = [{key: 6, value: 'Rows'}, {key: 5, value: 'Cols'}, {key: 8, value: ''}, {key: 9, value: ''}];
    let html = '';

    itemIdx.forEach((e) => {

            const val = element.cat[e.key];
            if (val !== 'null') {
                if (e.value.length > 0) {
                    html += `<span class="marked-text">${e.value}</span>: <span class="normal-text">${val}</span>`;
                } else {
                    html += `<span class='href-item'>${val}</span>`;
                }
            }

            //if (e.name.includes('cat')) {
            //    m += ' [ ' + url.replace('@', element.cat[idx]) + ' ]';
            //}

            html += '&nbsp;&nbsp;&nbsp;&nbsp;';
        }
    );
    return html;
}

class CatalogTableItem extends PureComponent {

    /**
     *
     * @param props
     */
    constructor(props) {
        super(props);
    }


    render() {
        const {itemData, onClick, isClicked, cols, indexItem} = this.props;

        const html = '<span class="item-cell-title">' + itemData.cat[2] + '</span></br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;' + itemMarkupTransform(itemData, cols);
        const v = {__html: html};
        const color = isClicked ? '#7df26a' : 'white';
        const catname = itemData.value;
        return (
            <tr>
                <td title={`Table name: ${catname}`} className="cell" id={indexItem} value={itemData.value}
                    onClick={(ev) => onClick ? onClick(ev) : null}
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
    indexItem: PropTypes.number,
    onClick: PropTypes.func
};

CatalogTableItem.defaultProps = {
    itemData: {},
    isClicked: false
};
