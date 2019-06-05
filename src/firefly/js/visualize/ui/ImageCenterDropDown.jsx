import React, {useState, Fragment} from 'react';
import {get} from 'lodash';
import {primePlot} from '../PlotViewUtil.js';
import {isImage, PlotAttribute} from '../WebPlot.js';
import {SingleColumnMenu} from '../../ui/DropDownMenu.jsx';
import {DropDownVerticalSeparator, ToolbarButton} from '../../ui/ToolbarButton.jsx';
import {DropDownToolbarButton} from '../../ui/DropDownToolbarButton.jsx';

import CENTER_DROP from 'html/images/center-dropdown.png';
import {getActivePlotView} from '../PlotViewUtil';
import {dispatchChangeTableAutoScroll, dispatchRecenter} from '../ImagePlotCntlr';
import {getTableGroup} from '../../tables/TableUtil';
import {TargetPanel} from '../../ui/TargetPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import CompleteButton from '../../ui/CompleteButton';
import {parseWorldPt, pointEquals} from '../Point';
import {dispatchAddPreference, getPreference} from '../../core/AppDataCntlr';
import {coordToString} from '../../data/form/PositionFieldDef';
import {DropDownSubMenu} from '../../ui/DropDownMenu';


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

function prettyTarget(wp) {
    if (wp.objName) {
        if (wp.resolver) {
            return (
                <Fragment>
                    <span style={{fontWeight:'bold'}}>{wp.objName}</span>
                    <span style={{fontSize: '80%' }}>
                        <span style={{fontStyle: 'italic'}}> by </span>
                        {`${wp.resolver.toString().toUpperCase()}`}
                    </span>
                </Fragment>
            );
        }
        else {
            return ( <span style={{fontWeight:'bold'}}>{wp.objName}</span> );
        }
    }
    else {
        return ( <span style={{fontWeight:'bold'}}>{`${wp.x}, ${wp.y} ${coordToString(wp.cSys)}`}</span> );
    }
}


export function ImageCenterDropDown({visRoot:vr, visible}) {

    const pv= getActivePlotView(vr);
    const plot= primePlot(pv);
    const coordTables= hasTablesWithCoordinates(pv);
    const hasSearchTarget= Boolean(get(plot, ['attributes',PlotAttribute.FIXED_TARGET]));
    const plotId= plot && plot.plotId;
    const [recentAry, setRecentAry] = useState(getRecentTargets());

    const moveToTarget= (fields) => {
        const wp=  parseWorldPt(get(fields,'UserTargetWorldPt'));
        if (!wp) return;
        dispatchRecenter({plotId, centerPt:wp});
        addToRecentTargets(wp, setRecentAry);
    };
    //   //todo code to call a future marker option
    // const createMarkerAndMoveToTarget= (fields) => {
    //     const wp=  parseWorldPt(get(fields,'UserTargetWorldPt'));
    //     if (!wp) return;
    //     moveToTarget(fields);
    //                              todo: the section below might need to be deferred to the plot has moved to the target first
    //     const cc = CysConverter.make(plot);
    //     if (cc.pointInPlot(wp)) {
    //         const title= prettyTarget(wp);
    //         const drawLayerId = title;
    //         dispatchCreateMarkerLayer(drawLayerId, title, plotId, true, wp, true);
    //     }
    // };
    //

    const dropDown= (
        <SingleColumnMenu>

            <ToolbarButton text='Pan by table row' tip='Center selected images position of the highlighted row'
                           enabled={coordTables}
                           horizontal={false} key={'pan-table'}
                           hasCheckBox={true} checkBoxOn={vr.autoScrollToHighlightedTableRow}
                           onClick={() => dispatchChangeTableAutoScroll(!vr.autoScrollToHighlightedTableRow)}/>

            <DropDownVerticalSeparator useLine={true}/>


            <ToolbarButton text='Center on Target' tip='Center on search target'
                           enabled={hasSearchTarget}
                           horizontal={false} key={'center-search-target'}
                           onClick={() => dispatchRecenter({plotId})} />

            <ToolbarButton text='Center on Image' tip='Center on the image'
                           enabled={Boolean(plot && isImage(plot))}
                           horizontal={false} key={'center-image'}
                           onClick={() => dispatchRecenter({plotId, centerOnImage:true})} />

            <DropDownVerticalSeparator useLine={true}/>
            <FieldGroup style={{display:'flex', fontSize:'10pt', padding:'5px 0 0 0'}}
                        groupKey='TARGET_DROPDOWN' validatorFunc={null} keepState={true}>
                <TargetPanel groupKey={'target-move-group'} labelWidth={80}
                             label={'Center On:'}
                             showResolveSourceOp={false} showExample={false}/>
                <div style={{display:'flex', flexDirection:'column', alignItems: 'flex-end'}}>
                    <CompleteButton style={{paddingLeft:10}} text= 'Go' onSuccess={moveToTarget} fireOnEnter={true}
                                    dialogId='ExampleDialog' />
                    {/*                //todo- button to add marker*/}
                    {/*<CompleteButton style={{paddingLeft:10, marginTop:4}} text= 'Marker' onSuccess={createMarkerAndMoveToTarget}*/}
                    {/*                dialogId='ExampleDialog' />*/}
                </div>
            </FieldGroup>
            {recentAry.length>0 && <DropDownVerticalSeparator useLine={true}/>}
            <DropDownSubMenu text={'Recent Positions'} visible={recentAry.length>0}>
                {() =>
                    getRecentTargets().map( (t) => <ToolbarButton text={prettyTarget(t)} tip='recent position'
                                                                  additionalStyle={{width:'100%'}}
                                                                  enabled={Boolean(plot)} horizontal={false} key={t.toString()}
                                                                  onClick={() => dispatchRecenter({plotId:pv.plotId, centerPt:t})} />)
                }
            </DropDownSubMenu>

        </SingleColumnMenu>
    );

    return (
        <DropDownToolbarButton icon={CENTER_DROP}
                               tip='Center images'
                               enabled={Boolean(plot)} horizontal={true} visible={visible}
                               hasHorizontalLayoutSep={false}
                               useDropDownIndicator={true}
                               dropDown={dropDown}/>

    );

}
