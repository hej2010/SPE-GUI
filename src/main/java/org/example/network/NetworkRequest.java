package org.example.network;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

public class NetworkRequest {
    private static final int DEFAULT_TIMEOUT = 5000;
    private final URL url;
    private final Map<String, String> parameters;
    private int connectionTimeout = DEFAULT_TIMEOUT;
    private int readTimeout = DEFAULT_TIMEOUT;

    public NetworkRequest(@NotNull String url, @Nullable Map<String, String> parameters) throws MalformedURLException {
        this.url = new URL(url);
        this.parameters = parameters;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * Starts a network connection, optionally sends {@code parameters} and returns a {@code String} response.
     *
     * @return A {@code String} response, or {@code null} if the connection failed
     */
    public String run() throws IOException {
        URLConnection connection = url.openConnection();
        connection.setDoOutput(parameters != null);
        connection.setConnectTimeout(connectionTimeout);
        connection.setReadTimeout(readTimeout);
        if (parameters != null) {
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            out.writeBytes(getParamsString(parameters));
            out.flush();
            out.close();
        }
        return getData(connection);
    }

    private String getData(URLConnection connection) throws IOException { // TODO api: https://graphite.readthedocs.io/en/latest/render_api.html
        // /metrics/find?query=* gets all top-level queries
        // /metrics/index.json returns all querys
        // https://graphite.readthedocs.io/en/latest/metrics_api.html#metrics-expand
        connection.connect();
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line).append("\n");
        }
        br.close();
        return sb.toString();
    }

    @NotNull
    public static String getParamsString(Map<String, String> params) {
        if (params == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();

        Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
        Map.Entry<String, String> m;
        while (it.hasNext()) {
            m = it.next();
            result.append(URLEncoder.encode(m.getKey(), StandardCharsets.UTF_8));
            result.append("=");
            result.append(URLEncoder.encode(m.getValue(), StandardCharsets.UTF_8));
            if (it.hasNext()) {
                result.append("&");
            }
        }
        return result.toString();
    }

    /*private void startServerListener() {
        new Thread(() -> {
            ServerSocket listener;
            try {
                listener = new ServerSocket(2004, 0, InetAddress.getByName("localhost"));
                System.out.println("new server socket");
                while (true) {
                    Socket socket = listener.accept();
                    System.out.println("Socket accept");
                    new Thread(() -> {
                        try {
                            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = br.readLine()) != null) {
                                sb.append(line);
                            }
                            br.close();
                            System.out.println(sb.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

    }*/

}
