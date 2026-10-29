(function () {
    'use strict';

    var section = document.querySelector('.how-it-works, .offers');
    if (!section || !('IntersectionObserver' in window) || !section.animate) return;

    var reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    if (reducedMotion.matches) return;

    var running = new Set();
    var elements = Array.from(document.querySelectorAll(
        '.offers .section-head, .offers__carousel, .offers__empty, .how-it-works .section-head, .how-it-works__step'
    ));
    var observer = new IntersectionObserver(function (entries) {
        // Stagger only elements entering together, so mobile steps never wait offscreen.
        var visible = entries.filter(function (entry) { return entry.isIntersecting; });
        visible.sort(function (a, b) { return elements.indexOf(a.target) - elements.indexOf(b.target); });
        var sectionDelays = new Map();
        visible.forEach(function (entry) {
            observer.unobserve(entry.target);
            if (reducedMotion.matches) return;

            var parentSection = entry.target.closest('section');
            var index = sectionDelays.get(parentSection) || 0;
            sectionDelays.set(parentSection, index + 1);
            var heading = entry.target.classList.contains('section-head');
            var offers = parentSection && parentSection.classList.contains('offers');
            var animation = entry.target.animate([
                { opacity: 0, transform: heading ? 'translateY(32px)' : offers ? 'translateY(40px)' : 'translateY(56px) scale(0.96)' },
                { opacity: 1, transform: 'translateY(0) scale(1)' }
            ], {
                duration: heading ? 850 : offers ? 950 : 1100,
                delay: index * 140,
                easing: 'cubic-bezier(0.16, 1, 0.3, 1)',
                fill: 'backwards'
            });
            running.add(animation);
            animation.onfinish = function () { running.delete(animation); };
            animation.oncancel = function () { running.delete(animation); };
        });
    }, { threshold: 0, rootMargin: '0px 0px -48px 0px' });

    elements.forEach(function (element) { observer.observe(element); });

    function respectMotionPreference() {
        if (!reducedMotion.matches) return;
        observer.disconnect();
        running.forEach(function (animation) { animation.cancel(); });
        running.clear();
    }
    if (reducedMotion.addEventListener) reducedMotion.addEventListener('change', respectMotionPreference);
})();
