import {Stack} from '@mui/joy';
import React, {useState} from 'react';
import {get} from 'lodash';
import {primePlot} from '../PlotViewUtil.js';
import {isImage} from '../WebPlot.js';
import {PlotAttribute} from '../PlotAttribute';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {DropDownVerticalSeparator, ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {getActivePlotView, getDrawLayersByType} from '../PlotViewUtil';
import {dispatchChangeTableAutoScroll, dispatchRecenter} from '../ImagePlotCntlr';
import {getTableGroup} from '../../tables/TableUtil';
import {TargetPanel} from '../../ui/TargetPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import CompleteButton from '../../ui/CompleteButton';
import {parseWorldPt, pointEquals} from '../Point';
import {dispatchAddPreference, getPreference} from '../../core/AppDataCntlr';
import {DropDownSubMenu} from '../../ui/DropDownMenu';
import FixedMarker from '../../drawingLayers/FixedMarker';
import {dispatchAttachLayerToPlot, dispatchCreateDrawLayer, getDlAry} from '../DrawLayerCntlr';
import {CenterDropdown} from './Buttons.jsx';
import {formatWorldPt, formatWorldPtToString, formatWorldPtToStringSimple} from './WorldPtFormat';


const MAX_TARGET_LEN= 10;
export const TARGET_LIST_PREF= 'RecentTargets';

function addToRecentTargets(wp, setRecentAry) {
    let tAry= getRecentTargets();
    if (tAry.find( (t) => pointEquals(t,wp))) {
        tAry= tAry.filter( (t) => !pointEquals(t,wp));
    }
    else {
        if (tAry.length > MAX_TARGET_LEN-1) tAry = tAry.slice(0, MAX_TARGET_LEN-1);
    }
    tAry.unshift(wp);
    const recent= tAry.map((t) => t.toString());
    dispatchAddPreference(TARGET_LIST_PREF, recent);
    setRecentAry(recent);
}


function getRecentTargets() {
    const strAry= getPreference(TARGET_LIST_PREF, []);
    return strAry.map( (s) => parseWorldPt(s));
}


function  hasTablesWithCoordinates(pv) {
    const group= getTableGroup();
    if (!group || !pv) return false;
    const tAry= Object.values(group.tables);
    const coordTable= tAry.find( (t) => t);
    return Boolean(pv && coordTable);
}


export function ImageCenterDropDown({visRoot:vr, visible, mi}) {

    const pv= getActivePlotView(vr);
    const plot= primePlot(pv);
    const coordTables= hasTablesWithCoordinates(pv);
    const hasSearchTarget= Boolean(
        (get(plot, ['attributes',PlotAttribute.FIXED_TARGET]) && pv.plotViewCtx.displayFixedTarget) ||
               get(plot, ['attributes',PlotAttribute.CENTER_ON_FIXED_TARGET]));
    let searchTarget;
    if (hasSearchTarget) {
        searchTarget=  plot.attributes[PlotAttribute.CENTER_ON_FIXED_TARGET] || plot.attributes[PlotAttribute.FIXED_TARGET];
    }
    const plotId= plot && plot.plotId;
    const [recentAry, setRecentAry] = useState(getRecentTargets());

    const moveToTarget= (fields) => {
        const wp=  parseWorldPt(get(fields,'UserTargetWorldPt'));
        if (!wp) return;
        dispatchRecenter({plotId, centerPt:wp});
        addToRecentTargets(wp, setRecentAry);
    };

    const createMarkerAndMoveToTarget= (fields) => {
        const wp=  parseWorldPt(get(fields,'UserTargetWorldPt'));
        if (!wp) return;
        const pv= getActivePlotView(vr);
        if (!pv) return;
        moveToTarget(fields);


        const fixedLayers= getDrawLayersByType(getDlAry(), FixedMarker.TYPE_ID);
        let newDL= fixedLayers.find( (dl) => pointEquals(dl.worldPt, wp));

        if (!newDL) {
            const title= formatWorldPt(wp);
            newDL= dispatchCreateDrawLayer(FixedMarker.TYPE_ID, {worldPt:wp, title});
        }
        dispatchAttachLayerToPlot(newDL.drawLayerId, pv.plotId, true);
    };

    const centerText= `Center on Target${searchTarget && searchTarget.objName ? ' - '+searchTarget.objName : ''}`;
    const centerTextTip= hasSearchTarget ? `Center on search target - ${formatWorldPtToStringSimple(searchTarget)}` :
                                          'Center on search target';

    const dropDown= (
        <SingleColumnMenu>

            <ToolbarButton text='Pan by table row' tip='Center selected images position of the highlighted row'
                           enabled={coordTables}
                           horizontal={false} key={'pan-table'}
                           hasCheckBox={true} checkBoxOn={vr.autoScrollToHighlightedTableRow}
                           visible={mi.panByTableRow}
                           onClick={() => dispatchChangeTableAutoScroll(!vr.autoScrollToHighlightedTableRow)}/>

            {mi.panByTableRow && <DropDownVerticalSeparator useLine={true}/>}


            <ToolbarButton text={centerText} tip={centerTextTip}
                           enabled={hasSearchTarget}
                           horizontal={false} key={'center-search-target'}
                           onClick={() => dispatchRecenter({plotId,centerPt:searchTarget})} />

            <ToolbarButton text='Center Image' tip='Center image in display frame'
                           enabled={Boolean(plot && isImage(plot))}
                           horizontal={false} key={'center-image'}
                           onClick={() => dispatchRecenter({plotId, centerOnImage:true})} />

            <DropDownVerticalSeparator useLine={true}/>
            <FieldGroup groupKey='TARGET_DROPDOWN'>
                <Stack direction='row' spacing={1}>
                    <TargetPanel groupKey={'target-move-group'}
                                 label={'<Enter position to center on>'} defaultToActiveTarget={false}
                                 showResolveSourceOp={false} showExample={false}/>
                    <Stack {...{alignItems:'stretch'}}>
                        <CompleteButton text= 'Go' onSuccess={moveToTarget} fireOnEnter={true}
                                        sx={{'& button': {width:1}}}/>
                        <CompleteButton style={{ marginTop:4, whiteSpace:'nowrap' }} innerStyle={{width:'100%'}}
                                        text= 'Go & Mark' onSuccess={createMarkerAndMoveToTarget} />
                    </Stack>
                </Stack>
            </FieldGroup>
            {recentAry.length>0 && <DropDownVerticalSeparator useLine={true}/>}
            <DropDownSubMenu text={'Recent Positions'} visible={recentAry.length>0}>
                {() =>
                    getRecentTargets().map( (t) => <ToolbarButton text={formatWorldPt(t)} tip={formatWorldPtToString(t)}
                                                                  style={{width:'100%'}}
                                                                  enabled={Boolean(plot)} key={t.toString()}
                                                                  onClick={() => dispatchRecenter({plotId:pv.plotId, centerPt:t})} />)
                }
            </DropDownSubMenu>

        </SingleColumnMenu>
    );

    return (
        <CenterDropdown tip='Image center drop down: center images'
                               enabled={Boolean(plot)} visible={visible}
                               dropDown={dropDown}/>

    );

}
