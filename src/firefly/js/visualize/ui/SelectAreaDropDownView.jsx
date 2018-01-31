/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {ToolbarButton,
        DropDownVerticalSeparator} from '../../ui/ToolbarButton.jsx';
import {getDrawLayerByType, getDrawLayersByType, isDrawLayerAttached } from '../PlotViewUtil.js';
import {dispatchCreateDrawLayer,
        getDlAry,
        dispatchAttachLayerToPlot,
        dispatchDetachLayerFromPlot,
        dispatchDestroyDrawLayer} from '../DrawLayerCntlr.js';
import SelectArea, {SelectedShape} from '../../drawingLayers/SelectArea.js';
import ImageOutline from '../../drawingLayers/ImageOutline.js';

import SELECT_RECT from 'html/images/icons-2014/Marquee.png';
import SELECT_RECT_ON from 'html/images/icons-2014/Marquee-ON.png';
import SELECT_CIRCLE from 'html/images/icons-2014/28x28_Circle.png';
import SELECT_CIRCLE_ON from 'html/images/icons-2014/28x28_Circle-ON.png';
import SELECT_NONE from 'html/images/icons-2014/28x28_Rect_DD.png';

const NONSELECT = 'nonselect';

export const selectAreaInfo = {
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
        label: 'Elliptical Selection',
        tip: 'select elliptical area',
        params: {selectedShape: SelectedShape.circle.key, handleColor: 'white'}
    }
};

export function getSelectedAreaIcon(isSelected = true) {

    if (!isSelected) return SELECT_NONE;
    const drawLayer = getDrawLayerByType(getDlAry(), SelectArea.TYPE_ID);
    return  (drawLayer && drawLayer.selectedShape) ?
             selectAreaInfo[drawLayer.selectedShape].iconId : SELECT_RECT;

}


function updateSelect(pv, ddCB, value, preValue, allPlots=true) {
    const detatchPreSelectArea = (areaToDetach) => {
        const dl = getDrawLayerByType(getDlAry(), selectAreaInfo[areaToDetach].typeId);

        if (dl) {
            if (isDrawLayerAttached(dl, pv.plotId)) {
                dispatchDetachLayerFromPlot(selectAreaInfo[areaToDetach].typeId, pv.plotId, allPlots, dl.destroyWhenAllDetached);
            }
        }
    };

    return ()=> {
        if (ddCB && Object.keys(selectAreaInfo).includes(value)) {
            ddCB(value);
        }

        if (!pv) return;

        if (preValue && preValue !== NONSELECT) {
            detatchPreSelectArea(preValue);
        }

        if (value !== NONSELECT && (!preValue || value !== preValue)) {
            destroySelectAreaRelatedLayers( selectAreaInfo[value].typeId);
            // create a new one
            const dl = dispatchCreateDrawLayer(selectAreaInfo[value].typeId, selectAreaInfo[value].params);

            // attach plot to the new one
            if (!isDrawLayerAttached(dl, pv.plotId)) {
               dispatchAttachLayerToPlot(dl.drawLayerId, pv.plotId, allPlots);
            }
        }
    };
}

export const SELECT_AREA_TITLE = 'Image outline on select area';

export function isOutlineImageForSelectArea(dl) {
    if (!dl.title)  return false;

    return (typeof dl.title === 'string') ? dl.title.includes(SELECT_AREA_TITLE)
                                          : Object.values(dl.title).find((v) => v.includes(SELECT_AREA_TITLE));
}

export function destroySelectAreaRelatedLayers(selectAreaId = SelectArea.TYPE_ID) {
    const dl = getDrawLayerByType(getDlAry(), selectAreaId);

    if (dl) {
        dispatchDestroyDrawLayer(dl.drawLayerId);  // remove existing one
    }

    destroyImageOutlineLayerForSelectArea();
}

function destroyImageOutlineLayerForSelectArea() {
    const dlAry = getDrawLayersByType(getDlAry(), ImageOutline.TYPE_ID);

    dlAry.forEach((dl) => {
        if (isOutlineImageForSelectArea(dl)) {
            dispatchDestroyDrawLayer(dl.drawLayerId);
        }
    });
}


export function SelectAreaDropDownView({plotView:pv, allPlots, dropDownCB, crtSelection}) {
    var enabled = !!pv;
    let sep = 1;

    const ddCB = dropDownCB ? dropDownCB : null;

    const selectAreaCommands = () => {

        return [SelectedShape.rect, SelectedShape.circle].reduce((prev, s) => {
            const key = s.key;
            prev.push((
                <ToolbarButton key={key}
                               text={selectAreaInfo[key].label}
                               enabled={enabled}
                               horizontal={false}
                               icon={selectAreaInfo[key].iconDropDown }
                               tip={selectAreaInfo[key].tip}
                               onClick={updateSelect(pv, ddCB, key, crtSelection, allPlots)}/>
            ));
            prev.push(<DropDownVerticalSeparator key={sep++}/>);
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
    dropDownCB: PropTypes.func,
    crtSelection: PropTypes.string
};
