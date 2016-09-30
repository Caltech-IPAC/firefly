/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';

import sCompare from 'react-addons-shallow-compare';
import './ProgressBar.css';

export class ProgressBar extends Component {

    constructor(props) {
        super(props);
    }

    render() {
        const {label, text, value} = this.props;
        return (
            <div className='ProgressBar'>
                {label && <div>{label}</div>}
                <div className='ProgressBar__shell'>
                    <div className='ProgressBar__bar' style={{width: `${value}%`}} />
                    <div className='ProgressBar__text'> {text} </div>
                </div>
            </div>
        );
    }
}

ProgressBar.propTypes = {
    label: PropTypes.string,
    text: PropTypes.string,
    value: PropTypes.number
};
