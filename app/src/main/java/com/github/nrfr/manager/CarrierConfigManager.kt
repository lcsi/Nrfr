package com.github.nrfr.manager

import android.content.Context
import android.os.Build
import android.os.ServiceManager
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyFrameworkInitializer
import android.telephony.TelephonyManager
import com.android.internal.telephony.ICarrierConfigLoader
import com.github.nrfr.model.SimCardInfo
import rikka.shizuku.ShizukuBinderWrapper

object CarrierConfigManager {
    fun getSimCards(context: Context): List<SimCardInfo> {
        val simCards = mutableListOf<SimCardInfo>()
        val subId1 = SubscriptionManager.getSubId(0)
        val subId2 = SubscriptionManager.getSubId(1)

        if (subId1 != null) {
            val config1 = getCurrentConfig(subId1[0])
            simCards.add(SimCardInfo(1, subId1[0], getCarrierNameBySubId(context, subId1[0]), config1))
        }
        if (subId2 != null) {
            val config2 = getCurrentConfig(subId2[0])
            simCards.add(SimCardInfo(2, subId2[0], getCarrierNameBySubId(context, subId2[0]), config2))
        }

        return simCards
    }

    private fun getCurrentConfig(subId: Int): Map<String, String> {
        try {
            val binder = ServiceManager.getService("carrier_config")
            val carrierConfigLoader = ICarrierConfigLoader.Stub.asInterface(ShizukuBinderWrapper(binder))

            val config = carrierConfigLoader.getConfigForSubId(subId, "com.github.nrfr") ?: return emptyMap()

            val result = mutableMapOf<String, String>()

            // 获取国家码配置
            config.getString(CarrierConfigManager.KEY_SIM_COUNTRY_ISO_OVERRIDE_STRING)?.let {
                result["国家码"] = it
            }

            // 获取运营商名称配置
            if (config.getBoolean(CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL, false)) {
                config.getString(CarrierConfigManager.KEY_CARRIER_NAME_STRING)?.let {
                    result["运营商名称"] = it
                }
            }

            return result
        } catch (e: Exception) {
            return emptyMap()
        }
    }

    private fun getCarrierNameBySubId(context: Context, subId: Int): String {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            ?: return ""

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 及以上使用新 API
                telephonyManager.getNetworkOperatorName(subId)
            } else {
                // Android 8-9 使用反射获取运营商名称
                val createForSubscriptionId = TelephonyManager::class.java.getMethod(
                    "createForSubscriptionId",
                    Int::class.javaPrimitiveType
                )
                val subTelephonyManager = createForSubscriptionId.invoke(telephonyManager, subId) as TelephonyManager
                subTelephonyManager.networkOperatorName
            }
        } catch (e: Exception) {
            // 如果获取失败，回退到默认的 TelephonyManager
            telephonyManager.networkOperatorName
        }
    }

    fun setCarrierConfig(subId: Int, countryCode: String?, carrierName: String? = null) {
        val bundle = PersistableBundle()

        // 设置国家码
        if (!countryCode.isNullOrEmpty() && countryCode.length == 2) {
            bundle.putString(
                CarrierConfigManager.KEY_SIM_COUNTRY_ISO_OVERRIDE_STRING,
                countryCode.lowercase()
            )
        }

        // 设置运营商名称
        if (!carrierName.isNullOrEmpty()) {
            bundle.putBoolean(CarrierConfigManager.KEY_CARRIER_NAME_OVERRIDE_BOOL, true)
            bundle.putString(CarrierConfigManager.KEY_CARRIER_NAME_STRING, carrierName)
        }

        overrideCarrierConfig(subId, bundle)
    }

    fun resetCarrierConfig(subId: Int) {
        overrideCarrierConfig(subId, null)
    }

    private fun overrideCarrierConfig(subId: Int, bundle: PersistableBundle?) {
        val binder = ServiceManager.getService("carrier_config")
        val carrierConfigLoader = ICarrierConfigLoader.Stub.asInterface(ShizukuBinderWrapper(binder))
        carrierConfigLoader.overrideConfig(subId, bundle, true)
    }
}
