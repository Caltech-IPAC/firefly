/* eslint-env node */

var publishRelease = require('publish-release');
var fs = require('fs');
var exec = require('child_process').exec;
var execSync = require('child_process').execSync;
var request = require('request');
var ffdesc = 'This standalone release enable Firefly to run without additional dependencies beside Java 1.8. \n' +
             'It comes with an embedded Tomcat 7. \n\n' +
             'To start Firefly: \n' +
             'java -jar fftools-exec.war \n\n' +
             'By default, it will start up on port 8080. Goto to http://localhost:8080/fftools/ after it has started. \n' +
             'To start it on a different port, add -httpPort to the java command. \n\n' +
             'This will extract the content of the war file into a directory called ".extract" in your current directory. \n' +
             'To change this, add -extractDirectory to the java command. \n\n';

var args = JSON.parse( process.argv[2] || '{}' );

if (args.tag && args.assets) {

    args.assets.forEach( function (fname) {
        if (!fs.existsSync(fname)) {
            console.log( 'Publish Release aborted!  Assets does not exist: ' + fname );
            process.exit(1);
        }
    });

    var rel_config = {
        token: '',
        owner: 'lsst',
        repo: 'firefly',
        tag: '',
        name: args.tag,
        draft: false,
        prerelease: true,
        assets: [],
        apiUrl: 'https://api.github.com'
    };

    rel_config = Object.assign(rel_config, args);

    console.log('Publishing release (' + rel_config.tag + ') to github.');

    if (rel_config.notes) {
        doPublish(rel_config);
    } else {
        checkGitHub(rel_config, getChangeLog);
    }

} else {
    console.log('Publish Release aborted!  Missing required arguments: tag and assets.');
    process.exit(1);
}

function getChangeLog(rel_config, lastdate) {
    var cmd = 'git log --pretty=format:"%h - %s [%cd]" --date=short --after="' + lastdate +'"';

    // push changes to github..
    exec(cmd, function (error, stdout, stderr) {
        rel_config.notes = 'Changelog: \n\n' + stdout;
        doPublish(rel_config);
    });
}

/**
 * check with github for latest commit datetime
 */
function checkGitHub(rel_config, callback) {

    request({
        uri: rel_config.apiUrl + '/repos/' + rel_config.owner + '/' + rel_config.repo + '/commits/master',
        method: 'GET',
        headers: {
            'Authorization': 'token ' + rel_config.token,
            'User-Agent': 'firely Jenkins'
        }
    }, function (err, res, body) {
        if (err) return callback(err);
        var result = JSON.parse(body);
        console.log( 'github latest commit: ' + result.sha );
        callback(rel_config, result.commit.author.date);
    });
}

function doPublish(rel_config) {

    rel_config.notes = ffdesc + rel_config.notes;
    execSync('git remote add lsst https://' + rel_config.token +'@github.com/lsst/firefly.git');
    execSync('git push --tags lsst HEAD:test');
    execSync('git remote rm lsst');

    console.log( JSON.stringify(rel_config, null, 2));
    publishRelease(rel_config,
        function (err, release) {
            if (err) {
                console.log('Failed: ' + JSON.stringify(err, null, 2));
            } else {
                if (release.html_url) {
                    console.log('Publish Done: ' + release.html_url);
                }
            }
        });
}
