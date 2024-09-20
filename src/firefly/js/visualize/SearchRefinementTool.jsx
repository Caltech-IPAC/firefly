import {Box, Stack, Typography} from '@mui/joy';
import React, {useEffect} from 'react';
import {DEFAULT_VERB, getSearchTypeDesc, getValidSize, searchMatches} from '../core/ClickToAction.js';
import { dispatchHideDialog, dispatchShowDialog, } from '../core/ComponentCntlr.js';
import SearchSelectTool from '../drawingLayers/SearchSelectTool.js';
import {ConnectionCtx} from '../ui/ConnectionCtx.js';
import DialogRootContainer from '../ui/DialogRootContainer.jsx';
import {SingleColumnMenu} from '../ui/DropDownMenu.jsx';
import {DropDownToolbarButton} from '../ui/DropDownToolbarButton.jsx';
import {FieldGroup} from '../ui/FieldGroup.jsx';
import HelpIcon from '../ui/HelpIcon.jsx';
import {InputAreaFieldConnected} from '../ui/InputAreaField.jsx';
import {LayoutType, PopupPanel} from '../ui/PopupPanel.jsx';
import {RadioGroupInputField} from '../ui/RadioGroupInputField.jsx';
import {useFieldGroupValue, useStoreConnector} from '../ui/SimpleComponent.jsx';
import {SizeInputFields} from '../ui/SizeInputField.jsx';
import {DEF_TARGET_PANEL_KEY, TargetPanel} from '../ui/TargetPanel.jsx';
import {ToolbarButton} from '../ui/ToolbarButton.jsx';
import {getDlAry} from './DrawLayerCntlr.js';
import {visRoot} from './ImagePlotCntlr.js';
import {getDrawLayerByType, getPlotViewById, primePlot} from './PlotViewUtil.js';
import {parseWorldPt} from './Point.js';
import {CONE_AREA_OPTIONS, CONE_CHOICE_KEY, POLY_CHOICE_KEY} from './ui/CommonUIKeys.js';
import {SelectAreaButton} from './ui/SelectAreaDropDownView.jsx';
import {closeToolbarModalLayers, getModalEndInfo} from './ui/ToolbarToolModalEnd.js';
import {
    convertStrToWpAry, convertWpAryToStr, initSearchSelectTool, markOutline, SEARCH_REFINEMENT_DIALOG_ID,
    updateModalEndInfo, updatePlotOverlayFromUserInput, updateUIFromPlot
} from './ui/VisualSearchUtils.js';

export const POLY_CONE= 'POLY_CONE';
const SIZE_KEY= 'SIZE';
const CONE_AREA_KEY = 'CONE_AREA_KEY';
const POLYGON_KEY= 'POLYGON';
const GROUP_KEY= 'SearchRefinementToolGroup';
const DD_KEY= 'moreSearches';

