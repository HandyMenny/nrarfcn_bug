package it.smartphonecombo.nr_arfcn_bug

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoNr
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.CellInfoCallback
import android.util.Log
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity() {

    private var job: Job? = null
    private var scanIntervalMs: Long = 3000L
    private var telephonyManager: TelephonyManager? = null
    private var textview: TextView? = null

    private fun launchLoop(): Job {
        return CoroutineScope(Job() + Dispatchers.IO).launch {
            while (isActive) {
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    updateNetworkData()
                }
                delay(scanIntervalMs)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textview = findViewById(R.id.textview)
        telephonyManager = this.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun updateNetworkData() {
        telephonyManager?.requestCellInfoUpdate(this.mainExecutor, object : CellInfoCallback() {
            @SuppressLint("SetTextI18n")
            override fun onCellInfo(cells: List<CellInfo>) {
                textview?.text = ""
                cells.filterIsInstance<CellInfoNr>().forEach {
                    val connectionStatus = it.cellConnectionStatus
                    val nrarfcn = (it.cellIdentity as CellIdentityNr).nrarfcn
                    val string = "connectionStatus=$connectionStatus, nrarfcn=$nrarfcn"
                    textview?.apply {
                        append(string)
                        append("\n")
                    }
                    Log.d("nrarfcn_bug", string)
                }
                textview?.apply {
                    if (text.isBlank()) {
                        text = "No NR cells found"
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            // recreate job
            job?.cancel()
            job = launchLoop().apply {
                start()
            }
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ), 0
            )
        }
    }

    override fun onPause() {
        super.onPause()
        // stop job
        job?.cancel().also {
            job = null
        }
    }
}