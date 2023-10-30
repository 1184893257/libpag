package libpag.pagviewer

import android.content.Context
import android.provider.Settings

public object AnimationUtil {

    public fun getAnimationScale(context: Context): Float {
        return Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f
        )
    }
}
