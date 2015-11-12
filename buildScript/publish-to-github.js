/* eslint-env node */

var publishRelease = require('publish-release');
var fs = require('fs');
var exec = require('child_process').exec;
var request = require('request')
var ffdesc = 'This standalone release enable Firefly to run without additional dependencies beside Java 1.8. \
It comes with an embedded Tomcat 7. \
\
To start Firefly: \
    java -jar fftools-exec.war \
\
By default, it will start up on port 8080. Goto to http://localhost:8080/fftools/ after it has started. \
    To start it on a different port, add -httpPort to the java command. \
\
    This will extract the content of the war file into a directory called ".extract" in your current directory. \
    To change this, add -extractDirectory to the java command. \
\
\
    ';

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
        console.log( 'github lastest commit: ' + result.sha );
        callback(rel_config, result.commit.author.date);
    });
}

function doPublish(rel_config) {

    rel_config.notes = ffdesc + rel_config.notes;
    exec('git remote add lsst https://' + rel_config.token +'@github.com/lsst/firefly.git');
    setTimeout(doReleasePush(rel_config), 2000);
}

function doReleasePush(rel_config) {
    var proc = exec('git push --tags lsst HEAD:master', function(error, stdout, stderr) {
        if (stdout) {
            console.log('stdout: ' + stdout);
        }
        if (stderr) {
            console.log('stderr: ' + stderr);
        }
        if (error !== null) {
            console.log('ERROR: Fail to push changes to github. ' + error);
        }
    });

    proc.on('exit', function (code) {
        if (code == 0) {
            exec('git remote rm lsst');

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
        } else {
            console.log('Publish Release aborted!  Errors while pushing to lsst/firefly.');
            process.exit(1);
        }
    });
}

