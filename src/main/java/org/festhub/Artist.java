package org.festhub;
import java.util.Scanner;
import java.util.UUID;

class Artist {

    // Variable declaration
    private String artistID;
    private String artistName;
    private String bookingContact;
    private String[] eventIDs; // exists in other classes; ensure sync

// ------------------------------------------------------------------- //

/* CONSTRUCTORS for new Artist instance/object */
        // 'artistID' AND 'eventIDs' are NOT provided
    public Artist(String newName, String newContact){
        setArtistName(newName);
        setBookingContact(newContact);
        this.artistID = UUID.randomUUID().toString();
        this.eventIDs = new String[0];
    }

        //  Only 'eventIDs' is NOT provided 
    public Artist(String newName, String newContact, String artistUUID){
        setArtistName(newName);
        setBookingContact(newContact);
        this.artistID = artistUUID;
        this.eventIDs = new String[0];
    }

        // ALL variables are explicitly entered
    public Artist(String newName, String newContact, String artistUUID, String[] newEventIDs){
        setArtistName(newName);
        setBookingContact(newContact);
        this.artistID = artistUUID;
        this.eventIDs = newEventIDs;
    }

// ----------------------------------------------------------------------------//

/* In-class methods */

    //*  INPUT VALIDATORS  *//
        // Input validator for 'artistName' - can't be >2000 chars
    public void setArtistName(String artistName){
        if (artistName != null && artistName.length() > 2000){
            throw new IllegalArgumentException("Artist Name cannot exceed 2000 characters.");
        }
        this.artistName = artistName;
    }

        // Input validation for 'BookingContact' - must be valid email format
    public void setBookingContact(String bookingContact){

        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"; // regex expression for expected email address format
        if (bookingContact == null || !bookingContact.matches(emailRegex)){
            throw new IllegalArgumentException("Booking contact must be a valid email address."); 
        }
    }   

        // Input validation for valid Concert Event UUIDs - ???
    public void setEventIDs(){
            // TBD
    }

    //* C-R-U-D OPERATIONS (???) *//
        // Create an Artist instance/object
  
        // Read out an Artist object

        // Update an Artist object's parameters
    
        // Delete an Artist object

// -----------------------------------------------------------------------------//
    // Checks if there are enough parameters for operation. Note: 0 is placeholder
    private static boolean hasArgs(String[] a, int n) {
        if (a.length < n) {
            System.out.println("Error: this action requires " + n + " argument(s).");
            return false;
        }
        return true;
    }

    static void run(String action, Scanner scanner) {
        switch (action) {
            case "1" -> {
                System.out.println("Artist created.");
            }
            case "2" -> {
                System.out.println("Artist read.");
            }
            case "3" -> {
                System.out.println("Artist updated.");
            }
            case "4" -> {
                System.out.println("Artist deleted.");
            }
            default -> System.out.println("Unknown action. Allowed: 1, 2, 3, 4");
        }
    }
}