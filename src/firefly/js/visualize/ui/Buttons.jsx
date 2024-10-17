import React from 'react';
import {node} from 'prop-types';
import {Box, Button, IconButton, ToggleButtonGroup} from '@mui/joy';

// TODO: we should use icon from one type(filled, outlined, rounded, two-tone, sharp).  It will yield better consistency.

import BiotechOutlinedIcon from '@mui/icons-material/BiotechOutlined';
import OpenInFull from '@mui/icons-material/OpenInFull';
import GRID_EXPAND from 'images/icons-2014/24x24_ExpandArrows-grid-3.png';
import {DropDownToolbarButton} from '../../ui/DropDownToolbarButton.jsx';
import {SimpleLayerOnOffButton} from './SimpleLayerOnOffButton.jsx';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import CenterFocusWeakOutlinedIcon from '@mui/icons-material/CenterFocusWeakOutlined';
import ColorLens from '@mui/icons-material/ColorLensOutlined';
import Layers from '@mui/icons-material/Layers';
import ConstructionRoundedIcon from '@mui/icons-material/ConstructionRounded';
import FlipOutlinedIcon from '@mui/icons-material/FlipOutlined';
import StraightenOutlinedIcon from '@mui/icons-material/StraightenOutlined';
import GrainIcon from '@mui/icons-material/Grain';
import HorizontalRuleRoundedIcon from '@mui/icons-material/HorizontalRuleRounded';
import ThreeSixtyRoundedIcon from '@mui/icons-material/ThreeSixtyRounded';
import LinkIcon from '@mui/icons-material/Link';
import LinkOffIcon from '@mui/icons-material/LinkOff';
import SquareRoundedIcon from '@mui/icons-material/SquareRounded';
import GridViewRoundedIcon from '@mui/icons-material/GridViewRounded';
import FormatListBulletedRoundedIcon from '@mui/icons-material/FormatListBulletedRounded';
import CenterFocusStrongTwoToneIcon from '@mui/icons-material/CenterFocusStrongTwoTone';

import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import CropRoundedIcon from '@mui/icons-material/CropRounded';
import DoneAllRoundedIcon from '@mui/icons-material/DoneAllRounded';
import RemoveDoneRoundedIcon from '@mui/icons-material/RemoveDoneRounded';
import FunctionsOutlinedIcon from '@mui/icons-material/FunctionsOutlined';

import FilterIco from '@mui/icons-material/FilterAltOutlined';
import ClearFilterIco from '@mui/icons-material/FilterAltOffOutlined';
import TextViewIco from '@mui/icons-material/TextFieldsOutlined';
import TableViewIco from '@mui/icons-material/TableChartOutlined';
import SettingsIco from '@mui/icons-material/SettingsOutlined';
import PropertySheetIco from '@mui/icons-material/ReadMoreOutlined';
import ResetIco from '@mui/icons-material/RestartAltOutlined';
import PanIco from '@mui/icons-material/OpenWithOutlined';
import PinChartIco from '@mui/icons-material/PushPin';
import ShowTableIco from '@mui/icons-material/TableView';
import CombineChartIco from '@mui/icons-material/JoinInner';
import UnfoldMoreOutlinedIcon from '@mui/icons-material/UnfoldMoreOutlined';
import UnfoldLessOutlinedIcon from '@mui/icons-material/UnfoldLessOutlined';
import AddCircleOutlineOutlinedIcon from '@mui/icons-material/AddCircleOutlineOutlined';
import ViewComfyAltRoundedIcon from '@mui/icons-material/ViewComfyAltRounded';
import ViewModuleRoundedIcon from '@mui/icons-material/ViewModuleRounded';
import NavigateNext from '@mui/icons-material/NavigateNextRounded';
import NavigateBefore from '@mui/icons-material/NavigateBeforeRounded';
import PictureAsPdfOutlinedIcon from '@mui/icons-material/PictureAsPdfOutlined';

import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import Crop169Icon from '@mui/icons-material/Crop169';

import ZBaseIcon from '@mui/icons-material/SearchOutlined';
import PlusIcon from '@mui/icons-material/AddOutlined';
import MinusIcon from '@mui/icons-material/RemoveOutlined';
import FitIcon from '@mui/icons-material/ZoomInMapOutlined';
import FillIcon from '@mui/icons-material/OpenInFullOutlined';
import OneXIcon from '@mui/icons-material/TimesOneMobiledataOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';

import FiberManualRecordRoundedIcon from '@mui/icons-material/FiberManualRecordRounded';

