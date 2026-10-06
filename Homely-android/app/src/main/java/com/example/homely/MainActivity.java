package com.example.homely;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.*;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.example.homely.data.*;
import com.example.homely.ui.HomeViewModel;
import org.json.*;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private HomeViewModel vm;
    private LinearLayout body;
    private TextView status,brand;
    private ProgressBar progress;
    private ChatSocket chat;
    private LinearLayout messages;
    private EditText composer;
    private JSONObject currentListing;
    private LinearLayout gallery;
    private String searchQuery="",searchProvince="",searchPrice="";
    private int searchPage;
    private final int green=Color.rgb(23,88,69),ink=Color.rgb(24,43,37),muted=Color.rgb(95,111,105);

    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);
        vm=new ViewModelProvider(this).get(HomeViewModel.class);
        if(saved!=null){vm.screen=saved.getString("screen","home");vm.selectedId=saved.getLong("selected");}
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(245,247,243));
        root.setPadding(dp(18),dp(8),dp(18),0);
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{var b=insets.getInsets(WindowInsetsCompat.Type.systemBars());v.setPadding(dp(18)+b.left,dp(8)+b.top,dp(18)+b.right,b.bottom);return insets;});
        brand=text(root,"homely",30,true);brand.setTextColor(green);
        text(root,"Tìm một nơi để gọi là nhà",13,false).setTextColor(muted);
        LinearLayout nav=row(root);
        button(nav,"Khám phá",()->route("home"));button(nav,"Đặt phòng",()->route("bookings"));button(nav,"Thông báo",()->route("inbox"));
        LinearLayout nav2=row(root);
        button(nav2,"Tin nhắn",()->route("conversations"));button(nav2,"Lịch xem",()->route("viewings"));button(nav2,"Tài khoản",()->route("profile"));
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);progress.setVisibility(View.GONE);root.addView(progress,new LinearLayout.LayoutParams(-1,dp(3)));
        status=text(root,"",13,false);status.setTextColor(muted);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(0,dp(12),0,dp(24));scroll.addView(body);
        setContentView(root);
        vm.result.observe(this,result->{
            if(result==null)return;
            progress.setVisibility(result.loading?View.VISIBLE:View.GONE);
            status.setText(result.loading?getString(R.string.status_loading):result.error==null?"":result.error);
            setActionsEnabled(body,!result.loading);
            if(result.loading || result.error!=null)return;
            try {receive(result.operation,result.data);}catch(Exception e){status.setText(R.string.status_data_unreadable);}
        });
        route(vm.screen);
        openNotification(getIntent());
        registerPush();
    }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("screen",vm.screen);out.putLong("selected",vm.selectedId);}
    @Override protected void onDestroy(){if(chat!=null)chat.close();super.onDestroy();}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);openNotification(intent);}
    private void openNotification(Intent intent){
        String type=intent.getStringExtra("ref_type"),id=intent.getStringExtra("ref_id");
        if(type==null || id==null || !vm.api.session.userId().equals(intent.getStringExtra("recipient_id")))return;
        try {vm.selectedId=Long.parseLong(id);route(switch(type){case "booking"->"booking";case "conversation"->"chat";case "viewing"->"viewing";case "listing"->user().optBoolean("host")?"host":"listing";default->"inbox";});}
        catch(NumberFormatException ignored){}
        intent.removeExtra("ref_type");
    }
    private boolean authenticated(){if(vm.api.session.loggedIn())return true;route("profile");status.setText(R.string.status_login_required);return false;}
    private JSONObject user(){JSONObject u=vm.api.session.session().optJSONObject("user");return u==null?new JSONObject():u;}
    private void route(String screen){
        vm.invalidate();vm.result.setValue(null);if(chat!=null){chat.close();chat=null;}
        vm.screen=screen;body.removeAllViews();status.setText("");progress.setVisibility(View.GONE);
        if(!Set.of("home","listing","profile","register").contains(screen) && !authenticated())return;
        switch(screen){
            case "home" -> home();
            case "profile" -> profile();
            case "register" -> register();
            case "listing" -> get("listing","/listings/"+vm.selectedId);
            case "bookings" -> get("bookings","/bookings/my?size=50");
            case "host_bookings" -> get("host_bookings","/bookings/host?size=50");
            case "booking" -> get("booking","/bookings/"+vm.selectedId);
            case "inbox" -> get("inbox","/notifications?size=50");
            case "conversations" -> get("conversations","/conversations?size=50");
            case "chat" -> chatScreen();
            case "viewings" -> get("viewings","/viewings/my?size=50");
            case "viewing" -> get("viewing","/viewings/"+vm.selectedId);
            case "host_viewings" -> get("host_viewings","/viewings/host?size=50");
            case "host" -> get("host","/listings/my?size=50");
            case "new_room" -> newRoom();
            case "admin" -> get("admin","/admin/listings?size=50");
            case "admin_cases" -> get("admin_cases","/admin/cases?size=50");
            case "cases" -> get("cases","/bookings/"+vm.selectedId+"/cases");
            default -> home();
        }
    }
    private void get(String operation,String path){vm.request(operation,"GET",path,null);}
    private void action(String path,JSONObject body){vm.request("action","POST",path,body);}
    private void receive(String op,JSONObject data) throws Exception {
        switch(op){
            case "search" -> listingCards(data);
            case "listing" -> listing(data);
            case "bookings","host_bookings" -> bookingCards(data);
            case "booking" -> booking(data);
            case "created_booking" -> {vm.selectedId=data.getLong("id");route("booking");}
            case "payment" -> payment(data);
            case "simulated" -> {vm.selectedId=data.getLong("booking_id");route("booking");}
            case "inbox" -> inbox(data);
            case "conversations" -> conversations(data);
            case "created_chat" -> {vm.selectedId=data.getLong("id");route("chat");}
            case "messages" -> messageHistory(data);
            case "slots" -> slots(data);
            case "viewings","host_viewings" -> viewings(data);
            case "viewing" -> viewings(ApiClient.json("data",new JSONArray().put(data)));
            case "host" -> hostListings(data);
            case "admin" -> adminListings(data);
            case "admin_cases" -> adminCases(data);
            case "cases" -> caseList(data);
            case "login" -> {registerPush();route("home");}
            case "registered" -> {route("profile");status.setText(R.string.status_registered);}
            case "logout" -> {getSystemService(NotificationManager.class).cancelAll();route("profile");}
            case "new_room" -> {vm.roomDraftKey=UUID.randomUUID().toString();route("host");status.setText(R.string.status_listing_submitted);}
            case "photos" -> showPhotos(data);
            case "photo_uploaded" -> {route("host");status.setText(R.string.status_photo_added);}
            case "action" -> {route(vm.screen);status.setText(R.string.status_updated);}
            case "email" -> status.setText(R.string.status_email_sent);
            case "verified" -> status.setText(R.string.status_email_verified);
            case "viewing_created" -> route("viewings");
        }
    }
    private void home(){
        title("Phòng phù hợp với bạn");
        LinearLayout filters=card(body);
        EditText q=field(filters,"Từ khóa / địa chỉ",searchQuery,false),province=field(filters,"Mã tỉnh/thành (ví dụ HN)",searchProvince,false);
        EditText price=field(filters,"Giá tối đa / tháng (VND)",searchPrice,true);
        button(filters,"Tìm phòng",()->{searchQuery=value(q);searchProvince=value(province);searchPrice=value(price);searchPage=0;search();});
        search();
    }
    private void search(){
        String path="/listings?page="+searchPage+"&size=10&sort=newest";
        if(!searchQuery.isBlank())path+="&query="+encode(searchQuery);
        if(!searchProvince.isBlank())path+="&province_code="+encode(searchProvince);
        if(!searchPrice.isBlank())path+="&max_rent_vnd="+encode(searchPrice);
        get("search",path);
    }
    private void listingCards(JSONObject data){
        while(body.getChildCount()>2)body.removeViewAt(2);
        JSONArray items=data.optJSONArray("data");
        if(items==null || items.length()==0){text(body,"Chưa có phòng phù hợp. Thử thay đổi bộ lọc.",16,false);return;}
        each(items,l->{
            LinearLayout c=card(body);JSONObject room=obj(l,"room");
            text(c,l.optString("title"),20,true);
            text(c,money(l.optString("rent_vnd"))+" / tháng",19,true).setTextColor(green);
            text(c,room.optString("area_m2")+" m² · "+room.optInt("max_occupants")+" người",14,false);
            text(c,obj(room,"address").optString("line"),14,false);
            button(c,"Xem phòng",()->{vm.selectedId=l.optLong("id");route("listing");});
        });
        LinearLayout paging=row(body);
        if(searchPage>0)button(paging,"Trang trước",()->{searchPage--;search();});
        if(searchPage+1<data.optInt("total_pages"))button(paging,"Trang sau",()->{searchPage++;search();});
    }
    private void listing(JSONObject l){
        body.removeAllViews();currentListing=l;
        title(l.optString("title"));JSONObject r=obj(l,"room");
        text(body,obj(r,"address").optString("line"),16,false);
        LinearLayout c=card(body);
        text(c,money(l.optString("rent_vnd"))+" / tháng",26,true).setTextColor(green);
        text(c,"Tiền cọc: "+money(l.optString("deposit_vnd")),16,false);
        text(c,r.optString("area_m2")+" m² · tối đa "+r.optInt("max_occupants")+" người · "+label(r.optString("availability")),15,false);
        text(body,l.optString("description"),16,false);
        gallery=new LinearLayout(this);gallery.setOrientation(LinearLayout.VERTICAL);body.addView(gallery);
        get("photos","/media/room/"+r.optLong("id"));
        each(l.optJSONArray("fees"),f->text(body,f.optString("fee_code")+": "+money(f.optString("amount_vnd"))+" / "+f.optString("unit_name","tháng"),14,false));
        button(body,"Nhắn tin cho chủ phòng",()->{if(authenticated())vm.request("created_chat","POST","/conversations",ApiClient.json("room_id",r.optLong("id")));});
        if("AVAILABLE".equals(r.optString("availability")))button(body,"Gửi yêu cầu thuê",()->{
            if(!authenticated())return;
            EditText occupants=new EditText(this);occupants.setInputType(InputType.TYPE_CLASS_NUMBER);occupants.setText("1");
            new AlertDialog.Builder(this).setTitle("Số người ở").setView(occupants).setNegativeButton("Hủy",null)
                    .setPositiveButton("Gửi",(d,w)->safe(()->vm.request("created_booking","POST","/bookings",
                            ApiClient.json("room_id",r.optLong("id"),"occupant_count",Integer.parseInt(value(occupants)))))).show();
        });
        button(body,"Chọn lịch xem phòng",()->{if(authenticated())get("slots","/viewing-slots/"+r.optLong("id"));});
        button(body,"Tải lại",()->route("listing"));
    }
    private void bookingCards(JSONObject data){
        body.removeAllViews();title(vm.screen.equals("host_bookings")?"Yêu cầu thuê của khách":"Yêu cầu thuê của tôi");
        JSONArray items=data.optJSONArray("data");if(items==null || items.length()==0)text(body,"Chưa có yêu cầu thuê.",16,false);
        each(items,b->{LinearLayout c=card(body);text(c,"Yêu cầu #"+b.optLong("id"),19,true);text(c,label(b.optString("status")),16,true).setTextColor(green);
            text(c,"Giá thuê: "+money(b.optString("rent_vnd")),15,false);button(c,"Xem tiến trình",()->{vm.selectedId=b.optLong("id");route("booking");});});
        button(body,"Tải lại",()->route(vm.screen));
    }
    private void booking(JSONObject b){
        body.removeAllViews();title("Yêu cầu thuê #"+b.optLong("id"));
        text(body,label(b.optString("status")),23,true).setTextColor(green);
        text(body,"Giá thuê: "+money(b.optString("rent_vnd"))+" / tháng",17,false);
        text(body,"Tiền cọc: "+money(b.optString("deposit_vnd")),17,false);
        if(!b.isNull("hold_expires_at"))text(body,"Hạn giữ phòng: "+b.optString("hold_expires_at"),14,false);
        boolean host=vm.api.session.userId().equals(b.optString("host_id"));
        String base="/bookings/"+b.optLong("id"),state=b.optString("status");
        if(state.equals("PENDING") && host){
            button(body,"Duyệt yêu cầu",()->action(base+"/approve",null));
            button(body,"Từ chối",()->reason("Lý do từ chối",s->action(base+"/reject",ApiClient.json("reason",s))));
        }
        if(state.equals("APPROVED") && !host)button(body,"Thanh toán cọc (sandbox)",()->vm.request("payment","POST","/payments",ApiClient.json("booking_id",b.optLong("id"))));
        if(state.equals("PENDING") || state.equals("APPROVED"))button(body,"Hủy yêu cầu",()->reason("Lý do hủy",s->action(base+"/cancel",ApiClient.json("reason",s))));
        if(state.equals("CONFIRMED")){
            button(body,host?"Xác nhận đã bàn giao":"Xác nhận đã nhận phòng",()->action(base+(host?"/handover-host":"/handover-tenant"),null));
            button(body,"Mở yêu cầu hỗ trợ",()->reason("Mô tả vấn đề",s->action(base+"/cases",ApiClient.json("type","DEPOSIT_DISPUTE","description",s))));
            button(body,"Xem yêu cầu hỗ trợ",()->route("cases"));
        }
        button(body,"Cập nhật trạng thái",()->route("booking"));
    }
    private void payment(JSONObject p){
        body.removeAllViews();title("Thanh toán thử nghiệm");
        text(body,"Số tiền cọc: "+money(p.optString("amount_vnd")),22,true);
        text(body,"Đây là giao dịch giả lập; không chuyển tiền thật.",16,false);
        text(body,"Trạng thái: "+label(p.optString("status")),16,false);
        button(body,"Mô phỏng thanh toán thành công",()->vm.request("simulated","POST","/payments/"+p.optLong("id")+"/simulate",null));
        button(body,"Quay về yêu cầu thuê",()->{vm.selectedId=p.optLong("booking_id");route("booking");});
    }
    private void slots(JSONObject data){
        body.removeAllViews();title("Lịch xem phòng");
        JSONArray list=data.optJSONArray("data");if(list==null||list.length()==0)text(body,"Chủ phòng chưa có lịch trống. Bạn có thể nhắn tin để hẹn.",16,false);
        each(list,s->{LinearLayout c=card(body);text(c,s.optString("start_at"),18,true);text(c,"Đến "+s.optString("end_at"),15,false);
            button(c,"Đặt lịch này",()->vm.request("viewing_created","POST","/viewings",ApiClient.json("viewing_slot_id",s.optLong("id"))));});
    }
    private void viewings(JSONObject data){
        body.removeAllViews();title("Lịch xem phòng");
        JSONArray items=data.optJSONArray("data");if(items==null||items.length()==0)text(body,"Chưa có lịch xem phòng.",16,false);
        each(items,v->{LinearLayout c=card(body);text(c,"Lịch #"+v.optLong("id")+" · "+label(v.optString("status")),18,true);text(c,v.optString("slot_start_at"),15,false);
            String path="/viewings/"+v.optLong("id");
            if(vm.api.session.userId().equals(v.optString("host_id")) && v.optString("status").equals("REQUESTED"))button(c,"Xác nhận",()->action(path+"/confirm",null));
            if(Set.of("REQUESTED","CONFIRMED").contains(v.optString("status")))button(c,"Hủy lịch",()->action(path+"/cancel",null));
        });
    }
    private void inbox(JSONObject data){
        body.removeAllViews();title("Hộp thông báo");
        button(body,"Đánh dấu tất cả đã đọc",()->action("/notifications/read-all",null));
        JSONArray list=data.optJSONArray("data");if(list==null||list.length()==0)text(body,"Bạn chưa có thông báo.",16,false);
        each(list,n->{LinearLayout c=card(body);text(c,(n.optBoolean("is_read")?"":"● ")+n.optString("title"),18,true);text(c,n.optString("body"),15,false);
            button(c,"Mở",()->{
                vm.api.http.dispatcher().executorService().execute(()->{try{vm.api.call("POST","/notifications/"+n.optLong("id")+"/read",null,UUID.randomUUID().toString());}catch(Exception ignored){}});
                vm.selectedId=n.optLong("ref_id");
                route(switch(n.optString("ref_type")){case "booking"->"booking";case "conversation"->"chat";case "viewing"->"viewing";case "listing"->user().optBoolean("host")?"host":"listing";default->"profile";});
            });
        });
        button(body,"Tải lại",()->route("inbox"));
    }
    private void conversations(JSONObject data){
        body.removeAllViews();title("Tin nhắn");
        JSONArray list=data.optJSONArray("data");if(list==null||list.length()==0)text(body,"Mở một phòng để bắt đầu trò chuyện với chủ nhà.",16,false);
        each(list,c->{LinearLayout v=card(body);text(v,c.optString("room_title"),18,true);text(v,c.optString("last_message_preview","Chưa có tin nhắn"),15,false);
            button(v,"Mở hội thoại · "+c.optLong("unread_count")+" chưa đọc",()->{vm.selectedId=c.optLong("id");route("chat");});});
    }
    private void chatScreen(){
        title("Hội thoại #"+vm.selectedId);
        messages=new LinearLayout(this);messages.setOrientation(LinearLayout.VERTICAL);body.addView(messages);
        composer=field(body,"Tin nhắn","",false);
        button(body,"Gửi",()->{
            String content=value(composer);if(content.isBlank())return;
            if(!content.equals(vm.pendingContent)){vm.pendingContent=content;vm.pendingMessageId=UUID.randomUUID().toString();}
            if(chat==null || !chat.send(ApiClient.json("conversation_id",vm.selectedId,"content",content,"content_type","TEXT","client_message_id",vm.pendingMessageId)))
                status.setText(R.string.status_chat_disconnected);
        });
        button(body,"Kết nối lại / đồng bộ",()->{connectChat();get("messages","/conversations/"+vm.selectedId+"/messages?limit=50");});
        connectChat();get("messages","/conversations/"+vm.selectedId+"/messages?limit=50");
    }
    private void connectChat(){
        if(chat!=null)chat.close();
        chat=new ChatSocket(vm.api,new ChatSocket.Listener(){
            public void ready(){runOnUiThread(()->{if(!isDestroyed())status.setText("Đã kết nối");});}
            public void error(String text){runOnUiThread(()->{if(!isDestroyed()&&vm.screen.equals("chat"))status.setText(text);});}
            public void changed(JSONObject m){runOnUiThread(()->{
                if(isDestroyed()||!vm.screen.equals("chat")||m.optLong("conversation_id")!=vm.selectedId)return;
                if(m.optString("client_message_id").equals(vm.pendingMessageId)){composer.setText("");vm.pendingMessageId=null;vm.pendingContent=null;}
                get("messages","/conversations/"+vm.selectedId+"/messages?limit=50");
            });}
        });chat.connect();
    }
    private void messageHistory(JSONObject data){
        if(messages==null || !vm.screen.equals("chat"))return;
        messages.removeAllViews();JSONArray list=data.optJSONArray("data");long last=0;
        if(list==null||list.length()==0)text(messages,"Gửi lời chào để bắt đầu cuộc trò chuyện.",16,false);
        if(list!=null)for(int i=0;i<list.length();i++){JSONObject m=list.optJSONObject(i);if(m==null)continue;
            text(messages,(vm.api.session.userId().equals(m.optString("sender_id"))?"Bạn: ":"Chủ đề phòng: ")+m.optString("content"),16,false);
            last=Math.max(last,m.optLong("sequence"));
        }
        if(last>0){long marker=last;long id=vm.selectedId;vm.api.http.dispatcher().executorService().execute(()->{try{vm.api.call("POST","/conversations/"+id+"/read-marker",ApiClient.json("last_read_sequence",marker),UUID.randomUUID().toString());}catch(Exception ignored){}});}
    }
    private void profile(){
        title(vm.api.session.loggedIn()?"Tài khoản":"Đăng nhập");
        if(!vm.api.session.loggedIn()){
            EditText email=field(body,"Email","",false),password=field(body,"Mật khẩu","",false);password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
            button(body,"Đăng nhập",()->{String loginEmail=value(email),loginPassword=value(password);
                vm.run("login",()->{vm.api.login(loginEmail,loginPassword);return new JSONObject();});});
            button(body,"Tạo tài khoản",()->route("register"));return;
        }
        text(body,user().optString("full_name",user().optString("email")),22,true);text(body,user().optString("email"),16,false);
        button(body,"Cho phép thông báo",()->{if(Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},100);registerPush();});
        button(body,"Gửi lại email xác minh",()->vm.request("email","POST","/auth/verify-email/resend",null));
        if(!user().optBoolean("email_verified"))button(body,"Nhập mã xác minh email",()->reason("Mã xác minh từ email",token->vm.run("login",()->{
            vm.api.call("POST","/auth/verify-email",ApiClient.json("token",token),null);vm.api.refreshSession();return new JSONObject();})));
        if(user().optBoolean("host")){
            button(body,"Quản lý tin đăng",()->route("host"));button(body,"Yêu cầu thuê của khách",()->route("host_bookings"));button(body,"Lịch xem của khách",()->route("host_viewings"));
        }else button(body,"Đăng ký làm chủ nhà",()->vm.run("login",()->{
            vm.api.call("POST","/auth/enable-host",null,null);vm.api.refreshSession();return new JSONObject();}));
        if(user().optJSONArray("roles")!=null && user().optJSONArray("roles").toString().contains("ROLE_ADMIN")){
            button(body,"Duyệt tin đăng",()->route("admin"));button(body,"Xử lý tranh chấp",()->route("admin_cases"));
        }
        button(body,"Đăng xuất",()->vm.run("logout",()->{vm.api.logout();return new JSONObject();}));
    }
    private void register(){
        title("Tạo tài khoản");
        EditText name=field(body,"Họ tên","",false),email=field(body,"Email","",false),password=field(body,"Mật khẩu (ít nhất 12 ký tự)","",false);
        password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        button(body,"Đăng ký",()->vm.request("registered","POST","/auth/register",ApiClient.json("full_name",value(name),"email",value(email),"password",value(password))));
        button(body,"Đã có tài khoản",()->route("profile"));
    }
    private void hostListings(JSONObject data){
        body.removeAllViews();title("Tin đăng của tôi");button(body,"Đăng phòng mới",()->route("new_room"));
        each(data.optJSONArray("data"),l->{LinearLayout c=card(body);text(c,l.optString("title"),19,true);text(c,label(l.optString("status")),15,false);
            button(c,"Thêm ảnh phòng",()->{vm.photoRoomId=l.optLong("room_id");Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);
                pick.setType("image/*");pick.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(pick,201);});
            button(c,"Thêm lịch xem (1 giờ)",()->chooseViewingTime(l.optLong("room_id")));
            if(Set.of("DRAFT","HIDDEN","REJECTED","EXPIRED").contains(l.optString("status")))button(c,"Sửa nội dung",()->editListing(l));
            if(Set.of("DRAFT","HIDDEN","REJECTED","EXPIRED").contains(l.optString("status")))button(c,"Gửi duyệt",()->action("/listings/"+l.optLong("id")+"/submit",null));
            if("PUBLISHED".equals(l.optString("status")))button(c,"Ẩn tin",()->action("/listings/"+l.optLong("id")+"/hide",null));
        });
    }
    private void newRoom(){
        title("Đăng phòng mới");
        EditText code=field(body,"Mã phòng (A-Z, 0-9)","",false),title=field(body,"Tiêu đề","",false),address=field(body,"Địa chỉ","",false);
        EditText province=field(body,"Mã tỉnh/thành","HN",false),ward=field(body,"Mã phường/xã","",false),wardName=field(body,"Tên phường/xã","",false),area=field(body,"Diện tích m²","25",true),rent=field(body,"Giá thuê VND","3000000",true),deposit=field(body,"Tiền cọc VND","1000000",true);
        EditText lat=field(body,"Vĩ độ","21.0285",false),lon=field(body,"Kinh độ","105.8542",false);
        button(body,"Tạo phòng và gửi duyệt",()->{
            JSONObject roomInput=ApiClient.json("unit_code",value(code),"room_type","SINGLE_ROOM",
                    "area_m2",value(area),"max_occupants",2,"amenity_ids",new JSONArray(),
                    "address",ApiClient.json("line",value(address),"province_code",value(province),"province_name",value(province),"ward_code",value(ward),"ward_name",value(wardName)),
                    "location",ApiClient.json("latitude",Double.parseDouble(value(lat)),"longitude",Double.parseDouble(value(lon))));
            JSONObject listingInput=ApiClient.json("title",value(title),"description",value(address),"rent_vnd",value(rent),"deposit_vnd",value(deposit));
            String key=vm.roomDraftKey;
            vm.run("new_room",()->{
            JSONObject room=vm.api.call("POST","/rooms",roomInput,key+"-room");
            JSONObject listing=vm.api.call("POST","/listings/room/"+room.getLong("id"),listingInput,key+"-listing");
            return vm.api.call("POST","/listings/"+listing.getLong("id")+"/submit",null,key+"-submit");
        });});
    }
    private void adminListings(JSONObject data){
        body.removeAllViews();title("Tin chờ duyệt");
        each(data.optJSONArray("data"),l->{LinearLayout c=card(body);text(c,l.optString("title"),18,true);
            text(c,money(l.optString("rent_vnd"))+" / tháng · cọc "+money(l.optString("deposit_vnd")),16,false);
            text(c,obj(obj(l,"room"),"address").optString("line"),15,false);text(c,l.optString("description"),15,false);
            button(c,"Duyệt tin",()->action("/admin/listings/"+l.optLong("id")+"/approve",null));
            button(c,"Từ chối",()->reason("Lý do",s->action("/admin/listings/"+l.optLong("id")+"/reject?reason="+encode(s),null)));
        });
    }
    private void caseList(JSONObject data){
        body.removeAllViews();title("Yêu cầu hỗ trợ");
        each(data.optJSONArray("data"),c->{LinearLayout v=card(body);text(v,"Yêu cầu #"+c.optLong("id")+" · "+label(c.optString("status")),18,true);
            text(v,c.optString("description"),16,false);if(!c.isNull("resolution_note"))text(v,c.optString("resolution_note"),16,false);
        });
        button(body,"Quay lại",()->route("booking"));
    }

    private void adminCases(JSONObject data){
        body.removeAllViews();title("Tranh chấp cần xử lý");
        JSONArray list=data.optJSONArray("data");if(list==null||list.length()==0)text(body,"Không có yêu cầu đang mở.",16,false);
        each(list,item->{LinearLayout c=card(body);text(c,"Yêu cầu #"+item.optLong("id")+" · booking #"+item.optLong("booking_id"),18,true);
            text(c,item.optString("description"),16,false);
            for(String decision:List.of("REFUND_TENANT","FORFEIT_DEPOSIT","SPLIT","NO_ACTION")){
                String caption=switch(decision){case "REFUND_TENANT"->"Hoàn toàn bộ cọc";case "FORFEIT_DEPOSIT"->"Giữ toàn bộ cọc";case "SPLIT"->"Chia tiền cọc";default->"Đóng, không điều chỉnh tiền";};
                button(c,caption,()->reason("Lý do quyết định",note->{
                    String path="/admin/cases/"+item.optLong("id")+"/resolve?decision="+decision+"&note="+encode(note);
                    if(decision.equals("SPLIT"))reason("Số VND hoàn cho khách thuê",amount->action(path+"&tenant_refund_vnd="+encode(amount),null));
                    else action(path,null);
                }));
            }
        });
    }
    private void registerPush(){
        if(FirebaseApp.getApps(this).isEmpty())return;
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token->vm.api.http.dispatcher().executorService().execute(()->{try{vm.api.registerPush(token);}catch(Exception ignored){}}));
    }

    private void chooseViewingTime(long roomId){
        java.util.Calendar date=java.util.Calendar.getInstance();date.add(java.util.Calendar.DAY_OF_MONTH,2);
        new DatePickerDialog(this,(picker,y,m,d)->new TimePickerDialog(this,(time,h,min)->{
            date.set(y,m,d,h,min,0);date.set(java.util.Calendar.MILLISECOND,0);
            java.time.Instant start=date.toInstant();
            action("/viewing-slots",ApiClient.json("room_id",roomId,"start_at",start.toString(),"end_at",start.plusSeconds(3600).toString()));
        },9,0,true).show(),date.get(java.util.Calendar.YEAR),date.get(java.util.Calendar.MONTH),date.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    private void editListing(JSONObject listing){
        LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.VERTICAL);fields.setPadding(dp(18),0,dp(18),0);
        EditText name=field(fields,"Tiêu đề",listing.optString("title"),false),description=field(fields,"Mô tả",listing.optString("description"),false);
        EditText rent=field(fields,"Giá thuê VND",listing.optString("rent_vnd"),true),deposit=field(fields,"Tiền cọc VND",listing.optString("deposit_vnd"),true);
        new AlertDialog.Builder(this).setTitle("Sửa tin đăng").setView(fields).setNegativeButton("Hủy",null).setPositiveButton("Lưu",(d,w)->
                vm.request("action","PATCH","/listings/"+listing.optLong("id"),ApiClient.json("title",value(name),"description",value(description),
                        "rent_vnd",value(rent),"deposit_vnd",value(deposit),"expected_version",listing.optInt("version")))).show();
    }

    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=201||result!=RESULT_OK||data==null||data.getData()==null)return;
        android.net.Uri uri=data.getData();long roomId=vm.photoRoomId;ContentResolver resolver=getContentResolver();
        vm.run("photo_uploaded",()->{
            String type=resolver.getType(uri);
            if(type==null||!Set.of("image/jpeg","image/png","image/webp").contains(type))throw new IllegalArgumentException("Chọn ảnh JPEG, PNG hoặc WebP.");
            byte[] bytes;
            try(java.io.InputStream in=resolver.openInputStream(uri);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){
                if(in==null)throw new java.io.IOException("Không mở được ảnh.");byte[] buffer=new byte[8192];int read;
                while((read=in.read(buffer))!=-1){out.write(buffer,0,read);if(out.size()>10*1024*1024)throw new java.io.IOException("Ảnh tối đa 10 MB.");}bytes=out.toByteArray();
            }
            JSONObject photo=vm.api.uploadPhoto(bytes,type);
            return vm.api.call("POST","/media/attach/room/"+roomId,ApiClient.json("media_ids",new JSONArray().put(photo.getLong("id"))),null);
        });
    }

    private void showPhotos(JSONObject data){
        if(gallery==null||!vm.screen.equals("listing"))return;
        LinearLayout target=gallery;each(data.optJSONArray("data"),photo->{
            if(!photo.optString("content_type").startsWith("image/"))return;
            ImageView view=new ImageView(this);view.setContentDescription("Ảnh phòng");view.setScaleType(ImageView.ScaleType.CENTER_CROP);target.addView(view,new LinearLayout.LayoutParams(-1,dp(220)));
            String url=java.net.URI.create(BuildConfig.API_URL).resolve(photo.optString("url")).toString();
            vm.api.http.newCall(new okhttp3.Request.Builder().url(url).build()).enqueue(new okhttp3.Callback(){
                public void onFailure(okhttp3.Call call,java.io.IOException error){}
                public void onResponse(okhttp3.Call call,okhttp3.Response response) throws java.io.IOException {
                    try(response){if(!response.isSuccessful()||response.body()==null)return;byte[] bytes=response.body().bytes();
                        android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inJustDecodeBounds=true;
                        android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);options.inSampleSize=1;
                        while(options.outWidth/options.inSampleSize>1200||options.outHeight/options.inSampleSize>1200)options.inSampleSize*=2;
                        options.inJustDecodeBounds=false;var bitmap=android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
                        runOnUiThread(()->{if(!isDestroyed()&&target==gallery)view.setImageBitmap(bitmap);});
                    }
                }
            });
        });
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void title(String t){text(body,t,25,true);}
    private TextView text(LinearLayout parent,String value,int size,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(ink);t.setPadding(0,dp(6),0,dp(6));
        if(bold)t.setTypeface(null,Typeface.BOLD);parent.addView(t);return t;
    }
    private LinearLayout row(LinearLayout parent){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);parent.addView(r);return r;}
    private LinearLayout card(LinearLayout parent){
        MaterialCardView card=new MaterialCardView(this);card.setRadius(dp(16));card.setCardBackgroundColor(Color.WHITE);card.setStrokeColor(Color.rgb(218,228,220));card.setStrokeWidth(dp(1));
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.setMargins(0,dp(8),0,dp(8));parent.addView(card,params);
        LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(16),dp(12),dp(16),dp(12));card.addView(content);return content;
    }
    private EditText field(LinearLayout parent,String hint,String initial,boolean number){
        TextInputLayout wrap=new TextInputLayout(this);wrap.setHint(hint);wrap.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        TextInputEditText input=new TextInputEditText(this);input.setText(initial);input.setSingleLine(true);
        input.setId(View.generateViewId());if(number)input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        wrap.addView(input);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(6),0,dp(6));parent.addView(wrap,p);return input;
    }
    private void button(LinearLayout parent,String label,Runnable click){
        MaterialButton b=new MaterialButton(this);b.setText(label);b.setTextSize(13);b.setAllCaps(false);b.setCornerRadius(dp(12));b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(green));
        LinearLayout.LayoutParams params=parent.getOrientation()==LinearLayout.HORIZONTAL?new LinearLayout.LayoutParams(0,-2,1):new LinearLayout.LayoutParams(-1,-2);
        params.setMargins(dp(2),dp(3),dp(2),dp(3));parent.addView(b,params);b.setOnClickListener(v->safe(click));
    }
    private void setActionsEnabled(ViewGroup parent,boolean enabled){for(int i=0;i<parent.getChildCount();i++){View v=parent.getChildAt(i);if(v instanceof MaterialButton)v.setEnabled(enabled);if(v instanceof ViewGroup)setActionsEnabled((ViewGroup)v,enabled);}}
    private String value(EditText e){return e.getText().toString().trim();}
    private JSONObject obj(JSONObject p,String key){JSONObject o=p.optJSONObject(key);return o==null?new JSONObject():o;}
    private void each(JSONArray list,java.util.function.Consumer<JSONObject> action){if(list!=null)for(int i=0;i<list.length();i++){JSONObject o=list.optJSONObject(i);if(o!=null)action.accept(o);}}
    private String money(String value){try{return NumberFormat.getIntegerInstance(new Locale("vi","VN")).format(new java.math.BigDecimal(value))+" đ";}catch(Exception e){return "—";}}
    private String encode(String value){try{return URLEncoder.encode(value,"UTF-8");}catch(Exception e){throw new IllegalArgumentException(e);}}
    private void safe(Runnable action){try{action.run();}catch(Exception e){status.setText(R.string.status_invalid_input);}}
    private void reason(String title,java.util.function.Consumer<String> next){EditText input=new EditText(this);new AlertDialog.Builder(this).setTitle(title).setView(input).setNegativeButton("Hủy",null).setPositiveButton("Gửi",(d,w)->{if(!value(input).isBlank())next.accept(value(input));}).show();}
    private String label(String state){return switch(state){
        case "PENDING","PENDING_REVIEW","REQUESTED"->"Đang chờ xác nhận";case "APPROVED"->"Đã duyệt · chờ cọc";case "CONFIRMED"->"Đã xác nhận";
        case "COMPLETED"->"Hoàn tất";case "CANCELLED","CANCELLED_BY_HOST","CANCELLED_BY_TENANT"->"Đã hủy";case "EXPIRED"->"Đã hết hạn";
        case "PUBLISHED"->"Đang hiển thị";case "DRAFT"->"Bản nháp";case "AVAILABLE"->"Còn phòng";case "HELD"->"Đang giữ chỗ";case "RENTED"->"Đã cho thuê";
        case "SUCCEEDED"->"Thành công";case "OPEN"->"Đang xử lý";case "RESOLVED"->"Đã giải quyết";default->state;};}
}
