/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import uniqueId from 'lodash/uniqueId';
import delay from 'lodash/delay';
import {flux} from '../Firefly.js';
import {DropDownMenuWrapper} from './DropDownMenu.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {dispatchShowDialog, dispatchHideDialog, isDialogVisible, getDialogOwner} from '../core/ComponentCntlr.js';
import {ToolbarButton} from './ToolbarButton.jsx';
import {DropDownDirCTX} from './DropDownDirContext.js';


function computeDropdownXY(divElement, isIcon) {
    const bodyRect = document.body.parentElement.getBoundingClientRect();
    const off= isIcon ? 0 : 6;
    const elemRect = divElement.getBoundingClientRect();
    const x = (elemRect.left - bodyRect.left);
    const y = elemRect.top - bodyRect.top- off;
    return {x,y};
}


function showDialog(divElement,dropDown,ownerId,offButtonCB, isIcon) {
    const {x,y}= computeDropdownXY(divElement, isIcon);

    const dropDownClone= React.cloneElement(dropDown, { toolbarElement:divElement });
    const dd= <DropDownMenuWrapper x={x} y={y} content={dropDownClone}/>;
    DialogRootContainer.defineDialog(DROP_DOWN_KEY,dd);
    dispatchShowDialog(DROP_DOWN_KEY,ownerId);
    document.addEventListener('mousedown', offButtonCB);
}



export const DROP_DOWN_KEY= 'toolbar-dropDown';
const OWNER_ROOT= 'toolbar-dropDown';
const DEFAULT_DROPDOWN_DIR = 'right';


export class DropDownToolbarButton extends PureComponent {
    constructor(props) {
        super(props);
        this.state= {dropDownVisible:false, dropDownOwnerId:null };
        this.ownerId= uniqueId(OWNER_ROOT);
    }

    componentWillUnmount() {
        if (this.storeListenerRemove) this.storeListenerRemove();
        document.removeEventListener('mousedown', this.docMouseDownCallback);// just in case
    }

    componentDidMount() {
        this.storeListenerRemove= flux.addListener(() => this.update());
        this.docMouseDownCallback= (ev)=> this.offButtonCallback(ev);
    }

    update() {
        const v= isDialogVisible(DROP_DOWN_KEY);
        const ownerId= v ? getDialogOwner(DROP_DOWN_KEY) : null;
        const {dropDownVisible, dropDownOwnerId}= this.state;
        if (v!==dropDownVisible || ownerId!==dropDownOwnerId) {
            this.setState({dropDownVisible:v, dropDownOwnerId:ownerId});
        }
    }

    offButtonCallback() {
        delay( () => {
            document.removeEventListener('mousedown', this.docMouseDownCallback);
            const {dropDownVisible, dropDownOwnerId}= this.state;
            if (dropDownVisible && dropDownOwnerId===this.ownerId) {
                dispatchHideDialog(DROP_DOWN_KEY);
            }
        },200);
    }


    handleDropDown(divElement,dropDown) {
        if (divElement) {
            const isIcon= Boolean(this.props.icon);
            const {dropDownVisible, dropDownOwnerId}= this.state;

            const dropDownWithContext= (
                <DropDownDirCTX.Provider value={{dropdownDirection: calcDropDownDir(divElement, this.props.menuMaxWidth)}}>
                    {dropDown}
                </DropDownDirCTX.Provider>
            );

            if (dropDownVisible) {
                if (dropDownOwnerId===this.ownerId) {
                    dispatchHideDialog(DROP_DOWN_KEY);
                    document.removeEventListener('mousedown', this.docMouseDownCallback);
                }
                else {
                    showDialog(divElement,dropDownWithContext,this.ownerId,this.docMouseDownCallback, isIcon);
                }

            }
            else {
                showDialog(divElement,dropDownWithContext,this.ownerId,this.docMouseDownCallback, isIcon);
            }
        }
    }


    render() {
        const {dropDown}= this.props;
        const {direction}= this.props || DEFAULT_DROPDOWN_DIR;
        const {dropDownVisible, dropDownOwnerId}= this.state;
        return (<ToolbarButton {...this.props} active={dropDownVisible && dropDownOwnerId===this.ownerId}
            dropDownCB={(divElement)=> this.handleDropDown(divElement,dropDown,direction)}/>);
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
    hasHorizontalLayoutSep: PropTypes.bool,
    menuMaxWidth: PropTypes.number,
    dropDown : PropTypes.object.isRequired,
    useDropDownIndicator: PropTypes.bool
};



function calcDropDownDir(element, menuWidth){
    if (!element || !menuWidth) return DEFAULT_DROPDOWN_DIR;
    const bodyRect = document.body.getBoundingClientRect();
    const elemRect = element.getBoundingClientRect();
    const space = bodyRect.width - elemRect.x;
    return space < menuWidth ? 'left' : 'right';
}