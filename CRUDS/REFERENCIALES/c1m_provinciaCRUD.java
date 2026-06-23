import java.sql.*;
import java.util.Scanner;

public class c1m_provinciaCRUD {

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
            System.out.println("\n===== CRUD REFERENCIAL: PROVINCIA =====");
            System.out.println("1. Insertar Provincia");
            System.out.println("2. Listar Provincias con su Región");
            System.out.println("3. Buscar Provincia por Código");
            System.out.println("4. Actualizar Provincia");
            System.out.println("5. Eliminar Provincia (Lógico)");
            System.out.println("6. Salir");
            System.out.print("Opción: ");
            opcion = sc.nextInt();
            sc.nextLine(); // Limpiar buffer

            switch (opcion) {
                case 1: insertarProvincia(); break;
                case 2: listarProvincias(); break;
                case 3: buscarProvincia(); break;
                case 4: actualizarProvincia(); break;
                case 5: eliminarProvinciaLogico(); break;
            }
        } while (opcion != 6);
    }

    public static void insertarProvincia() {
        Connection conn = conectar();
        try {
            // 1. Mostrar regiones disponibles para ayudar al usuario
            System.out.println("\n--- Regiones Activas Disponibles ---");
            String sqlReg = "SELECT RegCod, RegNom FROM c1m_region WHERE RegEstReg = '1'";
            Statement stReg = conn.createStatement();
            ResultSet rsReg = stReg.executeQuery(sqlReg);
            while(rsReg.next()){
                System.out.println("[" + rsReg.getInt("RegCod") + "] " + rsReg.getString("RegNom"));
            }
            rsReg.close();

            // 2. Solicitar datos
            System.out.print("\nSeleccione el Código de la Región (FK): ");
            int fkReg = sc.nextInt();
            sc.nextLine(); // Buffer
            
            System.out.print("Nombre de la Provincia (Ej. Arequipa, Camaná): ");
            String nom = sc.nextLine();

            // 3. Insertar
            String sql = "INSERT INTO c1m_provincia (FKProReg, ProNom, ProEstReg) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, fkReg);
            stmt.setString(2, nom);
            stmt.setString(3, "1");

            stmt.executeUpdate();
            System.out.println("Provincia registrada correctamente.");
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al insertar (Verifique si la Región FK existe): " + e.getMessage());
        }
    }

    public static void listarProvincias() {
        Connection conn = conectar();
        try {
            // Aplicamos INNER JOIN para mostrar el nombre de la región de forma amigable
            String sql = "SELECT p.ProCod, p.ProNom, r.RegNom " +
                         "FROM c1m_provincia p " +
                         "INNER JOIN c1m_region r ON p.FKProReg = r.RegCod " +
                         "WHERE p.ProEstReg = '1'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- LISTADO DE PROVINCIAS ACTIVAS ---");
            while (rs.next()) {
                System.out.println("Cod: " + rs.getInt("ProCod") + 
                                   " | Provincia: " + rs.getString("ProNom") + 
                                   " | Región: " + rs.getString("RegNom"));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
    }

    public static void buscarProvincia() {
        Connection conn = conectar();
        try {
            String sql = "SELECT p.*, r.RegNom FROM c1m_provincia p " +
                         "INNER JOIN c1m_region r ON p.FKProReg = r.RegCod WHERE p.ProCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código de Provincia: ");
            int cod = sc.nextInt();

            stmt.setInt(1, cod);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("\n--- PROVINCIA ENCONTRADA ---");
                System.out.println("Código Provincia: " + rs.getInt("ProCod"));
                System.out.println("Nombre Provincia: " + rs.getString("ProNom"));
                System.out.println("Pertenece a Región: " + rs.getString("RegNom") + " (Cod: " + rs.getInt("FKProReg") + ")");
                System.out.println("Estado Registro: " + rs.getString("ProEstReg"));
            } else {
                System.out.println("Provincia no encontrada.");
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al buscar: " + e.getMessage());
        }
    }

    public static void actualizarProvincia() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE c1m_provincia SET ProNom = ?, FKProReg = ? WHERE ProCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese el Código de la Provincia a modificar: ");
            int cod = sc.nextInt();
            sc.nextLine();

            System.out.print("Nuevo Nombre de la Provincia: ");
            String nuevoNom = sc.nextLine();

            System.out.print("Nuevo Código de Región FK: ");
            int nuevaReg = sc.nextInt();

            stmt.setString(1, nuevoNom);
            stmt.setInt(2, nuevaReg);
            stmt.setInt(3, cod);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Provincia actualizada correctamente.");
            else System.out.println("No se encontró el registro.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al actualizar: " + e.getMessage());
        }
    }

    public static void eliminarProvinciaLogico() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE c1m_provincia SET ProEstReg = '0' WHERE ProCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese el Código de la Provincia a dar de baja: ");
            int cod = sc.nextInt();

            stmt.setInt(1, cod);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Provincia deshabilitada (Baja lógica).");
            else System.out.println("Código no existente.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al deshabilitar: " + e.getMessage());
        }
    }
}