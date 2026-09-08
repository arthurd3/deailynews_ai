/* Progressive enhancement only. Every route renders and works server-side without this
   file; it adds the copy-to-clipboard convenience on top. */
(function () {
    'use strict';

    function toast(message) {
        var el = document.getElementById('toast');
        if (!el) return;
        el.textContent = message;
        el.hidden = false;
        clearTimeout(el._timer);
        el._timer = setTimeout(function () { el.hidden = true; }, 2600);
    }

    /* The markdown is fetched from the API rather than rebuilt here, so there is exactly
       one definition of what a brief looks like as prose. */
    async function copyAsMarkdown(button) {
        var url = button.getAttribute('data-markdown-url');
        if (!url) return;

        button.disabled = true;
        try {
            var response = await fetch(url, { headers: { Accept: 'text/markdown' } });
            if (!response.ok) throw new Error('HTTP ' + response.status);

            var text = await response.text();
            await navigator.clipboard.writeText(text);
            toast('Brief copied as markdown');
        } catch (error) {
            /* Clipboard access needs a secure context, and the fetch can fail like any
               other. Say so rather than leaving the button looking inert. */
            toast('Could not copy — ' + error.message);
        } finally {
            button.disabled = false;
        }
    }

    /* Explicit choice wins over the system preference, in both directions. */
    function toggleTheme() {
        var root = document.documentElement;
        var current = root.dataset.theme;

        if (!current) {
            var systemIsDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
            current = systemIsDark ? 'dark' : 'light';
        }

        var next = current === 'dark' ? 'light' : 'dark';
        root.dataset.theme = next;
        try { localStorage.setItem('newsbrief-theme', next); } catch (e) { /* ignore */ }
    }

    document.addEventListener('click', function (event) {
        var copyButton = event.target.closest('[data-markdown-url]');
        if (copyButton) {
            event.preventDefault();
            copyAsMarkdown(copyButton);
            return;
        }

        if (event.target.closest('[data-theme-toggle]')) {
            event.preventDefault();
            toggleTheme();
        }
    });
})();
