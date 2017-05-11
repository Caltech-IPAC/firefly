/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {debounce} from 'lodash';
import Enum from 'enum';
import ReactDOM from 'react-dom';
import {getPopupPosition, humanStart, humanMove, humanStop } from './PopupPanelHelper.js';
import './PopupPanel.css';

import DEL_ICO from 'html/images/blue_delete_10x10.png';


export const LayoutType= new Enum(['CENTER', 'TOP_EDGE_CENTER', 'TOP_CENTER', 'TOP_LEFT', 'TOP_RIGHT', 'NONE', 'USER_POSITION']);

export class PopupPanel extends PureComponent {

    constructor(props) {
        super(props);

        this.state= {
            activeLayoutType: LayoutType.NONE,
            posX: 0,
            posY: 0
            };
        this.browserResizeCallback= null;
        this.mouseCtx= null;
        this.titleBarRef= null;
        this.moveCallback= null;
        this.buttonUpCallback= null;

        this.updateLayoutPosition= this.updateLayoutPosition.bind(this);
        this.askParentToClose= this.askParentToClose.bind(this);
        this.dialogMoveStart= this.dialogMoveStart.bind(this);
        this.dialogMove= this.dialogMove.bind(this);
        this.dialogMoveEnd= this.dialogMoveEnd.bind(this);
        this.onMouseEnter= this.onMouseEnter.bind(this);
        this.onMouseLeave= this.onMouseLeave.bind(this);
        this.renderAsTopHeader= this.renderAsTopHeader.bind(this);
    }



    updateLayoutPosition() {
        const e= ReactDOM.findDOMNode(this);

        const activeLayoutType = this.props.layoutPosition||LayoutType.TOP_CENTER;
        setTimeout( () => {
            const r= getPopupPosition(e,activeLayoutType);
            this.setState({activeLayoutType, posX:r.left, posY:r.top });
        },10);

    }

    componentWillUnmount() {
        window.removeEventListener('resize', this.browserResizeCallback);
        document.removeEventListener('mousemove', this.moveCallback);
        document.removeEventListener('mouseup', this.buttonUpCallback);
        if (this.props.closeCallback) this.props.closeCallback();
    }


    componentDidMount() {
        const {visible}= this.props;
        this.moveCallback= (ev)=> this.dialogMove(ev);
        this.buttonUpCallback= (ev)=> this.dialogMoveEnd(ev);
        this.browserResizeCallback= debounce(() => { this.updateLayoutPosition(); },150);
        if (visible) this.updateLayoutPosition();
        window.addEventListener('resize', this.browserResizeCallback);
        document.addEventListener('mousemove', this.moveCallback);
        document.addEventListener('mouseup', this.buttonUpCallback);
        if (this.props.closePromise) {
            this.props.closePromise.then(()=>  {
                this.askParentToClose();
            });
        }
    }

    askParentToClose() {
        if (this.props.requestToClose) this.props.requestToClose();
    }

    dialogMoveStart(ev)  {
        const {requestOnTop,dialogId}= this.props;
        requestOnTop && requestOnTop(dialogId);
        const e= ReactDOM.findDOMNode(this);
        const titleBar= ReactDOM.findDOMNode(this.titleBarRef);
        this.mouseCtx= humanStart(ev,e,titleBar);
    }

    dialogMove(ev)  {
        if (this.mouseCtx) {
            const titleBar= ReactDOM.findDOMNode(this.titleBarRef);
            const r = humanMove(ev,this.mouseCtx,titleBar);
            if (r) {
                this.setState({posX:r.newX, posY:r.newY});
            }
        }
    }

    dialogMoveEnd(ev)  {
        this.mouseCtx= humanStop(ev,this.mouseCtx);
        this.mouseCtx= null;
    }


    onMouseEnter() {
        this.props.mouseInDialog && this.props.mouseInDialog(true);
    }

    onMouseLeave() {
        this.props.mouseInDialog && this.props.mouseInDialog(false);
    }


    renderAsTopHeader() {

        const rootStyle= {
            position: 'absolute',
            visibility : this.state.activeLayoutType===LayoutType.NONE ? 'hidden' : 'visible',
            left : this.state.posX,
            top : this.state.posY,
            zIndex:this.props.zIndex
        };


        const title= this.props.title||'';

        return (
                <div style={rootStyle} className={'popup-panel-shadow disable-select'}
                     onTouchStart={this.dialogMoveStart}
                     onTouchMove={this.dialogMove}
                     onTouchEnd={this.dialogMoveEnd}
                     onMouseEnter={this.onMouseEnter}
                     onMouseLeave={this.onMouseLeave} >
                    <div className={'standard-border'}>
                        <div style={{position:'relative', height:'16px', width:'100%', cursor:'default'}}
                             className={'title-bar title-color popup-panel-title-background'}
                            onTouchStart={this.dialogMoveStart}
                            onTouchMove={this.dialogMove}
                            onTouchEnd={this.dialogMoveEnd}
                            onMouseDownCapture={this.dialogMoveStart}>
                            <div ref={(c) => this.titleBarRef=c}
                                 style= {{position:'absolute', left: 0, top: 0, bottom: 0, width:'100%', padding: '3px 0 3px 10px'}}
                                 onMouseDownCapture={this.dialogMoveStart}
                                 onTouchStart={this.dialogMoveStart}
                                 onTouchMove={this.dialogMove}
                                 onTouchEnd={this.dialogMoveEnd}
                                 className={'title-label'} >
                                <div className={'text-ellipsis'} style={{width:'80%', height: '100%'}}>
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

    }


    render() {
        if (this.props.visible) {
            return  this.renderAsTopHeader();
        }
        else {
            return false;
        }
    }

}

PopupPanel.propTypes= {
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
};
