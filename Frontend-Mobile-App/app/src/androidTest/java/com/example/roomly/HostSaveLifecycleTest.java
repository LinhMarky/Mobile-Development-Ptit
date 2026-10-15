package com.example.roomly;

import static org.junit.Assert.*;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.roomly.ui.NavigationHelper;
import com.example.roomly.ui.host.HostSaveViewModel;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class HostSaveLifecycleTest {
    private void onMain(Runnable action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }

    @Test public void rapidTapsStartOnlyOneSave() {
        onMain(() -> {
            HostSaveViewModel state = new HostSaveViewModel(new SavedStateHandle());
            assertTrue(state.begin("owner-a"));
            assertFalse(state.begin("owner-a"));
            assertEquals(HostSaveViewModel.SAVING, state.currentPhase());
        });
    }

    @Test public void successIsConsumedExactlyOnce() {
        onMain(() -> {
            HostSaveViewModel state = new HostSaveViewModel(new SavedStateHandle());
            state.begin("owner-a");
            state.saved("saved");
            assertFalse(state.begin("owner-a"));
            assertEquals("saved", state.consumeSuccess("owner-a"));
            assertNull(state.consumeSuccess("owner-a"));
        });
    }

    @Test public void anotherAccountCannotConsumeSuccess() {
        onMain(() -> {
            HostSaveViewModel state = new HostSaveViewModel(new SavedStateHandle());
            state.begin("owner-a");
            state.saved("saved");
            assertNull(state.consumeSuccess("owner-b"));
            assertEquals("saved", state.consumeSuccess("owner-a"));
        });
    }

    @Test public void restoredInFlightSaveDoesNotResend() {
        onMain(() -> {
            HashMap<String, Object> restored = new HashMap<>();
            restored.put("save_phase", HostSaveViewModel.SAVING);
            restored.put("save_owner", "owner-a");
            HostSaveViewModel state = new HostSaveViewModel(new SavedStateHandle(restored));
            assertEquals(HostSaveViewModel.UNCERTAIN, state.currentPhase());
            assertFalse(state.begin("owner-a"));
            assertFalse(state.message().isEmpty());
        });
    }

    @Test public void restoredCompletedSaveKeepsItsResult() {
        onMain(() -> {
            HashMap<String, Object> restored = new HashMap<>();
            restored.put("save_phase", HostSaveViewModel.SAVED);
            restored.put("save_owner", "owner-a");
            restored.put("save_message", "saved");
            HostSaveViewModel state = new HostSaveViewModel(new SavedStateHandle(restored));
            assertEquals("saved", state.consumeSuccess("owner-a"));
        });
    }

    @Test public void aConfirmedFailureCanBeRetried() {
        onMain(() -> {
            HostSaveViewModel state = new HostSaveViewModel(new SavedStateHandle());
            state.begin("owner-a");
            state.failed("network error");
            assertEquals(HostSaveViewModel.ERROR, state.currentPhase());
            assertTrue(state.begin("owner-a"));
        });
    }

    @Test public void activityRecreationRetainsInFlightResult() {
        AtomicReference<HostSaveViewModel> first = new AtomicReference<>();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                HostSaveViewModel state = new ViewModelProvider(activity).get(HostSaveViewModel.class);
                assertTrue(state.begin("owner-a"));
                first.set(state);
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                HostSaveViewModel state = new ViewModelProvider(activity).get(HostSaveViewModel.class);
                assertSame(first.get(), state);
                assertEquals(HostSaveViewModel.SAVING, state.currentPhase());
                state.saved("saved after rotation");
                assertEquals("saved after rotation", state.consumeSuccess("owner-a"));
            });
        }
    }

    @Test public void backgroundNavigationWaitsForResumeAndRunsOnce() {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<Fragment> screen = new AtomicReference<>();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Fragment fragment = new Fragment(android.R.layout.simple_list_item_1);
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment).commitNow();
                screen.set(fragment);
            });
            scenario.moveToState(Lifecycle.State.CREATED);
            scenario.onActivity(activity -> NavigationHelper.whenResumed(screen.get(), calls::incrementAndGet));
            assertEquals(0, calls.get());
            scenario.moveToState(Lifecycle.State.RESUMED);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertEquals(1, calls.get());
            scenario.moveToState(Lifecycle.State.CREATED).moveToState(Lifecycle.State.RESUMED);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertEquals(1, calls.get());
        }
    }
}
