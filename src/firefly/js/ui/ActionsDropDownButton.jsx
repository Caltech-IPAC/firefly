import {isArray} from 'lodash';
import React, {Fragment, useRef} from 'react';
import {getTblById} from '../tables/TableUtil.js';
import {getWorldPtFromTableRow} from '../util/VOAnalyzer.js';
import {SingleColumnMenu} from './DropDownMenu.jsx';
import {DropDownToolbarButton} from './DropDownToolbarButton.jsx';
import {useStoreConnector} from './SimpleComponent.jsx';
import {DropDownVerticalSeparator, ToolbarButton} from './ToolbarButton.jsx';
import {getSearchTypeDesc, getValidSize, SearchTypes} from '../core/ClickToAction.js';
import CysConverter from '../visualize/CsysConverter.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import {PlotAttribute} from '../visualize/PlotAttribute.js';
import {getActivePlotView, primePlot} from '../visualize/PlotViewUtil.js';
import {markOutline, showSearchRefinementTool} from '../visualize/SearchRefinementTool.jsx';
import {convertWpAryToStr, getDetailsFromSelection} from '../visualize/ui/VisualSearchUtils.js';
import {formatWorldPt} from '../visualize/ui/WorldPtFormat.jsx';

import BINOCULARS from 'images/b4.png';

export function ActionsDropDownButton({searchActions, pv, tbl_id}) {
    const buttonRef = useRef();
    const spacial= Boolean(pv);
    const {cenWpt} = spacial ? getDetailsFromSelection(primePlot(pv)) :
                                {cenWpt:getWorldPtFromTableRow(getTblById(tbl_id))};
    if (!cenWpt) return <div/>;
    const dropDown = <SearchDropDown {...{searchActions, buttonRef, spacial, tbl_id, key:'searchDropDown'}}/>;

    return (
        <div ref={buttonRef}>
            <DropDownToolbarButton icon={BINOCULARS}
                                   tip='Search this area'
                                   useDropDownIndicator={true}
                                   enabled={true} horizontal={true} visible={true} dropDown={dropDown}/>
        </div>
    );
}

const spacialTypes= [SearchTypes.point,SearchTypes.pointSide,SearchTypes.area,SearchTypes.pointRadius];
const tableTypes= [SearchTypes.point_table_only, SearchTypes.table];

function doExecute(sa,cenWpt,radius,cornerStr,table) {
    switch (sa.searchType) {
        case SearchTypes.area:
        case SearchTypes.pointSide:
        case SearchTypes.pointRadius:
            const valRadius= getValidSize(sa, radius);
            sa.execute(sa, cenWpt, valRadius, cornerStr);
            markOutline(sa, primePlot(visRoot())?.plotId,{ wp:cenWpt, radius:valRadius, polyStr:cornerStr});
            break;
        case SearchTypes.point:
        case SearchTypes.point_table_only:
            sa.execute(sa, cenWpt);
            break;
        case SearchTypes.table:
            sa.execute(sa, table);
    }
}

function isSupported(sa,cenWpt,radius,cornerStr,table) {
    switch (sa.searchType) {
        case SearchTypes.area:
        case SearchTypes.pointSide:
        case SearchTypes.pointRadius:
            return sa.supported(sa, cenWpt, getValidSize(sa, radius), cornerStr);
        case SearchTypes.point:
        case SearchTypes.point_table_only:
            return sa.supported(sa, cenWpt);
        case SearchTypes.table:
            return sa.supported(sa, table);
    }
}

