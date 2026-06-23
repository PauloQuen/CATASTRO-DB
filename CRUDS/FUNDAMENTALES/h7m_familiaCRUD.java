import java.sql.*;
import java.util.Scanner;

public class h7m_familiaCRUD {

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
            System.out.println("\n===== CRUD GESTIÓN DE FAMILIAS (CATASTRO SOCIAL) =====");
            System.out.println("1. Registrar Grupo Familiar");
            System.out.println("2. Listar Familias Activas");
            System.out.println("3. Buscar Familia por Código");
            System.out.println("4. Actualizar Miembros e Ingresos");
            System.out.println("5. Eliminar Registro");
            System.out.println("6. Salir");
            System.out.print("Opcion: ");
            opcion = sc.nextInt();
            sc.nextLine(); // Limpieza de buffer

            switch (opcion) {
                case 1: insertarFamilia(); break;
                case 2: listarFamilias(); break;
                case 3: buscarFamilia(); break;
                case 4: actualizarFamilia(); break;
                case 5: eliminarFamilia(); break;
            }
        } while (opcion != 6);
    }

    // EXPLICACIÓN: Validador genérico para comprobar consistencia relacional previa en la base de datos
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

    public static void insertarFamilia() {
        Connection conn = conectar();
        try {
            System.out.print("DNI de la Cabeza de Familia (FamCab): ");
            String famCab = sc.nextLine();

            // EXPLICACIÓN: Valida integridad con la tabla fundamental de personas (H6M_PERSONA)
            if (!existeId(conn, "h6m_persona", "PerDNI", famCab)) {
                System.out.println("Error: El DNI de la cabeza de familia no está registrado en el padrón de personas.");
                return;
            }

            // EXPLICACIÓN: Opcional - Comprobar si esa persona ya lidera otro grupo familiar registrado
            if (existeId(conn, "h7m_familia", "FamCab", famCab)) {
                System.out.println("Advertencia: Este ciudadano ya figura como cabeza de un grupo familiar.");
            }

            System.out.print("Número Inicial de Miembros (FamNumMiem): ");
            int numMiem = sc.nextInt();

            System.out.print("Ingreso Total del Grupo Familiar S/ (FamIngTot): ");
            double ingTot = sc.nextDouble();
            sc.nextLine(); // Limpieza buffer

            String sql = "INSERT INTO h7m_familia (FamCab, FamNumMiem, FamIngTot, FamEstReg) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, famCab);
            stmt.setInt(2, numMiem);
            stmt.setDouble(3, ingTot);
            stmt.setString(4, "1");

            stmt.executeUpdate();
            System.out.println("Grupo Familiar registrado con éxito en el catastro social.");
            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al registrar familia: " + e.getMessage());
        }
    }

    public static void listarFamilias() {
        Connection conn = conectar();
        try {
            // EXPLICACIÓN: INNER JOIN con H6M_PERSONA para mostrar el nombre del líder familiar en lugar del DNI aislado
            String sql = "SELECT f.FamCod, f.FamCab, p.PerNom, p.PerApePat, f.FamNumMiem, f.FamIngTot " +
                         "FROM h7m_familia f " +
                         "INNER JOIN h6m_persona p ON f.FamCab = p.PerDNI " +
                         "WHERE f.FamEstReg = '1'";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- PADRÓN DE GRUPOS FAMILIARES REGISTRADOS ---");
            while (rs.next()) {
                System.out.println(
                    "Fam. Código: " + rs.getInt("FamCod") + " | " +
                    "Jefe de Hogar: " + rs.getString("PerNom") + " " + rs.getString("PerApePat") + " (" + rs.getString("FamCab") + ") | " +
                    "Nº Miembros: " + rs.getInt("FamNumMiem") + " | " +
                    "Ingresos Totales: S/ " + rs.getDouble("FamIngTot")
                );
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
    }

    public static void buscarFamilia() {
        Connection conn = conectar();
        try {
            String sql = "SELECT f.*, p.PerNom, p.PerApePat, p.PerApeMat, v.VivUbigeo " +
                         "FROM h7m_familia f " +
                         "INNER JOIN h6m_persona p ON f.FamCab = p.PerDNI " +
                         "LEFT JOIN c3m_vivienda v ON p.PerViv = v.VivCod " +
                         "WHERE f.FamCod = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);
            System.out.print("Ingrese el Código de la Familia a buscar: ");
            int codigo = sc.nextInt();
            stmt.setInt(1, codigo);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("\n--- EXPEDIENTE SOCIOECONÓMICO FAMILIAR ---");
                System.out.println("Código Interno Familia : " + rs.getInt("FamCod"));
                System.out.println("Cabeza de Familia      : " + rs.getString("PerNom") + " " + rs.getString("PerApePat") + " " + rs.getString("PerApeMat"));
                System.out.println("DNI de la Cabeza       : " + rs.getString("FamCab"));
                System.out.println("Miembros Consolidados  : " + rs.getInt("FamNumMiem"));
                System.out.println("Ingresos Mensuales S/  : S/ " + rs.getDouble("FamIngTot"));
                System.out.println("Ubigeo Predial Vivienda: " + (rs.getString("VivUbigeo") != null ? rs.getString("VivUbigeo") : "No asignado"));
            } else {
                System.out.println("El código de familia ingresado no existe en el sistema.");
            }
            rs.close(); stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al buscar: " + e.getMessage());
        }
    }

    public static void actualizarFamilia() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE h7m_familia SET FamNumMiem = ?, FamIngTot = ? WHERE FamCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código de Familia a modificar: ");
            int codigo = sc.nextInt();

            if (!existeId(conn, "h7m_familia", "FamCod", codigo)) {
                System.out.println("No existe el código de grupo familiar especificado.");
                return;
            }

            System.out.print("Nuevo Número de Miembros Integrantes: ");
            int numMiem = sc.nextInt();

            System.out.print("Nuevo Ingreso Consolidado Familiar: ");
            double ingTot = sc.nextDouble();

            stmt.setInt(1, numMiem);
            stmt.setDouble(2, ingTot);
            stmt.setInt(3, codigo);

            stmt.executeUpdate();
            System.out.println("Indicadores socioeconómicos actualizados correctamente.");
            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al actualizar: " + e.getMessage());
        }
    }

    public static void eliminarFamilia() {
        Connection conn = conectar();
        try {
            String sql = "DELETE FROM h7m_familia WHERE FamCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código de Grupo Familiar a eliminar físicamente: ");
            int codigo = sc.nextInt();
            stmt.setInt(1, codigo);

            int filas = stmt.executeUpdate();
            if (filas > 0) {
                System.out.println("Grupo Familiar purgado y removido físicamente del catastro social.");
            } else {
                System.out.println("No se localizó el código de familia.");
            }
            stmt.close(); conn.close();
        } catch (Exception e) {
            System.out.println("Error al eliminar: " + e.getMessage());
        }
    }
}