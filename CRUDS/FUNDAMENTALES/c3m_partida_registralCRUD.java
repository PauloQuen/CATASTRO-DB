import java.sql.*;
import java.util.Scanner;

public class c3m_partida_registralCRUD {

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
            System.out.println("\n===== CRUD PARTIDA REGISTRAL (SUNARP - CATASTRO) =====");
            System.out.println("1. Registrar Partida");
            System.out.println("2. Listar Partidas Inscriptas");
            System.out.println("3. Buscar Partida por Número");
            System.out.println("4. Actualizar Asiento o Fecha");
            System.out.println("5. Eliminar Registro");
            System.out.println("6. Salir");
            System.out.print("Opcion: ");
            opcion = sc.nextInt();
            sc.nextLine(); // Limpieza de buffer

            switch (opcion) {
                case 1: insertarPartida(); break;
                case 2: listarPartidas(); break;
                case 3: buscarPartida(); break;
                case 4: actualizarPartida(); break;
                case 5: eliminarPartida(); break;
            }
        } while (opcion != 6);
    }

    // EXPLICACIÓN: Validador genérico para comprobar consistencia de llaves foráneas
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

    public static void insertarPartida() {
        Connection conn = conectar();
        try {
            System.out.print("Número de Partida Electrónica (SUNARP): ");
            String numPartida = sc.nextLine();

            // EXPLICACIÓN: Evita la duplicidad de la clave primaria
            if (existeId(conn, "c3m_partida_registral", "PrtNumPartida", numPartida)) {
                System.out.println("Error: Esta partida registral ya se encuentra inscrita en el sistema.");
                return;
            }

            System.out.print("Código de la Vivienda asociada (PrtViv): ");
            String prtViv = sc.nextLine();

            // EXPLICACIÓN: Valida integridad con la tabla fundamental C3M_VIVIENDA
            if (!existeId(conn, "c3m_vivienda", "VivCod", prtViv)) {
                System.out.println("Error: El código de vivienda indicado no existe.");
                return;
            }

            System.out.print("DNI del Propietario Legal (PrtPropDNI): ");
            String prtPropDNI = sc.nextLine();

            // EXPLICACIÓN: Valida integridad con la tabla fundamental H6M_PERSONA
            if (!existeId(conn, "h6m_persona", "PerDNI", prtPropDNI)) {
                System.out.println("Error: El DNI del propietario no está registrado en el padrón de personas.");
                return;
            }

            System.out.print("Asiento Registral (Ejm: A0001): ");
            String asiento = sc.nextLine();

            System.out.print("Fecha de Inscripción (YYYY-MM-DD): ");
            String fechaIns = sc.nextLine();

            String sql = "INSERT INTO c3m_partida_registral (PrtNumPartida, PrtViv, PrtAsiento, PrtFecIns, PrtPropDNI, PrtEstReg) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, numPartida);
            stmt.setString(2, prtViv);
            stmt.setString(3, asiento);
            stmt.setDate(4, Date.valueOf(fechaIns)); // Parsea la cadena a tipo java.sql.Date
            stmt.setString(5, prtPropDNI);
            stmt.setString(6, "1");

            stmt.executeUpdate();
            System.out.println("Partida Registral vinculada y guardada con éxito.");
            stmt.close(); conn.close();
        } catch (IllegalArgumentException e) {
            System.out.println("Error: El formato de fecha introducido es incorrecto (use YYYY-MM-DD).");
        } catch (Exception e) {
            System.out.println("Error al insertar: " + e.getMessage());
        }
    }

    public static void listarPartidas() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Cruza datos con la vivienda y el propietario para generar un reporte formal inteligible
            String sql = "SELECT p.PrtNumPartida, p.PrtViv, p.PrtAsiento, p.PrtFecIns, per.PerNom, per.PerApePat " +
                         "FROM c3m_partida_registral p " +
                         "INNER JOIN c3m_vivienda v ON p.PrtViv = v.VivCod " +
                         "INNER JOIN h6m_persona per ON p.PrtPropDNI = per.PerDNI " +
                         "WHERE p.PrtEstReg = '1'";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- PADRÓN DE PREDIOS FORMALIZADOS (SUNARP) ---");
            while (rs.next()) {
                System.out.println(
                    "Partida: " + rs.getString("PrtNumPartida") + " | " +
                    "Predio Cod: " + rs.getString("PrtViv") + " | " +
                    "Asiento: " + rs.getString("PrtAsiento") + " | " +
                    "Fec. Insc: " + rs.getDate("PrtFecIns") + " | " +
                    "Titular: " + rs.getString("PerNom") + " " + rs.getString("PerApePat")
                );
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
    }

    public static void buscarPartida() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Trae todo el desglose legal e incluye la dirección de la vivienda usando múltiples JOINs
            String sql = "SELECT p.*, per.PerNom, per.PerApePat, d.DirViaNom, d.DirNum " +
                         "FROM c3m_partida_registral p " +
                         "INNER JOIN h6m_persona per ON p.PrtPropDNI = per.PerDNI " +
                         "INNER JOIN c3m_vivienda v ON p.PrtViv = v.VivCod " +
                         "INNER JOIN c3m_direccion d ON v.VivDir = d.DirCod " +
                         "WHERE p.PrtNumPartida = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            System.out.print("Ingrese el Número de Partida a buscar: ");
            String numPartida = sc.nextLine();
            stmt.setString(1, numPartida);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("\n--- CONSULTA DE PROPIEDAD JURÍDICA ---");
                System.out.println("Partida Electrónica : " + rs.getString("PrtNumPartida"));
                System.out.println("Asiento de Dominio  : " + rs.getString("PrtAsiento"));
                System.out.println("Fecha de Registro   : " + rs.getDate("PrtFecIns"));
                System.out.println("Código Catastral    : " + rs.getString("PrtViv"));
                System.out.println("Dirección Inmueble  : " + rs.getString("DirViaNom") + " #" + rs.getInt("DirNum"));
                System.out.println("Propietario Legal   : " + rs.getString("PerNom") + " " + rs.getString("PerApePat") + " (DNI: " + rs.getString("PrtPropDNI") + ")");
            } else {
                System.out.println("La partida registral ingresada no se encuentra en el catastro.");
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al buscar: " + e.getMessage());
        }
    }

    public static void actualizarPartida() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE c3m_partida_registral SET PrtAsiento = ?, PrtFecIns = ? WHERE PrtNumPartida = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Número de Partida a modificar: ");
            String numPartida = sc.nextLine();

            if (!existeId(conn, "c3m_partida_registral", "PrtNumPartida", numPartida)) {
                System.out.println("❌ No existe el registro de partida indicado.");
                return;
            }

            System.out.print("Nuevo Asiento (Ejm: A0002): ");
            String asiento = sc.nextLine();

            System.out.print("Nueva Fecha de Inscripción (YYYY-MM-DD): ");
            String fechaIns = sc.nextLine();

            stmt.setString(1, asiento);
            stmt.setDate(2, Date.valueOf(fechaIns));
            stmt.setString(3, numPartida);

            stmt.executeUpdate();
            System.out.println("✔ Información legal de SUNARP actualizada.");
            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al actualizar: " + e.getMessage());
        }
    }

    public static void eliminarPartida() {
        Connection conn = conectar();
        try {
            String sql = "DELETE FROM c3m_partida_registral WHERE PrtNumPartida = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Número de Partida Registral a eliminar físicamente: ");
            String numPartida = sc.nextLine();
            stmt.setString(1, numPartida);

            int filas = stmt.executeUpdate();
            if (filas > 0) {
                System.out.println("Partida dada de baja y removida físicamente de los registros.");
            } else {
                System.out.println("No se localizó el número de partida.");
            }
            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al eliminar: " + e.getMessage());
        }
    }
}