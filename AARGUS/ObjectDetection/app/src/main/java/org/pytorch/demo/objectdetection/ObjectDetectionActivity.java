package org.pytorch.demo.objectdetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.VibrationEffect;
import android.util.DisplayMetrics;
import android.view.TextureView;
import android.view.ViewStub;

//New Imports
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.camera.core.ImageProxy;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.PyTorchAndroid;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Map;

public class ObjectDetectionActivity extends AbstractCameraXActivity<ObjectDetectionActivity.AnalysisResult> {
    private Module mModule = null;
    private ResultView mResultView;
    private TextToSpeech mTTS;


    static class AnalysisResult {
        private final ArrayList<Result> mResults;

        public AnalysisResult(ArrayList<Result> results) {
            mResults = results;
        }
    }

    @Override
    protected int getContentViewLayoutId() {
        return R.layout.activity_object_detection;
    }

    @Override
    protected TextureView getCameraPreviewTextureView() {
        mResultView = findViewById(R.id.resultView);
        return ((ViewStub) findViewById(R.id.object_detection_texture_view_stub))
                .inflate()
                .findViewById(R.id.object_detection_texture_view);
    }

    @Override
    protected void applyToUiAnalyzeImageResult(AnalysisResult result) {
        mResultView.setResults(result.mResults);
        mResultView.invalidate();
    }

