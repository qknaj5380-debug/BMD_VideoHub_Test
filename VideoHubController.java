import android.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoHubController {

    public interface VideoHubListener {
        void onConnectionStatusChanged(boolean isConnected);
        void onDeviceInfoReceived(String info);
        void onRoutingStatusReceived(List<String> routes);
        void onError(String errorMessage);
    }

    private static final String TAG = "VideoHubController";
    private static final int PORT = 9990; // BMD VideoHub默认控制端口
    
    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private VideoHubListener listener;
    private String deviceIp;
    
    public VideoHubController(String ip) {
        this.deviceIp = ip;
    }
    
    public void setListener(VideoHubListener listener) {
        this.listener = listener;
    }
    
    public void connect() {
        executor.execute(() -> {
            try {
                socket = new Socket(deviceIp, PORT);
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                if (listener != null) {
                    listener.onConnectionStatusChanged(true);
                }
                
                // 发送初始查询命令
                sendCommand("VIDEOHUB DEVICE:\n");
                sendCommand("VIDEO OUTPUT ROUTING:\n");
                
                // 启动监听线程
                startListening();
                
            } catch (IOException e) {
                Log.e(TAG, "Connection error", e);
                if (listener != null) {
                    listener.onError("Connection failed: " + e.getMessage());
                }
                disconnect();
            }
        });
    }
    
    private void startListening() {
        executor.execute(() -> {
            try {
                String line;
                List<String> routes = new ArrayList<>();
                StringBuilder deviceInfo = new StringBuilder();
                
                while ((line = reader.readLine()) != null) {
                    Log.d(TAG, "Received: " + line);
                    
                    if (line.startsWith("VIDEOHUB DEVICE:")) {
                        // 设备信息
                        deviceInfo.append(line).append("\n");
                        while (!(line = reader.readLine()).isEmpty()) {
                            deviceInfo.append(line).append("\n");
                        }
                        if (listener != null) {
                            listener.onDeviceInfoReceived(deviceInfo.toString());
                        }
                    } 
                    else if (line.startsWith("VIDEO OUTPUT ROUTING:")) {
                        // 路由信息
                        routes.clear();
                        while (!(line = reader.readLine()).isEmpty()) {
                            routes.add(line);
                        }
                        if (listener != null) {
                            listener.onRoutingStatusReceived(routes);
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Read error", e);
                if (listener != null) {
                    listener.onError("Read error: " + e.getMessage());
                }
            } finally {
                disconnect();
            }
        });
    }
    
    public void setRoute(int output, int input) {
        executor.execute(() -> {
            try {
                String command = String.format("VIDEO OUTPUT ROUTING:\n%d %d\n", output, input);
                sendCommand(command);
                
                // 发送查询命令以确认更改
                sendCommand("VIDEO OUTPUT ROUTING:\n");
                
            } catch (Exception e) {
                Log.e(TAG, "Set route error", e);
                if (listener != null) {
                    listener.onError("Set route failed: " + e.getMessage());
                }
            }
        });
    }
    
    private void sendCommand(String command) throws IOException {
        if (writer != null) {
            writer.write(command);
            writer.flush();
            Log.d(TAG, "Sent: " + command);
        }
    }
    
    public void disconnect() {
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            Log.e(TAG, "Disconnect error", e);
        }
        
        if (listener != null) {
            listener.onConnectionStatusChanged(false);
        }
    }
}
