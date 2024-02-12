import React from 'react';
import {Box, ToggleButtonGroup} from '@mui/joy';

// TODO: we should use icon from one type(filled, outlined, rounded, two-tone, sharp).  It will yield better consistency.

import BiotechOutlinedIcon from '@mui/icons-material/BiotechOutlined.js';
import OpenInFull from '@mui/icons-material/OpenInFull';
import GRID_EXPAND from 'images/icons-2014/24x24_ExpandArrows-grid-3.png';
import {DropDownToolbarButton} from '../../ui/DropDownToolbarButton.jsx';
import {SimpleLayerOnOffButton} from './SimpleLayerOnOffButton.jsx';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import LockOpenTwoToneIcon from '@mui/icons-material/LockOpenTwoTone';
import LockTwoToneIcon from '@mui/icons-material/LockTwoTone';
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
import ReplayRoundedIcon from '@mui/icons-material/ReplayRounded';
import ThreeSixtyRoundedIcon from '@mui/icons-material/ThreeSixtyRounded';
import LinkIcon from '@mui/icons-material/Link';
import LinkOffIcon from '@mui/icons-material/LinkOff';
import SquareRoundedIcon from '@mui/icons-material/SquareRounded';
import GridViewRoundedIcon from '@mui/icons-material/GridViewRounded';
import FormatListBulletedRoundedIcon from '@mui/icons-material/FormatListBulletedRounded';
import CenterFocusStrongTwoToneIcon from '@mui/icons-material/CenterFocusStrongTwoTone';

import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import ViewCompactIcon from '@mui/icons-material/ViewCompact';
import CropRoundedIcon from '@mui/icons-material/CropRounded';
import DoneAllRoundedIcon from '@mui/icons-material/DoneAllRounded';
import RemoveDoneRoundedIcon from '@mui/icons-material/RemoveDoneRounded';
import FunctionsOutlinedIcon from '@mui/icons-material/FunctionsOutlined';

import FilterIco from '@mui/icons-material/FilterAltOutlined';
import ClearFilterIco from '@mui/icons-material/FilterAltOffOutlined';
import TextViewIco from '@mui/icons-material/TextFieldsOutlined';
import TableViewIco from '@mui/icons-material/TableChartOutlined';
import AddColumnIco from '@mui/icons-material/PostAddOutlined';
import SettingsIco from '@mui/icons-material/SettingsOutlined';
import PropertySheetIco from '@mui/icons-material/ReadMoreOutlined';
import ResetIco from '@mui/icons-material/RestartAltOutlined';
import PanIco from '@mui/icons-material/OpenWithOutlined';
import PinChartIco from '@mui/icons-material/PushPin';
import ShowTableIco from '@mui/icons-material/CallReceived';
import CombineChartIco from '@mui/icons-material/SyncAlt';


// import ContentCopySharpIcon from '@mui/icons-material/ContentCopySharp';
// import GridViewIcon from '@mui/icons-material/GridView';
// import ViewListIcon from '@mui/icons-material/ViewList';
// import LockOpenOutlinedIcon from '@mui/icons-material/LockOpenOutlined';
// import LockOutlinedIcon from '@mui/icons-material/Lock';
// import PanoramaHorizontalOutlinedIcon from '@mui/icons-material/PanoramaHorizontalTwoTone';
// import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined';
// import FilterAltTwoToneIcon from '@mui/icons-material/FilterAltTwoTone';
// import FilterAltOffTwoToneIcon from '@mui/icons-material/FilterAltOffTwoTone';
// --- save the following as other options for certain icons
// import RotateRight from '@mui/icons-material/RotateRight';
// import SettingsBackupRestoreRoundedIcon from '@mui/icons-material/SettingsBackupRestoreRounded';

// --- save the following as reference where the old icons came from
// import FLIP_Y_ON from 'images/icons-2014/Mirror-ON.png';
// import FLIP_Y from 'images/icons-2014/Mirror.png';
// import GRID_EXPAND from 'images/icons-2014/24x24_ExpandArrows-grid-3.png';
// import OUTLINE_EXPAND from 'images/icons-2014/24x24_ExpandArrowsWhiteOutline.png';
// import COLOR from 'images/icons-2014/28x28_ColorPalette.png';
// import FITS_HEADER from 'images/icons-2014/28x28_FITS_Information.png';
// import ROTATE from 'images/icons-2014/Rotate.png';
// import BINOCULARS from 'images/b4.png';
// import SAVE from 'images/icons-2014/Save.png';
// import LAYER_ICON from 'images/icons-2014/TurnOnLayers.png';
// import DIST_ON from 'images/icons-2014/Measurement-ON.png';
// import DIST_OFF from 'images/icons-2014/Measurement.png';
// import FLIP_Y_ON from 'images/icons-2014/Mirror-ON.png';
// import FLIP_Y from 'images/icons-2014/Mirror.png';
// import RESTORE from 'images/icons-2014/RevertToDefault.png';
// import LINE_EXTRACTION from 'images/line-extract.png';
// import POINT_EXTRACTION from 'images/points.png';
// import TOOL_DROP from 'images/tools-again-try2.png';
// import ZOOM_DROP from 'images/Zoom-drop.png';


