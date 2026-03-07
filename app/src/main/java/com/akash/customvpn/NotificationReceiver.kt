package com.akash.customvpn

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val domain = intent.getStringExtra("domain")
        if (domain != null) {
            DomainRepository.init(context)
            if (!DomainRepository.isBlocked(domain)) {
                DomainRepository.toggleBlockDomain(domain)
            }
            
            // Dismiss the notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(domain.hashCode())
        }
    }
}
