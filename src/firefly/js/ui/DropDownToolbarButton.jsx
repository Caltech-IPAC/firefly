/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import uniqueId from 'lodash/uniqueId';
import delay from 'lodash/delay';
import {flux} from '../core/ReduxFlux.js';
import {DropDownMenuWrapper} from './DropDownMenu.jsx';
import DialogRootContainer from './DialogRootContainer.jsx';
import {dispatchShowDialog, dispatchHideDialog, isDialogVisible, getDialogOwner} from '../core/ComponentCntlr.js';
import {ToolbarButton} from './ToolbarButton.jsx';
import {DropDownDirCTX} from './DropDownDirContext.js';
import {DROP_DOWN_WRAPPER_CLASSNAME} from './DropDownMenu';


/**
 * j
 * @param {Element} buttonElement
 * @param isIcon
 * @param {Element} dropdownElement
 * @return {{x: number, y: number}}
 */
function computeDropdownXY(buttonElement, isIcon, dropdownElement) {
    const bodyRect = document.body.parentElement.getBoundingClientRect();
    const dropdownRect = dropdownElement.getBoundingClientRect();
    const elemRect = buttonElement.getBoundingClientRect();
    const off= isIcon ? 4 : 10;
    let x = elemRect.left - bodyRect.left - 10;
    const leftAdjust= (bodyRect.right-20 < x + dropdownRect.width) ? dropdownRect.width-elemRect.width-20 : 0;
    x-= leftAdjust;
    const y = elemRect.bottom - bodyRect.top- off;
    return {x,y};
}


/**
 * Compute the drop down position and build the react components to show the dropdown.
 * The dropdown position is computed in two phases. The normal position and after the dropdown element exist
 * it is check to make sure it is not going off the right side of the screen. Part 2 is done in the beforeVisible
 * callback. At that point the element has be created and the visibility is set ot hidden.  They way we can do side
 * computations.
 *
 * @param {string} dropDownKey - the key to use to identify the dialog
 * @param {Object} buttonElement - the div of where the button is
 * @param {Object} dropDown - dropdown React component
 * @param {String} ownerId
 * @param {function} offButtonCB
 * @param {boolean} isIcon
 */
function showDialog(dropDownKey,buttonElement,dropDown,ownerId,offButtonCB, isIcon) {

    const beforeVisible= (e) =>{
        if (!e) return;
        const {x,y}= computeDropdownXY(buttonElement,isIcon, e);
        e.style.left= x+'px';
        e.style.top= y+'px';
    };

    const dropDownClone= React.cloneElement(dropDown, { toolbarElement:buttonElement});
    const dd= <DropDownMenuWrapper x={0} y={0} content={dropDownClone} beforeVisible={beforeVisible}/>;
    DialogRootContainer.defineDialog(dropDownKey,dd);
    document.removeEventListener('mousedown', offButtonCB);
    dispatchShowDialog(dropDownKey,ownerId);
    setTimeout(() => {
        document.addEventListener('mousedown', offButtonCB);
    },10);
}



export const DROP_DOWN_KEY= 'toolbar-dropDown';
const OWNER_ROOT= 'toolbar-dropDown';
const DEFAULT_DROPDOWN_DIR = 'right';


export class DropDownToolbarButton extends PureComponent {
    constructor(props) {
        super(props);
        this.state= {dropDownVisible:false, dropDownOwnerId:null };
        this.ownerId= uniqueId(OWNER_ROOT);
        this.mounted= true;
        this.divElement= undefined;
    }

    componentWillUnmount() {
        if (this.storeListenerRemove) this.storeListenerRemove();
        this.mounted= false;
        document.removeEventListener('mousedown', this.docMouseDownCallback);// just in case
    }

    componentDidMount() {
        this.storeListenerRemove= flux.addListener(() => this.update());
        this.docMouseDownCallback= (ev)=> this.offButtonCallback(ev);
        this.mounted= true;
    }