export const ExpandButton= ({expandGrid, ...props}) =>(
    <ToolbarButton {...{
        icon: (expandGrid? GRID_EXPAND : <OpenInFull sx={{transform: 'scale(1.1,-1.1)'}}/>), ...props }}/>);


// export const LockImages= ({locked, ...props}) =>{
//     const lockSx={position:'absolute', left:'12px', bottom:'0px', transform: 'scale(.9,.9)'};
//     const icon= (
//         <Box sx={{width:24,height:24}}>
//             <ContentCopySharpIcon sx={{position:'absolute', transform: 'scale(.85,.8)', top:'0px', left:'-1px'}}/>
//             {locked ? <LockTwoToneIcon sx={lockSx}/> :
//                 <LockOpenTwoToneIcon sx={lockSx}/>}
//         </Box>
//     );
//
//     return (<DropDownToolbarButton {...{ icon, dropPosition:{left:3,bottom:-2}, ...props, }}  />);
// };

export const LockImages= ({locked, ...props}) =>{
    const sx={position:'absolute', transform: 'scale(1.2,1.2)', top:1, left:4};
    const icon= (
        <Box sx={{width:24,height:24}}>
            {locked ? <LinkIcon {...{sx}}/> : <LinkOffIcon {...{sx}}/>}
        </Box>
    );
    return (<DropDownToolbarButton {...{ icon, dropPosition:{left:3,bottom:-2}, ...props, }}  />);
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
        dropPosition: {left: -1, bottom: 0}, ...props
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

export const ClearFilterButton = (props) => (
    <ToolbarButton {...{icon: <ClearFilterIco/>, tip: 'Remove all filters', iconButtonSize:'38px', ...props}}/>
);

export const TextViewButton = (props) => (
    <ToolbarButton {...{icon: <TextViewIco/>, tip: 'Text View', iconButtonSize:'38px', ...props}}/>
);

export const TableViewButton = (props) => (
    <ToolbarButton {...{icon: <TableViewIco/>, tip: 'Table View', iconButtonSize:'38px', ...props}}/>
);

export const AddColumnButton = (props) => (
    <ToolbarButton {...{icon: <AddColumnIco/>, tip: 'Add a column', iconButtonSize:'38px', ...props}}/>
);

export const SettingsButton = (props) => (
    <ToolbarButton {...{icon: <SettingsIco/>, tip: 'Chart options and tools', iconButtonSize:'38px', ...props}}/>
);

export const PropertySheetButton = (props) => (
    <ToolbarButton {...{icon: <PropertySheetIco/>, iconButtonSize:'38px', ...props}}/>
);

export const ResetButton = (props) => (
    <ToolbarButton {...{icon: <ResetIco/>, iconButtonSize:'38px', ...props}}/>
);

export const PanButton = (props) => (
    <ToolbarButton {...{icon: <PanIco/>, iconButtonSize:'38px', ...props}}/>
);

export const PinChartButton = (props) => (
    <ToolbarButton {...{icon: <PinChartIco/>, iconButtonSize:'38px', ...props}}/>
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

export const RestoreButton= (props) => ( <TB {...{ icon: <ReplayRoundedIcon/>, ...props}}/>);
export const RotateButton= (props) => ( <TB {...{ icon: <ThreeSixtyRoundedIcon/>, ...props,  }}/>);
export const SaveButton= (props) => ( <TB {...{ icon: <SaveOutlinedIcon/>, ...props}}/>);
export const InfoButton= (props) => (<TB {...{ icon: <InfoOutlinedIcon/>, ...props}}/>);
export const ColorButtonIcon= (props) => (<TB {...{ icon: <ColorLens/>, ...props}}/>);
export const OneTileButton= (props) => ( <TB {...{ icon: <SquareRoundedIcon/>, ...props}}/>);
export const GridTileButton= (props) => (<TB {...{ icon: <GridViewRoundedIcon/>, ...props}}/>);
export const GridTileCompactButton= (props) => ( <TB {...{ icon: <ViewCompactIcon/>, ...props}}/>);
export const ListViewButton= (props) => ( <TB {...{ icon: <FormatListBulletedRoundedIcon/>, ...props}}/>);
export const CheckedButton= (props) => ( <TB {...{ icon: <DoneAllRoundedIcon/>, ...props,}}/>);
export const CheckedClearButton= (props) => ( <TB {...{ icon: <RemoveDoneRoundedIcon/>, ...props}}/>);
export const CenterOnSelection= (props) => ( <TB {...{ icon: <CenterFocusStrongTwoToneIcon/>, ...props}}/>);
export const CropButton= (props) => ( <TB {...{ icon: <CropRoundedIcon/>, ...props}}/>);
export const StatsButton= (props) => ( <TB {...{ icon: <FunctionsOutlinedIcon/>, ...props}}/>);

export const FiltersOffButton= (props) => ( <TB {...{ icon: <ClearFilterIco/>, ...props}}/>);

// export const SettingsButton= (props) => ( <TB {...{ icon: <SettingsOutlinedIcon/>, ...props}}/>);

const TB= ({icon, ...props}) => (<ToolbarButton {...{ icon, iconButtonSize:'42px', ...props}}/>);
