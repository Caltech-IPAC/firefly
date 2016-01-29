/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import sCompare from 'react-addons-shallow-compare';



function drawOnCanvas(c,drawIt) {
    drawIt(c);
}

export class SimpleCanvas extends React.Component {


    constructor(props) {
        super(props);
        this.drawer= null;
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentDidUpdate() {
        if (this.canvas) {
            drawOnCanvas(this.canvas,this.props.drawIt);
        }
    }

    setUpCanvas(c) {
        this.canvas= c;
        drawOnCanvas(c,this.props.drawIt);
    }

    render() {
        var {drawIt, width, height}= this.props;
        return (
            <canvas width={width+''} height={height+''} ref={(c) => this.setUpCanvas(c)}/>
        );
    }
}

SimpleCanvas.propTypes= {
    drawIt : React.PropTypes.func.isRequired,
    width : React.PropTypes.number.isRequired,
    height : React.PropTypes.number.isRequired
};


