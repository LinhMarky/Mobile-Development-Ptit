package com.example.homely;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.homely.data.ApiClient;
import com.example.homely.data.SessionStore;
import com.google.android.material.button.MaterialButton;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONObject;
import java.util.UUID;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** Explicit opt-in: DEMO_ENABLED backend with seeded demo users and HOMELY_DEMO_PASSWORD. */
@RunWith(AndroidJUnit4.class)
public class ConnectedWorkflowTest {
    @Test public void realApiBookingPaymentAndAndroidNavigation() throws Exception {
        String password=InstrumentationRegistry.getArguments().getString("demoPassword","");
        assumeTrue("Set HOMELY_DEMO_PASSWORD for connected acceptance",!password.isEmpty());
        var context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        var api=new ApiClient(new SessionStore(context));
        api.login("host@homely.test",password);
        JSONObject room=api.call("POST","/rooms",ApiClient.json("unit_code","ANDROID_"+UUID.randomUUID().toString().substring(0,8).toUpperCase(),
                "room_type","SINGLE_ROOM","area_m2",25,"max_occupants",2,"amenity_ids",new org.json.JSONArray(),
                "address",ApiClient.json("line","Android acceptance room","province_code","HN","province_name","Ha Noi","ward_code","HA_DONG","ward_name","Ha Dong"),
                "location",ApiClient.json("latitude",20.98,"longitude",105.79)),UUID.randomUUID().toString());
        JSONObject listing=api.call("POST","/listings/room/"+room.getLong("id"),ApiClient.json("title","Android acceptance room",
                "description","Connected acceptance fixture","rent_vnd","3000000","deposit_vnd","1000000"),UUID.randomUUID().toString());
        api.call("POST","/listings/"+listing.getLong("id")+"/submit",null,UUID.randomUUID().toString());
        api.login("admin@homely.test",password);
        api.call("POST","/admin/listings/"+listing.getLong("id")+"/approve",null,UUID.randomUUID().toString());
        api.login("tenant@homely.test",password);
        String key=UUID.randomUUID().toString();JSONObject input=ApiClient.json("room_id",room.getLong("id"),"occupant_count",1);
        JSONObject booking=api.call("POST","/bookings",input,key);
        assertEquals(booking.getLong("id"),api.call("POST","/bookings",input,key).getLong("id"));
        api.login("host@homely.test",password);
        api.call("POST","/bookings/"+booking.getLong("id")+"/approve",null,UUID.randomUUID().toString());
        api.login("tenant@homely.test",password);
        JSONObject payment=api.call("POST","/payments",ApiClient.json("booking_id",booking.getLong("id")),UUID.randomUUID().toString());
        api.call("POST","/payments/"+payment.getLong("id")+"/simulate",null,UUID.randomUUID().toString());
        assertEquals("CONFIRMED",api.call("GET","/bookings/"+booking.getLong("id"),null,null).getString("status"));
        api.call("POST","/bookings/"+booking.getLong("id")+"/handover-tenant",null,UUID.randomUUID().toString());
        api.login("host@homely.test",password);
        assertEquals("COMPLETED",api.call("POST","/bookings/"+booking.getLong("id")+"/handover-host",null,UUID.randomUUID().toString()).getString("status"));
        api.login("tenant@homely.test",password);
        try(ActivityScenario<MainActivity> screen=ActivityScenario.launch(MainActivity.class)) {
            tap("Tài khoản");awaitVisible("Khách thuê demo");
            tap("Thông báo");awaitVisible("Hộp thông báo");
            tap("Lịch xem");awaitVisible("Lịch xem phòng");
            screen.recreate();awaitVisible("Lịch xem phòng");
            tap("Tài khoản");tap("Đăng xuất");awaitVisible("Đăng nhập");
        } finally { api.session.clear(); }
    }
    private void tap(String label) { onView(allOf(withText(label),isAssignableFrom(MaterialButton.class))).perform(click()); }
    private void awaitVisible(String label) throws Exception {
        AssertionError last=null;
        for(int i=0;i<60;i++){
            try {onView(allOf(withText(label),isDisplayed(),not(isAssignableFrom(MaterialButton.class)))).check(matches(isDisplayed()));return;}
            catch(AssertionError failure){last=failure;}
            catch(androidx.test.espresso.NoMatchingViewException notReady){last=new AssertionError("Screen not ready: "+label);}
            Thread.sleep(200);
        }
        throw last==null?new AssertionError("Screen not ready"):last;
    }
}
