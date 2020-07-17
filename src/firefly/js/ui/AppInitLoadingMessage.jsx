/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';

export function AppInitLoadingMessage({message= 'Loading...'}) {
    return (
        <div style={{position: 'relative', width:'100%', height:'100%'}}>
            <div style={{flex: '1 1 auto', display:'flex', flexDirection: 'column', justifyContent:'center', alignItems:'center',
                position:'absolute', left:0, top:300, width:'100%'}}>
                <div style={{fontSize: '40pt'}}>
                    {message}
                </div>
                <div style={{width:100, height:100, borderColor: 'transparent #89A6B8 #89A6B8', marginTop: 100}} className='loading-animation' />
            </div>
        </div>
    );
}

