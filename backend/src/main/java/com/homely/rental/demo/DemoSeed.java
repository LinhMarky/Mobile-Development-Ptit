package com.homely.rental.demo;

import com.homely.rental.auth.entity.*;
import com.homely.rental.auth.repository.*;
import com.homely.rental.catalog.entity.*;
import com.homely.rental.catalog.repository.*;
import com.homely.rental.interaction.entity.*;
import com.homely.rental.interaction.repository.ViewingSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Opt-in fixture creation; never changes an existing account or booking. */
@Component @RequiredArgsConstructor
@ConditionalOnProperty(name="homely.demo.seed", havingValue="true")
public class DemoSeed implements ApplicationRunner {
    private final UserRepository users;
    private final RoleRepository roles;
    private final RoomRepository rooms;
    private final ListingRepository listings;
    private final ViewingSlotRepository slots;
    private final PasswordEncoder encoder;
    @Value("${homely.demo.password:}") private String password;
    @Value("${homely.demo.enabled:false}") private boolean demoEnabled;

    @Override @Transactional public void run(ApplicationArguments args) {
        if(!demoEnabled || password.length()<12) throw new IllegalStateException("Demo seed requires DEMO_ENABLED=true and DEMO_PASSWORD with at least 12 characters");
        List<String> emails=List.of("admin@homely.test","host@homely.test","tenant@homely.test");
        long existing=emails.stream().filter(e->users.findByEmail(e)!=null).count();
        if(existing==3)return;
        if(existing!=0)throw new IllegalStateException("Demo accounts partially exist; use a separate empty demo database");
        for(RoleName name:RoleName.values())if(roles.findByName(name).isEmpty()){
            Role role=new Role();role.setName(name);roles.save(role);
        }
        createUser(emails.get(0),"Quản trị demo",RoleName.ROLE_ADMIN);
        User host=createUser(emails.get(1),"Chủ nhà demo",RoleName.ROLE_HOST);
        createUser(emails.get(2),"Khách thuê demo",RoleName.ROLE_TENANT);
        for(int i=1;i<=3;i++) {
            Room room=new Room();room.setHost(host);room.setUnitCode("DEMO_"+i);room.setRoomType(RoomType.SINGLE_ROOM);
            room.setAreaM2(BigDecimal.valueOf(20+i*5));room.setMaxOccupants(2);
            room.setAddress(new Address("Phòng mẫu "+i+", khu vực Hà Đông","HN","Hà Nội","HA_DONG","Hà Đông"));
            room.setLocation(new GeoPoint(20.97+i*0.001,105.78+i*0.001));rooms.save(room);
            Listing listing=new Listing();listing.setRoom(room);listing.setTitle("Phòng thoáng gần trường · "+room.getAreaM2()+" m²");
            listing.setDescription("Dữ liệu minh họa cho đồ án. Phòng có cửa sổ, lối đi riêng; liên hệ chủ nhà để hẹn xem.");
            listing.setRentVnd(BigDecimal.valueOf(2500000+i*500000));listing.setDepositVnd(BigDecimal.valueOf(1000000));
            listing.setStatus(i==3?ListingStatus.PENDING_REVIEW:ListingStatus.PUBLISHED);
            if(i<3){listing.setPublishedAt(Instant.now());listing.setExpiresAt(Instant.now().plus(30,ChronoUnit.DAYS));}
            listings.save(listing);
            ViewingSlot slot=new ViewingSlot();slot.setRoom(room);slot.setHost(host);
            slot.setStartAt(Instant.now().plus(3,ChronoUnit.DAYS));slot.setEndAt(slot.getStartAt().plusSeconds(3600));slots.save(slot);
        }
    }
    private User createUser(String email,String name,RoleName role) {
        User user=new User();user.setEmail(email);user.setFullName(name);user.setPassword(encoder.encode(password));user.setEmailVerified(true);
        user.getRoles().add(roles.findByName(role).orElseThrow());
        if(role==RoleName.ROLE_HOST)user.getRoles().add(roles.findByName(RoleName.ROLE_TENANT).orElseThrow());
        return users.save(user);
    }
}
