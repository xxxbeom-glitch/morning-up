package com.lumian.morningup.core.alarm

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** 흔들기를 알아챌 가속도 센서가 있는지. 없으면 미션 고르기에서 Shake Shake 카드를 비활성으로 둬요. */
fun interface ShakeSupport {
    fun available(): Boolean
}

class SensorShakeSupport @Inject constructor(@ApplicationContext private val context: Context) : ShakeSupport {
    override fun available(): Boolean =
        context.getSystemService(SensorManager::class.java)?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
}
