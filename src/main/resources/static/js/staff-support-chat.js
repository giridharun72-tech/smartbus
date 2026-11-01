document.addEventListener('DOMContentLoaded', () => {
    const list = document.getElementById('staff-conversation-list');
    const feed = document.getElementById('staff-chat-messages');
    const title = document.getElementById('staff-chat-title');
    const passenger = document.getElementById('staff-chat-passenger');
    const status = document.getElementById('staff-chat-status');
    const form = document.getElementById('staff-chat-form');
    const input = document.getElementById('staff-chat-input');
    const send = document.getElementById('staff-chat-send');
    const csrf = document.getElementById('staff-chat-csrf');
    const headers = { 'Content-Type': 'application/json' };
    headers['X-CSRF-TOKEN'] = csrf.value;
    let activeConversation = null;
    let conversations = [];
    let loading = false;

    async function request(url, options = {}) {
        const response = await fetch(url, { cache: 'no-store', ...options });
        if (!response.ok) {
            const details = await response.text();
            throw new Error(details || `Request failed (${response.status})`);
        }
        return response.json();
    }

    async function loadConversations() {
        try {
            conversations = await request('/support-chat/staff/conversations');
            renderConversationList();
            if (activeConversation) {
                const selected = conversations.find(item => item.id === activeConversation);
                if (selected) await loadThread(false);
            }
            status.textContent = `Updated ${new Date().toLocaleTimeString()}`;
        } catch (error) {
            status.textContent = `Inbox error: ${error.message}`;
        }
    }

    function renderConversationList() {
        list.replaceChildren();
        if (conversations.length === 0) {
            const empty = document.createElement('div');
            empty.className = 'p-3 text-muted';
            empty.textContent = 'No passenger conversations yet.';
            list.appendChild(empty);
            return;
        }
        conversations.forEach(conversation => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = `list-group-item list-group-item-action staff-conversation${conversation.id === activeConversation ? ' active' : ''}`;
            button.addEventListener('click', () => selectConversation(conversation));
            const row = document.createElement('div');
            row.className = 'd-flex justify-content-between gap-2';
            const name = document.createElement('strong');
            name.className = 'text-truncate';
            name.textContent = conversation.passengerName || conversation.passengerUsername;
            row.appendChild(name);
            if (conversation.unreadCount > 0) {
                const badge = document.createElement('span');
                badge.className = 'badge rounded-pill bg-danger';
                badge.textContent = String(conversation.unreadCount);
                row.appendChild(badge);
            }
            const id = document.createElement('small');
            id.className = 'd-block text-muted mt-1';
            id.textContent = conversation.passengerId
                ? `${conversation.passengerUsername} · ${conversation.passengerId}`
                : conversation.passengerUsername;
            const preview = document.createElement('div');
            preview.className = 'small text-truncate mt-1';
            preview.textContent = conversation.latestMessage || '';
            button.append(row, id, preview);
            list.appendChild(button);
        });
    }

    async function selectConversation(conversation) {
        activeConversation = conversation.id;
        title.textContent = conversation.passengerName || conversation.passengerUsername;
        passenger.textContent = `Username: ${conversation.passengerUsername}${conversation.passengerId ? ` · Passenger ID: ${conversation.passengerId}` : ''}`;
        input.disabled = false;
        send.disabled = false;
        input.placeholder = 'Write a reply to this passenger…';
        renderConversationList();
        await loadThread(true);
    }

    async function loadThread(showError) {
        if (!activeConversation || loading) return;
        loading = true;
        try {
            const thread = await request(`/support-chat/staff/conversations/${encodeURIComponent(activeConversation)}/messages`);
            renderThread(thread);
            const selected = conversations.find(item => item.id === activeConversation);
            if (selected) selected.unreadCount = 0;
            renderConversationList();
        } catch (error) {
            if (showError) status.textContent = `Conversation error: ${error.message}`;
        } finally {
            loading = false;
        }
    }

    function renderThread(thread) {
        feed.replaceChildren();
        if (!thread.messages || thread.messages.length === 0) {
            const empty = document.createElement('div');
            empty.className = 'text-center text-muted py-5';
            empty.textContent = 'No messages in this conversation yet.';
            feed.appendChild(empty);
            return;
        }
        thread.messages.forEach(message => {
            const bubble = document.createElement('div');
            bubble.className = `staff-message${message.staff ? ' outgoing' : ''}`;
            bubble.textContent = `${message.staff ? 'Staff' : passengerNameForActive()} · ${new Date(message.createdAt).toLocaleString()}\n${message.content}`;
            feed.appendChild(bubble);
        });
        feed.scrollTop = feed.scrollHeight;
    }

    function passengerNameForActive() {
        const selected = conversations.find(item => item.id === activeConversation);
        return selected ? (selected.passengerName || selected.passengerUsername) : 'Passenger';
    }

    form.addEventListener('submit', async event => {
        event.preventDefault();
        const content = input.value.trim();
        if (!activeConversation || !content) return;
        input.disabled = true;
        send.disabled = true;
        try {
            const thread = await request(`/support-chat/staff/conversations/${encodeURIComponent(activeConversation)}/messages`, {
                method: 'POST',
                headers,
                body: JSON.stringify({ content })
            });
            input.value = '';
            renderThread(thread);
            await loadConversations();
        } catch (error) {
            status.textContent = `Reply could not be sent: ${error.message}`;
        } finally {
            input.disabled = false;
            send.disabled = false;
            input.focus();
        }
    });

    loadConversations();
    window.setInterval(loadConversations, 7000);
});
