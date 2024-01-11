package com.app.controller;

import com.app.exception.PhotoRetrievalException;
import com.app.exception.ResourceNotFoundException;
import com.app.model.BookedRoom;
import com.app.model.Room;
import com.app.responsedto.BookingResponse;
import com.app.responsedto.RoomResponse;
import com.app.service.BookingService;
import com.app.service.IRoomService;

import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@RestController
@RequiredArgsConstructor // for Auto DI
@RequestMapping("/rooms") 
public class RoomController 
{
    private final IRoomService roomService;
    private final BookingService bookingService;

    @PostMapping("/add/new-room") // tested
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<RoomResponse> addNewRoom( @RequestParam("photo") MultipartFile photo, @RequestParam("roomType") String roomType, @RequestParam("roomPrice") BigDecimal roomPrice) throws SQLException, IOException 
    {
        Room savedRoom = roomService.addNewRoom(photo, roomType, roomPrice);
        RoomResponse response = new RoomResponse(savedRoom.getId(), savedRoom.getRoomType(), savedRoom.getRoomPrice());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/room/types") // tested
    public List<String> getRoomTypes() 
    {
        return roomService.getAllRoomTypes();
    }

    @GetMapping("/all-rooms") // tested
    public ResponseEntity<List<RoomResponse>> getAllRooms() throws SQLException 
    {
        List<Room> rooms = roomService.getAllRooms();
        List<RoomResponse> roomResponses = new ArrayList<>();
        for (Room room : rooms) 
        {
            byte[] photoBytes = roomService.getRoomPhotoByRoomId(room.getId());
            if (photoBytes != null && photoBytes.length > 0) 
            {
                String base64Photo = Base64.encodeBase64String(photoBytes);
                RoomResponse roomResponse = getRoomResponse(room);
                roomResponse.setPhoto(base64Photo);
                roomResponses.add(roomResponse);
            }
        }
        return ResponseEntity.ok(roomResponses);
    }
    
    
    
    @DeleteMapping("/delete/room/{roomId}") // tested
   // @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long roomId)
    {
        roomService.deleteRoom(roomId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    
    
    @PutMapping("/update/{roomId}") //  tested
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable Long roomId,
                                                   @RequestParam(required = false)  String roomType,
                                                   @RequestParam(required = false) BigDecimal roomPrice,
                                                   @RequestParam(required = false) MultipartFile photo) throws SQLException, IOException 
    {
        byte[] photoBytes = photo != null && !photo.isEmpty() ?
                photo.getBytes() : roomService.getRoomPhotoByRoomId(roomId);
        Blob photoBlob = photoBytes != null && photoBytes.length >0 ? new SerialBlob(photoBytes): null;
        Room theRoom = roomService.updateRoom(roomId, roomType, roomPrice, photoBytes);
        theRoom.setPhoto(photoBlob);
        RoomResponse roomResponse = getRoomResponse(theRoom);
        return ResponseEntity.ok(roomResponse);
    }

    
    
    @GetMapping("/room/{roomId}") // tested
    public ResponseEntity<Optional<RoomResponse>> getRoomById(@PathVariable Long roomId){
        Optional<Room> theRoom = roomService.getRoomById(roomId);
        return theRoom.map(room -> {
            RoomResponse roomResponse = getRoomResponse(room);
            return  ResponseEntity.ok(Optional.of(roomResponse));
        }).orElseThrow(() -> new ResourceNotFoundException("Room not found"));
    }

    
    
    @GetMapping("/available-rooms") // tested
    public ResponseEntity<List<RoomResponse>> getAvailableRooms(
            @RequestParam("checkInDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate checkInDate,
            @RequestParam("checkOutDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate checkOutDate,
            @RequestParam("roomType") String roomType) throws SQLException 
    {
        List<Room> availableRooms = roomService.getAvailableRooms(checkInDate, checkOutDate, roomType);
        List<RoomResponse> roomResponses = new ArrayList<>();
        for (Room room : availableRooms){
            byte[] photoBytes = roomService.getRoomPhotoByRoomId(room.getId());
            if (photoBytes != null && photoBytes.length > 0){
                String photoBase64 = Base64.encodeBase64String(photoBytes);
                RoomResponse roomResponse = getRoomResponse(room);
                roomResponse.setPhoto(photoBase64);
                roomResponses.add(roomResponse);
            }
        }
        if(roomResponses.isEmpty()){
            return ResponseEntity.noContent().build();
        }else{
            return ResponseEntity.ok(roomResponses);
        }
    }



    

    // helper method
    private RoomResponse getRoomResponse(Room room) 
    {
       List<BookedRoom> bookings = getAllBookingsByRoomId(room.getId());
       List<BookingResponse> bookingInfo = bookings
                .stream()
                .map(booking -> new BookingResponse(booking.getBookingId(),
                        booking.getCheckInDate(),
                        booking.getCheckOutDate(), booking.getBookingConfirmationCode())).toList();
        byte[] photoBytes = null;
        Blob photoBlob = room.getPhoto();
        if (photoBlob != null) 
        {
            try 
            {
                photoBytes = photoBlob.getBytes(1, (int) photoBlob.length());
            }
            catch (SQLException e) 
            {
                throw new PhotoRetrievalException("Error retrieving photo");
            }
        }
        return new RoomResponse(room.getId(),
                room.getRoomType(), room.getRoomPrice(),
                room.isBooked(), photoBytes, bookingInfo);
    }
    
    
    // helper method
    private List<BookedRoom> getAllBookingsByRoomId(Long roomId)
    {
        return bookingService.getAllBookingsByRoomId(roomId);

    }

}




/*
 The expression `@PreAuthorize("hasRole('ROLE_ADMIN')")` is typically used in the context of Spring Security, which is a powerful and customizable authentication and access control framework for Java applications, particularly those built with the Spring Framework.

In this specific context:

- `@PreAuthorize` is an annotation in Spring Security that is used to apply authorization checks before entering a method.
  
- `"hasRole('ROLE_ADMIN')"`: This is a SpEL (Spring Expression Language) expression used within the `@PreAuthorize` annotation. It checks whether the authenticated user has the specified role, in this case, the role "ROLE_ADMIN". In Spring Security, roles are often used to represent different levels of access or permissions within an application.

Here's a breakdown:

- `hasRole('ROLE_ADMIN')`: This expression checks if the authenticated user has the role "ROLE_ADMIN". In Spring Security, roles are often prefixed with "ROLE_" convention, but it's not mandatory.

So, when a method is annotated with `@PreAuthorize("hasRole('ROLE_ADMIN')")`, it means that only users with the role "ROLE_ADMIN" are allowed to invoke that method. If the authenticated user does not have the required role, access will be denied, and an exception will be thrown.

Example usage in a Spring MVC controller method:

```java
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

    @GetMapping("/admin/resource")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String adminResource() {
        // Method logic accessible only to users with the "ROLE_ADMIN" role
        return "This is an admin resource.";
    }
}
```

In this example, the `adminResource` method can only be accessed by users who have been assigned the "ROLE_ADMIN" role. If a user without this role tries to access the resource, they will encounter an access denied error.
   
   
  =========================================================== 

If you prefer not to use annotations for role-based access control in Spring Security, you can achieve the same result by configuring security rules in the XML configuration or Java configuration (using SecurityConfigurerAdapter).  
   
If you prefer Java configuration, you can extend WebSecurityConfigurerAdapter and override the configure(HttpSecurity http) method:

java
Copy code
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/admin/resource").hasRole("ADMIN")
                .and()
            // Other configurations...
    }
    
    // Other security-related configurations...

}
This Java configuration achieves the same result as the XML configuration. It specifies that access to "/admin/resource" is restricted to users with the "ADMIN" role.
   
   
   
   
   
*/
