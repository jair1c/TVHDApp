package com.tvhd.app.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import com.tvhd.app.databinding.ActivityPlayerBinding

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private val handler = Handler(Looper.getMainLooper())

    // Oculta la cabecera después de 3 segundos de inactividad
    private val hideHeaderRunnable = Runnable { hideHeader() }

    companion object {
        private const val EXTRA_TITLE  = "title"
        private const val EXTRA_URL    = "url"
        private const val HEADER_TIMEOUT = 3000L   // ms

        fun start(context: Context, title: String, url: String) {
            context.startActivity(Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_URL, url)
            })
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val url   = intent.getStringExtra(EXTRA_URL)   ?: ""

        binding.textTitle.text  = title
        binding.btnClose.setOnClickListener  { finish() }
        binding.btnReload.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            binding.webView.reload()
            scheduleHideHeader()
        }

        // Toque en el WebView → mostrar/ocultar cabecera
        binding.webView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                toggleHeader()
            }
            false   // no consumir el evento: el WebView sigue recibiendo clicks
        }

        setupWebView(url)

        // Ocultar cabecera tras 3s al arrancar
        scheduleHideHeader()
    }

    // ── Cabecera auto-ocultar ─────────────────────────────────────────────────

    private fun showHeader() {
        binding.headerControls.apply {
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(200).start()
        }
        scheduleHideHeader()
    }

    private fun hideHeader() {
        binding.headerControls.animate()
            .alpha(0f)
            .setDuration(400)
            .withEndAction { binding.headerControls.visibility = View.GONE }
            .start()
    }

    private fun toggleHeader() {
        if (binding.headerControls.visibility == View.VISIBLE) {
            handler.removeCallbacks(hideHeaderRunnable)
            hideHeader()
        } else {
            showHeader()
        }
    }

    private fun scheduleHideHeader() {
        handler.removeCallbacks(hideHeaderRunnable)
        handler.postDelayed(hideHeaderRunnable, HEADER_TIMEOUT)
    }

    // ── WebView ───────────────────────────────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(url: String) {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled                = true
                domStorageEnabled                = true
                allowFileAccess                  = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode                 = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                useWideViewPort                  = true
                loadWithOverviewMode             = true
                cacheMode                        = WebSettings.LOAD_DEFAULT
                userAgentString = (
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/124.0.0.0 Safari/537.36"
                )
            }

            webViewClient = object : WebViewClient() {
                private val AD_HOSTS = listOf(
                    "googlesyndication", "doubleclick", "googleadservices",
                    "amazon-adsystem", "adnxs", "taboola", "outbrain",
                    "criteo", "rubiconproject", "pubmatic", "openx",
                    "yieldmo", "smartadserver", "advertising",
                )

                override fun shouldOverrideUrlLoading(
                    view: WebView?, request: WebResourceRequest?
                ): Boolean {
                    val u = request?.url?.toString() ?: return false
                    return AD_HOSTS.any { u.contains(it) }
                }

                override fun shouldInterceptRequest(
                    view: WebView?, request: WebResourceRequest?
                ): WebResourceResponse? {
                    val u = request?.url?.toString() ?: return null
                    if (AD_HOSTS.any { u.contains(it) })
                        return WebResourceResponse("text/plain", "utf-8", null)
                    return null
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressBar.visibility = View.GONE
                    injectStyles(view)
                    // Autoplay: intento inmediato + reintentos diferidos
                    triggerAutoplay(view, delayMs = 0)
                    triggerAutoplay(view, delayMs = 1500)
                    triggerAutoplay(view, delayMs = 3500)
                    triggerAutoplay(view, delayMs = 6000)
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    binding.fullscreenContainer.addView(view)
                    binding.fullscreenContainer.visibility = View.VISIBLE
                    binding.webView.visibility = View.GONE
                }
                override fun onHideCustomView() {
                    binding.fullscreenContainer.visibility = View.GONE
                    binding.webView.visibility = View.VISIBLE
                    binding.fullscreenContainer.removeAllViews()
                }
            }

            loadUrl(url)
        }
    }

    /**
     * Inyecta CSS para ocultar anuncios/overlays del sitio.
     * También oculta el elemento "thumbnail/poster" del player para que no
     * tape el video una vez que arranca — eso era lo que causaba el efecto
     * de "vuelve a aparecer el botón play" al cargar la imagen del canal.
     */
    private fun injectStyles(view: WebView?) {
        val css = """
            .ad,.ads,.advertisement,.banner,
            [class*='ad-'],[class*='ads-'],[id*='ad-'],[id*='ads-'],
            .notification-bar,.cookie-notice,.gdpr-banner,
            [class*='popup'],[class*='modal']:not(.player-modal){
                display:none!important;visibility:hidden!important;
            }
            .jw-preview,.jw-poster,
            .vjs-poster,.vjs-thumbnail,
            video::poster,
            [class*='poster'],[class*='thumbnail'][class*='player']{
                display:none!important;
            }
        """.trimIndent().replace("\n", "")

        view?.evaluateJavascript("""
            (function(){
                var s=document.createElement('style');
                s.innerHTML='$css';
                document.head.appendChild(s);
            })();
        """.trimIndent(), null)
    }

    /**
     * Autoplay: dispara play() en todos los players conocidos.
     * delayMs permite reintentar cuando el player JS tarda en inicializarse.
     */
    private fun triggerAutoplay(view: WebView?, delayMs: Long) {
        val js = """
            (function(){
                // 1. HTML5 <video>
                document.querySelectorAll('video').forEach(function(v){
                    v.muted=false;
                    try{ v.play(); }catch(e){}
                });

                // 2. JWPlayer
                try{ if(typeof jwplayer==='function') jwplayer().play(true); }catch(e){}

                // 3. VideoJS
                try{
                    if(typeof videojs!=='undefined'){
                        videojs.getAllPlayers().forEach(function(p){ p.play(); });
                    }
                }catch(e){}

                // 4. Flowplayer
                try{ if(typeof flowplayer!=='function') flowplayer().play(); }catch(e){}

                // 5. Click en botón play visible (fallback)
                var sel=[
                    '.jw-icon-playback[aria-label="Play"]',
                    '.vjs-play-button',
                    '.play-button',
                    'button[title*="play" i]',
                    'button[aria-label*="play" i]',
                    '[class*="play-btn"]',
                    '[id*="play-btn"]'
                ];
                for(var i=0;i<sel.length;i++){
                    var el=document.querySelector(sel[i]);
                    if(el&&el.offsetParent!==null){
                        try{ el.click(); }catch(e){}
                        break;
                    }
                }
            })();
        """.trimIndent()

        if (delayMs == 0L) {
            view?.evaluateJavascript(js, null)
        } else {
            handler.postDelayed({ view?.evaluateJavascript(js, null) }, delayMs)
        }
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        binding.webView.destroy()
        super.onDestroy()
    }
}
