document.addEventListener('DOMContentLoaded', () => {
    const busSelect = document.getElementById('busId');
    const startButton = document.getElementById('startSharing');
    const stopButton = document.getElementById('stopSharing');
    const status = document.getElementById('gpsStatus');
    const error = document.getElementById('gpsError');
    const secureContextHelp = document.getElementById('secureContextHelp');
    let watchId = null;
    let sending = false;
    let latestPosition = null;

    document.getElementById('currentOrigin').textContent = window.location.origin;
    if (!window.isSecureContext || !navigator.geolocation) {
        secureContextHelp.classList.remove('d-none');
    }

    startButton.addEventListener('click', () => {
        error.classList.add('d-none');
        if (!window.isSecureContext || !navigator.geolocation) {
            showError('Phone GPS is unavailable on this HTTP address until Chrome trusts this exact hotspot origin. Follow the secure-origin instructions above.');
            return;
        }
        if (!busSelect.value) {
            showError('Select the bus that this phone is physically on before starting GPS sharing.');
            return;
        }
        if (watchId !== null) return;

        status.textContent = 'Waiting for an accurate phone GPS fix…';
        startButton.disabled = true;
        stopButton.disabled = false;
        watchId = navigator.geolocation.watchPosition(onPosition, onLocationError, {
            enableHighAccuracy: true,
            timeout: 20000,
            maximumAge: 1000
        });
    });

    stopButton.addEventListener('click', stopSharing);

    function onPosition(position) {
        latestPosition = position;
        document.getElementById('gpsLatitude').textContent = position.coords.latitude.toFixed(6);
        document.getElementById('gpsLongitude').textContent = position.coords.longitude.toFixed(6);
        document.getElementById('gpsAccuracy').textContent = `±${Math.round(position.coords.accuracy)} m`;
        status.textContent = 'GPS fix acquired; sending to SmartBus…';
        sendLatestPosition();
    }

    async function sendLatestPosition() {
        if (sending || !latestPosition || watchId === null) return;
        sending = true;
        const position = latestPosition;
        latestPosition = null;
        try {
            const response = await fetch(`/api/admin/buses/${encodeURIComponent(busSelect.value)}/location`, {
                method: 'POST',
                credentials: 'same-origin',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    latitude: position.coords.latitude,
                    longitude: position.coords.longitude,
                    speed: position.coords.speed == null ? 0 : Math.max(0, position.coords.speed * 3.6)
                })
            });
            const contentType = response.headers.get('content-type') || '';
            if (!contentType.includes('json')) {
                throw new Error(response.redirected
                    ? 'Your Admin session expired. Sign in again on this phone.'
                    : `Unexpected server response (HTTP ${response.status}).`);
            }
            const result = await response.json();
            if (!response.ok) {
                throw new Error(result.message || result.error || `GPS update failed (HTTP ${response.status}).`);
            }
            status.textContent = `Sharing live GPS for ${result.busNumber} — updated ${new Date().toLocaleTimeString()}`;
        } catch (exception) {
            showError(exception.message || 'Could not send GPS update to the laptop. Check the hotspot and retry.');
            status.textContent = 'GPS fix acquired, but the server did not receive the update.';
        } finally {
            sending = false;
            if (latestPosition && watchId !== null) sendLatestPosition();
        }
    }

    function onLocationError(locationError) {
        const messages = {
            1: 'Chrome location permission is blocked. Allow Location for this site in Android Chrome settings.',
            2: 'The phone could not get a GPS fix. Turn on Android Location and move outdoors.',
            3: 'Phone GPS timed out. Keep the phone screen awake and try again.'
        };
        showError(messages[locationError.code] || 'Phone GPS failed. Check Android location settings.');
        status.textContent = 'Waiting for phone GPS.';
    }

    function stopSharing() {
        if (watchId !== null) {
            navigator.geolocation.clearWatch(watchId);
            watchId = null;
        }
        latestPosition = null;
        startButton.disabled = false;
        stopButton.disabled = true;
        status.textContent = 'Not sharing';
    }

    function showError(message) {
        error.textContent = message;
        error.classList.remove('d-none');
    }
});
