/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Button, ChipDelete, Stack, Typography} from '@mui/joy';
import React from 'react';
import PropTypes, {object, string} from 'prop-types';
import {once} from 'lodash';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {checkProps, useStoreConnector} from '../../ui/SimpleComponent';
import {ToolbarButton, DropDownVerticalSeparator} from '../../ui/ToolbarButton.jsx';
import {
    getActivePlotView, getDrawLayerByType, getDrawLayersByType, getPlotViewAry, isDrawLayerAttached
} from '../PlotViewUtil.js';
import {dispatchCreateDrawLayer,
        getDlAry,
        dispatchAttachLayerToPlot,
        dispatchDetachLayerFromPlot} from '../DrawLayerCntlr.js';
import SelectArea from '../../drawingLayers/SelectArea.js';
import ImageOutline from '../../drawingLayers/ImageOutline.js';
import {SelectedShape} from '../../drawingLayers/SelectedShape';
import {visRoot} from '../ImagePlotCntlr.js';
import {onOff, SimpleLayerOnOffButton} from 'firefly/visualize/ui/SimpleLayerOnOffButton.jsx';

import AdsClickIcon from '@mui/icons-material/AdsClick';

import SELECT_RECT from 'html/images/icons-2014/Marquee.png';
import SELECT_RECT_ON from 'html/images/icons-2014/Marquee-ON.png';
import SELECT_CIRCLE from 'html/images/icons-2014/28x28_Circle.png';
import SELECT_CIRCLE_ON from 'html/images/icons-2014/28x28_Circle-ON.png';
import SELECT_NONE from 'html/images/icons-2014/28x28_Rect_DD.png';
import {clearModalEndInfo, getModalEndInfo, setModalEndInfo} from './ToolbarToolModalEnd.js';

const NONSELECT = 'nonselect';

export const DEFAULT_SELECTION_TEXT='Draw Area Selection';
export const DEFAULT_INSTRUCTIONS_CIRCLE='Choose search area: click and draw circle on map';
export const DEFAULT_INSTRUCTIONS_RECT= 'Choose search area: click and draw rectangle on map';



const selectAreaInfo = once(() => ({
    [NONSELECT] : {
        typeId: '',
        iconId: SELECT_NONE,
        label: 'None',
        tip: 'turn off area selection'
    },

    [SelectedShape.rect.key] : {
        typeId: SelectArea.TYPE_ID,
        iconId: SELECT_RECT_ON,
        iconDropDown: SELECT_RECT,
        label: 'Rectangular Selection',
        tip: 'select rectangular area',
        params: {imageRoration: 0.0}
    },
    [SelectedShape.circle.key] : {
        typeId: SelectArea.TYPE_ID,
        iconId:  SELECT_CIRCLE_ON,
        iconDropDown:  SELECT_CIRCLE,
        label: 'Cone Selection',
        tip: 'select cone area',
        params: {selectedShape: SelectedShape.circle.key, handleColor: 'white'}
    }
}));

function getSelectedAreaIcon(isSelected = true) {

    if (!isSelected) return SELECT_CIRCLE;
    const drawLayer = getDrawLayerByType(getDlAry(), SelectArea.TYPE_ID);
    return  (drawLayer && drawLayer.selectedShape) ?
             selectAreaInfo()[drawLayer.selectedShape].iconId : SELECT_RECT;

}


function updateSelect(pv, value, allPlots=true, modalEndInfo) {

    return ()=> {
        if (!pv) return;


        if (value !== NONSELECT) {
            modalEndInfo?.closeLayer?.(SelectArea.TYPE_ID);
            detachSelectAreaRelatedLayers( pv, allPlots, selectAreaInfo()[value].typeId);
            detachSelectArea(pv);
            // create a new one
            const dl = dispatchCreateDrawLayer(selectAreaInfo()[value].typeId, selectAreaInfo()[value].params);

            // attach plot to the new one
            if (!isDrawLayerAttached(dl, pv.plotId)) {
               dispatchAttachLayerToPlot(dl.drawLayerId, pv.plotId, allPlots);
            }
            setModalEndInfo({
                closeText:'End Select',
                closeLayer: () => onOff(pv,SelectArea.TYPE_ID,allPlots,false,false,modalEndInfo,'',true),
                offOnNewPlot: true,
                key: 'SelectArea'
            });
        }
    };
}

export const SELECT_AREA_TITLE = 'Image outline on select area';

export function isOutlineImageForSelectArea(dl) {
    if (!dl.title)  return false;

    return (typeof dl.title === 'string') ? dl.title.includes(SELECT_AREA_TITLE)
                                          : Object.values(dl.title).find((v) => v.includes(SELECT_AREA_TITLE));
}