function SearchDropDown({searchActions, buttonRef, spacial, tbl_id}) {

    const pv = useStoreConnector(() => spacial? getActivePlotView(visRoot()) : undefined);
    const table= tbl_id && getTblById(tbl_id);
    const {cenWpt, radius, corners} = spacial ?
        getDetailsFromSelection(primePlot(pv)) : {cenWpt:getWorldPtFromTableRow(table)};

    const cornerStr = isArray(corners) ? convertWpAryToStr(corners, primePlot(pv)) : undefined;
    const includeTypes= spacial ? spacialTypes : tableTypes;

    let lastGroupId;

    const supportedSearchActions= searchActions
        .filter(({searchType}) => includeTypes.includes(searchType))
        .filter((sa) => spacial ? sa.supported(pv,cenWpt, radius, corners) : sa.supported(table));

    const spacialSearchActions= supportedSearchActions.filter(({searchType}) => searchType!==SearchTypes.table);
    const tableNonSpacialSearchActions= supportedSearchActions.filter(({searchType}) => searchType===SearchTypes.table);



    return (
        <SingleColumnMenu key='searchMenu'>
            {Boolean(tableNonSpacialSearchActions.length) && tableNonSpacialSearchActions.map((sa) => {
                const text = getSearchTypeDesc(sa, cenWpt, radius, corners?.length);
                return (
                    <ToolbarButton text={text} tip={`${sa.tip} for\n${text}`}
                                   enabled={true} horizontal={false} key={sa.cmd}
                                   visible={isSupported(sa, cenWpt, radius, cornerStr, table)}
                                   onClick={() => doExecute(sa, cenWpt, radius, cornerStr, table)}/>
                );
            })}
            <div style={{whiteSpace: 'nowrap', fontSize: '10pt', padding: '5px 1px 5px 15px'}}>
                <span style={{fontStyle: 'italic'}}>
                    {'Actions based on center: '}
                </span>
                <span>
                    {formatWorldPt(cenWpt, 3)}
                </span>
            </div>
            {spacialSearchActions.map((sa,idx) => {
                const text = getSearchTypeDesc(sa, cenWpt, radius, corners?.length);
                let useSep= false;
                if (lastGroupId && sa.groupId!==lastGroupId) {
                    lastGroupId= sa.groupId;
                    useSep= true;
                }
                if (!lastGroupId) lastGroupId= sa.groupId;
                return (
                    <React.Fragment key={'frag='+idx}>
                        {useSep && <DropDownVerticalSeparator key={sa.cmd+'---separator'} useLine={true}/>}
                        <ToolbarButton text={text} tip={`${sa.tip} for\n${text}`}
                                       enabled={true} horizontal={false} key={sa.cmd}
                                       visible={isSupported(sa,cenWpt,radius,cornerStr,table) }
                                       onClick={() => doExecute(sa,cenWpt,radius,cornerStr,table) }/>
                    </React.Fragment>
                );
            })}
            {spacial &&
                <>
                    <DropDownVerticalSeparator useLine={true} key='refine---separator'/>
                    <ToolbarButton text='Refine search region' tip='Refine search region'
                                   enabled={true} horizontal={false} key='refine' visible={true}
                                   onClick={() =>
                                       showSearchRefinementTool({
                                           element: buttonRef.current, plotId: pv.plotId,
                                           searchActions:supportedSearchActions,
                                           wp: cenWpt, searchAreaInDeg: radius, polygonValue: corners}) }/>
                </>
            }
        </SingleColumnMenu>
    );

}

export function isSpacialActionsDropVisible(searchActions, pv) {
    if (!searchActions?.length) return false;
    if (!searchActions.some( ({searchType}) => spacialTypes.includes(searchType))) return false;
    const p = primePlot(pv);
    if (!p) return false;
    const sel = p.attributes[PlotAttribute.SELECTION];
    if (!sel) return false;
    const cc = CysConverter.make(p);
    if (!cc) return false;
    const wp0 = cc.getWorldCoords(sel.pt0);
    const wp1 = cc.getWorldCoords(sel.pt1);
    return Boolean(wp0 && wp1);
}

export function isTableActionsDropVisible(searchActions, tbl_id) {
    const table= getTblById(tbl_id);
    if (!searchActions.some( ({searchType}) => tableTypes.includes(searchType))) return false;
    const wp= getWorldPtFromTableRow(table);
    return Boolean(wp);
}
