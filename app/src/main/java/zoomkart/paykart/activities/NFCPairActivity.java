package zoomkart.paykart.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.gson.Gson;
import com.pusher.client.Pusher;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.SubscriptionEventListener;

import org.json.JSONObject;

import java.util.Date;

import io.paperdb.Paper;
import zoomkart.paykart.R;
import zoomkart.paykart.models.Customer;
import zoomkart.paykart.models.Item;
import zoomkart.paykart.models.Order;
import zoomkart.paykart.models.ZoomKart;

public class NFCPairActivity extends AppCompatActivity {

    NfcAdapter mNfcAdapter;
    SharedPreferences mSharedPreferences;
    int orderId;
    String customerId;
    Pusher pusher;
    Channel channel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfcpair);
        Context context = getApplicationContext();
        getSupportActionBar().setTitle("Tap + Checkout");

        Paper.init(this);

        mSharedPreferences= context.getSharedPreferences("NFC", Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean("isNFCOn", true);
        editor.commit();

        pusher = new Pusher("d6f65af47015b83ec19b");
        channel = pusher.subscribe("channel");

        channel.bind("items-event", new SubscriptionEventListener() {
            @Override
            public void onEvent(String s, String s1, String s2) {
                Item[] items = (new Gson()).fromJson(s2, Item[].class);
                Date date = new Date();
                float totalPrice = 0.0f;
                for (int i = 0; i < items.length; i++){
                    totalPrice += items[i].getPrice();
                    orderId = items[i].getOrderId();
                }
                final Customer customer = ZoomKart.getCustomer();
                customerId = customer.getId();
                Order order = new Order(orderId, customerId, date, totalPrice, items);

                Paper.book(customerId).write("Order ID: " + orderId + "\nDate: " + date.toString(), order);
                Paper.book(customerId).write("CurrentOrder", order);


                for (int i = 0; i < items.length; i++){
                    Log.d("[NFCPairActivity] Item", items[i].getName());
                }

                //Reset validRead
                editor.putBoolean("validRead", false);
                editor.commit();

                startBillActivity(order);
            }
        });

        pusher.connect();

        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        LinearLayout cancelNFCTapButton = (LinearLayout) findViewById(R.id.cancel_button);
        cancelNFCTapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pusher.unsubscribe("channel");
                pusher.disconnect();
                Intent intent = new Intent(NFCPairActivity.this, HomepageActivity.class);
                NFCPairActivity.this.startActivity(intent);
                finish();
            }
        });

    }

    @Override
    public void onBackPressed() {
    }

    private void startBillActivity(Order order){
        pusher.unsubscribe("channel");
        pusher.disconnect();
        Intent i = new Intent(this, BillActivity.class);
        startActivity(i);
        finish();
    }
}
