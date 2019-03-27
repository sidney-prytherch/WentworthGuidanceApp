package edu.wit.mobileapp.wentworthguidanceapp;

import android.animation.LayoutTransition;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.Inflater;

public class MapActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    NavigationView navigationView;
    private WifiManager wifiManager;
    private List<ScanResult> results;
    private ArrayList<String> ssidList = new ArrayList<>();
    private final short GRID_HEIGHT = 16;
    private final short GRID_WIDTH = 12;
    private GridNode[][] grid = new GridNode[GRID_WIDTH][GRID_HEIGHT];
    private TableLayout buttonTableLayout;



    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            results = wifiManager.getScanResults();
            unregisterReceiver(this);
            Log.d("myapp", "Size" + results.size());
            for (ScanResult scanResult : results) {
                Log.d("myapp", scanResult.SSID);
                Log.d("myapp", "SSID = \"" + scanResult.SSID + "\", SignalStrength = " + scanResult.level + " dBm, Frequency = " + scanResult.frequency + "MHz");
                ssidList.add(scanResult.SSID);
                Log.d("myapp", "Completed");
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        buttonTableLayout = findViewById(R.id.buttonTableLayout);

        ((ViewGroup) findViewById(R.id.constraintLayout)).getLayoutTransition()
                .enableTransitionType(LayoutTransition.CHANGING);

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "Wifi is disabled... enable wifi....", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        scanWifi();

//        ConstraintLayout main = (ConstraintLayout) findViewById(R.id.constraintLayout);

        int projectID = 1;

        for (int row = 0; row < buttonTableLayout.getChildCount(); ++row){
            ((TableRow) buttonTableLayout.getChildAt(row)).removeAllViews();
        }

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        for (int i = 0; i < GRID_HEIGHT; i++) {

            TableRow currentTableRow = getTableRow(i);

            for (int j = 0; j < GRID_WIDTH; j++) {
                SquareButton newGuessButton = (SquareButton) inflater.inflate(R.layout.grid_square, currentTableRow, false);
                if (
                        i == 0 ||
                        (j == 0 && i > 1 && i < 15) ||
                        ((i == 2 || i == 3) && (j > 1 && j < 10)) ||
                        ((j == 3 || j == 4 || j == 7 || j == 8) && (i > 4 && i < 13)) ||
                        (i == 15 && (j == 10 || (j < 8 && j > 0))) ||
                        (j == 11 && (i == 14 || (i > 1 && i < 12)))
                ) {
                    grid[j][i] = new GridNode(j, i, projectID++);
                } else {
                    grid[j][i] = new GridNode(j, i);
                }
                newGuessButton.setGridNode(grid[j][i]);
                currentTableRow.addView(newGuessButton);
            }
        }

        for (int i = 0; i < GRID_WIDTH; i++) {
            for (int j = 0; j < GRID_HEIGHT; j++) {
                if (j > 0) grid[i][j].up = grid[i][j - 1];
                if (j < GRID_HEIGHT - 1) grid[i][j].down = grid[i][j + 1];
                if (i > 0) grid[i][j].left = grid[i - 1][j];
                if (i < GRID_WIDTH - 1) grid[i][j].right = grid[i + 1][j];
            }
        }
    }
    private TableRow getTableRow(int row) {
        return (TableRow) buttonTableLayout.getChildAt(row);
    }

    public void onClick(View v) {
        GridNode gridNode = ((SquareButton) v).getGridNode();
        Toast.makeText(this, "Project: " + gridNode.id, Toast.LENGTH_SHORT).show();
    }

    private void scanWifi() {
        ssidList.clear();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
        Toast.makeText(this, "Scanning Wifi.....", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_slideshow) {
            scanWifi();
        } else if (id == R.id.nav_manage) {
            calculatePosition(0, 0, 0);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void showNavDrawer(View view) {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (!drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.openDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private double calculateDistance(double db, double freq) {
        return 20 * (Math.log10(freq) + Math.abs(db)) - 27.55;
    }

    private Coord getCordinate(double x1, double y1, double r1,
                               double x2, double y2, double r2,
                               double x3, double y3, double r3) {
        //double identity[][] =  {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};

        double delta = 4 * (((x1 - x2) * (y1 - y2)) - ((x1 - x3) * (y1 - y2)));
        double a = Math.pow(r2, 2) - Math.pow(r1, 2) - Math.pow(x2, 2) + Math.pow(x1, 2) - Math.pow(y2, 2) + Math.pow(y1, 2);
        double b = Math.pow(r3, 2) - Math.pow(r1, 2) - Math.pow(x3, 2) + Math.pow(x1, 2) - Math.pow(y3, 2) + Math.pow(y1, 2);

        double x = (1 / delta) * (2 * a * (y1 - y3) - 2 * b * (y1 - y2));
        double y = (1 / delta) * (2 * b * (x1 - x2) - 2 * a * (x1 - x2));

        Coord coord = new Coord((int) x, (int) y);

        return coord;
    }

    private Coord calculatePosition(int DSSI1, int DSSI2, int DSSI3) {


        ImageView marker = (ImageView) findViewById(R.id.marker);
        double locationX = 4.0;
        double locationY = 4.0;
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) marker.getLayoutParams();
        params.horizontalBias = (float) (locationX / 16.0); // here is one modification for example. modify anything else you want :)
        params.verticalBias = (float) (locationY / 16.0);
        marker.setLayoutParams(params); // request the view to use the new modified params


        return new Coord(0, 0);

    }

    private class Coord {
        public int x;
        public int y;

        public Coord(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
