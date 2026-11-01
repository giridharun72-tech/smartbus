// BCU Live Fleet Map and Real-time Signal Monitoring
let fleetMap;
let busMarkers = {};
let stompClient = null;
const busDetailsById = new Map();

function initBcuFleetMap() {
    fleetMap = L.map('fleet-map').setView([11.0168, 76.9650], 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        maxZoom: 19
    }).addTo(fleetMap);

    loadFleetData();
    connectBcuWebSocket();
}

function loadFleetData() {
    fetch('/api/bcu/fleet')
        .then(r => r.json())
        .then(buses => {
            buses.forEach(bus => {
                const busId = Number(bus.busId ?? bus.id);
                if (Number.isFinite(busId)) {
                    busDetailsById.set(busId, {
                        driverName: bus.driverName,
                        driverPhone: bus.driverPhone
                    });
                }
                updateOrAddBusMarker(bus);
            });
        })
        .catch(err => console.error('Error loading fleet data', err));
}

function getMarkerColor(status, source) {
    if (status === 'EMERGENCY') return '#dc3545';
    if (status === 'BREAKDOWN') return '#b02a37';
    if (status === 'GPS_SIGNAL_LOST') return '#fd7e14';
    if (source === 'MANUAL' || status === 'MANUAL_LOCATION_MODE') return '#0dcaf0';
    if (status === 'DELAYED') return '#ffc107';
    return '#198754'; // RUNNING
}

function updateOrAddBusMarker(bus) {
    if (!Number.isFinite(Number(bus.latitude)) || !Number.isFinite(Number(bus.longitude))) return;

    const latLng = [Number(bus.latitude), Number(bus.longitude)];
    const color = getMarkerColor(bus.status, bus.source);
    const iconElement = document.createElement('div');
    iconElement.style.cssText = 'color:#fff;border:2px solid #fff;border-radius:18px;padding:4px 8px;font-size:11px;font-weight:bold;box-shadow:0 2px 6px rgba(0,0,0,0.3);display:inline-flex;align-items:center;gap:4px;';
    iconElement.style.backgroundColor = color;
    const icon = document.createElement('i');
    icon.className = 'bi bi-bus-front';
    const busLabel = document.createElement('span');
    busLabel.textContent = String(bus.busNumber || 'Bus');
    iconElement.append(icon, busLabel);

    const customIcon = L.divIcon({
        className: 'bcu-bus-marker',
        html: iconElement,
        iconSize: [85, 28],
        iconAnchor: [42, 14]
    });
    const popup = createBusPopup(bus);

    if (busMarkers[bus.busId]) {
        busMarkers[bus.busId].setLatLng(latLng);
        busMarkers[bus.busId].setIcon(customIcon);
        busMarkers[bus.busId].setPopupContent(popup);
    } else {
        const marker = L.marker(latLng, { icon: customIcon }).addTo(fleetMap);
        marker.bindPopup(popup);
        busMarkers[bus.busId] = marker;
    }
}

