package com.nhatkycaitien.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private Uri cameraUri;
    private final ArrayList<Uri> cropQueue = new ArrayList<>();
    private final ArrayList<Uri> cropped = new ArrayList<>();
    private String pendingJson;
    private String pendingCaseId;
    private String nativeTarget;
    private boolean pageReady = false;
    private boolean pendingOpenNew = false;

    private static final int PICK_IMAGES = 101;
    private static final int TAKE_PHOTO = 102;
    private static final int CROP_IMAGE = 103;
    private static final int CREATE_JSON = 104;
    private static final int PICK_JSON = 105;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(true);
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                pageReady = true;
                openPendingCase();
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                resetImageSession(true);
                fileCallback = callback;
                boolean capture = params.isCaptureEnabled();
                if (capture) return openCamera();
                return openGallery(params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
            }
        });
        readWidgetIntent(getIntent());
        boolean widgetLaunch = pendingOpenNew || pendingCaseId != null;
        if (state == null || widgetLaunch) webView.loadUrl("file:///android_asset/index.html");
        else {
            webView.restoreState(state);
            webView.postDelayed(() -> { pageReady = true; openPendingCase(); }, 500);
        }
    }

    private boolean openCamera() {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(getPackageManager()) == null) {
                showError("Không tìm thấy ứng dụng Camera trên điện thoại");
                resetImageSession(true);
                return false;
            }
            File dir = new File(getCacheDir(), "camera");
            if (!dir.exists() && !dir.mkdirs()) throw new Exception("Không tạo được thư mục ảnh tạm");
            File file = File.createTempFile("photo_", ".jpg", dir);
            cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
            intent.setClipData(ClipData.newRawUri("camera_output", cameraUri));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            List<ResolveInfo> handlers = getPackageManager().queryIntentActivities(intent, 0);
            for (ResolveInfo info : handlers) {
                grantUriPermission(info.activityInfo.packageName, cameraUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            startActivityForResult(intent, TAKE_PHOTO);
            return true;
        } catch (Exception error) {
            showError("Không mở được Camera: " + safeMessage(error));
            resetImageSession(true);
            return false;
        }
    }

    private boolean openGallery(boolean multiple) {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple);
            startActivityForResult(intent, PICK_IMAGES);
            return true;
        } catch (Exception error) {
            showError("Không mở được thư viện ảnh: " + safeMessage(error));
            resetImageSession(true);
            return false;
        }
    }

    private void addToQueue(Uri uri) {
        if (uri != null && !cropQueue.contains(uri)) cropQueue.add(uri);
    }

    private void startCropQueue() {
        cropped.clear();
        cropNext();
    }

    private void cropNext() {
        if (cropQueue.isEmpty()) {
            finishImages();
            return;
        }
        Uri source = cropQueue.remove(0);
        try {
            Intent intent = new Intent(this, CropActivity.class);
            intent.setData(source);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CROP_IMAGE);
        } catch (Exception error) {
            showError("Không mở được màn hình cắt ảnh");
            cropNext();
        }
    }

    private void finishImages() {
        if (nativeTarget != null) {
            JSONArray array = new JSONArray();
            for (Uri uri : cropped) {
                try (InputStream input = getContentResolver().openInputStream(uri)) {
                    if (input == null) continue;
                    array.put("data:image/jpeg;base64," + Base64.encodeToString(readAll(input), Base64.NO_WRAP));
                } catch (Exception ignored) {}
            }
            String target = nativeTarget;
            nativeTarget = null;
            String script = "receiveNativePhotos(" + JSONObject.quote(target) + "," + JSONObject.quote(array.toString()) + ")";
            webView.post(() -> webView.evaluateJavascript(script, null));
        } else if (fileCallback != null) {
            fileCallback.onReceiveValue(cropped.isEmpty() ? null : cropped.toArray(new Uri[0]));
            fileCallback = null;
        }
        clearImageState();
    }

    private void resetImageSession(boolean notifyWebView) {
        if (notifyWebView && fileCallback != null) fileCallback.onReceiveValue(null);
        fileCallback = null;
        nativeTarget = null;
        clearImageState();
    }

    private void clearImageState() {
        cropQueue.clear();
        cropped.clear();
        if (cameraUri != null) {
            try { revokeUriPermission(cameraUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION); }
            catch (Exception ignored) {}
        }
        cameraUri = null;
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == TAKE_PHOTO) {
            if (result == RESULT_OK && cameraUri != null && canRead(cameraUri)) {
                addToQueue(cameraUri);
                startCropQueue();
            } else {
                showError("Không nhận được ảnh từ Camera");
                resetImageSession(true);
            }
            return;
        }
        if (request == PICK_IMAGES) {
            if (result == RESULT_OK && data != null) {
                ClipData clip = data.getClipData();
                if (clip != null) {
                    for (int i = 0; i < clip.getItemCount(); i++) addToQueue(clip.getItemAt(i).getUri());
                } else addToQueue(data.getData());
                if (!cropQueue.isEmpty()) startCropQueue();
                else resetImageSession(true);
            } else resetImageSession(true);
            return;
        }
        if (request == CROP_IMAGE) {
            if (result == RESULT_OK && data != null && data.getData() != null) {
                cropped.add(data.getData());
                cropNext();
            } else {
                // Skip only the cancelled image, keep previously cropped images.
                cropNext();
            }
            return;
        }
        if (request == PICK_JSON) {if(result==RESULT_OK&&data!=null&&data.getData()!=null){try(InputStream input=getContentResolver().openInputStream(data.getData())){if(input==null)throw new Exception("Không mở được tệp sao lưu");byte[] backupBytes=readAll(input);String content=decodeBackupText(backupBytes);String script="receiveNativeBackup("+JSONObject.quote(content)+")";webView.post(()->webView.evaluateJavascript(script,null));}catch(Exception error){showError("Không đọc được tệp sao lưu: "+safeMessage(error));}}return;}
        if (request == CREATE_JSON) {
            if (result == RESULT_OK && data != null && data.getData() != null && pendingJson != null) {
                try (OutputStream output = getContentResolver().openOutputStream(data.getData())) {
                    if (output == null) throw new Exception("Không mở được tệp");
                    output.write(pendingJson.getBytes(StandardCharsets.UTF_8));
                    Toast.makeText(this, "Đã sao lưu JSON", Toast.LENGTH_LONG).show();
                } catch (Exception error) {
                    showError("Sao lưu thất bại: " + safeMessage(error));
                }
            }
            pendingJson = null;
        }
    }

    private boolean canRead(Uri uri) {
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            return input != null && input.read() >= 0;
        } catch (Exception error) { return false; }
    }

    private static String decodeBackupText(byte[] data) throws Exception {
        if (data == null || data.length == 0) return "";
        if (data.length >= 2 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xFE)
            return new String(data, 2, data.length - 2, java.nio.charset.StandardCharsets.UTF_16LE);
        if (data.length >= 2 && (data[0] & 0xFF) == 0xFE && (data[1] & 0xFF) == 0xFF)
            return new String(data, 2, data.length - 2, java.nio.charset.StandardCharsets.UTF_16BE);
        if (data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF)
            return new String(data, 3, data.length - 3, java.nio.charset.StandardCharsets.UTF_8);
        return new String(data, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static byte[] readAll(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) > 0) output.write(buffer, 0, count);
        return output.toByteArray();
    }

    private void showError(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    private static String safeMessage(Exception error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    private static final String FULL_DATA_PREFS = "personal_growth_full_data";
    private static final String FULL_DATA_KEY = "cases_json";

    public class AndroidBridge {
        @JavascriptInterface public boolean saveFullData(String json) {
            try {
                String value = json == null ? "[]" : json;
                return getSharedPreferences(FULL_DATA_PREFS, MODE_PRIVATE)
                    .edit().putString(FULL_DATA_KEY, value).commit();
            } catch (Exception error) {
                return false;
            }
        }

        @JavascriptInterface public String loadFullData() {
            try {
                return getSharedPreferences(FULL_DATA_PREFS, MODE_PRIVATE)
                    .getString(FULL_DATA_KEY, "");
            } catch (Exception error) {
                return "";
            }
        }

        @JavascriptInterface public void selectPhotos(String source, String target) {
            runOnUiThread(() -> {
                resetImageSession(true);
                nativeTarget = "case".equals(target) ? "case" : "new";
                if ("camera".equals(source)) openCamera();
                else openGallery(true);
            });
        }

        @JavascriptInterface public void pickJsonBackup(){runOnUiThread(()->{Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("*/*");intent.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/json","text/plain","application/octet-stream"});startActivityForResult(intent,PICK_JSON);});}

        @JavascriptInterface public void saveJson(String name, String content) {
            runOnUiThread(() -> {
                pendingJson = content;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, name == null || name.isEmpty() ? "nhat_ky_cai_tien_backup.json" : name);
                startActivityForResult(intent, CREATE_JSON);
            });
        }

        @JavascriptInterface public void finishWidgetSession(boolean saved) {
            runOnUiThread(() -> {
                long delay = saved ? 550L : 0L;
                webView.postDelayed(() -> {
                    try { NhatKyWidgetProvider.refreshAll(MainActivity.this); } catch (Exception ignored) {}
                    if (android.os.Build.VERSION.SDK_INT >= 21) finishAndRemoveTask();
                    else finish();
                }, delay);
            });
        }

        @JavascriptInterface public void syncWidgetData(String json) {
            runOnUiThread(() -> {
                getSharedPreferences(NhatKyWidgetProvider.PREFS, MODE_PRIVATE).edit()
                    .putString(NhatKyWidgetProvider.KEY, json == null ? "[]" : json).apply();
                NhatKyWidgetProvider.refreshAll(MainActivity.this);
            });
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readWidgetIntent(intent);
        openPendingCase();
    }

    private void readWidgetIntent(Intent intent) {
        if (intent == null) return;
        pendingOpenNew = intent.getBooleanExtra(NhatKyWidgetProvider.EXTRA_OPEN_NEW, false);
        String id = intent.getStringExtra(NhatKyWidgetProvider.EXTRA_CASE_ID);
        if (id != null && !id.isEmpty()) pendingCaseId = id;
    }

    private void openPendingCase() {if(!pageReady)return;if(pendingOpenNew){pendingOpenNew=false;webView.evaluateJavascript("if(typeof enableWidgetQuickCreate==='function'){enableWidgetQuickCreate()} if(typeof openNew==='function'){openNew('problem')}",null);return;}if(pendingCaseId==null)return;String id=pendingCaseId;pendingCaseId=null;webView.evaluateJavascript("if(typeof enableWidgetQuickDetail==='function'){enableWidgetQuickDetail()} if(typeof openDetail==='function'){openDetail("+JSONObject.quote(id)+")}",null);}

    @Override protected void onSaveInstanceState(Bundle bundle) {
        webView.saveState(bundle);
        super.onSaveInstanceState(bundle);
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
