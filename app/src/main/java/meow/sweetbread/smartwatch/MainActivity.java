package meow.sweetbread.smartwatch;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void wifi_func(View v) {
       Intent i = new Intent(MainActivity.this, WiFiActivity.class);
        startActivity(i);
    }

    public void bt_func(View v) {
        Intent i = new Intent(MainActivity.this, BluetoothActivity.class);
        startActivity(i);
    }
}