document.addEventListener('DOMContentLoaded', () => {
    const createForm = document.getElementById('create-bus-driver-form');
    const createMessage = document.getElementById('form-message');
    const createButton = document.getElementById('createBusButton');
    const newStopsSection = document.getElementById('new-route-stops-section');
    const newStopsContainer = document.getElementById('new-route-stops');
    const routeNames = Array.from(document.querySelectorAll('.route-option'))
        .map(option => (option.dataset.routeName || '').trim().toLowerCase());

    addStopRow(newStopsContainer);
    addStopRow(newStopsContainer);
    document.getElementById('add-new-route-stop').addEventListener('click', () => addStopRow(newStopsContainer));

    document.getElementById('route').addEventListener('input', event => {
        const isNewRoute = !routeNames.includes(event.target.value.trim().toLowerCase());
        newStopsSection.classList.toggle('d-none', !isNewRoute);
        newStopsContainer.querySelectorAll('input').forEach(input => {
            input.required = isNewRoute;
        });
    });

    createForm.addEventListener('submit', async event => {
        event.preventDefault();
        if (!createForm.reportValidity()) return;

        const payload = Object.fromEntries(new FormData(createForm).entries());
        payload.totalSeats = Number(payload.totalSeats);
        payload.routeStops = newStopsSection.classList.contains('d-none') ? [] : readStops(newStopsContainer);
        if (!newStopsSection.classList.contains('d-none') && payload.routeStops.length < 2) {
            showMessage(createMessage, 'Add at least two route stops with coordinates.', 'danger');
            return;
        }

        createButton.disabled = true;
        hideMessage(createMessage);
        try {
            const result = await requestJson('/api/bcu/buses', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            createForm.reset();
            newStopsSection.classList.add('d-none');
            newStopsContainer.querySelectorAll('input').forEach(input => { input.required = false; });
            showMessage(createMessage, `Bus ${result.busNumber} created (Bus ID ${result.busId}). Driver login ID: ${result.driverId}.`, 'success');
            window.setTimeout(() => window.location.reload(), 1200);
        } catch (exception) {
            showMessage(createMessage, exception.message, 'danger');
        } finally {
            document.getElementById('aadhaarNumber').value = '';
            document.getElementById('password').value = '';
            createButton.disabled = false;
        }
    });

    const editModalElement = document.getElementById('edit-bus-modal');
    const editModal = bootstrap.Modal.getOrCreateInstance(editModalElement);
    const editMessage = document.getElementById('edit-bus-message');
    document.querySelectorAll('.edit-bus').forEach(button => {
        button.addEventListener('click', () => {
            document.getElementById('edit-bus-id').value = button.dataset.busId;
            document.getElementById('edit-bus-number').value = button.dataset.busNumber || '';
            document.getElementById('edit-registration').value = button.dataset.registration || '';
            document.getElementById('edit-route').value = button.dataset.routeId || '';
            document.getElementById('edit-seats').value = button.dataset.seats || '';
            document.getElementById('edit-driver-name').value = button.dataset.driverName || '';
            document.getElementById('edit-driver-phone').value = button.dataset.driverPhone || '';
            document.getElementById('edit-license').value = button.dataset.license || '';
            hideMessage(editMessage);
            editModal.show();
        });
    });

    document.getElementById('edit-bus-form').addEventListener('submit', async event => {
        event.preventDefault();
        const form = event.currentTarget;
        if (!form.reportValidity()) return;
        const saveButton = document.getElementById('save-bus-details');
        saveButton.disabled = true;
        hideMessage(editMessage);
        const busId = document.getElementById('edit-bus-id').value;
        const payload = {
            busNumber: document.getElementById('edit-bus-number').value,
            registrationNumber: document.getElementById('edit-registration').value,
            routeId: Number(document.getElementById('edit-route').value),
            totalSeats: Number(document.getElementById('edit-seats').value),
            driverName: document.getElementById('edit-driver-name').value,
            driverPhone: document.getElementById('edit-driver-phone').value,
            licenseNumber: document.getElementById('edit-license').value
        };
        try {
            await requestJson(`/api/bcu/buses/${encodeURIComponent(busId)}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            showMessage(editMessage, 'Bus and driver details updated.', 'success');
            window.setTimeout(() => window.location.reload(), 800);
        } catch (exception) {
            showMessage(editMessage, exception.message, 'danger');
        } finally {
            saveButton.disabled = false;
        }
    });

    const routeSelect = document.getElementById('change-route');
    const dateInput = document.getElementById('change-date');
    const affectedSelect = document.getElementById('affected-bus');
    const replacementSelect = document.getElementById('replacement-bus');
    const routeChangeForm = document.getElementById('route-change-form');
    const changeMessage = document.getElementById('change-message');
    const changeStops = document.getElementById('change-stops');
    const cancelChangeButton = document.getElementById('cancel-route-change');

    routeSelect.addEventListener('change', () => {
        filterAffectedBuses();
        loadServiceChange();
    });
    dateInput.addEventListener('change', loadServiceChange);
    affectedSelect.addEventListener('change', filterReplacementBuses);
    document.getElementById('add-change-stop').addEventListener('click', () => addStopRow(changeStops));

    routeChangeForm.addEventListener('submit', async event => {
        event.preventDefault();
        if (!routeChangeForm.reportValidity()) return;
        const stops = readStops(changeStops);
        if (stops.length < 2) {
            showMessage(changeMessage, 'A detour must include at least two stops with valid coordinates.', 'danger');
            return;
        }

        const routeId = routeSelect.value;
        const payload = {
            effectiveDate: dateInput.value,
            reason: document.getElementById('change-reason').value.trim(),
            affectedBusId: affectedSelect.value ? Number(affectedSelect.value) : null,
            replacementBusId: replacementSelect.value ? Number(replacementSelect.value) : null,
            stops
        };
        const submitButton = routeChangeForm.querySelector('button[type="submit"]');
        submitButton.disabled = true;
        hideMessage(changeMessage);
        try {
            await requestJson(`/api/bcu/routes/${encodeURIComponent(routeId)}/service-change`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            showMessage(changeMessage, 'The date-limited route update is live. Passengers subscribed to buses on this route have been notified.', 'success');
            cancelChangeButton.classList.remove('d-none');
        } catch (exception) {
            showMessage(changeMessage, exception.message, 'danger');
        } finally {
            submitButton.disabled = false;
        }
    });

    cancelChangeButton.addEventListener('click', async () => {
        const routeId = routeSelect.value;
        if (!routeId || !dateInput.value) return;
        cancelChangeButton.disabled = true;
        try {
            await requestJson(`/api/bcu/routes/${encodeURIComponent(routeId)}/service-change?date=${encodeURIComponent(dateInput.value)}`, {
                method: 'DELETE'
            });
            showMessage(changeMessage, 'Temporary route update ended. The normal route is restored.', 'success');
            cancelChangeButton.classList.add('d-none');
            document.getElementById('change-reason').value = '';
            affectedSelect.value = '';
            replacementSelect.value = '';
            filterAffectedBuses();
            await loadOriginalStops();
        } catch (exception) {
            showMessage(changeMessage, exception.message, 'danger');
        } finally {
            cancelChangeButton.disabled = false;
        }
    });

    if (dateInput.value) {
        dateInput.min = dateInput.value;
    }

    function filterAffectedBuses() {
        const routeId = routeSelect.value;
        Array.from(affectedSelect.options).forEach(option => {
            if (!option.value) return;
            const allowed = option.dataset.routeId === routeId;
            option.hidden = !allowed;
            if (!allowed && affectedSelect.value === option.value) affectedSelect.value = '';
        });
        filterReplacementBuses();
    }

    function filterReplacementBuses() {
        Array.from(replacementSelect.options).forEach(option => {
            if (!option.value) return;
            const isAffectedBus = option.value === affectedSelect.value;
            option.hidden = isAffectedBus;
            if (isAffectedBus && replacementSelect.value === option.value) replacementSelect.value = '';
        });
    }

    async function loadServiceChange() {
        const routeId = routeSelect.value;
        if (!routeId || !dateInput.value) {
            changeStops.replaceChildren();
            cancelChangeButton.classList.add('d-none');
            document.getElementById('change-reason').value = '';
            affectedSelect.value = '';
            replacementSelect.value = '';
            return;
        }
        hideMessage(changeMessage);
        try {
            const response = await fetch(`/api/routes/${encodeURIComponent(routeId)}/service-change?date=${encodeURIComponent(dateInput.value)}`, {
                credentials: 'same-origin'
            });
            if (response.status === 404) {
                cancelChangeButton.classList.add('d-none');
                document.getElementById('change-reason').value = '';
                affectedSelect.value = '';
                replacementSelect.value = '';
                filterAffectedBuses();
                await loadOriginalStops();
                return;
            }
            const change = await parseResponse(response);
            document.getElementById('change-reason').value = change.reason || '';
            affectedSelect.value = change.affectedBusId || '';
            replacementSelect.value = change.replacementBusId || '';
            filterAffectedBuses();
            populateStops(change.stops || []);
            cancelChangeButton.classList.remove('d-none');
        } catch (exception) {
            showMessage(changeMessage, exception.message, 'danger');
        }
    }

    async function loadOriginalStops() {
        const routeId = routeSelect.value;
        if (!routeId) {
            changeStops.replaceChildren();
            return;
        }
        try {
            const response = await fetch(`/api/routes/${encodeURIComponent(routeId)}/stops`, {
                credentials: 'same-origin'
            });
            const stops = await parseResponse(response);
            populateStops(stops);
        } catch (exception) {
            showMessage(changeMessage, exception.message, 'danger');
        }
    }

    function populateStops(stops) {
        changeStops.replaceChildren();
        stops.forEach(stop => addStopRow(changeStops, stop));
        if (stops.length < 2) {
            addStopRow(changeStops);
            addStopRow(changeStops);
        }
    }

    function addStopRow(container, values = {}) {
        const row = document.createElement('div');
        row.className = 'row g-2 align-items-end route-stop-row';
        const fields = [
            { key: 'name', label: 'Stop name', type: 'text', value: values.name || '', required: true },
            { key: 'latitude', label: 'Latitude', type: 'number', value: values.latitude ?? '', required: true, min: -90, max: 90, step: 'any' },
            { key: 'longitude', label: 'Longitude', type: 'number', value: values.longitude ?? '', required: true, min: -180, max: 180, step: 'any' }
        ];
        fields.forEach(field => {
            const column = document.createElement('div');
            column.className = field.key === 'name' ? 'col-md-5' : 'col-md-3';
            const label = document.createElement('label');
            label.className = 'form-label small mb-1';
            label.textContent = field.label;
            const input = document.createElement('input');
            input.className = 'form-control';
            input.dataset.field = field.key;
            input.type = field.type;
            input.value = field.value;
            input.required = field.required;
            if (field.min !== undefined) input.min = field.min;
            if (field.max !== undefined) input.max = field.max;
            if (field.step) input.step = field.step;
            column.append(label, input);
            row.append(column);
        });
        const actionColumn = document.createElement('div');
        actionColumn.className = 'col-md-1 d-grid';
        const removeButton = document.createElement('button');
        removeButton.type = 'button';
        removeButton.className = 'btn btn-outline-danger';
        removeButton.setAttribute('aria-label', 'Remove stop');
        removeButton.innerHTML = '<i class="bi bi-trash"></i>';
        removeButton.addEventListener('click', () => row.remove());
        actionColumn.append(removeButton);
        row.append(actionColumn);
        container.append(row);
    }

    function readStops(container) {
        return Array.from(container.querySelectorAll('.route-stop-row')).map(row => {
            const value = key => row.querySelector(`[data-field="${key}"]`).value.trim();
            return { name: value('name'), latitude: Number(value('latitude')), longitude: Number(value('longitude')) };
        });
    }

    async function requestJson(url, options) {
        const response = await fetch(url, { credentials: 'same-origin', ...options });
        return parseResponse(response);
    }

    async function parseResponse(response) {
        const contentType = response.headers.get('content-type') || '';
        if (response.status === 204) return null;
        if (!contentType.includes('json')) {
            throw new Error(response.redirected
                ? 'Your BCU session expired. Sign in again and retry.'
                : `Unexpected server response (HTTP ${response.status}).`);
        }
        const result = await response.json();
        if (!response.ok) throw new Error(result.message || `Request failed (HTTP ${response.status}).`);
        return result;
    }

    function showMessage(element, text, type) {
        element.textContent = text;
        element.className = `alert alert-${type}`;
    }

    function hideMessage(element) {
        element.textContent = '';
        element.className = 'alert d-none';
    }
});
