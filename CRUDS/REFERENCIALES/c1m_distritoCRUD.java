import java.sql.*;
import java.util.Scanner;

public class c1m_distritoCRUD {

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
            System.out.println("\n===== CRUD REFERENCIAL: DISTRITO =====");
            System.out.println("1. Insertar Distrito");
            System.out.println("2. Listar Distritos con su Provincia");
            System.out.println("3. Buscar Distrito por Código");
            System.out.println("4. Actualizar Distrito");
            System.out.println("5. Eliminar Distrito (Lógico)");
            System.out.println("6. Salir");
            System.out.print("Opción: ");
            opcion = sc.nextInt();
            sc.nextLine();

            switch (opcion) {
                case 1: insertarDistrito(); break;
                case 2: listarDistritos(); break;
                case 3: buscarDistrito(); break;
                case 4: actualizarDistrito(); break;
                case 5: eliminarDistritoLogico(); break;
            }
        } while (opcion != 6);
    }

    public static void insertarDistrito() {
        Connection conn = conectar();
        try {
            System.out.println("\n--- Provincias Activas Disponibles ---");
            String sqlPro = "SELECT ProCod, ProNom FROM c1m_provincia WHERE ProEstReg = '1'";
            Statement stPro = conn.createStatement();
            ResultSet rsPro = stPro.executeQuery(sqlPro);
            while(rsPro.next()){
                System.out.println("[" + rsPro.getInt("ProCod") + "] " + rsPro.getString("ProNom"));
            }
            rsPro.close();

            System.out.print("\nSeleccione el Código de la Provincia (FK): ");
            int fkPro = sc.nextInt();
            sc.nextLine();
            
            System.out.print("Nombre del Distrito (Ej. Miraflores, Yanahuara): ");
            String nom = sc.nextLine();

            String sql = "INSERT INTO c1m_distrito (FKDisPro, DisNom, DisEstReg) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, fkPro);
            stmt.setString(2, nom);
            stmt.setString(3, "1");

            stmt.executeUpdate();
            System.out.println("Distrito registrado de forma exitosa.");
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al insertar: " + e.getMessage());
        }
    }

    public static void listarDistritos() {
        Connection conn = conectar();
        try {
            String sql = "SELECT d.DisCod, d.DisNom, p.ProNom " +
                         "FROM c1m_distrito d " +
                         "INNER JOIN c1m_provincia p ON d.FKDisPro = p.ProCod " +
                         "WHERE d.DisEstReg = '1'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("\n--- LISTADO DE DISTRITOS ACTIVOS ---");
            while (rs.next()) {
                System.out.println("Cod: " + rs.getInt("DisCod") + 
                                   " | Distrito: " + rs.getString("DisNom") + 
                                   " | Provincia: " + rs.getString("ProNom"));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al listar: " + e.getMessage());
        }
    }

    public static void buscarDistrito() {
        Connection conn = conectar();
        try {
            String sql = "SELECT d.*, p.ProNom FROM c1m_distrito d " +
                         "INNER JOIN c1m_provincia p ON d.FKDisPro = p.ProCod WHERE d.DisCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese Código de Distrito: ");
            int cod = sc.nextInt();

            stmt.setInt(1, cod);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("\n--- DISTRITO ENCONTRADO ---");
                System.out.println("Código Distrito: " + rs.getInt("DisCod"));
                System.out.println("Nombre Distrito: " + rs.getString("DisNom"));
                System.out.println("Pertenece a Provincia: " + rs.getString("ProNom") + " (Cod: " + rs.getInt("FKDisPro") + ")");
                System.out.println("Estado Registro: " + rs.getString("DisEstReg"));
            } else {
                System.out.println("Distrito no encontrado.");
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al buscar: " + e.getMessage());
        }
    }

    public static void actualizarDistrito() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE c1m_distrito SET DisNom = ?, FKDisPro = ? WHERE DisCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese el Código del Distrito a modificar: ");
            int cod = sc.nextInt();
            sc.nextLine();

            System.out.print("Nuevo Nombre del Distrito: ");
            String nuevoNom = sc.nextLine();

            System.out.print("Nuevo Código de Provincia FK: ");
            int nuevaPro = sc.nextInt();

            stmt.setString(1, nuevoNom);
            stmt.setInt(2, nuevaPro);
            stmt.setInt(3, cod);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Distrito actualizado de forma correcta.");
            else System.out.println("No se encontró el registro.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al actualizar: " + e.getMessage());
        }
    }

    public static void eliminarDistritoLogico() {
        Connection conn = conectar();
        try {
            String sql = "UPDATE c1m_distrito SET DisEstReg = '0' WHERE DisCod = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            System.out.print("Ingrese el Código del Distrito a dar de baja: ");
            int cod = sc.nextInt();

            stmt.setInt(1, cod);

            int filas = stmt.executeUpdate();
            if (filas > 0) System.out.println("Distrito deshabilitado (Baja lógica).");
            else System.out.println("Código no existente.");

            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println("Error al deshabilitar: " + e.getMessage());
        }
    }
}