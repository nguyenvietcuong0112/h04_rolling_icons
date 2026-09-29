package com.iconchanger.rollingicons.utils

import android.view.View
import androidx.appcompat.app.AppCompatActivity

import com.iconchanger.rollingicons.R
import com.mobi.libraryads.ads.inter_ads.Inter
import com.mobi.libraryads.commons.utils.isInternetConnected

object AdsConfig {

    @Volatile
    private var isShowingOrLoadingAd = false

    private fun showGenericInterAd(
        activity: AppCompatActivity,
        adId: String = activity.getString(R.string.inter_click),
        view: View? = null,
        isEnabled: Boolean,
        onAdClosedAction: () -> Unit
    ) {
        if (isShowingOrLoadingAd) {
            return
        }

        isShowingOrLoadingAd = true
        view?.isEnabled = false

        fun resetState() {
            isShowingOrLoadingAd = false
            try {
                view?.isEnabled = true
            } catch (_: Exception) {}
        }

        val targetAdId = adId

        // 1. Check remote config enabled and internet connection
        if (!isEnabled || !activity.isInternetConnected()) {
            resetState()
            onAdClosedAction()
            return
        }

        // 2. Fullscreen Loading Dialog
        val loadingDialog = DialogLoadingAd(activity)
        try {
            if (!activity.isFinishing && !activity.isDestroyed) {
                loadingDialog.show()
            }
        } catch (_: Exception) {
            resetState()
            onAdClosedAction()
            return
        }

        fun dismissLoading() {
            try {
                if (loadingDialog.isShowing && !activity.isFinishing && !activity.isDestroyed) {
                    loadingDialog.dismiss()
                }
            } catch (_: Exception) {}
        }

        Inter.loadAndShowInter(
            activity = activity,
            adId = targetAdId,
            timeDelay = 500L,
            timeOut = 15000L,
            canShowId = isEnabled,
            onShown = {
                dismissLoading()
            },
            nextAction = { _ ->
                dismissLoading()
                resetState()
                onAdClosedAction()
            }
        )
    }

    // 1. inter_success: inter khi click button back to Home (màn Success)
    fun showInterSuccessAd(
        activity: AppCompatActivity,
        view: View? = null,
        onAdClosedAction: () -> Unit
    ) {
        showGenericInterAd(
            activity = activity,
            adId = activity.getString(R.string.inter_click),
            view = view,
            isEnabled = RemoteConfigs.inter_success,
            onAdClosedAction = onAdClosedAction
        )
    }

    // 2. inter_apply: inter khi click button apply trong các luồng feature và inter khi click button set wallpaper trong popup
    fun showInterApplyAd(
        activity: AppCompatActivity,
        view: View? = null,
        onAdClosedAction: () -> Unit
    ) {
        showGenericInterAd(
            activity = activity,
            adId = activity.getString(R.string.inter_click),
            view = view,
            isEnabled = RemoteConfigs.inter_apply,
            onAdClosedAction = onAdClosedAction
        )
    }

    // 3. inter_next: inter khi click button continue/next trong các luồng feature
    fun showInterNextAd(
        activity: AppCompatActivity,
        view: View? = null,
        onAdClosedAction: () -> Unit
    ) {
        showGenericInterAd(
            activity = activity,
            adId = activity.getString(R.string.inter_click),
            view = view,
            isEnabled = RemoteConfigs.inter_next,
            onAdClosedAction = onAdClosedAction
        )
    }

    // 4. inter_click: inter khi click vào bất kỳ menu nào ở home (trừ setting) & inter khi click button back từ setting về home
    fun showInterClickAd(
        activity: AppCompatActivity,
        view: View? = null,
        onAdClosedAction: () -> Unit
    ) {
        showGenericInterAd(
            activity = activity,
            adId = activity.getString(R.string.inter_click),
            view = view,
            isEnabled = RemoteConfigs.inter_click,
            onAdClosedAction = onAdClosedAction
        )
    }

    // 5. inter_set_wallpaper: inter khi set wallpaper ở màn FirstOpenWallpaperActivity
    fun showInterSetWallpaperAd(
        activity: AppCompatActivity,
        view: View? = null,
        onAdClosedAction: () -> Unit
    ) {
        showGenericInterAd(
            activity = activity,
            adId = activity.getString(R.string.inter_set_wallpaper),
            view = view,
            isEnabled = RemoteConfigs.inter_set_wallpaper,
            onAdClosedAction = onAdClosedAction
        )
    }
}
