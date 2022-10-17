import React, {useEffect} from 'react';
import {dispatchHideDialog, dispatchShowDialog} from '../core/ComponentCntlr.js';
import DialogRootContainer from '../ui/DialogRootContainer.jsx';
import {SingleColumnMenu} from '../ui/DropDownMenu.jsx';
import {DropDownToolbarButton} from '../ui/DropDownToolbarButton.jsx';
import {FieldGroup} from '../ui/FieldGroup.jsx';
import {InputAreaFieldConnected} from '../ui/InputAreaField.jsx';
import {LayoutType, PopupPanel} from '../ui/PopupPanel.jsx';
import {RadioGroupInputField} from '../ui/RadioGroupInputField.jsx';
import {useFieldGroupValue, useStoreConnector} from '../ui/SimpleComponent.jsx';
import {SizeInputFields} from '../ui/SizeInputField.jsx';
import {DEF_TARGET_PANEL_KEY, TargetPanel} from '../ui/TargetPanel.jsx';
import {ToolbarButton} from '../ui/ToolbarButton.jsx';
import {DEFAULT_VERB, getSearchTypeDesc, getValidSize, searchMatches} from '../core/ClickToAction.js';
import {visRoot} from './ImagePlotCntlr.js';
import {getPlotViewById, primePlot} from './PlotViewUtil.js';
import {parseWorldPt} from './Point.js';
import {CONE_AREA_OPTIONS, CONE_CHOICE_KEY, POLY_CHOICE_KEY} from './ui/CommonUIKeys.js';
import {closeToolbarModalLayers} from './ui/VisMiniToolbar.jsx';
import {ConnectionCtx} from '../ui/ConnectionCtx.js';
import {
    convertStrToWpAry, convertWpAryToStr, HelpLines, initSearchSelectTool, removeSearchSelectTool,
    updatePlotOverlayFromUserInput,
    updateUIFromPlot
} from './ui/VisualSearchUtils.js';

const DIALOG_ID = 'SEARCH_REFINEMENT_DIALOG';
const CONE_AREA_KEY = 'CONE_AREA_KEY';
export const POLY_CONE= 'POLY_CONE';
const SIZE_KEY= 'SIZE';
const POLYGON_KEY= 'POLYGON';
const GROUP_KEY= 'SearchRefinementToolGroup';


