package csm.csm.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class SessionAPI implements Accessor {

    public static String[] getProfileInfo(String token) throws IOException {
        URL url = new URL("https://api.minecraftservices.com/minecraft/profile");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("HTTP " + responseCode);
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) content.append(line);
        in.close();
        JsonObject json = JsonParser.parseString(content.toString()).getAsJsonObject();
        String ign = json.get("name").getAsString();
        String uuid = json.get("id").getAsString().replaceAll("-", "");
        return new String[]{ign, uuid};
    }

    public static boolean validateSession(String token) {
        try {
            String[] profileInfo = getProfileInfo(token);
            String ign = profileInfo[0];
            String uuid = profileInfo[1];
            return ign.equals(mc.getUser().getName())
                    && uuid.equals(mc.getUser().getProfileId().toString().replace("-", ""));
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean checkOnline(String uuid) {
        try {
            URL url = new URL("https://api.slothpixel.me/api/players/" + uuid);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() != 200) return false;
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) content.append(line);
            in.close();
            JsonObject json = JsonParser.parseString(content.toString()).getAsJsonObject();
            return json.get("online").getAsBoolean();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static UUID undashedToUUID(String uuid) {
        return UUID.fromString(
                uuid.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                        "$1-$2-$3-$4-$5"
                )
        );
    }

    public static int changeName(String newName, String token) throws IOException {
        URL url = new URL("https://api.minecraftservices.com/minecraft/profile/name/" + newName);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        return conn.getResponseCode();
    }

    public static int changeSkin(String url, String token) throws IOException {
        URL endpoint = new URL("https://api.minecraftservices.com/minecraft/profile/skins");
        HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("Content-Type", "application/json");
        String jsonString = String.format("{\"variant\":\"classic\",\"url\":\"%s\"}", url);
        conn.setDoOutput(true);
        conn.getOutputStream().write(jsonString.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return conn.getResponseCode();
    }
}