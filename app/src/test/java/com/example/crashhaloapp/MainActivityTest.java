package com.example.crashhaloapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.view.View;

import androidx.test.core.app.ActivityScenario;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;

@RunWith(RobolectricTestRunner.class)
public class MainActivityTest {

    private ActivityScenario<MainActivity> scenario;
    private MainActivity activity;

    @Before
    public void setUp() {
        scenario = ActivityScenario.launch(MainActivity.class);
        scenario.onActivity(act -> activity = act);
    }

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    @Test
    public void testScanButton_opensCameraActivity() {
        assertNotNull("Activity not initialized", activity);
        View btnScan = activity.findViewById(R.id.btn_scan);
        assertNotNull("Scan button not found", btnScan);

        btnScan.performClick();

        Intent expectedIntent = new Intent(activity, CameraCaptureActivity.class);
        ShadowActivity shadowActivity = shadowOf(activity);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertNotNull("Intent not started", actualIntent);
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
    }

    @Test
    public void testMapButton_opensWorkshopMapActivity() {
        assertNotNull("Activity not initialized", activity);
        View btnMap = activity.findViewById(R.id.btn_map);
        assertNotNull("Map button not found", btnMap);

        btnMap.performClick();

        Intent expectedIntent = new Intent(activity, WorkshopMapActivity.class);
        ShadowActivity shadowActivity = shadowOf(activity);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertNotNull("Intent not started", actualIntent);
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
    }

    @Test
    public void testSettingsButton_opensSettingsActivity() {
        assertNotNull("Activity not initialized", activity);
        View btnSettings = activity.findViewById(R.id.btn_settings);
        assertNotNull("Settings button not found", btnSettings);

        btnSettings.performClick();

        Intent expectedIntent = new Intent(activity, SettingsActivity.class);
        ShadowActivity shadowActivity = shadowOf(activity);
        Intent actualIntent = shadowActivity.getNextStartedActivity();

        assertNotNull("Intent not started", actualIntent);
        assertEquals(expectedIntent.getComponent(), actualIntent.getComponent());
    }
}
