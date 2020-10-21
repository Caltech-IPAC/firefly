

    window.onload = function () {
        const tests = document.getElementsByTagName('template');
        let cnt = 1;
        const allTest= Object.values(tests);
        const exclusiveTest= allTest.filter( (test) => test.className && test.className.includes('exclusive'));
        const activeTest= exclusiveTest.length ? exclusiveTest : allTest;
        activeTest.forEach((test) => {
                    const c = test.content;
                    const expected = c.querySelector('#expected');
                    const actual = c.querySelector('#actual');
                    const scpt = c.querySelector('script');
                    renderTest(cnt++, expected, actual, scpt, test, Boolean(exclusiveTest.length));
                });

        const testId = window.location.hash.substring(1);
        if (testId) {
            setTimeout(function() {
                window.scrollTo(0,document.getElementById(testId).offsetTop);
            }, 2000);
        }
    };


    function renderTest(cnt, expected, actual, script, testTmpl, onlyUsingExclusize) {
        const title = (onlyUsingExclusize? 'EXCLUSIVE - ' : '') + cnt + ' - ' + testTmpl.title;
        const iframe = document.createElement('iframe');
        iframe.id = 'iframe';
        iframe.src = './template.html';
        iframe.style.height= '100%';
        const iframeContainer = document.createElement('div');
        iframeContainer.id = 'test-' + cnt;
        iframeContainer.className = 'tst-iframe-container ' + testTmpl.className;
        iframeContainer.style.cssText = testTmpl.style.cssText;
        iframeContainer.appendChild(iframe);
        document.getElementById('tst-container').appendChild(iframeContainer);

        iframe.contentWindow.template = {expected, actual, script, title, className: testTmpl.className, iframeContainer};
        iframe.contentWindow.resizeIframeToHeight= function (size) {
            iframe.parentElement.style.minHeight= size;
        };
    }

