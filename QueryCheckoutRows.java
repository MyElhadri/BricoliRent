import java.sql.*;
public class QueryCheckoutRows {
  public static void main(String[] args) throws Exception {
    String url = "jdbc:postgresql://localhost:5432/Bricolirent";
    try (Connection c = DriverManager.getConnection(url, "postgres", "mba130404");
         PreparedStatement ps = c.prepareStatement(
           "select r.id, u.full_name, t.name as tool, r.quantity, r.start_date, r.end_date, r.checked_out_at, r.status " +
           "from reservations r " +
           "join clients c on c.user_id = r.client_id " +
           "join users u on u.id = c.user_id " +
           "join tools t on t.id = r.tool_id " +
           "where r.status = 'CHECKED_OUT' order by r.checked_out_at asc");
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        System.out.println(rs.getLong(1) + " | " + rs.getString(2) + " | " + rs.getString(3) + " | q=" + rs.getInt(4)
          + " | " + rs.getDate(5) + " -> " + rs.getDate(6) + " | out=" + rs.getTimestamp(7) + " | " + rs.getString(8));
      }
    }
  }
}
