# Getting Started

## Prerequisits

In order to run this application you will need an compatiable android device with google arcore installed.
To be comptabale the device must be atleast running atleast android version 9 (Android Pie)

## Installing

To install this app download the APK file from the BuildAPK folder found in the git repositry and copy it onto the root directory of your android device. 
The APK is titled 'app-debug.apk'. 
Then open your phones file manager, locate the APK file, and follow the install process. 
The app will be installed under the name Memory Palace.

## Versioning

This project is versioned using semantic versioning.

The version submitted at the MVP was v3.0.1

The most recent version is v5.5.2

# Technical Documentation

## Classes

### MainActivity
#### checkCameraPermissions()
Method to check to see if the camera permissions have been granted. This method is only a check and returns either true or false accordingly
#### checkReadPermissions()
Method to check if read permissions have been given. Returns a boolean accordingly. Read permission needed so that the user can upload images
#### checkWritePermissions()
Method to check if read permissions have been given. At no point does the app need to write to the device. However due to updates in android 10s privacy settings these must also be requested.
Method is very similar to the read one.
#### createArSession()
This method creates an Session requiring a config as supplied from the MyArFragment class. Will only start an session if checkCameraPermissions returns true.
This method is called on onCreate().
#### onCreate()
This method is called when the application starts. This method first applies the UI layout from the activity_main xml file using
```
setContentView(R.layout.activity_main);
```
It then begins to start the session using createArSession() and checks all of the permissions are given.
Once it has done this it will initialize the AR fragment and add the onUpdate() method to it as an update listener using:
```
arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdate);
```
Then an AR plane listener is set on the ar fragment. This checks to see if the user has tapped on the plane. If they have then it will attempt to
create an new cloud anchor at that point and either add the image object where tapped or an text object (depending what is selected)
The snippet for adding images objects is shown below. This is the same for text apart from there is no need to texture the object and it uses an different renderable object
```
textureObject(picturePath)
createModel(addNodeToScene(arFragment, cloudAnchor, image_anchorRenderable), selected, inputString);
```
Finally this method initialises the storage manager object based on the DBInterface class as well as all of the buttons.
The actionListeners method is also called.

#### actionListeners()
This method assigns action listeners to each of the 3 buttons.
The first button is to add text this causes a text field to appear when tapped and set the selected variable to 1 indicating the user wants to add an text object to the world.
The second button is for images and opens the image gallery. This sets the selected value to 2. Indicating the user wants to add an image object
The final button is for loading objects this runs the onLoadPress class to create the layout
#### onUpdate()
This method updates the frame when the app is tracking an AR plane. This updates the hit point using updateHit() and updates the tracking using updateTracking().
The textureObject() method is also called every frame to ensure that the objects are updated if the user uploads more than one.
#### onActivityResult()
This method handles the processing of the image from the devices phone gallery. It opens the gallery and once the user has selected an image will return the file path of that image.
It then sets the path to the variable picturePath which is used to texture the image object.
#### getPicturePath()
Getter for the picturePath variable to ensure the most up to date path is used.
#### createModel()
This method creates the renderable objects to be added into the scene. It also attaches them to an TransformableNode.
This type of node was used in order to allow for the objects placed to be able to be interacted with such as being resized and rotated.
It creates the model for both text and image objects and adds them to a node.
#### addNodeToScene()
This method takes the model created created in createModel() and a sets that as the renderable for an node. The Node is then returned to be added to the scene.
The node is then added as an child of the world.
This is called along with createModel() in onCreate() as can be seen above
#### addName()
The add name method build the text object. This takes the string which the user has entered and places it inside of the model_name xml layout.
This is then built as a transformable node. The position of the text relative to the world depends if its an text or image being added.
Text for image objects appears slightly higher in the world to avoid it clashing with the image object.
```
if (type.equals("text")) {
    nameView.setLocalPosition(new Vector3(0f, model.getLocalPosition().y + 0.2f, 0));
} /* Image type is placed higher in the world to avoid it overlapping with the image object*/
if (type.equals("image")) {
    nameView.setLocalPosition(new Vector3(0f, model.getLocalPosition().y + 0.6f, 0));
}
```
An action listener is also placed on this object. This is so that is the user taps on it it will be removed from the world.
```
txt_name.setOnClickListener(v -> anchorNode.setParent(null));
```
#### setupModel()
This build the text anchor referable from the sfb file. This is ued to attach the text model to.
#### textureObject()
This is the method responsible for creating and texturing the text object. It first takes the file path and decodes that into a bitmap.
Bitmaps used must have a height and width below 4096 pixels. Therefore if the image is bigger it is first compressed.
Images which have been compressed get rotated by 90 degrees (not sure of the exact reasoning behind this).
Therefore they are then rotated back to normal using an matrix. As can be seen below. This only is called if the image is compressed
```
float degrees = 90;
Matrix matrix = new Matrix();
matrix.setRotate(degrees);
userImageOut = Bitmap.createBitmap(userImage, 0, 0, userImage.getWidth(), userImage.getHeight(), matrix, true);
```
The processed bitmap is then converted into an texture.
Next ShapeFactory produces an cube and sets that as the image renderable. This is then textured using MaterialFactory as can be seen below
```
userTexture.handle((texture, throwable) -> {
    MaterialFactory.makeOpaqueWithTexture(finalContext, texture).handle((material, throwable1) -> {
        image_anchorRenderable = ShapeFactory.makeCube(new Vector3(1, 1, 0), new Vector3(0, 0, 0), material);
        return null;
    });
    return null;
});
```
#### setCloudAnchor
This method creates an new cloud anchor and sets its state
#### updateAnchorState()
This method checks to see if the state of the cloud anchor has changed. If the anchor is able to host itself it will be added into the data base by the following.
```
storageManager.storeUsingShortCode(shortCode, cleanPath(), inputString, cloudAnchor.getCloudAnchorId());
showToast("Anchor hosted, Short Code: " + shortCode);
```
It will also display the primary key which is used to reload the data.
If the is an error hosting the anchor it will be displayed as a toast.
#### cleanPath()
Google firebase does not allow text entries to have a '.' character in them. Because of this the picturePath needs to be changed to remove this character.
This method removes it and replaces it with '#'. When the picture path is loaded again the reverse happens returning it to normal.
#### onLoadSubmit()
This is the method responsible for loading all the data from the database. Once the load button is clicked this method takes the value the user enters and retries the entry which matches it.
This is then separated by the comma and placed into a list as follows:
```
List<String> result = Arrays.asList(cloudAnchorId.split("\\s*,\\s*"));
```
Each element of the list is then assigned as needed. to replace they previous values. The object is then added as is normally added
#### showToast()
Method used during testing. When supplied with a string and called it displays the string on the screen.
This is also used to display the unique ID of the cloudAnchor