    private Bitmap imgToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    @Override
    @WorkerThread
    @Nullable
    protected AnalysisResult analyzeImage(ImageProxy image, int rotationDegrees) {
        if (mModule == null) {
            mModule = PyTorchAndroid.loadModuleFromAsset(getAssets(), "d2go.pt");
        }
        Bitmap bitmap = imgToBitmap(image.getImage());
        Matrix matrix = new Matrix();
        matrix.postRotate(90.0f);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        //TEXT TO SPEECH SECTION

        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = mTTS.setLanguage(Locale.US);

                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });

        //END TEXT TO SPEECH DEFINES

        final FloatBuffer floatBuffer = Tensor.allocateFloatBuffer(3 * bitmap.getWidth() * bitmap.getHeight());
        TensorImageUtils.bitmapToFloatBuffer(bitmap, 0,0,bitmap.getWidth(),bitmap.getHeight(), PrePostProcessor.NO_MEAN_RGB, PrePostProcessor.NO_STD_RGB, floatBuffer, 0);
        final Tensor inputTensor =  Tensor.fromBlob(floatBuffer, new long[] {3, bitmap.getHeight(), bitmap.getWidth()});

        IValue[] outputTuple = mModule.forward(IValue.listFrom(inputTensor)).toTuple();
        final Map<String, IValue> map = outputTuple[1].toList()[0].toDictStringKey();
        float[] boxesData = new float[]{};
        float[] scoresData = new float[]{};
        long[] labelsData = new long[]{};

        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (map.containsKey("boxes")) {
            final Tensor boxesTensor = map.get("boxes").toTensor();
            final Tensor scoresTensor = map.get("scores").toTensor();
            final Tensor labelsTensor = map.get("labels").toTensor();
            boxesData = boxesTensor.getDataAsFloatArray();
            scoresData = scoresTensor.getDataAsFloatArray();
            labelsData = labelsTensor.getDataAsLongArray();

            final int n = scoresData.length;
            int count = 0;
            float[] outputs = new float[n * PrePostProcessor.OUTPUT_COLUMN];
            for (int i = 0; i < n; i++) {
                if (scoresData[i] < 0.6) //TODO: Modify score for precision (Original precision was 0.4)
                    continue;

                outputs[PrePostProcessor.OUTPUT_COLUMN * count] = boxesData[4 * i];
                outputs[PrePostProcessor.OUTPUT_COLUMN * count + 1] = boxesData[4 * i + 1];
                outputs[PrePostProcessor.OUTPUT_COLUMN * count + 2] = boxesData[4 * i + 2];
                outputs[PrePostProcessor.OUTPUT_COLUMN * count + 3] = boxesData[4 * i + 3];
                outputs[PrePostProcessor.OUTPUT_COLUMN * count + 4] = scoresData[i];
                outputs[PrePostProcessor.OUTPUT_COLUMN * count + 5] = labelsData[i] - 1;
                count++;

                System.out.println("Test");
            }


            float imgScaleX = (float) bitmap.getWidth() / PrePostProcessor.INPUT_WIDTH;
            float imgScaleY = (float) bitmap.getHeight() / PrePostProcessor.INPUT_HEIGHT;
            float ivScaleX = (float) mResultView.getWidth() / bitmap.getWidth();
            float ivScaleY = (float) mResultView.getHeight() / bitmap.getHeight();

            final ArrayList<Result> results = PrePostProcessor.outputsToPredictions(count, outputs, imgScaleX, imgScaleY, ivScaleX, ivScaleY, 0, 0);

            final int n_label = results.size();
            for (int i = 0; i<n_label; i++)
            {

                float boxSizeRatio = getObjectSizeRatio(results.get(i),0);
                if (boxSizeRatio > 0.17 && withinBox(results.get(i))) //If result is greater than 1/6 of total screen
                {
                    v.vibrate(VibrationEffect.createOneShot((int)(1000 * boxSizeRatio), (int)(255*boxSizeRatio)));
                    speak(getDirection(results.get(i)));
                }


            }
            if (results.stream().noneMatch(result -> result.classIndex == 0)) mTTS.stop(); //Stop speech if no more person detected.


            System.out.println(n_label);
            return new AnalysisResult(results);
        }
        return null;
    }

    /**
     *
     * Determines the ratio of the size of the detected object to the size of the device screen. Used to approximate how close an object is to the user.
     * @param result The object prediction who's size we want to evaluate
     * @param classToMatch The class of the object we want to test against. Will be obsolete once code is changed to only recognize useful classes.
     * @return the ratio of the object size to the screen size. Will be a float value between 0 and 1. Returns -1 if something goes wrong.
     */
    protected float getObjectSizeRatio(Result result, int classToMatch) {

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;
        int screenWidth = displayMetrics.widthPixels;

        int height = result.rect.height();
        int width = result.rect.width();

        // calculate the box area
        float totalArea = height * width;
        System.out.println("Total Area: " + totalArea);

        // calculate the screen area
        float screenSize = (float) screenHeight * screenWidth;

        if (result.classIndex == classToMatch) {
            return totalArea / screenSize;
        }
        return -1f;
    }

    private boolean withinBox(Result result){
        // check if the object is within the box
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;
        int screenWidth = displayMetrics.widthPixels;

        // set up the virtual box
        Utilities helper = new Utilities();
        Rect VB = helper.setupVirtualBox(screenHeight,screenWidth);

        // getting the center points of the result rect
        int resultCenterX = result.rect.centerX();
        int resultCenterY = result.rect.centerY();

        return VB.contains(resultCenterX,resultCenterY);
    }

    private int getDirection(Result result){
        // set up the virtual box
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;
        int screenWidth = displayMetrics.widthPixels;
        Utilities helper = new Utilities();
        Rect VB = helper.setupVirtualBox(screenHeight,screenWidth);
        int boxCenterX = VB.centerX();

        // getting the center points of the result rect
        int resultCenterX = result.rect.centerX();

        // set up middle section width
        int middleLeft = boxCenterX - 60;
        int middleRight = boxCenterX + 60;

        // check if the center is on the left side or right side of the box or in the middle
        // 0: middle  1: left  2: right
        if (resultCenterX >= middleLeft && resultCenterX <= middleRight){
            return 0;
        }
        else if (resultCenterX < middleLeft) return 1;
        else return 2;
    }

    private void speak(int direction) {
        //String text = "Person";
        CharSequence text = "";
        // right in front
        if (direction == 0) text = "Stop, person ahead!";

        // on left
        else if (direction == 1) text = "person on left!";

        // on right
        else text = "person on right!";

        float pitch = 1f;
        if (pitch < 0.1) pitch = 0.1f;
        float speed = 1f;
        if (speed < 0.1) speed = 0.1f;

        mTTS.setPitch(pitch);
        mTTS.setSpeechRate(speed);
        if (!mTTS.isSpeaking()) mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);

    }

}