export function detachSelectArea(pv, allPlots = true, id = SelectArea.TYPE_ID) {
    if (id) clearModalEndInfo();
    const dlAry = getDrawLayersByType(getDlAry(), id);

    dlAry.forEach((dl) => {
        dispatchDetachLayerFromPlot(dl.drawLayerId, getPlotViewAry(visRoot()).map( (pv) => pv.plotId), allPlots, dl.destroyWhenAllDetached);
    });
}

function detachImageOutlineLayerForSelectArea(pv, allPlots = true) {
    const dlAry = getDrawLayersByType(getDlAry(), ImageOutline.TYPE_ID);

    dlAry.forEach((dl) => {
        if (isOutlineImageForSelectArea(dl) && isDrawLayerAttached(dl, pv.plotId)) {
            dispatchDetachLayerFromPlot(dl.drawLayerId, pv.plotId, allPlots, dl.destroyWhenAllDetached);
        }
    });
}

export function detachSelectAreaRelatedLayers(pv, allPlots = true, selectId = SelectArea.TYPE_ID) {
    detachSelectArea(pv, allPlots, selectId);
    detachImageOutlineLayerForSelectArea(pv, allPlots);
}

const image24x24={width:24, height:24};

export const SelectAreaButton= ({pv:plotView,tip,visible=true,modalEndInfo,imageStyle= image24x24, style, text, color, variant}) => (
        <SimpleLayerOnOffButton {...{plotView, typeId:SelectArea.TYPE_ID, style, text, color, variant,
            tip, iconOn:getSelectedAreaIcon(), iconOff:getSelectedAreaIcon(false),
            useDropDownIndicator: modalEndInfo?.key!=='SelectArea',
            dropPosition:{left:3,bottom:-2},
            visible, imageStyle, modalEndInfo, modalLayer:true,
            dropDown: <SelectAreaDropDownView {...{plotView, modalEndInfo}} />}} />
    );



export function SelectAreaDropDownView({plotView:pv, allPlots, modalEndInfo}) {
    const enabled = !!pv;

    const selectAreaCommands = () => {

        return [SelectedShape.circle, SelectedShape.rect].reduce((prev, s,idx) => {
            const key = s.key;
            prev.push((
                <ToolbarButton key={key}
                               text={selectAreaInfo()[key].label}
                               enabled={enabled}
                               horizontal={false}
                               icon={selectAreaInfo()[key].iconDropDown }
                               tip={selectAreaInfo()[key].tip}
                               onClick={updateSelect(pv, key, allPlots, modalEndInfo)}/>
            ));
            prev.push(<DropDownVerticalSeparator key={idx+1}/>);
            return prev;
        }, []);
    };


    return (
        <SingleColumnMenu>
            {selectAreaCommands()}
        </SingleColumnMenu>
    );
}

SelectAreaDropDownView.propTypes= {
    plotView : PropTypes.object,
    allPlots: PropTypes.bool,
    modalEndInfo: PropTypes.object,
};

function getInstructions(shape, instructionsText) {
    if (instructionsText) return instructionsText;
    switch (shape) {
        case SelectedShape.rect: return DEFAULT_INSTRUCTIONS_RECT;
        case SelectedShape.circle: return DEFAULT_INSTRUCTIONS_CIRCLE;
        default : return 'click and draw';
    }

}


export function SelectAreaForEmbedded(props) {
    const {selectButtonText=DEFAULT_SELECTION_TEXT,instructionsText,shape=SelectedShape.circle, sx}= props;
    checkProps(props, SelectAreaForEmbedded);
    const modalEndInfo = useStoreConnector(() => getModalEndInfo());
    const selectActive= modalEndInfo?.key==='SelectArea';
    const doSelection= updateSelect(getActivePlotView(visRoot()), shape.key, true, modalEndInfo);

    return (
        <Stack sx={{justifyContent:'center', ...sx}}>
            <Stack alignItems='center'>
                {!selectActive
                    ? <Button {...{ size: 'sm', title: 'Select area',
                        startDecorator:<AdsClickIcon/>,
                        onClick: () => doSelection() }}>
                        {selectButtonText}
                    </Button>
                    : <Stack {...{direction:'row', sx:{alignItems: 'center'} }}>
                        <Typography level='body-xs'>{getInstructions(shape,instructionsText)}</Typography>
                        <ChipDelete {...{
                            size:'sm', title:`Cancel ${selectButtonText}`,
                            onClick:() => {
                                const pv = getActivePlotView(visRoot());
                                if (pv) dispatchDetachLayerFromPlot(SelectArea.TYPE_ID, pv.plotId, true, true);
                                clearModalEndInfo();
                            }
                        }}/>
                    </Stack>
                }
            </Stack>
        </Stack>
    );
}

SelectAreaForEmbedded.propTypes = {
    selectButtonText: string,
    instructionsText: string,
    shape: object,
    sx: object,
};