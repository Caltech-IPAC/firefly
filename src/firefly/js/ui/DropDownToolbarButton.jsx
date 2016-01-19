/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {Component, PropTypes} from 'react';
import uniqueId from 'lodash/utility/uniqueId';
import sCompare from 'react-addons-shallow-compare';
import {flux} from '../Firefly.js';
import {DropDownMenuWrapper} from './DropDownMenu.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import AppDataCntlr from '../core/AppDataCntlr.js';
import {ToolbarButton} from './ToolbarButton.jsx';


function computeDropdownXY(divElement) {
    var bodyRect = document.body.getBoundingClientRect();
    var elemRect = divElement.getBoundingClientRect();
    var x = (elemRect.left - bodyRect.left);
    var y = elemRect.top - bodyRect.top;
    return {x,y};
}


function defineDialog(divElement,dropDown) {
    var {x,y}= computeDropdownXY(divElement);
    var dd= <DropDownMenuWrapper x={x} y={y} content={dropDown}/>;
    DialogRootContainer.defineDialog(DROP_DOWN_KEY,dd);
}



export const DROP_DOWN_KEY= 'toolbar-dropDown';
const OWNER_ROOT= 'toolbar-dropDown';

export class DropDownToolbarButton extends Component {
    constructor(props) {
        super(props);
        this.state= {dropDownVisible:false, dropDownOwnerId:null };
        this.ownerId= uniqueId(OWNER_ROOT);
    }


    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentWillUnmount() {
        if (this.storeListenerRemove) this.storeListenerRemove();
    }

    componentDidMount() {
        this.storeListenerRemove= flux.addListener(() => this.update());
    }

    update() {
        var v= AppDataCntlr.isDialogVisible(DROP_DOWN_KEY);
        var ownerId= v ? AppDataCntlr.getDialogOwner(DROP_DOWN_KEY) : null;
        var {dropDownVisible, dropDownOwnerId}= this.state;
        if (v!==dropDownVisible || ownerId!=dropDownOwnerId) {
            this.setState({dropDownVisible:v, dropDownOwnerId:ownerId});
        }
    }





    handleDropDown(divElement,dropDown) {
        if (divElement) {
            var {dropDownVisible, dropDownOwnerId}= this.state;
            if (dropDownVisible) {
                if (dropDownOwnerId===this.ownerId) {
                    AppDataCntlr.hideDialog(DROP_DOWN_KEY);
                }
                else {
                    defineDialog(divElement,dropDown);
                    AppDataCntlr.showDialog(DROP_DOWN_KEY,this.ownerId);
                }

            }
            else {
                defineDialog(divElement,dropDown);
                AppDataCntlr.showDialog(DROP_DOWN_KEY,this.ownerId);
            }
        }
    }


    render() {
        var {dropDown}= this.props;
        var {dropDownVisible, dropDownOwnerId}= this.state;
        return <ToolbarButton {...this.props} active={dropDownVisible && dropDownOwnerId===this.ownerId}
            dropDownCB={(divElement)=> this.handleDropDown(divElement,dropDown)}/>;
    }
}

DropDownToolbarButton.propTypes= {
    icon : PropTypes.string,
    text : PropTypes.string,
    tip : PropTypes.string,
    badgeCount : PropTypes.number,
    enabled : PropTypes.bool,
    bgDark: PropTypes.bool,
    todo: PropTypes.bool,
    useBorder : PropTypes.bool,
    onClick : PropTypes.func,
    horizontal : PropTypes.bool,
    visible : PropTypes.bool,
    tipOnCB : PropTypes.func,
    tipOffCB : PropTypes.func,
    dropDown : PropTypes.object.isRequired
};

DropDownToolbarButton.defaultProps= {
};

