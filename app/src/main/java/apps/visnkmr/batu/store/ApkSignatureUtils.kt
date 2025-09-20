package apps.visnkmr.batu.store

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import java.io.File
import java.security.MessageDigest
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for APK signature verification and information extraction
 */
class ApkSignatureUtils(private val context: Context) {

    /**
     * Data class to hold signature information
     */
    data class SignatureInfo(
        val isSigned: Boolean,
        val signatureHash: String?,
        val certificateInfo: String?,
        val issuer: String?,
        val subject: String?,
        val validFrom: String?,
        val validUntil: String?,
        val serialNumber: String?,
        val algorithm: String?
    )

    /**
     * Get signature information for an APK file
     */
    fun getSignatureInfo(apkPath: String): SignatureInfo {
        return try {
            val signatures = getApkSignatures(apkPath)

            if (signatures.isEmpty()) {
                SignatureInfo(
                    isSigned = false,
                    signatureHash = null,
                    certificateInfo = null,
                    issuer = null,
                    subject = null,
                    validFrom = null,
                    validUntil = null,
                    serialNumber = null,
                    algorithm = null
                )
            } else {
                val cert = extractCertificate(signatures[0])
                val hash = getSignatureHash(signatures[0])

                SignatureInfo(
                    isSigned = true,
                    signatureHash = hash,
                    certificateInfo = formatCertificateInfo(cert),
                    issuer = cert?.issuerDN?.name,
                    subject = cert?.subjectDN?.name,
                    validFrom = cert?.notBefore?.let { formatDate(it) },
                    validUntil = cert?.notAfter?.let { formatDate(it) },
                    serialNumber = cert?.serialNumber?.toString(16)?.uppercase(),
                    algorithm = cert?.sigAlgName
                )
            }
        } catch (e: Exception) {
            SignatureInfo(
                isSigned = false,
                signatureHash = null,
                certificateInfo = "Error: ${e.message}",
                issuer = null,
                subject = null,
                validFrom = null,
                validUntil = null,
                serialNumber = null,
                algorithm = null
            )
        }
    }

    /**
     * Compare signatures between two APK files
     */
    fun areSignaturesEqual(apkPath1: String, apkPath2: String): Boolean {
        return try {
            val signatures1 = getApkSignatures(apkPath1)
            val signatures2 = getApkSignatures(apkPath2)

            signatures1.size == signatures2.size && signatures1.zip(signatures2).all { (sig1, sig2) ->
                sig1.hashCode() == sig2.hashCode() && sig1.toByteArray().contentEquals(sig2.toByteArray())
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get raw signatures from APK
     */
    private fun getApkSignatures(apkPath: String): Array<Signature> {
        val packageInfo = context.packageManager.getPackageArchiveInfo(
            apkPath,
            PackageManager.GET_SIGNATURES
        )

        return packageInfo?.signatures ?: emptyArray()
    }

    /**
     * Extract X.509 certificate from signature
     */
    private fun extractCertificate(signature: Signature): X509Certificate? {
        return try {
            val certFactory = java.security.cert.CertificateFactory.getInstance("X.509")
            certFactory.generateCertificate(signature.toByteArray().inputStream()) as? X509Certificate
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generate SHA-256 hash of signature
     */
    private fun getSignatureHash(signature: Signature): String? {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(signature.toByteArray())
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Format certificate information for display
     */
    private fun formatCertificateInfo(cert: X509Certificate?): String? {
        if (cert == null) return null

        return try {
            """
            Subject: ${cert.subjectDN}
            Issuer: ${cert.issuerDN}
            Serial: ${cert.serialNumber.toString(16).uppercase()}
            Algorithm: ${cert.sigAlgName}
            Valid: ${formatDate(cert.notBefore)} - ${formatDate(cert.notAfter)}
            """.trimIndent()
        } catch (e: Exception) {
            "Certificate parsing error: ${e.message}"
        }
    }

    /**
     * Format date for display
     */
    private fun formatDate(date: Date): String {
        return try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            date.toString()
        }
    }
}