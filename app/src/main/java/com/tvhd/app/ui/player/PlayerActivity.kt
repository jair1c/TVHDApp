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

        // Pantalla completa
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val url   = intent.getStringExtra(EXTRA_URL) ?: ""

        binding.textTitle.text = title
        binding.btnClose.setOnClickListener { finish() }
        binding.btnReload.setOnClickListener { binding.webView.reload() }

        setupWebView(url)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(url: String) {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled          = true
                domStorageEnabled          = true
                allowFileAccess            = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode           = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                useWideViewPort            = true
                loadWithOverviewMode       = true
                cacheMode                  = WebSettings.LOAD_DEFAULT
                userAgentString            = (
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/124.0.0.0 Safari/537.36"
                )
            }

            // Bloquear anuncios de terceros via WebViewClient
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
                    // Bloquear navegación a páginas de anuncios
                    if (AD_HOSTS.any { reqUrl.contains(it) }) return true
                    // Permitir navegación dentro del sitio
                    return false
                }

                override fun shouldInterceptRequest(
                    view: WebView?, request: WebResourceRequest?
                ): WebResourceResponse? {
                    val reqUrl = request?.url?.toString() ?: return null
                    // Bloquear recursos de anuncios
                    if (AD_HOSTS.any { reqUrl.contains(it) }) {
                        return WebResourceResponse("text/plain", "utf-8", null)
                    }
                    return null
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.progressBar.visibility = View.GONE
                    // Inyectar CSS para ocultar elementos de UI del sitio no deseados
                    injectAdBlockCSS(view)
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    // Fullscreen video
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

    private fun injectAdBlockCSS(view: WebView?) {
        // Ocultar popups, banners y overlays de anuncios
        val css = """
            .ad, .ads, .advertisement, .banner, .popup, .overlay,
            [class*='ad-'], [class*='ads-'], [id*='ad-'], [id*='ads-'],
            .notification-bar, .cookie-notice, .gdpr-banner,
            [class*='popup'], [class*='modal']:not(.player-modal) {
                display: none !important;
                visibility: hidden !important;
            }
        """.trimIndent().replace("\n", " ")

        view?.evaluateJavascript("""
            (function() {
                var style = document.createElement('style');
                style.innerHTML = '$css';
                document.head.appendChild(style);
            })();
        """.trimIndent(), null)
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }
}
