package com.gregrussell.maptest;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.location.Location;
import android.provider.BaseColumns;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class DataBaseHelperTracker extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 4;
    public static final String DATABASE_PATH = "/data/data/com.gregrussell.maptest/databases/";
    public static final String DATABASE_NAME = "tracker.db";
    private static Context mContext;
    private SQLiteDatabase myDataBase;


    public static class Routes implements BaseColumns{

        public static final String TABLE_NAME = "routes";
        public static final String COLUMN_ID = "route_id";
        //date + time route started
        public static final String COLUMN_NAME = "route_name";
        //"points_" + route name + "_" + random characters
        public static final String COLUMN_IDENTIFIER = "route_identifier";
        //0 if "deleted", 1 if in use
        public static final String COLUMN_ACTIVE = "route_active";
        //time created
        public static final String COLUMN_TIMESTAMP = "route_timestamp";
    }

    public static class Points implements BaseColumns{

        //Table Name will be route_identifier from routes table
        public static final String TABLE_NAME = "points";
        public static final String COLUMN_ID = "point_id";
        public static final String COLUMN_TIME = "point_time";
        public static final String COLUMN_LATITUDE = "point_latitude";
        public static final String COLUMN_LONGITUDE = "point_longitude";
        public static final String COLUMN_NAME = "point_name";
        public static final String COLUMN_WAYPOINT =  "point_waypoint";

        public static final int POSITION_ID = 0;
        public static final int POSITION_TIME = 1;
        public static final int POSITION_LATITUDE = 2;
        public static final int POSITION_LONGITUDE = 3;
        public static final int POSITION_NAME = 4;
        public static final int POSITION_WAYPOINT = 5;


    }

    public static class Landmarks implements BaseColumns{

        public static final String COLUMN_ID = "landmark_id";
        public static final String COLUMN_TIME = "landmark_time";
        public static final String COLUMN_LATITUDE = "landmark_latitude";
        public static final String COLUMN_LONGITUDE = "landmark_longitude";
        public static final String COLUMN_NAME = "landmark_name";
    }


    private static final String SQL_CREATE_TABLE_ROUTES = "CREATE TABLE " +
            Routes.TABLE_NAME + " (" +
            Routes.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            Routes.COLUMN_IDENTIFIER + " TEXT UNIQUE, " +
            Routes.COLUMN_NAME + " TEXT UNIQUE, " +
            Routes.COLUMN_ACTIVE + " INTEGER NOT NULL DEFAULT 1, " +
            Routes.COLUMN_TIMESTAMP + " INTEGER)";



    private static String SQL_CREATE_TABLE_POINTS(String tableName){

        return "CREATE TABLE " +
                tableName + " (" +
                Points.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                Points.COLUMN_TIME + " INTEGER, " +
                Points.COLUMN_LATITUDE + " NUMERIC, " +
                Points.COLUMN_LONGITUDE + " NUMERIC, " +
                Points.COLUMN_NAME + " TEXT, " +
                Points.COLUMN_WAYPOINT + " NUMERIC)";
    }

    private static String SQL_CREATE_TABLE_LANDMARKS(String tableName){

        return "CREATE TABLE " +
                tableName + " (" +
                Landmarks.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                Landmarks.COLUMN_TIME + " INTEGER, " +
                Landmarks.COLUMN_LATITUDE + " NUMERIC, " +
                Landmarks.COLUMN_LONGITUDE + " NUMERIC, " +
                Landmarks.COLUMN_NAME + " TEXT)";
    }

    private static final String SQL_DELETE_ROUTES = "DROP TABLE IF EXISTS " + Routes.TABLE_NAME;
    private static String SQL_DELETE_POINTS (String tableName){
        return "DROP TABLE IF EXISTS " + tableName;
    }

    public DataBaseHelperTracker(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;

    }

    public void onCreate(SQLiteDatabase db){

    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        Log.d("onUpgrade","on Upgrade");



    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        onUpgrade(db, oldVersion,newVersion);
    }

    public void createDataBase() throws IOException {
        boolean dbExist = checkDataBase();
        if(dbExist) {
            //do nothing, database already exists
            Log.d("databaseHelper1", "database exists");

            SQLiteDatabase db = this.getWritableDatabase();
            if (!checkTableExists(Routes.TABLE_NAME)) {
                db.execSQL(SQL_CREATE_TABLE_ROUTES);
                Log.d("databaseHelper5", String.valueOf(checkTableExists(Routes.TABLE_NAME)));
            }
        }else{
            SQLiteDatabase db = this.getWritableDatabase();
            db.execSQL(SQL_CREATE_TABLE_ROUTES);
        }
    }

    private boolean checkDataBase(){
        SQLiteDatabase checkDB = null;
        try{
            checkDB = SQLiteDatabase.openDatabase(DATABASE_PATH + DATABASE_NAME,null,SQLiteDatabase.OPEN_READONLY);
        }catch (SQLiteException e){
            //database doesn't exist
            e.printStackTrace();
            Log.d("databaseHelper3", "check database, database doesn't exist");
        }
        if(checkDB != null){
            checkDB.close();
        }
        return checkDB != null ? true : false;
    }

    private boolean checkTableExists(String table){
        try{
            openDataBase();
        }catch (SQLException sqle){
            throw sqle;
        }

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + table + "'",null);

        if(cursor!=null){
            if(cursor.getCount() > 0){
                cursor.close();
                return true;
            }
            cursor.close();
        }
        return false;

    }


    public void openDataBase() throws  SQLiteException{
        String myPath = DATABASE_PATH + DATABASE_NAME;
        Log.d("databasehelper4", myPath);
        myDataBase = SQLiteDatabase.openDatabase(myPath,null,SQLiteDatabase.OPEN_READONLY);
    }

    @Override
    public synchronized void close(){
        if(myDataBase !=null){
            myDataBase.close();
        }
        super.close();
    }


    /**
     * Method for adding a new route to the DB. A route entry is added and a corresponding table
     * for the points in the route is created
     */
    public void startNewRoute(){

        SQLiteDatabase db = this.getWritableDatabase();

        boolean wasSuccessful = true;
        String routeIdentifier = "";
        long index = 0;
        try{

            Long timestamp = System.currentTimeMillis();

            Date date = new Date();
            date.setTime(timestamp);
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.ENGLISH);

            String dateString = dateFormat.format(date);
            routeIdentifier = "_" + timestamp + "_" + randomData();


            db.beginTransaction();
            String query = "INSERT INTO " + Routes.TABLE_NAME + " (" +
                    Routes.COLUMN_IDENTIFIER + ", " +
                    Routes.COLUMN_NAME + ", " +
                    Routes.COLUMN_ACTIVE + ", " +
                    Routes.COLUMN_TIMESTAMP + ") VALUES (?,?,?,?)";
            SQLiteStatement statement = db.compileStatement(query);
            statement.bindString(1, routeIdentifier);
            statement.bindString(2, dateString);
            statement.bindLong(3, 1);
            statement.bindLong(4, timestamp);
            index = statement.executeInsert();
            db.setTransactionSuccessful();
        }catch (SQLiteException e){
            wasSuccessful = false;
            StringWriter error = new StringWriter();
            e.printStackTrace(new PrintWriter(error));
            Log.d("startNewRoute",error.toString());
        }finally {
            db.endTransaction();
        }
        //if the route was created, create the associated points table
        if(wasSuccessful){
            if (!checkTableExists(routeIdentifier)) {
                db.execSQL(SQL_CREATE_TABLE_POINTS(Points.TABLE_NAME + correspondingPointsTable(index)));
                Log.d("startNewRoute2", String.valueOf(checkTableExists(routeIdentifier)));
            }

        }else{
            Log.d("startNewRoute3","creating route unsuccessful");
        }


    }



    /**
     * Method used to return a 16 character string of random data. Gets a random integer between 48
     * and 122, which represent the ASCII values of integers and letters. If integers between 91 and
     * 96 are returned, the random number generator is run again because ASCII values between 91 and
     * 96 are not integers or numbers. Uses a for loop to generate 16 characters. Finally checks if
     * the resulting random data string is unique to the database. If not, the method is run again
     * @return 16 character string of randomly generated data containing integers and letters
     */
    private String randomData(){

        String randomData = "";
        Random random = new Random();
        for(int i = 0; i<16;i++){
            int randomASCII = 0;
            do{
                randomASCII = random.nextInt(122-48) + 48;
            }while ((randomASCII >= 91 && randomASCII <= 96) || (randomASCII >= 52 && randomASCII <= 64));
            randomData = randomData + (char)randomASCII;

        }

        Boolean isRandom = false;
        SQLiteDatabase db = this.getReadableDatabase();
        try{
            db.beginTransaction();
            String query = "SELECT " + Routes.COLUMN_NAME + " FROM " + Routes.TABLE_NAME + " WHERE " +
                    Routes.COLUMN_NAME + " LIKE ?";
            SQLiteStatement statement = db.compileStatement(query);
            statement.bindString(1,"%"+randomData+"%");
            statement.simpleQueryForString();
        }catch (Exception e){
            //if error is thrown, the value is not in the DB so it is unique
            isRandom = true;
        }finally {
            db.endTransaction();
        }
        //the value is not unique so run again
        if(!isRandom){
            randomData();
        }
        return randomData;
    }

    /**
     * Method used to add points to the points table of the current Route. Receives the route index
     * as a parameter and uses it to retrieve the name of the corresponding points table. Receives
     * the latitude and longitude coordinates of the point as a Location object. Updates the timestamp
     * of the route index, keeping the current route as the most recently updated
     * @param routeIndex
     * @param location
     * @return A list of all points in the table as LatLng objects
     */
    public List<LatLng> addPoints(Long routeIndex, Location location){

        SQLiteDatabase db = this.getWritableDatabase();

        String pointsTable = correspondingPointsTable(routeIndex);
        Location lastLocation = lastLocation(routeIndex);

        if(location.distanceTo(lastLocation) > 5) {


            if (!pointsTable.equals("")) {
                try {
                    db.beginTransaction();
                    String query = "INSERT INTO " + Points.TABLE_NAME + pointsTable + " (" +
                            Points.COLUMN_TIME + ", " +
                            Points.COLUMN_LATITUDE + ", " +
                            Points.COLUMN_LONGITUDE + ", " +
                            Points.COLUMN_NAME + ", " +
                            Points.COLUMN_WAYPOINT + ") VALUES (?,?,?,?,?)";
                    SQLiteStatement statement = db.compileStatement(query);
                    statement.bindLong(1, location.getTime());
                    statement.bindDouble(2, location.getLatitude());
                    statement.bindDouble(3, location.getLongitude());
                    statement.bindString(4, location.getLatitude() + ", " + location.getLongitude());
                    statement.bindLong(5, 0);
                    statement.executeInsert();
                    db.setTransactionSuccessful();
                } catch (SQLiteException e) {
                    StringWriter error = new StringWriter();
                    e.printStackTrace(new PrintWriter(error));
                    Log.d("addPoints2", "Add points to table error: " + error.toString());
                } finally {
                    db.endTransaction();
                }

                try {
                    db.beginTransaction();
                    String query = "UPDATE " + Routes.TABLE_NAME + " SET " + Routes.COLUMN_TIMESTAMP +
                            "= ? WHERE " + Routes.COLUMN_ID + " = ?";
                    SQLiteStatement statement = db.compileStatement(query);
                    statement.bindLong(1, System.currentTimeMillis());
                    statement.bindLong(2, routeIndex);
                    statement.executeUpdateDelete();
                    db.setTransactionSuccessful();
                } catch (SQLiteException e) {
                    StringWriter error = new StringWriter();
                    e.printStackTrace(new PrintWriter(error));
                    Log.d("addPoints3", "update route timestamp error: " + error.toString());
                } finally {
                    db.endTransaction();
                }

            }
        }else{
            Log.d("addPoints4", "Distance Changed by " + location.distanceTo(lastLocation) + "m , not enough to add");
        }
        return pointsList(routeIndex);
    }

    /**
     * Method used to retrieve the last point entered in the table as a Location object
     * @param routeIndex The index of the route as a Long
     * @return A Location object
     */
    private Location lastLocation(Long routeIndex){
        String pointsTable = correspondingPointsTable(routeIndex);
        Log.d("lastLocation","points table: " + pointsTable);
        Location location = new Location("");
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + Points.TABLE_NAME + pointsTable + " ORDER BY " +
                Points.COLUMN_TIME + " DESC";
        try {
            Cursor cursor = db.rawQuery(query, null);

            if (cursor.moveToFirst() && cursor.getCount() > 1) {
                location.setLatitude(cursor.getDouble(Points.POSITION_LATITUDE));
                location.setLongitude(cursor.getDouble(Points.POSITION_LONGITUDE));
            }
            cursor.close();
        }catch (Exception e){

        }

        return location;

    }

    /**
     * Method used to get table name of the points table that corresponds to the current route idex
     * @param routeIndex The index of the route as a long
     * @return The name of the points table as a String
     */
    private String correspondingPointsTable(Long routeIndex){

        SQLiteDatabase db = this.getWritableDatabase();
        String pointsTable = "";
        try{
            db.beginTransaction();
            String query = "SELECT " + Routes.COLUMN_IDENTIFIER + " FROM " + Routes.TABLE_NAME +
                    " WHERE " + Routes.COLUMN_ID + " LIKE ?";
            SQLiteStatement statement = db.compileStatement(query);
            statement.bindLong(1,routeIndex);
            Log.d("correspondingPoints1","routeIndex: " + routeIndex );
            pointsTable = statement.simpleQueryForString();

        }catch (SQLiteException e){
            StringWriter error = new StringWriter();
            e.printStackTrace(new PrintWriter(error));
            Log.d("correspondingPoints2","Get table name error: " + error.toString());

        }finally {
            db.endTransaction();
        }

        return pointsTable;


    }

    /**
     * Method used to return the index of the most recent route
     * @return The index of the most recent route as a long
     */
    public long mostRecentRoute(){

        SQLiteDatabase db = this.getReadableDatabase();
        long routeIndex = 0;
        try{
            String query = "SELECT * FROM " + Routes.TABLE_NAME + " ORDER BY " +
                    Routes.COLUMN_TIMESTAMP + " DESC";
            Cursor cursor = db.rawQuery(query,null);
            cursor.moveToFirst();
            routeIndex = cursor.getInt(0);

        }catch (SQLiteException e) {
            StringWriter error = new StringWriter();
            e.printStackTrace(new PrintWriter(error));
            Log.d("mostRecentRoute", error.toString());
        }

        return routeIndex;
    }

    /**
     * Method for retrieving all points in the table as a list of LatLng objects
     * @param routeIndex The index of the route as a long
     * @return A list of LatLng objects
     */

    private List<LatLng> pointsList(Long routeIndex){

        String pointsTable = correspondingPointsTable(routeIndex);
        List<LatLng> latLngList = new ArrayList<LatLng>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + Points.TABLE_NAME + pointsTable + " WHERE " +
                Points.COLUMN_WAYPOINT + " =?";
        try {
            Cursor cursor = db.rawQuery(query, new String[]{"0"});
            if (cursor.moveToFirst()) {
                do {
                    LatLng latLng = new LatLng(cursor.getDouble(Points.POSITION_LATITUDE),
                            cursor.getDouble(Points.POSITION_LONGITUDE));
                    latLngList.add(latLng);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }catch (Exception e){

        }

        return latLngList;


    }

    public void dropAllTables(){

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        List<String> tables = new ArrayList<>();

// iterate over the result set, adding every table name to a list
        while (c.moveToNext()) {
            tables.add(c.getString(0));
        }

// call DROP TABLE on every table name
        for (String table : tables) {
            String dropQuery = "DROP TABLE IF EXISTS " + table;
            db.execSQL(dropQuery);
        }

    }




}