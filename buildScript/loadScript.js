/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

(function (loaderScript, bundleScript) {
    var scriptTag = document.createElement('script');
    scriptTag.type = 'text/javascript';
    scriptTag.setAttribute('defer', 'defer');

    var getScriptURL = function(loader) {
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

    var bundleUrl = getScriptURL(loaderScript) + bundleScript;
    scriptTag.src = bundleUrl;
    scriptTag.charset='utf-8';
    document.getElementsByTagName('head')[0].appendChild(scriptTag);
})(/*PARAMS_HERE*/);


