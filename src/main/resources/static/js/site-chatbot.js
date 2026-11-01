(() => {
    function initializeChatbot() {
        if (document.getElementById('smartbus-chatbot')) return;

        const chatbot = document.createElement('section');
        chatbot.id = 'smartbus-chatbot';
        chatbot.className = 'smartbus-chatbot';
        chatbot.innerHTML = `
            <section id="smartbus-chat-panel" class="smartbus-chat-panel" aria-label="Neuronex AI assistant" hidden>
                <header class="smartbus-chat-header">
                    <img class="smartbus-chat-avatar" src="/images/neuronex-chat-avatar.png?v=2" alt="Neuronex logo">
                    <div class="smartbus-chat-heading">
                        <strong>Neuronex</strong>
                        <small>SmartBus AI · Local language model</small>
                    </div>
                    <button id="smartbus-chat-close" class="smartbus-chat-close" type="button" aria-label="Close chat"><i class="bi bi-x-lg"></i></button>
                </header>
                <p class="smartbus-chat-privacy">Current-information searches send your question to Ollama’s web-search service. Don’t include personal or sensitive information.</p>
                <div id="smartbus-chat-feed" class="smartbus-chat-feed" role="log" aria-live="polite">
                    <div class="smartbus-chat-bubble bot">Hi, I’m Neuronex, SmartBus’s AI assistant. Ask a general question or find a nearby bus.</div>
                    <div class="smartbus-chat-prompts">
                        <button type="button">Fastest bus to Ukkadam</button>
                        <button type="button">How do I book a ticket?</button>
                    </div>
                </div>
                <div id="smartbus-chat-error" class="smartbus-chat-error" role="status" hidden></div>
                <form id="smartbus-chat-form" class="smartbus-chat-form">
                    <label class="visually-hidden" for="smartbus-chat-input">Message Neuronex</label>
                    <input id="smartbus-chat-input" maxlength="300" autocomplete="off" placeholder="Ask Neuronex anything…" required>
                    <button id="smartbus-chat-send" type="submit" aria-label="Send message"><i class="bi bi-send-fill"></i></button>
                </form>
                <div class="smartbus-chat-footer"><a href="/user/nearby"><i class="bi bi-geo-alt me-1"></i>Open nearby map</a><button id="smartbus-chat-location" type="button"><i class="bi bi-crosshair me-1"></i>Use my location</button></div>
            </section>
            <button id="smartbus-chat-toggle" class="smartbus-chat-toggle" type="button" aria-label="Open Neuronex AI assistant" aria-expanded="false">
                <img src="/images/neuronex-chat-avatar.png?v=2" alt="" aria-hidden="true"><span>Ask Neuronex</span>
            </button>`;
        document.body.appendChild(chatbot);

        const panel = document.getElementById('smartbus-chat-panel');
        const toggle = document.getElementById('smartbus-chat-toggle');
        const close = document.getElementById('smartbus-chat-close');
        const feed = document.getElementById('smartbus-chat-feed');
        const form = document.getElementById('smartbus-chat-form');
        const input = document.getElementById('smartbus-chat-input');
        const send = document.getElementById('smartbus-chat-send');
        const errorMessage = document.getElementById('smartbus-chat-error');
        const welcome = feed.querySelector('.smartbus-chat-bubble.bot');
        const prompts = feed.querySelector('.smartbus-chat-prompts');
        let selectedLocation = window.nearbyCurrentLocation || null;

        loadRoleGuidance();

        async function loadRoleGuidance() {
            try {
                const response = await fetch('/api/auth/current', { cache: 'no-store' });
                if (!response.ok) return;
                const current = await response.json();
                const role = current.authenticated ? current.role : 'GUEST';
                const guidance = roleGuidance(role);
                welcome.textContent = guidance.welcome;
                prompts.replaceChildren();
                guidance.questions.forEach(question => {
                    const button = document.createElement('button');
                    button.type = 'button';
                    button.textContent = question;
                    button.addEventListener('click', () => {
                        input.value = question;
                        form.requestSubmit();
                    });
                    prompts.appendChild(button);
                });
            } catch {
                welcome.textContent = 'Hi, I’m Neuronex, SmartBus’s AI assistant. Ask about nearby bus routes, booking, tracking, or support.';
            }
        }

        function roleGuidance(role) {
            switch (role) {
                case 'ROLE_ADMIN':
                    return {
                        welcome: 'Hello, Admin! I’m Neuronex. Ask for guidance about fleet, routes, bookings, reports, and account access.',
                        questions: ['How do I manage the fleet?', 'Where are revenue reports?']
                    };
                case 'ROLE_BCU':
                    return {
                        welcome: 'Hello, BCU controller! I’m Neuronex. Ask about fleet monitoring, GPS, incidents, route changes, or passenger verification.',
                        questions: ['How do I handle GPS signal loss?', 'How do I manage a route diversion?']
                    };
                case 'ROLE_DRIVER':
                    return {
                        welcome: 'Hello, Driver! I’m Neuronex. Ask about sharing your assigned bus location, ticket checks, or reporting incidents.',
                        questions: ['How do I share my location?', 'How do I verify a ticket?']
                    };
                case 'ROLE_USER':
                    return {
                        welcome: 'Hello! I’m Neuronex. Ask about nearby buses, routes, ticket booking, tracking, fares, or passenger support.',
                        questions: ['Fastest bus to Ukkadam', 'How do I book a ticket?']
                    };
                default:
                    return {
                        welcome: 'Hi, I’m Neuronex, SmartBus’s AI assistant. Ask about nearby bus routes, booking, tracking, or support.',
                        questions: ['Fastest bus to Ukkadam', 'How do I book a ticket?']
                    };
            }
        }

        function setOpen(open) {
            panel.hidden = !open;
            toggle.setAttribute('aria-expanded', String(open));
            toggle.setAttribute('aria-label', open ? 'Close Neuronex AI assistant' : 'Open Neuronex AI assistant');
            toggle.querySelector('span').textContent = open ? 'Close chat' : 'Ask Neuronex';
            if (open) input.focus();
        }

        toggle.addEventListener('click', () => setOpen(panel.hidden));
        close.addEventListener('click', () => setOpen(false));

        document.querySelectorAll('.smartbus-chat-prompts button').forEach(button => {
            button.addEventListener('click', () => {
                input.value = button.textContent.trim();
                form.requestSubmit();
            });
        });

        document.getElementById('smartbus-chat-location').addEventListener('click', () => {
            if (!navigator.geolocation) {
                showError('Location is not supported by this browser. You can still ask using the default nearby area.');
                return;
            }
            navigator.geolocation.getCurrentPosition(
                position => {
                    selectedLocation = { lat: position.coords.latitude, lon: position.coords.longitude };
                    window.nearbyCurrentLocation = selectedLocation;
                    errorMessage.hidden = true;
                    appendMessage(feed, 'Using your current location for nearby trip suggestions.', 'bot');
                },
                () => showError('Location permission was not available. I’ll use the nearby map location or the default area instead.'),
                { enableHighAccuracy: true, timeout: 10000, maximumAge: 30000 }
            );
        });

        form.addEventListener('submit', async event => {
            event.preventDefault();
            const message = input.value.trim();
            if (!message) return;

            appendMessage(feed, message, 'user');
            input.value = '';
            selectedLocation = window.nearbyCurrentLocation || selectedLocation || { lat: 11.0168, lon: 76.9558 };
            input.disabled = true;
            send.disabled = true;
            errorMessage.hidden = true;
            const pending = appendMessage(feed, 'Neuronex is thinking…', 'bot');

            try {
                const response = await fetch('/assistant/nearby/reply', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        message,
                        latitude: selectedLocation.lat,
                        longitude: selectedLocation.lon
                    })
                });
                if (response.redirected && new URL(response.url).pathname.startsWith('/login')) {
                    throw new Error('Neuronex needs the latest SmartBus server. Please refresh the website and try again.');
                }
                if (!response.ok) {
                    let detail = '';
                    if ((response.headers.get('content-type') || '').includes('json')) {
                        const errorBody = await response.json();
                        detail = errorBody.detail || errorBody.message || '';
                    }
                    const fallback = response.status === 503
                        ? 'The local AI model is unavailable. Check that Ollama is running and the configured model is installed.'
                        : `Assistant is unavailable right now (HTTP ${response.status}). Please try again shortly.`;
                    throw new Error(detail || fallback);
                }
                if (!response.headers.get('content-type')?.includes('application/json')) {
                    throw new Error('Neuronex received an unexpected response. Refresh the website and try again.');
                }
                const result = await response.json();
                pending.remove();
                const answer = appendMessage(feed, result.reply, 'bot');
                renderSources(answer, result.sources);
                (result.suggestions || []).forEach(renderSuggestion);
            } catch (error) {
                pending.remove();
                showError(`Neuronex couldn’t answer right now: ${error.message}`);
            } finally {
                input.disabled = false;
                send.disabled = false;
                input.focus();
            }
        });

        function appendMessage(target, text, sender) {
            const bubble = document.createElement('div');
            bubble.className = `smartbus-chat-bubble ${sender}`;
            bubble.textContent = text;
            target.appendChild(bubble);
            target.scrollTop = target.scrollHeight;
            return bubble;
        }

        function renderSources(target, sources) {
            if (!Array.isArray(sources) || sources.length === 0) return;

            const list = document.createElement('div');
            list.className = 'smartbus-chat-sources';
            const heading = document.createElement('strong');
            heading.textContent = 'Sources';
            list.appendChild(heading);

            sources.slice(0, 5).forEach(source => {
                try {
                    const url = new URL(source.url);
                    if (url.protocol !== 'https:') return;
                    const link = document.createElement('a');
                    link.href = url.href;
                    link.target = '_blank';
                    link.rel = 'noopener noreferrer';
                    link.textContent = source.title || url.hostname;
                    list.appendChild(link);
                } catch {
                    return;
                }
            });

            if (list.querySelector('a')) target.appendChild(list);
        }

        function renderSuggestion(trip) {
            const card = document.createElement('article');
            card.className = 'smartbus-chat-trip';

            const name = document.createElement('strong');
            name.textContent = `${trip.destination || 'Bus service'} · ${trip.busNumber || ''}`;
            card.appendChild(name);

            const detail = document.createElement('p');
            detail.textContent = `${trip.routeName || 'Route'} · ${trip.busType || 'Bus'} · board at ${trip.stopName}, ${trip.distanceDisplay} away (about ${trip.walkingMinutes} min walk) · departure ${trip.departureTime} · about ${trip.totalEtaMinutes} min total`;
            card.appendChild(detail);

            const service = document.createElement('small');
            service.className = trip.issue ? 'text-warning-emphasis' : 'text-success';
            service.textContent = trip.issue ? `Service notice: ${trip.issue}` : `Service status: ${(trip.status || 'unknown').replaceAll('_', ' ')}`;
            card.appendChild(service);

            const actions = document.createElement('div');
            actions.className = 'smartbus-chat-actions';
            addAction(actions, `/user/tracking/${encodeURIComponent(trip.busId)}`, 'Track bus', 'outline');
            if (trip.bookable) addAction(actions, `/user/booking/${encodeURIComponent(trip.busId)}`, 'Book ticket', 'primary');
            card.appendChild(actions);
            feed.appendChild(card);
            feed.scrollTop = feed.scrollHeight;
        }

        function addAction(container, href, label, style) {
            const link = document.createElement('a');
            link.href = href;
            link.className = `smartbus-chat-action ${style}`;
            link.textContent = label;
            container.appendChild(link);
        }

        function showError(text) {
            errorMessage.textContent = text;
            errorMessage.hidden = false;
        }

    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initializeChatbot, { once: true });
    } else {
        initializeChatbot();
    }
})();
