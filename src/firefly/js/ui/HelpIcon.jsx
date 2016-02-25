/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PropTypes} from 'react';
import {flux} from '../Firefly.js';
import appDataCntlr from '../core/AppDataCntlr.js';

const Padding = '5';  // padding for div around image
const DefaultImageSize = '20';
const DefaultImageHelp = 'images/Help.png';

export class HelpIcon extends React.Component {
    constructor(props) {
        super(props);

        var {width, height} = Object.assign({}, {width: DefaultImageSize, height: DefaultImageSize}, props);

        this.state = {
            width: width.toString(),
            height: height.toString(),
            hover: false
        };

        this.onClick = this.onClick.bind(this);
        this.onMouseHover = this.onMouseHover.bind(this);
        this.onMouseOut = this.onMouseOut.bind(this);
    }

    onClick() {
        flux.process({
                        type:appDataCntlr.HELP_LOAD,
                        helpID: this.props.id
                    });
    }

    onMouseHover() {
        this.setState({hover: true});
    }

    onMouseOut() {
        this.setState({hover: false});
    }

    render() {
        const {width, height} = this.state;
        var   imgStyle = {width, height};
        var   divStyle = {padding: Padding};
        var   imgSrc = (this.props.src) ? this.prop.src : DefaultImageHelp;

        divStyle.cursor = this.state.hover ? 'pointer' : 'default';

        return (
            <div style={divStyle}
                 onMouseOver={this.onMouseHover}
                 onMouseOut={this.onMouseOut} >
                <img style={imgStyle} onClick={this.onClick} src={imgSrc} />
            </div>);
    }
}

HelpIcon.propTypes = {
    width: PropTypes.number,
    height: PropTypes.number,
    id: PropTypes.string.isRequired,
    src: PropTypes.string
};

export default HelpIcon;
