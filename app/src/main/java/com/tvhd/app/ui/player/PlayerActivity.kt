package com.tvhd.app.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import com.tvhd.app.databinding.ActivityPlayerBinding

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_URL   = "url"

        fun start(context: Context, title: String, url: String) {
            context.startActivity(Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_URL, url)
            })
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
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

        binding.textTitle.text = title
        binding.btnClose.setOnClickListener  { finish() }
        binding.btnReload.setOnClickListener { binding.webView.reload() }

        setupWebView(url)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(url: String) {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled                = true
                domStorageEnabled                = true
                allowFileAccess                  = true
                mediaPlaybackRequiresUserGesture = false   // permite autoplay
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
                    val reqUrl = request?.url?.toString() ?: return false
                    if (AD_HOSTS.any { reqUrl.contains(it) }) return true
                    return false
                }

                override fun shouldInterceptRequest(
                    view: WebView?, request: WebResourceRequest?
                ): WebResourceResponse? {
                    val reqUrl = request?.url?.toString() ?: return null
                    if (AD_HOSTS.any { reqUrl.contains(it) }) {
                        return WebResourceResponse("text/plain", "utf-8", null)
                    }
                    return null
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressBar.visibility = View.GONE
                    injectStyles(view)
                    triggerAutoplay(view)
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
     * Inyecta CSS para ocultar anuncios y elementos de UI no deseados.
     */
    private fun injectStyles(view: WebView?) {
        val css = """
            .ad,.ads,.advertisement,.banner,.popup,.overlay,
            [class*='ad-'],[class*='ads-'],[id*='ad-'],[id*='ads-'],
            .notification-bar,.cookie-notice,.gdpr-banner,
            [class*='popup'],[class*='modal']:not(.player-modal){
                display:none!important;visibility:hidden!important;
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
     * Autoplay: hace click en el primer botón play que encuentre en el DOM.
     * Cubre JWPlayer, VideoJS, HTML5 <video>, y botones genéricos.
     */
    private fun triggerAutoplay(view: WebView?) {
        val js = """
            (function(){
                // 1. HTML5 video directo
                var videos = document.querySelectorAll('video');
                for(var i=0;i<videos.length;i++){
                    try{ videos[i].play(); }catch(e){}
                }

                // 2. JWPlayer
                if(typeof jwplayer === 'function'){
                    try{ jwplayer().play(); }catch(e){}
                }

                // 3. VideoJS
                if(typeof videojs !== 'undefined'){
                    try{
                        var players = videojs.getAllPlayers();
                        for(var j=0;j<players.length;j++) players[j].play();
                    }catch(e){}
                }

                // 4. Click en botón play visible (fallback genérico)
                var selectors = [
                    '.jw-icon-playback',
                    '.vjs-play-button',
                    '.play-button',
                    '[class*="play"]',
                    'button[title*="play" i]',
                    'button[aria-label*="play" i]'
                ];
                for(var s=0;s<selectors.length;s++){
                    var el = document.querySelector(selectors[s]);
                    if(el && el.offsetParent !== null){
                        try{ el.click(); break; }catch(e){}
                    }
                }
            })();
        """.trimIndent()

        // Disparar inmediatamente y también con 2s de delay (por si el player tarda en init)
        view?.evaluateJavascript(js, null)
        view?.postDelayed({ view.evaluateJavascript(js, null) }, 2000)
        view?.postDelayed({ view.evaluateJavascript(js, null) }, 4000)
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
        binding.webView.destroy()
        super.onDestroy()
    }
}
