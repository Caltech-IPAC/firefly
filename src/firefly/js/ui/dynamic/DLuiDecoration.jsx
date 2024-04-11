import {Box, Button, Stack, Tooltip, Typography} from '@mui/joy';
import {bool, element, func, number, string} from 'prop-types';
import React from 'react';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';

import KeyboardDoubleArrowRight from '@mui/icons-material/KeyboardDoubleArrowRight';
import KeyboardDoubleArrowLeft from '@mui/icons-material/KeyboardDoubleArrowLeft';

const DEFAULT_SIDE_BAR_WIDTH= 470;



export function DLSearchTitle({desc, isAllSky, sideBarShowing, setSideBarShowing,
                         allSkyAdditionalLabel= '(Covers Whole Sky)',
                         showOtherDataLabel= 'Show Other Data Collections'}) {
    const titleDiv= (
        <span>
            { !isAllSky ? desc :
                <>
                    <span>{desc}</span>
                    <Typography color='warning' level='body-lg' sx={{pl: 2}}>
                        {allSkyAdditionalLabel}
                    </Typography>
                </> }
        </span>
    );

    if (sideBarShowing) {
        return (
            <Stack {...{direction:'row', alignItems:'center', justifyContent:'center'}}>
                <Typography level='h3'> {titleDiv} </Typography>
            </Stack>
        );
    }
    else {
        return (
            <Stack {...{direction:'row', alignItems:'center', justifyContent:'flex-start'}}>
                <ExpandButton icon={<KeyboardDoubleArrowRight/>} text={showOtherDataLabel} onClick={()  => setSideBarShowing(true)}/>
                <Stack {...{direction:'row', flexGrow:1, justifyContent:'center'}}>
                    <Typography level='h3'> {titleDiv} </Typography>
                </Stack>
            </Stack>
        );

    }
}


DLSearchTitle.propType= {
    desc: string,
    isAllSky: bool,
    sideBarShowing:bool,
    setSideBarShowing: func,
    allSkyAdditionalLabel: string,
    showOtherDataLabel: string,
};




export function SideBarTable({registryTblId,
                          setSideBarShowing,
                          hideLabel= 'Hide',
                          hideTip= 'Hide data collections chooser',
                          chooserTitle= 'Choose Data Collection',
                          sideBarWidth= DEFAULT_SIDE_BAR_WIDTH,
                          chooserDetails= 'Click on data collection to search; filter or sort table to find a data collection.',
                      }) {
    return (
        <Stack>
            <Stack {...{direction:'row', alignItems:'center', spacing:5}}>
                {<ExpandButton icon={<KeyboardDoubleArrowLeft/>} text={hideLabel} tip={hideTip}
                               onClick={()  => setSideBarShowing(false)}/> }
                <Typography level='h3'>
                    {chooserTitle}
                </Typography>
            </Stack>
            <Box style={{minWidth:sideBarWidth, flexGrow:1, pt:1/2, pr:1/2, pl:1/4}}>
                <TablePanel {...{
                    key:registryTblId, tbl_id:registryTblId,
                    showToolbar: false, selectable:false, showFilters:true, showOptions: false, showUnits: false,
                    showTypes: false, textView: false, showOptionButton: false
                }}/>
            </Box>
            <Typography level='body-sm'>{chooserDetails}</Typography>
        </Stack>
    );
}

SideBarTable.propType= {
    registryTblId: string,
    setSideBarShowing: func,
    hideLabel: string,
    hideTip: string,
    chooserTitle: string,
    sideBarWidth: number,
    chooserDetails: string,
};


export function SideBarAnimation({sideBar,sideBarShowing,
                              sideBarWidth=DEFAULT_SIDE_BAR_WIDTH,
                              sideTransition= 'all .5s ease-in-out'}) {
    const w= sideBarShowing?sideBarWidth:0;
    return (
        <Stack {...{direction:'row', sx:{minWidth:w, width:w, overflow:'hidden', transition:sideTransition}}} >
            {sideBar}
        </Stack>
    );
}

SideBarAnimation.propTypes= {
    sideBar: element,
    sideBarShowing: bool,
    sideBarWidth: number,
    sideTransition: string,
};



function ExpandButton({text, icon, tip='$text',onClick}) {
    return (
        <Tooltip title={tip}>
            <Button {...{variant:'plain', color:'neutral', onClick,
                startDecorator:icon
            }}>
                {text}
            </Button>

        </Tooltip>
    );
}
