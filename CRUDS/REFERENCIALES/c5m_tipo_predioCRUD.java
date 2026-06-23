import java.sql.*;
import java.util.Scanner;

public class c5m_tipo_predioCRUD {

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
            System.out.println("\n===== CRUD REFERENCIAL: TIPO DE PREDIO =====");
            System.out.println("1. Insertar Tipo de Predio");
            System.out.println("2. Listar Tipos de Predio Activos");
            System.out.println("3. Buscar por Código");
            System.out.println("4. Actualizar Tipo de Predio");
            System.out.println("5. Eliminar Tipo de Predio (Lógico)");
            System.out.println("6. Salir");
            System.out.print("Opción: ");
            opcion = sc.nextInt();
            sc.nextLine(); 

            switch (opcion) {
                case 1: insertarTipoPredio(); break;
                case 2: listarTiposPredio(); break;
                case 3: buscarTipoPredio(); break;
                case 4: actualizarTipoPredio(); break;
                case 5: eliminarTipoPredioLogico(); break;
            }
        } while (opcion != 6);
    }

    public static void insertarTipoPredio() {
        Connection conn = conectar();
        try {
            String sql = "INSERT INTO c5m_tipo_predio (TipPreNom, TipPreEstReg) VALUES (?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Nombre del Tipo de Predio (Ej. Urbano, Rural): ");
            String nom = sc.nextLine();

            stmt.setString(1, nom);
            stmt.setString(2, "1");

            stmt.executeUpdate();
            System.out.println("Tipo de predio guardado con éxito.");
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al insertar: " + e.getMessage());
        }
    }

    public static void listarTiposPredio() {
        Connection conn = conectar();
        try {
            String sql = "SELECT * FROM c5m_tipo_predio WHERE TipPreEstReg = '1'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- LISTADO DE TIPOS DE PREDIO ACTIVOS ---");
            while (rs.next()) {
                System.out.println("Cod: " + rs.getInt("TipPreCod") + " | Clasificación: " + rs.getString("TipPreNom"));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
    }

    public static void buscarTipoPredio() {
        Connection conn = conectar();
        try {
            String sql = "SELECT * FROM c5m_tipo_predio WHERE TipPreCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código de Tipo de Predio: ");
            int cod = sc.nextInt();

            stmt.setInt(1, cod);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("\n--- DATOS ENCONTRADOS ---");
                System.out.println("Código: " + rs.getInt("TipPreCod"));
                System.out.println("Clasificación: " + rs.getString("TipPreNom"));
                System.out.println("Estado: " + rs.getString("TipPreEstReg"));
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

    public static void actualizarTipoPredio() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE c5m_tipo_predio SET TipPreNom = ? WHERE TipPreCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código a actualizar: ");
            int cod = sc.nextInt();
            sc.nextLine();

            System.out.print("Nuevo Nombre del Tipo de Predio: ");
            String nuevoNom = sc.nextLine();

            stmt.setString(1, nuevoNom);
            stmt.setInt(2, cod);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Tipo de predio modificado correctamente.");
            else System.out.println("No se encontró el registro.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al actualizar: " + e.getMessage());
        }
    }

    public static void eliminarTipoPredioLogico() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE c5m_tipo_predio SET TipPreEstReg = '0' WHERE TipPreCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código para dar de baja: ");
            int cod = sc.nextInt();

            stmt.setInt(1, cod);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Tipo de predio deshabilitado de forma lógica.");
            else System.out.println("Código no existente.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al deshabilitar: " + e.getMessage());
        }
    }
}