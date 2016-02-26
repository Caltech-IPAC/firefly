/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import {flux} from '../Firefly.js';
import appDataCntlr from '../core/AppDataCntlr.js';

import largeHelp from 'html/images/icons-2014/Help.png';
import smallHelp from 'html/images/icons-2014/Help-16x16.png';

class HelpIcon extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            cursor: 'default'
        };

        this.onClick = this.onClick.bind(this);
        this.onMouseHover = this.onMouseHover.bind(this);
        this.onMouseOut = this.onMouseOut.bind(this);
    }

    onClick() {
        flux.process({
                        type:appDataCntlr.HELP_LOAD,
                        helpID: this.props.helpid
                    });
    }

    onMouseHover() {
        this.setState({cursor: 'pointer'});
    }

    onMouseOut() {
        this.setState({cursor: 'default'});
    }

    render() {
        var   imgSrc = this.props.largesize ? largeHelp : smallHelp;
        var   {cursor} = this.state;
        var   divStyle = {cursor};

        return (
            <div style={divStyle}
                 onMouseOver={this.onMouseHover}
                 onMouseOut={this.onMouseOut} >
                <img onClick={this.onClick} src={imgSrc} />
            </div>);
    }
}

HelpIcon.propTypes = {
    helpid: PropTypes.string,
    largesize: PropTypes.bool
};

HelpIcon.defaultProps = {
    largesize: false
};

export default HelpIcon;