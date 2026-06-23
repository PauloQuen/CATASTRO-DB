import java.sql.*;
import java.util.Scanner;

public class c1m_regionCRUD {

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
            System.out.println("\n===== CRUD REFERENCIAL: REGION =====");
            System.out.println("1. Insertar Región");
            System.out.println("2. Listar Regiones Activas");
            System.out.println("3. Buscar Región por Código");
            System.out.println("4. Actualizar Región");
            System.out.println("5. Eliminar Región (Lógico)");
            System.out.println("6. Salir");
            System.out.print("Opción: ");
            opcion = sc.nextInt();
            sc.nextLine(); // Limpiar buffer

            switch (opcion) {
                case 1: insertarRegion(); break;
                case 2: listarRegiones(); break;
                case 3: buscarRegion(); break;
                case 4: actualizarRegion(); break;
                case 5: eliminarRegionLogico(); break;
            }
        } while (opcion != 6);
    }

    public static void insertarRegion() {
        Connection conn = conectar();
        try {
            String sql = "INSERT INTO c1m_region (RegNom, RegEstReg) VALUES (?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Nombre de la Región (Ej. Arequipa): ");
            String nom = sc.nextLine();

            stmt.setString(1, nom);
            stmt.setString(2, "1"); // Activo por defecto

            stmt.executeUpdate();
            System.out.println("Región registrada correctamente.");
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al insertar: " + e.getMessage());
        }
    }

    public static void listarRegiones() {
        Connection conn = conectar();
        try {
            String sql = "SELECT * FROM c1m_region WHERE RegEstReg = '1'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- LISTADO DE REGIONES ACTIVAS ---");
            while (rs.next()) {
                System.out.println("Código: " + rs.getInt("RegCod") + " | Nombre: " + rs.getString("RegNom"));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
    }

    public static void buscarRegion() {
        Connection conn = conectar();
        try {
            String sql = "SELECT * FROM c1m_region WHERE RegCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código de Región: ");
            int cod = sc.nextInt();

            stmt.setInt(1, cod);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("\n--- REGION ENCONTRADA ---");
                System.out.println("Código: " + rs.getInt("RegCod"));
                System.out.println("Nombre: " + rs.getString("RegNom"));
                System.out.println("Estado: " + rs.getString("RegEstReg"));
            } else {
                System.out.println("Región no encontrada.");
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al buscar: " + e.getMessage());
        }
    }

    public static void actualizarRegion() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE c1m_region SET RegNom = ? WHERE RegCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese el Código de la Región a modificar: ");
            int cod = sc.nextInt();
            sc.nextLine(); 

            System.out.print("Nuevo Nombre de la Región: ");
            String nuevoNom = sc.nextLine();

            stmt.setString(1, nuevoNom);
            stmt.setInt(2, cod);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Región actualizada correctamente.");
            else System.out.println("No se encontró el registro.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al actualizar: " + e.getMessage());
        }
    }

    public static void eliminarRegionLogico() {
        Connection conn = conectar();
        try {
            // Cambio de Estado de Registro a '0' para no destruir integridad referencial de provincias
            String sql = "UPDATE c1m_region SET RegEstReg = '0' WHERE RegCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese el Código de la Región a dar de baja: ");
            int cod = sc.nextInt();

            stmt.setInt(1, cod);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Región dada de baja de manera lógica.");
            else System.out.println("No se encontró el código.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al dar de baja: " + e.getMessage());
        }
    }
}