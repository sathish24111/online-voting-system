const { spawn, exec } = require('child_process');

const PORT = 8080;
const SUBDOMAIN = 'kprcasitvoting-v2';
const EXPECTED_URL = `https://${SUBDOMAIN}.loca.lt`;

function startTunnel() {
    console.log(`Starting localtunnel for port ${PORT} with subdomain ${SUBDOMAIN}...`);
    const lt = spawn('npx', ['localtunnel', '--port', PORT, '--subdomain', SUBDOMAIN], { shell: true });

    let gotExpectedUrl = false;

    lt.stdout.on('data', (data) => {
        const output = data.toString().trim();
        console.log(`[localtunnel] ${output}`);

        if (output.includes('your url is:')) {
            if (output.includes(EXPECTED_URL)) {
                console.log(`Successfully connected to expected URL: ${EXPECTED_URL}`);
                gotExpectedUrl = true;
            } else {
                console.log(`Received wrong/random URL. Killing process tree and retrying in 3 seconds...`);
                exec(`taskkill /pid ${lt.pid} /t /f`, (err) => {
                    if (err) {
                        console.log('taskkill completed or process already closed.');
                    }
                });
            }
        }
    });

    lt.stderr.on('data', (data) => {
        console.error(`[localtunnel error] ${data.toString()}`);
    });

    lt.on('close', (code) => {
        console.log(`localtunnel process exited with code ${code}.`);
        console.log('Restarting tunnel in 3 seconds...');
        setTimeout(startTunnel, 3000);
    });
}

startTunnel();
