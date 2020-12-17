

    window.onload = function () {
        const tests = document.getElementsByTagName('template');
        const allTest= Object.values(tests);


        const {testNum,testKey}= getTestParam();
        const exclusiveTest= allTest.filter( (test,idx) => {
            return (test.className && test.className.includes('exclusive')) || test.title===testKey || testNum===idx+1;
        });
        const activeTest= exclusiveTest.length ? exclusiveTest : allTest;
        activeTest.forEach((test) => {
                    const textNum= allTest.findIndex((e) => test===e)+1;
                    const c = test.content;
                    const expected = c.querySelector('#expected');
                    const actual = c.querySelector('#actual');
                    const scpt = c.querySelector('script');
                    renderTest(textNum, expected, actual, scpt, test, Boolean(exclusiveTest.length));
                });

        const testId = window.location.hash.substring(1);
        if (testId) {
            setTimeout(function() {
                window.scrollTo(0,document.getElementById(testId).offsetTop);
            }, 2000);
        }
    };


    function renderTest(testNum, expected, actual, script, testTmpl, onlyUsingExclusive) {
        const title = (onlyUsingExclusive? 'EXCLUSIVE - ' : '') + testNum + ' - ' + testTmpl.title;
        const iframe = document.createElement('iframe');
        iframe.id = 'iframe';
        iframe.src = './template.html';
        iframe.style.height= '100%';
        const iframeContainer = document.createElement('div');
        iframeContainer.id = 'test-' + testNum;
        iframeContainer.className = 'tst-iframe-container ' + testTmpl.className;
        iframeContainer.style.cssText = testTmpl.style.cssText;
        iframeContainer.appendChild(iframe);
        document.getElementById('tst-container').appendChild(iframeContainer);

        iframe.contentWindow.template = {expected, actual, script, title,
            className: testTmpl.className,
            templateTitle:testTmpl.title,
            iframeContainer, testNum, onlyUsingExclusive};
        iframe.contentWindow.resizeIframeToHeight= function (size) {
            iframe.parentElement.style.minHeight= size;
        };
    }

    function getTestParam() {
        const {searchParams} = new URL(document.location.href);
        let testKey;
        let testNum=-1;
        for(const [k,v] of searchParams.entries()) {
            if (k.toLocaleLowerCase()==='test') {
                if (isNaN(Number(v))) {
                    const vLower= v.toLocaleLowerCase();
                    if (vLower.startsWith('test-') && !isNaN(Number(vLower.substring(5)))) {
                        testNum= Number(vLower.substring(5));
                    }
                    else {
                        testKey=v;
                    }
                }
                else {
                    testNum= Number(v);
                }
            }
        }
        return {testNum,testKey};
    }
