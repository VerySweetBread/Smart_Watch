package meow.sweetbread.smartwatch;

import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.harrysoft.androidbluetoothserial.BluetoothManager;
import com.harrysoft.androidbluetoothserial.BluetoothSerialDevice;
import com.harrysoft.androidbluetoothserial.SimpleBluetoothDeviceInterface;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static java.lang.Math.abs;

public class BluetoothActivity extends AppCompatActivity {
    SimpleBluetoothDeviceInterface deviceInterface;
    BluetoothManager bluetoothManager;
    TextView logs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        logs = findViewById(R.id.logs);
        ScrollView mScrollView = findViewById(R.id.mScrollView);

        mScrollView.fullScroll(View.FOCUS_DOWN);

        bluetoothManager = BluetoothManager.getInstance();
        if (bluetoothManager == null) {
            // Bluetooth unavailable on this device :( tell the user
            Toast.makeText(this, "Bluetooth not available.", Toast.LENGTH_LONG).show(); // Replace context with your context instance.
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void listBluetoothCon(View v) {
        Collection<BluetoothDevice> pairedDevices = bluetoothManager.getPairedDevicesList();
        LinearLayout list = findViewById(R.id.device_list);
        list.removeAllViews();

        for (BluetoothDevice device : pairedDevices) {
            Log.d("meow", "Device name: " + device.getName());
            Log.d("meow", "Device MAC Address: " + device.getAddress());

            Button device_button = new Button(getApplicationContext());
            device_button.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT)
            );
            device_button.setTag(device.getAddress());
            device_button.setOnClickListener(this::connect);
            device_button.setText(device.getName());
            list.addView(device_button);
        }
    }

    public void connect(View v) {
        String mac = (String) v.getTag();
        bluetoothManager.openSerialDevice(mac)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onConnected, this::onError);
    }

    private void onConnected(BluetoothSerialDevice connectedDevice) {
        // You are now connected to this device!
        // Here you may want to retain an instance to your device:
        deviceInterface = connectedDevice.toSimpleDeviceInterface();

        // Listen to bluetooth events
        deviceInterface.setListeners(this::onMessageReceived, this::onMessageSent, this::onError);

        // Let's send a message:
        deviceInterface.sendMessage("init");
    }

    private void onMessageSent(String s) {
        logs.append("Отп: " + s + "\n");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void onMessageReceived(String s) {
        logs.append("Пол: " + s + "\n");
        if (s.equals("Meow!")) {
            logs.append("Получен код инициализации. Соединение успешно\n");
            deviceInterface.sendMessage("time");
        } else if (s.startsWith("time: ")) {
            logs.append("Проверка... ");

            Date currentDate = new Date();
            SimpleDateFormat formatForDateNow =
                    new SimpleDateFormat("HH:mm:ss dd.MM.yyyy");
            Date watchDate = null;
            try {
                watchDate = formatForDateNow.parse(s.replace("time: ", ""));
            } catch (ParseException e) {
                onError(e);
            }
            int different = (int) (currentDate.getTime() - watchDate.getTime()) / 1000;

            logs.append("Текущее время " + formatForDateNow.format(currentDate) + "\n");
            logs.append("В миллисекундах: " + currentDate.getTime() + "\n");
            logs.append("Время с часов: " + formatForDateNow.format(watchDate) + "\n");
            logs.append("Время с часов в мс: " + watchDate.getTime() + "\n");
            logs.append("Разница в секундах: " + abs(different) + "\n");

            if (abs(different) > 10) {
                logs.append("Синхронизация... ");
                deviceInterface.sendMessage("sync_time");
            } else {
                logs.append("Успешно\n");
            }
        } else if (s.equals("sync_time")) {
            long seconds = java.time.Instant.now().getEpochSecond() + 3 * 60 * 60;
            Log.d("meow", "onMessageReceived: " + seconds);

            deviceInterface.sendMessage(String.valueOf(seconds));
        }
    }

    private void onError(Throwable throwable) {
        logs.append("Ошибка: " + throwable + "\n");
    }
}