const ThreeCIcon = () => (
    <Box sx={{width: 24, height: 24}}>
        <FiberManualRecordRoundedIcon style={{color:'red'}} sx={{position: 'absolute', left: 7, top: -4,  transform: 'scale(.5)'}}/>
        <FiberManualRecordRoundedIcon style={{color:'green'}} sx={{position: 'absolute', left: -2, top: 0,  transform: 'scale(.5)'}}/>
        <FiberManualRecordRoundedIcon style={{color:'blue'}} sx={{position: 'absolute', left: -4, top: 10,  transform: 'scale(.5)'}}/>
        <AddCircleOutlineIcon sx={{position: 'absolute', left: 8, top: 8, transform: 'scale(.85)'}}/>
    </Box>
);

export const ThreeColor = (props) => {
    return <ToolbarButton icon={<ThreeCIcon/>} tip='Three Color' {...props}/>;
};


export const ThreeColorOnOff= ({isIconOn, sx, ...props}) => {
    const sxToUse= (theme) => ({
        'button' :{
            background: isIconOn ? theme.vars.palette.primary?.softDisabledColor : undefined,
        },
        ...sx
    });
    return (<ToolbarButton {...{sx:sxToUse, icon: <ThreeCIcon/>, isIconOn, ...props}}/>);
};



export const ExpandButton= ({expandGrid, ...props}) =>(
    <ToolbarButton {...{
        icon: (expandGrid? GRID_EXPAND : <OpenInFull sx={{transform: 'scale(1.1,-1.1)'}}/>), ...props }}/>);

export const NextButton= (props) =>(
    <ToolbarButton {...{
        icon: (<NavigateNext sx={{transform: 'scale(1.6,1.6)'}}/>), ...props }}/>);

export const BeforeButton= (props) =>(
    <ToolbarButton {...{
        icon: (<NavigateBefore sx={{transform: 'scale(1.6,1.6)'}}/>), ...props }}/>);

export const PDFButton= (props) =>(
    <ToolbarButton {...{
        icon: (<PictureAsPdfOutlinedIcon sx={{transform: 'scale(1.6,1.6)'}}/>), ...props }}/>);


export const LockImages= ({locked, ...props}) =>{
    const sx={position:'absolute', transform: 'scale(1.2,1.2)', top:1, left:4};
    const icon= (
        <Box sx={{width:24,height:24}}>
            {locked ? <LinkIcon {...{sx}}/> : <LinkOffIcon {...{sx}}/>}
        </Box>
    );
    return (<DropDownToolbarButton {...{ icon, dropPosition:{left:6,bottom:-2}, ...props, }}  />);
};


export const FilterAddButton= (props) => {
    const icon= (
        <Box sx={{width:24,height:24}}>
            <FilterIco sx={{position:'absolute', top:0, left:-1}}/>
            <AddTwoToneIcon sx={{position:'absolute', left:8, bottom:-3, transform: 'scale(.6,.6)'}}/>
        </Box>
    );
    return <TB {...{ icon, ...props}}/>;
};

export const CenterDropdown= (props) => (
    <DropDownToolbarButton {...{ icon: <CenterFocusWeakOutlinedIcon/>, useDropDownIndicator: true,
        dropPosition:{left:3,bottom:-2},
        iconButtonSize:'40px', ...props,  }}/>);

export const ColorDropDownButton= (props) => (
    <DropDownToolbarButton {...{ icon: <ColorLens/>, useDropDownIndicator: true,
        dropPosition:{left:1,bottom:-2},
        iconButtonSize:'40px', ...props,  }}/>);

export const DrawLayersButton= (props) => (
    <ToolbarButton {...{ icon: <Layers viewBox={'2 0 22 20'}/>, iconButtonSize:'40px', ...props,  }}/>);


export const FlipYButton= ({isIconOn, ...props}) => {
    return (<SimpleLayerOnOffButton {...{SvgIconComponent:<FlipOutlinedIcon/>, isIconOn, iconButtonSize:'42px',
        ...props}}/>);
};

export const DistanceButton= ({isIconOn, ...props}) => {
    return (<SimpleLayerOnOffButton {...{isIconOn, SvgIconComponent:<StraightenOutlinedIcon className='distance'/>,
                                        iconButtonSize:'42px',
        sx:{'& .distance': {transform: 'rotate(142deg)'}},
        ...props}}/>);
};

export const ExtractPoints= (props) => (<TB {...{ icon: <GrainIcon/>, ...props,  }}/>);

export const ExtractLine= (props) => (
    <TB {...{ icon: <HorizontalRuleRoundedIcon className='extractLine'/>,
        sx:{'& .extractLine': {transform: 'rotate(142deg)'}}, ...props,  }}/>);

