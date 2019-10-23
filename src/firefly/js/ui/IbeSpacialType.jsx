/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */



// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// THIS PANEL IS TEMPORARY, ONLY TO TEST CATALOGS UNTIL WE FINISH THE REAL PANEL
// This panel will get spacial type for wise searches
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {get} from 'lodash';

import {InputGroup} from '../ui/InputGroup.jsx';

import FieldGroupUtils from '../fieldGroup/FieldGroupUtils.js';
import {RadioGroupInputField} from './RadioGroupInputField.jsx';
import {ListBoxInputField} from './ListBoxInputField.jsx';
import {SizeInputFields} from './SizeInputField.jsx';
import {GroupKeyCtx} from './FieldGroup';


const mcenTip= `Specifies whether to return only the most centered (in pixel space)
 image-set for the given input position.  In multi-input mode, this
 flag applies only for input tables without a best column.`;

export class IbeSpacialType extends PureComponent {

    constructor(props) {
        super(props);
        this.state= {fields:null};
    }

    componentWillUnmount() {
        if (this.removeListener) this.removeListener();
        this.iAmMounted= false;
    }

    componentDidMount() {
        this.iAmMounted= true;
        const groupKey= this.context && this.context.groupKey;
        this.removeListener= FieldGroupUtils.bindToStore(groupKey, (fields) => {
            this.setState({fields});
        });
    }

    render() {
        const {fields}= this.state;
        const searchType= get(fields, 'intersect.value', 'CENTER');


        return (
            <div style={{padding: 10}}>
                <InputGroup labelWidth={300}>
                    <ListBoxInputField
                        fieldKey='intersect'
                        initialState= {{
                                          tooltip: 'Please select an option',
                                          label : 'Search Type (Region Intersection):',
                                          value:'CENTER'
                                      }}
                        options={ [
                                        {value: 'CENTER', label: 'Image contains target' },
                                        {value: 'COVERS', label: 'Image covers entire search region' },
                                        {value: 'ENCLOSED', label: 'Image is entirely enclosed by search region' },
                                        {value: 'OVERLAPS', label: 'Any pixel overlaps search region' }
                                          ] }
                        multiple={false}
                    />
                    <p> </p>
                    {renderSearchRegion(searchType!=='CENTER')}
                    <p> </p>
                    {renderImageSize(searchType==='CENTER' || searchType==='COVERS')}
                    <p> </p>
                    {renderMostCenter(searchType==='CENTER' || searchType==='COVERS')}
                </InputGroup>
            </div>
        );

    }


}


IbeSpacialType.contextType = GroupKeyCtx;


function renderSearchRegion(visible) {
    
    return (
        <SizeInputFields fieldKey='size'
                         wrapperStyle={{visibility:visible?'visible':'hidden'}}
                         initialState= {{
                                           value: '0.00833333333333',
                                           tooltip: 'Please select an option',
                                           unit: 'arcsec',
                                           min:  1/3600,
                                           max:  43200/3600
                                 }}
                         label='Search Region (Square) Size:'
                         labelWidth={300} />

    );
}

function renderImageSize(visible) {
   return (
       <SizeInputFields fieldKey='subsize'
                        wrapperStyle={{visibility:visible?'visible':'hidden'}}
                        initialState= {{
                                               value: '0.1388888888889',
                                               tooltip: 'Please select an option',
                                               unit: 'arcsec',
                                               min:  1/3600,
                                               max:  7200/3600
                                           }}
                        label='Return Image Size (leave blank for full images):'
                        labelWidth={300} />
   ) ;
}

function renderMostCenter(visible) {
    return (
        <RadioGroupInputField fieldKey='mcenter'
                              wrapperStyle={{visibility:visible?'visible':'hidden'}}
                              inline={true}
                              alignment='horizontal'
                              initialState={{
                                        tooltip: mcenTip,
                                        value: 'mcen',
                                        label: 'Return only the most centered image containing the target:'
                                   }}
                              options={[
                                      {label: 'Yes', value: 'mcen'},
                                      {label: 'No', value: 'all'}
                                      ]}
        />
    );
}


