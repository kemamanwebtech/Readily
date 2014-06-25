package com.infm.readit;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.WindowManager;

import com.infm.readit.utils.FileUtils;
import com.infm.readit.utils.Utils;

public class ReceiverActivity extends Activity {

    public static final String LOGTAG = "ReceiverActivity";

    private static final String EXTRA_PATH = "path";
    private static final String EXTRA_ROWID = "_id";
    private static final String EXTRA_POSITION = "position";
    private static final String EXTRA_TYPE = "source_type";
    private static final String EXTRA_TEXT = "text";

    /**
     * Starts receiver activity
     *
     * @param utils = all needed is put in Utils abstract class
     */
    public static void startReceiverActivity(Context context, Utils utils) {
        utils.process();
        if (utils.isProcessFailed())
            return;

        Intent intent = new Intent(context, ReceiverActivity.class);

        String text = utils.getSb().toString();
        Pair<Integer, Integer> existingData = utils.getExistingData();
        int type = utils.getType();

        if (type != FileUtils.TYPE_EPUB)
            intent.setType("text/plain");
        else
            intent.setType("text/html");

        Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_TEXT, type);
        bundle.putString(EXTRA_TEXT, text);
        bundle.putString(EXTRA_PATH, utils.getPath());
        if (existingData != null)
            bundle.putInt(EXTRA_POSITION, existingData.second);

        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        startReaderFragment();
    }

    @Override
    public void onPause() {
        Log.d(LOGTAG, "onPause() called");
        super.onPause();
    }

    /**
     * probably should be moved to ReaderFragment. When db section is implemented, lol
     */
    @Override
    public void onStop() {
        Log.d(LOGTAG, "onStop() called");
        super.onStop();
    }

    /**
     * it seems crappy, really
     *
     * @return Bundle instance, which will be passed to ReaderFragment as bundle of args
     */
    private Bundle bundleReceivedData() {
        Bundle bundle = getIntent().getExtras();
        Log.d(LOGTAG, "bundle: " + bundle.toString());
        return bundle;
    }

    private void startReaderFragment() {
        Fragment readerFragment = new ReaderFragment();
        readerFragment.setArguments(bundleReceivedData());
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, readerFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
