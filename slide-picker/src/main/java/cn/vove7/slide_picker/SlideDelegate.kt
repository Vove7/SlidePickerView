package cn.vove7.slide_picker

import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.ScrollView
import androidx.core.view.ScrollingView
import androidx.core.view.children
import kotlin.math.cos
import kotlin.math.sin


/**
 * # SlideDelegate
 * Created on 2019/12/9
 *
 * 滑动选择代理
 *
 * 注：
 * - ACTION_DOWN 和 ACTION_MOVE 时 target一定非空
 * - ACTION_UP 时 target 一定为空
 *
 * > onProcess action 顺序不一定是以 ACTION_DOWN 开始。因为 ACTION_DOWN 时 不一定得到 target
 *
 * 可能执行顺序（[括号代表可能无此回调]）：(ACTION_DOWN) -> (ACTION_MOVE) -> ACTION_UP
 *
 * @author Vove
 */
fun View.toggleSelected() {
    isSelected = !isSelected
}

class SlideDelegate(
    val parentHasScrollable: Boolean = false,
    val onProcess: (target: View?, childIndex: Int, action: Int) -> Unit = { target, _, _ ->
        target?.toggleSelected()
    }
) {
    //上次选择元素
    private var lastTarget: View? = null

    //ViewParent是否可滚动
    private val ViewParent.isScrollable: Boolean
        get() = this is ScrollingView || this is ScrollView

    /**
     * 代理 ViewGroup 的 dispatchTouchEvent 方法
     */
    fun onProcessTouchEvent(container: ViewGroup, event: MotionEvent): Boolean {

        if (event.actionMasked == MotionEvent.ACTION_UP) {
            lastTarget = null
            //恢复父级事件
            requestAllDisallowInterceptTouchEvent(container, false)
            onProcess(null, -1, event.action)
            return true
        }
        val target: Pair<Int, View>? by lazy {
            container.findTarget(
                PointF(
                    event.x,
                    event.y
                )
            )
        }

        //若父级为可滚动View
        if (container.parent.isScrollable || parentHasScrollable) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                if (target != null) {
                    //拦截父级事件
                    requestAllDisallowInterceptTouchEvent(container, true)
                } else {
                    //此次滚动，不再响应 onProcessTouchEvent
                    return false
                }
            }
        }
        @Suppress("UnnecessaryVariable")
        val t = target
        if (t != null) {
            processTarget(t.second, t.first, event.actionMasked)
        } else {
            lastTarget = null
        }
        return true
    }

    private fun requestAllDisallowInterceptTouchEvent(view: View, f: Boolean) {
        var p = view.parent
        while (p != null) {
            p.requestDisallowInterceptTouchEvent(f)
            p = p.parent
        }
    }

    private fun processTarget(target: View, pos: Int, action: Int) {
        if (target != lastTarget) {
            onProcess(target, pos, action)
            lastTarget = target
        }
    }

    /**
     * 搜索手指位置 对应的 PickableView
     */
    private fun ViewGroup.findTarget(dp: PointF): Pair<Int, View>? {
        val ts = mutableListOf<Pair<Int, View>>()
        children.forEachIndexed { i, it ->
            //中心点
            val vcp = PointF(
                ((it.right + it.left) / 2).toFloat() + translationX,
                ((it.bottom + it.top) / 2).toFloat() + translationY
            )
            //旋转
            val rp = dp.rotationWith(vcp, it.rotation) // 这里

            if (it.isShown && it.pointInView(rp)) {
                ts.add(i to it)
            }
        }
        return if (ts.isEmpty()) null
        //面积最小
        else ts.minBy {
            val v = it.second
            v.width * v.height
        }
    }

    /**
     * @return 此(this)点以center点为圆心 旋转 α 度 后的点
     * 注意屏幕坐标 y轴 下 为正
     * 参考 https://zhidao.baidu.com/question/554563421433536852.html
     */
    private fun PointF.rotationWith(center: PointF, α: Float): PointF {
        if (α == 0f) return this
        //α转弧度
        val αp = (Math.PI * α / 180).toFloat()

        val a = center.x
        val b = center.y
        return PointF(
            (x - a) * cos(αp) + (y - b) * sin(αp) + a,
            -(x - a) * sin(αp) + (y - b) * cos(αp) + b
        )
    }

    /**
     * @return p 点是否在 View 视图内
     */
    private fun View.pointInView(p: PointF): Boolean {
        return p.x - translationX in left..right && p.y - translationY in top..bottom
    }

}
