let map;
let busMarker;
let userMarker;
let routeLine;
let routeStopLayer;
let stompClient = null;
let trackedBusIds = new Set();
let originalStops = [];
let currentBusNumber = '';
let currentBusId = null;

function initTrackingMap(busId, initialLat, initialLon, busNumber, stopsJson, routeId) {
    currentBusId = busId;
    currentBusNumber = busNumber;
    trackedBusIds = new Set([Number(busId)]);
    originalStops = stopsJson || [];

    const startLat = Number.isFinite(initialLat) ? initialLat : 11.0090;
    const startLon = Number.isFinite(initialLon) ? initialLon : 76.9650;

    map = L.map('map').setView([startLat, startLon], 14);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        maxZoom: 19
    }).addTo(map);

    busMarker = L.marker([startLat, startLon], { icon: createBusIcon(busNumber) }).addTo(map);
    busMarker.bindPopup(`<strong>Bus ${escapeHtml(busNumber)}</strong><br>Loading live updates...`).openPopup();
    routeStopLayer = L.layerGroup().addTo(map);
    drawRoute(originalStops, false);

    connectWebSocket(busId, routeId);
    if (routeId) loadCurrentRouteChange(routeId);

    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(pos => {
            userMarker = L.circleMarker([pos.coords.latitude, pos.coords.longitude], {
                radius: 8,
                fillColor: '#10b981',
                color: '#fff',
                weight: 2,
                fillOpacity: 1
            }).addTo(map);
            userMarker.bindPopup('<strong>Your Location</strong>');
        }, () => {});
    }
}

function createBusIcon(busNumber) {
    return L.divIcon({
        className: 'custom-bus-icon',
        html: `<div style="background:#0d6efd;color:#fff;padding:6px 10px;border-radius:20px;font-weight:bold;font-size:12px;box-shadow:0 3px 8px rgba(0,0,0,.3);display:inline-flex;align-items:center;gap:4px;border:2px solid #fff;"><i class="bi bi-bus-front"></i> ${escapeHtml(busNumber)}</div>`,
        iconSize: [100, 30],
        iconAnchor: [50, 15]
    });
}

function drawRoute(stopsJson, isDiversion) {
    routeStopLayer.clearLayers();
    if (routeLine) {
        map.removeLayer(routeLine);
        routeLine = null;
    }

    const stops = (stopsJson || []).map((item, index) => {
        const stop = item.stop || item;
        return {
            name: stop.name || 'Unnamed stop',
            latitude: Number(stop.latitude),
            longitude: Number(stop.longitude),
            order: Number(item.stopOrder || item.order || index + 1),
            fare: item.fareFromStart
        };
    }).filter(stop => Number.isFinite(stop.latitude) && stop.latitude >= -90 && stop.latitude <= 90
        && Number.isFinite(stop.longitude) && stop.longitude >= -180 && stop.longitude <= 180)
        .sort((first, second) => first.order - second.order);

    const latLngs = [];
    stops.forEach((stop, index) => {
        const point = [stop.latitude, stop.longitude];
        latLngs.push(point);
        const circle = L.circleMarker(point, {
            radius: 7,
            fillColor: index === 0 ? '#198754' : index === stops.length - 1 ? '#dc3545' : '#0d6efd',
            color: '#ffffff',
            weight: 2,
            opacity: 1,
            fillOpacity: 0.9
        }).addTo(routeStopLayer);
        const popup = document.createElement('span');
        popup.textContent = `Stop ${stop.order}: ${stop.name}${isDiversion ? ' · temporary route' : ''}`;
        circle.bindPopup(popup);
    });

    if (latLngs.length > 1) {
        routeLine = L.polyline(latLngs, {
            color: isDiversion ? '#dc3545' : '#0d6efd',
            weight: 4,
            opacity: 0.8,
            dashArray: isDiversion ? '8, 6' : '5, 5'
        }).addTo(map);
        map.fitBounds(routeLine.getBounds(), { padding: [40, 40] });
    }
    renderStopList(stops, isDiversion);
}

function renderStopList(stops, isDiversion) {
    const list = document.getElementById('route-stop-list');
    if (!list) return;
    list.replaceChildren();
    stops.forEach(stop => {
        const item = document.createElement('li');
        item.className = 'list-group-item d-flex justify-content-between align-items-center py-2 px-1';
        const label = document.createElement('span');
        const badge = document.createElement('span');
        badge.className = `badge ${isDiversion ? 'bg-danger' : 'bg-secondary'} me-1`;
        badge.textContent = String(stop.order);
        label.append(badge, document.createTextNode(stop.name));
        item.append(label);
        if (stop.fare != null) {
            const fare = document.createElement('span');
            fare.className = 'badge bg-light text-dark';
            fare.textContent = `Rs. ${stop.fare}`;
            item.append(fare);
        }
        list.append(item);
    });
}

