/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import {omit} from 'lodash';
import {CompleteButton} from '../../ui/CompleteButton.jsx';

const statusContainer= {
    position:'absolute',
    top: 0,
    left:0,
    width:'100%',
    height:'100%'
};

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
    zIndex:2,
    // backgroundColor: '#e3e3e3'
    backgroundColor: 'rgba(200,200,200,1)'
};

const statusTextCell= {
    paddingTop:'10px',
    textAlign:'center',
    flex:'1 1 auto'
};

const maskWrapper= {
    position:'absolute',
    left:0,
    top:0,
    width:'100%',
    height:'100%'
};


const statusTextAlpha= Object.assign({},statusText,  {backgroundColor: 'rgba(200,200,200,.8)'});
const statusTextCellWithClear= Object.assign({},statusTextCell,  {flex: '10 10 auto'});

export class ImageViewerStatus extends Component {
    constructor(props) {
        super(props);
        const {messageWaitTimeMS=0, maskWaitTimeMS=0} = this.props;
        this.startTime= Date.now();
        const messageShowing= messageWaitTimeMS<=0;
        const maskShowing= maskWaitTimeMS<=0;
        this.state= {messageShowing, maskShowing};
        if (!messageShowing || !maskShowing) this.manageTimer();
    }


    componentDidMount() {
        this.iAmMounted= true;
    }
    
    componentWillUnmount() {
        this.iAmMounted= false;
    }

    manageTimer() {
        const {messageWaitTimeMS=0, maskWaitTimeMS=0} = this.props;
        var timeOuts= [];
        if (messageWaitTimeMS>0) timeOuts.push({name:'messageShowing', wait:messageWaitTimeMS});
        if (maskWaitTimeMS>0) timeOuts.push({name: 'maskShowing', wait:maskWaitTimeMS});
        timeOuts= timeOuts.sort((t1,t2) => t1.wait-t2.wait);

        const handleTimeout= () => {
            const timeout= timeOuts.shift();
            if (this.iAmMounted) this.setState({[timeout.name]:true});
            if (timeOuts[0]) window.setTimeout( handleTimeout, timeOuts[0].wait);
        };

        window.setTimeout( handleTimeout, timeOuts[0].wait);
    }

    render() {
        const {message,working,useMessageAlpha=false, canClear=false, clearCB} = this.props;
        const {messageShowing, maskShowing}= this.state;

        var workingStatusText= useMessageAlpha ? statusTextAlpha : statusText;
        const workingStatusTextCell= canClear ? statusTextCellWithClear : statusTextCell;

        if (!working && !canClear) workingStatusText= omit(workingStatusText,'zIndex');

        return (
            <div style={statusContainer}>
                {working && maskShowing &&
                     <div style={maskWrapper}>
                         <div className='loading-mask'></div>
                     </div>
                }
                { messageShowing &&
                     <div style={workingStatusText} >
                         <div style={workingStatusTextCell}>{message}</div>
                         { canClear && <CompleteButton style={{flex: '2 2 auto'}} onSuccess={clearCB}/> }
                     </div>
                }
            </div>
        );
    }
}

ImageViewerStatus.propTypes= {
    message : PropTypes.string.isRequired,
    working : PropTypes.bool.isRequired,
    messageWaitTimeMS : PropTypes.number,
    maskWaitTimeMS : PropTypes.number,
    useMessageAlpha : PropTypes.bool,
    canClear : PropTypes.bool,
    clearCB : PropTypes.string
};
