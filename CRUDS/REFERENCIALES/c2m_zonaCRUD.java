import java.sql.*;
import java.util.Scanner;

public class c2m_zonaCRUD {

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
            System.out.println("\n===== CRUD ZONA URBANA =====");
            System.out.println("1. Insertar Zona");
            System.out.println("2. Listar Zonas");
            System.out.println("3. Buscar por Código");
            System.out.println("4. Actualizar Zona");
            System.out.println("5. Eliminar Zona");
            System.out.println("6. Salir");
            System.out.print("Opción: ");
            opcion = sc.nextInt();

            switch (opcion) {
                case 1: insertarZona(); break;
                case 2: listarZonas(); break;
                case 3: buscarZona(); break;
                case 4: actualizarZona(); break;
                case 5: eliminarZona(); break;
            }
        } while (opcion != 6);
    }

    public static void insertarZona() {
        Connection conn = conectar();
        try {
            String sql = "INSERT INTO c2m_zona " +
                         "(ZonCod, ZonMun, ZonNom, ZonEstReg) " +
                         "VALUES (?, ?, ?, ?)";

            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Código de la Zona (Int): ");
            int zonCod = sc.nextInt();

            System.out.print("Código Municipalidad FK (Int): ");
            int zonMun = sc.nextInt();
            sc.nextLine(); // Limpiar buffer

            System.out.print("Nombre de la Zona (Ej: Urb. Primavera): ");
            String zonNom = sc.nextLine();

            stmt.setInt(1, zonCod);
            stmt.setInt(2, zonMun);
            stmt.setString(3, zonNom);
            stmt.setString(4, "1");

            stmt.executeUpdate();
            System.out.println("Zona registrada de manera exitosa.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al insertar: " + e.getMessage());
        }
    }

    public static void listarZonas() {
        Connection conn = conectar();
        try {
            String sql = "SELECT * FROM c2m_zona WHERE ZonEstReg = '1'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- LISTADO DE ZONAS URBANAS ACTIVAS ---");
            while (rs.next()) {
                System.out.println(
                    "Cod Zona: " + rs.getInt("ZonCod") + " | " +
                    "Muni ID: " + rs.getInt("ZonMun") + " | " +
                    "Nombre Sector: " + rs.getString("ZonNom")
                );
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
    }

    public static void buscarZona() {
        Connection conn = conectar();
        try {
            String sql = "SELECT * FROM c2m_zona WHERE ZonCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código de Zona a buscar: ");
            int zonCod = sc.nextInt();

            stmt.setInt(1, zonCod);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("\n--- DATOS DE LA ZONA ---");
                System.out.println("Código Sector: " + rs.getInt("ZonCod"));
                System.out.println("Municipalidad Regente (FK): " + rs.getInt("ZonMun"));
                System.out.println("Nombre: " + rs.getString("ZonNom"));
                System.out.println("Estado Registro: " + rs.getString("ZonEstReg"));
            } else {
                System.out.println("Zona urbana no encontrada.");
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al buscar: " + e.getMessage());
        }
    }

    public static void actualizarZona() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE c2m_zona SET ZonNom = ? WHERE ZonCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese el Código de la Zona a modificar: ");
            int zonCod = sc.nextInt();
            sc.nextLine(); // Limpiar buffer

            System.out.print("Nuevo Nombre de la Zona / Asentamiento / Urb: ");
            String nuevoNom = sc.nextLine();

            stmt.setString(1, nuevoNom);
            stmt.setInt(2, zonCod);

            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Zona actualizada correctamente.");
            } else {
                System.out.println("No se encontró ninguna zona con ese código.");
            }

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al actualizar: " + e.getMessage());
        }
    }

    public static void eliminarZona() {
        Connection conn = conectar();
        try {
            String sql = "DELETE FROM c2m_zona WHERE ZonCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese el Código de la Zona a eliminar: ");
            int zonCod = sc.nextInt();

            stmt.setInt(1, zonCod);

            int filasAfectadas = stmt.executeUpdate();
            if (filasAfectadas > 0) {
                System.out.println("Zona eliminada físicamente de la base de datos.");
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