import java.sql.*;
public class DeleteBlockedCheckouts {
  public static void main(String[] args) throws Exception {
    long[] ids = {3L, 4L, 10L, 16L};
    String url = "jdbc:postgresql://localhost:5432/Bricolirent";
    try (Connection c = DriverManager.getConnection(url, "postgres", "mba130404")) {
      c.setAutoCommit(false);
      try (PreparedStatement select = c.prepareStatement(
              "select id, tool_id, quantity, status from reservations where id = ? for update");
           PreparedStatement updateTool = c.prepareStatement(
              "update tools set available_quantity = least(total_quantity, coalesce(available_quantity, 0) + ?) where id = ?");
           PreparedStatement deleteReservation = c.prepareStatement(
              "delete from reservations where id = ?")) {
        for (long id : ids) {
          select.setLong(1, id);
          try (ResultSet rs = select.executeQuery()) {
            if (!rs.next()) {
              System.out.println("Reservation introuvable: " + id);
              continue;
            }
            long toolId = rs.getLong("tool_id");
            int quantity = rs.getInt("quantity");
            String status = rs.getString("status");
            if (!"CHECKED_OUT".equals(status)) {
              System.out.println("Reservation ignoree (statut=" + status + "): " + id);
              continue;
            }
            updateTool.setInt(1, quantity);
            updateTool.setLong(2, toolId);
            updateTool.executeUpdate();

            deleteReservation.setLong(1, id);
            deleteReservation.executeUpdate();
            System.out.println("Supprimee: reservation=" + id + ", outil=" + toolId + ", quantite restauree=" + quantity);
          }
        }
      }
      c.commit();
    }
  }
}
