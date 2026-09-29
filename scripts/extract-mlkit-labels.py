#!/usr/bin/env python3
"""Extract the ML Kit base image-labeling label map from the published AAR.

Usage: scripts/extract-mlkit-labels.py 17.0.9 > app/src/test/resources/mlkit-base-labels-17.0.9.txt

The bundled model (.tflite) carries its label file inside a zip appended as TFLite metadata.
BundledCatalogTest validates every catalog label against this list. When bumping
com.google.mlkit:image-labeling, re-extract, rename the fixture and update the test.
"""
import io
import sys
import urllib.request
import zipfile

version = sys.argv[1] if len(sys.argv) > 1 else "17.0.9"
url = f"https://dl.google.com/android/maven2/com/google/mlkit/image-labeling/{version}/image-labeling-{version}.aar"
aar = zipfile.ZipFile(io.BytesIO(urllib.request.urlopen(url).read()))
model_name = next(n for n in aar.namelist() if n.startswith("assets/mlkit_label_default_model/"))
model = aar.read(model_name)
metadata = zipfile.ZipFile(io.BytesIO(model[model.find(b"PK\x03\x04"):]))
labels = metadata.read("0-labels-en.txt").decode().splitlines()
print("\n".join(labels))
print(f"{len(labels)} labels from {version}", file=sys.stderr)
