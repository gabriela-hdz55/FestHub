package org.festhub;
import java.util.Scanner;
import java.util.UUID;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


class Artist {

    // Variable declarations
    private String artistID;
    private String artistName;
    private String bookingContact;
    private String[] eventIDs; // exists in other classes; ensure sync

// ------------------------------------------------------------------- //


/*_____________________ CONSTRUCTORS for new Artist instance/object________________________ */

        // Both 'artistID' AND 'eventIDs' are NOT provided
    public Artist(String artistName, String bookingContact){
        setArtistName(artistName);
        setBookingContact(bookingContact);
        this.artistID = UUID.randomUUID().toString();
        this.eventIDs = new String[0];
    }

        //  Only 'eventIDs' is NOT provided 
    public Artist(String artistName, String bookingContact, String artistID){
        setArtistName(artistName);
        setBookingContact(bookingContact);
        this.artistID = artistID;
        this.eventIDs = new String[0];
    }


// ----------------------------------------------------------------------------//


/*_________In-class METHODS_____________*/

    //*_______________ GETTERS + SETTERS __________________ *//

    public String getArtistID() {
        return artistID;
    }

    public void setArtistID(String artistID) {
        this.artistID = artistID;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getBookingContact() {
        return bookingContact;
    }

    public String[] getEventIDs() {
        return eventIDs;
    }

    public void setEventIDs(String[] eventIDs) {
        this.eventIDs = eventIDs;
    }


    //*_______________ INPUT VALIDATORS ___________________*//

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
        this.bookingContact = bookingContact;
    }   

        // Input validation for valid Concert Event UUIDs - ???
    public void setEventIDs(){
            // TBD
    }


    //*________________ C-R-U-D OPERATIONS __________________*//
    
    // Create an Artist instance/object
    public String createArtist(String artistName, String bookingContact, String artistID, Connection connection){

        Artist newArtist;

        if (artistID == null){
            newArtist = new Artist(artistName, bookingContact);
        } else {
            newArtist = new Artist(artistName, bookingContact, artistID);
        }

    // Logic to save new Artist object to database
        String sql = "INSERT INTO artist (id, name, booking_contact) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newArtist.getArtistID());
            pstmt.setString(2, newArtist.getArtistName());
            pstmt.setString(3, newArtist.getBookingContact());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Cannot save artist to database", e);
        }

    // returns Artist's UUID
        return newArtist.getArtistID();
    }

        // Read out an Artist object
    public Artist readArtist(String artistID, Connection connection){

        String sql = "SELECT id, name, booking_contact FROM artist WHERE id = ?";
        Artist fetchedArtist = null;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)){
            pstmt.setString(1, artistID);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String dbId = rs.getString("id");
                String dbName = rs.getString("name");
                String dbContact = rs.getString("booking_contact");

                fetchedArtist = new Artist(dbName, dbContact, dbId);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read out artist from the database", e);
        }

        return fetchedArtist;
    }

        // Update an Artist object's parameters
    public void updateArtist(String artistID, String newName, String newContact, Connection connection){
        
        Artist targetArtist = readArtist(artistID, connection);

        if (targetArtist == null){
            throw new IllegalArgumentException("Artist ID does not exist.");
        }

        // defaulting updated variables to the old data
        String updatedName = targetArtist.getArtistName();
        String updatedContact = targetArtist.getBookingContact();
        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"; // regex expression for expected email address format

        if (newName != null && !newName.isBlank()) {
            updatedName = newName;
        }
        if (newContact != null && !newContact.isBlank()) {
            if (bookingContact == null || !bookingContact.matches(emailRegex)){
                throw new IllegalArgumentException("Booking contact must be a valid email address."); 
            }
            updatedContact = newContact;
        }

        String sql = "UPDATE artist SET name = ?, booking_contact = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, updatedName);
            pstmt.setString(2, updatedContact);
            pstmt.setString(3, artistID);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update artist's entry in database", e);
        }
    }

        // Delete an Artist object
    public String deleteArtist(String artistID, Connection connection){
        
        String sql = "DELETE FROM artist WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, artistID);

            if (pstmt.executeUpdate() == 1){
                return "Deletion successful.";
            } else {
                return "Deletion unsuccessful; Artist ID does not exist in database.";
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete artist entry from database", e);
        }
    }

    // Manage Events
    public void manageEvents(){
        
    }


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