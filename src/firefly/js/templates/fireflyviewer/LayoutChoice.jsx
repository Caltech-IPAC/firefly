

import {pick} from 'lodash';
import {func} from 'prop-types';
import React, {useState} from 'react';
import {Card, Stack, Typography} from '@mui/joy';
import {
    BIVIEW_I_ChCov, BIVIEW_ICov_Ch,
    BIVIEW_IChCov_T, BIVIEW_T_IChCov,
    dispatchSetLayoutMode, dispatchTriviewLayout, dispatchUpdateLayoutInfo, getLayouInfo, getResultCounts, LO_MODE,
    LO_VIEW, TRIVIEW_I_ChCov_T, TRIVIEW_ICov_Ch_T
} from '../../core/LayoutCntlr.js';
import {ListBoxInputFieldView} from '../../ui/ListBoxInputField.jsx';
import {AccordionPanelView} from '../../ui/panel/AccordionPanel.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';


const triViewKey= 'images | tables | xyplots';
const tblImgKey= 'tables | images';
const imgXyKey= 'images | xyplots';
const tblXyKey= 'tables | xyplots';
const xYTblKey= 'xyplots | tables';
const stateKeys= ['mode', 'showTables', 'showImages', 'showXyPlots', 'images', 'coverageSide'];
const LEFT= 'LEFT';
const RIGHT= 'RIGHT';

function getCovSideOptions(currLayoutMode, showImages) {
    const make= (l,r) => [ {label:l, value:LEFT}, {label:r, value:RIGHT}];

    switch (currLayoutMode) {
        case triViewKey: return make('Left', showImages?'Right':'Coverage w/Charts');
        case tblImgKey: return make('Coverage showing', 'Coverage hidden');
        case imgXyKey: return make('Left', 'Right');
        case tblXyKey: return make('Coverage hidden', 'Coverage showing');
        default: return make('Coverage left', 'Coverage right');
    }
}



export function LayoutChoiceAccordion({closeSideBar}) {
    const haveResults= useStoreConnector(() => getResultCounts().haveResults);
    const [panelOpen, setPanelOpen] = useState(true);

    if (!haveResults) return <div/>;

    return (
        <AccordionPanelView
            header={<Typography level='h4'>Results Layout</Typography>} expanded={panelOpen}
            onChange = {(v) => setPanelOpen(v)}>
            <LayoutChoice closeSideBar={closeSideBar} haveResults={haveResults}/>
        </AccordionPanelView>
    );
}

LayoutChoiceAccordion.propTypes= {
    closeSideBar: func,
};


export function LayoutChoiceVisualAccordion({closeSideBar}) {
    const haveResults= useStoreConnector(() => getResultCounts().haveResults);
    const [panelOpen, setPanelOpen] = useState(true);

    if (!haveResults) return <div/>;

    return (
        <AccordionPanelView
            header={<Typography level='h4'>Results Layout</Typography>} expanded={panelOpen}
            onChange = {(v) => setPanelOpen(v)}>
            <LayoutChoiceLayoutVisual closeSideBar={closeSideBar}/>
        </AccordionPanelView>
    );
}