function connectWebSocket(busId, routeId) {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, () => {
        const handleLocation = message => {
            const data = JSON.parse(message.body);
            if (data && trackedBusIds.has(Number(data.busId))) updateBusDisplay(data);
        };

        stompClient.subscribe('/topic/bus-location/' + busId, handleLocation);
        stompClient.subscribe('/topic/bus-location', handleLocation);
        if (routeId) {
            stompClient.subscribe(`/topic/route-changes/${routeId}`, message => {
                const change = message.body ? JSON.parse(message.body) : null;
                applyRouteChange(change);
            });
        }
    }, () => {
        window.setTimeout(() => connectWebSocket(busId, routeId), 5000);
    });
}

async function loadCurrentRouteChange(routeId) {
    const localDate = new Date();
    const date = `${localDate.getFullYear()}-${String(localDate.getMonth() + 1).padStart(2, '0')}-${String(localDate.getDate()).padStart(2, '0')}`;
    try {
        const response = await fetch(`/api/routes/${encodeURIComponent(routeId)}/service-change?date=${encodeURIComponent(date)}`);
        if (response.status === 404) return;
        if (!response.ok) throw new Error(`Could not load today's route information (HTTP ${response.status}).`);
        applyRouteChange(await response.json());
    } catch (error) {
        const banner = document.getElementById('route-change-banner');
        banner.textContent = error.message || 'Could not load route information.';
        banner.classList.remove('d-none');
    }
}

function applyRouteChange(change) {
    const banner = document.getElementById('route-change-banner');
    if (!change) {
        trackedBusIds = new Set([Number(currentBusId)]);
        currentBusNumber = document.querySelector('h3 span')?.textContent || currentBusNumber;
        busMarker.setIcon(createBusIcon(currentBusNumber));
        busMarker.setOpacity(1);
        drawRoute(originalStops, false);
        banner.classList.add('d-none');
        banner.textContent = '';
        return;
    }

    drawRoute(change.stops || [], true);
    const notices = [`Temporary route change on ${change.effectiveDate}: ${change.reason}.`];
    if (change.affectedBusNumber) notices.push(`Affected bus: ${change.affectedBusNumber}.`);
    if (change.replacementBusNumber) notices.push(`Replacement bus: ${change.replacementBusNumber}.`);
    banner.textContent = notices.join(' ');
    banner.classList.remove('d-none');

    if (change.replacementBusId) {
        trackedBusIds = new Set([Number(change.replacementBusId)]);
        currentBusNumber = change.replacementBusNumber;
        busMarker.setIcon(createBusIcon(currentBusNumber));
        busMarker.setOpacity(0);
    } else if (change.affectedBusId) {
        trackedBusIds = new Set([Number(change.affectedBusId)]);
    }
}

function updateBusDisplay(data) {
    if (!data || !Number.isFinite(Number(data.latitude)) || !Number.isFinite(Number(data.longitude))) return;
    busMarker.setLatLng([Number(data.latitude), Number(data.longitude)]);
    busMarker.setOpacity(1);

    const statusEl = document.getElementById('live-status-badge');
    if (statusEl) {
        statusEl.className = 'status-badge status-' + data.status;
        statusEl.innerText = String(data.status || 'UNKNOWN').replaceAll('_', ' ');
    }

    const sourceEl = document.getElementById('live-source-badge');
    if (sourceEl) {
        sourceEl.className = 'source-badge source-' + (data.source || 'GPS');
        const sourceLabel = data.source === 'MANUAL' ? 'MANUAL LOCATION UPDATE'
            : data.status === 'GPS_SIGNAL_LOST' ? 'LAST KNOWN LOCATION'
                : data.source === 'PHONE_GPS' ? 'PHONE GPS' : 'LIVE GPS';
        sourceEl.textContent = sourceLabel;
    }

    const speed = document.getElementById('live-speed');
    if (speed) speed.innerText = Math.round(data.speed || 0) + ' km/h';
    const nextStop = document.getElementById('live-next-stop');
    if (nextStop) nextStop.innerText = data.nextStopName || 'En route';
    const eta = document.getElementById('live-eta');
    if (eta) eta.innerText = (data.etaMinutes || 5) + ' mins';
    const distance = document.getElementById('live-distance');
    if (distance) distance.innerText = (data.distanceToNextKm ? data.distanceToNextKm.toFixed(1) : '1.8') + ' km';
    const updated = document.getElementById('live-updated-at');
    if (updated) updated.innerText = data.lastUpdated || 'Just now';

    busMarker.setPopupContent(`
        <div style="min-width:160px;">
            <strong>Bus ${escapeHtml(data.busNumber || currentBusNumber)}</strong><br>
            <span class="badge bg-primary mb-1">${escapeHtml(data.source || 'GPS')}</span><br>
            Speed: <b>${Math.round(data.speed || 0)} km/h</b><br>
            Next: <b>${escapeHtml(data.nextStopName || 'N/A')}</b><br>
            ETA: <b>${data.etaMinutes || 5} mins</b>
        </div>
    `);
}

function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, character => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    })[character]);
}
