/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {ToolbarButton,
        DropDownVerticalSeparator} from '../../ui/ToolbarButton.jsx';
import {getDrawLayerByType, isDrawLayerAttached } from '../PlotViewUtil.js';
import {dispatchCreateDrawLayer,
        getDlAry,
        dispatchAttachLayerToPlot,
        dispatchDetachLayerFromPlot} from '../DrawLayerCntlr.js';
import SelectArea, {SelectedShape} from '../../drawingLayers/SelectArea.js';

import SELECTRECT_DD from 'html/images/icons-2014/28x28_Rect_DD.png';
import SELECTCIRCLE_DD from 'html/images/icons-2014/28x28_Circle-ON_DD.png';
import SELECT_NONE_DD from 'html/images/icons-2014/28x28_NoSelect_DD.png';
import SELECT_RECT from 'html/images/icons-2014/marquee.png';
import SELECT_CIRCLE from 'html/images/icons-2014/28x28_Circle.png';
import SELECT_NONE from 'html/images/icons-2014/28x28_NoSelect.png';

const NONSELECT = 'nonselect';

export const selectAreaInfo = {
    [NONSELECT] : {
        typeId: '',
        iconId: SELECT_NONE,
        iconIdSelect: SELECT_NONE_DD,
        label: 'None',
        tip: 'turn off area selection'
    },

    [SelectedShape.rect.key] : {
        typeId: SelectArea.TYPE_ID,
        iconId: SELECT_RECT,
        iconIdSelect: SELECTRECT_DD,
        label: 'Rectangular Selection',
        tip: 'select rectangular area'
    },
    [SelectedShape.circle.key] : {
        typeId: SelectArea.TYPE_ID,
        iconId:  SELECT_CIRCLE,
        iconIdSelect: SELECTCIRCLE_DD,
        label: 'Elliptical Selection',
        tip: 'select elliptical area',
        params: {selectedShape: SelectedShape.circle.key, handleColor: 'white'}
    }
};

export function getSelectedAreaIcon(isSelected = true) {

    if (!isSelected) return SELECT_NONE_DD;
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
            let dl = getDrawLayerByType(getDlAry(), selectAreaInfo[value].typeId);

            if (!dl) {
                dl = dispatchCreateDrawLayer(selectAreaInfo[value].typeId, selectAreaInfo[value].params);
            }

            if (!isDrawLayerAttached(dl, pv.plotId)) {
               dispatchAttachLayerToPlot(selectAreaInfo[value].typeId, pv.plotId, allPlots);
            }
        }
    };
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
                               icon={selectAreaInfo[key].iconId }
                               tip={selectAreaInfo[key].tip}
                               onClick={updateSelect(pv, ddCB, key, crtSelection, allPlots)}/>
            ));
            prev.push(<DropDownVerticalSeparator key={sep++}/>);
            return prev;
        }, []);
        /*
        return [selectAreas.rect, selectAreas.circle, selectAreas.noselect].map((s) => {
            const key = s.key;
            const retv = (
                <ToolbarButton key={key}
                               text={selectAreaInfo[key].label}
                               enabled={enabled}
                               horizontal={false}
                               icon={selectAreaInfo[key].iconId }
                               tip={selectAreaInfo[key].tip}
                               onClick={updateSelect(pv, ddCB, key, crtSelection, allPlots)}/>
                <DropDownVerticalSeparator key={sep++}/>)
            )
            return prev;
        }, []);
        */
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
