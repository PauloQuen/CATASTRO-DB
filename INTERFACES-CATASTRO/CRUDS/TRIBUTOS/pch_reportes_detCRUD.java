import java.sql.*;
import java.util.Scanner;

public class pch_reportes_detCRUD {

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
            System.out.println("\n===== CRUD HISTORIAL DE REPORTES - DETALLE (PCH_REPORTES_DET) =====");
            System.out.println("1. Insertar Detalle Histórico");
            System.out.println("2. Listar Todos los Detalles (Con INNER JOIN)");
            System.out.println("3. Buscar Detalle por Código");
            System.out.println("4. Actualizar Glosa de Concepto");
            System.out.println("5. Eliminar Detalle (Físico)");
            System.out.println("6. Salir");
            System.out.print("Opción: ");
            opcion = sc.nextInt();

            switch (opcion) {
                case 1: insertarHistorialDet(); break;
                case 2: listarHistorialDet(); break;
                case 3: buscarHistorialDet(); break;
                case 4: actualizarHistorialDet(); break;
                case 5: eliminarHistorialDet(); break;
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

    public static void insertarHistorialDet() {
        Connection conn = conectar();
        try {
            System.out.print("Código Detalle Histórico (HRepDetCod - Int): ");
            int hRepDetCod = sc.nextInt();

            System.out.print("Código Cabecera FK (FKHRepDetCab - Int): ");
            int fkhRepDetCab = sc.nextInt();
            
            // VALIDACIÓN: Comprueba que exista la cabecera del reporte consolidado
            if (!existeIdEnTabla(conn, "pch_reportes_cab", "HRepCabCod", fkhRepDetCab)) {
                System.out.println("ERROR: La Cabecera Histórica (" + fkhRepDetCab + ") no existe. Operación cancelada.");
                conn.close();
                return;
            }

            System.out.print("Número de Cuota o Mes (HRepDetMes - Int [1-12]): ");
            int hRepDetMes = sc.nextInt();
            sc.nextLine(); // Limpiar buffer

            System.out.print("Concepto Tributario Liquidado (Ejm: Arbitrio Parques/Seguridad): ");
            String concepto = sc.nextLine();

            System.out.print("Monto Parcial Emitido S/: ");
            double monEmi = sc.nextDouble();

            System.out.print("Monto Parcial Recaudado S/: ");
            double monRec = sc.nextDouble();
            sc.nextLine(); // Limpiar buffer

            String sql = "INSERT INTO pch_reportes_det " +
                         "(HRepDetCod, FKHRepDetCab, HRepDetMes, HRepDetCon, HRepDetMonEmi, HRepDetMonRec, HRepDetEstReg) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, hRepDetCod);
            stmt.setInt(2, fkhRepDetCab);
            stmt.setInt(3, hRepDetMes);
            stmt.setString(4, concepto);
            stmt.setDouble(5, monEmi);
            stmt.setDouble(6, monRec);
            stmt.setString(7, "1");

            stmt.executeUpdate();
            System.out.println("Detalle histórico analítico guardado con éxito.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al insertar: " + e.getMessage());
        }
    }

    public static void listarHistorialDet() {
        Connection conn = conectar();
        try {
            // INNER JOIN para conectar el desglose con los metadatos de su cabecera correspondiente
            String sql = "SELECT d.HRepDetCod, d.FKHRepDetCab, c.HRepCabAno, d.HRepDetMes, d.HRepDetCon, d.HRepDetMonEmi, d.HRepDetMonRec " +
                         "FROM pch_reportes_det d " +
                         "INNER JOIN pch_reportes_cab c ON d.FKHRepDetCab = c.HRepCabCod " +
                         "WHERE d.HRepDetEstReg = '1' ORDER BY d.FKHRepDetCab ASC, d.HRepDetMes ASC";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- DESGLOSE ANALÍTICO HISTÓRICO DE AUDITORÍA ---");
            while (rs.next()) {
                System.out.println(
                    "Det ID: " + rs.getInt("HRepDetCod") + " | " +
                    "Cab ID: " + rs.getInt("FKHRepDetCab") + " (" + rs.getInt("HRepCabAno") + ") | " +
                    "Cuota/Mes: " + rs.getInt("HRepDetMes") + " | " +
                    "Concepto: " + rs.getString("HRepDetCon") + " | " +
                    "Emitido: S/ " + rs.getDouble("HRepDetMonEmi") + " | " +
                    "Recaudado: S/ " + rs.getDouble("HRepDetMonRec")
                );
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
    }

    public static void buscarHistorialDet() {
        Connection conn = conectar();
        try {
            String sql = "SELECT d.*, c.HRepCabAno FROM pch_reportes_det d " +
                         "INNER JOIN pch_reportes_cab c ON d.FKHRepDetCab = c.HRepCabCod " +
                         "WHERE d.HRepDetCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código de Detalle Histórico a consultar: ");
            int hRepDetCod = sc.nextInt();

            stmt.setInt(1, hRepDetCod);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("\n--- ELEMENTO HISTÓRICO CONGELADO ---");
                System.out.println("Código Detalle     : " + rs.getInt("HRepDetCod"));
                System.out.println("Código Cabecera (FK): " + rs.getInt("FKHRepDetCab") + " (Año Fiscal: " + rs.getInt("HRepCabAno") + ")");
                System.out.println("Mes / Periodo      : " + rs.getInt("HRepDetMes"));
                System.out.println("Concepto Grabado   : " + rs.getString("HRepDetCon"));
                System.out.println("-------------------------------------------------------");
                System.out.println("Monto Emitido      : S/ " + rs.getDouble("HRepDetMonEmi"));
                System.out.println("Monto Recaudado    : S/ " + rs.getDouble("HRepDetMonRec"));
                System.out.println("Estado Registro    : " + rs.getString("HRepDetEstReg"));
            } else {
                System.out.println("Detalle histórico no encontrado.");
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al buscar: " + e.getMessage());
        }
    }

    public static void actualizarHistorialDet() {
        Connection conn = conectar();
        try {
            // Los montos auditados y consolidados de periodos pasados no se alteran; solo se corrige la descripción del concepto si aplica.
            String sql = "UPDATE pch_reportes_det SET HRepDetCon = ? WHERE HRepDetCod = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese el Código del Detalle Histórico a modificar: ");
            int hRepDetCod = sc.nextInt();
            sc.nextLine(); // Limpiar buffer

            System.out.print("Ingrese la corrección nominal del Concepto Tributario: ");
            String nuevoConcepto = sc.nextLine();

            stmt.setString(1, nuevoConcepto);
            stmt.setInt(2, hRepDetCod);

            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Concepto histórico corregido con éxito.");
            } else {
                System.out.println("No se encontró ningún registro con ese código.");
            }

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al actualizar: " + e.getMessage());
        }
    }

    public static void eliminarHistorialDet() {
        Connection conn = conectar();
        try {
            String sql = "DELETE FROM pch_reportes_det WHERE HRepDetCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese el Código del Detalle Histórico a eliminar de las auditorías: ");
            int hRepDetCod = sc.nextInt();

            stmt.setInt(1, hRepDetCod);

            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Registro de detalle purgado físicamente del sistema.");
            } else {
                System.out.println("No se localizó el registro indicado.");
            }

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al eliminar: " + e.getMessage());
        }
    }
}