export const SearchDetailButton= (props) => (
    <DropDownToolbarButton {...{ icon: <BiotechOutlinedIcon/>, iconButtonSize:'44px', useDropDownIndicator: true,
        dropPosition: {left: 2, bottom: 0}, ...props
    }}/>);

export const ToolsDropDown= (props) => (
    <DropDownToolbarButton {...{ icon: <ConstructionRoundedIcon className='tools'/>, useDropDownIndicator: true,
        sx:{'& .tools': {transform: 'rotate(9deg) scale(-1.1,1.1)'}},
        dropPosition:{left:6,bottom:-2},
        iconButtonSize:'40px', ...props,  }}/>);

// Table and Chart buttons -->
export const FilterButton = (props) => (
    <ToolbarButton {...{icon: <FilterIco/>, tip: 'Show/edit filters', iconButtonSize:'38px', ...props}}/>
);

export const WarningButton = (props) => (
    <ToolbarButton {...{icon: <WarningAmberOutlinedIcon/>,
        tip: 'warning', color:'danger', iconButtonSize:'38px', ...props}}/>
);

export const ClearFilterButton = (props) => (
    <ToolbarButton {...{icon: <ClearFilterIco/>, tip: 'Remove all filters', iconButtonSize:'38px', ...props}}/>
);

export const TextViewButton = (props) => (
    <ToolbarButton {...{icon: <TextViewIco/>, tip: 'Text View', iconButtonSize:'38px', ...props}}/>
);

export const TableViewButton = (props) => (
    <ToolbarButton {...{icon: <TableViewIco/>, tip: 'Table View', iconButtonSize:'38px', ...props}}/>
);

export const AddColumnButton = (props) => {
    const icon = (
        <Box sx={{width: 24, height: 24}}>
            <Crop169Icon sx={{position: 'absolute', top: 3, left:-1, transform: 'rotate(90deg) scale(1.3,1)'}}/>
            <AddCircleOutlineIcon sx={{position: 'absolute', left: 11, top: 4, transform: 'scale(.75,.75)'}}/>
        </Box>
    );
    return <ToolbarButton icon={icon} tip='Add a column' {...props}/>;
};

export const SettingsButton = (props) => (
    <ToolbarButton {...{icon: <SettingsIco/>, tip: 'Chart options and tools', iconButtonSize:'38px', ...props}}/>
);

export const PropertySheetButton = (props) => (
    <ToolbarButton {...{icon: <PropertySheetIco/>, iconButtonSize:'38px', ...props}}/>
);


export const PanButton = (props) => (
    <ToolbarButton {...{icon: <PanIco/>, iconButtonSize:'38px', ...props}}/>
);

export const AddItem = (props) => (
    <ToolbarButton {...{icon: <AddCircleOutlineOutlinedIcon/>, iconButtonSize:'38px', ...props}}/>
);

export const PinButton = (props) => (
    <ToolbarButton {...{icon: <PinChartIco/>, iconButtonSize:'38px', ...props}}/>
);

export const ShowTableButton = (props) => (
    <ToolbarButton {...{icon: <ShowTableIco/>, iconButtonSize:'38px', ...props}}/>
);

export const CombineChartButton = (props) => (
    <ToolbarButton {...{icon: <CombineChartIco/>, iconButtonSize:'38px', tip:'Combine charts', ...props}}/>
);
// <-- Table and Chart buttons

export const RestoreButton= (props) => ( <TB {...{ icon: <ResetIco/>, ...props}}/>);
export const RotateButton= (props) => ( <TB {...{ icon: <ThreeSixtyRoundedIcon/>, ...props,  }}/>);
export const SaveButton= (props) => ( <TB {...{ icon: <SaveOutlinedIcon/>, ...props}}/>);
export const InfoButton= (props) => (<TB {...{ icon: <InfoOutlinedIcon/>, ...props}}/>);
export const ColorButtonIcon= (props) => (<TB {...{ icon: <ColorLens/>, ...props}}/>);
// export const OneTileButton= (props) => ( <TB {...{ icon: <SquareRoundedIcon/>, ...props}}/>);
// export const GridTileButton= (props) => (<TB {...{ icon: <GridViewRoundedIcon/>, ...props}}/>);
// export const GridTileCompactButton= (props) => ( <TB {...{ icon: <ViewCompactIcon/>, ...props}}/>);
export const ListViewButton= (props) => ( <TB {...{ icon: <FormatListBulletedRoundedIcon/>, ...props}}/>);
export const CheckedButton= (props) => ( <TB {...{ icon: <DoneAllRoundedIcon/>, ...props,}}/>);
export const CheckedClearButton= (props) => ( <TB {...{ icon: <RemoveDoneRoundedIcon/>, ...props}}/>);
export const CenterOnSelection= (props) => ( <TB {...{ icon: <CenterFocusStrongTwoToneIcon/>, ...props}}/>);
export const CropButton= (props) => ( <TB {...{ icon: <CropRoundedIcon/>, ...props}}/>);
export const StatsButton= (props) => ( <TB {...{ icon: <FunctionsOutlinedIcon/>, ...props}}/>);
export const ExpandAll= (props) => ( <TB {...{icon: <UnfoldMoreOutlinedIcon/>, ...props}}/>);
export const CollapseAll= (props) => ( <TB {...{icon: <UnfoldLessOutlinedIcon/>, ...props}}/>);

