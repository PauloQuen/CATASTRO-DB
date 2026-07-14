import java.sql.*;
import java.util.Scanner;

public class pch_reportes_cabCRUD {

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
            System.out.println("Error de conexión: " + e.getMessage());
        }
        return conn;
    }

    public static void menuPrincipal() {
        int opcion;
        do {
            System.out.println("\n===== CRUD HISTORIAL DE REPORTES - CABECERA (PCH_REPORTES_CAB) =====");
            System.out.println("1. Archivar Cabecera Histórica");
            System.out.println("2. Listar Reportes Históricos (Con INNER JOIN)");
            System.out.println("3. Buscar Reporte por Código");
            System.out.println("4. Modificar Observaciones del Histórico");
            System.out.println("5. Eliminar Histórico (Físico)");
            System.out.println("6. Salir");
            System.out.print("Opción: ");
            opcion = sc.nextInt();

            switch (opcion) {
                case 1: insertarHistorialCab(); break;
                case 2: listarHistorialCab(); break;
                case 3: buscarHistorialCab(); break;
                case 4: actualizarHistorialCab(); break;
                case 5: eliminarHistorialCab(); break;
            }
        } while (opcion != 6);
    }

    // ====================================================================
    // METODO AUXILIAR: Validación lógica de integridad referencial
    // ====================================================================
    private static boolean existeIdEnTabla(Connection conn, String tabla, String campoId, int id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tabla + " WHERE " + campoId + " = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public static void insertarHistorialCab() {
        Connection conn = conectar();
        try {
            System.out.print("Código Historial Cabecera (HRepCabCod - Int): ");
            int hRepCabCod = sc.nextInt();

            System.out.print("Código Municipalidad FK (FKHRepMun - Int): ");
            int fkhRepMun = sc.nextInt();
            
            // VALIDACIÓN: Comprueba que exista la Municipalidad que archiva el cierre
            if (!existeIdEnTabla(conn, "c1m_municipalidad", "MunCod", fkhRepMun)) {
                System.out.println("ERROR: La Municipalidad (" + fkhRepMun + ") no existe. Operación cancelada.");
                conn.close();
                return;
            }

            System.out.print("Año Fiscal de Cierre (HRepCabAno - Int Ejm: 2026): ");
            int hRepCabAno = sc.nextInt();
            sc.nextLine(); // Limpiar buffer

            System.out.print("Monto Total Calculado al Archivar S/: ");
            double monCal = sc.nextDouble();

            System.out.print("Monto Total Pagado al Archivar S/: ");
            double monPag = sc.nextDouble();
            sc.nextLine(); // Limpiar buffer

            System.out.print("Notas/Observaciones del Cierre de Auditoría: ");
            String obs = sc.nextLine();

            String sql = "INSERT INTO pch_reportes_cab " +
                         "(HRepCabCod, FKHRepMun, HRepCabAno, HRepCabMonCal, HRepCabMonPag, HRepCabObs, HRepCabEstReg) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, hRepCabCod);
            stmt.setInt(2, fkhRepMun);
            stmt.setInt(3, hRepCabAno);
            stmt.setDouble(4, monCal);
            stmt.setDouble(5, monPag);
            stmt.setString(6, obs);
            stmt.setString(7, "1");

            stmt.executeUpdate();
            System.out.println("Cabecera de reporte histórico guardada con éxito controlando su Integridad.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al insertar: " + e.getMessage());
        }
    }

    public static void listarHistorialCab() {
        Connection conn = conectar();
        try {
            // INNER JOIN para traer el nombre oficial del municipio emisor
            String sql = "SELECT h.HRepCabCod, m.MunNom, h.HRepCabAno, h.HRepCabMonCal, h.HRepCabMonPag, h.HRepCabObs " +
                         "FROM pch_reportes_cab h " +
                         "INNER JOIN c1m_municipalidad m ON h.FKHRepMun = m.MunCod " +
                         "WHERE h.HRepCabEstReg = '1' ORDER BY h.HRepCabAno DESC";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- EXPEDIENTES HISTÓRICOS DE AUDITORÍA MUNICIPAL ---");
            while (rs.next()) {
                System.out.println(
                    "ID Cierre: " + rs.getInt("HRepCabCod") + " | " +
                    "Muni: " + rs.getString("MunNom") + " | " +
                    "Año: " + rs.getInt("HRepCabAno") + " | " +
                    "Total Calc: S/ " + rs.getDouble("HRepCabMonCal") + " | " +
                    "Total Pag: S/ " + rs.getDouble("HRepCabMonPag") + " | " +
                    "Obs: " + rs.getString("HRepCabObs")
                );
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
    }

    public static void buscarHistorialCab() {
        Connection conn = conectar();
        try {
            String sql = "SELECT h.*, m.MunNom FROM pch_reportes_cab h " +
                         "INNER JOIN c1m_municipalidad m ON h.FKHRepMun = m.MunCod " +
                         "WHERE h.HRepCabCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código de Cabecera Histórica a consultar: ");
            int hRepCabCod = sc.nextInt();

            stmt.setInt(1, hRepCabCod);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("\n--- REGISTRO HISTÓRICO PERMANENTE ---");
                System.out.println("Código Histórico     : " + rs.getInt("HRepCabCod"));
                System.out.println("Municipalidad Emisora: " + rs.getString("MunNom") + " (ID: " + rs.getInt("FKHRepMun") + ")");
                System.out.println("Año Fiscal Concluido : " + rs.getInt("HRepCabAno"));
                System.out.println("-------------------------------------------------------");
                System.out.println("Monto Inicial Fijado : S/ " + rs.getDouble("HRepCabMonCal"));
                System.out.println("Monto Total Cobrado  : S/ " + rs.getDouble("HRepCabMonPag"));
                System.out.println("Observaciones        : " + rs.getString("HRepCabObs"));
                System.out.println("Estado Registro      : " + rs.getString("HRepCabEstReg"));
            } else {
                System.out.println("Registro histórico de cabecera no encontrado.");
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al buscar: " + e.getMessage());
        }
    }

    public static void actualizarHistorialCab() {
        Connection conn = conectar();
        try {
            // Nota de auditoría: Los montos financieros históricos son INMUTABLES, solo se permite actualizar las observaciones/metadatos.
            String sql = "UPDATE pch_reportes_cab SET HRepCabObs = ? WHERE HRepCabCod = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese el Código de la Cabecera Histórica a actualizar: ");
            int hRepCabCod = sc.nextInt();
            sc.nextLine(); // Limpiar buffer

            System.out.print("Ingrese las nuevas Glosas/Observaciones de Auditoría: ");
            String nuevasObs = sc.nextLine();

            stmt.setString(1, nuevasObs);
            stmt.setInt(2, hRepCabCod);

            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Glosas de auditoría actualizadas correctamente.");
            } else {
                System.out.println("No se encontró ninguna cabecera histórica con ese código.");
            }

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al actualizar: " + e.getMessage());
        }
    }

    public static void eliminarHistorialCab() {
        Connection conn = conectar();
        try {
            String sql = "DELETE FROM pch_reportes_cab WHERE HRepCabCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese el Código de la Cabecera Histórica a eliminar permanentemente: ");
            int hRepCabCod = sc.nextInt();

            stmt.setInt(1, hRepCabCod);

            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Registro histórico purgado correctamente.");
            } else {
                System.out.println("No se encontró el registro indicado.");
            }

            stmt.close();
            conn.close();
        } catch (Exception e) {
            // EXPLICACIÓN: Bloquea la eliminación si existen desgloses mensuales inmutables asociados a esta cabecera en pch_reportes_det.
            System.out.println("ERROR DE INTEGRIDAD: No se puede eliminar la cabecera porque contiene desgloses mensuales activos.");
        }
    }
}
