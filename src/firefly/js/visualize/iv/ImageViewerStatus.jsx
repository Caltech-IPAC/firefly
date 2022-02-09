/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {useEffect, memo, useState} from 'react';
import PropTypes from 'prop-types';
import {CompleteButton} from '../../ui/CompleteButton.jsx';

const statusText= {
    position:'absolute',
    left:0,
    top:0,
    width:'100%',
    minHeight : '15%',
    fontSize:'12pt',
    color:'black', 
    display:'flex',
    alignItems:'center',
    justifyContent:'flex-start',
    flexDirection:'row',
    backgroundColor: 'rgba(200,200,200,1)'
};

const statusContainer= { position:'absolute', top: 0, left:0, width:'100%', height:'100%' };
const statusTextCell= { paddingTop:10, paddingBottom:10, textAlign:'center', flex:'1 1 auto' };
const maskWrapper= { position:'absolute', left:0, top:0, width:'100%', height:'100%' };
const statusTextAlpha= {...statusText,  backgroundColor: 'rgba(200,200,200,.8)'};
const statusTextCellWithClear= {...statusTextCell,  flex: '10 10 auto'};

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

    const workingStatusText= useMessageAlpha ? statusTextAlpha : statusText;
    const workingStatusTextCell= buttonCB ? statusTextCellWithClear : statusTextCell;

    return (
        <div style={{...statusContainer, top}}>
            {working && showing.maskShowing && <div style={maskWrapper}> <div className='loading-mask'/> </div> }
            { showing.messageShowing &&
            <div className='enable-select' style={workingStatusText} >
                <div style={workingStatusTextCell}>{message}</div>
                { buttonCB && <CompleteButton text={buttonText} style={{flex: '2 2 auto'}} onSuccess={buttonCB}/> }
            </div>
            }
        </div>
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