export const FiltersOffButton= (props) => ( <TB {...{ icon: <ClearFilterIco/>, ...props}}/>);

// export const SettingsButton= (props) => ( <TB {...{ icon: <SettingsOutlinedIcon/>, ...props}}/>);


const ZoomBase= ({type,size=22, iconSize}) => (
    <Box sx={{width:size, height:size, position:'relative', overflow:'hidden'}}>
        <ZBaseIcon viewBox={'2 2 15 15'}
                   sx={{width:iconSize??size, height:iconSize??size, position: 'absolute', top: 0, left:0}}/>
        {type}
    </Box>
);



export const ZoomUpIcon= () => (
    <ZoomBase type={<PlusIcon viewBox={'-1 -1 24 24'}
                              sx={{position: 'absolute', width:21, height:21, left: 0, top: 0,
                                  transform: 'scale(.65)'}}/>}/>);

const ZoomDownIcon= () => (
    <ZoomBase type={<MinusIcon viewBox={'-1 -1 24 24'}
                               sx={{position: 'absolute', width:21, height:21, left: 0, top: 0,
                                   transform: 'scale(.65)'}}/>}/>);

export const Zoom1XIcon= ({size,iconSize, sx}) => (
    <ZoomBase size={size} iconSize={iconSize}
              type={<OneXIcon
                  sx={{position: 'absolute', width:21, height:21, left: '.1rem', top: 1,
                      transform: 'scale(.75)', ...sx}}/>}/>);


const ZoomFitIcon= () => (
    <ZoomBase type={<FitIcon viewBox={'1 1 24 24'}
                             sx={{position: 'absolute', width:21, height:21, left: 1, top: 1,
                                 transform: 'scale(.66)'}}/>}/>);

const ZoomFillIcon= () => (
    <ZoomBase type={<FillIcon
        sx={{position: 'absolute', width:21, height:21, left: 0, top: 1,
            transform: 'rotate(45deg) scale(.6)  translate(0px, -1px)'}}/>}/>);

const TB= ({icon, ...props}) => (<ToolbarButton {...{ icon, iconButtonSize:'42px', ...props}}/>);



export const ZoomUpButton = (props) => ( <ToolbarButton {...{icon: <ZoomUpIcon/>, ...props}}/> );
export const ZoomDownButton = (props) => ( <ToolbarButton {...{icon: <ZoomDownIcon/>, ...props}}/> );
export const Zoom1XButton = (props) => ( <ToolbarButton {...{icon: <Zoom1XIcon/>, ...props}}/> );
export const ZoomFitButton = (props) => ( <ToolbarButton {...{icon: <ZoomFitIcon/>, ...props}}/> );
export const ZoomFillButton = (props) => ( <ToolbarButton {...{icon: <ZoomFillIcon/>, ...props}}/> );



export function DisplayTypeButtonGroup({config, variant='outlined', size='sm', value, sx}) {

    const lookup= (v) => {
        switch (v) {
            case 'one' :
            case 'single' :
                return <SquareRoundedIcon/>;
            case 'grid' : return <GridViewRoundedIcon/>;
            case 'gridFull' : return <ViewModuleRoundedIcon/>;
            case 'gridRelated' : return <ViewComfyAltRoundedIcon/>;
        }
    };

    return (
        <ToggleButtonGroup {...{variant, size, value, sx}}>
            {config.map( ({value,title,onClick}) =>
                (<IconButton {...{value, title, onClick, key:value,
                            sx:{ '--IconButton-size':'38px', minHeight:'unset', minWidth:'unset', p:1/4} }}>
                    {lookup(value)}
                </IconButton>)
            )}
        </ToggleButtonGroup>
    );
}

// moved from FormPanel
export function TextButton({text, ...props}) {
    return (
        <Button size='md' {...props}>{text}</Button>
    );
}
TextButton.propTypes = {
    text: node,
    ...Button.propTypes
};