export function LayoutChoiceLayoutVisual({closeSideBar}) {
    const {mode, showTables, showImages, showXyPlots, images={}, coverageSide=LEFT}=
        useStoreConnector(() => pick(getLayouInfo(), stateKeys));

    const {showCoverage} = images;
    const coverageRight= showCoverage && coverageSide===RIGHT;
    const currLayoutMode= getLayouInfo()?.mode?.standard?.toString() ?? triViewKey;
    const isTriViewPossible = (showImages||showCoverage) && showXyPlots && showTables;

    if (mode?.expanded && mode.expanded!==LO_VIEW.none) {
        return <Typography sx={{pt:1}}>Single View Expanded</Typography>;
    }

    const layoutOps= getOptions(isTriViewPossible,showImages,showCoverage);

    const getCurrentOp= (currLayoutMode, showImages) => {
        if (layoutOps.length===1) return layoutOps[0].value;
        switch (currLayoutMode) {
            case triViewKey:
                if (showCoverage || showImages) {
                    return (coverageRight) ? TRIVIEW_I_ChCov_T : TRIVIEW_ICov_Ch_T;
                }
                else {
                    return BIVIEW_IChCov_T;
                }
            case imgXyKey:
                return (coverageRight && showImages) ? BIVIEW_I_ChCov : BIVIEW_ICov_Ch;
            case tblXyKey: return BIVIEW_T_IChCov;
            case xYTblKey: return BIVIEW_IChCov_T;
        }
        return TRIVIEW_I_ChCov_T;
    };


    return (
        <Stack spacing={1} pt={1}>
            <ListBoxInputFieldView {...{
                slotProps: {
                    input: {
                        slotProps: { listbox: { sx: { maxHeight: '60vh' } } }
                    }
                },
                options:layoutOps, value:getCurrentOp(currLayoutMode,showImages),
                onChange:(ev,newVal) => {
                    changeLayout(newVal);
                    closeSideBar?.();
                },
                renderValue:
                    ({value}) => (<OpRender {...{ key:value, value,useBorder:false, showImages,showCoverage}}/>),
                decorator:
                    (label,value) => (<OpRender {...{key:label, value, showImages,showCoverage}} />),

            }} />
        </Stack>
    );
}

function getOptions(isTriViewPossible,showImages,showCoverage) {
    const layoutOps= [];
    if (isTriViewPossible) layoutOps.push({label:'Tri-view (L:Cov,I  R: Charts  B: Tables)', value:TRIVIEW_ICov_Ch_T});  // L:Cov,Images - R: Charts - B: Tables
    if (isTriViewPossible && (showImages&&showCoverage)) {
        layoutOps.push({label:'Tri-view images (L:I  R: Charts,Cov   B: Tables)', value:TRIVIEW_I_ChCov_T}); // L:Images -  R: Cov,Charts - B: Tables
    }

    if (showCoverage) layoutOps.push({label:'Bi-view Charts', value:BIVIEW_ICov_Ch});   // L:I,Cov  R:Charts  (no tables)
    if (showCoverage && showImages) layoutOps.push({label:'Bi-view images', value:BIVIEW_I_ChCov});  // L:I - R:Cov,Charts - (no tables)
    layoutOps.push({label:'Bi-view Tables', value:BIVIEW_T_IChCov});   // L:Tables -  R: Charts,Cov,Images
    layoutOps.push({label:'Bi-view Tables', value:BIVIEW_IChCov_T});   // L: Charts,Cov,Images - R:Tables
    return layoutOps;
}

function changeLayout(newVal)  {
    dispatchTriviewLayout({triviewLayout:newVal});
}

function getICDesc(showImages,showCoverage) {
    if (showCoverage && showImages) return ['Coverage','Images'];
    if (showCoverage) return ['Coverage'];
    if (showImages) return ['Images'];
    return [''];
}

function OpRender({value, width='12rem', useBorder=true, showImages, showCoverage}) {
    const left= getICDesc(showImages,showCoverage);
    switch (value) {
        case TRIVIEW_ICov_Ch_T:
            return <TriViewItems {...{useBorder, width, left, right: ['Charts'], bottom:['Tables']}}/>;
        case TRIVIEW_I_ChCov_T:
            return <TriViewItems {...{useBorder, width, left:['Images'] , right: ['Coverage', 'Charts'], bottom:['Tables']}}/>;
        case BIVIEW_ICov_Ch:
            return <BiViewItems {...{useBorder, width, left, right: ['Charts']}} />;
        case BIVIEW_I_ChCov:
            return <BiViewItems {...{useBorder, width, left:['Images'] , right: ['Coverage','Charts']}} />;
        case BIVIEW_T_IChCov:
            const right=  [...getICDesc(showImages,showCoverage),'Charts'];
            return <BiViewItems {...{useBorder, width, left:['Tables'] , right}} />;
        case BIVIEW_IChCov_T:
            return <BiViewItems {...{useBorder, width, left:[...getICDesc(showImages,showCoverage), 'Charts'], right:['Tables']}} />;
    }
    return <div>{value}</div>;
}

