// Interactive Visual Seat Selection
let selectedSeats = new Set();
let farePerSeat = 25.0;

function initSeatSelection(busId, totalCapacity) {
    const seatGrid = document.getElementById('seat-grid');
    if (!seatGrid) return;

    seatGrid.innerHTML = '<div class="text-center py-3 text-muted"><div class="spinner-border spinner-border-sm me-2"></div>Loading seat layout...</div>';

    fetch('/api/seats?busId=' + busId)
        .then(r => r.json())
        .then(seats => {
            const bookedSet = new Set(seats.filter(s => s.status === 'BOOKED').map(s => s.seatNumber));
            renderSeatLayout(seatGrid, totalCapacity || 40, bookedSet);
        })
        .catch(() => {
            renderSeatLayout(seatGrid, totalCapacity || 40, new Set());
        });
}

function renderSeatLayout(container, capacity, bookedSet) {
    container.innerHTML = '';
    const rows = Math.ceil(capacity / 4);

    for (let r = 1; r <= rows; r++) {
        const rowDiv = document.createElement('div');
        rowDiv.className = 'seat-row';

        // Left Pair: A, B
        const leftPair = document.createElement('div');
        leftPair.className = 'seat-pair';
        const s1 = `A${r}`;
        const s2 = `B${r}`;
        leftPair.appendChild(createSeatElement(s1, bookedSet.has(s1)));
        leftPair.appendChild(createSeatElement(s2, bookedSet.has(s2)));

        // Aisle
        const aisle = document.createElement('div');
        aisle.className = 'seat-aisle';
        aisle.innerText = r;

        // Right Pair: C, D
        const rightPair = document.createElement('div');
        rightPair.className = 'seat-pair';
        const s3 = `C${r}`;
        const s4 = `D${r}`;
        rightPair.appendChild(createSeatElement(s3, bookedSet.has(s3)));
        rightPair.appendChild(createSeatElement(s4, bookedSet.has(s4)));

        rowDiv.appendChild(leftPair);
        rowDiv.appendChild(aisle);
        rowDiv.appendChild(rightPair);

        container.appendChild(rowDiv);
    }
}

function createSeatElement(seatNumber, isBooked) {
    const div = document.createElement('div');
    div.className = 'seat ' + (isBooked ? 'booked' : 'available');
    div.innerHTML = `<i class="bi bi-person"></i><span>${seatNumber}</span>`;
    div.dataset.seat = seatNumber;

    if (!isBooked) {
        div.onclick = () => toggleSeat(div, seatNumber);
    } else {
        div.title = 'Seat already booked';
    }
    return div;
}

function toggleSeat(el, seatNumber) {
    if (selectedSeats.has(seatNumber)) {
        selectedSeats.delete(seatNumber);
        el.classList.remove('selected');
        el.classList.add('available');
    } else {
        selectedSeats.add(seatNumber);
        el.classList.remove('available');
        el.classList.add('selected');
    }
    updateBookingSummary();
}

function updateBookingSummary() {
    const seatListEl = document.getElementById('selected-seats-list');
    const countEl = document.getElementById('selected-count');
    const fareEl = document.getElementById('total-fare');
    const btn = document.getElementById('btn-confirm-booking');

    const list = Array.from(selectedSeats);
    if (seatListEl) seatListEl.innerText = list.length > 0 ? list.join(', ') : 'None selected';
    if (countEl) countEl.innerText = list.length;
    if (fareEl) fareEl.innerText = 'Rs. ' + (list.length * farePerSeat).toFixed(2);

    if (btn) {
        btn.disabled = list.length === 0;
    }
}