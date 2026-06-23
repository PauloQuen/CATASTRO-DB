import java.sql.*;
import java.util.Scanner;

public class pat_tributo_cabCRUD {

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
            System.out.println("\n===== CRUD CUENTA CORRIENTE TRIBUTARIA (CABECERA) =====");
            System.out.println("1. Aperturar Año Tributario a Propietario");
            System.out.println("2. Listar Cabeceras de Estado de Cuenta");
            System.out.println("3. Buscar Estado de Cuenta por Código");
            System.out.println("4. Regularizar Saldos Acumulados");
            System.out.println("5. Eliminar Registro (Físico)");
            System.out.println("6. Salir");
            System.out.print("Opción: ");
            opcion = sc.nextInt();
            sc.nextLine();

            switch (opcion) {
                case 1: insertarCabecera(); break;
                case 2: listarCabeceras(); break;
                case 3: buscarCabecera(); break;
                case 4: actualizarSaldos(); break;
                case 5: eliminarCabecera(); break;
            }
        } while (opcion != 6);
    }

    private static boolean existePropietario(Connection conn, int proCod) throws SQLException {
        String sql = "SELECT COUNT(*) FROM h8m_propietario WHERE ProCod = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, proCod);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    private static boolean existeCabeceraAno(Connection conn, int proCod, int ano) throws SQLException {
        String sql = "SELECT COUNT(*) FROM pat_tributo_cab WHERE TriCabPro = ? AND TriCabAno = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, proCod);
            stmt.setInt(2, ano);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public static void insertarCabecera() {
        Connection conn = conectar();
        try {
            System.out.print("Código del Propietario Contribuyente (TriCabPro): ");
            int proCod = sc.nextInt();
            
            if (!existePropietario(conn, proCod)) {
                System.out.println("Error: El código de propietario no existe en H8M_PROPIETARIO.");
                return;
            }

            System.out.print("Año Fiscal a Aperturar (TriCabAno - Ejm: 2026): ");
            int ano = sc.nextInt();

            if (existeCabeceraAno(conn, proCod, ano)) {
                System.out.println("Error: El propietario ya cuenta con un estado de cuenta abierto para el año " + ano);
                return;
            }

            System.out.print("Monto Inicial Determinado/Calculado S/ (TriCabTotCal): ");
            double totCal = sc.nextDouble();

            System.out.print("Monto Inicial Pagado S/ (TriCabTotPag): ");
            double totPag = sc.nextDouble();

            String sql = "INSERT INTO pat_tributo_cab (TriCabPro, TriCabAno, TriCabTotCal, TriCabTotPag, TriCabEstReg) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, proCod);
            stmt.setInt(2, ano);
            stmt.setDouble(3, totCal);
            stmt.setDouble(4, totPag);
            stmt.setString(5, "1");

            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                System.out.println("✔ Cabecera de tributo aperturada con éxito. Código Asignado: " + rs.getInt(1));
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al insertar cabecera: " + e.getMessage());
        }
    }

    public static void listarCabeceras() {
        Connection conn = conectar();
        try {
            String sql = "SELECT tc.TriCabCod, tc.TriCabPro, p.PerNom, p.PerApePat, tc.TriCabAno, tc.TriCabTotCal, tc.TriCabTotPag " +
                         "FROM pat_tributo_cab tc " +
                         "INNER JOIN h8m_propietario pr ON tc.TriCabPro = pr.ProCod " +
                         "INNER JOIN h6m_persona p ON pr.ProPer = p.PerDNI " +
                         "WHERE tc.TriCabEstReg = '1'";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- ESTADOS DE CUENTA ANUALES - RESUMEN GENERAL ---");
            while (rs.next()) {
                double saldo = rs.getDouble("TriCabTotCal") - rs.getDouble("TriCabTotPag");
                System.out.println(
                    "ID Cab: " + rs.getInt("TriCabCod") + " | " +
                    "Propietario: " + rs.getString("PerNom") + " " + rs.getString("PerApePat") + " (Cod: " + rs.getInt("TriCabPro") + ") | " +
                    "Año: " + rs.getInt("TriCabAno") + " | " +
                    "Total Calc: S/ " + rs.getDouble("TriCabTotCal") + " | " +
                    "Total Pag: S/ " + rs.getDouble("TriCabTotPag") + " | " +
                    "Deuda Pendiente: S/ " + saldo
                );
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar cabeceras: " + e.getMessage());
        }
    }

    public static void buscarCabecera() {
        Connection conn = conectar();
        try {
            String sql = "SELECT tc.*, p.PerNom, p.PerApePat, pr.ProViv " +
                         "FROM pat_tributo_cab tc " +
                         "INNER JOIN h8m_propietario pr ON tc.TriCabPro = pr.ProCod " +
                         "INNER JOIN h6m_persona p ON pr.ProPer = p.PerDNI " +
                         "WHERE tc.TriCabCod = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            System.out.print("Ingrese Código de Cabecera Tributaria: ");
            int id = sc.nextInt();
            stmt.setInt(1, id);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("\n--- EXPEDIENTE FINANCIERO ANUAL ---");
                System.out.println("Código Cuenta Corriente: " + rs.getInt("TriCabCod"));
                System.out.println("Año Gravable            : " + rs.getInt("TriCabAno"));
                System.out.println("Contribuyente Titular  : " + rs.getString("PerNom") + " " + rs.getString("PerApePat"));
                System.out.println("Predio Base Declarado  : " + rs.getString("ProViv"));
                System.out.println("----------------------------------------");
                System.out.println("Monto Total Determinado : S/ " + rs.getDouble("TriCabTotCal"));
                System.out.println("Monto Total Amortizado  : S/ " + rs.getDouble("TriCabTotPag"));
                System.out.println("Saldo Neto Actual       : S/ " + (rs.getDouble("TriCabTotCal") - rs.getDouble("TriCabTotPag")));
            } else {
                System.out.println("No se encontró el estado de cuenta solicitado.");
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al buscar: " + e.getMessage());
        }
    }

    public static void actualizarSaldos() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE pat_tributo_cab SET TriCabTotCal = ?, TriCabTotPag = ? WHERE TriCabCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código de Cabecera a regularizar: ");
            int id = sc.nextInt();

            sql = "SELECT COUNT(*) FROM pat_tributo_cab WHERE TriCabCod = ?";
            try (PreparedStatement check = conn.prepareStatement(sql)) {
                check.setInt(1, id);
                ResultSet rs = check.executeQuery();
                if (!rs.next() || rs.getInt(1) == 0) {
                    System.out.println("Error: Código de cabecera no válido.");
                    return;
                }
            }

            System.out.print("Nuevo Total Determinado / Ajustado S/: ");
            double totCal = sc.nextDouble();
            System.out.print("Nuevo Total Pagado / Recaudado S/: ");
            double totPag = sc.nextDouble();

            stmt.setDouble(1, totCal);
            stmt.setDouble(2, totPag);
            stmt.setInt(3, id);

            stmt.executeUpdate();
            System.out.println("Saldos consolidados de la cabecera actualizados.");
            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al actualizar saldo: " + e.getMessage());
        }
    }

    public static void eliminarCabecera() {
        Connection conn = conectar();
        try {
            String sql = "DELETE FROM pat_tributo_cab WHERE TriCabCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código de cabecera tributaria a purgar físicamente: ");
            int id = sc.nextInt();
            stmt.setInt(1, id);

            int filas = stmt.executeUpdate();
            if (filas > 0) {
                System.out.println("Cabecera eliminada del sistema.");
            } else {
                System.out.println("No se encontró el registro.");
            }
            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error de integridad: No puede eliminar la cabecera si posee desgloses activos en la tabla de detalles.");
        }
    }
}
