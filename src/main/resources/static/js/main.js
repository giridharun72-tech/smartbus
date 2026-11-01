// SmartBus common utility script

(() => {
    const originalFetch = window.fetch.bind(window);
    window.fetch = (input, init = {}) => {
        const request = input instanceof Request ? input : null;
        const method = (init.method || request?.method || 'GET').toUpperCase();
        const url = new URL(request ? request.url : input, window.location.href);
        const safeMethods = ['GET', 'HEAD', 'OPTIONS', 'TRACE'];
        if (url.origin !== window.location.origin || safeMethods.includes(method)) {
            return originalFetch(input, init);
        }

        const headers = new Headers(request?.headers);
        new Headers(init.headers).forEach((value, name) => headers.set(name, value));
        const csrfCookie = document.cookie.split('; ')
            .find(cookie => cookie.startsWith('XSRF-TOKEN='));
        if (csrfCookie && !headers.has('X-XSRF-TOKEN')) {
            headers.set('X-XSRF-TOKEN', decodeURIComponent(csrfCookie.slice('XSRF-TOKEN='.length)));
        }
        return originalFetch(input, { ...init, headers });
    };
})();

function showToast(message, type = 'info') {
    const toastContainer = document.getElementById('toast-container');
    if (!toastContainer) {
        const div = document.createElement('div');
        div.id = 'toast-container';
        div.className = 'toast-container position-fixed bottom-0 end-0 p-3';
        div.style.zIndex = '1100';
        document.body.appendChild(div);
    }

    const toastEl = document.createElement('div');
    toastEl.className = `toast align-items-center text-white bg-${type === 'error' ? 'danger' : type === 'success' ? 'success' : 'primary'} border-0`;
    toastEl.setAttribute('role', 'alert');
    toastEl.setAttribute('aria-live', 'assertive');
    toastEl.setAttribute('aria-atomic', 'true');

    const content = document.createElement('div');
    content.className = 'd-flex';
    const body = document.createElement('div');
    body.className = 'toast-body';
    const icon = document.createElement('i');
    icon.className = 'bi bi-info-circle me-2';
    body.append(icon, document.createTextNode(String(message ?? '')));
    const close = document.createElement('button');
    close.type = 'button';
    close.className = 'btn-close btn-close-white me-2 m-auto';
    close.setAttribute('data-bs-dismiss', 'toast');
    close.setAttribute('aria-label', 'Close');
    content.append(body, close);
    toastEl.appendChild(content);

    document.getElementById('toast-container').appendChild(toastEl);
    const toast = new bootstrap.Toast(toastEl, { delay: 4500 });
    toast.show();
}

// Check logged in user status and update navigation dynamically if present
document.addEventListener('DOMContentLoaded', () => {
    const chatbotScript = document.createElement('script');
    chatbotScript.src = '/js/site-chatbot.js';
    document.body.appendChild(chatbotScript);

    fetch('/api/auth/current')
        .then(r => r.json())
        .then(data => {
            if (data.authenticated) {
                const userSpan = document.getElementById('nav-user-info');
                if (userSpan) {
                    const role = document.createElement('span');
                    role.className = 'badge bg-light text-dark me-2';
                    role.textContent = String(data.role || '').replace('ROLE_', '');
                    const username = document.createElement('strong');
                    username.textContent = String(data.username || '');
                    userSpan.replaceChildren(role, document.createTextNode(' '), username,
                        document.createTextNode(` (${data.passengerId || 'Staff'})`));
                }
            }
        })
        .catch(() => {});
});
