package com.app.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.exception.InvalidBookingRequestException;
import com.app.exception.ResourceNotFoundException;
import com.app.model.BookedRoom;
import com.app.model.Room;
import com.app.responsedto.BookingResponse;
import com.app.responsedto.RoomResponse;
import com.app.service.IBookingService;
import com.app.service.IRoomService;

import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@RestController
@RequestMapping("/bookings")
public class BookingController 
{
    private final IBookingService bookingService;
    private final IRoomService roomService;

    @GetMapping("/all-bookings")
    @PreAuthorize("hasRole('ROLE_ADMIN')") // tested
    public ResponseEntity<List<BookingResponse>> getAllBookings()
    {
        List<BookedRoom> bookings = bookingService.getAllBookings();
        List<BookingResponse> bookingResponses = new ArrayList<>();
        for (BookedRoom booking : bookings){
            BookingResponse bookingResponse = getBookingResponse(booking);
            bookingResponses.add(bookingResponse);
        }
        return ResponseEntity.ok(bookingResponses);
    }

    
    
    @PostMapping("/room/{roomId}/booking") // 
    public ResponseEntity<?> saveBooking(@PathVariable Long roomId, @RequestBody BookedRoom bookingRequest)
    {
        try
        {
            String confirmationCode = bookingService.saveBooking(roomId, bookingRequest);
            return ResponseEntity.ok("Room booked successfully, Your booking confirmation code is :"+confirmationCode);

        }
        catch (InvalidBookingRequestException e)
        {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    

    @GetMapping("/confirmation/{confirmationCode}") // tested
    public ResponseEntity<?> getBookingByConfirmationCode(@PathVariable String confirmationCode)
    {
        try
        {
            BookedRoom booking = bookingService.findByBookingConfirmationCode(confirmationCode);
            BookingResponse bookingResponse = getBookingResponse(booking);
            return ResponseEntity.ok(bookingResponse);
        }
        catch (ResourceNotFoundException ex)
        {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }

    @GetMapping("/user/{email}/bookings") // tested
    public ResponseEntity<List<BookingResponse>> getBookingsByUserEmail(@PathVariable String email) 
    {
        List<BookedRoom> bookings = bookingService.getBookingsByUserEmail(email);
        List<BookingResponse> bookingResponses = new ArrayList<>();
        for (BookedRoom booking : bookings) 
        {
            BookingResponse bookingResponse = getBookingResponse(booking);
            bookingResponses.add(bookingResponse);
        }
        return ResponseEntity.ok(bookingResponses);
    }

    
    @DeleteMapping("/booking/{bookingId}/delete") // tested
    public void cancelBooking(@PathVariable Long bookingId)
    {
        bookingService.cancelBooking(bookingId);
    }

    
    //  helper method
    private BookingResponse getBookingResponse(BookedRoom booking) 
    {
        Room theRoom = roomService.getRoomById(booking.getRoom().getId()).get();
        RoomResponse room = new RoomResponse(
                theRoom.getId(),
                theRoom.getRoomType(),
                theRoom.getRoomPrice());
        return new BookingResponse(
                booking.getBookingId(), booking.getCheckInDate(),
                booking.getCheckOutDate(),booking.getGuestFullName(),
                booking.getGuestEmail(), booking.getNumOfAdults(),
                booking.getNumOfChildren(), booking.getTotalNumOfGuest(),
                booking.getBookingConfirmationCode(), room);
    }
}
