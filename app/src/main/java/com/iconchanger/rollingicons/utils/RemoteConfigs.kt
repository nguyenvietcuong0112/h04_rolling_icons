package com.iconchanger.rollingicons.utils

import com.mobi.libraryads.commons.remote.KonfigModel
import com.mobi.libraryads.commons.remote.konfig

object RemoteConfigs : KonfigModel {

    val native_permission by konfig("native_permission", true)
    val native_all by konfig("native_all", true)
    val native_popup by konfig("native_popup", true)
    val native_home by konfig("native_home", true)
    val banner_collap_home by konfig("banner_collap_home", true)
    val banner_collap_preview by konfig("banner_collap_preview", true)
    val inter_click by konfig("inter_click", true)
    val inter_success by konfig("inter_success", true)
    val inter_next by konfig("inter_next", true)
    val inter_apply by konfig("inter_apply", true)
    val inter_set_wallpaper by konfig("inter_set_wallpaper", true)


}