export function showSearchRefinementTool({popupClosing, element, plotId, cone,
                                      searchActions, searchAreaInDeg, wp, polygonValue}) {


    const doClose= () => {
        dispatchHideDialog(SEARCH_REFINEMENT_DIALOG_ID);
        popupClosing?.();
        const dl = getDrawLayerByType(getDlAry(), SearchSelectTool.TYPE_ID);
        if (dl?.isInteractive) closeToolbarModalLayers();
    };

    const panel= (
        <PopupPanel title='Search refinement tool' layoutPosition={LayoutType.TOP_LEFT} element={element}
                    closeCallback={() => doClose()} initTop={220} initLeft={8} >
            <SearchRefinementTool {...{searchActions, plotId, searchAreaInDeg, wp, polygonValue, cone}} />
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(SEARCH_REFINEMENT_DIALOG_ID, panel, element );
    dispatchShowDialog(SEARCH_REFINEMENT_DIALOG_ID);
}


/**
 *
 * @param {Array.<ClickToActionCommand>} searchActions
 */
function findSearchTypes(searchActions) {
    const hasCone= searchActions.some((sa) => searchMatches(sa,true,false,true));
    const hasArea= searchActions.some((sa) => searchMatches(sa,false,true,false));
    if (hasCone && hasArea) return POLY_CONE;
    if (hasArea) return POLY_CHOICE_KEY;
    if (hasCone) return CONE_CHOICE_KEY;
}


function evalSearchActions(searchActions) {
    const hasRadius= searchActions.some((sa) => searchMatches(sa,true,false,false));
    const min= Math.min(...searchActions.map( (sa) => sa.min).filter( (n) => !isNaN(n)));
    const max= Math.max(...searchActions.map( (sa) => sa.max).filter( (n) => !isNaN(n)));
    return {min,max,hasRadius};
}

function SearchRefinementTool({searchActions, plotId, searchAreaInDeg, wp, polygonValue, cone}) {

    const modalEndInfo = useStoreConnector(() => getModalEndInfo());

    const pv= useStoreConnector(() => getPlotViewById(visRoot(),plotId));
    const [getConeAreaOp] = useFieldGroupValue(CONE_AREA_KEY, GROUP_KEY);
    const [getWP,setWP] = useFieldGroupValue(DEF_TARGET_PANEL_KEY, GROUP_KEY);
    const [getPoly,setPoly] = useFieldGroupValue(POLYGON_KEY, GROUP_KEY);
    const [getSize,setSize] = useFieldGroupValue(SIZE_KEY, GROUP_KEY);
    const polyStr= convertWpAryToStr(polygonValue,primePlot(pv));

    const searchTypes= findSearchTypes(searchActions);
    const usingToggle = Boolean(searchTypes===POLY_CONE);
    const {min,max,hasRadius}= evalSearchActions(searchActions);
    const whichOverlay= searchTypes===POLY_CONE ? getConeAreaOp() :
                              searchTypes===CONE_CHOICE_KEY ? CONE_CHOICE_KEY : POLY_CHOICE_KEY;


    useEffect(() => {
        initSearchSelectTool(plotId);
        updatePlotOverlayFromUserInput(plotId, whichOverlay, parseWorldPt(getWP()),
            Number(hasRadius ? getSize() : .0002), convertStrToWpAry(getPoly()));
        updateModalEndInfo(plotId);
        return () => {
        };
    },[plotId]);

    useEffect(() => {
        polyStr && setPoly(polyStr);
    }, [polyStr]);

    useEffect(() => {
        wp && setWP(wp);
        hasRadius && searchAreaInDeg && setSize(searchAreaInDeg);
    }, [wp, searchAreaInDeg]);

    useEffect(() => { // if plot view changes then update the target or polygon field
        updateUIFromPlot({
            plotId,
            undefined, whichOverlay,
            setTargetWp:setWP,
            getTargetWp:getWP,
            setHiPSRadius:setSize,
            getHiPSRadius:getSize,
            setPolygon:setPoly,
            getPolygon:getPoly,
            minSize:hasRadius?min:.00001,
            maxSize:hasRadius?max:.0002 });
    },[pv]);

    useEffect(() => { // if target or radius field change then hips plot to reflect it
        updatePlotOverlayFromUserInput(plotId, whichOverlay, parseWorldPt(getWP()),
            Number(hasRadius ? getSize() : .0002), convertStrToWpAry(getPoly()));
    }, [getWP, getSize, getPoly, whichOverlay]);

    const cenWpt= parseWorldPt(getWP()??wp);

    return (
        <ConnectionCtx.Provider value={{controlConnected:true, setControlConnected: () => undefined}}>
            <FieldGroup groupKey={GROUP_KEY} style={{display:'flex', flexDirection:'column'}}>
                <Stack {...{alignItems: 'center', minWidth: 480}}>
                    <HelpLines {...{whichOverlay, usingToggle}}/>
                    {usingToggle &&
                        <Stack {...{mb:1, direction:'column', justifyContent:'space-around', alignItems:'center'}}>
                            <RadioGroupInputField {...{
                                inline: true, fieldKey: CONE_AREA_KEY,
                                tooltip: 'Chose type of search',
                                orientation:'horizontal',
                                initialState: {value: cone ? CONE_CHOICE_KEY : POLY_CHOICE_KEY},
                                options: CONE_AREA_OPTIONS
                            }} />
                        </Stack>
                    }
                    {whichOverlay === CONE_CHOICE_KEY &&
                        <Stack>
                            <TargetPanel label='Center' defaultToActiveTarget={false} />
                            {hasRadius && <SizeInputFields {...{
                                fieldKey:SIZE_KEY, showFeedback:true, nullAllowed:false,
                                label: 'Size', sx:{mt:2},
                                initialState:{ unit: 'arcsec', value: searchAreaInDeg+'', min, max }
                            }} />}
                        </Stack> }
                    {whichOverlay === POLY_CHOICE_KEY &&
                        <InputAreaFieldConnected {...{
                            fieldKey:POLYGON_KEY,
                            label:'Search Polygon',
                            tooltip:'Search area of the polygon',
                            initialState:{value:convertWpAryToStr(polygonValue,primePlot(pv))},
                            sx: {width: 0.75}
                        }} /> }
                </Stack>
                <Stack {...{direction:'row', justifyContent:'space-between', mt:3, mb:1, ml:2, alignItems:'center'}}>
                    <ActionsDrop {...{searchActions, whichOverlay, polyStr:getPoly()??polyStr,
                        size:getSize()??searchAreaInDeg, cenWpt, op:getConeAreaOp()}}/>
                    <Stack {...{direction:'row', justifyContent:'space-between', alignItems:'center'}}>
                        <SelectAreaButton {...{pv,modalEndInfo,tip:'Reselect an area for search',text:'Select Again', variant:'soft'}}/>
                        <HelpIcon helpId={'SearchRefinementTool'}/>
                    </Stack>
                </Stack>
            </FieldGroup>
        </ConnectionCtx.Provider>
    );
}

const ActionsDrop= ({searchActions, polyStr, size, cenWpt, whichOverlay, op}) => {
    const post= op===CONE_CHOICE_KEY ? 'cone' : 'polygon';
    const allDefault= searchActions.every((sa) => sa.verb === DEFAULT_VERB);
    const searchText=  (allDefault) ? 'Search ' + post : 'Actions for ' + post;
    return (
        <DropDownToolbarButton text={searchText} tip='Search this area' disableHiding={true} dropDownKey={DD_KEY}
                               color='primary' variant='solid'
                               useDropDownIndicator={true}
                               style={{
                                   alignSelf: 'flex-start',
                                   height: 22,
                                   borderRadius: '3px',
                                   border: '1px outset rgba(0,0,0,.4)',
                                   padding: '0 6px 0 0',
                                   marginTop:2,
                               }}
                               dropDown={<SearchDropDown {...{searchActions, cenWpt, size, polyStr, whichOverlay}}/>}
        />
    );
};

function SearchDropDown({searchActions, cenWpt, size, polyStr, whichOverlay}) {
    const polyStrLen= polyStr ? polyStr.split(' ').length/2 : 0;
    return (
        <SingleColumnMenu>
            {searchActions
                .filter( (sa) =>
                   searchMatches(sa, whichOverlay===CONE_CHOICE_KEY, whichOverlay===POLY_CHOICE_KEY, whichOverlay===CONE_CHOICE_KEY) )
                .map( (sa) => {
                    const text= getSearchTypeDesc(sa,cenWpt,Number(size),polyStrLen);
                    return (
                        <ToolbarButton text={text} tip={`${sa.tip} for\n${text}`}
                                       enabled={true} horizontal={false} key={sa.cmd+sa.tip}
                                       visible={sa.supported()}
                                       onClick={() => {
                                           markOutline(sa,
                                               primePlot(visRoot())?.plotId,{
                                                   wp:cenWpt,
                                                   radius:Number(getValidSize(sa,size)),
                                                   polyStr});
                                           dispatchHideDialog(SEARCH_REFINEMENT_DIALOG_ID);
                                           sa.execute(sa,cenWpt,getValidSize(sa,size),polyStr);
                                       }
                                       }/>
                    );
                }) }
        </SingleColumnMenu>
    );
}

function HelpLines({usingToggle, whichOverlay}) {
    const isCone= whichOverlay===CONE_CHOICE_KEY;

    const reSelectMsg= isCone ?
        'Or reselect the cone area' :
        'Or reselect the polygon area';
    const entryMsg= isCone ?
        'Or enter new values for center (name or coordinate) and size' :
        'Or enter new values for polygon ';
    const otherSearchMsg=  isCone ?
        'Or switch to polygon searches' :
        'Or switch cone searches';

    return (
        <Box {...{mb:2}}>
            <Typography level='title-sm' sx={{mb:.5}}>
                Try the following:
            </Typography>
            <Typography level='body-sm' {...{sx:{ml: 1}}}>
                <li>Click on the image to choose a new search center</li>
                <li>{entryMsg}</li>
                <li>{reSelectMsg}</li>
                {usingToggle && <li>{otherSearchMsg}</li>}
                <li>Then initiate the search of your choice from the menu below.</li>
            </Typography>
        </Box>);
}

