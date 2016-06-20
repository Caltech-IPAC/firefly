/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


function loadScript(loader, script, callback){
    var scriptTag = document.createElement('script');
    scriptTag.type = 'text/javascript';

    const url = getScriptURL(loader) + script;
    if (scriptTag.readyState){  //IE
        scriptTag.onreadystatechange = function(){
            if (['loaded', 'complete'].includes(scriptTag.readyState)) {
                scriptTag.onreadystatechange = null;
                callback && callback();
            }
        };
    } else {  //Others
        scriptTag.onload = function(){
            callback && callback();
        };
    }

    scriptTag.src = url;
    document.getElementsByTagName('head')[0].appendChild(scriptTag);
}

function getScriptURL(loader) {
    loader = loader || 'firefly_loader.js';
    var scripts = document.getElementsByTagName('script');
    var url = '/';
    for (var i = 0; (i < scripts.length); i++) {
        if (scripts[i].src.indexOf(loader) > -1) {
            url = scripts[i].src.replace(loader, '');
        }
    }
    return url;
};
