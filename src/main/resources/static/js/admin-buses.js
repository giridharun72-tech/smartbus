document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('addBusForm');
    const modalElement = document.getElementById('addBusModal');
    const locationAlert = document.getElementById('locationAlert');
    const gpsLockBadge = document.getElementById('gpsLockBadge');
    const saveButton = document.getElementById('saveBusButton');
    const latitudeInput = document.getElementById('latitude');
    const longitudeInput = document.getElementById('longitude');

    document.getElementById('getDeviceLocation').addEventListener('click', () => {
        locationAlert.classList.add('d-none');
        locationAlert.textContent = '';
        gpsLockBadge.classList.add('d-none');

        if (!navigator.geolocation) {
            showLocationError('Geolocation requires HTTPS or localhost/Chrome flag settings.');
            return;
        }

        navigator.geolocation.getCurrentPosition(position => {
            latitudeInput.value = position.coords.latitude.toFixed(6);
            longitudeInput.value = position.coords.longitude.toFixed(6);
            gpsLockBadge.textContent = `● GPS Locked (±${Math.round(position.coords.accuracy)}m)`;
            gpsLockBadge.className = 'badge bg-success';
        }, error => {
            const messageByCode = {
                1: 'Location permission was denied. Allow location access in your browser settings and try again.',
                2: 'Your device could not determine its location. Check that location services are enabled.',
                3: 'The location request timed out. Try again where GPS reception is better.'
            };
            showLocationError(messageByCode[error.code] || 'Unable to get your device location. Please try again.');
        }, { enableHighAccuracy: true, timeout: 10000 });
    });

    form.addEventListener('submit', async event => {
        event.preventDefault();
        locationAlert.classList.add('d-none');

        if (!form.reportValidity()) return;

        const payload = {
            busNumber: form.elements.busNumber.value.trim(),
            route: form.elements.route.value.trim(),
            driverName: form.elements.driverName.value.trim(),
            totalSeats: Number(form.elements.totalSeats.value),
            currentLatitude: Number(latitudeInput.value),
            currentLongitude: Number(longitudeInput.value),
            status: 'RUNNING'
        };
        const headers = { 'Content-Type': 'application/json' };
        const token = sessionStorage.getItem('token') || localStorage.getItem('token');
        if (token) headers.Authorization = `Bearer ${token}`;

        saveButton.disabled = true;
        try {
            const response = await fetch('/api/admin/buses', {
                method: 'POST',
                credentials: 'same-origin',
                headers,
                body: JSON.stringify(payload)
            });
            const contentType = response.headers.get('content-type') || '';
            if (!contentType.includes('json')) {
                if (response.redirected || response.url.endsWith('/login')) {
                    throw new Error('Your admin session may have expired. Sign in again and retry.');
                }
                throw new Error(`The server returned an unexpected response (HTTP ${response.status}).`);
            }
            const result = await response.json();
            if (![200, 201].includes(response.status)) {
                throw new Error(result.message || result.error || `Bus could not be saved (HTTP ${response.status}).`);
            }

            addOrUpdateFleetRow(result);
            bootstrap.Modal.getOrCreateInstance(modalElement).hide();
            form.reset();
            gpsLockBadge.className = 'badge bg-secondary d-none';
            gpsLockBadge.textContent = '';
            showToast('New bus was added successfully.', 'success');
        } catch (error) {
            showLocationError(error.message || 'Unable to save the bus. Please try again.');
        } finally {
            saveButton.disabled = false;
        }
    });

    modalElement.addEventListener('hidden.bs.modal', () => {
        locationAlert.classList.add('d-none');
    });

    if (window.SockJS && window.Stomp) {
        const stompClient = Stomp.over(new SockJS('/ws'));
        stompClient.debug = null;
        stompClient.connect({}, () => {
            stompClient.subscribe('/topic/fleet-updates', message => {
                const bus = JSON.parse(message.body);
                if (bus && bus.busId) {
                    addOrUpdateFleetRow({
                        id: bus.busId,
                        busNumber: bus.busNumber,
                        registrationNumber: bus.registrationNumber,
                        busType: 'STANDARD',
                        capacity: bus.capacity || 40,
                        routeName: bus.routeName,
                        driverName: bus.driverName,
                        gpsDeviceId: bus.gpsDeviceId,
                        status: bus.status
                    });
                }
            });
        });
    }

    if (new URLSearchParams(window.location.search).get('addBus') === 'true') {
        bootstrap.Modal.getOrCreateInstance(modalElement).show();
        window.history.replaceState({}, document.title, window.location.pathname);
    }

    function showLocationError(message) {
        locationAlert.textContent = message;
        locationAlert.classList.remove('d-none');
    }
});

function addOrUpdateFleetRow(bus) {
    const tableBody = document.querySelector('.table-responsive tbody');
    if (!tableBody || !bus.id) return;

    let row = tableBody.querySelector(`tr[data-bus-id="${CSS.escape(String(bus.id))}"]`);
    const isNew = !row;
    if (!row) {
        row = document.createElement('tr');
        row.dataset.busId = bus.id;
        row.append(document.createElement('td'), document.createElement('td'),
            document.createElement('td'), document.createElement('td'),
            document.createElement('td'), document.createElement('td'),
            document.createElement('td'), document.createElement('td'),
            document.createElement('td'));
        tableBody.appendChild(row);
    }

    const cells = row.cells;
    cells[0].replaceChildren();
    const number = document.createElement('strong');
    number.className = 'text-primary';
    number.textContent = bus.busNumber || '';
    cells[0].appendChild(number);
    cells[1].textContent = bus.registrationNumber || '';
    cells[2].replaceChildren();
    const type = document.createElement('span');
    type.className = 'badge bg-secondary';
    type.textContent = bus.busType || 'STANDARD';
    cells[2].appendChild(type);
    cells[3].textContent = `${bus.capacity || 40} seats`;
    cells[4].textContent = bus.routeName || 'Local Route';
    cells[5].textContent = bus.driverName || 'Unassigned';
    cells[6].replaceChildren();
    const gpsId = document.createElement('code');
    gpsId.textContent = bus.gpsDeviceId || '';
    cells[6].appendChild(gpsId);
    cells[7].replaceChildren();
    const status = document.createElement('span');
    status.className = `status-badge status-${bus.status || 'RUNNING'}`;
    status.textContent = bus.status || 'RUNNING';
    cells[7].appendChild(status);

    if (isNew) {
        const cancelButton = document.createElement('button');
        cancelButton.type = 'button';
        cancelButton.className = 'btn btn-sm btn-outline-danger';
        cancelButton.innerHTML = '<i class="bi bi-x-circle"></i> Cancel Bus';
        cancelButton.addEventListener('click', () => updateStatus(bus.id, 'CANCELLED'));
        cells[8].appendChild(cancelButton);
        const count = document.getElementById('fleet-count');
        if (count) count.textContent = String(Number(count.textContent) + 1);
    }
}
