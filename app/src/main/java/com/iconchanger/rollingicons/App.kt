package com.iconchanger.rollingicons

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.mobi.libraryads.data.AdsLanguageConfig
import com.mobi.libraryads.data.AdsOBConfig
import com.mobi.libraryads.data.AdsSplashConfig
import com.mobi.libraryads.data.LanguageConfig
import com.mobi.libraryads.data.LanguageSetting
import com.mobi.libraryads.data.OBConfig
import com.mobi.libraryads.data.OnActivityCallBack
import com.mobi.libraryads.data.UiLanguageConfig
import com.mobi.libraryads.data.UiOBConfig
import com.mobi.libraryads.data.UiSplashConfig
import com.iconchanger.rollingicons.ui.FirstOpenWallpaperActivity
import com.iconchanger.rollingicons.ui.MainActivity
import com.iconchanger.rollingicons.ui.PermissionActivity
import com.iconchanger.rollingicons.utils.EnumSelectLanguage
import com.iconchanger.rollingicons.utils.RemoteConfigs
import com.iconchanger.rollingicons.utils.SharePreferenceUtils
import com.iconchanger.rollingicons.utils.SystemUtil
import com.mobi.libraryads.AdsApplication
import com.mobi.libraryads.commons.sharepreference.SPF
import com.mobi.libraryads.data.SplashConfig


class App : Application() {
    override fun onCreate() {
        super.onCreate()
        initFO()

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        val savedLangCode = SPF(this).language_code_selected
        if (!savedLangCode.isNullOrBlank()) {
            SystemUtil.setLocale(this)
        }

//        Executors.newSingleThreadExecutor().execute {
//            FirebaseApp.initializeApp(this)
//            FacebookSdk.setClientToken(getString(R.string.facebook_client_token))
//            AdjustHelper.init(
//                application = this,
//                appToken = AppAdjustTokens.ADJUST_APP_TOKEN,
//                iapEventToken = AppAdjustTokens.EVENT_IAP_COMMON,
//                isDebug = BuildConfig.DEBUG
//            )
//        }

    }

    private fun initFO() {
        val adsLibrary = AdsApplication(this, RemoteConfigs, BuildConfig.DEBUG)
        val isOrganic = SharePreferenceUtils.isOrganic(this)



        adsLibrary.initSdk(
            adjustAppToken = "",
            gsmAppId = "",
            splashConfig = SplashConfig(
                uiSplashConfig = UiSplashConfig(
                    resLayout = R.layout.activity_splash,
                    showFOForever = true,
                    homeActivity = MainActivity::class.java,
                    timeout = 30_000
                ),
                adsSplashConfig = AdsSplashConfig(
                    bannerId = getString(R.string.banner_splash),
                    interHighId = getString(R.string.inter_splash_high),
                    interAllId = getString(R.string.inter_splash),
                    nativeFullLayout = R.layout.layout_native_full,
                    admobAOAId = getString(R.string.resume_open_app),
                    isCheckOrganicUser = true
                )
            ),
            languageConfig =
                LanguageConfig(
                    uiLanguageConfig = UiLanguageConfig(
                        resLayout = R.layout.activity_language_app,
                        itemLangDefault = R.layout.item_select_language_default,
                        itemLangSelected = R.layout.item_select_language_selected,
                        listLanguage = EnumSelectLanguage.toLanguageModelList(),
                        languageSetting = object : LanguageSetting {
                            override fun onDone(activity: Activity) {
                                val code = SPF(activity).language_code_selected
                                val finalCode = if (!code.isNullOrBlank()) code else SystemUtil.getPreLanguage(activity)
                                android.util.Log.d("LanguageDebug", "App languageSetting.onDone: code=$code, finalCode=$finalCode")
                                SystemUtil.changeLang(finalCode, activity)
                                val intent = Intent(activity, MainActivity::class.java).apply {
                                    flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    putExtra("disable_animation", true)
                                }
                                activity.startActivity(intent)
                                activity.finish()
                            }
                        }
                    ),
                    adsLanguageConfig = AdsLanguageConfig(
                        nativeLangHighId = getString(R.string.native_language_high),
                        nativeLangId = getString(R.string.native_language),
                        nativeLangClickHighId = getString(R.string.native_language_high_click),
                        nativeLangClickId = getString(R.string.native_language_click),
                        layoutNative = R.layout.layout_native_media,
                        layoutNativeClick = R.layout.layout_native_media_click
                    )
                ),
            obConfig =
                OBConfig(
                    uiOBConfig = UiOBConfig(
                        resFragmentOB1 = R.layout.fragment_intro1,
                        resFragmentOB2 = R.layout.fragment_intro2,
                        resFragmentOB3 = R.layout.fragment_intro3,
                        resFragmentOB4 = R.layout.fragment_intro4,
                        resFragmentOBAdFull = R.layout.fragment_ob_ad_full,
                        activityCallback = object : OnActivityCallBack {
                            override fun onNextActivity(activity: Activity, inSession2: Boolean) {
                                val savedLang = SPF(activity).language_code_selected
                                if (!savedLang.isNullOrBlank()) {
                                    SystemUtil.changeLang(savedLang, activity)
                                }
                                if (inSession2) {
                                    val intent = Intent(activity, PermissionActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    activity.startActivity(intent)
                                } else {
                                    val intent = Intent(activity, FirstOpenWallpaperActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    activity.startActivity(intent)
                                }
                            }
                        },
//                        nextOBActivity = PermissionActivity::class.java
                    ),
                    adsOBConfig = AdsOBConfig(
                        nativeOB1Id = getString(R.string.native_onboarding_1),
                        nativeOB4Id = getString(R.string.native_onboarding_4),
                        nativeOBFull12Id = getString(R.string.native_onboarding_full_1),
                        nativeOBFull23Id = getString(R.string.native_onboarding_full_2),
                        layoutNativeOB1 = R.layout.layout_native_media,
                        layoutNativeOB4 = R.layout.layout_native_media,
                        layoutNativeFullOB = R.layout.admob_layout_native_full,
                        isLoadNativeOBInLanguage = true,
                    )
                ))
    }
}
