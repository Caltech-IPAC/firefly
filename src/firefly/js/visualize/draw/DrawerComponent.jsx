/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import difference from 'lodash/array/difference';
import sCompare from 'react-addons-shallow-compare';
import CanvasWrapper from './CanvasWrapper.jsx';
import TextDrawer from './TextDrawer.jsx';


const isVisible= (drawLayer,plotId) => drawLayer.visiblePlotIdAry.includes(plotId);


function makeTextDrawIfNecessary(textDrawAry,width,height) {
    if (textDrawAry && textDrawAry.length) {
        return (<TextDrawer textDrawAry={textDrawAry} width={width} height={height}/>);
    }
    else {
        return null;
    }
}


class DrawerComponent extends React.Component {
    constructor(props)  {
        super(props);
        this.state= {textDrawAry:[]};
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    textUpdateCallback(textDrawAry) {
        var {textDrawAry:old}= this.state;
        //if ((!textDrawAry && !old)  || !textDrawAry.length && !old.length) return;
        if (!difference(textDrawAry,old).length) return;
        this.setState({textDrawAry});
    }

    render() {
        var {plotView, drawLayer}= this.props;
        if (!plotView || !drawLayer) return false;
        if (!isVisible(drawLayer,plotView.plotId)) return false;
        var {primaryPlot}= plotView;
        if (!primaryPlot) return false;
        var {dim:{width,height}}= primaryPlot.viewPort;
        var style= {position:'absolute',left:0,right:0,width,height};

        return (
            <div className='drawComponent' style={style}>
                <CanvasWrapper plotView={plotView} drawLayer={drawLayer} textUpdateCallback={this.textUpdateCallback.bind(this)}/>
                {makeTextDrawIfNecessary(this.state.textDrawAry,width,height)}
            </div>
        );
    }
}

DrawerComponent.propTypes= {
    plotView : React.PropTypes.object.isRequired,
    drawLayer : React.PropTypes.object.isRequired
};




export default DrawerComponent;

