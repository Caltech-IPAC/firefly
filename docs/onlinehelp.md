Firefly Online Help
----------------------- 


## Overview

Provides context sensitive help for Firefly-based applications.

Below are key parts you should be aware of:

- Table of content:  
    A tree-like structure consisting of HelpItems rendered as the left navigation panel of the app.

- Topic:      
    A HelpItem with sub-items.  Because HelpItem may contain sub-items of other HelpItems, you can create a tree
    with unlimited branches and leaves.

- HelpItem:   
    One entry of help that can be referenced by the app or other help pages.
    HelpItem contains properties that's defined below: @typedef {object} HelpItem


For greater reusability, a topic should be written as 2 parts; overview and details.
Overview is the project's specific information.  The details part should be in sub-items and written in a generic way
so that other projects can easily include them into their onlinehelp.

###### Example:

    export const toc_a_topic = {
        id: 'a_topic',
        title: 'My topic',
        href: 'a_topic.html',           // points to html file in the public folder
        items: [
            {
                id: 'a_topic.item1',
                title: 'Item 1',
                href: 'share/a_topic_details.html#item1'         // points to html file in the public/share folder
            },
            {
                id: 'a_topic.item2',
                title: 'Item 2',
                href: 'share/a_topic_details_item2.html'         // can be part of a shared file or separate files
            }
        ]
    };


Because `toc_a_topic` is just a plain JavaScript object, there are many ways to modify a it.     
One way is to merge any updates into the original source:
        
    import {toc_a_topic} from 'firelfyHelp/toc';
    const my_topic = {...toc_a_topic, title: 'custom title if needed', href: 'my_topic.html'};
    
    
## Build requirements

- Node.js
- Yarn
- Gradle
- HTMLDOC (optional: generate PDF)

Refer to internal documentation for installation guide: `<env>/doc/Developer Setup.txt` 



## Getting Started
    
    git clone https://github.com/Caltech-IPAC/firefly
    cd firefly/src/onlinehelp
    
    
To build and open Firefly Online Help using your default browser.  This only works on macOS and Windows.
    
    gradle run
    
Alternatively, you can build Firefly Online help, then using your favorite browser, open the generated `index.html` file located at `./firefly/build/firefly/war/onlinehelp/index.html`
    
    gradle build



## Development

During development, it's convenience to have continunous build where your JavaScript code changes are automatically updated.  
`devMode` will do that for you.  You still need to reload your browser to pick up the changes.
    
    gradle devMode





