package dev.rohitverma882.adbtest.adb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build

class UsbReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                val device: UsbDevice? =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(
                            UsbManager.EXTRA_DEVICE, UsbDevice::class.java
                        )
                    } else {
                        @Suppress("DEPRECATION") intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                forwardUsbDevice(context, device)
            }
        }
    }

    companion object {
        const val ACTION_USB_DEVICE_FORWARD = "adbtest.usb.forward"

        fun forwardUsbDevice(context: Context?, device: UsbDevice?) {
            if (context != null && device != null) {
                val intent = Intent(ACTION_USB_DEVICE_FORWARD)
                intent.putExtra(UsbManager.EXTRA_DEVICE, device)
                intent.setPackage(context.packageName)
                context.sendBroadcast(intent)
            }
        }
    }
}
