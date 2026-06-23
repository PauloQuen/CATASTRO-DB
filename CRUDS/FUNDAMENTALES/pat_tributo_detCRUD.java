import java.sql.*;
import java.util.Scanner;

public class pat_tributo_detCRUD {

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
            System.out.println("\n===== CRUD CUENTA CORRIENTE TRIBUTARIA (DETALLE) =====");
            System.out.println("1. Añadir Cuota/Periodo a una Cabecera");
            System.out.println("2. Listar Todo el Detalle de Transacciones");
            System.out.println("3. Ver Estado de Cuenta Desglosado por Cabecera");
            System.out.println("4. Registrar Pago de una Cuota/Periodo");
            System.out.println("5. Eliminar Cuota (Físico)");
            System.out.println("6. Salir");
            System.out.print("Opción: ");
            opcion = sc.nextInt();
            sc.nextLine();

            switch (opcion) {
                case 1: insertarDetalle(); break;
                case 2: listarTodoDetalle(); break;
                case 3: buscarDetallesPorCabecera(); break;
                case 4: registrarPagoCuota(); break;
                case 5: eliminarDetalle(); break;
            }
        } while (opcion != 6);
    }

    private static boolean existeCabecera(Connection conn, int cabCod) throws SQLException {
        String sql = "SELECT COUNT(*) FROM pat_tributo_cab WHERE TriCabCod = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, cabCod);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public static void insertarDetalle() {
        Connection conn = conectar();
        try {
            System.out.print("Código de Cabecera Vinculada (TriDetCab): ");
            int cabCod = sc.nextInt();
            sc.nextLine();

            if (!existeCabecera(conn, cabCod)) {
                System.out.println("Error: La cabecera tributaria especificada no existe.");
                return;
            }

            System.out.print("Periodo Tributario (TriDetPer - Ejm: 202601): ");
            String periodo = sc.nextLine().trim();

            System.out.print("Monto de la Cuota S/ (TriDetMonCal): ");
            double monCal = sc.nextDouble();

            System.out.print("Monto Pagado de la Cuota S/ (TriDetMonPag): ");
            double monPag = sc.nextDouble();

            String sql = "INSERT INTO pat_tributo_det (TriDetCab, TriDetPer, TriDetMonCal, TriDetMonPag, TriDetEstReg) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, cabCod);
            stmt.setString(2, periodo);
            stmt.setDouble(3, monCal);
            stmt.setDouble(4, monPag);
            stmt.setString(5, "1");

            stmt.executeUpdate();
            System.out.println("✔ Cuota de periodo añadida correctamente al desglose financiero.");
            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al insertar detalle: " + e.getMessage());
        }
    }

    public static void listarTodoDetalle() {
        Connection conn = conectar();
        try {
            String sql = "SELECT d.TriDetCod, d.TriDetCab, d.TriDetPer, d.TriDetMonCal, d.TriDetMonPag, c.TriCabAno " +
                         "FROM pat_tributo_det d " +
                         "INNER JOIN pat_tributo_cab c ON d.TriDetCab = c.TriCabCod " +
                         "WHERE d.TriDetEstReg = '1'";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- LISTADO GENERAL DE DESGLOSE DE CUOTAS DE CATASTRO ---");
            while (rs.next()) {
                System.out.println(
                    "ID Det: " + rs.getInt("TriDetCod") + " | " +
                    "ID Cab Ref: " + rs.getInt("TriDetCab") + " (" + rs.getInt("TriCabAno") + ") | " +
                    "Periodo: " + rs.getString("TriDetPer") + " | " +
                    "Monto Impuesto: S/ " + rs.getDouble("TriDetMonCal") + " | " +
                    "Monto Cobrado: S/ " + rs.getDouble("TriDetMonPag")
                );
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar todo el detalle: " + e.getMessage());
        }
    }

    public static void buscarDetallesPorCabecera() {
        Connection conn = conectar();
        try {
            System.out.print("Ingrese el Código de la Cabecera Tributaria a desglosar: ");
            int cabCod = sc.nextInt();

            if (!existeCabecera(conn, cabCod)) {
                System.out.println("No existe el estado de cuenta consultado.");
                return;
            }

            String sql = "SELECT d.TriDetCod, d.TriDetPer, d.TriDetMonCal, d.TriDetMonPag, " +
                         "(d.TriDetMonCal - d.TriDetMonPag) AS BalanceCuota " +
                         "FROM pat_tributo_det d " +
                         "WHERE d.TriDetCab = ? AND d.TriDetEstReg = '1' " +
                         "ORDER BY d.TriDetPer ASC";

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, cabCod);
            ResultSet rs = stmt.executeQuery();

            System.out.println("\n--- ESTADO DE CUENTA CORRIENTE DETALLADO DE LA CABECERA: " + cabCod + " ---");
            boolean vacio = true;
            while (rs.next()) {
                vacio = false;
                System.out.println(
                    "- ID Item: " + rs.getInt("TriDetCod") + " | " +
                    "Periodo Fiscal: " + rs.getString("TriDetPer") + " | " +
                    "Monto Base: S/ " + rs.getDouble("TriDetMonCal") + " | " +
                    "Amortizado: S/ " + rs.getDouble("TriDetMonPag") + " | " +
                    "Por Pagar: S/ " + rs.getDouble("BalanceCuota")
                );
            }
            if (vacio) {
                System.out.println("Esta cuenta corriente aún no tiene subcuotas ni periodos liquidados.");
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al consultar desglose: " + e.getMessage());
        }
    }

    public static void registrarPagoCuota() {
        Connection conn = conectar();
        try {
            System.out.print("Ingrese ID del Detalle de Cuota (TriDetCod): ");
            int detCod = sc.nextInt();

            String sql = "SELECT TriDetMonCal, TriDetMonPag FROM pat_tributo_det WHERE TriDetCod = ?";
            double cuotaCal = 0, cuotaPag = 0;
            boolean existe = false;

            try (PreparedStatement check = conn.prepareStatement(sql)) {
                check.setInt(1, detCod);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        existe = true;
                        cuotaCal = rs.getDouble("TriDetMonCal");
                        cuotaPag = rs.getDouble("TriDetMonPag");
                    }
                }
            }

            if (!existe) {
                System.out.println("El código de cuota especificado no existe.");
                return;
            }

            System.out.println("Monto de la cuota: S/ " + cuotaCal + " | Pagado a la fecha: S/ " + cuotaPag);
            System.out.print("Ingrese el monto a pagar en esta operación S/: ");
            double nuevoPago = sc.nextDouble();

            if (cuotaPag + nuevoPago > cuotaCal) {
                System.out.println("Alerta: El pago excede el total de la deuda mensual. Ajustando al límite.");
                cuotaPag = cuotaCal;
            } else {
                cuotaPag += nuevoPago;
            }

            sql = "UPDATE pat_tributo_det SET TriDetMonPag = ? WHERE TriDetCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setDouble(1, cuotaPag);
            stmt.setInt(2, detCod);
            stmt.executeUpdate();

            System.out.println("Transacción de pago registrada correctamente en la subcuota.");
            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al procesar pago de cuota: " + e.getMessage());
        }
    }

    public static void eliminarDetalle() {
        Connection conn = conectar();
        try {
            String sql = "DELETE FROM pat_tributo_det WHERE TriDetCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese el ID del detalle de tributo a eliminar físicamente: ");
            int detCod = sc.nextInt();
            stmt.setInt(1, detCod);

            int filas = stmt.executeUpdate();
            if (filas > 0) {
                System.out.println("Subcuota purgada físicamente de las transacciones.");
            } else {
                System.out.println("No se encontró el registro de detalle indicado.");
            }
            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al eliminar detalle: " + e.getMessage());
        }
    }
}
