

makeFFNotifier= function(msgTitle, message, oncloseFunc) {

    var title= msgTitle;
    var msg= message
    var retObj= new Object();




    retObj.isSupported= function() {
        if (window.webkitNotifications)  return true;
        else                             return false;
    };

    retObj.hasPermission= function() {
        if (retObj.isSupported()) {
            return window.webkitNotifications.checkPermission()==0;
        }
        else {
            return false;
        }
    };

    retObj.requestPermission= function(callback) {
        if (retObj.isSupported()) {
            window.webkitNotifications.requestPermission(callback);
        }
    };


    retObj.send= function() {
        var not = window.webkitNotifications.createNotification( null, title, msg);
        not.onclick = oncloseFunc;
        not.show();
     };

    retObj.notify= function() {
        if (retObj.isSupported()) {
            if (retObj.hasPermission()) {
                retObj.send();
            }
            else {
                retObj.requestPermission(function() {
                    if (retObj.hasPermission()) {
                        retObj.send();
                    }
                });
            }
        }
    };

    return retObj;

};

