/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Componennt, PropTypes} from 'react';
import {getRootURL} from '../util/BrowserUtil.js';
import {debounce} from 'lodash';
import Enum from 'enum';
import ReactDOM from 'react-dom';
import {getPopupPosition, humanStart, humanMove, humanStop } from './PopupPanelHelper.js';
import './PopupPanel.css';

import DEL_ICO from 'html/images/blue_delete_10x10.png';


export const LayoutType= new Enum(['CENTER', 'TOP_EDGE_CENTER', 'TOP_CENTER', 'TOP_LEFT', 'TOP_RIGHT', 'NONE', 'USER_POSITION']);

export var PopupPanel= React.createClass(
{
    browserResizeCallback : null,
    mouseCtx: null,
    titleBarRef: null,
    moveCallback : null,
    buttonUpCallback : null,

    propTypes: {
        layoutPosition : PropTypes.object,
        title : PropTypes.string,
        closePromise : PropTypes.object,
        requestToClose : PropTypes.func,
        requestOnTop : PropTypes.func,
        closeCallback : PropTypes.func,
        visible : PropTypes.bool,
        dialogId : PropTypes.string,
        zIndex : PropTypes.number,
        mouseInDialog : PropTypes.func
    },

    getDefaultProps() {
        return {
            layoutPosition : LayoutType.TOP_CENTER
        };
    },

    updateLayoutPosition() {
        var e= ReactDOM.findDOMNode(this);

        var activeLayoutType = this.props.layoutPosition;
        setTimeout( () => {
            var r= getPopupPosition(e,activeLayoutType);
            this.setState({activeLayoutType, posX:r.left, posY:r.top });
        },10);

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
        if (this.props.closeCallback) this.props.closeCallback();
    },


    componentDidMount() {
        var {visible}= this.props;
        this.moveCallback= (ev)=> this.dialogMove(ev);
        this.buttonUpCallback= (ev)=> this.dialogMoveEnd(ev);
        this.browserResizeCallback= debounce(() => { this.updateLayoutPosition(); },150);
        var e= ReactDOM.findDOMNode(this);
        if (visible) this.updateLayoutPosition();
        window.addEventListener('resize', this.browserResizeCallback);
        document.addEventListener('mousemove', this.moveCallback);
        document.addEventListener('mouseup', this.buttonUpCallback);
        if (this.props.closePromise) {
            this.props.closePromise.then(()=>  {
                this.askParentToClose();
            });
        }
    },

    askParentToClose() {
        if (this.props.requestToClose) this.props.requestToClose();
    },

    dialogMoveStart(ev)  {
        const {requestOnTop,dialogId}= this.props;
        requestOnTop && requestOnTop(dialogId);
        var e= ReactDOM.findDOMNode(this);
        var titleBar= ReactDOM.findDOMNode(this.titleBarRef);
        this.mouseCtx= humanStart(ev,e,titleBar);
    },

    dialogMove(ev)  {
        if (this.mouseCtx) {
            var titleBar= ReactDOM.findDOMNode(this.titleBarRef);
            var r = humanMove(ev,this.mouseCtx,titleBar);
            if (r) {
                this.setState({posX:r.newX, posY:r.newY});
            }
        }
    },

    onMouseEnter() {
        this.props.mouseInDialog && this.props.mouseInDialog(true);
    },

    onMouseLeave() {
        this.props.mouseInDialog && this.props.mouseInDialog(false);
    },

    dialogMoveEnd(ev)  {
        this.mouseCtx= humanStop(ev,this.mouseCtx);
        this.mouseCtx= null;
    },


    renderAsTopHeader() {

        var rootStyle= {position: 'absolute',
            visibility : this.state.activeLayoutType===LayoutType.NONE ? 'hidden' : 'visible',
            left : this.state.posX,
            top : this.state.posY,
            zIndex:this.props.zIndex
        };



        var title= this.props.title||'';

        return (
                <div style={rootStyle} className={'popup-panel-shadow disable-select'}
                     onTouchStart={this.dialogMoveStart}
                     onTouchMove={this.dialogMove}
                     onTouchEnd={this.dialogMoveEnd}
                     onMouseEnter={this.onMouseEnter}
                     onMouseLeave={this.onMouseLeave}
                >
                    <div className={'standard-border'}>
                        <div style={{position:'relative', height:'14px', width:'100%', cursor:'default'}}
                             className={'title-bar title-color popup-panel-title-background'}
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
                                <div className={'text-ellipsis'} style={{width:'80%'}}>
                                    {title}
                                </div>
                            </div>
                            <img className='popup-panel-header'
                                 src= {DEL_ICO}
                                 style= {{position:'absolute', right:0, top:0}}
                                 onClick={this.askParentToClose} />

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