const TriViewItems= ({left,right,bottom,width=1,useBorder}) => (
    <Wrap {...{width,useBorder}}>
        <Stack width={1} spacing={1/4}>
            <HorizontalItems {...{left, right}} />
            <Items stringAry={bottom}/>
        </Stack>
    </Wrap>
);

const BiViewItems= ({left,right,width=1,useBorder}) => (
    <Wrap {...{width,useBorder}}>
        <HorizontalItems {...{left, right}} />
    </Wrap>
);


const HorizontalItems= ({left,right}) => (
    <Stack {...{direction:'row', justifyContent:'space-between', width:1, spacing:1/4}}>
        <Items {...{stringAry:left, sx:{width:1} }}/>
        <Items {...{stringAry:right, sx:{width:1} }}/>
    </Stack>
);

const Wrap= ({useBorder, width, children}) => (
    <Card {...{variant:useBorder?'outlined':'plain',
        sx:{'--Card-padding': useBorder?'.4rem':'.05rem', alignSelf:'stretch', width,
            my: useBorder?1/2:0, backgroundColor:'transparent'} }}>
        {children}
    </Card>

);


const Items= ({stringAry, sx}) => (
    <Card {...{color:'warning', variant:'outlined',
        sx:{'--Card-padding': '.3rem', backgroundColor:'transparent', ...sx}}}>
        <Stack {...{alignItems:'center', width:1}}>
            {stringAry.map( (s) =>
                <Typography {...{sx:{width:1}, color:'neutral', textAlign:'center', key:s, level:'body-sm'}}>{s}</Typography>)}
        </Stack>
    </Card>
);





export function LayoutChoice({closeSideBar, haveResults}) {
    const state= useStoreConnector(() => pick(getLayouInfo(), stateKeys));
    if (!haveResults) return <div/>;

    const {showTables, showImages, showXyPlots, images={}, coverageSide=LEFT} = state;
    const {showCoverage} = images;
    const coverageRight= showCoverage && coverageSide===RIGHT;
    const coverageLeft= showCoverage && coverageSide===LEFT;
    const currLayoutMode= getLayouInfo()?.mode?.standard?.toString() ?? triViewKey;
    const isTriViewPossible = (showImages||coverageLeft) && (showXyPlots||coverageRight) && showTables;

    const covSideOptions= getCovSideOptions(currLayoutMode, showImages);

    const options= [
        {label:'Tri-view', value:triViewKey},
        {label:'Bi-view Images', value:imgXyKey},
        {label:'Bi-view Tables', value:tblXyKey},
    ];


    return (
        <Stack spacing={1} pt={1}>
            {showCoverage && currLayoutMode!==tblXyKey &&
                <ListBoxInputFieldView {...{
                    options:covSideOptions, value:coverageSide,
                    label:'Coverage',
                    slotProps:{label: {sx: {width: '7rem'}}},
                    onChange:(ev,newVal) => {
                        dispatchUpdateLayoutInfo({coverageSide:newVal});
                        closeSideBar?.();
                    },
                }} /> }
            {isTriViewPossible &&
               <ListBoxInputFieldView
                    {...{
                        options, value:currLayoutMode,
                        label:'Layout',
                        slotProps:{label: {sx: {width: '7rem'}}},
                        onChange:(ev,newVal) => {
                            dispatchSetLayoutMode(LO_MODE.standard, newVal);
                            closeSideBar?.();
                        },
                    }} />
            }
        </Stack>
    );
}