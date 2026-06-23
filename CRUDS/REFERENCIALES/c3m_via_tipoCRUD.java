import java.sql.*;
import java.util.Scanner;

public class c3m_via_tipoCRUD {

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
            System.out.println("\n===== CRUD REFERENCIAL: TIPO DE VIA =====");
            System.out.println("1. Insertar Tipo de Vía");
            System.out.println("2. Listar Tipos de Vía Activos");
            System.out.println("3. Buscar por Código");
            System.out.println("4. Actualizar Tipo de Vía");
            System.out.println("5. Eliminar Tipo de Vía (Lógico)");
            System.out.println("6. Salir");
            System.out.print("Opción: ");
            opcion = sc.nextInt();
            sc.nextLine(); 

            switch (opcion) {
                case 1: insertarViaTipo(); break;
                case 2: listarViaTipos(); break;
                case 3: buscarViaTipo(); break;
                case 4: actualizarViaTipo(); break;
                case 5: eliminarViaTipoLogico(); break;
            }
        } while (opcion != 6);
    }

    public static void insertarViaTipo() {
        Connection conn = conectar();
        try {
            String sql = "INSERT INTO c3m_via_tipo (ViaTipNom, ViaTipEstReg) VALUES (?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Nombre del Tipo de Vía (Ej. Avenida): ");
            String nom = sc.nextLine();

            stmt.setString(1, nom);
            stmt.setString(2, "1");

            stmt.executeUpdate();
            System.out.println("Tipo de vía guardado con éxito.");
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al insertar: " + e.getMessage());
        }
    }

    public static void listarViaTipos() {
        Connection conn = conectar();
        try {
            String sql = "SELECT * FROM c3m_via_tipo WHERE ViaTipEstReg = '1'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- LISTADO DE TIPOS DE VIA ACTIVOS ---");
            while (rs.next()) {
                System.out.println("Cod: " + rs.getInt("ViaTipCod") + " | Descripción: " + rs.getString("ViaTipNom"));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
    }

    public static void buscarViaTipo() {
        Connection conn = conectar();
        try {
            String sql = "SELECT * FROM c3m_via_tipo WHERE ViaTipCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código de Tipo de Vía: ");
            int cod = sc.nextInt();

            stmt.setInt(1, cod);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("\n--- DATOS ENCONTRADOS ---");
                System.out.println("Código: " + rs.getInt("ViaTipCod"));
                System.out.println("Tipo Vía: " + rs.getString("ViaTipNom"));
                System.out.println("Estado: " + rs.getString("ViaTipEstReg"));
            } else {
                System.out.println("Registro no encontrado.");
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al buscar: " + e.getMessage());
        }
    }

    public static void actualizarViaTipo() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE c3m_via_tipo SET ViaTipNom = ? WHERE ViaTipCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código a actualizar: ");
            int cod = sc.nextInt();
            sc.nextLine();

            System.out.print("Nuevo Nombre del Tipo de Vía: ");
            String nuevoNom = sc.nextLine();

            stmt.setString(1, nuevoNom);
            stmt.setInt(2, cod);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Tipo de vía modificado correctamente.");
            else System.out.println("No se encontró el registro especificado.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al actualizar: " + e.getMessage());
        }
    }

    public static void eliminarViaTipoLogico() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE c3m_via_tipo SET ViaTipEstReg = '0' WHERE ViaTipCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código para dar de baja: ");
            int cod = sc.nextInt();

            stmt.setInt(1, cod);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Tipo de vía deshabilitado de forma lógica.");
            else System.out.println("Código no existente.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al deshabilitar: " + e.getMessage());
        }
    }
}