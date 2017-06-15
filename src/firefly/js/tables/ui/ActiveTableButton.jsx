/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import shallowequal from 'shallowequal';

import {flux} from '../../Firefly.js';
import {getTblInfoById, getActiveTableId} from '../TableUtil.js';

/**
 * This button will track the highlighted row of the given tbl_id or of the active table of the given tbl_grp.
 * There are 2 callbacks available as props.
 * onClick: This function is called when a user click on this button.
 * onChange({tbl_id, highlightedRow}): This function is called when highlightedRow changes.
 *                                     The returned object of this function will be set into state.
 *                                     Props like show or title can be changed via this function.
 */
export class ActiveTableButton extends PureComponent {

    constructor(props) {
        super(props);
        this.state = {...props};
        this.onClick = this.onClick.bind(this);
    }

    componentDidMount() {
        this.removeListener= flux.addListener(() => this.storeUpdate());
    }

    componentWillUnmount() {
        this.removeListener && this.removeListener();
        this.isUnmounted = true;
    }

    storeUpdate() {
        if (!this.isUnmounted) {
            var {tbl_id, tbl_grp, onChange} = this.props;
            tbl_id = tbl_id || getActiveTableId(tbl_grp);
            const {highlightedRow} = getTblInfoById(tbl_id);
            const nextState= Object.assign({}, this.state, {tbl_id, highlightedRow});

            if (!shallowequal(this.state, nextState)) {
                this.setState({tbl_id, highlightedRow});
                onChange && this.setState(onChange({tbl_id, highlightedRow}));
            }
        }
    }

    onClick() {
        const {onClick} = this.props;
        const {tbl_id, highlightedRow} = this.state;
        onClick && onClick({tbl_id, highlightedRow});
    }

    render() {
        const {show=true, enable=true, title, style, type='standard'} = this.state;
        const astyle = Object.assign({textOverflow: 'ellipsis', maxWidth: 200, overflow: 'hidden'}, style);
        const className = type === 'standard' ? 'button std' : 'button std hl';
        return show && (
            <button type = 'button' className={className} onClick = {this.onClick} disabled={!enable}>
                <div title={title} style={astyle}>{title}</div>
            </button>
        );
    }
}


ActiveTableButton.propTypes = {
    tbl_id      : PropTypes.string,
    tbl_grp     : PropTypes.string,
    show        : PropTypes.bool,
    title       : PropTypes.string,
    type        : PropTypes.oneOf(['standard', 'highlight']),
    style       : PropTypes.object,
    onChange    : PropTypes.func,
    onClick     : PropTypes.func
};

