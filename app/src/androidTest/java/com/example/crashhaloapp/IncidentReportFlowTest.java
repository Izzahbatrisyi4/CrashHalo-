package com.example.crashhaloapp;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class IncidentReportFlowTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testFullIncidentReportingFlow() throws InterruptedException {
        // 1. Start from Main and click Scan
        onView(withId(R.id.btn_scan)).perform(click());

        // 2. Verify we are in CameraCaptureActivity (checking step 1 text)
        onView(withId(R.id.guide_text)).check(matches(withText("STEP 1: TAKE PHOTO FROM 45°")));

        // 3. Take first photo
        onView(withId(R.id.capture_button)).perform(click());
        Thread.sleep(1000); // Wait for capture/UI update

        // 4. Verify step 2
        onView(withId(R.id.guide_text)).check(matches(withText("STEP 2: TAKE PHOTO OF VIN NUMBER")));
        onView(withId(R.id.capture_button)).perform(click());
        Thread.sleep(1000);

        // 5. Verify step 3
        onView(withId(R.id.guide_text)).check(matches(withText("STEP 3: TAKE PHOTO OF DAMAGE AREA")));
        onView(withId(R.id.capture_button)).perform(click());
        
        // 6. Wait for AI and Firestore (this might take a few seconds)
        Thread.sleep(5000);

        // 7. Verify we returned to Home Screen
        onView(withId(R.id.rv_incidents)).check(matches(isDisplayed()));
    }
}
