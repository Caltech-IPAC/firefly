/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {Box, Card, Skeleton, Typography} from '@mui/joy';
import React, {useEffect, memo, useState} from 'react';
import PropTypes, {bool, object, shape, string} from 'prop-types';
import {CompleteButton} from '../../ui/CompleteButton.jsx';
import {checkProps} from '../../ui/SimpleComponent';
import BrowserInfo from '../../util/BrowserInfo.js';

export const ctxBG= (theme, opacity=80) =>
    BrowserInfo.supportsCssColorMix() ?
        `color-mix(in srgb, ${theme.vars.palette.warning.softBg} ${opacity}%, transparent)` :
        theme.vars.palette.neutral.softBg;





export const ImageViewerStatus= memo(
    ({message='',working,useMessageAlpha=false, buttonCB, buttonText='OK', messageWaitTimeMS=0, maskWaitTimeMS=0, top=0} ) => {

    const [showing, setShowing]= useState( { messageShowing:messageWaitTimeMS<=0, maskShowing:maskWaitTimeMS<=0 });

    useEffect(() => {
        if (showing.messageShowing && showing.maskShowing) return;
        if (messageWaitTimeMS===0 && !showing.messageShowing) setShowing({...showing,messageShowing:true});
        let alive= true;
        const timeOuts= [
            messageWaitTimeMS>0 && {name:'messageShowing', wait:messageWaitTimeMS},
            maskWaitTimeMS>0 && {name: 'maskShowing', wait:maskWaitTimeMS},
        ].filter( (t) => t).sort((t1,t2) => t1.wait-t2.wait);

        const handleTimeout= () => {
            if (!alive) return;
            const timeout= timeOuts.shift();
            setShowing({...showing, [timeout.name]:true});
            timeOuts[0] && window.setTimeout( handleTimeout, timeOuts[0].wait);
        };
        timeOuts[0] && window.setTimeout( handleTimeout, timeOuts[0].wait);
        return () => void (alive= false);
    }, [messageWaitTimeMS, maskWaitTimeMS] );

    return (
        <ImageViewStatusPanel {...{
            maskShowing:showing.maskShowing&&working, messageShowing:showing.messageShowing, useMessageAlpha,
            sx:{top},
            slotProps :{
                button: { text:buttonText, onClick: buttonCB, sx: {}},
                message: { text: message}
            }
        }}/>
    );
});



ImageViewerStatus.displayName = 'ImageViewerStatus';
ImageViewerStatus.propTypes= {
    message: PropTypes.string,
    working: PropTypes.bool.isRequired,
    messageWaitTimeMS: PropTypes.number,
    maskWaitTimeMS: PropTypes.number,
    useMessageAlpha: PropTypes.bool,
    buttonCB: PropTypes.func,
    buttonText: PropTypes.string
};

export function ImageViewStatusPanel(props) {
    const {maskShowing=false, messageShowing, useMessageAlpha, sx, slotProps={}}=
        checkProps(props,ImageViewStatusPanel);

    const {sx:messageSx={}, text:messageText=''}= slotProps.message ?? {};
    const {sx:buttonSx={}, onClick:buttonCB, text:buttonText='OK'}= slotProps.button ?? {};

    const finalMsgSx= { py: 1, textAlign:'center', flex:buttonCB ?'10 10 auto' : '1 1 auto' , ...messageSx};
    const statusTextSx= {
        position:'absolute', left:0, top:0, width:1, minHeight : '15%', color:'black', display:'flex',
        alignItems:'center', justifyContent:'flex-start', flexDirection:'row', zIndex: maskShowing ? 10 : 'auto'
    };

    return (
        <Box sx={{position:'absolute', top: maskShowing && messageShowing? 2: 0, left:0, width:'100%', height:'100%', ...sx}}>
            {maskShowing && <Box sx={{ position:'absolute', left:0, top:0, width:1, height:1}}> <Skeleton/> </Box> }
            { messageShowing &&
                <Card {...{
                    color:'warning', variant:'soft', position:'relative',
                    zIndex: maskShowing ? 10 : 'auto',
                    sx: (theme) =>
                        !useMessageAlpha ? statusTextSx : { ...statusTextSx, backgroundColor: ctxBG(theme,65)}
                }}>
                    <Typography level='body-lg' sx={finalMsgSx}>{messageText}</Typography>
                    { buttonCB && <CompleteButton text={buttonText} sx={{flex: '2 2 auto',...buttonSx}} onSuccess={buttonCB}/> }
                </Card>
            }
        </Box>
    );
}

ImageViewStatusPanel.propTypes= {
    maskShowing: bool,
    messageShowing: bool,
    useMessageAlpha: bool,
    sx: object,
    slotProps: shape({
        message: shape({
            sx: object,
            text: string,
        }),
        button: shape({
            sx: object,
            text: string,
            okClick: Function,
        })
    })
};