function createBusPopup(bus) {
    const popup = document.createElement('div');
    popup.style.minWidth = '220px';
    popup.style.fontSize = '13px';

    const header = document.createElement('div');
    header.className = 'd-flex justify-content-between align-items-center mb-1';
    const title = document.createElement('strong');
    title.className = 'fs-6';
    title.textContent = `Bus ${bus.busNumber || ''}`;
    const status = document.createElement('span');
    status.className = `badge ${bus.status === 'RUNNING' ? 'bg-success' : bus.status === 'GPS_SIGNAL_LOST' ? 'bg-warning text-dark' : 'bg-danger'}`;
    status.textContent = String(bus.status || 'UNKNOWN');
    header.append(title, status);
    popup.appendChild(header);

    const route = document.createElement('div');
    route.className = 'text-muted small mb-2';
    route.textContent = String(bus.routeName || 'Local Route');
    popup.appendChild(route);

    const table = document.createElement('table');
    table.className = 'table table-sm table-borderless small mb-2';
    const addRow = (label, value) => {
        const row = document.createElement('tr');
        const labelCell = document.createElement('td');
        labelCell.className = 'text-muted p-0';
        labelCell.textContent = label;
        const valueCell = document.createElement('td');
        valueCell.className = 'p-0';
        const boldValue = document.createElement('b');
        boldValue.textContent = String(value);
        valueCell.appendChild(boldValue);
        row.append(labelCell, valueCell);
        table.appendChild(row);
    };
    const speed = Number(bus.speed);
    addRow('Location:', bus.nextStopName || 'En route');
    addRow('Speed:', `${Number.isFinite(speed) ? Math.round(speed) : 0} km/h`);
    addRow('Source:', bus.source || 'GPS');
    addRow('Driver:', `${bus.driverName || 'Assigned'} (${bus.driverPhone || 'N/A'})`);
    addRow('Last GPS:', bus.lastUpdated || 'N/A');
    popup.appendChild(table);

    const actions = document.createElement('div');
    actions.className = 'd-grid gap-1';
    if (bus.status === 'GPS_SIGNAL_LOST') {
        const manualModeButton = document.createElement('button');
        manualModeButton.type = 'button';
        manualModeButton.className = 'btn btn-sm btn-primary';
        manualModeButton.textContent = 'Enable Manual Mode';
        manualModeButton.addEventListener('click', () => enableManualMode(bus.busId, bus.busNumber));
        actions.appendChild(manualModeButton);
    }

    const phone = String(bus.driverPhone || '').replace(/[^\d+(). -]/g, '');
    if (phone) {
        const callButton = document.createElement('a');
        callButton.href = `tel:${phone}`;
        callButton.className = 'btn btn-sm btn-outline-secondary';
        callButton.textContent = 'Call Driver';
        actions.appendChild(callButton);
    }
    popup.appendChild(actions);
    return popup;
}

function connectBcuWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, function () {
        // Fleet location updates
        stompClient.subscribe('/topic/bus-location', function (message) {
            const bus = JSON.parse(message.body);
            updateOrAddBusMarker({ ...busDetailsById.get(Number(bus.busId)), ...bus });
        });

        stompClient.subscribe('/topic/fleet-updates', function (message) {
            const bus = JSON.parse(message.body);
            const busId = Number(bus.busId ?? bus.id);
            if (Number.isFinite(busId)) {
                busDetailsById.set(busId, {
                    driverName: bus.driverName,
                    driverPhone: bus.driverPhone
                });
            }
            updateOrAddBusMarker(bus);
        });

        // BCU high-priority alerts
        stompClient.subscribe('/topic/bcu-alerts', function (message) {
            const alert = JSON.parse(message.body);
            handleBcuAlert(alert);
        });
    });
}

function handleBcuAlert(alert) {
    const alertBox = document.getElementById('bcu-live-alerts');
    if (alertBox) {
        const div = document.createElement('div');
        div.className = 'alert alert-danger alert-dismissible fade show shadow-sm mb-2';
        const heading = document.createElement('strong');
        const icon = document.createElement('i');
        icon.className = 'bi bi-exclamation-octagon-fill me-1';
        heading.append(icon, document.createTextNode(`${String(alert.type || 'FLEET ALERT')}: `));
        div.append(heading, document.createTextNode(String(alert.message || '')));

        const busId = Number(alert.busId);
        if (Number.isSafeInteger(busId) && busId > 0) {
            const manualModeButton = document.createElement('button');
            manualModeButton.type = 'button';
            manualModeButton.className = 'btn btn-sm btn-dark ms-3';
            manualModeButton.textContent = 'Enable Manual Mode';
            manualModeButton.addEventListener('click', () => enableManualMode(busId, alert.busNumber));
            div.appendChild(manualModeButton);
        }

        const closeButton = document.createElement('button');
        closeButton.type = 'button';
        closeButton.className = 'btn-close';
        closeButton.setAttribute('data-bs-dismiss', 'alert');
        closeButton.setAttribute('aria-label', 'Close');
        div.appendChild(closeButton);
        alertBox.prepend(div);
    }
    showToast(alert.message || 'A fleet alert was received.', 'error');
}

function enableManualMode(busId, busNumber) {
    fetch('/api/bcu/manual-location/enable?busId=' + busId, { method: 'POST' })
        .then(r => r.json())
        .then(data => {
            showToast(data.message, 'success');
            loadFleetData();
        })
        .catch(() => showToast('Failed to enable manual location mode', 'error'));
}