package com.github.nrfr

import android.content.Context
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyFrameworkInitializer
import android.telephony.TelephonyManager
import com.android.internal.telephony.ICarrierConfigLoader
import rikka.shizuku.ShizukuBinderWrapper

object CarrierConfigManager {
    // 添加预设运营商配置
    object PresetCarriers {
        data class CarrierPreset(
            val name: String,
            val displayName: String,
            val region: String
        )

        val presets = listOf(
            // 中国大陆运营商
            CarrierPreset("中国移动", "China Mobile", "CN"),
            CarrierPreset("中国联通", "China Unicom", "CN"),
            CarrierPreset("中国电信", "China Telecom", "CN"),

            // 中国香港运营商
            CarrierPreset("中国移动香港", "CMHK", "HK"),
            CarrierPreset("香港电讯", "HKT", "HK"),
            CarrierPreset("3香港", "3HK", "HK"),
            CarrierPreset("SmarTone", "SmarTone", "HK"),

            // 中国澳门运营商
            CarrierPreset("澳门电讯", "CTM", "MO"),
            CarrierPreset("3澳门", "3 Macau", "MO"),

            // 中国台湾运营商
            CarrierPreset("中华电信", "Chunghwa Telecom", "TW"),
            CarrierPreset("台湾大哥大", "Taiwan Mobile", "TW"),
            CarrierPreset("远传电信", "FarEasTone", "TW"),

            // 日本运营商
            CarrierPreset("NTT docomo", "NTT docomo", "JP"),
            CarrierPreset("au", "au by KDDI", "JP"),
            CarrierPreset("Softbank", "Softbank", "JP"),
            CarrierPreset("Rakuten", "Rakuten Mobile", "JP"),

            // 韩国运营商
            CarrierPreset("SK Telecom", "SK Telecom", "KR"),
            CarrierPreset("KT", "KT Corporation", "KR"),
            CarrierPreset("LG U+", "LG U+", "KR"),

            // 美国运营商
            CarrierPreset("AT&T", "AT&T", "US"),
            CarrierPreset("T-Mobile", "T-Mobile USA", "US"),
            CarrierPreset("Verizon", "Verizon", "US"),
            CarrierPreset("Sprint", "Sprint", "US"),

            // 英国运营商
            CarrierPreset("EE", "EE", "GB"),
            CarrierPreset("O2", "O2 UK", "GB"),
            CarrierPreset("Three", "Three UK", "GB"),
            CarrierPreset("Vodafone", "Vodafone UK", "GB"),

            // 新加坡运营商
            CarrierPreset("Singtel", "Singtel", "SG"),
            CarrierPreset("StarHub", "StarHub", "SG"),
            CarrierPreset("M1", "M1", "SG"),

            // 马来西亚运营商
            CarrierPreset("Maxis", "Maxis", "MY"),
            CarrierPreset("Celcom", "Celcom", "MY"),
            CarrierPreset("Digi", "Digi", "MY"),
            CarrierPreset("U Mobile", "U Mobile", "MY"),

            // 泰国运营商
            CarrierPreset("AIS", "AIS", "TH"),
            CarrierPreset("DTAC", "DTAC", "TH"),
            CarrierPreset("True Move H", "True Move H", "TH"),

            // 越南运营商
            CarrierPreset("Viettel", "Viettel Mobile", "VN"),
            CarrierPreset("Vinaphone", "Vinaphone", "VN"),
            CarrierPreset("Mobifone", "Mobifone", "VN"),

            // 印度尼西亚运营商
            CarrierPreset("Telkomsel", "Telkomsel", "ID"),
            CarrierPreset("Indosat", "Indosat Ooredoo", "ID"),
            CarrierPreset("XL Axiata", "XL Axiata", "ID"),

            // 菲律宾运营商
            CarrierPreset("Globe", "Globe Telecom", "PH"),
            CarrierPreset("Smart", "Smart Communications", "PH"),
            CarrierPreset("DITO", "DITO Telecommunity", "PH"),

            // 印度运营商
            CarrierPreset("Jio", "Reliance Jio", "IN"),
            CarrierPreset("Airtel", "Bharti Airtel", "IN"),
            CarrierPreset("Vi", "Vodafone Idea", "IN"),

            // 澳大利亚运营商
            CarrierPreset("Telstra", "Telstra", "AU"),
            CarrierPreset("Optus", "Optus", "AU"),
            CarrierPreset("Vodafone", "Vodafone AU", "AU"),

            // 加拿大运营商
            CarrierPreset("Bell", "Bell Mobility", "CA"),
            CarrierPreset("Rogers", "Rogers Wireless", "CA"),
            CarrierPreset("Telus", "Telus Mobility", "CA"),

            // 德国运营商
            CarrierPreset("Telekom", "T-Mobile DE", "DE"),
            CarrierPreset("Vodafone", "Vodafone DE", "DE"),
            CarrierPreset("O2", "O2 DE", "DE"),

            // 法国运营商
            CarrierPreset("Orange", "Orange FR", "FR"),
            CarrierPreset("SFR", "SFR", "FR"),
            CarrierPreset("Free", "Free Mobile", "FR"),
            CarrierPreset("Bouygues", "Bouygues Telecom", "FR"),

            // 意大利运营商
            CarrierPreset("TIM", "Telecom Italia", "IT"),
            CarrierPreset("Vodafone", "Vodafone IT", "IT"),
            CarrierPreset("Wind Tre", "Wind Tre", "IT"),

            // 西班牙运营商
            CarrierPreset("Movistar", "Movistar", "ES"),
            CarrierPreset("Vodafone", "Vodafone ES", "ES"),
            CarrierPreset("Orange", "Orange ES", "ES"),

            // 俄罗斯运营商
            CarrierPreset("MTS", "MTS", "RU"),
            CarrierPreset("MegaFon", "MegaFon", "RU"),
            CarrierPreset("Beeline", "Beeline", "RU"),

            // 巴西运营商
            CarrierPreset("Vivo", "Vivo", "BR"),
            CarrierPreset("Claro", "Claro", "BR"),
            CarrierPreset("TIM", "TIM Brasil", "BR"),

            // 自定义选项
            CarrierPreset("自定义", "", "")
        )
    }

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
            val carrierConfigLoader = ICarrierConfigLoader.Stub.asInterface(
                ShizukuBinderWrapper(
                    TelephonyFrameworkInitializer
                        .getTelephonyServiceManager()
                        .carrierConfigServiceRegisterer
                        .get()
                )
            )
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
        return telephonyManager.getNetworkOperatorName(subId)
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
        val carrierConfigLoader = ICarrierConfigLoader.Stub.asInterface(
            ShizukuBinderWrapper(
                TelephonyFrameworkInitializer
                    .getTelephonyServiceManager()
                    .carrierConfigServiceRegisterer
                    .get()
            )
        )
        carrierConfigLoader.overrideConfig(subId, bundle, true)
    }
}

data class SimCardInfo(
    val slot: Int,
    val subId: Int,
    val carrierName: String,
    val currentConfig: Map<String, String> = emptyMap()
)
