/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../Firefly.js';
import {convertAngle} from '../visualize/VisUtil.js';
import {TextLocation} from '../visualize/draw/DrawingDef.js';
import {dispatchModifyCustomField, DRAWING_LAYER_KEY} from '../visualize/DrawLayerCntlr.js';
import {addFootprintDrawLayer} from '../visualize/ui/MarkerDropDownView.jsx';
import {ANGLE_UNIT} from '../visualize/draw/MarkerFootprintObj.js';
import {primePlot, getDrawLayerById} from '../visualize/PlotViewUtil.js';
import CoordUtil from '../visualize/CoordUtil.js';
import CsysConverter from '../visualize/CsysConverter.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import {InputFieldView} from '../ui/InputFieldView.jsx';
import {isNil, get, isEmpty} from 'lodash';
import {sprintf} from '../externalSource/sprintf';
import validator from 'validator';

export const getFootprintToolUIComponent = (drawLayer,pv) => <FootprintToolUI drawLayer={drawLayer} pv={pv}/>;
export const defaultFootprintTextLoc = TextLocation.REGION_SE;

const precision = '%.1f';

class FootprintToolUI extends PureComponent {
    constructor(props) {
        super(props);

        var fpObj = get(this.props.drawLayer, ['drawData', 'data', this.props.pv.plotId], {});
        var {angle = 0.0, angleUnit = ANGLE_UNIT.radian, text = '', textLoc = defaultFootprintTextLoc} = fpObj;
        var angleDeg = `${formatAngle(convertAngle(angleUnit.key, 'deg', angle))}`;
        var {fpInfo} = this.props.drawLayer;
        var {currentPt} = get(fpObj, 'actionInfo', {});
        const plot= primePlot(visRoot(),this.props.pv.plotId);

        this.csys = CsysConverter.make(plot);
        this.state = {fpText: text,  fpTextLoc: textLoc.key, angleDeg, fpInfo,
                      currentPt: this.csys.getWorldCoords(currentPt), isValidAngle: true};
        this.changeFootprintText = this.changeFootprintText.bind(this);
        this.changeFootprintTextLocation = this.changeFootprintTextLocation.bind(this);
        this.changeFootprintAngle = this.changeFootprintAngle.bind(this);
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
            const crtFpObj = get(dl, ['drawData', 'data', this.props.pv.plotId]);

            if (crtFpObj) {
                var {currentPt} = get(crtFpObj, 'actionInfo', {});
                if (currentPt) {
                    currentPt = this.csys.getWorldCoords(currentPt);
                    if (currentPt !== this.state.currentPt) {
                        this.setState({currentPt});
                    }
                }

                if (!get(crtFpObj, 'angleFromUI', false)) {
                    var {angle = 0.0, angleUnit = ANGLE_UNIT.radian} = crtFpObj;

                    angle = convertAngle(angleUnit.key, 'deg', angle);
                    this.setState({angleDeg: `${formatAngle(angle)}`, isValidAngle: true});
                }

                var {text = '', textLoc = defaultFootprintTextLoc} = crtFpObj;
                if (text !== this.state.fpText) {
                    this.setState({fpText: text});
                }
                if (textLoc.key !== this.state.fpTextLoc) {
                    this.setState({fpTextLoc: textLoc.key});
                }
            }
        }
    }

    changeFootprintText(ev) {
        var fpText = get(ev, 'target.value');

        if (isNil(fpText) || !fpText) {
            var dl = getDrawLayerById(flux.getState()[DRAWING_LAYER_KEY], this.props.drawLayer.drawLayerId);

            fpText = '';
            this.props.drawLayer.title = get(dl, 'defaultTitle');
        } else {
            this.props.drawLayer.title = fpText;
        }
        this.setState({fpText});

        dispatchModifyCustomField( this.props.drawLayer.drawLayerId,
                    {fpText, fpTextLoc: TextLocation.get(this.state.fpTextLoc), activePlotId: this.props.pv.plotId },
                     this.props.pv.plotId);
    }

    changeFootprintTextLocation(ev) {
        var fpTextLoc = get(ev, 'target.value');

        this.setState({fpTextLoc});
        dispatchModifyCustomField( this.props.drawLayer.drawLayerId,
                    {fpText: this.state.fpText, fpTextLoc: TextLocation.get(fpTextLoc), activePlotId: this.props.pv.plotId },
                     this.props.pv.plotId);
    }

    changeFootprintAngle(ev) {
        var angleDeg = get(ev, 'target.value');
        var isValidAngle = true;

        if  (isEmpty(angleDeg) || !validator.isFloat(angleDeg+'')) {
            if (!angleDeg) angleDeg = '';
            isValidAngle = false;
        }
        this.setState({isValidAngle, angleDeg});
        if (isValidAngle) {
            dispatchModifyCustomField(this.props.drawLayer.drawLayerId, {angleDeg, activePlotId: this.props.pv.plotId },
                                      this.props.pv.plotId);
        }
    }

    render() {
        const tStyle= {
            display:'flex',
            flexDirection:'column',
            paddingLeft : 10
        };

        const mStyle = {
            marginTop: 5
        };

        var {isValidAngle, angleDeg, fpText, fpTextLoc, fpInfo} = this.state;
        var textOnLink = get(fpInfo, 'fromFile') ? `Add ${get(fpInfo, 'fromFile')}`
                         :(get(fpInfo, 'fromRegionAry')
                                        ? `Add ${this.props.drawLayer.title}`
                                        : `Add ${get(fpInfo, 'footprint')} ${get(fpInfo, 'instrument')}`);


        return (
            <div style={{display:'flex', justifyContent:'flex-start', flexDirection: 'column', padding:'5px 0 9px 0'}}>
                <div style={{display:'flex', justifyContent: 'flex-start', paddingLeft:10, paddingBottom:8}}>
                    <div> Center:</div>
                    <div style={tStyle} className={'enable-select'}>
                        {convertWorldLonToString(this.state.currentPt, this.csys) + ', ' +
                        convertWorldLatToString(this.state.currentPt, this.csys)}
                    </div>
                </div>

                <div style={{display:'flex', justifyContent:'flex-start'}}>

                    <div style={tStyle}>
                        <div title={'Add a lable to this footprint'}> Label:&nbsp;<input style={{width: 86}}
                                           type='text'
                                           value={fpText}
                                           onChange={this.changeFootprintText}/>
                        </div>
                        <div style={mStyle} title={'Choose a corner'}> Label Location:&nbsp;
                            <select value={fpTextLoc} onChange={ this.changeFootprintTextLocation }>
                                <option value={TextLocation.REGION_NE.key}> NE </option>
                                <option value={TextLocation.REGION_NW.key}> NW </option>
                                <option value={TextLocation.REGION_SE.key}> SE </option>
                                <option value={TextLocation.REGION_SW.key}> SW </option>
                            </select>
                        </div>
                    </div>
                    <div style={tStyle}>
                        <InputFieldView
                                    valid={isValidAngle}
                                    onChange={this.changeFootprintAngle}
                                    value={angleDeg}
                                    message={'invalid angle value'}
                                    label={'Angle:'}
                                    labelWidth={30}
                                    style={{width: 50}}
                                    tooltip={'Enter the angle in degree you want the footprint rotated'}

                        />
                        <div style={mStyle} title={textOnLink}>
                            <a className='ff-href' style={{textDecoration: 'underline'}}
                               onClick={()=>addFootprintDrawLayer(this.props.pv, this.state.fpInfo)}>{textOnLink}</a>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
}


FootprintToolUI.propTypes= {
    drawLayer     : PropTypes.object.isRequired,
    pv            : PropTypes.object.isRequired
};

function convertWorldLonToString(pt) {
    var str = '';

    if (!pt) return str;
    return CoordUtil.convertLonToString(pt.getLon(), pt.getCoordSys());
}

function convertWorldLatToString(pt) {
    var str = '';

    if (!pt) return str;
    return `${CoordUtil.convertLatToString(pt.getLat(), pt.getCoordSys())} Equ J2000`;
}

function convertWorldToString(pt) {
    var str = '';
    if (!pt) return str;

    return `${sprintf(precision,pt.x)}, ${sprintf(precision,pt.y)}`;
}

function formatAngle(angle) {
     var anglePre = parseFloat(`${sprintf(precision,angle)}`);

     return `${anglePre}`;
}
