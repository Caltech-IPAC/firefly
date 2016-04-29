/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import difference from 'lodash/difference';
import isEqual from 'lodash/isEqual';
import sCompare from 'react-addons-shallow-compare';
import CanvasWrapper from './CanvasWrapper.jsx';
import TextDrawer from './TextDrawer.jsx';
import {getDrawLayerById} from '../PlotViewUtil.js';
import {dlRoot} from '../DrawLayerCntlr.js';
import {flux} from '../../Firefly.js';


const isVisible= (drawLayer,plotId) => drawLayer.visiblePlotIdAry.includes(plotId);


function makeTextDrawIfNecessary(textDrawAry,width,height) {
    if (textDrawAry && textDrawAry.length) {
        return (<TextDrawer textDrawAry={textDrawAry} width={width} height={height}/>);
    }
    else {
        return null;
    }
}


export class DrawerComponent extends React.Component {
    constructor(props)  {
        super(props);
        const {drawLayerId, plot}=  props;
        const drawLayer= (plot && drawLayerId) ? getDrawLayerById(dlRoot(),drawLayerId) : null;
        this.state= {textDrawAry:[], drawLayer};
        this.getDrawLayer= this.getDrawLayer.bind(this);
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }


    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.removeListener) this.removeListener();
    }


    componentDidMount() {
        this.iAmMounted= true;
        if (this.props.plot) {
            this.removeListener= flux.addListener(() => this.storeUpdate());
            this.storeUpdate();
        }
        if (this.props.setSimpleUpdateNotify) {
            this.props.setSimpleUpdateNotify((drawLayer) => this.simpleUpdate(drawLayer));
        }
    }

    storeUpdate() {
        const {drawLayerId, plot}= this.props;

        if (plot && drawLayerId)  {
            const drawLayer= getDrawLayerById(dlRoot(), drawLayerId);
            if (drawLayer!==this.state.drawLayer && this.iAmMounted) {
                this.setState({drawLayer});
            }
        }
    }
    
    simpleUpdate(drawLayer) {
        if (drawLayer!==this.state.drawLayer && this.iAmMounted) {
            this.setState({drawLayer});
        }
    }
    
    getDrawLayer() {
        return this.props.getDrawLayer ? this.props.getDrawLayer() : this.state.drawLayer;
    }
    

    textUpdateCallback(textDrawAry) {
        var {textDrawAry:old}= this.state;
        //if ((!textDrawAry && !old)  || !textDrawAry.length && !old.length) return;
        if (!difference(textDrawAry,old).length) return;

        var doUpdate=
            old.length!=textDrawAry.length ||
            textDrawAry.some( (e,idx) =>
                        !isEqual(e.style,old[idx].style) || e.text!=old[idx].text);
        if (!doUpdate) return;
        if (this.iAmMounted) this.setState({textDrawAry});
    }

    render() {
        var {plot, width, height}= this.props;
        if (plot&& !isVisible(this.state.drawLayer,plot.plotId)) return false;
        var style= {position:'absolute',left:0,right:0,width,height};

        return (
            <div className='drawerComponent' style={style}>
                <CanvasWrapper {...this.props} getDrawLayer={this.getDrawLayer}
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
    drawLayerId : React.PropTypes.string, //drawLayer or drawData is Required
    getDrawLayer : React.PropTypes.func,
    setSimpleUpdateNotify : React.PropTypes.func
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
    getDrawLayer : React.PropTypes.func.isRequired
};



