# CandyCrop
[![](https://jitpack.io/v/infinity-innovative-projects/candycrop.svg)](https://jitpack.io/#infinity-innovative-projects/candycrop)

A simple to use, customizable and perfomant image cropping library for Android written in Kotlin.

## Usage

### Step 1. Add the JitPack repository to your build file

##### Gradle:
Add it in your root build.gradle at the end of repositories:

```
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```
##### Maven:
```
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
```

### Step 2. Add the dependency
##### Gradle:

```
dependencies {
  implementation 'com.github.infinity-innovative-projects:candycrop:v1.0.0'
}
```
##### Maven:
```
<dependency>
  <groupId>com.github.infinity-innovative-projects</groupId>
  <artifactId>candycrop</artifactId>
  <version>v1.0.0</version>
</dependency>
```
### Step 3. Add permissions to your manifest
```
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

### Step 4a. Using as an Activity
Add ```CandyCropActivity``` into your AndroidManifest.xml
 ```
<activity android:name="com.workwithinfinity.android.candycrop.CandyCropActivity"/>
```
Use the Activity with the ```CandyCrop.Builder```
```
//For activity
CandyCrop.Builder.activity(sourceUri)
  .setResultUri(destinationUri) //Set the uri where the result should be saved
  .start(requireContext())

//For fragment
CandyCrop.Builder.activity(sourceUri)
  .setResultUri(destinationUri) //Set the uri where the result should be saved
  .start(requireContext(),this)
```
Handle the result in the ```onActivityResult``` callback
```
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
  when {
    (requestCode == CandyCrop.CANDYCROP_ACTIVITY_REQUEST) -> {
      if(resultCode == RESULT_OK) {
        val result = data?.getParcelableExtra<CandyCrop.CandyCropActivityResult>(CandyCrop.CANDYCROP_RESULT_EXTRA)
        //do your stuff here
      }      
  }
}
```

### Step 4b. Using as an View
1. Add ```CandyCropView``` into your layout xml
```
<com.workwithinfinity.android.candycrop.CandyCropView
  android:layout_width="match_parent"
  android:layout_height="match_parent"
  android:id="@+id/candyCropView" />
```
2. Set ```CandyCropView.OnLoadUriImageCompleteListener``` for the loading result (optional)
```
candyCropView.setOnLoadUriImageCompleteListener(object : CandyCropView.OnLoadUriImageCompleteListener {
  override fun onLoadUriImageComplete(result: Bitmap, uri: Uri) {
    //Do stuff here (like hiding the progress bar)
  }
})
```
3. Load the image into the view
```
candyCropView.setImageUriAsync(uri) //recommended
//or
candyCropView.setBitmap(bitmap)
```

4. Set ```CandyCropView.OnCropCompleteListener``` to handle the cropping result
```
cropView.setOnCropCompleteListener(object : CandyCropView.OnCropCompleteListener {
  override fun onCropComplete(result: CandyCropView.CropResult) {
    val croppedBitmap = result.croppedBitmap //the cropped bitmap
    val croppedUri = result.croppedUri //The uri of the cropped bitmap. Null if setResultUri was not used
    //Do stuff with the cropped image here
  }
}) 
```
5. Start the cropping process
```
candyCropView.getCroppedBitmapAsync()

```

## Customization

### Customize the Activity
Use the Builder to customize the activity
```
CandyCrop.Builder.activity(uri)
  .setResultUri(resultUri) //Sets the uri where the result will be saved
  .setResultSize(1024,1024) //Sets the width and height of the result bitmap
  .setResultFormat(Bitmap.CompressFormat.JPEG) //Set the format of the saved image
  .setResultQuality(85) //Sets the compression quality of the saved image (ignored for PNG)
  .setLabelText("Scale and Move") //Sets the text displayed on top
  .setButtonVisibility(positive=true,negative=true) //Show or hide the positive and negative button in the activity
  .setPositiveText("Crop it!") //Sets the label of the positive button
  .setNegativeText("Cancel") //Sets the label of the negative button
  .setUseToolbar(false) //Sets the visibility of the toolbar
  .setBackgroundColor(Color.BLACK) //Sets the background color of the view
  .setOverlayColor(Color.argb(150,0,0,0)) //Sets the color of the overlay
  .setCropRatio(1,1) //Sets the aspect ratio of the cropping section
  .setCropWindowSize(0.9f) //Sets the size of the cropping section. 1=Full screen, 0.5=Half screen
  .setDrawRect(false) //Sets if the border of the cropping section is visible
  .start(this)
```
### Customize the View
Some properties can be set in the xml layout
```
<com.workwithinfinity.android.candycrop.CandyCropView
  android:layout_width="match_parent"
  android:layout_height="match_parent"
  android:id="@+id/candyCropView" 
  app:bg_color="@android:color/white" //Sets the background color of the view
  app:overlay_color="@color/black_transparent" //Sets the color of the overlay
  app:crop_size="0.8" //Sets the size of the cropping section. 1=full view, 0.5=Half view
  app:crop_aspect_ratio_x="1" //Sets the aspect ratio of the cropping section for the x dimension
  app:crop_aspect_ratio_y="1" //Sets the aspect ratio of the cropping section for the y dimension
  app:draw_rect="true" //Sets if the border of the cropping section is visible
  />
```
Or use the setters of the ```CandyCropView``` to customize the view
```
with(candyCropView) {
  setResultUri(resultUri) //Sets the uri where the result will be saved
  setResultSize(width,height) //Sets the width and height of the result bitmap
  setFormat(Bitmap.CompressFormat.JPEG) //Set the format of the saved image
  setQuality(85) //Sets the compression quality of the saved image (ignored for PNG)
  setBgColor(Color.BLACK) //Sets the background color of the view
  setOverlayColor(Color.argb(150,0,0,0)) //Sets the color of the overlay
  setAspectRatio(1,1) //Sets the aspect ratio of the cropping section
  setCropSize(mOptions.cropSize) //Sets the size of the cropping section. 1=Full screen, 0.5=Half screen
  setDrawRect(mOptions.drawRect) //Sets if the border of the cropping section is visible
  setInitialRotation(mOptions.rotation) //Sets the initial rotation of the loaded image. Must be 0, 90, 180 or 270
}
```
## Changelog

## License
Copyright 2019, Yannick Bertel, Infinity - Innovative projects GmbH
Licensed under the Gnu General Public License, Version 3.0 you may not use this work except in compliance with the License. You may obtain a copy of the License in the LICENSE file, or at:

https://www.gnu.org/licenses/gpl-3.0.de.html
