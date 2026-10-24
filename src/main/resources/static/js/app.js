(function () {
            var root = document.documentElement;
            var toggle = document.getElementById('theme-toggle');
            if (!toggle) return;
            toggle.addEventListener('click', function () {
                var next = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
                root.setAttribute('data-theme', next);
                try { localStorage.setItem('hc-theme', next); } catch (e) { }
            });
        })();