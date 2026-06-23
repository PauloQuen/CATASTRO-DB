import java.sql.*;
import java.util.Scanner;

public class h8m_prop_viviendaCRUD {

    static final String URL = "jdbc:mysql://localhost:3306/catastro_db";
    static final String USER = "root";
    static final String PASSWORD = "";

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        menuPrincipal();
    }

    public static Connection conectar() {
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            System.out.println("Error conexión: " + e.getMessage());
        }
        return conn;
    }

    public static void menuPrincipal() {
        int opcion;
        do {
            System.out.println("\n===== CRUD RELACIÓN PROPIETARIO - VIVIENDA (MULTIPROPIEDAD) =====");
            System.out.println("1. Asignar Propiedad de Vivienda");
            System.out.println("2. Listar Distribución de Propiedades");
            System.out.println("3. Buscar Viviendas por Propietario (o viceversa)");
            System.out.println("4. Actualizar Porcentaje de Co-propiedad");
            System.out.println("5. Eliminar Asignación (Físico)");
            System.out.println("6. Salir");
            System.out.print("Opción: ");
            opcion = sc.nextInt();
            sc.nextLine(); // Limpieza de buffer

            switch (opcion) {
                case 1: insertarPropVivienda(); break;
                case 2: listarPropViviendas(); break;
                case 3: buscarPropVivienda(); break;
                case 4: actualizarPorcentaje(); break;
                case 5: eliminarPropVivienda(); break;
            }
        } while (opcion != 6);
    }

    // EXPLICACIÓN: Validador para llaves foráneas individuales de propietarios y viviendas
    private static boolean existeId(Connection conn, String tabla, String campoId, Object id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tabla + " WHERE " + campoId + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (id instanceof Integer) stmt.setInt(1, (Integer) id);
            else stmt.setString(1, (String) id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    // EXPLICACIÓN: Validador para salvaguardar la clave primaria compuesta (PViProCod, PViVivCod)
    private static boolean existeRelacion(Connection conn, int proCod, String vivCod) throws SQLException {
        String sql = "SELECT COUNT(*) FROM h8m_prop_vivienda WHERE PViProCod = ? AND PViVivCod = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, proCod);
            stmt.setString(2, vivCod);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public static void insertarPropVivienda() {
        Connection conn = conectar();
        try {
            System.out.print("Código del Propietario Contribuyente (PViProCod): ");
            int proCod = sc.nextInt();
            sc.nextLine(); // Limpieza buffer

            if (!existeId(conn, "h8m_propietario", "ProCod", proCod)) {
                System.out.println("Error: El código de propietario no existe en H8M_PROPIETARIO.");
                return;
            }

            System.out.print("Código Catastral de la Vivienda (PViVivCod): ");
            String vivCod = sc.nextLine().trim();

            if (!existeId(conn, "c3m_vivienda", "VivCod", vivCod)) {
                System.out.println("Error: El código de vivienda no existe en C3M_VIVIENDA.");
                return;
            }

            if (existeRelacion(conn, proCod, vivCod)) {
                System.out.println("Error: Este propietario ya tiene asignada una fracción o la totalidad de esta vivienda.");
                return;
            }

            System.out.print("Porcentaje de Posesión (PViPorPro - Máx 100.00): ");
            double porcentaje = sc.nextDouble();

            if (porcentaje <= 0 || porcentaje > 100) {
                System.out.println("Error: El porcentaje debe situarse en el rango de > 0 y <= 100.");
                return;
            }

            String sql = "INSERT INTO h8m_prop_vivienda (PViProCod, PViVivCod, PViPorPro, PViEstReg) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, proCod);
            stmt.setString(2, vivCod);
            stmt.setDouble(3, porcentaje);
            stmt.setString(4, "1");

            stmt.executeUpdate();
            System.out.println("Relación dominial guardada exitosamente.");
            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al insertar: " + e.getMessage());
        }
    }

    public static void listarPropViviendas() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Múltiples INNER JOINs conectando la tabla cruzada con el padrón de ciudadanos y direcciones físicas
            String sql = "SELECT pv.PViProCod, p.PerNom, p.PerApePat, pv.PViVivCod, d.DirViaNom, d.DirNum, pv.PViPorPro " +
                         "FROM h8m_prop_vivienda pv " +
                         "INNER JOIN h8m_propietario pr ON pv.PViProCod = pr.ProCod " +
                         "INNER JOIN h6m_persona p ON pr.ProPer = p.PerDNI " +
                         "INNER JOIN c3m_vivienda v ON pv.PViVivCod = v.VivCod " +
                         "INNER JOIN c3m_direccion d ON v.VivDir = d.DirCod " +
                         "WHERE pv.PViEstReg = '1'";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- COMPROBACIONES DE CO-PROPIEDAD URBANA ---");
            while (rs.next()) {
                System.out.println(
                    "ID Prop: " + rs.getInt("PViProCod") + " -> " + rs.getString("PerNom") + " " + rs.getString("PerApePat") + " | " +
                    "Predio Cod: " + rs.getString("PViVivCod") + " (" + rs.getString("DirViaNom") + " #" + rs.getInt("DirNum") + ") | " +
                    "Participación: " + rs.getDouble("PViPorPro") + "%"
                );
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
    }

    public static void buscarPropVivienda() {
        Connection conn = conectar();
        try {
            System.out.println("Buscar por: 1) Propietario  2) Vivienda");
            int modo = sc.nextInt();
            sc.nextLine(); // Buffer

            if (modo == 1) {
                System.out.print("Ingrese Código de Propietario: ");
                int proCod = sc.nextInt();
                
                String sql = "SELECT pv.PViVivCod, d.DirViaNom, d.DirNum, pv.PViPorPro " +
                             "FROM h8m_prop_vivienda pv " +
                             "INNER JOIN c3m_vivienda v ON pv.PViVivCod = v.VivCod " +
                             "INNER JOIN c3m_direccion d ON v.VivDir = d.DirCod " +
                             "WHERE pv.PViProCod = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, proCod);
                ResultSet rs = stmt.executeQuery();
                
                System.out.println("\n--- INMUEBLES PERTENECIENTES AL PROPIETARIO " + proCod + " ---");
                while (rs.next()) {
                    System.out.println("- " + rs.getString("PViVivCod") + " en " + rs.getString("DirViaNom") + " #" + rs.getInt("DirNum") + " | Fracción: " + rs.getDouble("PViPorPro") + "%");
                }
                rs.close(); stmt.close();
            } else {
                System.out.print("Ingrese Código Catastral de la Vivienda: ");
                String vivCod = sc.nextLine().trim();
                
                String sql = "SELECT pv.PViProCod, p.PerNom, p.PerApePat, pv.PViPorPro " +
                             "FROM h8m_prop_vivienda pv " +
                             "INNER JOIN h8m_propietario pr ON pv.PViProCod = pr.ProCod " +
                             "INNER JOIN h6m_persona p ON pr.ProPer = p.PerDNI " +
                             "WHERE pv.PViVivCod = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, vivCod);
                ResultSet rs = stmt.executeQuery();
                
                System.out.println("\n--- CONDÓMINOS / DUEÑOS ASOCIADOS AL PREDIO " + vivCod + " ---");
                while (rs.next()) {
                    System.out.println("- Prop ID: " + rs.getInt("PViProCod") + " -> " + rs.getString("PerNom") + " " + rs.getString("PerApePat") + " | Cuota: " + rs.getDouble("PViPorPro") + "%");
                }
                rs.close(); stmt.close();
            }
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al buscar: " + e.getMessage());
        }
    }

    public static void actualizarPorcentaje() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE h8m_prop_vivienda SET PViPorPro = ? WHERE PViProCod = ? AND PViVivCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código Propietario: ");
            int proCod = sc.nextInt();
            sc.nextLine(); // Buffer
            System.out.print("Código Vivienda: ");
            String vivCod = sc.nextLine().trim();

            if (!existeRelacion(conn, proCod, vivCod)) {
                System.out.println("No existe un registro de propiedad para ese par de claves.");
                return;
            }

            System.out.print("Nuevo Porcentaje de Posesión: ");
            double nuevoPorcentaje = sc.nextDouble();

            if (nuevoPorcentaje <= 0 || nuevoPorcentaje > 100) {
                System.out.println("Porcentaje fuera de rango legítimo.");
                return;
            }

            stmt.setDouble(1, nuevoPorcentaje);
            stmt.setInt(2, proCod);
            stmt.setString(3, vivCod);

            stmt.executeUpdate();
            System.out.println("Derechos porcentuales actualizados con éxito.");
            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al actualizar: " + e.getMessage());
        }
    }

    public static void eliminarPropVivienda() {
        Connection conn = conectar();
        try {
            String sql = "DELETE FROM h8m_prop_vivienda WHERE PViProCod = ? AND PViVivCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código Propietario a desvincular físicamente: ");
            int proCod = sc.nextInt();
            sc.nextLine(); // Buffer
            System.out.print("Código Vivienda a desvincular físicamente: ");
            String vivCod = sc.nextLine().trim();

            stmt.setInt(1, proCod);
            stmt.setString(2, vivCod);

            int filas = stmt.executeUpdate();
            if (filas > 0) {
                System.out.println("Relación de propiedad removida físicamente de la base catastral.");
            } else {
                System.out.println("No se encontró la combinación de llaves especificada.");
            }
            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al eliminar: " + e.getMessage());
        }
    }
}
