package br.com.halleytelecom.play;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.Locale;

/**
 * Halley Play para Android TV / Google TV.
 * Abre o player web em tela cheia e permite usá-lo com o controle remoto
 * (setas movem um cursor, OK clica, segurar OK alterna para navegação por foco).
 */
public class MainActivity extends Activity {

    private static final String SITE_URL = "https://play.halleytelecom.com.br";
    private static final String OFFLINE_URL = "file:///android_asset/offline.html";
    private static final String USER_AGENT_SUFFIX = " HalleyPlayTV/1.0";
    private static final long LONG_PRESS_MS = 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private FrameLayout root;
    private WebView webView;
    private PointerView pointer;

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    private boolean pointerMode = true;
    private boolean longPressHandled = false;
    private long centerDownTime = 0L;
    private long lastBackPress = 0L;

    // Rola o elemento sob o cursor (ou a página) quando o cursor encosta na borda da tela.
    private static final String SCROLL_JS =
            "(function(x,y,dx,dy){"
                    + "var r=window.devicePixelRatio||1;x=x/r;y=y/r;dx=dx/r;dy=dy/r;"
                    + "var e=document.elementFromPoint(x,y);"
                    + "while(e&&e!==document.body&&e!==document.documentElement){"
                    + "var cs=getComputedStyle(e);"
                    + "var cy=(cs.overflowY==='auto'||cs.overflowY==='scroll')&&e.scrollHeight>e.clientHeight&&dy!==0;"
                    + "var cx=(cs.overflowX==='auto'||cs.overflowX==='scroll')&&e.scrollWidth>e.clientWidth&&dx!==0;"
                    + "if(cy||cx){if(cy)e.scrollTop+=dy;if(cx)e.scrollLeft+=dx;return;}"
                    + "e=e.parentElement;}"
                    + "window.scrollBy(dx,dy);"
                    + "})(%f,%f,%f,%f);";

    private static final String PLAY_PAUSE_JS =
            "(function(m){var v=document.querySelector('video');if(!v)return;"
                    + "if(m==='play'){v.play();}else if(m==='pause'){v.pause();}"
                    + "else{if(v.paused){v.play();}else{v.pause();}}})('%s');";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        pointer = new PointerView(this);
        root.addView(pointer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);
        configureWebView();
        webView.requestFocus();
        webView.loadUrl(SITE_URL);

