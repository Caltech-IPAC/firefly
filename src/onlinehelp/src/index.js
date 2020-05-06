import React from 'react';
import ReactDOM from 'react-dom';
import {App} from './App';
import {toc} from './toc';

ReactDOM.render(<App showHidden={false} tableOfContent={toc}/>, document.getElementById('app-root'));





