import java.sql.*;
import java.util.Scanner;

public class pct_reportes_detCRUD {

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
            System.out.println("\n===== CRUD DETALLE DE REPORTES TRANSACCIONALES (PCT_REPORTES_DET) =====");
            System.out.println("1. Agregar Línea de Detalle (Vivienda a Reporte)");
            System.out.println("2. Listar Líneas de Detalle (Con INNER JOINs)");
            System.out.println("3. Buscar Detalle por Código Único");
            System.out.println("4. Actualizar Montos de Fiscalización");
            System.out.println("5. Eliminar Línea de Detalle");
            System.out.println("6. Salir");
            System.out.print("Opcion: ");
            opcion = sc.nextInt();
            sc.nextLine(); // Limpieza buffer de opción

            switch (opcion) {
                case 1: insertarReporteDet(); break;
                case 2: listarReporteDet(); break;
                case 3: buscarReporteDet(); break;
                case 4: actualizarReporteDet(); break;
                case 5: eliminarReporteDet(); break;
            }
        } while (opcion != 6);
    }

    // EXPLICACIÓN: Método genérico para validar la existencia de llaves foráneas en tablas maestras.
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

    public static void insertarReporteDet() {
        Connection conn = conectar();
        try {
            System.out.print("Código de Detalle (RepDetCod - Int): ");
            int repDetCod = sc.nextInt();

            System.out.print("Código de Cabecera Transaccional FK (FKRepDetCab - Int): ");
            int fkRepDetCab = sc.nextInt();
            // EXPLICACIÓN: Valida integridad referencial impidiendo añadir desgloses operacionales a una orden de cabecera inexistente.
            if (!existeId(conn, "pct_reportes_cab", "RepCabCod", fkRepDetCab)) {
                System.out.println("Error: La cabecera transaccional indicada no existe."); return;
            }
            sc.nextLine(); // Limpieza buffer

            System.out.print("Código Vivienda Fiscalizada FK (FKRepDetViv - String): ");
            String fkRepDetViv = sc.nextLine();
            // EXPLICACIÓN: Valida integridad referencial corroborando que el predio asignado se encuentre empadronado físicamente.
            if (!existeId(conn, "c3m_vivienda", "VivCod", fkRepDetViv)) {
                System.out.println("Error: La vivienda catastrada indicada no existe."); return;
            }

            System.out.print("Monto Calculado por Impuesto de Fiscalización S/: ");
            double monCal = sc.nextDouble();

            System.out.print("Monto Pagado a la Fecha S/: ");
            double monPag = sc.nextDouble();
            sc.nextLine(); // Limpieza buffer

            String sql = "INSERT INTO pct_reportes_det (RepDetCod, FKRepDetCab, FKRepDetViv, RepDetMonCal, RepDetMonPag, RepDetEstReg) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, repDetCod);
            stmt.setInt(2, fkRepCabPar(fkRepDetCab)); // Asignación del valor entero evaluado
            stmt.setString(3, fkRepDetViv);
            stmt.setDouble(4, monCal);
            stmt.setDouble(5, monPag);
            stmt.setString(6, "1");

            // Corrección dinámica directa de la asignación del parámetro de cabecera de ejecución externa
            stmt.setInt(2, fkRepDetCab);

            stmt.executeUpdate();
            System.out.println("Línea de detalle transaccional añadida con éxito.");
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // Auxiliar estructural interno
    private static int fkRepCabPar(int v) { return v; }

    public static void listarReporteDet() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: INNER JOIN múltiple para descifrar las claves foráneas cruzando con la descripción del reporte y la calle del predio afectado.
            String sql = "SELECT rd.RepDetCod, rd.FKRepDetCab, rc.RepCabDes, rd.FKRepDetViv, d.DirViaNom, rd.RepDetMonCal, rd.RepDetMonPag " +
                         "FROM pct_reportes_det rd " +
                         "INNER JOIN pct_reportes_cab rc ON rd.FKRepDetCab = rc.RepCabCod " +
                         "INNER JOIN c3m_vivienda v ON rd.FKRepDetViv = v.VivCod " +
                         "INNER JOIN c3m_direccion d ON v.VivDir = d.DirCod " +
                         "WHERE rd.RepDetEstReg = '1' ORDER BY rd.FKRepDetCab ASC, rd.RepDetCod ASC";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- DESGLOSE ACTIVO DE PREDIOS BAJO FISCALIZACIÓN ---");
            while (rs.next()) {
                System.out.println(
                    "ID Detalle: " + rs.getInt("RepDetCod") + " | " +
                    "Cabecera ID: " + rs.getInt("FKRepDetCab") + " (" + rs.getString("RepCabDes") + ") | " +
                    "Viv ID: " + rs.getString("FKRepDetViv") + " (" + rs.getString("DirViaNom") + ") | " +
                    "Calc: S/ " + rs.getDouble("RepDetMonCal") + " | " +
                    "Pagado: S/ " + rs.getDouble("RepDetMonPag")
                );
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void buscarReporteDet() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Trae la línea transaccional específica cruzando su información descriptiva habitacional externa.
            String sql = "SELECT rd.*, rc.RepCabDes, d.DirViaNom, d.DirNum FROM pct_reportes_det rd " +
                         "INNER JOIN pct_reportes_cab rc ON rd.FKRepDetCab = rc.RepCabCod " +
                         "INNER JOIN c3m_vivienda v ON rd.FKRepDetViv = v.VivCod " +
                         "INNER JOIN c3m_direccion d ON v.VivDir = d.DirCod " +
                         "WHERE rd.RepDetCod = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            System.out.print("Código de Línea de Detalle a consultar: ");
            int repDetCod = sc.nextInt();
            stmt.setInt(1, repDetCod);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("\n--- EXPEDIENTE DE RENDIMIENTO DE PREDIO EN ORDEN ---");
                System.out.println("Código Registro Detalle: " + rs.getInt("RepDetCod"));
                System.out.println("Orden de Cabecera       : " + rs.getString("RepCabDes") + " (ID: " + rs.getInt("FKRepDetCab") + ")");
                System.out.println("Predio Identificado    : ID " + rs.getString("FKRepDetViv") + " (" + rs.getString("DirViaNom") + " #" + rs.getInt("DirNum") + ")");
                System.out.println("Monto Determinado      : S/ " + rs.getDouble("RepDetMonCal"));
                System.out.println("Monto Amortizado Real  : S/ " + rs.getDouble("RepDetMonPag"));
                System.out.println("Estado de Auditoría    : " + rs.getString("RepDetEstReg"));
            } else {
                System.out.println("Línea de desglose transaccional no encontrada.");
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void actualizarReporteDet() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Modifica los saldos contables financieros de la vivienda auditada ubicándola por su ID maestro de detalle.
            String sql = "UPDATE pct_reportes_det SET RepDetMonCal=?, RepDetMonPag=? WHERE RepDetCod=?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código del Detalle de Reporte a modificar: ");
            int repDetCod = sc.nextInt();

            System.out.print("Nuevo Monto Calculado S/: ");
            double nCal = sc.nextDouble();
            System.out.print("Nuevo Monto Pagado Real S/: ");
            double nPag = sc.nextDouble();

            stmt.setDouble(1, nCal);
            stmt.setDouble(2, nPag);
            stmt.setInt(3, repDetCod);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Balances financieros de la línea transaccional actualizados.");
            else System.out.println("No se localizó el sub-registro solicitado.");

            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public static void eliminarReporteDet() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Ejecuta la remoción física directa de una línea de desglose de la orden operativa activa.
            String sql = "DELETE FROM pct_reportes_det WHERE RepDetCod=?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código del Detalle de Reporte a purgar físicamente: ");
            int repDetCod = sc.nextInt();
            stmt.setInt(1, repDetCod);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Registro de línea de reporte eliminado físicamente del sistema operativo.");
            else System.out.println("No se encontró el desglose especificado.");

            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
