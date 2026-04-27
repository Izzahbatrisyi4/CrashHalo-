package com.example.crashhaloapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.intent.Intents;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.example.crashhaloapp.R;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testScanButton_opensCameraActivity() {
        onView(withId(R.id.btn_scan)).perform(click());
        intended(hasComponent(CameraCaptureActivity.class.getName()));
    }

    @Test
    public void testMapButton_opensWorkshopMapActivity() {
        onView(withId(R.id.btn_map)).perform(click());
        intended(hasComponent(WorkshopMapActivity.class.getName()));
    }

    @Test
    public void testSettingsButton_opensSettingsActivity() {
        onView(withId(R.id.btn_settings)).perform(click());
        intended(hasComponent(SettingsActivity.class.getName()));
    }
}