export function showSearchRefinementTool({popupClosing, element, plotId,
                                      searchActions, searchAreaInDeg, wp, polygonValue}) {


    const doClose= () => {
        closeToolbarModalLayers();
        popupClosing?.();
    };

    const panel= (
        <PopupPanel title='Search refinement tool' layoutPosition={LayoutType.TOP_LEFT} element={element}
                    closeCallback={() => doClose()} initTop={220} initLeft={8} >
            <SearchRefinementTool {...{searchActions, plotId, searchAreaInDeg, wp, polygonValue}} />
        </PopupPanel>
    );
    DialogRootContainer.defineDialog(DIALOG_ID, panel, element );
    dispatchShowDialog(DIALOG_ID);
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


function SearchRefinementTool({searchActions, plotId, searchAreaInDeg, wp, polygonValue}) {
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
        return () => {
            removeSearchSelectTool(plotId);
        };
    },[plotId]);

    useEffect(() => {
        polyStr && setPoly(polyStr);
    }, [polyStr]);

    useEffect(() => {
        wp && setWP();
        hasRadius && searchAreaInDeg && setSize(searchAreaInDeg);
    }, [wp, searchAreaInDeg]);

    useEffect(() => { // if plot view changes then update the target or polygon field
        updateUIFromPlot(plotId,whichOverlay,setWP,getWP,setSize,getSize,setPoly,getPoly,
            hasRadius?min:.00001,hasRadius?max:.0002);
    },[pv]);

    useEffect(() => { // if target or radius field change then hips plot to reflect it
        updatePlotOverlayFromUserInput(plotId, whichOverlay, parseWorldPt(getWP()),
            Number(hasRadius ? getSize() : .0002), convertStrToWpAry(getPoly()));
    }, [getWP, getSize, getPoly, whichOverlay]);

    return (
        <ConnectionCtx.Provider value={{controlConnected:true, setControlConnected: () => undefined}}>
            <FieldGroup groupKey={GROUP_KEY} style={{display:'flex', flexDirection:'column', padding: '10px 5px 5px 5px'}}>
                <div style={{display: 'flex', flexDirection: 'column', alignItems: 'center', minWidth: 480}}>
                    <div style={{fontSize:'9pt', paddingRight: 10}}>
                        {<HelpLines whichOverlay={whichOverlay} />}
                    </div>
                    {usingToggle &&
                        <RadioGroupInputField {...{
                            inline: true, fieldKey: CONE_AREA_KEY, wrapperStyle: {paddingBottom: 10},
                            tooltip: 'Chose type of search', initialState: {value: CONE_CHOICE_KEY}, options: CONE_AREA_OPTIONS
                        }} />
                    }
                    {whichOverlay === CONE_CHOICE_KEY &&
                        <div style={{display:'flex', flexDirection:'column'}}>
                            <TargetPanel labelWidth={50} label='Center'
                                         feedbackStyle={{height:35}}
                                         labelStyle={{paddingRight:10, textAlign:'right'}}
                                         inputStyle={{width:250}}/>
                            {hasRadius && <SizeInputFields {...{
                                fieldKey:SIZE_KEY, showFeedback:true, labelWidth:50, nullAllowed:false,
                                label: 'Size',
                                feedbackStyle:{textAlign:'center', marginLeft:0},
                                labelStyle:{textAlign:'right', paddingRight:10},
                                initialState:{ unit: 'arcsec', value: searchAreaInDeg+'', min, max }
                            }} />}
                        </div> }
                    {whichOverlay === POLY_CHOICE_KEY &&
                        <InputAreaFieldConnected {...{
                            fieldKey:POLYGON_KEY, label:'Search Polygon',
                            labelStyle:{textAlign:'right', paddingRight:4},
                            labelWidth:100, tooltip:'Search area of the polygon',
                            wrapperStyle:{display:'flex', alignItems:'center'},
                            style:{overflow:'auto', height:55, maxHeight:200, minWidth: 100, width:280, maxWidth:360,},
                            initialState:{value:convertWpAryToStr(polygonValue,primePlot(pv))},
                        }} /> }
                </div>
                <ActionsDrop {...{searchActions, whichOverlay, polyStr:getPoly()??polyStr, size:getSize()??searchAreaInDeg, cenWpt:getWP()??wp}}/>
            </FieldGroup>
        </ConnectionCtx.Provider>
    );
}

const ActionsDrop= ({searchActions, polyStr, size, cenWpt, whichOverlay}) => (
        <DropDownToolbarButton text={searchActions.every( (sa) => sa.verb===DEFAULT_VERB) ? 'Searches' : 'Actions'}
                               tip='Search this area'
                               disableHiding={true} dropDownKey={'moreSearches'}
                               useDropDownIndicator={true} enabled={true} horizontal={true} visible={true}
                               style={{
                                   margin: '15px 0 3px 10px',
                                   alignSelf: 'flex-start',
                                   height: 20,
                                   borderRadius: '3px',
                                   border: '1px outset rgba(0,0,0,.4)',
                                   padding: '0 6px 0 0',
                               }}
                               dropDown={<SearchDropDown {...{searchActions, cenWpt, size, polyStr, whichOverlay}}/>}
                               />
);

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
                                       enabled={true} horizontal={false} key={sa.cmd}
                                       visible={sa.supported()}
                                       onClick={() => {
                                           dispatchHideDialog(DIALOG_ID);
                                           sa.execute(sa,cenWpt,getValidSize(sa,size),polyStr);
                                       }
                                       }/>
                    );
                }) }
        </SingleColumnMenu>
    );

}