### MyArFragment
#### getSessionConfiguration()
This method configures the ARFragment. They only configuration which is needed is to disable the Sceneform instruction tool and
to enable cloud anchor support.

### PointerDrawable
#### isEnabled()
Getter method for the enabled variable
#### setEnabled()
Setter method for the enabled variable. Variable is a boolean which is true if a plane is found. This is done in MainActivity
#### draw()
Draw method extended from Drawable. This allows for shapes to be drawn on the screen. The use for this is to show a green dot on the
screen if the if an AR plane has been found. If it has not been found an X will be drawn on screen.
This is shown using the two methods shown below. Values cx and cy represent the center height and width of the display
```
            canvas.drawCircle(cx, cy, 10, paint);           
            canvas.drawText("X", cx, cy, paint);
```

### DBInterface
#### DBInterface()
Constructor for the class. The constructor initializes the Firebase app, sets the root reference(where all the data will be stored) and sets the data base online as so it can be written too and read
#### nextShort()
This method generates the short code required for the primary key of each DB entry. This is done by running an transaction.
The Transaction will set the next code to be used to the current code + 1 or if the current code is null. Will set it as the first code - 1.
If the transaction cannot be complete an error will be thrown will set the short code to null.
#### anchorID()
This is similar to the method above. Uses an transaction to get the cloudAnchor ID associated with the primary key parsed into it.
Again this has similar error checking and will thrown an exception if it cant be completed, setting the available anchor to null.
#### storesUsingShortCode()
This is the method which updates the database with all the data from the user. This uses the short code as the primary key.
It then sets the values of the CloudAnchorID, the user uploaded image path and the user uploaded string. These are all separated with a comma as in a CSV file. For easy reading
```
 rootDBref.child(PREFIX + shortCode).setValue(cloudAnchorId + "," + imagePath + "," + userText);
```
### onLoadPressed
#### inputLayout()
This method defines the layout for the input field used to reload objects. Holds an text field and two buttons inside of a linear layout.
The text field is restricted to numbers only using:
```
textField.setInputType(InputType.TYPE_CLASS_NUMBER);
```
It returns the layout so that it can be set on the press of the load button.
#### onCreateDialog()
This method applied the layout created above. Returns the value which is entered on the press of the ok button. Handles the action listeners for the Load, and Cancel button.
If load is pressed an no value is entered nothing will happen, preventing an null pointer exception when trying to load an object

## Authors

Aaron Brace

## Relevant Links

- Android Studio - https://developer.android.com/studio

- Google ArCore - https://developers.google.com/ar

- Google Sceneform tools (Beta) - https://plugins.jetbrains.com/plugin/10698-google-sceneform-tools-beta-

- Google Cloud Anchor API - https://developers.google.com/ar/develop/java/cloud-anchors/overview-android

- Firebase realtime database - https://firebase.google.com/products/realtime-database