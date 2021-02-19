package org.nypl.drm.core

import java.io.File

class DRMProtectedFile(file: File, val adobeRightsFile: File?)
    : org.readium.r2.shared.util.File(path = file.path)