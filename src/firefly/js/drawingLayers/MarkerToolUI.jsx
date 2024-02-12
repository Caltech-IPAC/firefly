/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {Button, Chip, Stack, Tooltip} from '@mui/joy';
import PropTypes from 'prop-types';
import {InputField} from '../ui/InputField.jsx';
import {ListBoxInputFieldView} from '../ui/ListBoxInputField.jsx';
import {useStoreConnector} from '../ui/SimpleComponent.jsx';
import {TextLocation} from '../visualize/draw/DrawingDef.js';
import {flux} from '../core/ReduxFlux.js';
import {dispatchModifyCustomField, DRAWING_LAYER_KEY} from '../visualize/DrawLayerCntlr.js';
import {getDrawLayerById} from '../visualize/PlotViewUtil.js';
import {addNewDrawLayer} from '../visualize/ui/MarkerDropDownView.jsx';

export const defaultMarkerTextLoc = TextLocation.REGION_SE;

export function MarkerToolUI({pv,drawLayer}) {
    const markerType = drawLayer?.markerType ?? 'marker';

    const {markerText, markerTextLoc}=  useStoreConnector(() => {
        const dl = getDrawLayerById(flux.getState()[DRAWING_LAYER_KEY], drawLayer.drawLayerId);
        const crtMarkerObj = dl?.drawData?.data?.[pv.plotId];
        if (!crtMarkerObj ) return {markerText: 'marker',  markerTextLoc: defaultMarkerTextLoc.key, markerType};
        const {text = '', textLoc = defaultMarkerTextLoc} = crtMarkerObj;
        return {markerText: text, markerTextLoc:textLoc.key};
    });

    const changeMarkerText= (ev) => {
        let markerText = ev?.value ?? '';

        if (!markerText) {
            const dl = getDrawLayerById(flux.getState()[DRAWING_LAYER_KEY], drawLayer.drawLayerId);
            markerText = '';
            drawLayer.title = dl?.defaultTitle;
        } else {
            drawLayer.title = markerText;
        }
        dispatchModifyCustomField( drawLayer.drawLayerId,
            {markerText, markerTextLoc: TextLocation.get(markerTextLoc), activePlotId: pv.plotId},
            pv.plotId);
    };

    const changeMarkerTextLocation= (ev,newValue) => {
        const markerTextLoc = newValue;
        dispatchModifyCustomField( drawLayer.drawLayerId,
            {markerText, markerTextLoc: TextLocation.get(markerTextLoc), activePlotId: pv.plotId},
            pv.plotId);
    };

    return (
        <Stack direction='row' alignItems='center' spacing={2}>
            <InputField label='Label:' orientation='horizontal'
                        value={markerText}
                        tooltip='Change the label on this marker'
                        sx={{width:'10em'}} onChange={changeMarkerText} />
            <ListBoxInputFieldView
                onChange={changeMarkerTextLocation }
                value={markerTextLoc}
                label='Corner:' tooltip='shows the marker on the choosen corner'
                options={[
                    {value: TextLocation.REGION_NE.key, label:'NE'},
                    {value: TextLocation.REGION_NW.key, label:'NW'},
                    {value: TextLocation.REGION_SE.key, label:'SE'},
                    {value: TextLocation.REGION_SW.key, label:'SW'},
                ]}/>
            <Tooltip title='Add an additional marker'>
                <Chip onClick={()=>addNewDrawLayer(pv, markerType)}>
                    {`Add ${markerType}`}
                </Chip>
            </Tooltip>
        </Stack>
    );
}


MarkerToolUI.propTypes= {
    drawLayer     : PropTypes.object.isRequired,
    pv            : PropTypes.object.isRequired
};