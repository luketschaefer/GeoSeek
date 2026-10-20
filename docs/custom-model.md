# Custom detection model (AirPods benchmark)

ML Kit's stock image labeler has no label for AirPods, earbuds, or headphones, so it can never
identify them. GeoSeek therefore runs a second, custom image classifier on each locked-on crop,
alongside the stock labeler. AirPods are the benchmark object for that model.

The app looks for two files:

| File | What it is |
| --- | --- |
| `app/src/main/assets/custom_model.tflite` | TensorFlow Lite image classifier, input `[1, H, W, 3]` |
| `app/src/main/assets/custom_labels.txt` | One class per line, in output order (`0 AirPods` or `AirPods`) |

If they're missing, the app runs as before with the stock labeler only.

## Training it with Teachable Machine

1. Open <https://teachablemachine.withgoogle.com/train/image> and create three classes. The
   names matter, because they're matched against the aliases in `ObjectLabelMatcher`:
   - `AirPods`: the earbuds on their own, one or both, held or on a surface.
   - `AirPods case`: the case closed, and open with the buds in it.
   - `Background`: anything else, such as desks, hands, phones, other earbuds, and empty
     tables. This class keeps the model from calling everything AirPods. It is not in the
     aliases, so it never counts as a find.
2. Record at least 200 images per class with the webcam, or upload phone photos. Vary the
   angle, distance, lighting and background. Fill most of the frame with the object, because
   the app classifies a crop around the outlined object rather than the whole scene.
3. Train, then go to **Export model**, choose **TensorFlow Lite** and **Floating point**, and
   download.
4. Unzip the download. Copy `model_unquant.tflite` to `app/src/main/assets/custom_model.tflite`
   and `labels.txt` to `app/src/main/assets/custom_labels.txt`.
5. Rebuild and install. Point the camera at the AirPods and hold for about two seconds.

A quantized model also works: the classifier detects uint8 input and output and handles both.

## Adding more objects later

Add a class to the model, then add a `GameObject` in `ObjectList.kt` and give it an alias entry
in `ObjectLabelMatcher` that matches the class name.
