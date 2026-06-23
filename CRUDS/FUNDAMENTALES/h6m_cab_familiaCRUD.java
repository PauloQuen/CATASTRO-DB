import java.sql.*;
import java.util.Scanner;

public class h6m_cab_familiaCRUD {

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
            System.out.println("\n===== CRUD RELACIÓN CABEZA - MIEMBRO FAMILIAR =====");
            System.out.println("1. Vincular Miembro a Cabeza de Familia");
            System.out.println("2. Listar Relaciones Familiares Activas");
            System.out.println("3. Buscar Miembros de una Cabeza (por DNI)");
            System.out.println("4. Actualizar Fecha Fin / Concluir Vínculo");
            System.out.println("5. Eliminar Registro (Físico)");
            System.out.println("6. Salir");
            System.out.print("Opcion: ");
            opcion = sc.nextInt();
            sc.nextLine(); // Limpieza buffer

            switch (opcion) {
                case 1: insertarRelacion(); break;
                case 2: listarRelaciones(); break;
                case 3: buscarPorCabeza(); break;
                case 4: actualizarFechaFin(); break;
                case 5: eliminarRelacion(); break;
            }
        } while (opcion != 6);
    }

    // EXPLICACIÓN: Validador genérico para integridad referencial de llaves foráneas (DNI)
    private static boolean existePersona(Connection conn, int dni) throws SQLException {
        String sql = "SELECT COUNT(*) FROM h6m_persona WHERE PerDNI = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, dni);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    // EXPLICACIÓN: Validador específico para evitar duplicar la clave primaria compuesta (CabDNI, MieDNI)
    private static boolean existeRelacion(Connection conn, int cabDni, int mieDni) throws SQLException {
        String sql = "SELECT COUNT(*) FROM h6m_cab_familia WHERE CabDNI = ? AND MieDNI = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, cabDni);
            stmt.setInt(2, mieDni);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public static void insertarRelacion() {
        Connection conn = conectar();
        try {
            System.out.print("DNI de la Cabeza de Familia (CabDNI): ");
            int cabDni = sc.nextInt();
            
            if (!existePersona(conn, cabDni)) {
                System.out.println("Error: El DNI de la cabeza de familia no existe en H6M_PERSONA.");
                return;
            }

            System.out.print("DNI del Miembro Familiar (MieDNI): ");
            int mieDni = sc.nextInt();
            sc.nextLine(); // Limpieza buffer

            if (!existePersona(conn, mieDni)) {
                System.out.println("Error: El DNI del miembro no existe en H6M_PERSONA.");
                return;
            }

            if (existeRelacion(conn, cabDni, mieDni)) {
                System.out.println("Error: Ya existe este vínculo familiar registrado en el sistema.");
                return;
            }

            System.out.print("Fecha de Inicio de Relación (YYYY-MM-DD): ");
            String fecIni = sc.nextLine();

            String sql = "INSERT INTO h6m_cab_familia (CabDNI, MieDNI, CabFecIni, CabFecFin, CabEstReg) VALUES (?, ?, ?, NULL, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, cabDni);
            stmt.setInt(2, mieDni);
            stmt.setDate(3, Date.valueOf(fecIni));
            stmt.setString(4, "1");

            stmt.executeUpdate();
            System.out.println("Relación familiar vinculada exitosamente.");
            stmt.close(); conn.close();
        } catch (IllegalArgumentException e) {
            System.out.println("Error: Formato de fecha inválido. Use YYYY-MM-DD.");
        } catch (Exception e) {
            System.out.println("Error al insertar: " + e.getMessage());
        }
    }

    public static void listarRelaciones() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: Se ejecutan dos INNER JOIN a la tabla h6m_persona diferenciando por alias (c y m) para traer los nombres reales
            String sql = "SELECT r.CabDNI, CONCAT(c.PerNom, ' ', c.PerApePat) AS Cabeza, " +
                         "r.MieDNI, CONCAT(m.PerNom, ' ', m.PerApePat) AS Miembro, " +
                         "r.CabFecIni, r.CabFecFin " +
                         "FROM h6m_cab_familia r " +
                         "INNER JOIN h6m_persona c ON r.CabDNI = c.PerDNI " +
                         "INNER JOIN h6m_persona m ON r.MieDNI = m.PerDNI " +
                         "WHERE r.CabEstReg = '1'";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- MAPEO COMPLETO DE NÚCLEOS FAMILIARES ACTIVOS ---");
            while (rs.next()) {
                Date fFin = rs.getDate("CabFecFin");
                String finStr = (fFin == null) ? "VIGENTE" : fFin.toString();
                System.out.println(
                    "CABEZA: " + rs.getString("Cabeza") + " (" + rs.getInt("CabDNI") + ") " +
                    "➡ MIEMBRO: " + rs.getString("Miembro") + " (" + rs.getInt("MieDNI") + ") | " +
                    "Inicio: " + rs.getDate("CabFecIni") + " | Fin: " + finStr
                );
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
    }

    public static void buscarPorCabeza() {
        Connection conn = conectar();
        try {
            String sql = "SELECT r.MieDNI, CONCAT(m.PerNom, ' ', m.PerApePat) AS Miembro, r.CabFecIni, r.CabFecFin " +
                         "FROM h6m_cab_familia r " +
                         "INNER JOIN h6m_persona m ON r.MieDNI = m.PerDNI " +
                         "WHERE r.CabDNI = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            System.out.print("Ingrese DNI de la Cabeza de Familia para ver sus dependientes: ");
            int cabDni = sc.nextInt();
            stmt.setInt(1, cabDni);

            ResultSet rs = stmt.executeQuery();
            System.out.println("\n--- MIEMBROS ASOCIADOS AL DNI JURADO: " + cabDni + " ---");
            boolean tieneRegistros = false;
            while (rs.next()) {
                tieneRegistros = true;
                Date fFin = rs.getDate("CabFecFin");
                String finStr = (fFin == null) ? "VIGENTE" : fFin.toString();
                System.out.println(
                    "- DNI Miembro: " + rs.getInt("MieDNI") + " | Apellidos y Nombres: " + rs.getString("Miembro") +
                    " | Integrado desde: " + rs.getDate("CabFecIni") + " | Estado: " + finStr
                );
            }
            if (!tieneRegistros) {
                System.out.println("No se encontraron miembros dependientes para el DNI consultado.");
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al buscar: " + e.getMessage());
        }
    }

    public static void actualizarFechaFin() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE h6m_cab_familia SET CabFecFin = ? WHERE CabDNI = ? AND MieDNI = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("DNI Cabeza de Familia: ");
            int cabDni = sc.nextInt();
            System.out.print("DNI Miembro a desvincular: ");
            int mieDni = sc.nextInt();
            sc.nextLine(); // Limpieza buffer

            if (!existeRelacion(conn, cabDni, mieDni)) {
                System.out.println("No existe el vínculo especificado.");
                return;
            }

            System.out.print("Ingrese Fecha de Fin / Salida del hogar (YYYY-MM-DD): ");
            String fecFin = sc.nextLine();

            stmt.setDate(1, Date.valueOf(fecFin));
            stmt.setInt(2, cabDni);
            stmt.setInt(3, mieDni);

            stmt.executeUpdate();
            System.out.println("Vínculo familiar actualizado/concluido correctamente.");
            stmt.close(); conn.close();
        } catch (IllegalArgumentException e) {
            System.out.println("Error: Formato de fecha inválido. Use YYYY-MM-DD.");
        } catch (Exception e) {
            System.out.println("Error al actualizar: " + e.getMessage());
        }
    }

    public static void eliminarRelacion() {
        Connection conn = conectar();
        try {
            String sql = "DELETE FROM h6m_cab_familia WHERE CabDNI = ? AND MieDNI = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("DNI Cabeza de la relación a eliminar físicamente: ");
            int cabDni = sc.nextInt();
            System.out.print("DNI Miembro de la relación a eliminar físicamente: ");
            int mieDni = sc.nextInt();

            stmt.setInt(1, cabDni);
            stmt.setInt(2, mieDni);

            int filas = stmt.executeUpdate();
            if (filas > 0) {
                System.out.println("Relación purgada y borrada físicamente de la base de datos.");
            } else {
                System.out.println("No se encontró el par de llaves indicado.");
            }
            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al eliminar: " + e.getMessage());
        }
    }
}