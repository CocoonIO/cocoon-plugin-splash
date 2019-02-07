package com.ludei.splash.android;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class SplashPlugin extends CordovaPlugin implements ViewTreeObserver.OnGlobalLayoutListener {

    public static final String ORIGINAL_BUNDLE_ID = "_BUNDLE_ID_";

    private static final String SCALE_ASPECT_FIT = "scaleAspectFit";
    private static final String SCALE_ASPECT_FILL = "scaleAspectFill";
    private static final String SCALE_TO_FILL = "scaleToFill";

    private static final int SPLASH_DEFAULT_DELAY = 3;
    private static final String SPLASH_DEFAULT_BACKGROUND_COLOR = "#1c2023";

    private static String SPLASH_SCALE_MODE = "DefaultCocoonSplashScaleMode";
    private static String SPLASH_BACKGROUND_COLOR = "DefaultCocoonSplashBackgroundColor";
    private static String SPLASH_DELAY = "DefaultCocoonSplashDelay";
    private static String SPLASH_PADDING = "DefaultCocoonSplashPadding";

    private String splashScaleMode = SCALE_ASPECT_FIT;
    private String splashBackgroundColor = SPLASH_DEFAULT_BACKGROUND_COLOR;
    public static int splashDelay = SPLASH_DEFAULT_DELAY;
    private String splashPadding = "0";

    private static Dialog splashDialog;

    private View root;
    private boolean screenReady = false;

    static {
        System.loadLibrary("SplashPlugin");
    }

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    @Override
    public void initialize(final CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        splashScaleMode = preferences.getString(SPLASH_SCALE_MODE, SCALE_ASPECT_FIT);
        splashBackgroundColor = preferences.getString(SPLASH_BACKGROUND_COLOR, SPLASH_DEFAULT_BACKGROUND_COLOR);
        splashDelay = preferences.getInteger(SPLASH_DELAY, SPLASH_DEFAULT_DELAY) * 1000;
        splashPadding = preferences.getString(SPLASH_PADDING, "0");

        webView.getPluginManager().postMessage("com.ludei.splash.android.delay", splashDelay);
    }

    @Override
    protected void pluginInitialize() {
        root = cordova.getActivity().getWindow().getDecorView().findViewById(android.R.id.content);
        ViewTreeObserver vto = root.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(this);

        prepareSplash();
    }

    private void prepareSplash() {
        cordova.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                // Create and show the dialog
                splashDialog = new Dialog(cordova.getActivity(), android.R.style.Theme_Translucent_NoTitleBar);
                // check to see if the splash screen should be full screen
                if ((cordova.getActivity().getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) == WindowManager.LayoutParams.FLAG_FULLSCREEN) {
                    splashDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }
                splashDialog.setCancelable(false);
                splashDialog.show();
            }
        });
    }

    private void showSplash() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                removeSplash();
            }
        }, splashDelay);
    }

    private void removeSplash() {
        if (splashDialog != null && splashDialog.isShowing()) {
            if (splashDialog != null) {
                splashDialog.dismiss();
                splashDialog = null;
            }

            ViewTreeObserver vto = root.getViewTreeObserver();
            vto.removeGlobalOnLayoutListener(this);
        }
    }

    public class ByteBufferBackedInputStream extends InputStream {

        ByteBuffer buf;

        public ByteBufferBackedInputStream(ByteBuffer buf) {
            this.buf = buf;
        }

        public int read() throws IOException {
            if (!buf.hasRemaining()) {
                return -1;
            }
            return buf.get() & 0xFF;
        }

        public int read(byte[] bytes, int off, int len)
                throws IOException {
            if (!buf.hasRemaining()) {
                return -1;
            }

            len = Math.min(len, buf.remaining());
            buf.get(bytes, off, len);
            return len;
        }
    }

    private native ByteBuffer getNativeSplash();

    private ImageView getSplashView(Rect screenSize) throws IOException {
        if (screenSize.width() <=0 || screenSize.height() <= 0)
            return null;

        // Get padding
        int padding = 0;
        if (splashPadding.contains("%")) {
            int index = splashPadding.lastIndexOf("%");
            padding = Integer.parseInt(splashPadding.substring(0, index));
            padding = Math.min(screenSize.width(), screenSize.height()) * padding/100;

        } else {
            padding = Integer.parseInt(splashPadding);
        }

        // Use an ImageView to render the image because of its flexible scaling options.
        ByteBufferBackedInputStream is = new ByteBufferBackedInputStream(getNativeSplash());
        Bitmap originalImage = BitmapFactory.decodeStream(is);
        Bitmap image = Bitmap.createBitmap(screenSize.width(), screenSize.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawColor(SplashPlugin.ColorFromHTMLColor(splashBackgroundColor));

        float tw = screenSize.width() - padding * 2;
        float th = screenSize.height() - padding * 2;
        float canvasAspectRatio = tw/th;
        float imageAspectRatio = originalImage.getWidth()/originalImage.getHeight();

        float dx;
        float dy;
        float dw;
        float dh;

        if (splashScaleMode.toLowerCase().equalsIgnoreCase(SCALE_TO_FILL.toLowerCase())) {
            dw = tw;
            dh = th;
            dx = padding;
            dy = padding;

        } else if (splashScaleMode.toLowerCase().equalsIgnoreCase(SCALE_ASPECT_FILL.toLowerCase())) {
            if (imageAspectRatio > canvasAspectRatio) {
                dw = th/originalImage.getHeight() * originalImage.getWidth();
                dh = th;
            }
            else {
                dw = tw;
                dh = tw/originalImage.getWidth() * originalImage.getHeight();
            }
            dx = screenSize.width()/2 - dw/2;
            dy = screenSize.height()/2 - dh/2;

        } else {
            if (canvasAspectRatio > imageAspectRatio) {
                dw = th/originalImage.getHeight() * originalImage.getWidth();
                dh = th;
            }
            else {
                dw = tw;
                dh = tw/originalImage.getWidth() * originalImage.getHeight();
            }
            dx = screenSize.width()/2 - dw/2;
            dy = screenSize.height()/2 - dh/2;
        }

        Rect srcRect = new Rect(0, 0, originalImage.getWidth(), originalImage.getHeight());
        Rect dstRect = new Rect((int)(dx), (int)dy, (int)(dx+dw), (int)(dy+dh));
        canvas.drawBitmap(originalImage, srcRect, dstRect, null);

        ImageView imageView = new ImageView(cordova.getActivity());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        imageView.setLayoutParams(layoutParams);
        imageView.setImageBitmap(image);

        return imageView;
    }

    @Override
    public void onGlobalLayout() {
        Rect screenSize = new Rect();
        Window window = cordova.getActivity().getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(screenSize);

        try {
            ImageView splash = getSplashView(screenSize);
            if (splash != null && splashDialog != null)
                splashDialog.setContentView(splash);

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (screenReady == false && screenSize.width() >0 && screenSize.height() > 0) {
            screenReady = true;

            webView.getPluginManager().postMessage("com.ludei.splash.android.started", true);

            showSplash();
        }
    }

    private static HashMap<String, String> colorHashMap;
    static {
        colorHashMap = new HashMap<String, String>();
        colorHashMap.put("aqua", "#00ffff");
        colorHashMap.put("black", "#000000");
        colorHashMap.put("blue", "#0000ff");
        colorHashMap.put("fuchsia", "#ff00ff");
        colorHashMap.put("green", "#008000");
        colorHashMap.put("gray", "#808080");
        colorHashMap.put("grey", "#808080");
        colorHashMap.put("lime", "#00ff00");
        colorHashMap.put("maroon", "#800000");
        colorHashMap.put("navy", "#000080");
        colorHashMap.put("olive", "#808000");
        colorHashMap.put("purple", "#800080");
        colorHashMap.put("red", "#ff0000");
        colorHashMap.put("silver", "#c0c0c0");
        colorHashMap.put("teal", "#008080");
        colorHashMap.put("white", "#ffffff");
        colorHashMap.put("yellow", "#ffff00");
        colorHashMap.put("aliceblue", "#f0f8ff");
        colorHashMap.put("antiquewhite", "#faebd7");
        colorHashMap.put("aquamarine", "#7fffd4");
        colorHashMap.put("azure", "#f0ffff");
        colorHashMap.put("beige", "#f5f5dc");
        colorHashMap.put("bisque", "#ffe4c4");
        colorHashMap.put("blanchedalmond", "#ffebcd");
        colorHashMap.put("blueviolet", "#8a2be2");
        colorHashMap.put("brown", "#a52a2a");
        colorHashMap.put("burlywood", "#deb887");
        colorHashMap.put("cadetblue", "#5f9ea0");
        colorHashMap.put("chartreuse", "#7fff00");
        colorHashMap.put("chocolate", "#d2691e");
        colorHashMap.put("coral", "#ff7f50");
        colorHashMap.put("cornflowerblue", "#6495ed");
        colorHashMap.put("cornsilk", "#fff8dc");
        colorHashMap.put("crimson", "#dc143c");
        colorHashMap.put("cyan", "#00ffff");
        colorHashMap.put("darkblue", "#00008b");
        colorHashMap.put("darkcyan", "#008b8b");
        colorHashMap.put("darkgoldenrod", "#b8860b");
        colorHashMap.put("darkgray", "#a9a9a9");
        colorHashMap.put("darkgreen", "#006400");
        colorHashMap.put("darkkhaki", "#bdb76b");
        colorHashMap.put("darkmagenta", "#8b008b");
        colorHashMap.put("darkolivegreen", "#556b2f");
        colorHashMap.put("darkorange", "#ff8c00");
        colorHashMap.put("darkorchid", "#9932cc");
        colorHashMap.put("darkred", "#8b0000");
        colorHashMap.put("darksalmon", "#e9967a");
        colorHashMap.put("darkseagreen", "#8fbc8f");
        colorHashMap.put("darkslateblue", "#483d8b");
        colorHashMap.put("darkslategray", "#2f4f4f");
        colorHashMap.put("darkturquoise", "#00ced1");
        colorHashMap.put("darkviolet", "#9400d3");
        colorHashMap.put("deeppink", "#ff1493");
        colorHashMap.put("deepskyblue", "#00bfff");
        colorHashMap.put("dimgray", "#696969");
        colorHashMap.put("dodgerblue", "#1e90ff");
        colorHashMap.put("firebrick", "#b22222");
        colorHashMap.put("floralwhite", "#fffaf0");
        colorHashMap.put("forestgreen", "#228b22");
        colorHashMap.put("gainsboro", "#dcdcdc");
        colorHashMap.put("ghostwhite", "#f8f8ff");
        colorHashMap.put("gold", "#ffd700");
        colorHashMap.put("goldenrod", "#daa520");
        colorHashMap.put("gray", "#808080");
        colorHashMap.put("greenyellow", "#adff2f");
        colorHashMap.put("honeydew", "#f0fff0");
        colorHashMap.put("hotpink", "#ff69b4");
        colorHashMap.put("indianred", "#cd5c5c");
        colorHashMap.put("indigo", "#4b0082");
        colorHashMap.put("ivory", "#fffff0");
        colorHashMap.put("khaki", "#f0e68c");
        colorHashMap.put("lavender", "#e6e6fa");
        colorHashMap.put("lavenderblush", "#fff0f5");
        colorHashMap.put("lawngreen", "#7cfc00");
        colorHashMap.put("lemonchiffon", "#fffacd");
        colorHashMap.put("lightblue", "#add8e6");
        colorHashMap.put("lightcoral", "#f08080");
        colorHashMap.put("lightcyan", "#e0ffff");
        colorHashMap.put("lightgoldenrodyellow", "#fafad2");
        colorHashMap.put("lightgreen", "#90ee90");
        colorHashMap.put("lightgrey", "#d3d3d3");
        colorHashMap.put("lightpink", "#ffb6c1");
        colorHashMap.put("lightsalmon", "#ffa07a");
        colorHashMap.put("lightseagreen", "#20b2aa");
        colorHashMap.put("lightskyblue", "#87cefa");
        colorHashMap.put("lightslategray", "#778899");
        colorHashMap.put("lightsteelblue", "#b0c4de");
        colorHashMap.put("lightyellow", "#ffffe0");
        colorHashMap.put("limegreen", "#32cd32");
        colorHashMap.put("linen", "#faf0e6");
        colorHashMap.put("magenta", "#ff00ff");
        colorHashMap.put("mediumblue", "#0000cd");
        colorHashMap.put("mediumorchid", "#ba55d3");
        colorHashMap.put("mediumpurple", "#9370db");
        colorHashMap.put("midnightblue", "#191970");
        colorHashMap.put("mistyrose", "#ffe4e1");
        colorHashMap.put("moccasin", "#ffe4b5");
        colorHashMap.put("oldlace", "#fdf5e6");
        colorHashMap.put("orange", "#ffa500");
        colorHashMap.put("orchid", "#da70d6");
        colorHashMap.put("peachpuff", "#ffdab9");
        colorHashMap.put("peru", "#cd853f");
        colorHashMap.put("pink", "#ffc0cb");
        colorHashMap.put("plum", "#dda0dd");
        colorHashMap.put("purple", "#800080");
        colorHashMap.put("rosybrown", "#bc8f8f");
        colorHashMap.put("royalblue", "#4169e1");
        colorHashMap.put("salmon", "#fa8072");
        colorHashMap.put("sandybrown", "#f4a460");
        colorHashMap.put("seagreen", "#2e8b57");
        colorHashMap.put("sienna", "#a0522d");
        colorHashMap.put("skyblue", "#87ceeb");
        colorHashMap.put("slateblue", "#6a5acd");
        colorHashMap.put("steelblue", "#4682b4");
        colorHashMap.put("tan", "#d2b48c");
        colorHashMap.put("thistle", "#d8bfd8");
        colorHashMap.put("tomato", "#ff6347");
        colorHashMap.put("violet", "#ee82ee");
        colorHashMap.put("wheat", "#f5deb3");
        colorHashMap.put("whitesmoke", "#f5f5f5");
        colorHashMap.put("yellow", "#ffff00");
        colorHashMap.put("yellowgreen", "#9acd32ff");
    }

    private static int ColorFromHTMLColor(String htmlColor) {
        String color = colorHashMap.get(htmlColor);
        if (color != null) {
            htmlColor = color;

        } else if(!htmlColor.startsWith("#")) {
            htmlColor = "#".concat(htmlColor);
        }

        return Color.parseColor(htmlColor);
    }
}