package net.drehtuer.nextgallery

import android.os.StrictMode

/** Debug builds only: logs disk and network use on the main thread and leaks under the tag `StrictMode`. */
internal fun enableStrictMode() {
    StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
    StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
}
