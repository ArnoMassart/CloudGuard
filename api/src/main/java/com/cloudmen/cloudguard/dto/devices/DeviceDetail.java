package com.cloudmen.cloudguard.dto.devices;

/**
 * A Data Transfer Object (DTO) representing detailed information and security compliance status of a single device. <p>
 *
 * This record encapsulates hardware details, user assignment, synchronization status, and specific security checks
 * (such as screen lock, encryption, OS updates, and integrity) along with their corresponding descriptive texts.
 *
 * @param resourceId        the unique identifier for the device
 * @param deviceType        the category or type of the device (e.g., MOBILE, CHROME_OS)
 * @param userName          the display name of the assigned user
 * @param userEmail         the email address of the assigned user
 * @param deviceName        the given name or hostname of the device
 * @param model             the hardware model of the device
 * @param os                the operating system and version
 * @param lastSync          a formatted string indicating when the device last synced with the server
 * @param status            the current operational or compliance status of the device
 * @param complianceScore   a numerical score representing the device's overall security health
 * @param lockSecure        {@code true} if the screen lock meets the organization's security requirements
 * @param screenLockText    descriptive text explaining the screen lock status
 * @param encSecure         {@code true} if the device encryption meets security requirements
 * @param encryptionText    descriptive text explaining the encryption text
 * @param osSecure          {@code true} if the operating system is up-to-date and secure
 * @param osText            descriptive text explaining the OS security status
 * @param intSecure         {@code true} if the device integrity is intact (e.g., not rooted or jailbroken)
 * @param integrityText     descriptive text explaining the device integrity status
 */
public record DeviceDetail(
        String resourceId,
        String deviceType,
        String userName,
        String userEmail,
        String deviceName,
        String model,
        String os,
        String lastSync,
        String status,
        int complianceScore,
        boolean lockSecure,
        String screenLockText,
        boolean encSecure,
        String encryptionText,
        boolean osSecure,
        String osText,
        boolean intSecure,
        String integrityText
) {
}
