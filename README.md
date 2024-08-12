<h1 style="text-align: center;">
    Development of a Mobile Interface for Processing Ophthalmic Images
</h1>

&nbsp

<img src="screenshots/screenshot_01.png" width="36" height="36" alt="screenshot">

###

<h3 style="text-align: center;">Main Screen</h3>

<div style="display: flex; align-items: center; justify-content: space-between;">
    <img src="screenshots/screenshot_02.jpeg" alt="Main Screen" style="width: 40%; margin-right: 20px;">
    <p style="text-align: justify; width: 60%;">
        Main screen of the RetinaCapture app with buttons for selecting the diagnostic mode: Auto-diagnostics (A) and Manual-diagnostics (B). By pressing one of these buttons, we navigate to the Bluetooth device selection screen.
    </p>
</div>

###

<h3 style="text-align: center;">Bluetooth Device Selection Screen</h3>

<div style="display: flex; align-items: center; justify-content: space-between;">
    <div style="width: 60%; text-align: justify; margin-right: 20px;">
        <p>- <strong>The text element "Found Devices"</strong> (A) displays a list of found devices.</p>
        <p>- <strong>The button "Scan Devices"</strong> (B) initiates the scanning process to detect available Bluetooth devices.</p>
        <p>- <strong>The device list</strong> (C) displays the names and addresses of found devices, allowing the user to select a device for connection.</p>
    </div>
    <img src="screenshots/screenshot_03.jpeg" alt="Bluetooth Device Selection Screen" style="width: 40%;">
</div>

###

<h3 style="text-align: center;">LED Check Screen</h3>

<div style="display: flex; align-items: center; justify-content: space-between;">
    <img src="screenshots/screenshot_04.jpeg" alt="LED Control" style="width: 40%; margin-right: 20px;">
    <div style="width: 60%; text-align: justify;">
        <p>- <strong>Buttons "ON" and "OFF"</strong> (А) for turning the LEDs on and off, respectively.</p>
        <p>- <strong>Slider "R"</strong> for checking the intensity of the red color.</p>
        <p>- <strong>Slider "G"</strong> for checking the intensity of the green color.</p>
        <p>- <strong>Slider "B"</strong> for checking the intensity of the blue color.</p>
        <p>- <strong>Slider "W"</strong> for checking the intensity of the white color.</p>
        <p>- <strong>Button "Disconnect"</strong> (С) for disconnecting from the Bluetooth device.</p>
        <p>- <strong>Button "Next"</strong> (D) for proceeding to the next diagnostic screen.</p>
    </div>
</div>


###

<h3 style="text-align: center;">Quick Diagnostics Screen</h3>

<div style="display: flex; align-items: center; justify-content: space-between;">
    <div style="width: 60%; text-align: justify; margin-right: 20px;">
        <p>- <strong>The circular image preview</strong> (A) is located below the logo and shows in real-time what the camera is capturing.</p>
        <p>- <strong>A card with sliders</strong> (B) is located below the preview and allows adjustment of image parameters such as contrast, brightness, saturation, white balance, and grayscale.</p>
        <p>- <strong>The photo capture button</strong> (C) is located at the bottom of the screen for capturing the image.</p>
        <p>- <strong>The Progress Bar</strong> (D) is displayed during the image capture to indicate progress.</p>
    </div>
    <img src="screenshots/screenshot_05.jpeg" alt="Camera Preview" style="width: 40%;">
</div>

###

<h3 style="text-align: center;">Image Results Screen</h3>

<div style="display: flex; align-items: center; justify-content: space-between;">
    <img src="screenshots/screenshot_06.jpeg" alt="Image Previews" style="width: 40%; margin-right: 20px;">
    <div style="width: 60%; text-align: justify;">
        <p>- <strong>Four image previews</strong> (A) display the captured images under the red, green, blue, and white LEDs.</p>
        <p>- <strong>Image processing buttons</strong> (B) allow the user to perform matrix subtraction between different image channels for analysis:</p>
        <ul>
            <li>"R-G" to subtract the red channel from the green channel.</li>
            <li>"R-B" to subtract the red channel from the blue channel.</li>
            <li>"B-G" to subtract the blue channel from the green channel.</li>
        </ul>
        <p>- <strong>The resulting image</strong> (C) displays the outcome of the image processing after pressing one of the processing buttons.</p>
    </div>
</div>


###

<h3 style="text-align: center;">Save to Gallery</h3>

<div style="display: flex; align-items: center; justify-content: space-between;">
    <div style="width: 60%; text-align: justify; margin-right: 20px;">
        <p>This screen allows users to view captured images in fullscreen mode and save them for further use or analysis. After pressing the "Save" button, the image is stored in the "DCIM/RC_AutoDiagnosis" folder in the device's gallery.</p>
    </div>
    <img src="screenshots/screenshot_07.jpeg" alt="Fullscreen Image View" style="width: 40%;">
</div>


###

<h3 style="text-align: center;">Manual Diagnostics Screen</h3>

<div style="display: flex; align-items: center; justify-content: space-between;">
    <div style="width: 40%; display: flex; flex-direction: column; gap: 10px;">
        <img src="screenshots/screenshot_08.jpeg" alt="Circular Image Preview" style="width: 100%;">
        <img src="screenshots/screenshot_09.jpeg" alt="Camera Footage" style="width: 100%;">
    </div>
    <div style="width: 60%; text-align: justify;">
        <p>- <strong>The circular image preview</strong> (A) shows real-time footage from the camera for framing adjustments.</p>
        <p>- <strong>The "Filter/RGBW" mode switch</strong> (B) allows toggling between filter modes and RGBW modes.</p>
        <p>- <strong>The filter sliders card</strong> (C) for adjusting contrast, brightness, saturation, white balance, and grayscale.</p>
        <p>- <strong>The RGBW sliders card</strong> (D) for adjusting the intensity of RGBW LEDs.</p>
        <p>- <strong>The "Take Photo" button</strong> (E) for capturing the image.</p>
        <p>- <strong>The image preview</strong> (F) allows viewing the image in fullscreen mode with an option to save it to the gallery.</p>
    </div>
</div>


