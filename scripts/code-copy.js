for (const code of document.querySelectorAll('.content pre > code')) {
    const pre = code.parentElement;
    const wrapper = document.createElement('figure');
    pre.before(wrapper);
    wrapper.append(pre);

    const toolbar = document.createElement('figcaption');
    wrapper.append(toolbar);
    const languageClass = [...code.classList].find(name => name.startsWith('language-'));
    if (languageClass) {
        const label = document.createElement('small');
        label.textContent = languageClass.slice('language-'.length);
        toolbar.append(label);
    }

    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'secondary outline';
    button.textContent = 'Copy';
    button.setAttribute('aria-label', 'Copy code to clipboard');
    toolbar.append(button);

    const reserveSpace = () => {
        pre.style.paddingRight = `${Math.ceil(toolbar.getBoundingClientRect().width) + 18}px`;
    };
    reserveSpace();

    let resetTimeout;
    button.addEventListener('click', async () => {
        try {
            if (navigator.clipboard?.writeText) {
                await navigator.clipboard.writeText(code.textContent);
            } else {
                const selection = window.getSelection();
                const previousRange = selection.rangeCount ? selection.getRangeAt(0) : null;
                const range = document.createRange();
                range.selectNodeContents(code);
                selection.removeAllRanges();
                selection.addRange(range);
                try {
                    if (!document.execCommand('copy')) throw new Error('Copy failed');
                } finally {
                    selection.removeAllRanges();
                    if (previousRange) selection.addRange(previousRange);
                }
            }
            button.textContent = 'Copied!';
        } catch {
            button.textContent = 'Copy failed';
        }
        reserveSpace();
        clearTimeout(resetTimeout);
        resetTimeout = setTimeout(() => {
            button.textContent = 'Copy';
            reserveSpace();
        }, 2000);
    });
}
