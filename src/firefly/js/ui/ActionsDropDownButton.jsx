import {Typography} from '@mui/joy';
import {isArray} from 'lodash';
import React, {Fragment, useRef} from 'react';
import {getTblById} from '../tables/TableUtil.js';
import {SearchDetailButton} from '../visualize/ui/Buttons.jsx';
import {getWorldPtFromTableRow} from '../voAnalyzer/TableAnalysis.js';
import {getDefMenuItemKeys} from '../visualize/MenuItemKeys.js';
import {SingleColumnMenu} from './DropDownMenu.jsx';
import {useStoreConnector} from './SimpleComponent.jsx';
import {DropDownVerticalSeparator, ToolbarButton} from './ToolbarButton.jsx';
import {getSearchTypeDesc, getValidSize, SearchTypes} from '../core/ClickToAction.js';
import CysConverter from '../visualize/CsysConverter.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import {PlotAttribute} from '../visualize/PlotAttribute.js';
import {getActivePlotView, primePlot} from '../visualize/PlotViewUtil.js';
import {showSearchRefinementTool} from '../visualize/SearchRefinementTool.jsx';
import {convertWpAryToStr, getDetailsFromSelection, markOutline} from '../visualize/ui/VisualSearchUtils.js';
import {formatWorldPt} from '../visualize/ui/WorldPtFormat.jsx';

export function ActionsDropDownButton({searchActions, pv, tbl_id, style, tip='Search this area'}) {
    const mi= pv?.plotViewCtx.menuItemKeys ?? getDefMenuItemKeys();
    const buttonRef = useRef();
    const spacial= Boolean(pv);
    const {cenWpt} = spacial ? getDetailsFromSelection(primePlot(pv)) :
                                {cenWpt:getWorldPtFromTableRow(getTblById(tbl_id))};
    if (!cenWpt || !mi?.clickToSearch) return <div/>;
    const dropDown = <SearchDropDown {...{searchActions, buttonRef, spacial, tbl_id, key:'searchDropDown'}}/>;

    return (
        <div ref={buttonRef} style={style}>
            <SearchDetailButton tip={tip} dropDown={dropDown}/>
        </div>
    );
}

const spacialTypes= [SearchTypes.point,SearchTypes.pointSide,SearchTypes.area,SearchTypes.pointRadius];
const tableTypes= [SearchTypes.point_table_only, SearchTypes.wholeTable];

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
        case SearchTypes.wholeTable:
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
        case SearchTypes.wholeTable:
            return sa.supported(sa, table);
    }
}

