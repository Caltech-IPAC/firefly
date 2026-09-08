/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Chip, Stack, Tooltip, Typography} from '@mui/joy';
import React, {useState} from 'react';
import PropTypes from 'prop-types';
import {flux} from '../core/ReduxFlux.js';
import {ListBoxInputFieldView} from '../ui/ListBoxInputField.jsx';
import {useStoreConnector} from '../ui/SimpleComponent.jsx';
import {dispatchModifyCustomField} from '../visualize/DrawLayerDispatch';
import {formatWorldPt} from '../visualize/ui/WorldPtFormat.jsx';
import {DRAWING_LAYER_KEY} from '../visualize/VisConst';
import {convertAngle} from '../visualize/VisUtil.js';
import {TextLocation} from '../visualize/draw/DrawingDef.js';
import {addFootprintDrawLayer} from '../visualize/ui/MarkerDropDownView.jsx';
import {ANGLE_UNIT} from '../visualize/draw/MarkerFootprintObj.js';
import {currentP, getDrawLayerById} from '../visualize/PlotViewUtil.js';
import CsysConverter from '../visualize/CsysConverter.js';
import {InputFieldView} from '../ui/InputFieldView.jsx';
import {sprintf} from '../externalSource/sprintf';
import {FixedPtControl} from './FixedPtControl.jsx';

// key by plotId to handle switching between images
export const getFootprintToolUIComponent = (drawLayer,pv) =>
    <FootprintToolUI key={pv.plotId} drawLayer={drawLayer} pv={pv}/>;
export const defaultFootprintTextLoc = TextLocation.REGION_SE;

const precision = '%.1f';

