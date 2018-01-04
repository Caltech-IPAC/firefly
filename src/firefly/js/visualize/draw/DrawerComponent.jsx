/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import CanvasWrapper from './CanvasWrapper.jsx';
import {getDrawLayerById} from '../PlotViewUtil.js';
import {dlRoot} from '../DrawLayerCntlr.js';
import {flux} from '../../Firefly.js';


const isVisible= (drawLayer,plotId) => drawLayer.visiblePlotIdAry.includes(plotId);




export class DrawerComponent extends PureComponent {
    constructor(props)  {
        super(props);
        const {drawLayerId, plot}=  props;
        const drawLayer= (plot && drawLayerId) ? getDrawLayerById(dlRoot(),drawLayerId) : null;
        this.state= {drawLayer};
        this.getDrawLayer= this.getDrawLayer.bind(this);
    }

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
    

    render() {
        const {plot, width, height, idx}= this.props;
        if (plot&& !isVisible(this.state.drawLayer,plot.plotId)) return false;
        const style= {position:'absolute',left:0,right:0,width,height};

        return (
            <div className='drawerComponent' style={style}>
                <CanvasWrapper {...this.props} getDrawLayer={this.getDrawLayer} idx={idx}/>
            </div>
        );
    }
}

DrawerComponent.propTypes= {
    width: PropTypes.number.isRequired,
    height: PropTypes.number.isRequired,
    plot: PropTypes.object, // plot is not used if drawLayer is not passed
    drawLayerId : PropTypes.string, //drawLayer or drawData is Required
    getDrawLayer : PropTypes.func,
    idx : PropTypes.number,
    setSimpleUpdateNotify : PropTypes.func
};


export function PlotDrawer(props) {
    return <DrawerComponent {...props} />;
}

PlotDrawer.propTypes= {
    width: PropTypes.number.isRequired,
    height: PropTypes.number.isRequired,
    plot: PropTypes.object, // plot is not used if drawLayer is not passed
    drawLayer : PropTypes.object.isRequired //drawLayer or drawData is Required
};

export function AnyDrawer(props) {
    return <DrawerComponent {...props} />;
}

AnyDrawer.propTypes= {
    width: PropTypes.number.isRequired,
    height: PropTypes.number.isRequired,
    getDrawLayer : PropTypes.func.isRequired
};