function SearchDropDown({searchActions, buttonRef, spacial, tbl_id}) {

    const pv = useStoreConnector(() => spacial? getActivePlotView(visRoot()) : undefined);
    const plot= primePlot(pv);
    const table= tbl_id && getTblById(tbl_id);
    const {cenWpt, radius, corners, cone} = spacial ?
        getDetailsFromSelection(plot) : {cenWpt:getWorldPtFromTableRow(table), cone:true};

    const cornerStr = isArray(corners) ? convertWpAryToStr(corners, primePlot(pv)) : undefined;
    const includeTypes= spacial ? spacialTypes : tableTypes;

    let lastGroupId;

    const supportedSearchActions= searchActions
        .filter(({searchType}) => includeTypes.includes(searchType))
        .filter((sa) => spacial ? sa.supported(pv,cenWpt, radius, corners) : sa.supported(table));

    const wholeTableSearchActions= supportedSearchActions.filter(({searchType}) => searchType===SearchTypes.wholeTable);

    const coneSearchActions= supportedSearchActions
        .filter(({searchType}) => searchType===SearchTypes.point ||  searchType===SearchTypes.pointRadius ||  searchType===SearchTypes.point_table_only );
    const areaSearchActions= supportedSearchActions
        .filter(({searchType}) => searchType===SearchTypes.area ||  searchType===SearchTypes.pointSide );

    const saOrder= (cone ? [coneSearchActions, areaSearchActions] : [areaSearchActions, coneSearchActions])
        .filter( (list) => list.length);

    const makePartialList= (sActions, cenWpt, asCone) => {

        const buttons= sActions.map((sa,idx) => {
            const text = getSearchTypeDesc(sa, cenWpt, radius, corners?.length);
            let useSep = false;
            if (lastGroupId && sa.groupId !== lastGroupId && idx!==sActions.length-1) {
                lastGroupId = sa.groupId;
                useSep = true;
            }
            if (!lastGroupId) lastGroupId = sa.groupId;
            return (
                <React.Fragment key={'frag=' + idx}>
                    {/*{useSep && <DropDownVerticalSeparator key={sa.cmd + '---separator'} useLine={true} style={{marginLeft:30}}/>}*/}
                    <ToolbarButton text={text} tip={`${sa.tip} for\n${text}`}
                                   style={{paddingLeft:30}}
                                   enabled={true} horizontal={false} key={sa.cmd+text}
                                   visible={isSupported(sa, cenWpt, radius, cornerStr, table)}
                                   onClick={() => doExecute(sa, cenWpt, radius, cornerStr, table)}/>
                </React.Fragment>
            );
        });

        return (
            <Fragment key='type-of-group'>
                <div key='cone-stuff' style={{whiteSpace: 'nowrap', fontSize: '10pt', padding: '5px 1px 5px 0'}}>
                    {asCone ?
                        <Fragment key='cone-on-center'>
                            <Typography color='warning' level='body-sm'>
                                Cone and Point Actions based on center:
                                {formatWorldPt(cenWpt, 3)}
                            </Typography>
                        </Fragment> :
                        <Fragment key='polygon-on-center'>
                            <Typography color='warning' level='body-sm'>
                                Polygon Actions
                            </Typography>
                        </Fragment>
                    }
                </div>
                {buttons}
            </Fragment>
        );


    };

    const doWholeTable= Boolean(wholeTableSearchActions.length);

    return (
        <SingleColumnMenu key='searchMenu'>
            {doWholeTable &&
                <Typography color='warning' level='body-sm'> Whole table actions </Typography>
            }
            { doWholeTable && wholeTableSearchActions.map((sa) => {
                const text = getSearchTypeDesc(sa, cenWpt, radius, corners?.length);
                return (
                    <ToolbarButton text={text} tip={`${sa.tip} for\n${text}`}
                                   style={{paddingLeft:30}}
                                   enabled={true} horizontal={false} key={sa.cmd+sa.tip}
                                   visible={isSupported(sa, cenWpt, radius, cornerStr, table)}
                                   onClick={() => doExecute(sa, cenWpt, radius, cornerStr, table)}/>
                );
            })}
            {
                saOrder.map( (saList,idx) => {
                    const pList= makePartialList(saList, cenWpt, (cone) ? idx===0 : idx===1);
                    if (idx===1 && saOrder[0].length>0 && saList.length>0) {
                        return (
                            <React.Fragment key={'frag-part-'+idx}>
                                {/*<DropDownVerticalSeparator key={'parts'+idx+'---separator'} useLine={false} style={{marginBottom:8}}/>*/}
                                {pList}
                            </React.Fragment>
                        );
                    }
                    else {
                        return pList;
                    }
                } )

            }
            {spacial &&
                <>
                    <DropDownVerticalSeparator useLine={true} key='refine---separator' style={{marginBottom:8}}/>
                    <ToolbarButton text='Refine search region' tip='Refine search region'
                                   style={{fontWeight:'bold'}}
                                   enabled={true} horizontal={false} key='refine' visible={true}
                                   onClick={() =>
                                       showSearchRefinementTool({
                                           element: buttonRef.current, plotId: pv.plotId, cone,
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
    if (!searchActions?.length) return false;
    const table= getTblById(tbl_id);
    if (!table) return false;
    if (!searchActions.some( ({searchType}) => tableTypes.includes(searchType))) return false;
    const wp= getWorldPtFromTableRow(table);
    return Boolean(wp);
}
