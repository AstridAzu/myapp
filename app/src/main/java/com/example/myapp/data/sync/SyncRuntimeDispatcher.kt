package com.example.myapp.data.sync

import android.content.Context

/**
 * Dispatcher global para solicitar sync inmediato desde capas sin acceso directo a Context.
 * Si aún no está inicializado, la solicitud se ignora de forma segura.
 */
object SyncRuntimeDispatcher {
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun requestSyncNow() {
        val context = appContext ?: return
        SyncScheduler.enqueueOneTimeSync(context)
    }

    fun getAppContextOrNull(): Context? = appContext
}
