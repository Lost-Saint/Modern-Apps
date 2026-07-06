package com.vayunmathur.findfamily.uwb

/**
 * Apple's "UWB Specification for Third-Party Accessories" protocol — the bytes
 * that flow between an iPhone (running `NINearbyAccessoryConfiguration`) and a
 * UWB accessory. In the FindFamily cross-platform pairing, the Android device
 * plays the accessory role.
 *
 * The byte format is specified in:
 *   - Apple's PDF "Implementing Spatial Interactions with Third-Party Devices"
 *     (developer.apple.com/nearby-interaction → "Specification")
 *   - FiRa MAC Technical Requirements (TLV-encoded MAC params)
 *
 * The encode function below is only a compatibility placeholder. Parsing
 * iOS shareable configuration data is not currently supported, and the UI
 * routing disables Android/iOS UWB sessions until that protocol is complete.
 */
object UwbAccessoryProtocol {

    /**
     * Generate the "accessory configuration data" blob to send to iOS at
     * session start. Must encode this device's UWB MAC parameters per
     * Apple's accessory spec.
     */
    fun encodeAccessoryConfigurationData(localAddress: ByteArray): ByteArray {
        // TODO(uwb-cross-platform): Implement per Apple's spec.
        // The blob is roughly: [protocol-version u8][profile-id u8][TLV...].
        return byteArrayOf(0x01, 0x00) + localAddress
    }

    /**
     * Parse the "shareableConfigurationData" blob received from iOS into the
     * concrete FiRa parameters this device should use as controlee.
     */
    fun parseShareableConfigurationData(shareableConfigData: ByteArray): Parsed? = null

    data class Parsed(
        val peerAddress: ByteArray,
        val sessionId: Int,
        val sessionKey: ByteArray,
        val channelNumber: Int,
        val preambleIndex: Int,
    )
}
