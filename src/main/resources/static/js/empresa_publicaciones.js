document.addEventListener('DOMContentLoaded', () => {
    const scripts = new Map();
    function loadScript(url) {
        if (!scripts.has(url)) scripts.set(url, new Promise((resolve, reject) => {
            const script = document.createElement('script'); script.src = url; script.async = true;
            script.onload = resolve; script.onerror = reject; document.head.append(script);
        }));
        return scripts.get(url);
    }
    async function render(post) {
        const container = post.querySelector('[data-social-embed]');
        const url = post.dataset.socialUrl, network = post.dataset.socialNetwork;
        if (post.dataset.embedLoaded) return;
        post.dataset.embedLoaded = 'true';
        const link = document.createElement('a'); link.href = url; link.textContent = 'Ver publicación en ' + network;
        link.target = '_blank'; link.rel = 'noopener noreferrer';
        try {
            if (network === 'Facebook') {
                const iframe = document.createElement('iframe');
                iframe.src = 'https://www.facebook.com/plugins/post.php?href=' + encodeURIComponent(url) + '&show_text=true&width=500';
                iframe.title = 'Publicación de Facebook'; iframe.loading = 'lazy'; iframe.allowFullscreen = true;
                iframe.height = '520'; iframe.width = '500'; iframe.referrerPolicy = 'strict-origin-when-cross-origin';
                container.append(iframe); return;
            }
            const quote = document.createElement('blockquote'); quote.append(link); container.append(quote);
            if (network === 'Instagram') {
                quote.className = 'instagram-media'; quote.dataset.instgrmPermalink = url; quote.dataset.instgrmVersion = '14';
                await loadScript('https://www.instagram.com/embed.js'); window.instgrm?.Embeds?.process();
            } else if (network === 'Threads') {
                quote.className = 'text-post-media'; quote.dataset.textPostPermalink = url; quote.dataset.textPostVersion = '0';
                await loadScript('https://www.threads.com/embed.js');
            } else if (network === 'X') {
                quote.className = 'twitter-tweet'; quote.dataset.dnt = 'true';
                await loadScript('https://platform.twitter.com/widgets.js'); window.twttr?.widgets?.load(container);
            }
        } catch (error) {
            container.replaceChildren();
            const text = document.createElement('p'); text.textContent = 'La vista previa no está disponible. Abre el enlace para ver la publicación.';
            container.append(text);
        }
    }
    const posts = Array.from(document.querySelectorAll('[data-social-network]'));
    if (!posts.length) return;
    let visible = 5;
    const more = document.querySelector('[data-social-more]');
    function paginate() {
        posts.forEach((post, index) => { post.hidden = index >= visible; });
        if (more) more.hidden = visible >= posts.length;
    }
    paginate();
    if (more) more.addEventListener('click', () => {
        const firstNew = visible; visible += 5; paginate();
        posts[firstNew]?.querySelector('a')?.focus({ preventScroll: true });
    });
    // Prepara todos los embeds juntos: Threads busca sus bloques cuando carga el script.
    if ('IntersectionObserver' in window) {
        const observer = new IntersectionObserver(entries => {
            if (!entries.some(entry => entry.isIntersecting)) return;
            posts.forEach(render); observer.disconnect();
        }, { rootMargin: '160px' });
        posts.forEach(post => observer.observe(post));
    } else posts.forEach(render);
});
