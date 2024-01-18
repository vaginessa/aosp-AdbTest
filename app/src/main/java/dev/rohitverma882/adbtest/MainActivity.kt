package dev.rohitverma882.adbtest

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import dev.rohitverma882.adbtest.adb.AdbDevice
import dev.rohitverma882.adbtest.adb.UsbReceiver
import dev.rohitverma882.adbtest.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val usbManager by lazy { getSystemService(UsbManager::class.java) }

    private var usbDevice: UsbDevice? = null
    private var usbDeviceConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var adbDevice: AdbDevice? = null

    private var usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                UsbReceiver.ACTION_USB_DEVICE_FORWARD -> {
                    val device: UsbDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                UsbManager.EXTRA_DEVICE, UsbDevice::class.java
                            )
                        } else {
                            @Suppress("DEPRECATION") intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }

                    if (device != null && filterDevice(device)) {
                        if (usbManager.hasPermission(device)) {
                            log("device added: ${device.deviceName}")
                            val adbInterface = findAdbInterface(device)
                            setAdbInterface(device, adbInterface)
                        } else {
                            requestUsbPermission(device)
                        }
                    }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device: UsbDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                UsbManager.EXTRA_DEVICE, UsbDevice::class.java
                            )
                        } else {
                            @Suppress("DEPRECATION") intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }
                    if (device != null && filterDevice(device)) {
                        if (usbDevice != null && usbDevice?.deviceName == device.deviceName) {
                            log("device removed: ${device.deviceName}")
                            setAdbInterface(null, null)
                        }
                    }
                }
            }
        }
    }

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            synchronized(this) {
                when (intent?.action) {
                    ACTION_USB_PERMISSION -> {
                        val device: UsbDevice? =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(
                                    UsbManager.EXTRA_DEVICE, UsbDevice::class.java
                                )
                            } else {
                                @Suppress("DEPRECATION") intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                            }

                        if (device != null && intent.getBooleanExtra(
                                UsbManager.EXTRA_PERMISSION_GRANTED, false
                            )
                        ) {
                            log("device added: ${device.deviceName}")
                            val adbInterface = findAdbInterface(device)
                            setAdbInterface(device, adbInterface)
                        }
                    }
                }
            }
        }
    }

    private var handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_LOG -> appendLog((msg.obj as String))
                MESSAGE_DEVICE_ONLINE -> handleDeviceOnline(msg.obj as AdbDevice)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        handleUsbIntent(intent)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        log("started")

        usbManager.deviceList.values.forEach {
            if (filterDevice(it)) {
                if (usbManager.hasPermission(it)) {
                    val adbInterface = findAdbInterface(it)
                    if (setAdbInterface(it, adbInterface)) {
                        log("device added: ${it.deviceName}")
                        return@forEach
                    }
                } else {
                    requestUsbPermission(it)
                }
            }
        }

        val filter = IntentFilter()
        filter.addAction(UsbReceiver.ACTION_USB_DEVICE_FORWARD)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        ContextCompat.registerReceiver(
            this, usbReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )

        ContextCompat.registerReceiver(
            this,
            usbPermissionReceiver,
            IntentFilter(ACTION_USB_PERMISSION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun log(s: String) {
        val m = Message.obtain(handler, MESSAGE_LOG)
        m.obj = s
        handler.sendMessage(m)
    }

    @SuppressLint("SetTextI18n")
    private fun appendLog(line: String) {
        val tvOutput = binding.contentMain.tvOutput
        val oldLines = tvOutput.text.trim()
        if (oldLines.isEmpty()) {
            tvOutput.text = line
        } else {
            tvOutput.text = "$oldLines\n$line"
        }
    }

    fun deviceOnline(device: AdbDevice) {
        val m = Message.obtain(handler, MESSAGE_DEVICE_ONLINE)
        m.obj = device
        handler.sendMessage(m)
    }

    private fun handleDeviceOnline(device: AdbDevice) {
        log("device online: " + device.serial)
//        device.openSocket("shell:exec logcat")
        device.openSocket("shell:exec pm")
    }

    private fun requestUsbPermission(device: UsbDevice) {
        val permissionIntent = PendingIntent.getBroadcast(
            this, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
        )
        usbManager.requestPermission(device, permissionIntent)
    }

    private fun handleUsbIntent(intent: Intent?) {
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
                UsbReceiver.forwardUsbDevice(this, device)
            }
        }
    }

    private fun setAdbInterface(device: UsbDevice?, adbInterface: UsbInterface?): Boolean {
        if (usbDeviceConnection != null) {
            if (usbInterface != null) {
                usbDeviceConnection?.releaseInterface(usbInterface)
                usbInterface = null
            }
            usbDeviceConnection?.close()
            usbDevice = null
            usbDeviceConnection = null
        }
        if (device != null) {
            val connection = usbManager.openDevice(device)
            if (connection != null) {
                log("open succeeded")

                if (connection.claimInterface(adbInterface, false)) {
                    log("claim interface succeeded")
                    usbDevice = device
                    usbDeviceConnection = connection
                    usbInterface = adbInterface
                    adbDevice = AdbDevice(this, usbDeviceConnection, adbInterface)
                    log("call start")
                    adbDevice?.start()
                    return true
                } else {
                    log("claim interface failed")
                    connection.close()
                }
            } else {
                log("open failed")
            }
        }
        if (usbDeviceConnection == null && adbDevice != null) {
            adbDevice?.stop()
            adbDevice = null
        }
        return false
    }

    private fun filterDevice(device: UsbDevice): Boolean {
        if (device.deviceClass == ADB_CLASS && device.deviceSubclass == ADB_SUBCLASS && device.deviceProtocol == ADB_PROTOCOL) {
            return true
        }
        return findAdbInterface(device) != null
    }

    private fun findAdbInterface(device: UsbDevice): UsbInterface? {
        for (i: Int in 0 until device.interfaceCount) {
            val deviceInterface = device.getInterface(i)
            if (deviceInterface.interfaceClass == ADB_CLASS && deviceInterface.interfaceSubclass == ADB_SUBCLASS && deviceInterface.interfaceProtocol == ADB_PROTOCOL) {
                return deviceInterface
            }
        }
        return null
    }

    override fun onNewIntent(intent: Intent?) {
        handleUsbIntent(intent)
        super.onNewIntent(intent)
    }

    override fun onDestroy() {
        unregisterReceiver(usbReceiver)
        setAdbInterface(null, null)
        super.onDestroy()
    }

    companion object {
        private const val ADB_CLASS = 0xff
        private const val ADB_SUBCLASS = 0x42
        private const val ADB_PROTOCOL = 0x1

        private const val ACTION_USB_PERMISSION = "adbtest.usb.permission"

        private const val MESSAGE_LOG = 1
        private const val MESSAGE_DEVICE_ONLINE = 2
    }
}