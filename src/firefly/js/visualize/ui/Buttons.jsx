import BiotechOutlinedIcon from '@mui/icons-material/BiotechOutlined.js';
import {Box} from '@mui/joy';
import React from 'react';
import OpenInFull from '@mui/icons-material/OpenInFull';
import GRID_EXPAND from 'images/icons-2014/24x24_ExpandArrows-grid-3.png';
import {DropDownToolbarButton} from '../../ui/DropDownToolbarButton.jsx';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';
import LockOpenOutlinedIcon from '@mui/icons-material/LockOpenOutlined';
import LockOutlinedIcon from '@mui/icons-material/Lock';
import PanoramaHorizontalOutlinedIcon from '@mui/icons-material/PanoramaHorizontalTwoTone';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined';
import CenterFocusWeakOutlinedIcon from '@mui/icons-material/CenterFocusWeakOutlined';
import RotateRight from '@mui/icons-material/RotateRight';
import ColorLens from '@mui/icons-material/ColorLensOutlined';
import Layers from '@mui/icons-material/Layers';

// import GRID_EXPAND from 'images/icons-2014/24x24_ExpandArrows-grid-3.png';
// import OUTLINE_EXPAND from 'images/icons-2014/24x24_ExpandArrowsWhiteOutline.png';
// import COLOR from 'images/icons-2014/28x28_ColorPalette.png';
// import FITS_HEADER from 'images/icons-2014/28x28_FITS_Information.png';
// import ROTATE from 'images/icons-2014/Rotate.png';
// import BINOCULARS from 'images/b4.png';
// import SAVE from 'images/icons-2014/Save.png';
// import LAYER_ICON from 'images/icons-2014/TurnOnLayers.png';


export const ExpandButton= ({expandGrid, ...props}) =>(
    <ToolbarButton {...{
        icon: (expandGrid? GRID_EXPAND : <OpenInFull sx={{transform: 'scale(1.1,-1.1)'}}/>), ...props }}/>);


export const LockImages= ({locked, ...props}) =>{
    const lockSx={position:'absolute', left:'10px', bottom:'2px'};
    const icon= (
        <Box sx={{width:24,height:24}}>
            <PanoramaHorizontalOutlinedIcon sx={{position:'absolute', top:'2px', left:'3px'}}/>
            {locked ? <LockOutlinedIcon sx={lockSx}/> :
                <LockOpenOutlinedIcon sx={lockSx}/>}
        </Box>
    );

    return (<DropDownToolbarButton {...{ icon, ...props, }}  />);
};

export const SaveButton= (props) => (
    <ToolbarButton {...{ icon: <SaveOutlinedIcon/>, iconButtonSize:'44px', ...props,  }}/>);

export const InfoButton= (props) => (
    <ToolbarButton {...{ icon: <InfoOutlinedIcon/>, iconButtonSize:'44px', ...props,  }}/>);

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
    <ToolbarButton {...{ icon: <RotateRight/>, iconButtonSize:'44px', ...props,  }}/>);

export const DrawLayersButton= (props) => (
    <ToolbarButton {...{ icon: <Layers viewBox={'2 0 22 20'}/>, iconButtonSize:'40px', ...props,  }}/>);

export const SearchDetailButton= (props) => (
    <DropDownToolbarButton {...{ icon: <BiotechOutlinedIcon/>, iconButtonSize:'44px', useDropDownIndicator: true,
        dropPosition: {left: -1, bottom: 0}, ...props
    }}/>);

