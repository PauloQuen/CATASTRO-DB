import java.sql.*;
import java.util.Scanner;

public class pcm_reportes_preCRUD {

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
            System.out.println("\n===== CRUD PRESUPUESTO MUNICIPAL ANUAL (PCM_REPORTES_PRE) =====");
            System.out.println("1. Insertar Plan Presupuestario");
            System.out.println("2. Listar Presupuestos (Con INNER JOIN)");
            System.out.println("3. Buscar por Código de Reporte");
            System.out.println("4. Actualizar Valores del Presupuesto");
            System.out.println("5. Eliminar Presupuesto");
            System.out.println("6. Salir");
            System.out.print("Opción: ");
            opcion = sc.nextInt();

            switch (opcion) {
                case 1: insertarPresupuesto(); break;
                case 2: listarPresupuestos(); break;
                case 3: buscarPresupuesto(); break;
                case 4: actualizarPresupuesto(); break;
                case 5: eliminarPresupuesto(); break;
            }
        } while (opcion != 6);
    }

    // ====================================================================
    // METODOS AUXILIARES: Validaciones lógicas de integridad referencial
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

    public static void insertarPresupuesto() {
        Connection conn = conectar();
        try {
            System.out.print("Código del Reporte Presupuesto (RepCod - Int): ");
            int repCod = sc.nextInt();

            System.out.print("Código Municipalidad FK (FKRepMun - Int): ");
            int fkRepMun = sc.nextInt();
            // VALIDACIÓN: Comprueba que exista la Municipalidad emisora
            if (!existeIdEnTabla(conn, "c1m_municipalidad", "MunCod", fkRepMun)) {
                System.out.println("ERROR: La Municipalidad (" + fkRepMun + ") no existe. Operación cancelada.");
                conn.close();
                return;
            }

            System.out.print("Código Zona FK (FKRepZon - Int): ");
            int fkRepZon = sc.nextInt();
            // VALIDACIÓN: Comprueba que exista la Zona de jurisdicción territorial
            if (!existeIdEnTabla(conn, "c2m_zona", "ZonCod", fkRepZon)) {
                System.out.println("ERROR: La Zona (" + fkRepZon + ") no existe. Operación cancelada.");
                conn.close();
                return;
            }

            System.out.print("Año del Presupuesto (RepAno - Int Ejm: 2026): ");
            int repAno = sc.nextInt();
            sc.nextLine(); // Limpiar buffer

            System.out.print("Proyección de Ingresos Tributarios S/: ");
            double proyIng = sc.nextDouble();

            System.out.print("Total Efectivamente Recaudado S/: ");
            double recTotal = sc.nextDouble();

            System.out.print("Total Gastos Apropiados en Presupuesto S/: ");
            double gasTotal = sc.nextDouble();
            sc.nextLine(); // Limpiar buffer

            System.out.print("Fecha de Aprobación (AAAA-MM-DD): ");
            String fechaAprob = sc.nextLine();

            String sql = "INSERT INTO pcm_reportes_pre " +
                         "(RepCod, FKRepMun, FKRepZon, RepAno, RepProyIng, RepRecTotal, RepGasTotal, RepFecAprob, RepEstReg) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, repCod);
            stmt.setInt(2, fkRepMun);
            stmt.setInt(3, fkRepZon);
            stmt.setInt(4, repAno);
            stmt.setDouble(5, proyIng);
            stmt.setDouble(6, recTotal);
            stmt.setDouble(7, gasTotal);
            stmt.setString(8, fechaAprob);
            stmt.setString(9, "1");

            stmt.executeUpdate();
            System.out.println("Plan presupuestario registrado con éxito controlando su Integridad Referencial.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al insertar: " + e.getMessage());
        }
    }

    public static void listarPresupuestos() {
        Connection conn = conectar();
        try {
            // INNER JOIN multi-tabla para resolver descripciones legibles de la comuna y zona afectada
            String sql = "SELECT p.RepCod, m.MunNom, z.ZonNom, p.RepAno, p.RepProyIng, p.RepRecTotal, p.RepGasTotal, p.RepFecAprob " +
                         "FROM pcm_reportes_pre p " +
                         "INNER JOIN c1m_municipalidad m ON p.FKRepMun = m.MunCod " +
                         "INNER JOIN c2m_zona z ON p.FKRepZon = z.ZonCod " +
                         "WHERE p.RepEstReg = '1'";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- RECAUDACIÓN VS PRESUPUESTO ANUAL CONSOLIDADO ---");
            while (rs.next()) {
                double balance = rs.getDouble("RepRecTotal") - rs.getDouble("RepProyIng");
                System.out.println(
                    "Cod Rep: " + rs.getInt("RepCod") + " | " +
                    "Muni: " + rs.getString("MunNom") + " | " +
                    "Zona: " + rs.getString("ZonNom") + " | " +
                    "Año: " + rs.getInt("RepAno") + " | " +
                    "Proyectado: S/ " + rs.getDouble("RepProyIng") + " | " +
                    "Recaudado: S/ " + rs.getDouble("RepRecTotal") + " | " +
                    "Balance: S/ " + balance + " | " +
                    "Fec. Aprob: " + rs.getString("RepFecAprob")
                );
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
    }

    public static void buscarPresupuesto() {
        Connection conn = conectar();
        try {
            String sql = "SELECT p.*, m.MunNom, z.ZonNom FROM pcm_reportes_pre p " +
                         "INNER JOIN c1m_municipalidad m ON p.FKRepMun = m.MunCod " +
                         "INNER JOIN c2m_zona z ON p.FKRepZon = z.ZonCod " +
                         "WHERE p.RepCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código de Reporte Presupuestal a buscar: ");
            int repCod = sc.nextInt();

            stmt.setInt(1, repCod);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("\n--- EVALUACIÓN FINANCIERA DEL PRESUPUESTO ---");
                System.out.println("Código Reporte       : " + rs.getInt("RepCod"));
                System.out.println("Municipalidad Emisora: " + rs.getString("MunNom") + " (ID: " + rs.getInt("FKRepMun") + ")");
                System.out.println("Zona de Control      : " + rs.getString("ZonNom") + " (ID: " + rs.getInt("FKRepZon") + ")");
                System.out.println("Año Fiscal Evaluado  : " + rs.getInt("RepAno"));
                System.out.println("-------------------------------------------------------");
                System.out.println("Ingresos Proyectados : S/ " + rs.getDouble("RepProyIng"));
                System.out.println("Ingresos Recaudados  : S/ " + rs.getDouble("RepRecTotal"));
                System.out.println("Gastos Asignados     : S/ " + rs.getDouble("RepGasTotal"));
                System.out.println("Fecha de Aprobación  : " + rs.getString("RepFecAprob"));
                System.out.println("Estado Registro      : " + rs.getString("RepEstReg"));
            } else {
                System.out.println("Reporte de presupuesto no encontrado.");
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al buscar: " + e.getMessage());
        }
    }

    public static void actualizarPresupuesto() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE pcm_reportes_pre " +
                         "SET RepProyIng = ?, RepRecTotal = ?, RepGasTotal = ? " +
                         "WHERE RepCod = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese el Código del Presupuesto a modificar: ");
            int repCod = sc.nextInt();

            System.out.print("Nueva Proyección de Ingresos S/: ");
            double proy = sc.nextDouble();

            System.out.print("Nueva Recaudación Acumulada Real S/: ");
            double rec = sc.nextDouble();

            System.out.print("Nuevo Total de Gastos Ejecutados S/: ");
            double gas = sc.nextDouble();

            stmt.setDouble(1, proy);
            stmt.setDouble(2, rec);
            stmt.setDouble(3, gas);
            stmt.setInt(4, repCod);

            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Montos presupuestales actualizados correctamente.");
            } else {
                System.out.println("No se encontró ningún reporte con ese código.");
            }

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al actualizar: " + e.getMessage());
        }
    }

    public static void eliminarPresupuesto() {
        Connection conn = conectar();
        try {
            String sql = "DELETE FROM pcm_reportes_pre WHERE RepCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese el Código del Presupuesto a eliminar físicamente: ");
            int repCod = sc.nextInt();

            stmt.setInt(1, repCod);

            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Presupuesto purgado de la base de datos.");
            } else {
                System.out.println("No se encontró el registro presupuestal.");
            }

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al eliminar: " + e.getMessage());
        }
    }
}
