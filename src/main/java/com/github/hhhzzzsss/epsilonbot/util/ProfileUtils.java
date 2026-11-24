package com.github.hhhzzzsss.epsilonbot.util;

import com.google.gson.Gson;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ProfileUtils {
    public static Gson gson = new Gson();

    @Data
    public static class PlayerProfileResponse {
        UUID uuid = null;
        String username = null;
    }

    public static PlayerProfileResponse getPlayerProfile(String identifier) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + identifier);
        InputStreamReader isr = new InputStreamReader(DownloadUtils.DownloadToOutputStream(url, 1024*1024));
        PlayerProfileResponse response = null;
        try {
            response = gson.fromJson(isr, PlayerProfileResponse.class);
        } catch (Exception e) {
            throw new IOException("Player not found");
        }
        if (response.getUuid() == null || response.getUsername() == null) {
            throw new IOException("Player not found");
        }
        return response;
    }
}
