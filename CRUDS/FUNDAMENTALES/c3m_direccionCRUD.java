import java.sql.*;
import java.util.Scanner;

public class c3m_direccionCRUD {

    static final String URL = "jdbc:mysql://localhost:3306/catastro_db";
    static final String USER = "root";
    static final String PASSWORD = "";

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        menuPrincipal();
    }

    // ==========================
    // CONEXION
    // ==========================
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

    // ==========================
    // MENU
    // ==========================
    public static void menuPrincipal() {
        int opcion;
        do {
            System.out.println("\n===== CRUD DIRECCION =====");
            System.out.println("1. Insertar Dirección");
            System.out.println("2. Listar Direcciones");
            System.out.println("3. Buscar por Código");
            System.out.println("4. Actualizar Dirección");
            System.out.println("5. Eliminar Dirección");
            System.out.println("6. Salir");
            System.out.print("Opción: ");
            opcion = sc.nextInt();

            switch (opcion) {
                case 1: insertarDireccion(); break;
                case 2: listarDirecciones(); break;
                case 3: buscarDireccion(); break;
                case 4: actualizarDireccion(); break;
                case 5: eliminarDireccion(); break;
            }
        } while (opcion != 6);
    }

    // ==========================
    // INSERT
    // ==========================
    public static void insertarDireccion() {
        Connection conn = conectar();
        try {
            String sql = "INSERT INTO C3M_DIRECCION " +
                         "(DirCod, FKDirZon, FKDirViaTip, DirViaNom, DirNum, DirInt, DirUrb, DirRef, DirCodPos, DirLat, DirLon, DirEstReg) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código Dirección (Int): ");
            int dirCod = sc.nextInt();
            
            System.out.print("Código Zona FK (Int): ");
            int fkZon = sc.nextInt();
            
            System.out.print("Código Tipo Vía FK (Int): ");
            int fkViaTip = sc.nextInt();
            sc.nextLine(); // Limpiar buffer

            System.out.print("Nombre de la Vía: ");
            String viaNom = sc.nextLine();

            System.out.print("Número: ");
            int num = sc.nextInt();

            System.out.print("Interior: ");
            int interior = sc.nextInt();
            sc.nextLine(); // Limpiar buffer

            System.out.print("Urbanización: ");
            String urb = sc.nextLine();

            System.out.print("Referencia: ");
            String ref = sc.nextLine();

            System.out.print("Código Postal: ");
            String codPos = sc.nextLine();

            System.out.print("Latitud (Int): ");
            int lat = sc.nextInt();

            System.out.print("Longitud (Int): ");
            int lon = sc.nextInt();
            sc.nextLine(); // Limpiar buffer

            // Seteo de parámetros a la Query
            stmt.setInt(1, dirCod);
            stmt.setInt(2, fkZon);
            stmt.setInt(3, fkViaTip);
            stmt.setString(4, viaNom);
            stmt.setInt(5, num);
            stmt.setInt(6, interior);
            stmt.setString(7, urb);
            stmt.setString(8, ref);
            stmt.setString(9, codPos);
            stmt.setInt(10, lat);
            stmt.setInt(11, lon);
            stmt.setString(12, "1"); // Estado de registro por defecto activo

            stmt.executeUpdate();
            System.out.println("Dirección registrada con éxito.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al insertar: " + e.getMessage());
        }
    }

    // ==========================
    // LIST
    // ==========================
    public static void listarDirecciones() {
        Connection conn = conectar();
        try {
            String sql = "SELECT * FROM C3M_DIRECCION WHERE DirEstReg = '1'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- LISTADO DE DIRECCIONES ACTIVAS ---");
            while (rs.next()) {
                System.out.println(
                    "Cod: " + rs.getInt("DirCod") + " | " +
                    "Urb: " + rs.getString("DirUrb") + " | " +
                    "Vía: " + rs.getString("DirViaNom") + " Nro: " + rs.getInt("DirNum") + " Int: " + rs.getInt("DirInt") + " | " +
                    "Ref: " + rs.getString("DirRef") + " | " +
                    "CP: " + rs.getString("DirCodPos")
                );
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
    }

    // ==========================
    // SEARCH
    // ==========================
    public static void buscarDireccion() {
        Connection conn = conectar();
        try {
            String sql = "SELECT * FROM C3M_DIRECCION WHERE DirCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código de Dirección a buscar: ");
            int dirCod = sc.nextInt();

            stmt.setInt(1, dirCod);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("\n--- DATOS ENCONTRADOS ---");
                System.out.println("Código: " + rs.getInt("DirCod"));
                System.out.println("Urbanización: " + rs.getString("DirUrb"));
                System.out.println("Vía: " + rs.getString("DirViaNom") + " #" + rs.getInt("DirNum") + " Interior: " + rs.getInt("DirInt"));
                System.out.println("Referencia: " + rs.getString("DirRef"));
                System.out.println("Código Postal: " + rs.getString("DirCodPos"));
                System.out.println("Coordenadas: Lat: " + rs.getInt("DirLat") + " / Lon: " + rs.getInt("DirLon"));
                System.out.println("Estado Registro: " + rs.getString("DirEstReg"));
            } else {
                System.out.println("Dirección no encontrada.");
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al buscar: " + e.getMessage());
        }
    }

    // ==========================
    // UPDATE
    // ==========================
    public static void actualizarDireccion() {
        Connection conn = conectar();
        try {
            // Actualizamos los datos más comunes y cambiantes como la vía, el número, la urb y la referencia.
            String sql = "UPDATE C3M_DIRECCION " +
                         "SET DirViaNom = ?, DirNum = ?, DirUrb = ?, DirRef = ? " +
                         "WHERE DirCod = ?";

            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese el Código de la Dirección a modificar: ");
            int dirCod = sc.nextInt();
            sc.nextLine(); // Limpiar buffer

            System.out.print("Nuevo Nombre de la Vía: ");
            String viaNom = sc.nextLine();

            System.out.print("Nuevo Número: ");
            int num = sc.nextInt();
            sc.nextLine(); // Limpiar buffer

            System.out.print("Nueva Urbanización: ");
            String urb = sc.nextLine();

            System.out.print("Nueva Referencia: ");
            String ref = sc.nextLine();

            stmt.setString(1, viaNom);
            stmt.setInt(2, num);
            stmt.setString(3, urb);
            stmt.setString(4, ref);
            stmt.setInt(5, dirCod);

            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Dirección actualizada correctamente.");
            } else {
                System.out.println("No se encontró ninguna dirección con ese código.");
            }

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al actualizar: " + e.getMessage());
        }
    }

    // ==========================
    // DELETE (Físico)
    // ==========================
    public static void eliminarDireccion() {
        Connection conn = conectar();
        try {
            String sql = "DELETE FROM C3M_DIRECCION WHERE DirCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese el Código de la Dirección a eliminar: ");
            int dirCod = sc.nextInt();

            stmt.setInt(1, dirCod);

            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Dirección eliminada de la base de datos.");
            } else {
                System.out.println("No se encontró el registro.");
            }

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al eliminar: " + e.getMessage());
        }
    }
}