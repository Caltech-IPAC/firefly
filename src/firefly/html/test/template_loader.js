

    window.onload = function () {
        const tests = document.getElementsByTagName('template');
        var cnt = 1;
        Object.values(tests).forEach(function(test) {
            const c = test.content;
            const expected = c.querySelector('#expected');
            const actual = c.querySelector('#actual');
            const scpt = c.querySelector('script');
            const title = cnt++ + ' - ' + test.title;

            renderTest(expected, actual, scpt, title, test);
        });
    };

    function renderTest(expected, actual, script, title, testTmpl) {
        const iframe = document.createElement('iframe');
        iframe.id = 'iframe';
        iframe.src = './template.html';
        iframe.style.height= '100%';
        const iframeContainer = document.createElement('div');
        iframeContainer.className = 'tst-iframe-container ' + testTmpl.className;
        iframeContainer.style.cssText = testTmpl.style.cssText;
        iframeContainer.appendChild(iframe);
        document.getElementById('tst-container').appendChild(iframeContainer);

        iframe.contentWindow.template = {expected, actual, script, title, className: testTmpl.className, iframeContainer};
        iframe.contentWindow.resizeIframeToHeight= function (size) {
            iframe.parentElement.style.minHeight= size;
        };
    }

