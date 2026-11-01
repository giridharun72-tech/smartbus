// GPS Movement and Signal Loss Simulator for Demonstration
let activeSimulationInterval = null;

function stepSimulation(busId) {
    fetch(`/api/gps/simulate/${busId}/step`, { method: 'POST' })
        .then(r => {
            if (!r.ok) return r.json().then(e => { throw new Error(e.message || 'Simulation error'); });
            return r.json();
        })
        .then(data => {
            showToast(`Bus ${data.busNumber} moved to: ${data.nextStopName} (${data.speed} km/h)`, 'success');
            const logEl = document.getElementById('sim-log');
            if (logEl) {
                const entry = document.createElement('div');
                entry.textContent = `[${new Date().toLocaleTimeString()}] Bus ${data.busNumber} -> Lat: ${Number(data.latitude).toFixed(4)}, Lon: ${Number(data.longitude).toFixed(4)}, Speed: ${data.speed} km/h, Next: ${data.nextStopName}`;
                logEl.prepend(entry);
            }
        })
        .catch(err => {
            showToast(err.message, 'error');
        });
}

function startContinuousSimulation(busId) {
    if (activeSimulationInterval) {
        clearInterval(activeSimulationInterval);
        activeSimulationInterval = null;
        document.getElementById('btn-continuous-sim').innerText = 'Start Continuous Movement';
        document.getElementById('btn-continuous-sim').className = 'btn btn-success';
        showToast('Simulation stopped', 'info');
        return;
    }

    activeSimulationInterval = setInterval(() => stepSimulation(busId), 4000);
    document.getElementById('btn-continuous-sim').innerText = 'Stop Continuous Movement';
    document.getElementById('btn-continuous-sim').className = 'btn btn-warning';
    showToast('Continuous bus movement started (every 4 seconds)', 'info');
}

function cutGpsSignal(busId) {
    if (activeSimulationInterval) {
        clearInterval(activeSimulationInterval);
        activeSimulationInterval = null;
        const btn = document.getElementById('btn-continuous-sim');
        if (btn) {
            btn.innerText = 'Start Continuous Movement';
            btn.className = 'btn btn-success';
        }
    }

    fetch(`/api/gps/simulate/${busId}/signal-loss`, { method: 'POST' })
        .then(r => r.json())
        .then(data => {
            showToast(data.message, 'error');
            const logEl = document.getElementById('sim-log');
            if (logEl) {
                const entry = document.createElement('div');
                entry.className = 'text-danger fw-bold';
                entry.textContent = `[${new Date().toLocaleTimeString()}] ALERT: GPS Signal Loss triggered for Bus ${busId}`;
                logEl.prepend(entry);
            }
        })
        .catch(err => showToast('Failed to trigger signal loss', 'error'));
}

function resumeGpsSignal(busId) {
    fetch(`/api/gps/simulate/${busId}/resume`, { method: 'POST' })
        .then(r => r.json())
        .then(data => {
            showToast(data.message, 'success');
            const logEl = document.getElementById('sim-log');
            if (logEl) {
                const entry = document.createElement('div');
                entry.className = 'text-success fw-bold';
                entry.textContent = `[${new Date().toLocaleTimeString()}] GPS Signal resumed for Bus ${busId}`;
                logEl.prepend(entry);
            }
        })
        .catch(err => showToast('Failed to resume GPS', 'error'));
}