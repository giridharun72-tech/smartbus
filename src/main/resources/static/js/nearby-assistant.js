document.addEventListener('DOMContentLoaded', () => {
    async function responseError(response) {
        const body = await response.text();
        return body || `Request failed (${response.status})`;
    }

    const supportForm = document.getElementById('support-chat-form');
    if (supportForm) {
        const supportInput = document.getElementById('support-chat-input');
        const supportFeed = document.getElementById('support-chat-messages');
        const csrf = document.getElementById('support-chat-csrf');
        const headers = { 'Content-Type': 'application/json' };
        headers['X-CSRF-TOKEN'] = csrf.value;

        supportForm.addEventListener('submit', async event => {
            event.preventDefault();
            const content = supportInput.value.trim();
            if (!content) return;
            supportInput.disabled = true;
            try {
                const response = await fetch('/support-chat/conversation/messages', {
                    method: 'POST',
                    headers,
                    body: JSON.stringify({ content })
                });
                if (!response.ok) throw new Error(await responseError(response));
                supportInput.value = '';
                renderSupportThread(supportFeed, await response.json());
            } catch (error) {
                showChatError(supportFeed, `Message could not be sent: ${error.message}`);
            } finally {
                supportInput.disabled = false;
                supportInput.focus();
            }
        });

        const loadThread = async () => {
            try {
                const response = await fetch('/support-chat/conversation', { cache: 'no-store' });
                if (!response.ok) throw new Error(await responseError(response));
                renderSupportThread(supportFeed, await response.json());
            } catch (error) {
                showChatError(supportFeed, `Chat could not be loaded: ${error.message}`);
            }
        };
        loadThread();
        window.setInterval(loadThread, 7000);
    }

    function renderSupportThread(feed, thread) {
        feed.replaceChildren();
        if (!thread.messages || thread.messages.length === 0) {
            const empty = document.createElement('div');
            empty.className = 'assistant-empty';
            empty.textContent = 'Your conversation with BCU/Admin will appear here.';
            feed.appendChild(empty);
            return;
        }
        thread.messages.forEach(message => {
            const item = document.createElement('div');
            item.className = `support-chat-message${message.staff ? ' staff' : ''}`;
            item.textContent = `${message.staff ? 'BCU/Admin' : 'You'} · ${new Date(message.createdAt).toLocaleString()}\n${message.content}`;
            feed.appendChild(item);
        });
        feed.scrollTop = feed.scrollHeight;
    }

    function showChatError(feed, text) {
        const error = document.createElement('div');
        error.className = 'alert alert-warning py-2 mb-0';
        error.textContent = text;
        feed.appendChild(error);
    }
});
