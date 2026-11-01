document.addEventListener('DOMContentLoaded', () => {
    const panel = document.getElementById('driver-gps-panel');
    if (!panel) return;

    const startButton = document.getElementById('driver-gps-start');
    const stopButton = document.getElementById('driver-gps-stop');
    const status = document.getElementById('driver-gps-status');
    const error = document.getElementById('driver-gps-error');
    const routeMapElement = document.getElementById('driver-route-map');
    const modal = document.getElementById('driver-location-modal');
    const modalStartButton = document.getElementById('driver-modal-gps-start');
    const modalError = document.getElementById('driver-modal-gps-error');
    const busNumber = panel.dataset.busNumber;
    let routeMap = null;
    let busMarker = null;
    let watchId = null;
    let sending = false;
    let latestPosition = null;
    let lastSentAt = 0;
    let sendTimer = null;

    if (modal && window.bootstrap) {
        const locationModal = bootstrap.Modal.getOrCreateInstance(modal);
        modal.addEventListener('shown.bs.modal', () => {
            initializeRouteMap();
            if (routeMap) routeMap.invalidateSize();
        });
        locationModal.show();
    }

    if (modalStartButton) {
        modalStartButton.addEventListener('click', () => {
            startButton.click();
            if (watchId !== null && modal) bootstrap.Modal.getOrCreateInstance(modal).hide();
        });
    }

    startButton.addEventListener('click', () => {
        clearError();
        if (modalError) {
            modalError.textContent = '';
            modalError.classList.add('d-none');
        }
        if (!window.isSecureContext || !navigator.geolocation) {
            showError('Device location requires HTTPS or localhost. Open the secure site and try again.');
            return;
        }
        if (watchId !== null) return;

        startButton.disabled = true;
        stopButton.disabled = false;
        status.textContent = 'Waiting for a GPS fix from this device…';
        watchId = navigator.geolocation.watchPosition(onPosition, onLocationError, {
            enableHighAccuracy: true,
            maximumAge: 1000,
            timeout: 20000
        });
    });

    stopButton.addEventListener('click', stopSharing);

    function onPosition(position) {
        latestPosition = position;
        updateBusMarker(position.coords.latitude, position.coords.longitude);
        status.textContent = `GPS acquired (±${Math.round(position.coords.accuracy)} m); sharing with SmartBus…`;
        const wait = Math.max(0, 5000 - (Date.now() - lastSentAt));
        if (wait > 0) {
            if (sendTimer === null) {
                sendTimer = window.setTimeout(() => {
                    sendTimer = null;
                    sendLatestPosition();
                }, wait);
            }
            return;
        }
        sendLatestPosition();
    }

    async function sendLatestPosition() {
        if (sending || !latestPosition || watchId === null) return;
        sending = true;
        const position = latestPosition;
        latestPosition = null;
        try {
            const response = await fetch('/api/driver/location', {
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
                    ? 'Your driver session has expired. Sign in again to share location.'
                    : `Unexpected server response (HTTP ${response.status}).`);
            }
            const result = await response.json();
            if (!response.ok) {
                throw new Error(result.message || result.error || `Location update failed (HTTP ${response.status}).`);
            }
            lastSentAt = Date.now();
            status.textContent = `Sharing live GPS for Bus ${busNumber} — updated ${new Date().toLocaleTimeString()}.`;
            clearError();
        } catch (exception) {
            showError(exception.message || 'Could not send your location. Check your connection and retry.');
        } finally {
            sending = false;
            if (latestPosition && watchId !== null) sendLatestPosition();
        }
    }

    function onLocationError(locationError) {
        const messages = {
            1: 'Location permission was denied. Allow location access for this site in your browser settings.',
            2: 'Your device could not determine its location. Turn on Location Services and try again.',
            3: 'The GPS request timed out. Move to an area with better GPS reception and try again.'
        };
        const message = messages[locationError.code] || 'Device location is unavailable.';
        showError(message);
        if (modalError && modal && modal.classList.contains('show')) {
            modalError.textContent = message;
            modalError.classList.remove('d-none');
        }
        status.textContent = 'Waiting for device location.';
    }

    function stopSharing() {
        if (watchId !== null) {
            navigator.geolocation.clearWatch(watchId);
            watchId = null;
        }
        if (sendTimer !== null) {
            window.clearTimeout(sendTimer);
            sendTimer = null;
        }
        latestPosition = null;
        startButton.disabled = false;
        stopButton.disabled = true;
        status.textContent = 'Location sharing is off.';
    }

    function showError(message) {
        error.textContent = message;
        error.classList.remove('d-none');
        if (modalError && modal && modal.classList.contains('show')) {
            modalError.textContent = message;
            modalError.classList.remove('d-none');
        }
    }

    function clearError() {
        error.textContent = '';
        error.classList.add('d-none');
    }

    function initializeRouteMap() {
        if (!routeMapElement || routeMap || !window.L) return;
        const stops = Array.from(routeMapElement.querySelectorAll('.driver-route-stop'))
            .map(stop => ({
                name: stop.dataset.name,
                order: Number(stop.dataset.order),
                latitude: stop.dataset.lat === '' ? NaN : Number(stop.dataset.lat),
                longitude: stop.dataset.lon === '' ? NaN : Number(stop.dataset.lon)
            }))
            .filter(stop => Number.isFinite(stop.latitude) && Number.isFinite(stop.longitude)
                && stop.latitude >= -90 && stop.latitude <= 90
                && stop.longitude >= -180 && stop.longitude <= 180);

        if (stops.length === 0) {
            const emptyMessage = document.getElementById('driver-route-empty');
            if (emptyMessage) emptyMessage.classList.remove('d-none');
            routeMapElement.classList.add('d-none');
            return;
        }

        routeMap = L.map(routeMapElement).setView([stops[0].latitude, stops[0].longitude], 13);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; OpenStreetMap contributors',
            maxZoom: 19
        }).addTo(routeMap);

        const route = stops
            .sort((first, second) => first.order - second.order)
            .map((stop, index) => {
                const point = [stop.latitude, stop.longitude];
                const stopPopup = document.createElement('span');
                stopPopup.textContent = `${stop.order}. ${stop.name}`;
                L.circleMarker(point, {
                    radius: 7,
                    color: '#fff',
                    weight: 2,
                    fillColor: index === 0 ? '#198754' : index === stops.length - 1 ? '#dc3545' : '#0d6efd',
                    fillOpacity: 1
                }).addTo(routeMap).bindPopup(stopPopup);
                return point;
            });
        if (route.length > 1) {
            L.polyline(route, { color: '#0d6efd', weight: 4 }).addTo(routeMap);
        }
        routeMap.fitBounds(L.latLngBounds(route), { padding: [24, 24] });
    }

    function updateBusMarker(latitude, longitude) {
        if (!routeMap) return;
        const point = [latitude, longitude];
        if (busMarker) {
            busMarker.setLatLng(point);
            return;
        }
        busMarker = L.circleMarker(point, {
            radius: 10,
            color: '#fff',
            weight: 3,
            fillColor: '#ffc107',
            fillOpacity: 1
        }).addTo(routeMap);
        const busPopup = document.createElement('span');
        busPopup.textContent = `Bus ${busNumber} · Current phone location`;
        busMarker.bindPopup(busPopup);
    }
});
