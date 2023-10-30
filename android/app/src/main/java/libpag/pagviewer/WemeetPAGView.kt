package libpag.pagviewer

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.libpag.PAGComposition
import org.libpag.PAGFile
import org.libpag.PAGImage
import org.libpag.PAGLayer
import org.libpag.PAGView

public open class WemeetPAGView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    PAGView(context, attrs, defStyleAttr) {
    private val scope = MainScope()
    private var container: PAGComposition? = null
    private var fileCache = PAGFileCache(20) // 默认缓存20个PAGFile, 点赞6个pag文件
    private var imagecache = PAGImageCache(3 * 1024L * 1024L) // 默认3M图片缓存, 点赞表情图总共7*40KB=280KB

    public interface CompositionPlayer {
        public fun play(composition: PAGComposition)
    }

    public fun resetCache(maxFiles: Int, maxImageBytes: Long) {
        fileCache = PAGFileCache(maxFiles)
        imagecache = PAGImageCache(maxImageBytes)
    }

    public open fun getContainer(composition: PAGComposition): PAGComposition {
        var container = container ?: PAGComposition.Make(composition.width(), composition.height())
        this.container = container
        return container
    }

    public open fun removeChild(container: PAGComposition, layer: PAGLayer) {
        container.removeLayer(layer)
    }

    /**
     * 叠加播放一个动画 composition, composition 将会被追加到 mContainer 中
     * @param delay 单位是微妙, 1/1000000 秒
     */
    public fun play(composition: PAGComposition, delay: Long = 0L, maxLayerCount: Int = 100) {
        // 移除已经播放完毕的，偏移还没有播放完的(container会重新从0开始播放)
        val container = getContainer(composition)
        val currentTime = container.currentTime()
        for (i in container.numChildren() - 1 downTo 0) {
            val child = container.getLayerAt(i) ?: continue
            if (child.startTime() + child.duration() <= currentTime) {
                removeChild(container, child)
            } else {
                child.setStartTime(-(child.currentTime() - child.startTime()))
            }
        }

        composition.setStartTime(delay)
        container.addLayer(composition)

        // 防止 container Layer 数量爆炸
        while (container.numChildren() > maxLayerCount) {
            val oldest = container.removeLayerAt(0)
            val path = if (oldest is PAGFile) {
                oldest.path()
            } else {
                oldest.javaClass.simpleName
            }
        }

        this.composition = container
        progress = 0.0
        if (!isPlaying) {
            Log.i(TAG, "fuck not playing, start play duration: ${container.duration()}")
            play()
        } else {
            Log.i(TAG, "fuck playing, restart from progress 0")
        }
    }

    public fun playPAG() {
        scope.launch {
            val file = withContext(Dispatchers.IO) {
                val pagPath = "love.pag"
                val pagFile = PAGFile.Load(context.assets, pagPath).copyOriginal()
                pagFile?.let {
                    val transformPath = "high.pag"
                    PAGFile.Load(context.assets, transformPath)?.copyOriginal()?.apply {
                        val comp = getLayerAt(0) as PAGComposition
                        comp.removeAllLayers()
                        comp.addLayer(it)
                        return@let this
                    }
                    it
                }
            }
            if (file != null) {
                play(file)
            }
        }
    }

    /**
     * 子类可以重写以实现适合业务的缓存逻辑
     */
    public open fun getPAGFile(path: String): PAGFile? {
        return fileCache.getOrPut(path) {
            PAGFile.Load(path)
        }
    }

    /**
     * 子类可以重写以实现适合业务的缓存逻辑
     */
    public open fun getImage(path: String): PAGImage? {
        return imagecache.getOrPut(path) {
            PAGImage.FromPath(path)
        }
    }

    private companion object {
        private const val TAG = "fuck"
    }
}
