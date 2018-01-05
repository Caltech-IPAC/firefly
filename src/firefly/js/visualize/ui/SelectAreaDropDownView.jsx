/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import Enum from 'enum';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {ToolbarButton,
        DropDownVerticalSeparator} from '../../ui/ToolbarButton.jsx';
import {getDrawLayerByType, isDrawLayerAttached } from '../PlotViewUtil.js';
import {dispatchCreateDrawLayer,
        getDlAry,
        dispatchAttachLayerToPlot,
        dispatchDetachLayerFromPlot} from '../DrawLayerCntlr.js';
import SelectArea from '../../drawingLayers/SelectArea.js';

import SELECTRECT_DD from 'html/images/icons-2014/28x28_Rect_DD.png';
import SELECTCIRCLE_DD from 'html/images/icons-2014/28x28_Circle-ON_DD.png';
import SELECT_NONE_DD from 'html/images/icons-2014/28x28_NoSelect_DD.png';
import SELECT_RECT from 'html/images/icons-2014/marquee.png';
import SELECT_CIRCLE from 'html/images/icons-2014/28x28_Circle.png';
import SELECT_NONE from 'html/images/icons-2014/28x28_NoSelect.png';

export const selectAreas = new Enum([ 'rect', 'circle', 'noselect']);
export const selectAreaInfo = {
    [selectAreas.noselect.key] : {
        typeId: '',
        iconId: SELECT_NONE,
        iconIdSelect: SELECT_NONE_DD,
        label: 'None',
        tip: 'turn off area selection'
    },
    [selectAreas.rect.key] : {
        typeId: SelectArea.TYPE_ID,
        iconId: SELECT_RECT,
        iconIdSelect: SELECTRECT_DD,
        label: 'Rectangular Selection',
        tip: 'select rectangular area'
    },
    [selectAreas.circle.key] : {
        typeId: SelectArea.TYPE_ID,
        iconId:  SELECT_CIRCLE,
        iconIdSelect: SELECTCIRCLE_DD,
        label: 'Elliptical Selection',
        tip: 'select elliptical area',
        params: {selectedShape: selectAreas.circle.key, handleColor: 'white'}
    }
};

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


        if (value !== selectAreas.noselect.key && value !== preValue) {
            if (preValue !== selectAreas.noselect.key) {
                detatchPreSelectArea(preValue);
            }
            let dl = getDrawLayerByType(getDlAry(), selectAreaInfo[value].typeId);

            if (!dl) {
                dl = dispatchCreateDrawLayer(selectAreaInfo[value].typeId, selectAreaInfo[value].params);
            }

            if (!isDrawLayerAttached(dl, pv.plotId)) {
               dispatchAttachLayerToPlot(selectAreaInfo[value].typeId, pv.plotId, allPlots);
            }
        }
        if (value === selectAreas.noselect.key && value !== preValue) {
            detatchPreSelectArea(preValue);
        }
    };
}

export function SelectAreaDropDownView({plotView:pv, allPlots, dropDownCB, crtSelection}) {
    var enabled = !!pv;
    let sep = 1;

    const ddCB = dropDownCB ? dropDownCB : null;

    const selectAreaCommands = () => {
        return [selectAreas.rect, selectAreas.circle, selectAreas.noselect].reduce((prev, s) => {
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
    crtSelection: PropTypes.string.isRequired

};
