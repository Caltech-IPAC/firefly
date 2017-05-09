/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {TextLocation} from '../visualize/draw/DrawingDef.js';
import {flux} from '../Firefly.js';
import {dispatchModifyCustomField, DRAWING_LAYER_KEY} from '../visualize/DrawLayerCntlr.js';
import {getDrawLayerById} from '../visualize/PlotViewUtil.js';
import {addNewDrawLayer} from '../visualize/ui/MarkerDropDownView.jsx';
import {isNil, get} from 'lodash';

export const getMarkerToolUIComponent = (drawLayer,pv) => <MarkerToolUI drawLayer={drawLayer} pv={pv}/>;
//export const defaultMarkerTextLoc = TextLocation.CIRCLE_SE;
export const defaultMarkerTextLoc = TextLocation.REGION_SE;

class MarkerToolUI extends PureComponent {
    constructor(props) {
        super(props);

        var markerObj = get(this.props.drawLayer, ['drawData', 'data', this.props.pv.plotId], {});
        var {text = '', textLoc = defaultMarkerTextLoc} = markerObj;
        var markerType = get(this.props.drawLayer, ['markerType'], 'marker');

        this.state = {markerText: text,  markerTextLoc: textLoc.key, markerType};

        this.changeMarkerText = this.changeMarkerText.bind(this);
        this.changeMarkerTextLocation = this.changeMarkerTextLocation.bind(this);
    }

    componentWillUnmount() {
        this.iAmMounted= false;
        if (this.removeListener) this.removeListener();
    }

    componentDidMount() {
        this.iAmMounted= true;
        this.removeListener= flux.addListener(() => this.stateUpdate());
    }

    stateUpdate() {
        var dl = getDrawLayerById(flux.getState()[DRAWING_LAYER_KEY], this.props.drawLayer.drawLayerId);

        if (dl && this.iAmMounted) {
            const crtMarkerObj = get(dl, ['drawData', 'data', this.props.pv.plotId]);

            if (crtMarkerObj) {
                var {text = '', textLoc = defaultMarkerTextLoc} = crtMarkerObj;

                if (text !== this.state.markerText) {
                    this.setState({markerText: text});
                }
                if (textLoc.key !== this.state.markerTextLoc) {
                    this.setState({markerTextLoc: textLoc.key});
                }
            }
        }
    }

    changeMarkerText(ev) {
        var markerText = get(ev, 'target.value');

        if (isNil(markerText) || !markerText) {
            var dl = getDrawLayerById(flux.getState()[DRAWING_LAYER_KEY], this.props.drawLayer.drawLayerId);

            markerText = '';
            this.props.drawLayer.title = get(dl, 'defaultTitle');
        } else {
            this.props.drawLayer.title = markerText;
        }
        this.setState({markerText});

        dispatchModifyCustomField( this.props.drawLayer.drawLayerId,
                    {markerText, markerTextLoc: TextLocation.get(this.state.markerTextLoc), activePlotId: this.props.pv.plotId},
                     this.props.pv.plotId);
    }

    changeMarkerTextLocation(ev) {
        var markerTextLoc = get(ev, 'target.value');

        this.setState({markerTextLoc});
        dispatchModifyCustomField( this.props.drawLayer.drawLayerId,
                    {markerText: this.state.markerText, markerTextLoc: TextLocation.get(markerTextLoc), activePlotId: this.props.pv.plotId},
                     this.props.pv.plotId);
    }

    render() {
        const tStyle= {
            display:'inline-block',
            whiteSpace: 'nowrap',
            minWidth: '3em',
            paddingLeft : 5
        };

        var textOnLink = `Add ${this.state.markerType}`;
        var tipOnLink = `Add a ${this.state.markerType}`;

        return (
            <div style={{ padding:'5px 0 9px 0'}}>
                <div style={tStyle} title={'Add a label to this marker'}> Label:<input style={{width: 60}}
                                                  type='text'
                                                  value={this.state.markerText}
                                                  onChange={this.changeMarkerText}/>
                </div>
                <div style={tStyle} title={'Chhose a corner'}> Corners:
                    <select value={this.state.markerTextLoc} onChange={ this.changeMarkerTextLocation }>
                        <option value={TextLocation.REGION_NE.key}> NE </option>
                        <option value={TextLocation.REGION_NW.key}> NW </option>
                        <option value={TextLocation.REGION_SE.key}> SE </option>
                        <option value={TextLocation.REGION_SW.key}> SW </option>
                    </select>
                </div>
                <div style={tStyle}  title={tipOnLink}>
                    <a className='ff-href' style={{textDecoration: 'underline'}}
                       onClick={()=>addNewDrawLayer(this.props.pv, this.state.markerType)}>{textOnLink}</a>
                </div>
            </div>
        );
    }
}


MarkerToolUI.propTypes= {
    drawLayer     : PropTypes.object.isRequired,
    pv            : PropTypes.object.isRequired
};
