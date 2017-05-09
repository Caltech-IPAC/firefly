/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';



function drawOnCanvas(c,drawIt) {
    drawIt(c);
}

export class SimpleCanvas extends PureComponent {


    constructor(props) {
        super(props);
        this.drawer= null;
    }

    setUpCanvas(c) {
        this.canvas= c;
        drawOnCanvas(c,this.props.drawIt);
    }

    render() {
        const {width, height, backgroundColor}= this.props;
        return (
            <canvas width={width+''} height={height+''} style={{backgroundColor}} ref={(c) => this.setUpCanvas(c)}/>
        );
    }
}

SimpleCanvas.propTypes= {
    drawIt : PropTypes.func.isRequired,
    width : PropTypes.number.isRequired,
    height : PropTypes.number.isRequired,
    backgroundColor: PropTypes.string
};