    update() {
        const dropDownKey= this.props.dropDownKey || DROP_DOWN_KEY;
        const v= isDialogVisible(dropDownKey);
        const ownerId= v ? getDialogOwner(dropDownKey) : null;
        const {dropDownVisible, dropDownOwnerId}= this.state;
        if (v!==dropDownVisible || ownerId!==dropDownOwnerId) {
            this.setState({dropDownVisible:v, dropDownOwnerId:ownerId});
        }
    }

    offButtonCallback(ev) {
        delay( () => {
            document.removeEventListener('mousedown', this.docMouseDownCallback);
            const {dropDownVisible, dropDownOwnerId}= this.state;
            if (!dropDownVisible) return;

            let e= document.activeElement;
            let focusIsDropwdownInput= false;
            if (e && e.tagName==='INPUT') {
                for(;e;e= e.parentElement) {
                     if (e.className===DROP_DOWN_WRAPPER_CLASSNAME) {
                         focusIsDropwdownInput= true;
                         break;
                     }
                }
            }
            const tgt= ev.target;
            e= ev.target;
            const maxBack= 10;
            let clickOnButton= false;
            for(let i=0;(e&&i<maxBack);e= e.parentElement,i++) {
                if (this.divElement===e) {
                   clickOnButton= true;
                   break;
                }
            }
            const onDropDownInput= ev &&
                (focusIsDropwdownInput && tgt.tagName==='INPUT') ||
                (tgt.tagName==='DIV' && tgt.className?.includes('allow-scroll')) ||
                (tgt.className?.includes('allow-input')) ||
                (tgt.tagName==='DIV' && tgt.className?.includes('rc-slider'));

            if (!clickOnButton && !onDropDownInput && dropDownOwnerId===this.ownerId) {
                dispatchHideDialog(this.props.dropDownKey || DROP_DOWN_KEY);
            }
            else {
                document.addEventListener('mousedown', this.docMouseDownCallback);
            }
        },200);
    }


    handleDropDown(divElement,dropDown) {
        if (divElement) {
            this.divElement= divElement;
            const isIcon= Boolean(this.props.icon);
            const {dropDownVisible, dropDownOwnerId}= this.state;

            const dropdownDirection= calcDropDownDir(divElement, this.props.menuMaxWidth);
            const dropDownWithContext= (
                <DropDownDirCTX.Provider value={{dropdownDirection}}>
                    {dropDown}
                </DropDownDirCTX.Provider>
            );

            const dropDownKey= this.props.dropDownKey || DROP_DOWN_KEY;
            if (dropDownVisible) {
                if (dropDownOwnerId===this.ownerId) {
                    dispatchHideDialog(dropDownKey);
                    document.removeEventListener('mousedown', this.docMouseDownCallback);
                    this.setState({dropDownVisible:false});
                }
                else {
                    showDialog(dropDownKey, divElement,dropDownWithContext,this.ownerId,this.docMouseDownCallback, isIcon);
                }

            }
            else {
                showDialog(dropDownKey, divElement,dropDownWithContext,this.ownerId,this.docMouseDownCallback, isIcon);
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
    onClick : PropTypes.func,
    horizontal : PropTypes.bool,
    visible : PropTypes.bool,
    tipOnCB : PropTypes.func,
    tipOffCB : PropTypes.func,
    menuMaxWidth: PropTypes.number,
    dropDown : PropTypes.object.isRequired,
    dropDownKey: PropTypes.string,
    useDropDownIndicator: PropTypes.bool
};



function calcDropDownDir(element, menuWidth){
    if (!element || !menuWidth) return DEFAULT_DROPDOWN_DIR;
    const bodyRect = document.body.getBoundingClientRect();
    const elemRect = element.getBoundingClientRect();
    const space = bodyRect.width - elemRect.x;
    return space < menuWidth ? 'left' : 'right';
}
