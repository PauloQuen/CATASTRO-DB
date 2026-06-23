import java.sql.*;
import java.util.Scanner;

public class h6m_personaCRUD {

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
            System.out.println(e.getMessage());
        }
        return conn;
    }

    public static void menuPrincipal() {
        int opcion;
        do {
            System.out.println("\n===== CRUD PERSONA (CONTROL INTEGRIDAD) =====");
            System.out.println("1. Insertar");
            System.out.println("2. Listar (Con Datos de Vivienda)");
            System.out.println("3. Buscar");
            System.out.println("4. Actualizar");
            System.out.println("5. Eliminar");
            System.out.println("6. Salir");

            System.out.print("Opcion: ");
            opcion = sc.nextInt();
            sc.nextLine(); // CAMBIO: Limpieza del buffer de opción

            switch (opcion) {
                case 1: insertarPersona(); break;
                case 2: listarPersona(); break;
                case 3: buscarPersona(); break;
                case 4: actualizarPersona(); break;
                case 5: eliminarPersona(); break;
            }
        } while (opcion != 6);
    }

    // CAMBIO: Método auxiliar para comprobar la existencia de la vivienda asociada antes de insertar
    private static boolean existeVivienda(Connection conn, String vivCod) throws SQLException {
        String sql = "SELECT COUNT(*) FROM c3m_vivienda WHERE VivCod = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, vivCod);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public static void insertarPersona() {
        Connection conn = conectar();
        try {
            System.out.print("DNI: ");
            int dni = sc.nextInt();
            sc.nextLine();

            System.out.print("Nombre: ");
            String nom = sc.nextLine();

            System.out.print("Apellido Paterno: ");
            String apePat = sc.nextLine();

            System.out.print("Apellido Materno: ");
            String apeMat = sc.nextLine();

            System.out.print("Ingreso: ");
            double ingreso = sc.nextDouble();
            sc.nextLine();

            System.out.print("Codigo Vivienda: ");
            String viv = sc.nextLine();

            // CAMBIO: Validación lógica de integridad referencial para evitar excepciones de FK
            if (!existeVivienda(conn, viv)) {
                System.out.println("Error: El código de vivienda '" + viv + "' no existe en el sistema. Operación cancelada.");
                conn.close();
                return;
            }

            String sql = "INSERT INTO H6M_PERSONA (PerDNI,PerNom,PerApePat,PerApeMat,PerIng,PerViv,PerEstReg) VALUES (?,?,?,?,?,?,?)";
            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setInt(1, dni);
            stmt.setString(2, nom);
            stmt.setString(3, apePat);
            stmt.setString(4, apeMat);
            stmt.setDouble(5, ingreso);
            stmt.setString(6, viv);
            stmt.setString(7, "1");

            stmt.executeUpdate();
            System.out.println("Persona registrada controlando su consistencia relacional.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al insertar: " + e.getMessage());
        }
    }

    public static void listarPersona() {
        Connection conn = conectar();
        try {
            // CAMBIO: INNER JOIN para acoplar y desplegar información descriptiva de la vivienda de la persona
            String sql = "SELECT p.PerDNI, p.PerNom, p.PerApePat, p.PerApeMat, p.PerIng, v.VivUbigeo, v.VivVal " +
                         "FROM H6M_PERSONA p " +
                         "INNER JOIN c3m_vivienda v ON p.PerViv = v.VivCod " +
                         "WHERE p.PerEstReg = '1'";

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- LISTADO DE CONTRIBUYENTES ACTIVOS ---");
            while (rs.next()) {
                System.out.println(
                    "DNI: " + rs.getInt("PerDNI") + " | " +
                    rs.getString("PerNom") + " " + rs.getString("PerApePat") + " " + rs.getString("PerApeMat") + " | " +
                    "Ingreso: S/ " + rs.getDouble("PerIng") + " | " +
                    "Ubigeo Casa: " + rs.getString("VivUbigeo") + " | " +
                    "Val. Catastral: S/ " + rs.getDouble("VivVal")
                );
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
    }

    public static void buscarPersona() {
        Connection conn = conectar();
        try {
            // CAMBIO: INNER JOIN múltiple para recuperar tanto la vivienda como la vía de su dirección física
            String sql = "SELECT p.*, v.VivUbigeo, d.DirViaNom, d.DirNum FROM H6M_PERSONA p " +
                         "INNER JOIN c3m_vivienda v ON p.PerViv = v.VivCod " +
                         "INNER JOIN c3m_direccion d ON v.VivDir = d.DirCod " +
                         "WHERE p.PerDNI=?";

            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("DNI a buscar: ");
            int dni = sc.nextInt();
            sc.nextLine(); // CAMBIO: Limpieza del buffer tras leer entero

            stmt.setInt(1, dni);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("\n--- CONTRIBUYENTE ENCONTRADO ---");
                System.out.println("DNI: " + rs.getInt("PerDNI"));
                System.out.println("Nombre Completo: " + rs.getString("PerNom") + " " + rs.getString("PerApePat") + " " + rs.getString("PerApeMat"));
                System.out.println("Ingresos Mensuales: S/ " + rs.getDouble("PerIng"));
                System.out.println("Código Predio Asociado: " + rs.getString("PerViv"));
                System.out.println("Ubicación: " + rs.getString("DirViaNom") + " #" + rs.getInt("DirNum") + " (Ubigeo: " + rs.getString("VivUbigeo") + ")");
            } else {
                System.out.println("Persona no encontrada.");
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al buscar: " + e.getMessage());
        }
    }

    public static void actualizarPersona() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE H6M_PERSONA SET PerNom=?, PerIng=? WHERE PerDNI=?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("DNI de la persona a actualizar: ");
            int dni = sc.nextInt();
            sc.nextLine();

            System.out.print("Nuevo Nombre: ");
            String nombre = sc.nextLine();

            System.out.print("Nuevo Ingreso: ");
            double ingreso = sc.nextDouble();
            sc.nextLine(); // CAMBIO: Limpieza del buffer

            stmt.setString(1, nombre);
            stmt.setDouble(2, ingreso);
            stmt.setInt(3, dni);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Datos actualizados correctamente.");
            else System.out.println("No se encontró ninguna persona con ese DNI.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al actualizar: " + e.getMessage());
        }
    }

    public static void eliminarPersona() {
        Connection conn = conectar();
        try {
            String sql = "DELETE FROM H6M_PERSONA WHERE PerDNI=?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("DNI de la persona a eliminar: ");
            int dni = sc.nextInt();
            sc.nextLine(); // CAMBIO: Limpieza del buffer

            stmt.setInt(1, dni);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Registro eliminado físicamente.");
            else System.out.println("No se encontró el registro.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al eliminar: " + e.getMessage());
        }
    }
}