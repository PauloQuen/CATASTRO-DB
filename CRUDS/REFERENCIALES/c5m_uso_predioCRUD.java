import java.sql.*;
import java.util.Scanner;

public class c5m_uso_predioCRUD {

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
            System.out.println("\n===== CRUD REFERENCIAL: USO DE PREDIO =====");
            System.out.println("1. Insertar Uso de Predio");
            System.out.println("2. Listar Usos de Predio Activos");
            System.out.println("3. Buscar por Código");
            System.out.println("4. Actualizar Uso de Predio");
            System.out.println("5. Eliminar Uso de Predio (Lógico)");
            System.out.println("6. Salir");
            System.out.print("Opción: ");
            opcion = sc.nextInt();
            sc.nextLine(); 

            switch (opcion) {
                case 1: insertarUsoPredio(); break;
                case 2: listarUsosPredio(); break;
                case 3: buscarUsoPredio(); break;
                case 4: actualizarUsoPredio(); break;
                case 5: eliminarUsoPredioLogico(); break;
            }
        } while (opcion != 6);
    }

    public static void insertarUsoPredio() {
        Connection conn = conectar();
        try {
            String sql = "INSERT INTO c5m_uso_predio (UsoPreNom, UsoPreEstReg) VALUES (?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Nombre del Destino/Uso de Predio (Ej. Casa Habitación, Comercio): ");
            String nom = sc.nextLine();

            stmt.setString(1, nom);
            stmt.setString(2, "1");

            stmt.executeUpdate();
            System.out.println("Uso de predio registrado con éxito.");
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al insertar: " + e.getMessage());
        }
    }

    public static void listarUsosPredio() {
        Connection conn = conectar();
        try {
            String sql = "SELECT * FROM c5m_uso_predio WHERE UsoPreEstReg = '1'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- LISTADO DE USOS DE PREDIO ACTIVOS ---");
            while (rs.next()) {
                System.out.println("Cod: " + rs.getInt("UsoPreCod") + " | Destino: " + rs.getString("UsoPreNom"));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
    }

    public static void buscarUsoPredio() {
        Connection conn = conectar();
        try {
            String sql = "SELECT * FROM c5m_uso_predio WHERE UsoPreCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código de Uso de Predio: ");
            int cod = sc.nextInt();

            stmt.setInt(1, cod);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("\n--- DATOS ENCONTRADOS ---");
                System.out.println("Código: " + rs.getInt("UsoPreCod"));
                System.out.println("Destino/Uso: " + rs.getString("UsoPreNom"));
                System.out.println("Estado: " + rs.getString("UsoPreEstReg"));
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

    public static void actualizarUsoPredio() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE c5m_uso_predio SET UsoPreNom = ? WHERE UsoPreCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código a actualizar: ");
            int cod = sc.nextInt();
            sc.nextLine();

            System.out.print("Nuevo Nombre del Uso/Destino: ");
            String nuevoNom = sc.nextLine();

            stmt.setString(1, nuevoNom);
            stmt.setInt(2, cod);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Uso de predio modificado correctamente.");
            else System.out.println("No se encontró el registro.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al actualizar: " + e.getMessage());
        }
    }

    public static void eliminarUsoPredioLogico() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE c5m_uso_predio SET UsoPreEstReg = '0' WHERE UsoPreCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código para dar de baja: ");
            int cod = sc.nextInt();

            stmt.setInt(1, cod);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Uso de predio deshabilitado de forma lógica.");
            else System.out.println("Código no existente.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al deshabilitar: " + e.getMessage());
        }
    }
}