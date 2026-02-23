package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.devices.MobileDeviceDetail;
import com.cloudmen.cloudguard.dto.devices.MobileDeviceOverviewResponse;
import com.cloudmen.cloudguard.dto.devices.MobileDevicePageResponse;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.admin.directory.model.MobileDevice;
import com.google.api.services.admin.directory.model.MobileDevices;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GoogleMobileDeviceService {
    private final GoogleApiFactory googleApiFactory;

    public GoogleMobileDeviceService(GoogleApiFactory googleApiFactory) {
        this.googleApiFactory = googleApiFactory;
    }

    public MobileDevicePageResponse getMobileDevicesPaged(
            String loggedInEmail, String pageToken, int size,
            String filterStatus, String filterType, boolean isTestMode) {

        try {
            Directory directoryService = googleApiFactory.getDirectoryService(
                    DirectoryScopes.ADMIN_DIRECTORY_DEVICE_MOBILE_READONLY,
                    loggedInEmail
            );

            List<MobileDevice> googleDevices;
            String nextTokenToReturn;

            if (true) {
                // --- MOCK LOGICA ---
                List<MobileDevice> allMockDevices = createMockGoogleDevices();

                // Filters toepassen op de testdata

                if (filterStatus != null && !filterStatus.equals("Alle statussen")) {
                    allMockDevices = allMockDevices.stream()
                            .filter(d -> d.getStatus() != null && d.getStatus().equalsIgnoreCase(filterStatus))
                            .toList();
                }
                if (filterType != null && !filterType.equals("Alle device typen")) {
                    allMockDevices = allMockDevices.stream()
                            .filter(d -> d.getOs() != null && d.getOs().toLowerCase().startsWith(filterType.toLowerCase()))
                            .toList();
                }

                // Paginatie toepassen op de testdata
                int startIndex = 0;
                if (pageToken != null && !pageToken.isEmpty()) {
                    try {
                        startIndex = Integer.parseInt(pageToken);
                    } catch (NumberFormatException e) {
                        startIndex = 0;
                    }
                }

                int totalSize = allMockDevices.size();
                int endIndex = Math.min(startIndex + size, totalSize);

                if (startIndex >= totalSize) {
                    googleDevices = Collections.emptyList();
                } else {
                    googleDevices = allMockDevices.subList(startIndex, endIndex);
                }

                nextTokenToReturn = (endIndex < totalSize) ? String.valueOf(endIndex) : null;

            } else {
                // --- ECHTE GOOGLE API LOGICA ---
                Directory.Mobiledevices.List request = directoryService.mobiledevices().list("my_customer")
                        .setMaxResults(size)
                        .setPageToken(pageToken)
                        .setProjection("FULL");

                // Google Query Builder voor echte filters
                List<String> queryParts = new ArrayList<>();

                if (filterStatus != null && !filterStatus.equals("Alle statussen")) {
                    queryParts.add("status:" + filterStatus);
                }
                if (filterType != null && !filterType.equals("Alle device typen")) {
                    queryParts.add("os:" + filterType + "*");
                }

                if (!queryParts.isEmpty()) {
                    request.setQuery(String.join(" ", queryParts));
                }

                MobileDevices result = request.execute();
                googleDevices = result.getMobiledevices() != null ? result.getMobiledevices() : Collections.emptyList();
                nextTokenToReturn = result.getNextPageToken();
            }

            // --- 2. GEDEELDE MAPPING LOGICA ---
            if (googleDevices.isEmpty()) {
                return new MobileDevicePageResponse(Collections.emptyList(), null);
            }

            List<MobileDeviceDetail> mappedDevices = googleDevices.stream().map(device -> {

                String email = (device.getName() != null && !device.getName().isEmpty()) ? device.getName().get(0) : "Onbekend";
                String userName = email.split("@")[0].replace(".", " ");
                String deviceName = userName.split(" ")[0].toLowerCase() + "'s " + (device.getOs() != null && device.getOs().contains("iOS") ? "iPhone" : "Android");

                String pwdStatus = device.getDevicePasswordStatus();
                boolean lockSecure = "PASSWORD_SET".equalsIgnoreCase(pwdStatus);
                String lockText = lockSecure ? "PIN code/Wachtwoord actief" : "Geen vergrendeling ingesteld";

                String encStatus = device.getEncryptionStatus();
                boolean encSecure = "ENCRYPTED".equalsIgnoreCase(encStatus) || "ENCRYPTING".equalsIgnoreCase(encStatus);
                String encText = encSecure ? "Volledige disk encryptie actief" : "Encryptie niet actief";

                String compStatus = device.getDeviceCompromisedStatus();
                boolean intSecure = "UNCOMPROMISED".equalsIgnoreCase(compStatus);
                String intText = intSecure ? "Play Protect actief, geen root detectie" : "Toestel gecompromitteerd (Root/Jailbreak)";

                String os = device.getOs() != null ? device.getOs() : "Onbekend";
                boolean osSecure = checkOsVersion(os);
                String osText = osSecure ? os + " - Up to date" : os + " - Verouderd";

                int compliance = 0;
                if (lockSecure) compliance += 25;
                if (encSecure) compliance += 25;
                if (intSecure) compliance += 25;
                if (osSecure) compliance += 25;

                String syncTime = device.getLastSync() != null ? DateTimeConverter.convertToTimeAgo(device.getLastSync()) : "Nooit";

                return new MobileDeviceDetail(
                        device.getResourceId(),
                        capitalizeWords(userName),
                        email,
                        deviceName,
                        device.getModel(),
                        os,
                        syncTime,
                        capitalizeWords(device.getStatus()),
                        compliance,
                        lockSecure, lockText,
                        encSecure, encText,
                        osSecure, osText,
                        intSecure, intText
                );
            }).toList();

            return new MobileDevicePageResponse(mappedDevices, nextTokenToReturn);

        } catch (Exception e) {
            throw new RuntimeException("Fout bij ophalen Mobile Devices: " + e.getMessage());
        }
    }

    public List<String> getUniqueDeviceTypes(String loggedInEmail, boolean isTestMode) {
        // --- TEST MODE SWITCH ---
        Set<String> uniqueTypes = new HashSet<>();

        if (isTestMode) {
            // Haal de mock apparaten op
            List<MobileDevice> mockDevices = createMockGoogleDevices();

            for (MobileDevice device : mockDevices) {
                if (device.getOs() != null && !device.getOs().trim().isEmpty()) {
                    // Pak het eerste woord (bijv. "Android" uit "Android 14.0")
                    String baseOs = device.getOs().split(" ")[0];
                    uniqueTypes.add(baseOs);
                }
            }
        } else {
            // --- ECHTE GOOGLE API CALL ---
            try {
                Directory directoryService = googleApiFactory.getDirectoryService(DirectoryScopes.ADMIN_DIRECTORY_DEVICE_MOBILE_READONLY, loggedInEmail);


                String pageToken = null;

                do {
                    Directory.Mobiledevices.List request = directoryService.mobiledevices().list("my_customer")
                            .setPageToken(pageToken)
                            // CRUCIAAL: Haal alleen het OS veld op voor maximale snelheid!
                            .setFields("nextPageToken, mobiledevices(os)");

                    MobileDevices result = request.execute();

                    if (result.getMobiledevices() != null) {
                        for (MobileDevice device : result.getMobiledevices()) {
                            if (device.getOs() != null && !device.getOs().trim().isEmpty()) {
                                String baseOs = device.getOs().split(" ")[0];
                                uniqueTypes.add(baseOs);
                            }
                        }
                    }
                    pageToken = result.getNextPageToken();
                } while (pageToken != null);

            } catch (Exception e) {
                throw new RuntimeException("Fout bij ophalen unieke OS types: " + e.getMessage());
            }
        }

        // Zet de Set om naar een gesorteerde Lijst
        List<String> sortedTypes = new ArrayList<>(uniqueTypes);
        Collections.sort(sortedTypes);

        return sortedTypes;
    }

    public MobileDeviceOverviewResponse getMobileDevicesPageOverview(String loggedInEmail, boolean isTestMode) {
        List<MobileDevice> allDevices = new ArrayList<>();

        if (isTestMode) {
            allDevices = createMockGoogleDevices();
        } else {
            try {
                Directory directoryService = googleApiFactory.getDirectoryService(
                        "https://www.googleapis.com/auth/admin.directory.device.mobile.readonly",
                        loggedInEmail
                );

                String pageToken = null;
                do {
                    Directory.Mobiledevices.List request = directoryService.mobiledevices().list("my_customer")
                            .setPageToken(pageToken)
                            // Haal alleen de velden op die we écht nodig hebben voor de score!
                            .setFields("nextPageToken, mobiledevices(os, status, devicePasswordStatus, encryptionStatus, deviceCompromisedStatus)");

                    MobileDevices result = request.execute();

                    if (result.getMobiledevices() != null) {
                        allDevices.addAll(result.getMobiledevices());
                    }
                    pageToken = result.getNextPageToken();
                } while (pageToken != null);
            } catch (Exception e) {
                throw new RuntimeException("Failed to fetch mobile devices overview from Google: " + e.getMessage());
            }
        }


        long totalDevices = 0;
        long approvedDevices = 0;
        long nonCompliantDevices = 0;
        long totalScoreSum = 0;

        for (MobileDevice device : allDevices) {
            totalDevices++;

            if ("APPROVED".equalsIgnoreCase(device.getStatus())) {
                approvedDevices++;
            }

            boolean lockSecure = "PASSWORD_SET".equalsIgnoreCase(device.getDevicePasswordStatus());
            boolean encSecure = "ENCRYPTED".equalsIgnoreCase(device.getEncryptionStatus()) ||
                    "ENCRYPTING".equalsIgnoreCase(device.getEncryptionStatus());
            boolean intSecure = "UNCOMPROMISED".equalsIgnoreCase(device.getDeviceCompromisedStatus());
            boolean osSecure = checkOsVersion(device.getOs());

            int deviceScore = 0;
            if (lockSecure) deviceScore += 25;
            if (encSecure) deviceScore += 25;
            if (intSecure) deviceScore += 25;
            if (osSecure) deviceScore += 25;

            totalScoreSum += deviceScore;

            if (deviceScore < 75) nonCompliantDevices++;
        }

        int securityScore = totalDevices == 0 ? 0 : (int) Math.round((double) totalScoreSum / totalDevices);

        return new MobileDeviceOverviewResponse(
                totalDevices,
                nonCompliantDevices,
                approvedDevices,
                securityScore
        );

    }

    private boolean checkOsVersion(String os) {
        if (os == null) return false;
        return os.contains("Android 14") || os.contains("Android 13") ||
                os.contains("iOS 17") || os.contains("iOS 16");
    }

    private String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase()).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private List<MobileDevice> createMockGoogleDevices() {
        List<MobileDevice> list = new ArrayList<>();
        long now = System.currentTimeMillis();

        MobileDevice d1 = new MobileDevice();
        d1.setResourceId("dev-test-1");
        d1.setName(Arrays.asList("jan.jansen@bedrijf.nl"));
        d1.setModel("Samsung Galaxy S24");
        d1.setOs("Android 14");
        d1.setDevicePasswordStatus("PASSWORD_SET"); // Veilige PIN
        d1.setEncryptionStatus("ENCRYPTED");        // Encryptie aan
        d1.setDeviceCompromisedStatus("UNCOMPROMISED"); // Geen Root
        d1.setStatus("APPROVED");
        d1.setLastSync(new DateTime(now - 86400000L)); // 1 dag geleden
        list.add(d1);

        MobileDevice d2 = new MobileDevice();
        d2.setResourceId("dev-test-2");
        d2.setName(Arrays.asList("pieter.devries@bedrijf.nl"));
        d2.setModel("Google Pixel 5");
        d2.setOs("Android 11");                     // Verouderd (Geeft 0%)
        d2.setDevicePasswordStatus("NO_PASSWORD");  // Geen PIN (Geeft 0%)
        d2.setEncryptionStatus("UNENCRYPTED");      // Geen encryptie (Geeft 0%)
        d2.setDeviceCompromisedStatus("UNCOMPROMISED"); // Wél veilig (Geeft 25%)
        d2.setStatus("BLOCKED");
        d2.setLastSync(new DateTime(now - (86400000L * 30))); // 30 dagen geleden
        list.add(d2);

        MobileDevice d3 = new MobileDevice();
        d3.setResourceId("dev-test-3");
        d3.setName(Arrays.asList("lisa.smit@bedrijf.nl"));
        d3.setModel("iPhone 15 Pro");
        d3.setOs("iOS 17.2");
        d3.setDevicePasswordStatus("NO_PASSWORD");
        d3.setEncryptionStatus("ENCRYPTED");
        d3.setDeviceCompromisedStatus("COMPROMISED");
        d3.setStatus("APPROVED");
        d3.setLastSync(new DateTime(now - 3600000L)); // 1 uur geleden
        list.add(d3);

        MobileDevice d4 = new MobileDevice();
        d4.setResourceId("dev-test-4");
        d4.setName(Arrays.asList("mark.rutte@bedrijf.nl"));
        d4.setModel("iPhone 8");
        d4.setOs("iOS 15.0");                       // Verouderd (Geeft 0%)
        d4.setDevicePasswordStatus("PASSWORD_SET"); // Wel veilig
        d4.setEncryptionStatus("ENCRYPTED");        // Wel veilig
        d4.setDeviceCompromisedStatus("UNCOMPROMISED"); // Wel veilig
        d4.setStatus("PENDING");                    // In afwachting van goedkeuring
        d4.setLastSync(null); // 3 dagen geleden
        list.add(d4);

        MobileDevice d5 = new MobileDevice();
        d5.setResourceId("dev-test-5");
        d5.setName(Arrays.asList("emma.devries@bedrijf.nl"));
        d5.setModel("iPad Pro 11-inch");
        d5.setOs("iOS 16.5");
        d5.setDevicePasswordStatus("PASSWORD_SET");
        d5.setEncryptionStatus("ENCRYPTED");
        d5.setDeviceCompromisedStatus("COMPROMISED"); // Jailbreak! (Geeft 0%)
        d5.setStatus("BLOCKED");
        d5.setLastSync(new DateTime(now - 600000L)); // 10 min geleden
        list.add(d5);

        MobileDevice d6 = new MobileDevice();
        d6.setResourceId("dev-test-6");
        d6.setName(Arrays.asList("tom.klaassen@bedrijf.nl"));
        d6.setModel("Motorola Edge");
        d6.setOs("Android 14");
        d6.setDevicePasswordStatus("PASSWORD_SET");
        d6.setEncryptionStatus("ENCRYPTING");       // Jouw code vangt dit netjes op als Encrypted!
        d6.setDeviceCompromisedStatus("UNCOMPROMISED");
        d6.setStatus("APPROVED");
        d6.setLastSync(null); // Paar seconden geleden
        list.add(d6);

        return list;
    }
}
