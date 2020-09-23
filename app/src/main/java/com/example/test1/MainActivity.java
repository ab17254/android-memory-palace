package com.example.test1;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.TransformableNode;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Main class initializes the and runs the activity and fragments
 */

public class MainActivity extends AppCompatActivity {


    private static final int PERMISSION_REQUEST = 0;
    private static final int RESULT_LOAD_IMAGE = 1;

    MyArFragment arFragment;
    int selected = 1;
    TextView txt;
    String inputString;
    String picturePath;
    Button load;
    ImageButton addImage, btn;
    boolean isImage = false;
    DBInterface storageManager;
    private ModelRenderable text_anchorRenderable, image_anchorRenderable;
    private PointerDrawable pointer = new PointerDrawable();
    private boolean isTracking;
    private boolean isHitting;
    private Anchor cloudAnchor;
    private anchorState appAnchorState = anchorState.NONE;

    /**
     * Checks if camera permissions have been granted
     * @param activity it checks
     * @return boolean
     */
    public static boolean checkCameraPermissions(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Permissions granted");
            } else {
                showToast("Permissions not granted");
                finish();
            }
        }
    }

    /**
     * Checks if the app has been granted read permissions
     * @return boolean
     */
    public boolean checkReadPermissions() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            Log.v("myApp", "Permission is granted");
            return true;
        } else {

            Log.v("myApp", "Permission is revoked");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            return false;
        }
    }

    /**
     * Checks if the app has been granted write permissions
     * @return boolean
     */
    public boolean checkWritePermissions() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            Log.v("myApp", "Permission is granted");
            return true;
        } else {

            Log.v("myApp", "Permission is revoked");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            return false;
        }
    }

    /**
     * Creates an new AR session from the activity
     * @param activity which the session gets created in
     * @param installRequested ensures arCore is installed
     * @return session
     * @throws UnavailableException if ARCore is not installed
     */
    public static Session createArSession(Activity activity, boolean installRequested) throws UnavailableException {
        Session session = null;
        if (checkCameraPermissions(activity)) {
            switch (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
                case INSTALL_REQUESTED:
                    return null;
                case INSTALLED:
                    break;
            }
            session = new Session(activity);
            Config config = new Config(session);
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            session.configure(config);
        }
        return session;
    }

    /**
     * Method whihc is ran on the creation of the app
     * Initialises the UI, and ARFragment
     * Creates an new session
     * @param savedInstanceState
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            Session session = new Session(this);
            session.getAllAnchors();
        } catch (UnavailableArcoreNotInstalledException | UnavailableApkTooOldException | UnavailableSdkTooOldException | UnavailableDeviceNotCompatibleException e) {
            e.printStackTrace();
        }

        try {
            createArSession(this, true);
        } catch (UnavailableException e) {
            e.printStackTrace();
            Log.v("ArApp", "Session not started");
        }

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
        }
        checkReadPermissions();
        checkWritePermissions();

        /*
          Finds the AR fragment
         */
        arFragment = (MyArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        if (arFragment != null) {
            arFragment.getPlaneDiscoveryController().hide();
            arFragment.getPlaneDiscoveryController().setInstructionView(null);
            arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdate);
        }

        /*
          Adds an tap listener to the fragment. If the user taps the screen an object is added
         */
        arFragment.setOnTapArPlaneListener((HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
            if (inputString != null | getPicturePath() != null) {
                setupModel();
                Anchor newAnchor = arFragment.getArSceneView().getSession().hostCloudAnchor(hitResult.createAnchor());
                setCloudAnchor(newAnchor);
                appAnchorState = anchorState.LAUNCHING;
                showToast("Now Hosting Anchor...");

                /*
                Swithces between Image and text objects depending on which as been selected
                 */
                if (selected == 2) {
                    try {
                        textureObject(picturePath);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    createModel(addNodeToScene(arFragment, cloudAnchor, image_anchorRenderable), selected, inputString);
                }
                if (selected == 1) {
                    createModel(addNodeToScene(arFragment, cloudAnchor, text_anchorRenderable), selected, inputString);
                }
            }

        });

        storageManager = new DBInterface(this);

        txt = findViewById(R.id.editText);
        btn = findViewById(R.id.addText);
        load = findViewById(R.id.loadButton);
        addImage = findViewById(R.id.addImage);

        txt.setVisibility(View.INVISIBLE);

        actionListeners();

    }

    /**
     * Method which adds action listeners to each of the buttons. Called on onCreate()
     */
    public void actionListeners() {
        btn.setOnClickListener(new View.OnClickListener() {
            boolean visible;
            @Override
            public void onClick(View v) {
                visible = !visible;
                txt.setVisibility(visible ? View.VISIBLE : View.GONE);
                inputString = txt.getText().toString();
                selected = 1;
            }
        });
        addImage.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, RESULT_LOAD_IMAGE);
            selected = 2;
        });
        load.setOnClickListener(v -> {
            setCloudAnchor(null);
            if (cloudAnchor == null) {
                showToast("Cloud anchor cleared");
            }
            onLoadPressed onLoad = new onLoadPressed();
            onLoad.setListener(MainActivity.this::onLoadSubmit);
            onLoad.show(getSupportFragmentManager(), "Load");
        });
    }

    /**
     * Updates every frame when tracking
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void onUpdate(FrameTime frameTime) {
        updateAnchorState();

        boolean trackingChanged = updateTracking();
        View contentView = findViewById(android.R.id.content);
        if (trackingChanged) {
            if (isTracking) {
                contentView.getOverlay().add(pointer);
            } else {
                contentView.getOverlay().remove(pointer);
            }
            contentView.invalidate();
        }
        if (isTracking) {
            boolean hitTestChanged = updateHit();
            if (hitTestChanged) {
                pointer.setEnabled(isHitting);
                contentView.invalidate();
            }
        }
        try {
            textureObject(getPicturePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Log.v("state", String.valueOf(appAnchorState));
        picturePath = getPicturePath();
        Log.v("picPath", picturePath + "");
    }

    /**
     * Creates an list of points which have been hit
     * @return boolean
     */
    private boolean updateHit() {

        Frame frame = arFragment.getArSceneView().getArFrame();
        android.graphics.Point pt = findCenter();
        List<HitResult> hits;
        boolean wasHitting = isHitting;
        if (frame != null) {
            hits = frame.hitTest(pt.x, pt.y);
            for (HitResult hit : hits) {
                Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                    isHitting = true;
                    break;
                }
            }
        }
        return wasHitting != isHitting;
    }

    /**
     * Updates the tracking to current frame
     * @return boolean
     */
    private boolean updateTracking() {
        Frame currentFrame = arFragment.getArSceneView().getArFrame();
        boolean wasTracking = isTracking;
        isTracking = currentFrame != null && currentFrame.getCamera().getTrackingState() == TrackingState.TRACKING;
        return isTracking != wasTracking;
    }

    /**
     * Finds the center of the screen
     * @return Point of the center of the screen
     */
    private android.graphics.Point findCenter() {
        View content = findViewById(android.R.id.content);
        return new android.graphics.Point(content.getWidth() / 2, content.getHeight() / 2);
    }

    /**
     * Method which allows the user to open the device gallery on their device
     * Saves the path of the selected image
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE) {
            if (resultCode == RESULT_OK) {
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                assert selectedImage != null;
                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                assert cursor != null;
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                picturePath = cursor.getString(columnIndex);
//                showToast(picturePath);
            }
        }
    }

    /**
     *Getter for the picture path
     */
    public String getPicturePath() {
        return picturePath;
    }

    /**
     * Method which creates the renderable models and attaches them to the anchor
     * @param anchorNode Node to attach objects too
     * @param selected Integer identifying if its an text or image object
     * @param inputString String which the user supplied as an note
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void createModel(AnchorNode anchorNode, int selected, String inputString) {

        if (selected == 1) {
            TransformableNode textAnchor = new TransformableNode(arFragment.getTransformationSystem());
            textAnchor.setParent(anchorNode);
            textAnchor.setRenderable(text_anchorRenderable);
            textAnchor.select();
            addName(anchorNode, textAnchor, inputString, "text");
        }

        if (selected == 2) {
            TransformableNode textAnchor = new TransformableNode(arFragment.getTransformationSystem());
            textAnchor.setParent(anchorNode);
            textAnchor.setRenderable(image_anchorRenderable);
            textAnchor.select();
            addName(anchorNode, textAnchor, inputString, "image");
        }
    }

    /**
     * Method which adds the renderable to the node to be attached to the anchor
     * @param fragment ARFragment
     * @param anchor anchor the node is attached to
     * @param renderable model to be set ot the node
     * @return anchorNode
     */
    private AnchorNode addNodeToScene(com.google.ar.sceneform.ux.ArFragment fragment, Anchor anchor, ModelRenderable renderable) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode node = new TransformableNode(fragment.getTransformationSystem());
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        fragment.getArSceneView().getScene().addChild(anchorNode);
        node.select();

        return anchorNode;
    }

    /**
     * Builds the string text in given layout and attaches to the renderable
     * @param anchorNode which object get set to
     * @param model text anchor renderable
     * @param name user given text
     * @param type either text or image refers to the position of the text relative to the anchor
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void addName(AnchorNode anchorNode, TransformableNode model, String name, String type) {
        ViewRenderable.builder()
                .setView(this, R.layout.model_name)
                .build()
                .thenAccept(viewRenderable -> {
                    TransformableNode nameView = new TransformableNode(arFragment.getTransformationSystem());
                    if (type.equals("text")) {
                        nameView.setLocalPosition(new Vector3(0f, model.getLocalPosition().y + 0.2f, 0));
                    } /* Image type is placed higher in the world to avoid it overlapping with the image object*/
                    if (type.equals("image")) {
                        nameView.setLocalPosition(new Vector3(0f, model.getLocalPosition().y + 0.6f, 0));
                    }
                    nameView.setParent(anchorNode);
                    nameView.setRenderable(viewRenderable);
                    nameView.select();

                    TextView txt_name = (TextView) viewRenderable.getView();
                    txt_name.setText(name);

                    txt_name.setOnClickListener(v -> anchorNode.setParent(null));
                });
    }

    /**
     *Builds the text renderable with the
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setupModel() {
        ModelRenderable.builder()
                .setSource(this, R.raw.text_anchor)
                .build().thenAccept(renderable -> text_anchorRenderable = renderable)
                .exceptionally(throwable -> {
                    showToast( "Unable to load textAnchor Model");
                    return null;
                });
    }

    /**
     * Takes the user selected image, converts into bitmap
     * Creates and textures an sqaure with the bitmap
     * @param path String of the image path selected
     */
    private void textureObject(String path) throws IllegalArgumentException, FileNotFoundException {
        Bitmap userImageOut = null;
        Bitmap tempImage = BitmapFactory.decodeFile(path);
        if (tempImage != null) {
//          Only compressed images which are too large
            if (tempImage.getWidth() > 4096 | tempImage.getHeight() > 4096) {

                if (path != null) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(path, options);

//                  Must be a power of 2
                    final int requiredSize = 512;

                    int scale = 1;
                    while (options.outWidth / scale / 2 >= requiredSize && options.outHeight / scale / 2 >= requiredSize) {
                        scale *= 2;
                    }
                    BitmapFactory.Options options1 = new BitmapFactory.Options();
                    options1.inSampleSize = scale;
                    Bitmap userImage = BitmapFactory.decodeFile(path, options1);

                    if (userImage != null) {
//                      When compressed image gets rotates, so this rotates back
                        float degrees = 90;
                        Matrix matrix = new Matrix();
                        matrix.setRotate(degrees);
                        userImageOut = Bitmap.createBitmap(userImage, 0, 0, userImage.getWidth(), userImage.getHeight(), matrix, true);
                    }
                }
            } else {
                userImageOut = tempImage;
            }
        }
