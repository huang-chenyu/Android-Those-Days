package io.github.huang_chenyu.thosedays;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Chiao on 2017/11/25.
 */

public class Algorithm {
    private static final String LOG_TAG = "[Algorithm]";

    private static final String SERVER_PREDICTIONS_FILE_SUFFIX = ".server_predictions.json";
    private static final String USER_REPORTED_LABELS_FILE_SUFFIX = ".user_reported_labels.json";
    private static final String UUID_DIR_PREFIX = "extrasensory.labels.";
    private static final String EXTRASENSORY_PKG_NAME = "edu.ucsd.calab.extrasensory";

    private static final int TIME_INTERVAL = 60;

    private static final int SLIDING_WINDOW_SIZE = 10;

//    private static final int[] ACTIVITIES = { 0, 1, 2, 3, 4, 5, 6, 19, 20, 21,
//                                    22, 23, 24, 25, 26, 27, 28, 29, 33, 34, 35, 36, 37, 38, 39, 44 };
//
//    private static final int[] LOCATIONS =
//            { 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 30, 31, 32, 40, 41, 42, 43, 45 };
//
//    private static final int[] NO_NEED = {18, 46, 47, 48, 49, 50};

    // 3: Activity, 2: Locations, 1: No Need

    private static final int[] catOfAct = {3, 3, 3, 3, 3, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1,
            3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 2, 2, 2, 2, 3, 2, 1, 1, 1, 1, 1};

    private static final String[] label2Act =
            { "Lying down", "Sitting", "Walking", "Running", "Bicycling", "Sleeping", "Lab work", "In class", "In a meeting", "At work",
                    "Indoors", "Outside", "In a car", "On a bus", "Drive - I'm the driver", "Drive - I'm a passenger", "At home", "At a restaurant",
                    "Phone in pocket", "Exercise", "Cooking", "Shopping", "Strolling", "Drinking (alcohol)", "Bathing - shower", "Cleaning", "Doing laundry",
                    "Washing dishes", "Watching TV", "Surfing the internet", "At a party", "At a bar", "At the beach", "Singing", "Talking", "Computer work",
                    "Eating", "Toilet", "Grooming", "Dressing", "At the gym", "Stairs - going up", "Stairs - going down", "Elevator", "Standing", "At school",
                    "Phone in hand", "Phone in bag", "Phone on table", "With co-workers", "With friends"};

    private static File ESAFilesDir;

    public static void process(Context context) {


        try {

            ESAFilesDir = getUsersFilesDirectory(context);

            List<JSONObject> files = getLabelFiles(ESAFilesDir);

            List<String> schedule = raw2schedule(files);

            List<Pair<String, Integer>> rleSchedule = runLength(schedule);

        }

        catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }


    /**
     * Read files and determine the activity of each time interval.
     * @param files: filenames of the unprocessed data.
     * @return List<Double> probabilities of activities.
     */
    private static List<String> raw2schedule(List<JSONObject> files) throws IOException, JSONException{

        List<Integer> timestamps = new ArrayList<>();

        for ( int i = 0; i < files.size(); i++ ) {
            timestamps.add(files.get(i).getInt("timestamp"));
        }

        int curTime = timestamps.get(0);
        int endTime = timestamps.get(timestamps.size()-1);

        int idx = 0;

        MyQue<String> actQue = new MyQue<>(SLIDING_WINDOW_SIZE);

        List<String> actRes = new ArrayList<>();

        while (curTime < endTime) {

            if (curTime >= timestamps.get(idx)) {
                while (curTime >= timestamps.get(idx)) {

                    // Load the file of this timestamp
                    List<Double> probs = new ArrayList<>();

                    JSONArray probArray = files.get(idx).getJSONArray("label_probs");

                    for (int i = 0; i < probArray.length(); i++) {

                        probs.add(probArray.getDouble(i));

                    }

                    // Get index of the activity which has the highest prob
                    String maxAct = label2Act[findMaxAct(probs)];

                    // Append to myQue
                    actQue.append(maxAct);

                    // Go to next timestamp.
                    idx += 1;
                }

            } else {
                // No new data available, use the last data.
                String tmp = actQue.peek();
                actQue.append(tmp);
            }

            // Append Result
            actRes.add(actQue.findMax());

            // Update curTime
            curTime += TIME_INTERVAL;
        }

        return actRes;
    }

    /**
     * Return the activity with the highest probability.
     * @param probs: Probabilities of all the labels.
     * @return int: Index of the activity with the highest prob.
     */
    private static int findMaxAct(List<Double> probs) {

        int maxAct = -1;
        double maxProb = -1;

        for( int i = 0; i < probs.size(); i++ ) {
            if(catOfAct[i] == 3){
                // These are the activities.
                if( probs.get(i) - maxProb > 0 ) {
                    maxAct = i;
                    maxProb = probs.get(i);
                }

            } else if (catOfAct[i] == 2) {
                // Labels about locations.
            } else {
                // Labels we don't need.
            }
        }
        return maxAct;
    }


    private static List<Pair<String, Integer>> runLength (List<String> acts) {

        List<Pair<String, Integer>> res = new ArrayList<>();

        String curAct = acts.get(0);
        int idx = 1, cnt = 1;

        while ( idx < acts.size()) {

            if (acts.get(idx).equals(curAct)) {

                cnt += 1;

            } else {

                res.add(new Pair<String, Integer>(curAct, cnt));

                // Update current activity.
                curAct = acts.get(idx);

                // Update count
                cnt = 1;

            }

            idx += 1;

        }
        res.add( new Pair<String, Integer>(curAct, cnt));

        return res;
    }

    private static List<JSONObject> getLabelFiles(File filesDir) throws IOException, JSONException{

        String[] filenames = filesDir.list();

        List<JSONObject> res = new ArrayList<>();

        for (String filename : filenames) {
            File file = new File(filesDir, filename);
            StringBuilder text = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(file));
            text.append(br.readLine());
            JSONObject jsonObject = new JSONObject(text.toString());
            String[] splitFN = filename.split("\\.");
            jsonObject.put("timestamp",Integer.valueOf(splitFN[0]));
            res.add(jsonObject);
        }

        return res;
    }

    private static File getUsersFilesDirectory(Context context) throws PackageManager.NameNotFoundException {
        // Locate the ESA saved files directory, and the specific minute-example's file:
        Context extraSensoryAppContext = context.createPackageContext(EXTRASENSORY_PKG_NAME, 0);
        File esaFilesDir = extraSensoryAppContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (esaFilesDir == null) {
            Log.e(LOG_TAG, "Cannot find ExtraSensory directory.");
            return null;
        }

        // String[] filenames = esaFilesDir.list();
        String[] filenames = esaFilesDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.startsWith(UUID_DIR_PREFIX);
            }
        });

        // Assume there's only one user using this phone, and take first user's data
        File userEsaFilesDir = new File(extraSensoryAppContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), filenames[0]);
        if (!userEsaFilesDir.exists()) {
            Log.e(LOG_TAG, "Cannot find ExtraSensory user data.");
            return null;
        }
        else {
            Log.d(LOG_TAG, "ExtraSensory directory exists! " + esaFilesDir.getPath());
            return userEsaFilesDir;
        }
    }

//      There should be a attribute for this app recording the timestamp of the file it processed last time

    //      Maybe need to sort or not -> check which files are not processed yet -> Dump the unprocessed ones into "dataProcess".
//      -> Dumping the processed data into database may happen within the function '"ataProcess".
//        if (blah blah) {
//      }

//        Read data from database and render the screen of the app.

}
