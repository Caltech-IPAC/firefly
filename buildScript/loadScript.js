/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


function loadScript(url, callback){
    var script = document.createElement('script');
    script.type = 'text/javascript';

    if (script.readyState){  //IE
        script.onreadystatechange = function(){
            if (['loaded', 'complete'].includes(script.readyState)) {
                script.onreadystatechange = null;
                callback && callback();
            }
        };
    } else {  //Others
        script.onload = function(){
            callback && callback();
        };
    }

    script.src = url;
    document.getElementsByTagName('head')[0].appendChild(script);
}