        Toast.makeText(this,
                "Setas movem o cursor, OK clica. Segure OK por 1 segundo para trocar o modo.",
                Toast.LENGTH_LONG).show();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setUserAgentString(s.getUserAgentString() + USER_AGENT_SUFFIX);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setBackgroundColor(Color.BLACK);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false; // mantém toda a navegação dentro do app
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.isForMainFrame()) {
                    view.loadUrl(OFFLINE_URL);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                root.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                pointer.bringToFront();
            }

            @Override
            public void onHideCustomView() {
                hideCustomView();
            }
        });
    }

    private void hideCustomView() {
        if (customView == null) {
            return;
        }
        root.removeView(customView);
        customView = null;
        if (customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
            customViewCallback = null;
        }
        webView.requestFocus();
    }

    // ---------------------------------------------------------------- controle remoto

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        int action = event.getAction();

        // Teclas de mídia (play/pause do controle)
        if (code == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || code == KeyEvent.KEYCODE_MEDIA_PLAY
                || code == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (action == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                String mode = "toggle";
                if (code == KeyEvent.KEYCODE_MEDIA_PLAY) {
                    mode = "play";
                } else if (code == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                    mode = "pause";
                }
                runJs(String.format(Locale.US, PLAY_PAUSE_JS, mode));
            }
            return true;
        }

        // Botão OK
        if (code == KeyEvent.KEYCODE_DPAD_CENTER
                || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            if (action == KeyEvent.ACTION_DOWN) {
                if (event.getRepeatCount() == 0) {
                    longPressHandled = false;
                    centerDownTime = event.getEventTime();
                } else if (!longPressHandled
                        && event.getEventTime() - centerDownTime >= LONG_PRESS_MS) {
                    longPressHandled = true;
                    togglePointerMode();
                }
                if (pointerMode || longPressHandled) {
                    return true;
                }
                return super.dispatchKeyEvent(event);
            }
            if (action == KeyEvent.ACTION_UP) {
                if (longPressHandled) {
                    longPressHandled = false;
                    return true;
                }
                if (pointerMode) {
                    performClick();
                    return true;
                }
            }
            return super.dispatchKeyEvent(event);
        }

        // Setas movem o cursor (modo mouse)
        if (pointerMode && (code == KeyEvent.KEYCODE_DPAD_UP
                || code == KeyEvent.KEYCODE_DPAD_DOWN
                || code == KeyEvent.KEYCODE_DPAD_LEFT
                || code == KeyEvent.KEYCODE_DPAD_RIGHT)) {
            if (action == KeyEvent.ACTION_DOWN) {
                movePointer(code, event.getRepeatCount());
            }
            return true;
        }

        return super.dispatchKeyEvent(event);
    }

    private void togglePointerMode() {
        pointerMode = !pointerMode;
        pointer.setVisibility(pointerMode ? View.VISIBLE : View.INVISIBLE);
        Toast.makeText(this,
                pointerMode ? "Modo cursor (mouse)" : "Modo navegação por foco",
                Toast.LENGTH_SHORT).show();
    }

    private void movePointer(int code, int repeat) {
        float density = getResources().getDisplayMetrics().density;
        float step = (14f + Math.min(repeat, 20) * 3f) * density; // acelera ao segurar

        float dx = 0f;
        float dy = 0f;
        if (code == KeyEvent.KEYCODE_DPAD_UP) {
            dy = -step;
        } else if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
            dy = step;
        } else if (code == KeyEvent.KEYCODE_DPAD_LEFT) {
            dx = -step;
        } else if (code == KeyEvent.KEYCODE_DPAD_RIGHT) {
            dx = step;
        }

        float w = root.getWidth();
        float h = root.getHeight();
        float nx = pointer.getPx() + dx;
        float ny = pointer.getPy() + dy;
        float scrollX = 0f;
        float scrollY = 0f;

        if (nx < 0f) {
            scrollX = dx * 3f;
            nx = 0f;
        } else if (nx > w - 1f) {
            scrollX = dx * 3f;
            nx = w - 1f;
        }
        if (ny < 0f) {
            scrollY = dy * 3f;
            ny = 0f;
        } else if (ny > h - 1f) {
            scrollY = dy * 3f;
            ny = h - 1f;
        }

        pointer.moveTo(nx, ny);

        if ((scrollX != 0f || scrollY != 0f) && customView == null) {
            runJs(String.format(Locale.US, SCROLL_JS, nx, ny, scrollX, scrollY));
        }
    }

    private void performClick() {
        final View target = customView != null ? customView : webView;
        final float x = pointer.getPx();
        final float y = pointer.getPy();
        final long downTime = SystemClock.uptimeMillis();

        pointer.pulse();

        MotionEvent down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        target.dispatchTouchEvent(down);
        down.recycle();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                MotionEvent up = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_UP, x, y, 0);
                up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
                target.dispatchTouchEvent(up);
                up.recycle();
            }
        }, 70);
    }

    private void runJs(String js) {
        if (webView != null) {
            webView.evaluateJavascript(js, null);
        }
    }

    // ---------------------------------------------------------------- ciclo de vida

    @Override
    public void onBackPressed() {
        if (customView != null) {
            hideCustomView();
            return;
        }
        if (webView.canGoBack()) {
            webView.goBack();
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (now - lastBackPress < 2000L) {
            finish();
        } else {
            lastBackPress = now;
            Toast.makeText(this, "Pressione Voltar novamente para sair", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enterImmersive();
        }
    }

    @SuppressWarnings("deprecation")
    private void enterImmersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        root.removeAllViews();
        webView.destroy();
        super.onDestroy();
    }
}
