package com.example.capture;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.fonts.Font;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static ArrayList<String> phrases;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        verifyStoragePermissions(this);

        phrases = new ArrayList<>();
        initPhrases(phrases);
    }

    public void btnAccessGalleryOnClick(View view) {
        /*AlertDialog alertDialog1 = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog1.setTitle("Access Phone's Gallery");
        alertDialog1.setMessage("Let's take a look at your gallery");
        alertDialog1.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(), "you clicked on OK", Toast.LENGTH_SHORT).show();
            }
        });
        alertDialog1.show();*/
        Toast.makeText(getApplicationContext(), "Let's take a look at your gallery", Toast.LENGTH_SHORT).show();

        // Open the gallery
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 0);
    }

    // Dennis' sharing button
    public void btnShareOnClick(View view) {
        Toast.makeText(getApplicationContext(), "Dennis let's share our creation!", Toast.LENGTH_SHORT).show();
    }

    public void btnCameraOnClick(View view) {
        Toast.makeText(getApplicationContext(), "Open up the camera", Toast.LENGTH_SHORT).show();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // TODO: should be updated such that it is a custom resultCode signifying loading of an image from the gallery
        if (resultCode == RESULT_OK){
            Uri targetUri = data.getData();
            Bitmap bitmap;
            ImageView targetImage = findViewById(R.id.gallerySample);
            // TODO: fix orientation issue when landscape photo is used
            try {
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(targetUri));
                int rotate = checkForRotation(getRealPathFromURI(targetUri));
                if (rotate != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotate);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                }
                //Bitmap capturedBitmap = captureBitmap(this);
                //captureImage(bitmap);
                Bitmap capturedImage = captureImage(bitmap);
                targetImage.setImageBitmap(capturedImage);

                // TODO: Save this in a "Capture" folder on device
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                MediaStore.Images.Media.insertImage(getContentResolver(), capturedImage, "Capture_" + timeStamp, "");
                //createDirectoryAndSaveFile(capturedImage, "Capture_" + timeStamp);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    //private Bitmap captureBitmap(Context mContext) {// TODO: Refactor to also pass in desired string
    private Bitmap captureImage(Bitmap bitmap) { // TODO: Refactor to pass in reference to Bitmap, not Bitmap object (too memory heavy)
        // Constants
        int COLUMN_STEP_SIZE = 50;
        int ROW_STEP_SIZE = 50;

        int PIXEL_BUFFER = 5;

        // Size of image
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Standard deviation variables
        double mean;
        double squareDiffMean;
        double stDev;
        ArrayList<Double> rowStDev = new ArrayList<>();

        int r;
        int g;
        int b;

        // Colors - for average color
        float redBucket;
        float greenBucket;
        float blueBucket;
        float pixelCount;
        ArrayList<Integer> averageColor = new ArrayList<>();

        int gray;



        //Bitmap capturedBmp = Bitmap.createBitmap(bitmap);
        Bitmap capturedBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        /* Ideal empty space finding algorithm:
        Iterate over a percentage of image pixels (every ROW_STEP_SIZE and COL_STEP_SIZE), and store
        the grayscale value of this subset of pixels in a 2D ArrayList<Double>. Calculate standard
        deviation for each row and column outside of main iteration loop (should help reduce operating
        time (O(n^2) as opposed to current O(n^3)). Empty row for text location should effectively
        remain unchanged. Apply same approach to locate optimal column to centre the text around.
         */

        // TODO: Add support for handling edge (no pun intended) cases of text in last row/col of picture

        int minStDevIndex;
        for(int row = ROW_STEP_SIZE; row < height; row += ROW_STEP_SIZE) {
            ArrayList<Integer> grayRow = new ArrayList<>();

            redBucket = 0;
            greenBucket = 0;
            blueBucket = 0;
            pixelCount = 0;

            mean = 0;

            for(int col = COLUMN_STEP_SIZE; col < width; col += COLUMN_STEP_SIZE) {
                int c = bitmap.getPixel(col, row);
                r = (c >> 16) & 0xff;//c.red();
                g = (c >> 8) & 0xff;//c.green();
                b = c & 0xff;//c.blue();

                pixelCount++;
                redBucket += r;
                greenBucket += g;
                blueBucket += b;

                // int a = p >> 24 & 255; // Don't care about alpha value
                //int r = p >> 16 & 255;
                //int g = p >> 8 & 255;
                //int b = p & 255;
                gray = (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
                //int gray = (grayLevel << 16) + (grayLevel << 8) + grayLevel;
                grayRow.add(gray);

                mean += gray;
            }

            mean = mean / grayRow.size();
            squareDiffMean = 0;
            for (int i = 0; i < grayRow.size(); i++) {
                squareDiffMean += Math.pow(grayRow.get(i) - mean, 2);
            }
            squareDiffMean = squareDiffMean / grayRow.size();
            stDev = Math.sqrt(squareDiffMean);
            rowStDev.add(stDev);

            averageColor.add(Color.rgb(redBucket / pixelCount,
                                        greenBucket / pixelCount,
                                        blueBucket / pixelCount));
        }

        minStDevIndex = rowStDev.indexOf(Collections.min(rowStDev));

        // Draw caption on image
        Canvas canvas = new Canvas(capturedBmp);
        canvas.drawBitmap(bitmap, 0, 0, null);
        Paint paint = new Paint();
        paint.setColor(getContrastColor(averageColor.get(minStDevIndex))); //
        //String hexColor = String.format("#%06X", (0xFFFFFF & paint.getColor()));
        //Toast.makeText(getApplicationContext(), hexColor + "", Toast.LENGTH_SHORT).show();
        //paint.setTextSize(100); // TODO: Refine size and scaling factors
        paint.setTextSize((float) 0.029 * Math.max(width, height));
        //paint.setTextScaleX(1);
        // TODO: Any more text settings to play with? Font?
        //paint.setTypeface();
        //paint.measureText()

        Random random = new Random();
        String str = "I’m here wondering: Was I Am Legend was based on a true story?";
                // phrases.get(random.nextInt(phrases.size() + 1));

        // Find optimal x location
        ArrayList<Integer> selectedRowGrayValues = new ArrayList<>();
        for(int col = COLUMN_STEP_SIZE; col < (width - COLUMN_STEP_SIZE); col += COLUMN_STEP_SIZE) {
            int c = bitmap.getPixel(col, (minStDevIndex + 1) * ROW_STEP_SIZE);
            r = (c >> 16) & 0xff;
            g = (c >> 8) & 0xff;
            b = c & 0xff;

            gray = (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
            selectedRowGrayValues.add(gray);
        }

        squareDiffMean = 0;
        ArrayList<Double> stDevCol = new ArrayList<>();
        for (int i = 1; i < selectedRowGrayValues.size(); i++) {
            mean = (selectedRowGrayValues.get(i - 1) + selectedRowGrayValues.get(i)) / 2;
            squareDiffMean = (Math.pow(selectedRowGrayValues.get(i - 1) - mean, 2) + Math.pow(selectedRowGrayValues.get(i) - mean, 2)) / 2;
            stDevCol.add(Math.sqrt(squareDiffMean));
        }

        int minStDevCol = stDevCol.indexOf(Collections.min(stDevCol));

        canvas.drawBitmap(capturedBmp, 0, 0, paint);
        int x = (minStDevCol + 1) * COLUMN_STEP_SIZE;
        int y = (minStDevIndex + 1) * ROW_STEP_SIZE;


        // Size caption to fit
        paint.setTextSize((float) 0.029 * Math.max(width, height));
        int overshoot = 0;



        int textWidth = (int) Math.round(paint.measureText(str));

        if ((x + textWidth) > (width - PIXEL_BUFFER)) {
            overshoot = (x + textWidth) - width + PIXEL_BUFFER;
            if (overshoot < (0.2 * (width))){ // if overshoot is less than 20% of image width, just shift image over
                x -= overshoot;
            }

            // overshoot is greater than 20% but caption still fits within image boundaries
            if ((Math.abs(x) + textWidth) > (width - PIXEL_BUFFER)) {
                paint.setTextSize((float) 0.023 * Math.max(width, height));
                x -= (int) Math.round((x + paint.measureText(str)) - width);
            }
        }

        // if caption is just flat out too big for image
        double scalingFactor = 0.003;
        while ((paint.measureText(str)) > (width - PIXEL_BUFFER)) {
            x = PIXEL_BUFFER; //(width - 2 * PIXEL_BUFFER); // TODO: position with a random offset from centred
            paint.setTextSize((float) (0.029 - scalingFactor) * Math.max(width, height));
            scalingFactor += 0.003;
        }



        canvas.drawText(str, x, y, paint);

        // TODO: Remove this
        //Toast.makeText(getApplicationContext(), x + ", " + y + ": " + str, Toast.LENGTH_LONG).show();
        //Toast.makeText(getApplicationContext(), x + ", " + y + ": " + "|| w:" + width + ", h: " + height, Toast.LENGTH_LONG).show();
        return capturedBmp;
    }

    private int getContrastColor(@ColorInt int color) {
        // Counting the perceptive luminance - human eye favours green colour
        double a = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return (a < 0.5) ? Color.BLACK : Color.WHITE;
    }

    private static int checkForRotation(String imageFilePath) {
        int rotate = 0;
        try {
            ExifInterface exif;

            exif = new ExifInterface(imageFilePath);
            String exifOrientation = exif
                    .getAttribute(ExifInterface.TAG_ORIENTATION);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rotate;
    }

    public String getRealPathFromURI(Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = getContentResolver().query(contentUri, proj, null, null,
                    null);
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    private void createDirectoryAndSaveFile(Bitmap imageToSave, String fileName) { // TODO: Refactor this into two separate functions

        /*
        File directory = new File(Environment.getExternalStorageDirectory() + "/Capture/");

        if (!directory.exists()) {
            directory = new File(Environment.getExternalStorageDirectory().getPath() + "/Capture/");
            directory.mkdirs();
        }

        File file = new File(directory, fileName);
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            imageToSave.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("Capture2", Context.MODE_PRIVATE);

        File mypath = new File(directory, fileName);

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(mypath);
            imageToSave.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            Log.e("SAVE_IMAGE", e.getMessage(), e);
        }
    }

    private static void initPhrases(ArrayList<String> phrases) {
        phrases.add("Fingers smelling like garlic");
        phrases.add("Always use a coaster");
        phrases.add("Feeling blessed");
        phrases.add("A great place for squats");
        phrases.add("Look at all these chickens");
        phrases.add("Live for this");
        phrases.add("Grandma’s yumyums");
        phrases.add("I think it grew larger");
        phrases.add("All of this was made in China");
        phrases.add("Always use a coaster");
        phrases.add("Two Down, One to Go");
        phrases.add("Smells weird here");
        phrases.add("Hippity Hoppity Get off my Property");
        phrases.add("I still can’t believe it’s not butter");
        phrases.add("Just big people things");
        phrases.add("If I had a nickel for every time this happened…");
        phrases.add("What are those!?!");
        phrases.add("She said yes ?");
        phrases.add("If my parents had wheels, they would be bicycles");
        phrases.add("MMm MMm Grandma!");
        phrases.add("3rd time’s the charm!");
        phrases.add("With Great Power comes Great Responsibility");
        phrases.add("I’m a level 900 Dark Wizard");
        phrases.add("I wish I had my fidget spinner right now");
        phrases.add("Is there anything better than unlimited garlic bread?");
        phrases.add("Parmesan? More like pardon me, Sam.");
        phrases.add("No Officer, it’s Hi, How are you?");
        phrases.add("I never run with scissors");
        phrases.add("3rd time’s the charm!");
        phrases.add("With Great Power comes Great Responsibility");
        phrases.add("I’m a level 900 Dark Wizard");
        phrases.add("I wish I had my fidget spinner right now");
        phrases.add("Is there anything better than unlimited garlic bread?");
        phrases.add("Parmesan? More like pardon me, Sam.");
        phrases.add("No Officer, it’s Hi, How are you?");
        phrases.add("I never run with scissors");
        phrases.add("That's a yikers!");
        phrases.add("Everything fits if you're brave enough ;)");
        phrases.add("it's not the size that counts. It's how you use it!");
        phrases.add("Ah, to be young and in love...");
        phrases.add("I’m here wondering: Was I Am Legend was based on a true story?");
        phrases.add("Well well well...we meet again");
        phrases.add("They see me rollin’ They hatin’");
        phrases.add("+1 to negotiation");
        phrases.add("I would not last in prison");
        phrases.add("Life gave me lemons. Now I’m here");
        phrases.add("You know what would make this better? Baby wipes");
        phrases.add("Ok Boomer");
        phrases.add("Can you tell I don’t wanna be here?");
        phrases.add("RIP my thighs");
        phrases.add("Here for a liver transplant. Not a long time.");
        phrases.add("Apply directly to the forehead");
        phrases.add("Send Dudes");
        phrases.add("Sand Nudes");
        phrases.add("Nend Sudes");
        phrases.add("The only language I speak is beef");

    }
}
