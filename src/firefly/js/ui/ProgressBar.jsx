/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import './ProgressBar.css';

export class ProgressBar extends PureComponent {

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
