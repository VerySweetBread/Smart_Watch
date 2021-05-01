package meow.sweetbread.smartwatch;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static java.lang.Math.abs;

public class WiFiActivity extends AppCompatActivity {
    TextView status;
    EditText ip_adr;
    Editable ip;
    TextView logs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);

        ip_adr = findViewById(R.id.ip_address);
        status = findViewById(R.id.status);
        logs = findViewById(R.id.logs);
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart) +
                            source.subSequence(start, end) +
                            destTxt.substring(dend);
                    if (!resultingTxt.matches ("^\\d{1,3}(\\." +
                            "(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i=0; i<splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }

                return null;
            }
        };
        ip_adr.setFilters(filters);
    }

    public void connect(View v) {
        ip = ip_adr.getText();
        logs.append(ip + "\n");

        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest =  new StringRequest(Request.Method.GET, "http://" + ip + "/meow",
                new Response.Listener<String>() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onResponse(String response) {
                        if (response.equals("meow")) {
                            status.setText("Подключено");
                            logs.append("Connected\n");
                            checkTime();
                        } else {
                            logs.append(response + "\n");
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                logs.append(error + "\n");
            }
        });
        queue.add(stringRequest);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    void checkTime() {
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, "http://" + ip + "/time",
                response -> {
                    logs.append("Проверка... ");

                    Date currentDate = new Date();
                    SimpleDateFormat formatForDateNow =
                            new SimpleDateFormat("HH:mm:ss dd.MM.yyyy");
                    Date watchDate = null;
                    try {
                        watchDate = formatForDateNow.parse(response.replace("time: ", ""));
                    } catch (ParseException e) {
                       logs.append((CharSequence) e);
                    }
                    int different = (int) (currentDate.getTime() - watchDate.getTime()) / 1000;

                    logs.append("Текущее время " + formatForDateNow.format(currentDate) + "\n");
                    logs.append("В миллисекундах: " + currentDate.getTime() + "\n");
                    logs.append("Время с часов: " + formatForDateNow.format(watchDate) + "\n");
                    logs.append("Время с часов в мс: " + watchDate.getTime() + "\n");
                    logs.append("Разница в секундах: " + abs(different) + "\n");

                    if (abs(different) > 10) {
                        logs.append("Синхронизация... ");
                        sendTime(String.valueOf(java.time.Instant.now().getEpochSecond() + 3 * 60 * 60));
                    }
                },
                error -> logs.append((CharSequence) error));
        queue.add(stringRequest);
    }

    void sendTime(String time) {
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, "http://" + ip + "/set_time?time=" + time,
                response -> logs.append("Successful\n"),
                error -> logs.append((CharSequence) error)
        );
        queue.add(stringRequest);
    }
}