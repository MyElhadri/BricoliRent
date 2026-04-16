import java.sql.*;
public class InspectPaymentsSchema {
  public static void main(String[] args) throws Exception {
    String url = "jdbc:postgresql://localhost:5432/Bricolirent";
    try (Connection c = DriverManager.getConnection(url, "postgres", "mba130404");
         PreparedStatement ps = c.prepareStatement(
           "select column_name, data_type, udt_name, is_nullable, column_default from information_schema.columns where table_name = 'payments' order by ordinal_position");
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        System.out.println(rs.getString(1)+" | "+rs.getString(2)+" | "+rs.getString(3)+" | nullable="+rs.getString(4)+" | default="+rs.getString(5));
      }
    }
  }
}
