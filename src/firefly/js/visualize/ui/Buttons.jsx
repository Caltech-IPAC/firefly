import BiotechOutlinedIcon from '@mui/icons-material/BiotechOutlined.js';
import {Box} from '@mui/joy';
import React from 'react';
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
import ContentCopySharpIcon from '@mui/icons-material/ContentCopySharp';
import LinkIcon from '@mui/icons-material/Link';
import LinkOffIcon from '@mui/icons-material/LinkOff';

import LockOpenOutlinedIcon from '@mui/icons-material/LockOpenOutlined';
import LockOutlinedIcon from '@mui/icons-material/Lock';
import PanoramaHorizontalOutlinedIcon from '@mui/icons-material/PanoramaHorizontalTwoTone';


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




export const SaveButton= (props) => (
    <ToolbarButton {...{ icon: <SaveOutlinedIcon/>, iconButtonSize:'42px', ...props,  }}/>);

export const InfoButton= (props) => (
    <ToolbarButton {...{ icon: <InfoOutlinedIcon/>, iconButtonSize:'42px', ...props,  }}/>);

export const CenterDropdown= (props) => (
    <DropDownToolbarButton {...{ icon: <CenterFocusWeakOutlinedIcon/>, useDropDownIndicator: true,
        dropPosition:{left:3,bottom:-2},
        iconButtonSize:'40px', ...props,  }}/>);

export const ColorDropDownButton= (props) => (
    <DropDownToolbarButton {...{ icon: <ColorLens/>, useDropDownIndicator: true,
        dropPosition:{left:1,bottom:-2},
        iconButtonSize:'40px', ...props,  }}/>);

export const ColorButtonIcon= (props) => (<ToolbarButton {...{ icon: <ColorLens/>, iconButtonSize:'40px', ...props,  }}/>);

export const RotateButton= (props) => (
    <ToolbarButton {...{ icon: <ThreeSixtyRoundedIcon/>, iconButtonSize:'42px', ...props,  }}/>);

export const DrawLayersButton= (props) => (
    <ToolbarButton {...{ icon: <Layers viewBox={'2 0 22 20'}/>, iconButtonSize:'40px', ...props,  }}/>);

export const RestoreButton= (props) => (
    <ToolbarButton {...{ icon: <ReplayRoundedIcon/>, iconButtonSize:'42px', ...props,  }}/>);

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

export const ExtractPoints= (props) => (
    <ToolbarButton {...{ icon: <GrainIcon/>, iconButtonSize:'42px', ...props,  }}/>);

export const ExtractLine= (props) => (
    <ToolbarButton {...{ icon: <HorizontalRuleRoundedIcon className='extractLine'/>, iconButtonSize:'42px',
        sx:{'& .extractLine': {transform: 'rotate(142deg)'}},
        ...props,  }}/>);

export const SearchDetailButton= (props) => (
    <DropDownToolbarButton {...{ icon: <BiotechOutlinedIcon/>, iconButtonSize:'44px', useDropDownIndicator: true,
        dropPosition: {left: -1, bottom: 0}, ...props
    }}/>);

export const ToolsDropDown= (props) => (
    <DropDownToolbarButton {...{ icon: <ConstructionRoundedIcon className='tools'/>, useDropDownIndicator: true,
        sx:{'& .tools': {transform: 'rotate(9deg) scale(-1.1,1.1)'}},
        dropPosition:{left:6,bottom:-2},
        iconButtonSize:'40px', ...props,  }}/>);

