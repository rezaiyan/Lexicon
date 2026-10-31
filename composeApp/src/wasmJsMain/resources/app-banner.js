/*
 * Lexicon "get the app" banner for the mobile web.
 *
 * - iOS Safari: the native Smart App Banner (apple-itunes-app meta tag in index.html)
 *   handles it. Its button opens the App Store install sheet, so no custom banner is shown.
 * - Android Chrome: the web manifest lists the Play app in related_applications with
 *   prefer_related_applications=true, so Chrome fires `beforeinstallprompt`. Calling
 *   prompt() on it opens the Google Play install sheet over the page, without leaving the site.
 *   Browsers without that event fall back to the Play Store listing.
 * - Other iOS browsers (Chrome, Firefox, in-app browsers): the button opens the App Store listing.
 * - Desktop, installed PWA, app already installed, or dismissed recently: no banner.
 */
(function () {
    'use strict';

    var CONFIG = {
        appName: 'Lexicon',
        iosAppId: '6753774009',
        androidPackage: 'com.alirezaiyan.vokab',
        dismissDays: 14,
        storageKey: 'lexicon.appBanner.dismissedAt'
    };

    var STORE = {
        ios: 'https://apps.apple.com/app/id' + CONFIG.iosAppId,
        android: 'https://play.google.com/store/apps/details?id=' + CONFIG.androidPackage +
            '&referrer=' + encodeURIComponent('utm_source=lexicon_web&utm_medium=app_banner')
    };

    var ua = navigator.userAgent || '';
    var isIOS = /iPhone|iPad|iPod/.test(ua) ||
        (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
    var isAndroid = /Android/i.test(ua);
    var isIOSSafari = isIOS && /Safari/.test(ua) &&
        !/CriOS|FxiOS|EdgiOS|OPiOS|GSA\/|FBAN|FBAV|Instagram|Line\//.test(ua);
    var isStandalone = (window.matchMedia && window.matchMedia('(display-mode: standalone)').matches) ||
        navigator.standalone === true;

    var deferredPrompt = null;
    var bannerEl = null;

    function readDismissedAt() {
        try {
            return parseInt(window.localStorage.getItem(CONFIG.storageKey) || '0', 10) || 0;
        } catch (e) {
            return 0;
        }
    }

    function rememberDismissal() {
        try {
            window.localStorage.setItem(CONFIG.storageKey, String(Date.now()));
        } catch (e) { /* storage unavailable: banner just comes back next visit */ }
    }

    function recentlyDismissed() {
        var at = readDismissedAt();
        return at > 0 && Date.now() - at < CONFIG.dismissDays * 24 * 60 * 60 * 1000;
    }

    function shouldOffer() {
        if (isStandalone || recentlyDismissed()) return false;
        if (isIOSSafari) return false; // native Smart App Banner takes over
        return isIOS || isAndroid;
    }

    // Must be registered early: Chrome fires it once, shortly after the manifest loads.
    window.addEventListener('beforeinstallprompt', function (event) {
        event.preventDefault();
        deferredPrompt = event;
    });

    window.addEventListener('appinstalled', hide);

    function hide() {
        if (!bannerEl) return;
        bannerEl.classList.remove('app-banner--visible');
        var el = bannerEl;
        bannerEl = null;
        setTimeout(function () { if (el.parentNode) el.parentNode.removeChild(el); }, 250);
    }

    function openStore() {
        window.location.href = isIOS ? STORE.ios : STORE.android;
    }

    function install() {
        if (deferredPrompt) {
            var promptEvent = deferredPrompt;
            deferredPrompt = null; // prompt() can only be called once per event
            promptEvent.prompt();
            if (promptEvent.userChoice) {
                promptEvent.userChoice.then(function (choice) {
                    if (choice && choice.outcome === 'accepted') hide();
                }, function () { /* ignore */ });
            }
            return;
        }
        openStore();
    }

    function el(tag, className, text) {
        var node = document.createElement(tag);
        if (className) node.className = className;
        if (text) node.textContent = text;
        return node;
    }

    function render() {
        var banner = el('aside', 'app-banner');
        banner.setAttribute('aria-label', 'Get the ' + CONFIG.appName + ' app');

        var close = el('button', 'app-banner__close');
        close.type = 'button';
        close.setAttribute('aria-label', 'Dismiss');
        close.textContent = '×';
        close.addEventListener('click', function () {
            rememberDismissal();
            hide();
        });

        var icon = el('img', 'app-banner__icon');
        icon.src = 'icon-192.png';
        icon.alt = '';
        icon.width = 44;
        icon.height = 44;

        var text = el('div', 'app-banner__text');
        text.appendChild(el('strong', 'app-banner__title', CONFIG.appName + ' — Learn Vocabulary'));
        text.appendChild(el('span', 'app-banner__subtitle',
            isIOS ? 'Free on the App Store' : 'Free on Google Play'));

        var action = el('button', 'app-banner__action', isIOS ? 'Get' : 'Install');
        action.type = 'button';
        action.addEventListener('click', install);

        banner.appendChild(close);
        banner.appendChild(icon);
        banner.appendChild(text);
        banner.appendChild(action);

        document.body.insertBefore(banner, document.body.firstChild);
        bannerEl = banner;
        requestAnimationFrame(function () { banner.classList.add('app-banner--visible'); });
    }

    function hasInstalledApp() {
        // Android Chrome reports the Play app only when it is installed and declares this site
        // in its asset statements; anywhere else this resolves to false.
        if (!navigator.getInstalledRelatedApps) return Promise.resolve(false);
        return navigator.getInstalledRelatedApps()
            .then(function (apps) { return apps && apps.length > 0; }, function () { return false; });
    }

    function start() {
        if (!shouldOffer()) return;
        hasInstalledApp().then(function (installed) {
            if (!installed) render();
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start);
    } else {
        start();
    }
})();
