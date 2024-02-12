/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Chip, Stack, Typography} from '@mui/joy';
import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../core/ReduxFlux.js';
import {ListBoxInputFieldView} from '../ui/ListBoxInputField.jsx';
import {formatWorldPt} from '../visualize/ui/WorldPtFormat.jsx';
import {convertAngle} from '../visualize/VisUtil.js';
import {TextLocation} from '../visualize/draw/DrawingDef.js';
import {dispatchModifyCustomField, DRAWING_LAYER_KEY} from '../visualize/DrawLayerCntlr.js';
import {addFootprintDrawLayer} from '../visualize/ui/MarkerDropDownView.jsx';
import {ANGLE_UNIT} from '../visualize/draw/MarkerFootprintObj.js';
import {primePlot, getDrawLayerById} from '../visualize/PlotViewUtil.js';
import CsysConverter from '../visualize/CsysConverter.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import {InputFieldView} from '../ui/InputFieldView.jsx';
import {isNil} from 'lodash';
import {sprintf} from '../externalSource/sprintf';
import {FixedPtControl} from './FixedPtControl.jsx';

export const getFootprintToolUIComponent = (drawLayer,pv) => <FootprintToolUI drawLayer={drawLayer} pv={pv}/>;
export const defaultFootprintTextLoc = TextLocation.REGION_SE;

const precision = '%.1f';

class FootprintToolUI extends PureComponent {
    constructor(props) {
        super(props);

        const fpObj = this.props.drawLayer?.drawData?.data?.[this.props.pv.plotId] ?? {};
        const {angle = 0.0, angleUnit = ANGLE_UNIT.radian, text = '', textLoc = defaultFootprintTextLoc} = fpObj;
        const angleDeg = `${formatAngle(convertAngle(angleUnit.key, 'deg', angle))}`;
        const {fpInfo} = this.props.drawLayer;
        const {currentPt} = fpObj?.actionInfo ?? {};
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
        const dl = getDrawLayerById(flux.getState()[DRAWING_LAYER_KEY], this.props.drawLayer.drawLayerId);

        if (dl && this.iAmMounted) {
            const crtFpObj = dl?.drawData?.data?.[this.props.pv.plotId];

            if (crtFpObj) {
                let {currentPt} = crtFpObj?.actionInfo ?? {};
                if (currentPt) {
                    currentPt = this.csys.getWorldCoords(currentPt);
                    if (currentPt !== this.state.currentPt) {
                        this.setState({currentPt});
                    }
                }

                if (!crtFpObj?.angleFromUI) {
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
        let fpText = ev?.target?.value;

        if (isNil(fpText) || !fpText) {
            const dl = getDrawLayerById(flux.getState()[DRAWING_LAYER_KEY], this.props.drawLayer.drawLayerId);

            fpText = '';
            this.props.drawLayer.title = dl?.defaultTitle;
        } else {
            this.props.drawLayer.title = fpText;
        }
        this.setState({fpText});

        dispatchModifyCustomField( this.props.drawLayer.drawLayerId,
                    {fpText, fpTextLoc: TextLocation.get(this.state.fpTextLoc), activePlotId: this.props.pv.plotId },
                     this.props.pv.plotId);
    }

    changeFootprintTextLocation(ev,fpTextLoc ) {

        this.setState({fpTextLoc});
        dispatchModifyCustomField( this.props.drawLayer.drawLayerId,
                    {fpText: this.state.fpText, fpTextLoc: TextLocation.get(fpTextLoc), activePlotId: this.props.pv.plotId },
                     this.props.pv.plotId);
    }

    changeFootprintAngle(ev) {
        let angleDeg = ev?.target?.value;
        let isValidAngle = true;

        if  (isNaN(parseFloat(angleDeg))) {
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
        const {isValidAngle, angleDeg, fpText, fpTextLoc, fpInfo} = this.state;
        const textOnLink = fpInfo?.fromFile ? `Add another ${fpInfo.fromFile}`
                         :fpInfo?.fromRegionAry
                                        ? `Add another ${this.props.drawLayer.title}`
                                        : `Add another ${fpInfo?.footprint} ${fpInfo?.instrument}`;


        return (
            <Stack {...{py:1}}>
                <Stack {...{direction:'row', alignItems:'center', justifyContent: 'flex-start', spacing:1, pl:1, pb:1}}>
                    <Typography level='body-sm'>Center:</Typography>
                    {formatWorldPt(this.state.currentPt,3,false)}
                    <FixedPtControl wp={this.state.currentPt} pv={this.props.pv}/>
                </Stack>

                <Stack {...{direction:'row', spacing:1, justifyContent:'flex-start'}}>
                    <Stack spacing={1}>
                        <InputFieldView label='Label' tooltip= 'Add a lable to this footprint'
                                        slotProps={{input:{sx:{width:'15rem'}}}}
                                        onChange={this.changeFootprintText} value={fpText}/>
                        <ListBoxInputFieldView
                            onChange={this.changeFootprintTextLocation}
                            value={fpTextLoc}
                            label='Label Location' tooltip='Choose a corner'
                            options={[
                                {value: TextLocation.REGION_NE.key, label:'NE'},
                                {value: TextLocation.REGION_NW.key, label:'NW'},
                                {value: TextLocation.REGION_SE.key, label:'SE'},
                                {value: TextLocation.REGION_SW.key, label:'SW'},
                            ]}/>
                    </Stack>
                    <Stack spacing={1}>
                        <InputFieldView
                                    valid={isValidAngle}
                                    slotProps={{input:{sx:{width:'7rem'}}}}
                                    onChange={this.changeFootprintAngle}
                                    value={angleDeg}
                                    message='invalid angle value'
                                    label='Angle'
                                    tooltip='Enter the angle in degree you want the footprint rotated'

                        />
                        <Chip onClick={()=>addFootprintDrawLayer(this.props.pv, this.state.fpInfo)}>
                            {textOnLink}
                        </Chip>
                    </Stack>
                </Stack>
            </Stack>
        );
    }
}


FootprintToolUI.propTypes= {
    drawLayer     : PropTypes.object.isRequired,
    pv            : PropTypes.object.isRequired
};

function formatAngle(angle) {
     const anglePre = parseFloat(`${sprintf(precision,angle)}`);
     return `${anglePre}`;
}
