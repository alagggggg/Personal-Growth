package com.nhatkycaitien.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class CropActivity extends Activity {
    private CropImageView cropView;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_crop);
        cropView = findViewById(R.id.cropView);
        try {
            Uri source = getIntent().getData();
            Bitmap bitmap = decodeSampled(source, 2200);
            if (bitmap == null) throw new Exception("Không đọc được ảnh");
            cropView.setImage(rotateByExif(source, bitmap));
        } catch (Exception error) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        findViewById(R.id.cropCancel).setOnClickListener(view -> { setResult(RESULT_CANCELED); finish(); });
        findViewById(R.id.cropUse).setOnClickListener(view -> saveCrop());
    }

    private Bitmap decodeSampled(Uri uri, int maxSide) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(input, null, bounds); }
        int sample = 1;
        while (Math.max(bounds.outWidth / sample, bounds.outHeight / sample) > maxSide) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = Math.max(1, sample);
        try (InputStream input = getContentResolver().openInputStream(uri)) { return BitmapFactory.decodeStream(input, null, options); }
    }

    private Bitmap rotateByExif(Uri uri, Bitmap bitmap) {
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            ExifInterface exif = new ExifInterface(input);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            float degrees = 0;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) degrees = 90;
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) degrees = 180;
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) degrees = 270;
            if (degrees == 0) return bitmap;
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (Exception ignored) { return bitmap; }
    }

    private void saveCrop() {
        try {
            Bitmap output = cropView.crop(1200);
            File directory = new File(getCacheDir(), "crop");
            if (!directory.exists() && !directory.mkdirs()) throw new Exception("Không tạo được thư mục cắt ảnh");
            File file = File.createTempFile("crop_", ".jpg", directory);
            try (OutputStream stream = new FileOutputStream(file)) { output.compress(Bitmap.CompressFormat.JPEG, 86, stream); }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent result = new Intent();
            result.setData(uri);
            result.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            setResult(RESULT_OK, result);
            finish();
        } catch (Exception error) {
            setResult(RESULT_CANCELED);
            finish();
        }
    }
}