export function FootprintToolUI({drawLayer, pv}) {
    const {drawLayerId, fpInfo} = drawLayer;
    const {plotId} = pv;

    const {hasData, currentPt, angle, angleUnit, angleFromUI, text, textLoc} = useStoreConnector(() => {
        const dl = getDrawLayerById(flux.getState()[DRAWING_LAYER_KEY], drawLayerId);
        const fpObj = dl?.drawData?.data?.[plotId];
        const {angle = 0.0, angleUnit = ANGLE_UNIT.radian, angleFromUI = false,
               text = '', textLoc = defaultFootprintTextLoc} = fpObj ?? {};
        return {hasData: Boolean(fpObj), currentPt: fpObj?.actionInfo?.currentPt,
                angle, angleUnit, angleFromUI, text, textLoc};
    }, [drawLayerId, plotId]);

    const csys = CsysConverter.make(currentP(plotId)?.plot);
    const derivedCenterPt = currentPt && csys?.getWorldCoords(currentPt);
    const derivedCenterKey = derivedCenterPt
        ? `${derivedCenterPt.x},${derivedCenterPt.y},${derivedCenterPt.cSys}` : '';

    const storeAngleDeg = formatAngle(convertAngle(angleUnit.key, 'deg', angle));

    const storeFpKey = `${storeAngleDeg}|${currentPt?.x},${currentPt?.y}`;

    const [angleDeg, setAngleDeg] = useState(storeAngleDeg);
    const [centerPt, setCenterPt] = useState(derivedCenterPt);
    const [lastCenterKey, setLastCenterKey] = useState(derivedCenterKey);
    const [lastStoreFpKey, setLastStoreFpKey] = useState(storeFpKey);
    const [fpText, setFpText] = useState(text);
    const [fpTextLoc, setFpTextLoc] = useState(textLoc);

    const isValidAngle = !isNaN(parseFloat(angleDeg));

    if (hasData) {
        if (derivedCenterKey && derivedCenterKey !== lastCenterKey) {
            setLastCenterKey(derivedCenterKey);
            setCenterPt(derivedCenterPt);
        }

        // these are local so an in-progress edit survives, but follow the store when it disagrees.
        // a local edit dispatches synchronously, so it already matches
        if (text !== fpText) setFpText(text);

        if (textLoc !== fpTextLoc) setFpTextLoc(textLoc);

        // storeFpKey changes when the footprint itself moves (drag or rotate) - on those, an uncommitted
        // (invalid) angle entry is discarded, since only a valid entry ever reached the store
        if (storeFpKey !== lastStoreFpKey) {
            setLastStoreFpKey(storeFpKey);
            if (!angleFromUI || !isValidAngle) {
                setAngleDeg(storeAngleDeg);
            }
        }
    }

    const changeFootprintText = (ev) => {
        const newText = ev?.target?.value ?? '';

        setFpText(newText);
        dispatchModifyCustomField(drawLayerId,
            {fpText: newText, fpTextLoc, activePlotId: plotId},
            plotId);
    };

    const changeFootprintTextLocation = (ev, newLocKey) => {
        const newLoc = TextLocation.get(newLocKey) ?? defaultFootprintTextLoc;

        setFpTextLoc(newLoc);
        dispatchModifyCustomField(drawLayerId,
            {fpText, fpTextLoc: newLoc, activePlotId: plotId},
            plotId);
    };

    const changeFootprintAngle = (ev) => {
        const newAngleDeg = ev?.target?.value ?? '';
        const valid = !isNaN(parseFloat(newAngleDeg));

        setAngleDeg(newAngleDeg);

        if (valid) {
            dispatchModifyCustomField(drawLayerId, {angleDeg: newAngleDeg, activePlotId: plotId}, plotId);
        }
    };

    const textOnLink = fpInfo?.fromFile ? `Add another ${fpInfo.fromFile}`
                     : fpInfo?.fromRegionAry
                                    ? `Add another ${drawLayer.title}`
                                    : `Add another ${fpInfo?.footprint}${fpInfo?.instrument ? ' '+fpInfo.instrument : ''}`;

    return (
        <Stack {...{py:1, spacing:1}}>
            <Stack {...{direction:'row', alignItems:'center', justifyContent:'flex-start', spacing:1, pl:1}}>
                <Typography level='body-sm'>Center:</Typography>
                {formatWorldPt(centerPt,3,false)}
                <FixedPtControl wp={centerPt} pv={pv}/>
                <InputFieldView
                            valid={isValidAngle}
                            orientation='horizontal'
                            slotProps={{input:{sx:{width:'7rem'}}}}
                            onChange={changeFootprintAngle}
                            value={angleDeg}
                            message='invalid angle value'
                            label='Angle:'
                            tooltip='Enter the angle in degree you want the footprint rotated'
                />
            </Stack>

            <Stack {...{direction:'row', alignItems:'center', justifyContent:'flex-start',
                        spacing:1, pl:1, flexWrap:'wrap', useFlexGap:true}}>
                <InputFieldView label='Label:' tooltip='Add a label to this footprint'
                                orientation='horizontal'
                                slotProps={{input:{sx:{width:'10em'}}}}
                                onChange={changeFootprintText} value={fpText}/>
                <ListBoxInputFieldView
                    onChange={changeFootprintTextLocation}
                    value={fpTextLoc.key}
                    label='Corner:' tooltip='Choose a corner'
                    options={[
                        {value: TextLocation.REGION_NE.key, label:'NE'},
                        {value: TextLocation.REGION_NW.key, label:'NW'},
                        {value: TextLocation.REGION_SE.key, label:'SE'},
                        {value: TextLocation.REGION_SW.key, label:'SW'},
                    ]}/>
                <Tooltip title='Add an additional footprint'>
                    <Chip onClick={()=>addFootprintDrawLayer(pv, fpInfo)}>
                        {textOnLink}
                    </Chip>
                </Tooltip>
            </Stack>
        </Stack>
    );
}


FootprintToolUI.propTypes= {
    drawLayer     : PropTypes.object.isRequired,
    pv            : PropTypes.object.isRequired
};

function formatAngle(angle) {
     const anglePre = parseFloat(`${sprintf(precision,angle)}`);
     return `${anglePre}`;
}
