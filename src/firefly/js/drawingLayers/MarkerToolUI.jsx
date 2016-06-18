/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import sCompare from 'react-addons-shallow-compare';
import {TextLocation} from '../visualize/draw/DrawingDef.js';
import {dispatchModifyCustomField} from '../visualize/DrawLayerCntlr.js';
import {addNewDrawLayer} from '../visualize/ui/MarkerDropDownView.jsx';
import {isNil, get} from 'lodash';

export const getMarkerToolUIComponent = (drawLayer,pv) => <MarkerToolUI drawLayer={drawLayer} pv={pv}/>;
export const defaultMarkerTextLoc = TextLocation.CIRCLE_SE;


class MarkerToolUI extends React.Component {
    constructor(props) {
        super(props);

        var markerText = get(this.props.drawLayer, ['drawData', 'data', '0', 'text'], '');
        var markerTextLoc = get(this.props.drawLayer, ['drawData', 'data', '0', 'textLoc'], defaultMarkerTextLoc);
        var markerType = get(this.props.drawLayer, ['markerType'], 'marker');

        this.state = {markerText,  markerTextLoc: markerTextLoc.key, markerType};
        this.changeMarkerText = this.changeMarkerText.bind(this);
        this.changeMarkerTextLocation = this.changeMarkerTextLocation.bind(this);
    }

    shouldComponentUpdate(np, ns) {return sCompare(this, np, ns); }

    changeMarkerText(ev) {
        var markerText = get(ev, 'target.value');

        if (isNil(markerText)) markerText = '';
        this.setState({markerText});

        this.props.drawLayer.title = markerText;
        dispatchModifyCustomField( this.props.drawLayer.drawLayerId,
                    {markerText, markerTextLoc: TextLocation.get(this.state.markerTextLoc)}, this.props.pv.plotId);
    }

    changeMarkerTextLocation(ev) {
        var markerTextLoc = get(ev, 'target.value');

        this.setState({markerTextLoc});
        dispatchModifyCustomField( this.props.drawLayer.drawLayerId,
                    {markerText: this.state.markerText, markerTextLoc: TextLocation.get(markerTextLoc)}, this.props.pv.plotId);
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
                <div style={tStyle}> Label:<input style={{width: 60}}
                                                  type='text'
                                                  value={this.state.markerText}
                                                  onChange={this.changeMarkerText}/>
                </div>
                <div style={tStyle}> Corners:
                    <select value={this.state.markerTextLoc} onChange={ this.changeMarkerTextLocation }>
                        <option value={TextLocation.CIRCLE_NE.key}> NE </option>
                        <option value={TextLocation.CIRCLE_NW.key}> NW </option>
                        <option value={TextLocation.CIRCLE_SE.key}> SE </option>
                        <option value={TextLocation.CIRCLE_SW.key}> SW </option>
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
    drawLayer     : React.PropTypes.object.isRequired,
    pv            : React.PropTypes.object.isRequired
};
