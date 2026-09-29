package com.iconchanger.rollingicons.ui

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.iconchanger.rollingicons.R
import com.iconchanger.rollingicons.model.AppInfo
import com.iconchanger.rollingicons.data.AppRepository
import com.iconchanger.rollingicons.data.PreferenceRepository
import com.iconchanger.rollingicons.render.GameRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LauncherActivity : AndroidApplication() {

    private val coroutineScope = MainScope()

    private lateinit var btnDrawer: ImageView
    private lateinit var appDrawerLayout: FrameLayout
    private lateinit var drawerRecyclerView: RecyclerView
    private lateinit var btnMinimizeDrawer: ImageView
    private lateinit var btnSearch: ImageView

    private lateinit var appRepository: AppRepository
    private lateinit var preferenceRepository: PreferenceRepository
    private val appList = ArrayList<AppInfo>()
    private lateinit var drawerAdapter: DrawerAppAdapter
    private var cachedApps: List<AppInfo>? = null
    private var activeExitDialog: android.app.Dialog? = null
    private var backInvokedCallback: Any? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            useGL30 = false
        }
        val gdxView = initializeForView(GameRenderer(this), config)

        setContentView(R.layout.activity_launcher)

        val gdxContainer = findViewById<FrameLayout>(R.id.gdxContainer)
        gdxContainer.addView(gdxView)

        val txtGameScore = findViewById<TextView>(R.id.txtGameScore)
        val layoutClock = findViewById<LinearLayout>(R.id.layoutClock)
        btnDrawer = findViewById(R.id.btnDrawer)
        appDrawerLayout = findViewById(R.id.appDrawerLayout)
        drawerRecyclerView = findViewById(R.id.drawerRecyclerView)
        btnMinimizeDrawer = findViewById(R.id.btnMinimizeDrawer)
        btnSearch = findViewById(R.id.btnSearch)

        appRepository = AppRepository(this)
        preferenceRepository = PreferenceRepository(this)

        // Thiết lập RecyclerView cho Drawer
        drawerRecyclerView.layoutManager = GridLayoutManager(this, 4)
        drawerAdapter = DrawerAppAdapter()
        drawerRecyclerView.adapter = drawerAdapter

        txtGameScore.visibility = View.GONE
        layoutClock.visibility = View.VISIBLE
        btnDrawer.visibility = View.VISIBLE

        // Click mở Drawer
        btnDrawer.setOnClickListener {
            loadDrawerApps()
            appDrawerLayout.visibility = View.VISIBLE
        }

        // Click đóng Drawer
        btnMinimizeDrawer.setOnClickListener {
            appDrawerLayout.visibility = View.GONE
        }
        appDrawerLayout.setOnClickListener {
            appDrawerLayout.visibility = View.GONE
        }

        // Nút tìm kiếm nhanh
        btnSearch.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra("query", "")
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.toast_opening_search), Toast.LENGTH_SHORT).show()
            }
        }

        // Đăng ký OnBackInvokedCallback cho Android 13+ (API 33+) do enableOnBackInvokedCallback="true"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val callback = android.window.OnBackInvokedCallback {
                handleBackPress()
            }
            backInvokedCallback = callback
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                callback
            )
        }

        // Bắt đầu tải trước danh sách app trong Dispatchers.IO để cache lại
        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val apps = appRepository.getInstalledApps()
            cachedApps = apps
        }
    }

    override fun onResume() {
        super.onResume()
        coroutineScope.launch {
            val isFloatEnabled = preferenceRepository.isFloatButtonEnabled()
            btnDrawer.visibility = if (isFloatEnabled) View.VISIBLE else View.GONE
        }
    }

    private fun handleBackPress() {
        if (appDrawerLayout.visibility == View.VISIBLE) {
            appDrawerLayout.visibility = View.GONE
            return
        }

        if (activeExitDialog?.isShowing == true) {
            return
        }

        showExitDialog()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        handleBackPress()
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
            handleBackPress()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun loadDrawerApps() {
        val cached = cachedApps
        if (cached != null) {
            appList.clear()
            appList.addAll(cached)
            drawerAdapter.notifyDataSetChanged()
        } else {
            // Nếu chưa cache xong, ta thực hiện tải song song bất đồng bộ trên IO
            coroutineScope.launch {
                val apps = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    appRepository.getInstalledApps()
                }
                cachedApps = apps
                appList.clear()
                appList.addAll(apps)
                drawerAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun showExitDialog() {
        // Hiển thị Dialog xác nhận thoát app đồng bộ với theme
        val dialog = android.app.Dialog(this)
        activeExitDialog = dialog
        val view = layoutInflater.inflate(R.layout.dialog_exit, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        val btnCancel = view.findViewById<android.widget.Button>(R.id.btnDialogCancel)
        val btnExit = view.findViewById<android.widget.Button>(R.id.btnDialogExit)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnExit.setOnClickListener {
            dialog.dismiss()
            
            if (isDefaultLauncher()) {
                Toast.makeText(this@LauncherActivity, getString(R.string.toast_change_default_home), Toast.LENGTH_LONG).show()
                try {
                    val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                    } catch (ex: Exception) {
                        // ignore
                    }
                }
            } else {
                // Tìm ứng dụng Home khác (trình chạy hệ thống) để launch trực tiếp
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                }
                val homeActivities = packageManager.queryIntentActivities(homeIntent, 0)
                var launchedSystemHome = false
                for (info in homeActivities) {
                    val pkgName = info.activityInfo.packageName
                    if (pkgName != packageName) {
                        val systemHomeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            setPackage(pkgName) // Chỉ định đích danh package của Home hệ thống
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            startActivity(systemHomeIntent)
                            launchedSystemHome = true
                            break
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                }
                if (!launchedSystemHome) {
                    // Fallback nếu không tìm thấy launcher nào khác
                    try {
                        val fallbackHome = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(fallbackHome)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
                
                // Đóng tất cả các tasks của ứng dụng và xóa sạch khỏi danh sách Recents
                val activityManager = getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                activityManager.appTasks.forEach { task ->
                    try {
                        task.finishAndRemoveTask()
                    } catch (e: Exception) {
                        // ignore
                    }
                }
                
                // Trì hoãn 150ms để hệ thống chuyển tiếp mượt mà sang Home rồi kill tiến trình triệt để
                view.postDelayed({
                    android.os.Process.killProcess(android.os.Process.myPid())
                    System.exit(0)
                }, 150)
            }
        }

        dialog.setOnDismissListener {
            activeExitDialog = null
        }
        dialog.show()
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        val currentHomePkg = resolveInfo?.activityInfo?.packageName
        return currentHomePkg == packageName
    }

    override fun onDestroy() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            (backInvokedCallback as? android.window.OnBackInvokedCallback)?.let {
                onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it)
            }
        }
        coroutineScope.cancel()
        super.onDestroy()
    }

    // Adapter danh sách ứng dụng trong Drawer
    private inner class DrawerAppAdapter : RecyclerView.Adapter<DrawerAppAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val iconView: ImageView = view.findViewById(R.id.app_icon_view)
            val titleView: TextView = view.findViewById(R.id.app_title_view)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_drawer_app, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val appInfo = appList[position]
            holder.titleView.text = appInfo.appName

            val cachedIcon = com.iconchanger.rollingicons.data.AppIconCache.get(appInfo.packageName)
            if (cachedIcon != null) {
                holder.iconView.setImageDrawable(cachedIcon)
            } else {
                holder.iconView.setImageResource(android.R.drawable.sym_def_app_icon)
                holder.iconView.tag = appInfo.packageName
                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val icon = packageManager.getApplicationIcon(appInfo.packageName)
                        com.iconchanger.rollingicons.data.AppIconCache.put(appInfo.packageName, icon)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            if (holder.iconView.tag == appInfo.packageName) {
                                holder.iconView.setImageDrawable(icon)
                            }
                        }
                    } catch (e: Exception) {
                        // ignore, fallback already set
                    }
                }
            }

            holder.itemView.setOnClickListener {
                val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                if (launchIntent != null) {
                    try {
                        startActivity(launchIntent)
                        appDrawerLayout.visibility = View.GONE
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        Toast.makeText(this@LauncherActivity, getString(R.string.toast_cannot_open_app), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@LauncherActivity, getString(R.string.toast_cannot_open_app), Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun getItemCount(): Int = appList.size
    }
}

