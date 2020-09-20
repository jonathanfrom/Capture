package com.example.capture;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.fonts.Font;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static ArrayList<String> phrases;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
                //Bitmap capturedBitmap = captureBitmap(this);
                //captureImage(bitmap);
                targetImage.setImageBitmap(captureImage(bitmap));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    //private Bitmap captureBitmap(Context mContext) {// TODO: Refactor to also pass in desired string
    private Bitmap captureImage(Bitmap bitmap) {
        // Constants
        int COLUMN_STEP_SIZE = 50;
        int ROW_STEP_SIZE = 50;

        // Size of image
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Standard deviation variables
        int sum = 0;
        int squareSum = 0;
        double squareMean;
        double stDev;
        ArrayList<Double> rowStDev = new ArrayList<>();
        double mean;
        double squareDiffMean;

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

        int minStDevIndex;
        for(int row = ROW_STEP_SIZE; row < height; row += ROW_STEP_SIZE) {
            ArrayList<Integer> grayRow = new ArrayList<>();

            redBucket = 0;
            greenBucket = 0;
            blueBucket = 0;
            pixelCount = 0;

            mean = 0;

            for(int col = COLUMN_STEP_SIZE; col < width; col += COLUMN_STEP_SIZE) {
                //Color c = capturedBmp.getColor(col, row);
                Color c = bitmap.getColor(col, row);
                r = (c.toArgb() >> 16) & 0xff;//c.red();
                g = (c.toArgb() >> 8) & 0xff;//c.green();
                b = c.toArgb() & 0xff;//c.blue();

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

            /*Integer colourVal = 0;
            Iterator grayIter;
            for(grayIter = grayRow.iterator(); grayIter.hasNext();
                squareSum = (int)((double)squareSum + Math.pow((double)colourVal, 2))) {
                colourVal = (Integer)grayIter.next();
                sum += colourVal;
            }

            squareMean = Math.pow((double)sum, 2) / (double)grayRow.size();
            stDev = Math.sqrt((double)squareSum - squareMean) / (double)grayRow.size();
            rowStDev.add(stDev);*/

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
        paint.setTextSize(100); // TODO: Refine size and scaling factors
        paint.setTextScaleX(1);
        // TODO: Any more text settings to play with? Font?

        Random random = new Random();
        String str = phrases.get(random.nextInt(phrases.size() + 1));

        canvas.drawBitmap(capturedBmp, 0, 0, paint);
        int x = random.nextInt(width - 100);
        int y = (minStDevIndex + 1) * ROW_STEP_SIZE;
        canvas.drawText(str, x, y, paint);

        // TODO: Remove this
        //Toast.makeText(getApplicationContext(), x + ", " + y + ": " + str, Toast.LENGTH_LONG).show();
        Toast.makeText(getApplicationContext(), x + ", " + y + ": " + "|| w:" + width + ", h: " + height, Toast.LENGTH_LONG).show();
        return capturedBmp;
    }

    private int getContrastColor(@ColorInt int color) {
        // Counting the perceptive luminance - human eye favours green colour
        double a = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return (a < 0.5) ? Color.BLACK : Color.WHITE;
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
    }
}
