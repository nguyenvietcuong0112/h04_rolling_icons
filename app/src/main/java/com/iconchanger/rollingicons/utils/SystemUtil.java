package com.iconchanger.rollingicons.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.mobi.libraryads.commons.sharepreference.SPF;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class SystemUtil {
    private static Locale myLocale;

    // Lưu ngôn ngữ đã cài đặt
    public static void saveLocale(Context context, String lang) {
        if (lang == null || lang.trim().isEmpty()) return;
        Log.d("LanguageDebug", "SystemUtil.saveLocale called with lang=" + lang);
        setPreLanguage(context, lang);
    }

    public static void setLocale(Context context) {
        String language = getPreLanguage(context);
        Log.d("LanguageDebug", "SystemUtil.setLocale called, resolved preLanguage=" + language);
        if (language == null || language.trim().isEmpty()) {
            language = "en";
        }
        Locale newLocale = new Locale(language);
        Locale.setDefault(newLocale);
        myLocale = newLocale;

        try {
            Configuration config = new Configuration(context.getResources().getConfiguration());
            config.setLocale(newLocale);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.os.LocaleList localeList = new android.os.LocaleList(newLocale);
                android.os.LocaleList.setDefault(localeList);
                config.setLocales(localeList);
            }
            context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        } catch (Exception e) {
            Log.e("LanguageDebug", "SystemUtil.setLocale updateConfiguration error: " + e.getMessage());
        }
    }

    public static void changeLang(String lang, Context context) {
        if (lang == null || lang.trim().isEmpty())
            return;
        Log.d("LanguageDebug", "SystemUtil.changeLang called with lang=" + lang);

        saveLocale(context, lang);
        setLocale(context);

        try {
            androidx.core.os.LocaleListCompat currentLocales = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales();
            String currentTag = currentLocales.toLanguageTags();
            if (!lang.equalsIgnoreCase(currentTag)) {
                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                    androidx.core.os.LocaleListCompat.forLanguageTags(lang)
                );
            }
        } catch (Exception e) {
            Log.e("LanguageDebug", "SystemUtil.changeLang setApplicationLocales error: " + e.getMessage());
        }
    }

    public static String getPreLanguage(Context mContext) {
        String cscLang = null;
        try {
            cscLang = new SPF(mContext).getLanguage_code_selected();
        } catch (Exception e) {
            Log.e("LanguageDebug", "SystemUtil.getPreLanguage: CSCSPF read error=" + e.getMessage());
        }
        SharedPreferences preferences = mContext.getSharedPreferences("data", Context.MODE_PRIVATE);
        String dataLang = preferences.getString("KEY_LANGUAGE", null);
        Log.d("LanguageDebug", "SystemUtil.getPreLanguage: cscLang=" + cscLang + ", dataLang=" + dataLang);
        if (cscLang != null && !cscLang.trim().isEmpty()) {
            return cscLang;
        }
        if (dataLang != null && !dataLang.trim().isEmpty()) {
            return dataLang;
        }
        return Locale.getDefault().getLanguage();
    }

    public static void setPreLanguage(Context context, String language) {
        Log.d("LanguageDebug", "SystemUtil.setPreLanguage called with language=" + language);
        if (language == null || language.equals("")) {
            return;
        } else {
            try {
                new SPF(context).setLanguage_code_selected(language);
                Log.d("LanguageDebug", "SystemUtil.setPreLanguage: saved to CSCSPF=" + language);
            } catch (Exception e) {
                Log.e("LanguageDebug", "SystemUtil.setPreLanguage: CSCSPF write error=" + e.getMessage());
            }
            SharedPreferences preferences = context.getSharedPreferences("data", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("KEY_LANGUAGE", language);
            editor.apply();
            Log.d("LanguageDebug", "SystemUtil.setPreLanguage: saved to SharedPreferences(data)=" + language);
        }
    }

    public static String getPath(Context context, Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) return null;
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String s = cursor.getString(column_index);
        cursor.close();
        return s;
    }

    public static File fileFromContentUri(Context context, Uri contentUri) throws IOException {
        String abc = "";
        String fileName = "";
        try {
            String fileDevice = getPath(context, contentUri);
            File file = new File(fileDevice);
            abc = file.getName();
        } catch (Exception e) {
            String fileExtension = getFileExtension(context, contentUri);
            try {
                String listUri[] = contentUri.toString().split("/");
                String fileNameEx = listUri[listUri.length - 1];
                Log.e("fileNameEx", fileNameEx);
                String urlDecodedTitle = URLDecoder.decode(fileNameEx, StandardCharsets.UTF_8.toString());
                String listUri2[] = urlDecodedTitle.split("_");
                if (listUri2.length > 1) {
                    for (int i = 0; i < listUri2.length - 1; i++) {
                        abc += listUri2[i];
                        if (i < listUri2.length - 2) {
                            abc += "_";
                        }
                    }
                } else {
                    abc = urlDecodedTitle;
                }
                abc += "." + fileExtension;
            } catch (Exception x) {
                if (fileExtension != null) {
                    abc = abc + fileExtension;
                } else {
                    abc = "";
                }
            }
        }
        fileName = abc;
        File tempFile = new File(context.getCacheDir(), fileName);
        tempFile.createNewFile();
        try {
            FileOutputStream oStream = new FileOutputStream(tempFile);
            InputStream inputStream = context.getContentResolver().openInputStream(contentUri);
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                oStream.write(buf, 0, len);
            }
            oStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
            return tempFile;
        }
        return tempFile;
    }

    private static String getFileExtension(Context context, Uri uri) {
        String fileType = context.getContentResolver().getType(uri);
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(fileType);
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }
}