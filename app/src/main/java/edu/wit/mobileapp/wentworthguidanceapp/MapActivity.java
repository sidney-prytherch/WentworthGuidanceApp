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

    private NavigationView navigationView;
    private WifiManager wifiManager;
    private List<ScanResult> results;
    private ArrayList<String> ssidList = new ArrayList<>();
    private final short GRID_HEIGHT = 16;
    private final short GRID_WIDTH = 12;
    private GridNode[][] grid = new GridNode[GRID_WIDTH][GRID_HEIGHT];
    private GridNodePathData[][] optimalPaths = new GridNodePathData[GRID_WIDTH][GRID_HEIGHT];
    private GridNode[] projects;
    private GridNode currentLocation;
    private GridNode destination;
    private TableLayout buttonTableLayout;
    private ImageView marker;
    private ConstraintLayout.LayoutParams markerParams;


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
        ArrayList<GridNode> projects = new ArrayList<>();

        for (int row = 0; row < buttonTableLayout.getChildCount(); ++row) {
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
                    grid[j][i] = new GridNode(j, i, projectID++, newGuessButton);
                    projects.add(grid[j][i]);
                } else {
                    grid[j][i] = new GridNode(j, i, newGuessButton);
                }
                newGuessButton.setGridNode(grid[j][i]);
                currentTableRow.addView(newGuessButton);
            }
        }

        GridNode[] projectsArray = new GridNode[projects.size()];
        for (int i = 0; i < projects.size(); i++) {
            projectsArray[i] = projects.get(i);
        }

        this.projects = projectsArray;

        for (int i = 0; i < GRID_WIDTH; i++) {
            for (int j = 0; j < GRID_HEIGHT; j++) {
                if (j > 0) grid[i][j].up = grid[i][j - 1];
                if (j < GRID_HEIGHT - 1) grid[i][j].down = grid[i][j + 1];
                if (i > 0) grid[i][j].left = grid[i - 1][j];
                if (i < GRID_WIDTH - 1) grid[i][j].right = grid[i + 1][j];
            }
        }

        marker = findViewById(R.id.marker);
        markerParams = (ConstraintLayout.LayoutParams) marker.getLayoutParams();
    }


    private TableRow getTableRow(int row) {
        return (TableRow) buttonTableLayout.getChildAt(row);
    }

    public void onClick(View v) {
        GridNode gridNode = ((SquareButton) v).getGridNode();
        if (gridNode.id != -1) {
            Toast.makeText(this, "Project: " + gridNode.id, Toast.LENGTH_SHORT).show();
            setDestination(gridNode);
        } else {
            moveMarker(gridNode.x, gridNode.y);
            currentLocation = gridNode;
            paintPath();
        }
    }

    private void setDestination(GridNode dest) {
        this.destination = dest;
        optimalPaths = new GridNodePathData[GRID_WIDTH][GRID_HEIGHT];
        if (dest != null) {
            calculateRoutes();
        }
    }

    private void calculateRoutes() {
        Direction[] directions = destination.getValidDirections();
        for (Direction direction : directions) {
            if (direction != null) {
                GridNode firstNode = destination.getNeighbor(direction);
                optimalPaths[firstNode.x][firstNode.y] = new GridNodePathData(destination.getNeighbor(direction), getOppositeDirection(direction), 0, 0);
                calculateRoutesHelper(new GridNodePathData[]{optimalPaths[firstNode.x][firstNode.y]});
                break;
            }
        }
    }

    /*
    iterate through arraylist of next grid nodes, given current distance, arraylist of next directions, arraylist of turnCount
     */
    private void calculateRoutesHelper(GridNodePathData[] currentGridNodes) {
        ArrayList<GridNodePathData> nextGridNodes = new ArrayList<>();
        int distanceToDest = currentGridNodes[0].distanceToDest;
        for (GridNodePathData gridNodePathData : currentGridNodes) {
            GridNode currentNode = gridNodePathData.gridNode;
            Direction directionFromLast = gridNodePathData.direction;
            int turns = gridNodePathData.turnCountToDest;

            Direction[] directions = currentNode.getValidDirections();
            for (Direction direction : directions) {
                if (direction != null) {
                    GridNode nextNode = currentNode.getNeighbor(direction);
                    int nextTurnCount = turns + (directionFromLast == getOppositeDirection(direction) ? 0 : 1);
                    int nextDistanceToDest = distanceToDest + 1;
                    if (optimalPaths[nextNode.x][nextNode.y] == null) {
                        optimalPaths[nextNode.x][nextNode.y] = new GridNodePathData(nextNode, getOppositeDirection(direction), nextTurnCount, nextDistanceToDest);
                        nextGridNodes.add(optimalPaths[nextNode.x][nextNode.y]);
                    } else if (optimalPaths[nextNode.x][nextNode.y].distanceToDest == nextDistanceToDest && nextTurnCount < optimalPaths[nextNode.x][nextNode.y].turnCountToDest) {
                        nextGridNodes.remove(optimalPaths[nextNode.x][nextNode.y]);
                        optimalPaths[nextNode.x][nextNode.y] = new GridNodePathData(nextNode, getOppositeDirection(direction), nextTurnCount, nextDistanceToDest);
                        nextGridNodes.add(optimalPaths[nextNode.x][nextNode.y]);
                    }
                }
            }
        }
        GridNodePathData[] nextGridNodesArray = new GridNodePathData[nextGridNodes.size()];
        for (int i = 0; i < nextGridNodes.size(); i++) {
            nextGridNodesArray[i] = nextGridNodes.get(i);
        }
        if (nextGridNodesArray.length > 0) {
            calculateRoutesHelper(nextGridNodesArray);
        } else {
            String str1 = "a\n";
            for (int j = 0; j < GRID_HEIGHT; j++) {
                for (int i = 0; i < GRID_WIDTH; i++) {
                    if (grid[i][j].isProject) {
                        str1 += "████";
                    } else if (optimalPaths[i][j] == null) {
                        str1 += "    ";
                    } else {
                        str1 += (optimalPaths[i][j].distanceToDest < 10 ? "--" + optimalPaths[i][j].distanceToDest + "-" : "-" + optimalPaths[i][j].distanceToDest + "-");
                    }
                }
                str1 += "\n";
            }
            Log.v("myapp", str1);
            String str2 = "a\n";
            for (int j = 0; j < GRID_HEIGHT; j++) {
                for (int i = 0; i < GRID_WIDTH; i++) {
                    if (grid[i][j].isProject) {
                        str2 += "███";
                    } else if (optimalPaths[i][j] == null) {
                        str2 += "   ";
                    } else {
                        str2 += (optimalPaths[i][j].turnCountToDest < 10 ? "-" + optimalPaths[i][j].turnCountToDest + "-" : "-" + optimalPaths[i][j].turnCountToDest);
                    }
                }
                str2 += "\n";
            }
            Log.v("myapp", str2);
            String str3 = "a\n";
            for (int j = 0; j < GRID_HEIGHT; j++) {
                for (int i = 0; i < GRID_WIDTH; i++) {
                    if (grid[i][j].isProject) {
                        str3 += "███";
                    } else if (optimalPaths[i][j] == null) {
                        str3 += "   ";
                    } else {
                        str3 += (optimalPaths[i][j].direction == Direction.RIGHT ? " → " :
                                optimalPaths[i][j].direction == Direction.DOWN ? " ↓ " :
                                        optimalPaths[i][j].direction == Direction.UP ? " ↑ " : " ← ");
                    }
                }
                str3 += "\n";
            }
            Log.v("myapp", str3);
            paintPath();
        }
    }

    private void paintPath() {
        for (GridNode[] row : grid) {
            for (GridNode node : row) {
                node.button.setBackgroundColor(getResources().getColor(R.color.transparent));
            }
        }
        if (currentLocation != null) {
            int countSinceArrow = 1;
            GridNodePathData currentPath = optimalPaths[currentLocation.x][currentLocation.y];
            if (currentPath != null) {
                while (currentPath.distanceToDest > 0) {
                    if (countSinceArrow == 0) {
                        this.printArrows(currentPath);
                    }
                    countSinceArrow = (countSinceArrow + 1) % 2;
                    GridNode nextNode = currentPath.gridNode.getNeighbor(currentPath.direction);
                    GridNodePathData nextPath = optimalPaths[nextNode.x][nextNode.y];
                    currentPath = nextPath;
                }
                if (currentLocation.x != currentPath.gridNode.x || currentLocation.y != currentPath.gridNode.y) {
                    this.printArrows(currentPath);
                } else {
                    setDestination(null);
                }
            }
        }
    }

    private void printArrows(GridNodePathData pathData) {
        switch (pathData.direction) {
            case UP:
                pathData.gridNode.button.setBackground(getDrawable(R.drawable.ic_up));
                break;
            case DOWN:
                pathData.gridNode.button.setBackground(getDrawable(R.drawable.ic_down));
                break;
            case RIGHT:
                pathData.gridNode.button.setBackground(getDrawable(R.drawable.ic_right));
                break;
            case LEFT:
                pathData.gridNode.button.setBackground(getDrawable(R.drawable.ic_left));
                break;
        }
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

        if (id == R.id.nav_manage) {
            calculatePosition(0, 0, 0);
        } else {
            int projectNum = Integer.parseInt((item.getTitle().toString().split(" "))[1]) - 1;
            Log.v("myapp", ""+projectNum);
            setDestination(projects[projectNum]);
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
        moveMarker(4, 4);
        return new Coord(0, 0);
    }

    public void moveMarker(double x, double y) {
        markerParams.horizontalBias = (float) (x / 11.0); // here is one modification for example. modify anything else you want :)
        markerParams.verticalBias = (float) (y / 15.0);
        marker.setLayoutParams(markerParams); // request the view to use the new modified params
    }

    private Direction getOppositeDirection(Direction direction) {
        switch (direction) {
            case RIGHT:
                return Direction.LEFT;
            case LEFT:
                return Direction.RIGHT;
            case UP:
                return Direction.DOWN;
            case DOWN:
                return Direction.UP;
            default:
                return null;
        }
    }

    private class Coord {
        public int x;
        public int y;

        public Coord(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private class GridNodePathData {
        public Direction direction;
        public int turnCountToDest;
        public int distanceToDest;
        public GridNode gridNode;

        public GridNodePathData(GridNode gridNode, Direction direction, int turnCountToDest, int distanceToDest) {
            this.gridNode = gridNode;
            this.direction = direction;
            this.turnCountToDest = turnCountToDest;
            this.distanceToDest = distanceToDest;
        }
    }
}
