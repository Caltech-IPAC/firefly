/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import './Toolbar.css';


/*
Usage:
 <Toolbar>
     <ul role='left'>
     <li><button>Add</button></li>
     </ul>
     <ul role='middle' style="width:300px">
     <li><button>Mark</button></li>
     <li><button>Send</button></li>
     <li><button>Move</button></li>
     <li><button>Share</button></li>
     </ul>
     <ul role='right'>
     <li><button>Delete</button></li>
     </ul>
 </toolbar>

 */
export const Toolbar = function (props) {
    var {children} = props;
    return (
        <div role='toolbar' {...props}>
            <div className='toolbar__content'>
                {children}
            </div>
        </div>
    );
};

