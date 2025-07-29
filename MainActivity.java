import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class MainActivity extends AppCompatActivity 
        implements VideoHubController.VideoHubListener {

    private EditText etIpAddress;
    private Button btnConnect;
    private Spinner spinnerOutput;
    private Spinner spinnerInput;
    private Button btnSetRoute;
    private TextView tvStatus;
    private ProgressBar progressBar;
    
    private VideoHubController videoHubController;
    private boolean isConnected = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化UI组件
        etIpAddress = findViewById(R.id.etIpAddress);
        btnConnect = findViewById(R.id.btnConnect);
        spinnerOutput = findViewById(R.id.spinnerOutput);
        spinnerInput = findViewById(R.id.spinnerInput);
        btnSetRoute = findViewById(R.id.btnSetRoute);
        tvStatus = findViewById(R.id.tvStatus);
        progressBar = findViewById(R.id.progressBar);
        
        // 设置连接按钮点击事件
        btnConnect.setOnClickListener(v -> toggleConnection());
        
        // 设置路由按钮点击事件
        btnSetRoute.setOnClickListener(v -> setRoute());
        
        // 初始化路由下拉菜单
        initializeSpinners();
    }
    
    private void toggleConnection() {
        if (isConnected) {
            disconnect();
        } else {
            connect();
        }
    }
    
    private void connect() {
        String ip = etIpAddress.getText().toString().trim();
        if (ip.isEmpty()) {
            tvStatus.setText("请输入IP地址");
            return;
        }
        
        showProgress(true);
        tvStatus.setText("正在连接...");
        
        // 创建并连接控制器
        videoHubController = new VideoHubController(ip);
        videoHubController.setListener(this);
        videoHubController.connect();
    }
    
    private void disconnect() {
        if (videoHubController != null) {
            videoHubController.disconnect();
        }
        isConnected = false;
        updateUIState();
    }
    
    private void setRoute() {
        if (!isConnected) {
            tvStatus.setText("请先连接到设备");
            return;
        }
        
        int output = spinnerOutput.getSelectedItemPosition();
        int input = spinnerInput.getSelectedItemPosition();
        
        if (videoHubController != null) {
            videoHubController.setRoute(output, input);
        }
    }
    
    private void initializeSpinners() {
        // 创建模拟输入输出列表
        String[] ports = new String[12];
        for (int i = 0; i < ports.length; i++) {
            ports[i] = "端口 " + (i + 1);
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, ports);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        spinnerOutput.setAdapter(adapter);
        spinnerInput.setAdapter(adapter);
    }
    
    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnConnect.setEnabled(!show);
    }
    
    private void updateUIState() {
        btnConnect.setText(isConnected ? R.string.disconnect : R.string.connect);
        btnSetRoute.setEnabled(isConnected);
        spinnerOutput.setEnabled(isConnected);
        spinnerInput.setEnabled(isConnected);
        etIpAddress.setEnabled(!isConnected);
    }
    
    @Override
    public void onConnectionStatusChanged(boolean isConnected) {
        this.isConnected = isConnected;
        runOnUiThread(() -> {
            showProgress(false);
            updateUIState();
            if (isConnected) {
                tvStatus.setText(R.string.connected);
            } else {
                tvStatus.setText("连接已断开");
            }
        });
    }
    
    @Override
    public void onDeviceInfoReceived(String info) {
        runOnUiThread(() -> {
            tvStatus.setText("设备信息:\n" + info);
        });
    }
    
    @Override
    public void onRoutingStatusReceived(List<String> routes) {
        runOnUiThread(() -> {
            StringBuilder status = new StringBuilder("当前路由状态:\n");
            for (String route : routes) {
                String[] parts = route.split(" ");
                if (parts.length >= 2) {
                    int output = Integer.parseInt(parts[0]);
                    int input = Integer.parseInt(parts[1]);
                    status.append("输出 ").append(output)
                          .append(" → 输入 ").append(input).append("\n");
                }
            }
            tvStatus.setText(status.toString());
        });
    }
    
    @Override
    public void onError(String errorMessage) {
        runOnUiThread(() -> {
            showProgress(false);
            tvStatus.setText(errorMessage);
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoHubController != null) {
            videoHubController.disconnect();
        }
    }
}
