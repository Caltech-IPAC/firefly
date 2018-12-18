

    window.onload = function () {
        const tests = document.getElementsByTagName('template');
        Object.values(tests).forEach(function(test) {
            const c = test.content;
            const expected = c.querySelector('#expected');
            const actual = c.querySelector('#actual');
            const scpt = c.querySelector('script');

            renderTest(expected, actual, scpt, test.title, test.className);
        });
    };

    function resizeIframe(obj) {
        obj.style.height = obj.contentWindow.document.body.scrollHeight + 20 + 'px';
    }

    function renderTest(expected, actual, script, title, className) {
        const iframe = document.createElement('iframe');
        iframe.src = './template.html';
        const idiv = document.createElement('div');
        idiv.className = 'tst-iframe-container';
        idiv.appendChild(iframe);
        document.getElementById('tst-container').appendChild(idiv);

        iframe.contentWindow.template = {expected, actual, script, title, className};
        iframe.contentWindow.resizeIframe = function () {
            resizeIframe(iframe);
        };
    }

