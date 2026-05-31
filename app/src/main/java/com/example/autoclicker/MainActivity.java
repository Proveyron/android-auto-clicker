package com.example.autoclicker;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnOverlay;
    private Button btnAccessibility;
    private Button btnStart;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnOverlay = findViewById(R.id.btnOverlayPermission);
        btnAccessibility = findViewById(R.id.btnAccessibilityPermission);
        btnStart = findViewById(R.id.btnStartService);
        status = findViewById(R.id.permissionStatus);

        btnOverlay.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });

        btnAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(this, AutoClickerService.class);
            startService(intent);
            Toast.makeText(this, "Floating controls shown", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
    }

    private void checkPermissions() {
        boolean overlayGranted = Settings.canDrawOverlays(this);
        boolean accessibilityEnabled = isAccessibilityServiceEnabled(this, AutoClickerService.class);

        btnOverlay.setEnabled(!overlayGranted);
        btnOverlay.setText(overlayGranted ? "Overlay Permission Granted" : "Grant Overlay Permission");

        btnAccessibility.setEnabled(!accessibilityEnabled);
        btnAccessibility.setText(accessibilityEnabled ? "Accessibility Service Enabled" : "Grant Accessibility Permission");

        if (overlayGranted && accessibilityEnabled) {
            status.setText("All permissions granted! You can now start the service.");
            btnStart.setVisibility(View.VISIBLE);
        } else {
            status.setText("Please grant the required permissions to continue.");
            btnStart.setVisibility(View.GONE);
        }
    }

    private boolean isAccessibilityServiceEnabled(Context context, Class<?> service) {
        String serviceName = context.getPackageName() + "/" + service.getName();
        String settingValue = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (settingValue != null) {
            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
            splitter.setString(settingValue);
            while (splitter.hasNext()) {
                if (splitter.next().equalsIgnoreCase(serviceName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