//      Creates an cube, and textures it
        if (userImageOut != null) {
            CompletableFuture<Texture> userTexture = Texture.builder().setSource(userImageOut)
                    .build();
            final Context finalContext = this;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                userTexture.handle((texture, throwable) -> {
                    MaterialFactory.makeOpaqueWithTexture(finalContext, texture).handle((material, throwable1) -> {
                        image_anchorRenderable = ShapeFactory.makeCube(new Vector3(1, 1, 0), new Vector3(0, 0, 0), material);
                        return null;
                    });
                    return null;
                });
            }
        }
    }

    /**
     * Creates an new anchor and sets it as the current
     * @param newAnchor to be set as the cloud anchor
     */
    public void setCloudAnchor(Anchor newAnchor) {
        cloudAnchor = newAnchor;
        appAnchorState = anchorState.NONE;
    }


    /**
     * Updates that state of the anchor when an new object is hosted or loaded back
     */
    private synchronized void updateAnchorState() {
        if (appAnchorState != anchorState.LAUNCHING && appAnchorState != anchorState.LOADING) {
            return;
        }
        Anchor.CloudAnchorState cloudState = cloudAnchor.getCloudAnchorState();
        if (appAnchorState == anchorState.LAUNCHING) {
            if (cloudState.isError()) {
                showToast("Error Hosting anchor " + cloudState);
            } else if (cloudState == Anchor.CloudAnchorState.SUCCESS) {
                storageManager.nextShort((shortCode -> {
                    if (shortCode == null) {
                        showToast("Could not get short Code");
                        return;
                    }
//                  Stores data into the database
                    storageManager.storeUsingShortCode(shortCode, cleanPath(), inputString, cloudAnchor.getCloudAnchorId());
                    showToast("Anchor hosted, Short Code: " + shortCode);
                }));
                appAnchorState = anchorState.LAUNCHED;
            }
        } else if (appAnchorState == anchorState.LOADING) {
            if (cloudState.isError()) {
                showToast("Error Resolving " + cloudState);
                appAnchorState = anchorState.NONE;
            } else if (cloudState == Anchor.CloudAnchorState.SUCCESS) {
                showToast("Anchor Resolved");
                appAnchorState = anchorState.LOADED;
            }
        }
    }

    /**
     * Cleans the picture path string
     * Firebase does not allow for '.' to be placed in the DB. Replaces this with an '#' value
     * @return string of cleaned path
     */
    String cleanPath() {
        StringBuilder string = new StringBuilder("");
        if (getPicturePath() != null) {
            string = new StringBuilder(getPicturePath());
            int stringLen = getPicturePath().length();
            if (stringLen != 0) {
                string.setCharAt(stringLen - 4, '#');
            }
            return String.valueOf(string);
        }
        return String.valueOf(string);
    }

    /**
     * Method for retrieving the data from the database
     * User enters an code and all values associated with that value are retrieved and set.
     */
    private void onLoadSubmit(String value) {
        try {
            int shortCode = Integer.parseInt(value);
            storageManager.anchorID(shortCode, (cloudAnchorId -> {
                List<String> result = Arrays.asList(cloudAnchorId.split("\\s*,\\s*"));
                Anchor resolvedAnchor = arFragment.getArSceneView().getSession().resolveCloudAnchor(result.get(0));
                String path = result.get(1);
                StringBuilder resolvedPath = new StringBuilder(path);
                int resolvedPathLen = resolvedPath.length();

                if (resolvedPathLen > 5) {
                    resolvedPath.setCharAt(resolvedPathLen - 4, '.');
                    isImage = true;
                }
                String userText = result.get(2);

                Log.v("CSV", String.valueOf(result));
                Log.v("CSV", String.valueOf(resolvedAnchor));
                Log.v("CSV", path);
                Log.v("CSV", String.valueOf(resolvedPath));
                Log.v("CSV", "userText " + userText);

                if (!userText.equals("null")) {
                    inputString = userText;
                }

                picturePath = String.valueOf(resolvedPath);

                showToast(cloudAnchorId);
                setCloudAnchor(resolvedAnchor);
                if (picturePath != null) {
                    try {
                        textureObject(String.valueOf(resolvedPath));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                if (isImage) {
                    selected = 2;
                }
                if (!isImage) {
                    selected = 1;
                }
                showToast("Resolving");
                appAnchorState = anchorState.LOADED;
            }));
        } catch (ArrayIndexOutOfBoundsException e) {
            showToast("Invalid Code");
            throw e;
        }
    }

    /**
     * Testing method used to generate popups on screen of strings of text
     * @param text String
     */
    private void showToast(String text) {
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    /**
     * Defines the different state of an cloud anchor
     */
    private enum anchorState {
        NONE, LAUNCHING, LAUNCHED, LOADING, LOADED
    }
}