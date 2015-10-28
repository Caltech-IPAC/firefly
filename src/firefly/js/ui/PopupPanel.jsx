/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {getRootURL, getAbsoluteLeft, getAbsoluteTop} from '../util/BrowserUtil.js';
import _ from 'lodash';
import Enum from 'enum';
import React from 'react/addons';
import {getPopupPosition, humanStart, humanMove, humanStop } from './PopupPanelHelper.js';


const LayoutType= new Enum(['CENTER', 'TOP_CENTER', 'NONE', 'USER_POSITION']);

var PopupPanel= React.createClass(
{
    browserResizeCallback : null,
    mouseCtx: null,
    titleBarRef: null,
    moveCallback : null,
    buttonUpCallback : null,

    propTypes: {
        layoutPosition : React.PropTypes.object.isRequired,
        title : React.PropTypes.string,
        closePromise : React.PropTypes.object,
        closeCallback : React.PropTypes.func,
        visible : React.PropTypes.bool.isRequired
    },

    //onClick: function(ev) {
    //    this.doClose();
    //},

    updateLayoutPosition() {
        var e= React.findDOMNode(this);

        var activeLayoutType = LayoutType.TOP_CENTER;
        //var {posX,posY}= getPopupPosition(e,lt);
        var r= getPopupPosition(e,activeLayoutType);
        //e.style.left= results.left;
        //e.style.top= results.top;
        this.setState({activeLayoutType, posX:r.left, posY:r.top });
        //e.style.visibility="visible";

    },

    getInitialState() {
        return {
            activeLayoutType: LayoutType.NONE,
            posX: 0,
            posY: 0
            };
    },



    componentWillUnmount() {
        window.removeEventListener('resize', this.browserResizeCallback);
        document.removeEventListener('mousemove', this.moveCallback);
        document.removeEventListener('mouseup', this.buttonUpCallback);
    },


    componentDidMount() {
        var {visible}= this.props;
        this.moveCallback= (ev)=> this.dialogMove(ev);
        this.buttonUpCallback= (ev)=> this.dialogMoveEnd(ev);
        this.browserResizeCallback= _.debounce(() => { this.updateLayoutPosition(); },150);
        var e= React.findDOMNode(this);
        if (visible) this.updateLayoutPosition();
        //_.defer(function() {
        //    this.computeDir(e);
        //}.bind(this));
        window.addEventListener('resize', this.browserResizeCallback);
        document.addEventListener('mousemove', this.moveCallback);
        document.addEventListener('mouseup', this.buttonUpCallback);
        if (this.props.closePromise) {
            this.props.closePromise.then(()=>  {
                console.log('now closing dialog');
                this.doClose();
            });
        }
    },

    doClose() {
        if (this.props.closeCallback) this.props.closeCallback();
        console.log('close dialog');
    },

    dialogMoveStart(ev)  {
        var e= React.findDOMNode(this);
        var titleBar= React.findDOMNode(this.titleBarRef);
        this.mouseCtx= humanStart(ev,e,titleBar);
    },

    dialogMove(ev)  {
        if (this.mouseCtx) {
            var titleBar= React.findDOMNode(this.titleBarRef);
            var r = humanMove(ev,this.mouseCtx,titleBar);
            if (r) {
                this.setState({posX:r.newX, posY:r.newY});
            }
        }
    },

    dialogMoveEnd(ev)  {
        this.mouseCtx= humanStop(ev,this.mouseCtx);
        this.mouseCtx= null;
    },


    renderAsTopHeader() {

        var rootStyle= {position: 'absolute',
            //width : "100px",
            //height : "100px",
            //background : 'white',
            visibility : this.state.activeLayoutType===LayoutType.NONE ? 'hidden' : 'visible',
            //left : '40px',
            //right : '170px'
            left : `${this.state.posX}px`,
            top : `${this.state.posY}px`
        };



        var title= this.props.title||'';
        //var newChildren= React.Children.map(this.props.children, (c) => {
        //        return React.cloneElement(c, {closeDialog:this.doClose.bind(this)})
        //    })

        return (
                <div style={rootStyle} className={'popup-pane-shadow disable-select'}
                     onTouchStart={this.dialogMoveStart}
                     onTouchMove={this.dialogMove}
                     onTouchEnd={this.dialogMoveEnd} >
                    <div className={'standard-border'}>
                        <div style={{position:'relative', height:'14px', width:'100%', cursor:'default'}}
                             className={'title-bar title-color popup-title-horizontal-background'}
                            onTouchStart={this.dialogMoveStart}
                            onTouchMove={this.dialogMove}
                            onTouchEnd={this.dialogMoveEnd}
                            onMouseDownCapture={this.dialogMoveStart}>
                            <div ref={(c) => this.titleBarRef=c}
                                 style= {{position:'absolute', left:'0px', top:'0px',width:'100%', padding: '3px 0 3px 10px'}}
                                 onMouseDownCapture={this.dialogMoveStart}
                                 onTouchStart={this.dialogMoveStart}
                                 onTouchMove={this.dialogMove}
                                 onTouchEnd={this.dialogMoveEnd}
                                 className={'title-label'} >
                                {title}
                            </div>
                            <image className={'popup-header'}
                                   src= {`${getRootURL()}images/blue_delete_10x10.gif`}
                                   style= {{position:'absolute', right:'0px', top:'0px'}}
                                   onClick={this.doClose} />

                        </div>
                        <div style={{display:'table'}}>
                            {this.props.children}
                        </div>
                    </div>
                </div>
        );

    },


    render() {
        if (this.props.visible) {
            return  this.renderAsTopHeader();
        }
        else {
            return false;
        }
    }

});

export default PopupPanel;
