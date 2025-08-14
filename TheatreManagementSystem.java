import java.sql.*;
import java.util.Scanner;

public class TheatreManagementSystem {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/theatre_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "your_password"; // Replace with your MySQL password
    private static Connection conn;
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            // Initialize database connection
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            createTables(); // Create necessary tables if they don't exist
            showMenu();
        } catch (SQLException e) {
            System.out.println("Database connection error: " + e.getMessage());
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.out.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    // Create database tables
    private static void createTables() throws SQLException {
        String createMoviesTable = """
            CREATE TABLE IF NOT EXISTS movies (
                movie_id INT AUTO_INCREMENT PRIMARY KEY,
                title VARCHAR(100) NOT NULL,
                show_time VARCHAR(50) NOT NULL,
                total_seats INT NOT NULL,
                available_seats INT NOT NULL
            )
        """;

        String createBookingsTable = """
            CREATE TABLE IF NOT EXISTS bookings (
                booking_id INT AUTO_INCREMENT PRIMARY KEY,
                movie_id INT,
                customer_name VARCHAR(100) NOT NULL,
                seats_booked INT NOT NULL,
                booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (movie_id) REFERENCES movies(movie_id)
            )
        """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createMoviesTable);
            stmt.execute(createBookingsTable);
            System.out.println("Database tables initialized.");
        }
    }

    // Display main menu
    private static void showMenu() {
        while (true) {
            System.out.println("\n=== Theatre Management System ===");
            System.out.println("1. Add Movie");
            System.out.println("2. View All Movies");
            System.out.println("3. Book Tickets");
            System.out.println("4. View Bookings");
            System.out.println("5. Exit");
            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            try {
                switch (choice) {
                    case 1:
                        addMovie();
                        break;
                    case 2:
                        viewMovies();
                        break;
                    case 3:
                        bookTickets();
                        break;
                    case 4:
                        viewBookings();
                        break;
                    case 5:
                        System.out.println("Exiting system...");
                        return;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    // Add a new movie to the database
    private static void addMovie() throws SQLException {
        System.out.print("Enter movie title: ");
        String title = scanner.nextLine();
        System.out.print("Enter show time (e.g., 14:00): ");
        String showTime = scanner.nextLine();
        System.out.print("Enter total seats: ");
        int totalSeats = scanner.nextInt();

        String sql = "INSERT INTO movies (title, show_time, total_seats, available_seats) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, showTime);
            pstmt.setInt(3, totalSeats);
            pstmt.setInt(4, totalSeats);
            pstmt.executeUpdate();
            System.out.println("Movie added successfully!");
        }
    }

    // View all movies
    private static void viewMovies() throws SQLException {
        String sql = "SELECT * FROM movies";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n=== Movie List ===");
            while (rs.next()) {
                System.out.printf("ID: %d, Title: %s, Show Time: %s, Available Seats: %d/%d%n",
                    rs.getInt("movie_id"),
                    rs.getString("title"),
                    rs.getString("show_time"),
                    rs.getInt("available_seats"),
                    rs.getInt("total_seats"));
            }
        }
    }

    // Book tickets for a movie
    private static void bookTickets() throws SQLException {
        viewMovies();
        System.out.print("Enter movie ID to book: ");
        int movieId = scanner.nextInt();
        scanner.nextLine(); // Consume newline
        System.out.print("Enter customer name: ");
        String customerName = scanner.nextLine();
        System.out.print("Enter number of seats to book: ");
        int seats = scanner.nextInt();

        // Check available seats
        String checkSql = "SELECT available_seats FROM movies WHERE movie_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
            pstmt.setInt(1, movieId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int availableSeats = rs.getInt("available_seats");
                if (seats <= availableSeats) {
                    // Book tickets
                    String bookSql = "INSERT INTO bookings (movie_id, customer_name, seats_booked) VALUES (?, ?, ?)";
                    String updateSql = "UPDATE movies SET available_seats = available_seats - ? WHERE movie_id = ?";
                    try (PreparedStatement bookStmt = conn.prepareStatement(bookSql);
                         PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        // Insert booking
                        bookStmt.setInt(1, movieId);
                        bookStmt.setString(2, customerName);
                        bookStmt.setInt(3, seats);
                        bookStmt.executeUpdate();

                        // Update available seats
                        updateStmt.setInt(1, seats);
                        updateStmt.setInt(2, movieId);
                        updateStmt.executeUpdate();

                        System.out.println("Tickets booked successfully!");
                    }
                } else {
                    System.out.println("Not enough seats available!");
                }
            } else {
                System.out.println("Invalid movie ID!");
            }
        }
    }

    // View all bookings
    private static void viewBookings() throws SQLException {
        String sql = """
            SELECT b.booking_id, b.customer_name, b.seats_booked, b.booking_date, m.title
            FROM bookings b
            JOIN movies m ON b.movie_id = m.movie_id
        """;
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n=== Booking List ===");
            while (rs.next()) {
                System.out.printf("Booking ID: %d, Movie: %s, Customer: %s, Seats: %d, Date: %s%n",
                    rs.getInt("booking_id"),
                    rs.getString("title"),
                    rs.getString("customer_name"),
                    rs.getInt("seats_booked"),
                    rs.getString("booking_date"));
            }
        }
    }
}