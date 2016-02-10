/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import difference from 'lodash/difference';
import isEqual from 'lodash/isEqual';
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

        var doUpdate=
            old.length!=textDrawAry.length ||
            textDrawAry.some( (e,idx) =>
                        !isEqual(e.style,old[idx].style) || e.text!=old[idx].text);
        if (!doUpdate) return;
        this.setState({textDrawAry});
    }

    render() {
        var {plot, drawLayer, width, height}= this.props;
        if (plot&& !isVisible(drawLayer,plot.plotId)) return false;
        var style= {position:'absolute',left:0,right:0,width,height};

        return (
            <div className='drawerComponent' style={style}>
                <CanvasWrapper {...this.props}
                               textUpdateCallback={this.textUpdateCallback.bind(this)}/>
                {makeTextDrawIfNecessary(this.state.textDrawAry,width,height)}
            </div>
        );
    }
}

DrawerComponent.propTypes= {
    width: React.PropTypes.number.isRequired,
    height: React.PropTypes.number.isRequired,
    plot: React.PropTypes.object, // plot is not used if drawLayer is not passed
    drawLayer : React.PropTypes.object, //drawLayer or drawData is Required
    drawData : React.PropTypes.array // only used it drawLayer is not defined
};


export function PlotDrawer(props) {
    return <DrawerComponent {...props} />;
}

PlotDrawer.propTypes= {
    width: React.PropTypes.number.isRequired,
    height: React.PropTypes.number.isRequired,
    plot: React.PropTypes.object, // plot is not used if drawLayer is not passed
    drawLayer : React.PropTypes.object.isRequired //drawLayer or drawData is Required
};

export function AnyDrawer(props) {
    return <DrawerComponent {...props} />;
}

AnyDrawer.propTypes= {
    width: React.PropTypes.number.isRequired,
    height: React.PropTypes.number.isRequired,
    drawData : React.PropTypes.array.isRequired // only used it drawLayer is not defined
};



